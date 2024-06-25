package app.lokey0905.location.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class polygon {
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private var context: Context? = null
    private var client: OkHttpClient? = null

    fun Polygon(context: Context?) {
        this.context = context
        this.client = OkHttpClient()
    }

    fun checkPogoVersion(
        key: String,
        versionNumber: Int,
        callback: (String) -> Unit
    ) {
        val url = "https://api.login.polygonsharp.io/Porygon/Login"

        val jsonObject = JSONObject()
        try {
            jsonObject.put("key", key)
            jsonObject.put("version", versionNumber)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return
        }

        val body: RequestBody = jsonObject.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client?.newCall(request)?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("checkPogoVersion", "請求失敗")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "請求失敗", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.i("checkPogoVersion", "Unexpected code $response")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "請求失敗 $response", Toast.LENGTH_SHORT).show()
                    }
                    return
                }


                val responseData = response.body!!.string()
                try {
                    val jsonResponse = JSONObject(responseData)
                    when (val status = jsonResponse.getString("status")) {
                        "SUCCESS" -> {
                            val token = jsonResponse.getString("token")
                            Log.i("checkPogoVersion", "Login successful, token: $token")
                            callback(token)
                        }

                        "ERROR_KEY_NOT_VALID" -> {
                            Log.i("checkPogoVersion", "Key not valid")
                            callback("ERROR")
                        }

                        else -> {
                            Log.i("checkPogoVersion", "Unexpected response: $status")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "登入檢查失敗 狀態: $status",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Log.i("checkPogoVersion", "Response parsing failed")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "回應解析失敗", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    fun sendSecondJsonRequest(
        key: String,
        token: String,
        pogoVersion: String,
        versionNumber: Int,
        callback: (String) -> Unit
    ) {
        val url = "https://api.login.polygonsharp.io/Porygon/Start"
        val jsonObject = JSONObject()
        try {
            jsonObject.put("key", key)
            jsonObject.put("token", token)
            jsonObject.put("pogo", pogoVersion)
            jsonObject.put("scriptVersion", 1)
            jsonObject.put("treecko", true)
            jsonObject.put("mudkip", true)
            jsonObject.put("torchic", true)
            jsonObject.put("offline", true)
            jsonObject.put("customServer", false)
            jsonObject.put("mode", "ENHANCER")
            jsonObject.put("versionNumber", versionNumber)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val body: RequestBody = jsonObject.toString().toRequestBody(JSON)

        val request: Request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client?.newCall(request)?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.i("sendSecondJsonRequest", "Request failed")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "請求失敗", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.i("sendSecondJsonRequest", "Unexpected code $response")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "請求失敗 $response", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val responseData = response.body!!.string()
                try {
                    val jsonResponse = JSONObject(responseData)
                    when (val status = jsonResponse.getString("status")) {
                        "SUCCESS" -> {
                            Log.i("sendSecondJsonRequest", "Pogo version $pogoVersion supported")
                            callback(pogoVersion)
                        }

                        "ERROR_POGO_VERSION_NOT_SUPPORTED" -> {
                            Log.i(
                                "sendSecondJsonRequest",
                                "Pogo version $pogoVersion not supported"
                            )
                        }

                        "ERROR_KEY_NOT_VALID" -> {
                            Log.i("sendSecondJsonRequest", "Key not valid")
                            callback("ERROR")
                        }

                        else -> {
                            Log.i("sendSecondJsonRequest", "Unexpected response: $status")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "登入檢查失敗 狀態: $status",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.i("sendSecondJsonRequest", "Response parsing failed")
                    /*Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "回應解析失敗", Toast.LENGTH_SHORT).show()
                    }*/
                }
            }
        })
    }
}