package app.lokey0905.location.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import app.lokey0905.location.fragment.PogoVersionInfo
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
    val JSON = "application/json; charset=utf-8".toMediaType()
    private var context: Context? = null
    private var client: OkHttpClient? = null

    fun Polygon(context: Context?) {
        this.context = context
        this.client = OkHttpClient()
    }

    fun checkPogoVersion(key: String, pogoVersion: ArrayList<PogoVersionInfo>, versionNumber: Int, callback: (ArrayList<String>) -> Unit) {
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
                Log.i("checkPogoVersion", "Request failed")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.i("checkPogoVersion", "Unexpected code $response")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "Unexpected code $response",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                val responseData = response.body!!.string()
                try {
                    val jsonResponse = JSONObject(responseData)
                    val status = jsonResponse.getString("status")

                    if ("SUCCESS" == status) {
                        val token = jsonResponse.getString("token")
                        val pogoVersionList = ArrayList<String>()
                        Log.i("checkPogoVersion", "Login successful, token: $token")

                        for (pogo in pogoVersion) {
                            sendSecondJsonRequest(key, token, pogo.pogoVersion, versionNumber) { it ->
                                pogoVersionList += it
                                Log.i("checkPogoVersion", pogoVersionList.toString())
                                callback(pogoVersionList)
                            }
                        }

                    } else {
                        Log.i("checkPogoVersion", "Unexpected response: $status")
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Log.i("checkPogoVersion", "Response parsing failed")
                }
            }
        })
    }

    fun sendSecondJsonRequest(key: String, token: String, pogoVersion: String, versionNumber: Int, callback: (String) -> Unit) {
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

        val body: RequestBody = RequestBody.create(JSON, jsonObject.toString())

        val request: Request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client?.newCall(request)?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.i("sendSecondJsonRequest", "Request failed")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.i("sendSecondJsonRequest", "Unexpected code $response")
                    return
                }

                val responseData = response.body!!.string()
                try {
                    val jsonResponse = JSONObject(responseData)
                    val status = jsonResponse.getString("status")

                    if ("SUCCESS" == status) {
                        Log.i("checkPogoVersion", "Pogo version supported")
                        callback(pogoVersion)
                    } else {
                        Log.i("checkPogoVersion", "Pogo version not supported")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.i("sendSecondJsonRequest", "Response parsing failed")
                }
            }
        })
    }


}