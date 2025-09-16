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

    fun generateImageFromPrompt(prompt: String, callback: (ByteArray?) -> Unit) {
        // Prepare JSON body
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
            .url("$baseUrl/gemini-2.0-flash:generateContent?key=$apiKey") // Use your Gemini endpoint
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string() ?: ""
                    try {
                        val json = JSONObject(responseBody)
                        val imageBase64 = json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("image_data") // Gemini returns image data in base64

                        val imageBytes = imageBase64?.let { data ->
                            android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                        }

                        callback(imageBytes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(null)
                    }
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

    fun generateContentFromPrompt(
        prompt: String,
        imageFile: File,
        callback: (String?, ByteArray?) -> Unit
    ) {
        val base64Image = encodeImageToBase64(imageFile)

        // Build JSON body with text + input image
        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        // text input
                        put(JSONObject().put("text", prompt))
                        // image input
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/png") // adjust if jpeg
                                put("data", base64Image)
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
                e.printStackTrace()
                callback(null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string() ?: ""
                    try {
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val parts = firstCandidate
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")

                        var textResponse: String? = null
                        var imageResponse: ByteArray? = null

                        parts?.let {
                            for (i in 0 until it.length()) {
                                val part = it.optJSONObject(i)

                                // Text output
                                part?.optString("text")?.let { txt ->
                                    textResponse = txt
                                }

                                // Image output
                                part?.optJSONObject("inline_data")?.let { inline ->
                                    val data = inline.optString("data")
                                    if (!data.isNullOrEmpty()) {
                                        imageResponse = android.util.Base64.decode(
                                            data,
                                            android.util.Base64.DEFAULT
                                        )
                                    }
                                }
                            }
                        }

                        callback(textResponse, imageResponse)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(null, null)
                    }
                }
            }
        })
    }



}
