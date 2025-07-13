package app.lokey0905.location.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class Pokemod {
    suspend fun checkPokemod(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val urlObject = URL(url)
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                val inputStream = if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }

                val response = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                inputStream?.close()

                response.contains("#GAME_VERSION@")
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}