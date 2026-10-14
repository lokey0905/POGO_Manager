package app.lokey0905.location.api

import app.lokey0905.location.version.compareVersions
import org.json.JSONObject

enum class MhnToolsChannel {
    RELEASE,
    TEST
}

data class MhnToolsVersionInfo(
    val supportedVersions: List<String>,
    val appVersion: String?,
    val downloadUrl: String?
)

/** Parses one channel without changing the selected game version or another channel's data. */
fun parseMhnToolsVersionInfo(json: String, channel: MhnToolsChannel): MhnToolsVersionInfo {
    // A missing or malformed support object is a failed check, not a confirmed empty list.
    val supportedVersions = JSONObject(json).getJSONObject("supportedVersions")
    val versionKeys = supportedVersions.keys().asSequence().toList()
        .sortedWith { first, second -> compareVersions(second, first) }
    val gameVersions = mutableListOf<String>()
    var latestAppVersionCode = -1L
    var latestAppVersionName: String? = null
    var latestAppVersionHash: String? = null

    versionKeys.forEach { versionKey ->
        val versionObject = supportedVersions.optJSONObject(versionKey) ?: return@forEach
        val gameVersion = versionObject.optJSONObject("game")
            ?.optString("gameVersionName", versionKey)
            ?.takeIf(String::isNotBlank)
            ?: versionKey
        gameVersions.add(gameVersion)

        val app = versionObject.optJSONObject("app")
        val appVersionCode = app?.optLong("appVersionCode", -1L) ?: -1L
        if (appVersionCode > latestAppVersionCode) {
            latestAppVersionCode = appVersionCode
            latestAppVersionName = app?.optString("appVersionName")?.takeIf(String::isNotBlank)
            latestAppVersionHash = app?.optString("appVersionHash")?.takeIf(String::isNotBlank)
        }
    }

    val downloadUrl = if (latestAppVersionName != null && latestAppVersionHash != null) {
        val channelPrefix = if (channel == MhnToolsChannel.TEST) "test-" else ""
        "https://assets.mhntools.net/${channelPrefix}mhntools-$latestAppVersionName-$latestAppVersionHash.apk?"
    } else {
        null
    }
    return MhnToolsVersionInfo(gameVersions.distinct(), latestAppVersionName, downloadUrl)
}

/** Uses available channels only; a failed channel must not discard the successful channel. */
fun mergeMhnSupportedVersions(release: List<String>?, test: List<String>?): List<String> =
    (release.orEmpty() + test.orEmpty()).distinct()
        .sortedWith { first, second -> compareVersions(second, first) }
