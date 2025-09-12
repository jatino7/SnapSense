package com.o7solutions.snapsense.Utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApi(private val apiKey: String) {

    // Add logging interceptor
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Build OkHttpClient with interceptor and increased timeout
    private val client = OkHttpClient.Builder()
//        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    fun encodeImageToBase64(imageFile: File): String {
        val bytes = imageFile.readBytes()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    fun analyzeImage(imageFile: File, prompt: String, callback: (String) -> Unit) {
        val imageBase64 = encodeImageToBase64(imageFile)

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", imageBase64)
                            })
                        })
                    })
                })
            })
        }

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            requestBodyJson.toString()
        )

        val request = Request.Builder()
            .url("$baseUrl/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    val output = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: "No response"

                    callback(output)
                }
            }
        })
    }

    fun analyzeText(prompt: String, callback: (String) -> Unit) {

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))

                    })
                })
            })
        }

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            requestBodyJson.toString()
        )

        val request = Request.Builder()
            .url("$baseUrl/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    val output = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: "No response"

                    callback(output)
                }
            }
        })
    }


    fun analyzeImageFromUrl(imageUrl: String, prompt: String, callback: (String) -> Unit) {
        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        // Text prompt
                        put(JSONObject().put("text", prompt))
                        // Image URL
                        put(JSONObject().apply {
                            put("image", JSONObject().apply {
                                put("url", imageUrl)
                            })
                        })
                    })
                })
            })
        }

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            requestBodyJson.toString()
        )

        val request = Request.Builder()
            .url("$baseUrl/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    val output = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: "No response"

                    callback(output)
                }
            }
        })
    }
}
