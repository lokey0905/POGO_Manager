package app.lokey0905.location.api

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class PgSharpRootApi {
    suspend fun getVersionInfo(url: String): PgSharpRootVersionInfo? {
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
                    throw IOException("PGSharp Root request failed with HTTP $responseCode")
                }

                val response = activeConnection.inputStream.bufferedReader().use { it.readText() }
                PgSharpRootCodec.parseApiResponse(response)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PgSharpRootApi", "Failed to fetch or parse PGSharp Root versions", e)
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}
