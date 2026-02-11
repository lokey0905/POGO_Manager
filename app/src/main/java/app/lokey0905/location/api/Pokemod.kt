package app.lokey0905.location.api

import android.util.Log
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
                    "未知版本"
                }
            } catch (e: Exception) {
                Log.e("Pokemod", "getPokemodVersion 錯誤", e)
                e.printStackTrace()
                "ERROR"
            }
        }
    }
}