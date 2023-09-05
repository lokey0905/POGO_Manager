package app.lokey0905.location.api

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class DiscordApi(private val token: String) {
    private val client = OkHttpClient()
    private var username: String? = null
    private var id: String? = null
    private var email: String? = null
    private var phone: String? = null

    init {
        validate()
    }

    private fun validate() {
        val url = "https://discord.com/api/users/@me"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to validate token: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = JSONObject(response.body?.string()!!)
                    username =
                        responseData.getString("username") + "#" + responseData.getString("discriminator")
                    id = responseData.getString("id")
                    email = responseData.optString("email")
                    phone = responseData.optString("phone")
                    println("Valid token: $token")
                } else {
                    println("Invalid token: $token")
                }
            }
        })
    }

    fun send_message(msg: String, channel_id: String) {
        val url = "https://discord.com/api/channels/$channel_id/messages"
        val json = JSONObject()
        json.put("content", msg)

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )
        val request = Request.Builder()
            .url(url)
            .header("Authorization", token)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to send message: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("$username | Message sent to $channel_id")
                } else {
                    println("$username | Error ${response.code}: $channel_id")
                }
            }
        })
    }

    fun get_messages(channel_id: String, page: Int = 0, callback: (List<String>) -> Unit) {
        val offset = 25 * page
        val url = "https://discord.com/api/channels/$channel_id/messages/search?offset=$offset"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println(responseBody)
                    try {
                        val responseObject = JSONObject(responseBody)
                        val messagesArrayArray = responseObject.getJSONArray("messages")

                        val messages = mutableListOf<String>()
                        for (i in 0 until messagesArrayArray.length()) {
                            val messagesArray = messagesArrayArray.getJSONArray(i)
                            for (j in 0 until messagesArray.length()) {
                                val messageObject = messagesArray.getJSONObject(j)
                                val content = messageObject.optString("content")
                                messages.add(content)
                            }
                        }
                        callback(messages)
                    } catch (e: JSONException) {
                        callback(emptyList())
                    }
                } else {
                    callback(emptyList())
                }
            }
        })
    }
}