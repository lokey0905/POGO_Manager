package app.lokey0905.location.api

import android.content.Context
import app.lokey0905.location.R
import app.lokey0905.location.catalog.CatalogApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Loads one source at a time so each card can update independently in its view lifecycle. */
class CatalogVersionRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(app: CatalogApp): CatalogRelease = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val release = try {
            loadOnIo(app)
        } catch (error: Exception) {
            // Legacy blocking APIs may return an empty value after interruption. Never turn that
            // into a failed card check when the owning view has actually been cancelled.
            currentCoroutineContext().ensureActive()
            throw error
        }
        currentCoroutineContext().ensureActive()
        release
    }

    private suspend fun loadOnIo(app: CatalogApp): CatalogRelease = when (app) {
        CatalogApp.PIKMIN -> {
            val version = ApkPure().getVersionHistory(
                appContext.getString(R.string.catalog_package_pikmin)
            ).firstOrNull()?.versionName
                ?: throw IOException("Pikmin version source returned no versions")
            CatalogRelease(
                requireCatalogVersion(version),
                appContext.getString(R.string.catalog_url_pikmin_download)
            )
        }
        CatalogApp.JOYSTICK -> parseJoystickCatalogRelease(
            readJson(appContext.getString(R.string.url_appInfo))
        )
        CatalogApp.DEFIT -> apkMirrorRelease(
            ApkPure().getDeFitLatestVersion(), R.string.url_defit
        )
        CatalogApp.FITNESS -> parseFitnessCatalogRelease(
            readJson(appContext.getString(R.string.catalog_url_fitness_api))
        )
        CatalogApp.APKMIRROR_INSTALLER -> apkMirrorRelease(
            ApkPure().getAPKMirrorInstallerLatestVersion(), R.string.url_apkMirrorInstaller
        )
        CatalogApp.GOOGLE_FIT, CatalogApp.HEALTH_CONNECT ->
            throw IllegalArgumentException("This app uses its store or system settings, not a remote version")
    }

    private fun apkMirrorRelease(version: String, urlResource: Int): CatalogRelease {
        val normalizedVersion = requireCatalogVersion(version)
        val urlVersion = catalogApkMirrorVersionToken(normalizedVersion)
        return CatalogRelease(normalizedVersion, appContext.getString(urlResource, urlVersion, urlVersion))
    }

    private suspend fun readJson(url: String): String {
        currentCoroutineContext().ensureActive()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "POGO-Manager")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Catalog source returned HTTP ${connection.responseCode}")
            }
            val payload = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (payload.size() + count > MAX_JSON_BYTES) {
                        throw IOException("Catalog response exceeded size limit")
                    }
                    payload.write(buffer, 0, count)
                }
            }
            currentCoroutineContext().ensureActive()
            return payload.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_JSON_BYTES = 1024 * 1024
    }
}
