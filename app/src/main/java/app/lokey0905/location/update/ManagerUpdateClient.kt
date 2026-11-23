package app.lokey0905.location.update

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.DateTimeException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException

internal sealed interface ManagerUpdateResult {
    data class Release(val version: String, val tag: String, val notes: String) : ManagerUpdateResult
    data class RateLimited(val statusCode: Int, val retryAtMillis: Long) : ManagerUpdateResult
    data class HttpError(val statusCode: Int, val retryAtMillis: Long? = null) : ManagerUpdateResult
    data object InvalidResponse : ManagerUpdateResult
    data class NetworkError(val reason: UpdateNetworkFailure) : ManagerUpdateResult
}

internal enum class UpdateNetworkFailure { DNS, TIMEOUT, TLS, CONNECTION }

private const val MINIMUM_RETRY_MILLIS = 60_000L
private const val SUCCESS_CHECK_INTERVAL_MILLIS = 3_600_000L
private const val MAX_RELEASE_BYTES = 512 * 1024
private const val MAX_ERROR_BYTES = 16 * 1024
private val releaseTagPattern = Regex("[vV]?([0-9]+(?:\\.[0-9]+)*)")

internal fun parseManagerRelease(body: String): ManagerUpdateResult.Release? {
    val release = parseObject(body) ?: return null
    // A display title must not override an existing, but unusable, release tag.
    val tag = release.opt(if (release.has("tag_name")) "tag_name" else "name") as? String
        ?: return null
    val version = releaseTagPattern.matchEntire(tag)?.groupValues?.get(1) ?: return null
    val notes = when (val value = release.opt("body")) {
        null, JSONObject.NULL -> ""
        is String -> value
        else -> return null
    }
    return ManagerUpdateResult.Release(version, tag, notes)
}

internal fun nextManagerUpdateCheckAt(result: ManagerUpdateResult, nowMillis: Long): Long {
    if (result is ManagerUpdateResult.Release) {
        return addDelay(nowMillis, SUCCESS_CHECK_INTERVAL_MILLIS)
    }
    val minimum = addDelay(nowMillis, MINIMUM_RETRY_MILLIS)
    return when (result) {
        is ManagerUpdateResult.RateLimited -> maxOf(minimum, result.retryAtMillis)
        is ManagerUpdateResult.HttpError -> maxOf(minimum, result.retryAtMillis ?: minimum)
        else -> minimum
    }
}

internal class ManagerUpdateClient(
    private val openConnection: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    },
    private val nanoTime: () -> Long = System::nanoTime
) {
    fun fetch(
        endpoint: String,
        userAgent: String,
        nowMillis: Long = System.currentTimeMillis()
    ): ManagerUpdateResult {
        val startedNanos = nanoTime()
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(URL(endpoint))
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", userAgent)

            // getInputStream throws for HTTP errors, so classify the status first.
            val status = connection.responseCode
            if (status in 200..299) {
                val body = connection.inputStream?.let { readBounded(it, MAX_RELEASE_BYTES) }
                    ?: return ManagerUpdateResult.InvalidResponse
                return parseManagerRelease(body) ?: ManagerUpdateResult.InvalidResponse
            }

            val serverDate = parseHttpDate(connection.getHeaderField("Date"))
            val retryAfterHeader = connection.getHeaderField("Retry-After")
            val resetHeader = connection.getHeaderField("X-RateLimit-Reset")
            val remaining = nonNegativeNumber(connection.getHeaderField("X-RateLimit-Remaining"))
            val errorBody = try {
                connection.errorStream?.let { readBounded(it, MAX_ERROR_BYTES) }.orEmpty()
            } catch (_: IOException) {
                // Keep an already received HTTP status even if its optional body fails.
                ""
            }
            // Include even a slow error body before starting relative waits. nanoTime
            // avoids shortening them when the wall clock changes during the request.
            val elapsedMillis = (nanoTime() - startedNanos).coerceAtLeast(0L) / 1_000_000L
            val receivedAtMillis = addDelay(nowMillis, elapsedMillis)
            val retryAfter = retryAfterDeadline(retryAfterHeader, receivedAtMillis, serverDate)
            val reset = nonNegativeNumber(resetHeader)?.let(::secondsToMillis)
                ?.let { localDeadline(it, receivedAtMillis, serverDate) }
            val limited = status == 429 || (status == 403 && (
                remaining == 0L || retryAfter != null || isRateLimitMessage(errorBody)
            ))
            val minimum = addDelay(receivedAtMillis, MINIMUM_RETRY_MILLIS)
            return if (limited) {
                ManagerUpdateResult.RateLimited(
                    status, maxOf(minimum, retryAfter ?: minimum, reset ?: minimum)
                )
            } else {
                ManagerUpdateResult.HttpError(status, retryAfter?.let { maxOf(minimum, it) })
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: UnknownHostException) {
            return ManagerUpdateResult.NetworkError(UpdateNetworkFailure.DNS)
        } catch (_: SocketTimeoutException) {
            return ManagerUpdateResult.NetworkError(UpdateNetworkFailure.TIMEOUT)
        } catch (_: SSLException) {
            return ManagerUpdateResult.NetworkError(UpdateNetworkFailure.TLS)
        } catch (_: IOException) {
            return ManagerUpdateResult.NetworkError(UpdateNetworkFailure.CONNECTION)
        } finally {
            connection?.disconnect()
        }
    }
}

