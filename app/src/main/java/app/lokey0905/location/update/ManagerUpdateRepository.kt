package app.lokey0905.location.update

import android.content.Context
import android.util.Log
import app.lokey0905.location.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

internal data class ManagerUpdateCheck(val result: ManagerUpdateResult, val cached: Boolean = false)

internal sealed interface ManagerUpdateCacheDecision {
    data object Fetch : ManagerUpdateCacheDecision
    data object Wait : ManagerUpdateCacheDecision
    data class Reuse(val result: ManagerUpdateResult) : ManagerUpdateCacheDecision
}

internal fun managerUpdateCacheDecision(
    checkedAtMillis: Long,
    nextCheckAtMillis: Long,
    releaseJson: String?,
    nowMillis: Long,
    failureJson: String? = null
): ManagerUpdateCacheDecision {
    if (nowMillis < checkedAtMillis || nowMillis >= nextCheckAtMillis) {
        return ManagerUpdateCacheDecision.Fetch
    }
    if (releaseJson == null) {
        if (failureJson == null) return ManagerUpdateCacheDecision.Wait
        return parseManagerUpdateFailure(failureJson)?.let { ManagerUpdateCacheDecision.Reuse(it) }
            ?: ManagerUpdateCacheDecision.Fetch
    }
    // An unreadable cache must not masquerade as an up-to-date result.
    return parseManagerRelease(releaseJson)?.let { ManagerUpdateCacheDecision.Reuse(it) }
        ?: ManagerUpdateCacheDecision.Fetch
}

/** Store only structured status, never the server's error body or public IP. */
internal fun managerUpdateFailureJson(result: ManagerUpdateResult): String? {
    val json = JSONObject()
    when (result) {
        is ManagerUpdateResult.Release -> return null
        is ManagerUpdateResult.RateLimited -> {
            json.put("kind", "rate_limit").put("status", result.statusCode).put("retry_at", result.retryAtMillis)
        }
        is ManagerUpdateResult.HttpError -> {
            json.put("kind", "http").put("status", result.statusCode)
            result.retryAtMillis?.let { json.put("retry_at", it) }
        }
        is ManagerUpdateResult.NetworkError -> json.put("kind", "network").put("reason", result.reason.name)
        ManagerUpdateResult.InvalidResponse -> json.put("kind", "invalid_response")
    }
    return json.toString()
}

internal fun parseManagerUpdateFailure(value: String): ManagerUpdateResult? = try {
    val json = JSONObject(value)
    val status = json.optInt("status", -1)
    val retryAt = json.optLong("retry_at", -1).takeIf { it > 0 }
    when (json.optString("kind")) {
        "rate_limit" -> if (status in listOf(403, 429) && retryAt != null) {
            ManagerUpdateResult.RateLimited(status, retryAt)
        } else null
        "http" -> if (status in 100..599 && status !in 200..299) {
            ManagerUpdateResult.HttpError(status, retryAt)
        } else null
        "network" -> UpdateNetworkFailure.entries.firstOrNull { it.name == json.optString("reason") }
            ?.let { ManagerUpdateResult.NetworkError(it) }
        "invalid_response" -> ManagerUpdateResult.InvalidResponse
        else -> null
    }
} catch (_: JSONException) {
    null
}

/** App-wide gate: rotation/background returns must not start duplicate GitHub requests. */
internal object ManagerUpdateRepository {
    private val mutex = Mutex()
    private val client = ManagerUpdateClient()

    suspend fun check(context: Context, endpoint: String): ManagerUpdateCheck? =
        withContext(Dispatchers.IO) {
            // A recreated activity waits for the existing request, then reads its cached result.
            mutex.withLock {
                val preferences = context.getSharedPreferences("manager_update", Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                val checkedAt = preferences.getLong("checked_at", 0)
                val nextCheckAt = preferences.getLong("next_check_at", 0)
                when (val decision = managerUpdateCacheDecision(
                    checkedAt, nextCheckAt, preferences.getString("release", null), now,
                    preferences.getString("failure", null)
                )) {
                    ManagerUpdateCacheDecision.Wait -> {
                        Log.d("ManagerUpdate", "Check deferred until $nextCheckAt")
                        return@withContext null
                    }
                    is ManagerUpdateCacheDecision.Reuse ->
                        return@withContext ManagerUpdateCheck(decision.result, cached = true)
                    ManagerUpdateCacheDecision.Fetch -> Unit
                }

                // Persist a short attempt gate before I/O, including if the process is interrupted.
                preferences.edit()
                    .putLong("checked_at", now)
                    .putLong("next_check_at", nextManagerUpdateCheckAt(ManagerUpdateResult.InvalidResponse, now))
                    .remove("release")
                    .remove("failure")
                    .apply()
                val fetched = client.fetch(endpoint, "POGO-Manager/${BuildConfig.VERSION_NAME}")
                val finishedAt = System.currentTimeMillis()
                val retryAt = nextManagerUpdateCheckAt(fetched, finishedAt)
                val result = if (fetched is ManagerUpdateResult.RateLimited) {
                    fetched.copy(retryAtMillis = retryAt)
                } else fetched
                preferences.edit().apply {
                    putLong("checked_at", finishedAt)
                    putLong("next_check_at", retryAt)
                    putString("failure", managerUpdateFailureJson(result))
                    if (result is ManagerUpdateResult.Release) {
                        putString("release", JSONObject().apply {
                            put("tag_name", result.tag)
                            put("body", result.notes)
                        }.toString())
                    }
                    apply()
                }
                // Do not log response bodies: GitHub error messages can contain the public IP.
                val status = when (result) {
                    is ManagerUpdateResult.Release -> "success tag=${result.tag}"
                    is ManagerUpdateResult.RateLimited -> "rate_limited HTTP=${result.statusCode}"
                    is ManagerUpdateResult.HttpError -> "http_error HTTP=${result.statusCode}"
                    is ManagerUpdateResult.NetworkError -> "network_error ${result.reason}"
                    ManagerUpdateResult.InvalidResponse -> "invalid_response"
                }
                Log.i("ManagerUpdate", "$status; nextCheckAt=$retryAt")
                ManagerUpdateCheck(result)
            }
        }
}
