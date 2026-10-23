package app.lokey0905.location.api

import org.json.JSONObject
import java.net.URI

data class CatalogRelease(val versionName: String, val downloadUrl: String)

private val catalogNumericVersion = Regex("[vV]?\\d+(?:\\.\\d+)*")
private val catalogApkName = Regex("[A-Za-z0-9][A-Za-z0-9._-]*\\.apk", RegexOption.IGNORE_CASE)

/** The new page uses the same joystick metadata as Pokémon, without a new server contract. */
fun parseJoystickCatalogRelease(json: String): CatalogRelease {
    val joystick = JSONObject(json).getJSONObject("pogo").getJSONObject("joystick")
    val version = requireCatalogVersion(joystick.getString("version"))
    val url = joystick.getString("url").trim()
    requireHttpsCatalogUrl(url)
    return CatalogRelease(version, url)
}

/** Only published, numeric stable releases and APK assets belonging to this repository are accepted. */
fun parseFitnessCatalogRelease(json: String): CatalogRelease {
    val release = JSONObject(json)
    require(release.get("draft") == false && release.get("prerelease") == false) {
        "Fitness Hooks release is not a published stable release"
    }
    val tag = release.getString("tag_name").trim()
    require(catalogNumericVersion.matches(tag)) { "Invalid Fitness Hooks release version" }
    val version = tag.removePrefix("v").removePrefix("V")
    val assets = release.getJSONArray("assets")
    val trustedAssets = buildList {
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.opt("name") as? String ?: continue
            if (!catalogApkName.matches(name)) continue
            val url = asset.opt("browser_download_url") as? String ?: continue
            val uri = runCatching { requireHttpsCatalogUrl(url) }.getOrNull() ?: continue
            if (uri.host.equals("github.com", ignoreCase = true) &&
                uri.rawQuery == null &&
                uri.rawPath == "/lokey0905/Fitness-Hooks/releases/download/$tag/$name"
            ) {
                add(name to url)
            }
        }
    }
    // Preserve the project's published APK name if other assets are attached to the release later.
    val downloadUrl = (trustedAssets.firstOrNull { it.first == "app-debug.apk" }
        ?: trustedAssets.singleOrNull())?.second
        ?: throw IllegalArgumentException("Fitness Hooks release has no unambiguous trusted APK")
    return CatalogRelease(version, downloadUrl)
}

/** Match the existing Pokémon page's APKMirror version formatting, including build suffixes. */
fun catalogApkMirrorVersionToken(versionName: String): String =
    requireCatalogVersion(versionName)
        .replace(".", "-")
        .replace(" (", "-")
        .replace(")", "")

internal fun requireCatalogVersion(versionName: String): String {
    val version = versionName.trim()
    require(version.isNotBlank() && !version.equals("ERROR", ignoreCase = true)) {
        "No remote version available"
    }
    require(version.first().isDigit() && version.none { it == '\n' || it == '\r' }) {
        "Invalid remote version"
    }
    return version
}

private fun requireHttpsCatalogUrl(url: String): URI {
    val uri = URI(url)
    require(uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() && uri.userInfo == null &&
        (uri.port == -1 || uri.port == 443) && uri.rawFragment == null
    ) { "Invalid catalog download URL" }
    return uri
}
