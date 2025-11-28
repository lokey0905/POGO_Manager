package app.lokey0905.location.api

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class ApkPureVersion(
    val versionCode: String,
    val versionName: String
)

class ApkPure {
    fun getPokemonGoVersions(): List<ApkPureVersion> {
        return getVersionHistory("com.nianticlabs.pokemongo")
    }

    fun getVersionHistory(packageName: String): List<ApkPureVersion> {
        val versionList = mutableListOf<ApkPureVersion>()

        try {
            // 使用 GET 方法，將參數放在 URL 中
            val url = URL("https://tapi.pureapk.com/v3/get_app_his_version?package_name=$packageName&hl=tw")
            val connection = url.openConnection() as HttpURLConnection

            // 設置連接屬性
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent-WebView", "Mozilla/5.0 (Linux; Android 13; M2101K9G; zh-TW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36 Mobile Safari/537.36")
            connection.setRequestProperty("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 13; M2101K9G Build/TKQ1.220829.002); APKPure/3.20.51 (Aegon)")
            connection.setRequestProperty("Accept-Encoding", "gzip")
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Host", "tapi.pureapk.com")

            // 設置 APKPure 專用的頭信息
            connection.setRequestProperty("Ual-Access-Businessid", "projecta")

            // 構建設備和應用信息
            val deviceInfo = JSONObject().apply {
                put("abis", listOf("arm64-v8a", "armeabi-v7a", "armeabi"))
                put("os_ver", "33")
                put("os_ver_name", "13")
            }


            val projectAData = JSONObject().apply {
                put("device_info", deviceInfo)
            }

            connection.setRequestProperty("Ual-Access-ProjectA", projectAData.toString())
            connection.setRequestProperty("Ual-Access-Sequence", "b51b1e51-7b15-4ac3-800c-fc2a4c00ae2f")
            connection.setRequestProperty("Ual-Access-Nonce", "70738428")
            connection.setRequestProperty("Ual-Access-Timestamp", System.currentTimeMillis().toString())
            connection.setRequestProperty("Ual-Access-Signature", "")

            // 設置 Cookie
            connection.setRequestProperty("Cookie", "_usi=s%3A8e41ebcb89352589ed568c9c33aa2b35b1de243ba522234a1483b48c357c7cf3.7fEqtMuw%2FhO%2BZPCZa6IQLakiUNIueyrBdyhUkuLPn4U; expires=Mon, 08 Sep 2025 08:07:50 GMT; domain=pureapk.com; path=/; httponly")

            connection.connectTimeout = 15000
            connection.readTimeout = 20000

            Log.d("ApkPure", "請求 URL: $url")

            val responseCode = connection.responseCode
            Log.d("ApkPure", "回應代碼: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = if (connection.contentEncoding == "gzip") {
                    java.util.zip.GZIPInputStream(connection.inputStream)
                } else {
                    connection.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val response = reader.readText()
                reader.close()

                //Log.d("ApkPure", "API 回應: $response")

                val jsonObject = JSONObject(response)

                if (jsonObject.has("retcode") && jsonObject.getInt("retcode") == 0) {
                    if (jsonObject.has("version_list")) {
                        val versions = jsonObject.getJSONArray("version_list")
                        val seenVersions = mutableSetOf<String>()
                        //Log.d("ApkPure", "找到 ${versions.length()} 個版本")

                        for (i in 0 until versions.length()) {
                            val version = versions.getJSONObject(i)
                            val versionCode = version.getString("version_code")
                            val versionName = version.getString("version_name")
                            val versionKey = "$versionCode|$versionName"

                            if (seenVersions.add(versionKey)) {
                                versionList.add(ApkPureVersion(versionCode, versionName))
                            }
                            //Log.d("ApkPure", "versionCode: $versionName, versionCode: $versionCode")
                        }
                    } else {
                        Log.w("ApkPure", "回應中沒有 version_list 欄位")
                    }
                } else {
                    val retcode = if (jsonObject.has("retcode")) jsonObject.getInt("retcode") else -1
                    val message = if (jsonObject.has("message")) jsonObject.getString("message") else "未知錯誤"
                    Log.e("ApkPure", "API 錯誤 - retcode: $retcode, message: $message")
                }
            } else {
                // 讀取錯誤回應
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorReader = BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8))
                    val errorResponse = errorReader.readText()
                    errorReader.close()
                    Log.e("ApkPure", "HTTP 錯誤 $responseCode: $errorResponse")
                } else {
                    Log.e("ApkPure", "HTTP 錯誤 $responseCode: ${connection.responseMessage}")
                }
            }
        } catch (e: Exception) {
            Log.e("ApkPure", "獲取版本歷史失敗", e)
            e.printStackTrace()
        }

        return versionList
    }

    // 過濾出可登入的版本（大於等於最低版本）
    fun filterValidVersions(allVersions: List<ApkPureVersion>, minVersionName: String): List<ApkPureVersion> {
        return allVersions.filter { version ->
            compareVersions(version.versionName, minVersionName) >= 0
        }
    }

    // 比較版本號
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val v1Part = if (i < v1Parts.size) v1Parts[i] else 0
            val v2Part = if (i < v2Parts.size) v2Parts[i] else 0

            when {
                v1Part > v2Part -> return 1
                v1Part < v2Part -> return -1
            }
        }
        return 0
    }
}
