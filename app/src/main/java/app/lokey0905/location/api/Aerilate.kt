package app.lokey0905.location.api

import AerilateHello
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class Aerilate {
    suspend fun checkAerilate(
        aerilateVersion: String,
        aerilateVersionNumber: Long,
        pogoVersion: String,
        pogoVersionNumber: Long
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                var setAerilateVersion = aerilateVersion
                var setAerilateVersionNumber = aerilateVersionNumber
                if (aerilateVersion=="未安裝" || aerilateVersion.isEmpty() || aerilateVersionNumber == 0L) {
                    setAerilateVersion = "0.2.1"
                    setAerilateVersionNumber = 2025051301L
                    Log.i("Aerilate", "Aerilate not found, using default settings")
                }
                Log.i("Aerilate", "Aerilate version: $setAerilateVersion\n" +
                        "Aerilate version number: $setAerilateVersionNumber\n" +
                        "Pogo version: $pogoVersion\n" +
                        "Pogo version number: $pogoVersionNumber")

                val request = AerilateHello.HelloAPIRequest.newBuilder()
                    .setAerilateVersion(setAerilateVersion)
                    .setAerilateVersionNumber(setAerilateVersionNumber)
                    .setPogoVersion(pogoVersion)
                    .setPogoVersionNumber(pogoVersionNumber)
                    .setDebug(false)
                    .setLocalizationLocale("en_US")
                    .setLocalizationTimestamp(System.currentTimeMillis() / 1000L)
                    .setGamemasterBatchId(0L)
                    .build()

                val postData: ByteArray = request.toByteArray()

                val url = URL(String.format("https://api.aerilate.io/Aerilate/Hello", pogoVersion))
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-protobuf")
                conn.setRequestProperty("Content-Length", postData.size.toString())

                val os: OutputStream = conn.outputStream
                os.write(postData)
                os.flush()
                os.close()

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBin = conn.inputStream.readBytes()
                    conn.inputStream.close()
                    val resp = AerilateHello.HelloAPIResponse.parseFrom(responseBin)
                    resp.hasScriptUpdate()
                } else {
                    Log.e("Aerilate", "Error response code: ${conn.responseCode}")
                    false

                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Aerilate", "Exception occurred: ${e.message}")
                false
            }
        }
    }

}