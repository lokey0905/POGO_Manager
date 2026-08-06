package app.lokey0905.location.api

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Pokemod {
    /**
     * Returns true only when Pokemod explicitly marks the game version as unsupported,
     * false when the version is supported, and null when the check itself fails.
     */
    suspend fun checkPokemod(url: String): Boolean? {
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
                val responseStream = if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    activeConnection.errorStream
                } else {
                    activeConnection.inputStream
                }
                val response = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                // Pokemod's API wording is counterintuitive and must keep these meanings:
                // "#GAME_VERSION@" means the game version is unsupported.
                // HTTP 422 "Unsupported version" means the version is supported but has no token.
                if (response.contains("#GAME_VERSION@")) {
                    Log.i("Pokemod", "Game version is not supported: $url")
                    return@withContext true
                }

                if (responseCode == 422) {
                    val message = runCatching {
                        JSONObject(response).optString("message").trim()
                    }.getOrDefault("")
                    if (message.equals("Unsupported version", ignoreCase = true)) {
                        Log.i("Pokemod", "Game version is supported but has no token: $url")
                        return@withContext false
                    }
                }

                if (responseCode !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
                    Log.w("Pokemod", "Version check failed with HTTP $responseCode: $url")
                    return@withContext null
                }

                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Pokemod", "Version check failed: $url", e)
                null
            } finally {
                connection?.disconnect()
            }
        }
    }

    suspend fun getPokemodVersion(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                var currentUrl = url
                var redirectCount = 0
                val maxRedirects = 5

                // 手動處理重定向
                while (redirectCount < maxRedirects) {
                    val urlObject = URL(currentUrl)
                    val connection: HttpURLConnection = urlObject.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false // 關閉自動重定向
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000

                    try {
                        connection.connect()
                        val responseCode = connection.responseCode
                        Log.i("Pokemod", "Response Code: $responseCode, URL: $currentUrl")

                        // 檢查是否為重定向狀態碼
                        if (responseCode in 300..399) {
                            val location = connection.getHeaderField("Location")

                            if (location.isNullOrEmpty()) {
                                Log.e("Pokemod", "重定向但沒有 Location header")
                                break
                            }

                            // 處理相對路徑和絕對路徑
                            currentUrl = if (location.startsWith("http")) {
                                location
                            } else {
                                URL(urlObject, location).toString()
                            }

                            Log.i("Pokemod", "重定向到: $currentUrl")
                            redirectCount++
                        } else {
                            // 沒有重定向，使用當前 URL
                            break
                        }
                    } finally {
                        connection.disconnect()
                    }
                }

                Log.i("Pokemod", "最終 URL: $currentUrl")

                // 從 URL 中提取版本號 - 支援多種格式
                // 格式1: PokemodPublic-v11.1.2r1112 (新格式)
                // 格式2: Pokemod_Public_v1_2_3r (舊格式)
                val newFormatRegex = Regex("""PokemodPublic-v(\d+)\.(\d+)\.(\d+)r""", RegexOption.IGNORE_CASE)
                val oldFormatRegex = Regex("""Pokemod_Public_v(\d+)_(\d+)_(\d+)r""", RegexOption.IGNORE_CASE)

                val matchResult = newFormatRegex.find(currentUrl) ?: oldFormatRegex.find(currentUrl)

                if (matchResult != null) {
                    val (major, minor, patch) = matchResult.destructured
                    val version = "$major.$minor.$patch"
                    Log.i("Pokemod", "提取版本號: $version")
                    version
                } else {
                    Log.w("Pokemod", "無法從 URL 提取版本號: $currentUrl")
                    "ERROR"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Pokemod", "getPokemodVersion 錯誤", e)
                "ERROR"
            }
        }
    }
}
