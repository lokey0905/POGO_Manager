package app.lokey0905.location.api

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class Pokemon {
    suspend fun checkPokemon(url: String): String {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val urlObject = URL(url)
                val activeConnection = urlObject.openConnection() as HttpURLConnection
                connection = activeConnection
                activeConnection.requestMethod = "GET"
                activeConnection.connectTimeout = 15000
                activeConnection.readTimeout = 20000

                val responseCode = activeConnection.responseCode
                if (responseCode !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
                    Log.w("Pokemon", "Minimum version check failed with HTTP $responseCode")
                    return@withContext "ERROR"
                }

                val response = activeConnection.inputStream.bufferedReader().use { it.readText() }

                // Extract the version number from the response
                response.trim().substringAfter('', "").trim().ifBlank { "ERROR" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Pokemon", "Minimum version check failed", e)
                "ERROR"
            } finally {
                connection?.disconnect()
            }
        }
    }
}

