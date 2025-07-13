package app.lokey0905.location.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class Pokemon {
    suspend fun checkPokemon(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val urlObject = URL(url)
                val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }

                inputStream.close()

                // Extract the version number from the response
                response.trim().substringAfter("")
            } catch (e: Exception) {
                e.printStackTrace()
                "ERROR"
            }
        }
    }
}

