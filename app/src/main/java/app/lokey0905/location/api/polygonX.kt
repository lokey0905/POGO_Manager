package app.lokey0905.location.api

import tessellate.PolygonXHello
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

data class PolygonXCheckResult(
    val status: Status,
    val latestVersionCode: String? = null
) {
    enum class Status {
        SUCCESS,
        UPDATE_REQUIRED,
        FAILURE
    }
}

class polygonX {
    suspend fun checkPolygonXUpdate(
        polygonXVersionNumber: Int
    ): PolygonXCheckResult = withContext(Dispatchers.IO) {
        try {
            Log.i("PolygonX", "Checking PolygonX version $polygonXVersionNumber")

            val request = PolygonXHello.PolygonXHelloAPIRequest.newBuilder()
                .setVersionNumber(polygonXVersionNumber)
                .setLocalizationLocale("en_US")
                .setLocalizationTimestamp(System.currentTimeMillis() / 1000L)
                .build()

            val url = URL("https://v1.api.polygonx.evermorelabs.io/PolygonX/Hello")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-protobuf")
            conn.setRequestProperty("Accept", "application/x-protobuf")
            conn.doOutput = true

            val outputStream: OutputStream = conn.outputStream
            request.writeTo(outputStream)
            outputStream.close()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseBin = conn.inputStream.readBytes()
                conn.inputStream.close()
                try {
                    val resp = PolygonXHello.PolygonXHelloAPIResponse.parseFrom(responseBin)
                    Log.d("PolygonX", "Response for version $polygonXVersionNumber: $resp")
                    Log.d("PolygonX", "Latest Gamemaster Available: ${resp.latestGamemasterAvailable}")
                    Log.d("PolygonX", "Forced Version Number: ${resp.forcedVersionNumber}")

                    return@withContext if (resp.forcedVersionNumber > polygonXVersionNumber) {
                        PolygonXCheckResult(
                            PolygonXCheckResult.Status.UPDATE_REQUIRED,
                            resp.latestVersionCode
                        )
                    } else if (resp.latestGamemasterAvailable > 0) {
                        PolygonXCheckResult(
                            PolygonXCheckResult.Status.SUCCESS,
                            resp.latestVersionCode
                        )
                    } else {
                        PolygonXCheckResult(
                            PolygonXCheckResult.Status.FAILURE,
                            resp.latestVersionCode
                        )
                    }
                } catch (e: com.google.protobuf.InvalidProtocolBufferException) {
                    val responseAsString = String(responseBin, Charsets.UTF_8)
                    Log.e("PolygonX", "Failed to parse protobuf, server response: $responseAsString", e)
                    return@withContext PolygonXCheckResult(PolygonXCheckResult.Status.FAILURE)
                }
            } else {
                Log.e("PolygonX", "Error for version $polygonXVersionNumber: ${conn.responseCode} ${conn.responseMessage}")
                return@withContext PolygonXCheckResult(PolygonXCheckResult.Status.FAILURE)
            }
        } catch (e: Exception) {
            Log.e("PolygonX", "Exception in checkPolygonXUpdate for version $polygonXVersionNumber", e)
            return@withContext PolygonXCheckResult(PolygonXCheckResult.Status.FAILURE)
        }
    }
}