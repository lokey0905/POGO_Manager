package app.lokey0905.location.api

import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

data class DataHubVersionInfo(
    val appVersion: String,
    val downloadUrl: String
)

internal object DataHubVersionParser {
    private val versionRegex = Regex("""\d+(?:\.\d+)+""")

    fun parseApiResponse(response: String, downloadUrlFormat: String): DataHubVersionInfo {
        val version = (JSONObject(response).opt("appName") as? String)?.trim()
        if (version == null || !versionRegex.matches(version)) {
            throw JSONException("Data Hub appName must be a dotted numeric version")
        }

        return DataHubVersionInfo(
            appVersion = version,
            downloadUrl = String.format(Locale.ROOT, downloadUrlFormat, version)
        )
    }
}
