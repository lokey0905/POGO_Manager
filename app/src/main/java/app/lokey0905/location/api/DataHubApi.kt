package app.lokey0905.location.api

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DataHubApi {
    suspend fun getVersionInfo(url: String, downloadUrlFormat: String): DataHubVersionInfo? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val activeConnection = URL(url).openConnection() as HttpURLConnection
                connection = activeConnection
                activeConnection.requestMethod = "GET"
                activeConnection.connectTimeout = 15000
                activeConnection.readTimeout = 20000

                val responseCode = activeConnection.responseCode
                if (responseCode !in HttpURLConnection.HTTP_OK until
                    HttpURLConnection.HTTP_MULT_CHOICE
                ) {
                    throw IOException("Data Hub request failed with HTTP $responseCode")
                }

                val response = activeConnection.inputStream.bufferedReader().use { it.readText() }
                currentCoroutineContext().ensureActive()
                DataHubVersionParser.parseApiResponse(response, downloadUrlFormat)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DataHubApi", "Failed to fetch or parse Data Hub version", e)
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}