private fun parseObject(body: String): JSONObject? = try {
    val tokener = JSONTokener(body)
    val value = tokener.nextValue()
    if (value is JSONObject && tokener.nextClean() == '\u0000') value else null
} catch (_: JSONException) {
    null
}

private fun isRateLimitMessage(body: String): Boolean {
    val message = (parseObject(body)?.opt("message") as? String)?.lowercase(Locale.ROOT)
        ?: return false
    return message.contains("rate limit") || message.contains("secondary limit")
}

private fun retryAfterDeadline(value: String?, nowMillis: Long, serverDate: Long?): Long? {
    nonNegativeNumber(value)?.let { seconds ->
        return secondsToMillis(seconds)?.let { checkedAddDelay(nowMillis, it) }
    }
    return parseHttpDate(value)?.let { localDeadline(it, nowMillis, serverDate) }
}

private fun parseHttpDate(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant().toEpochMilli()
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

private fun nonNegativeNumber(value: String?): Long? {
    val number = value?.trim()?.takeIf { it.isNotEmpty() && it.all { digit -> digit in '0'..'9' } }
        ?: return null
    return number.toLongOrNull()
}

private fun secondsToMillis(seconds: Long): Long? =
    if (seconds > Long.MAX_VALUE / 1_000L) null else seconds * 1_000L

private fun checkedAddDelay(nowMillis: Long, delay: Long): Long? =
    if (nowMillis > Long.MAX_VALUE - delay) null else nowMillis + delay

private fun addDelay(nowMillis: Long, delay: Long): Long =
    checkedAddDelay(nowMillis, delay) ?: Long.MAX_VALUE

private fun localDeadline(deadline: Long, nowMillis: Long, serverDate: Long?): Long? {
    if (serverDate == null) return maxOf(nowMillis, deadline)
    val delay = when {
        deadline <= serverDate -> 0L
        serverDate < 0 && deadline > Long.MAX_VALUE + serverDate -> return null
        else -> deadline - serverDate
    }
    return checkedAddDelay(nowMillis, delay)
}

private fun readBounded(stream: InputStream, limit: Int): String? = stream.use { input ->
    val output = ByteArrayOutputStream(minOf(limit, 8 * 1024))
    val buffer = ByteArray(4 * 1024)
    while (true) {
        val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size() + 1))
        if (count == -1) break
        if (output.size() + count > limit) return@use null
        output.write(buffer, 0, count)
    }
    output.toString(Charsets.UTF_8.name())
}
