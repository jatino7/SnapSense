package com.o7solutions.snapsense.Utils

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.o7solutions.snapsense.R
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object AppFunctions {



    fun generateImageFromText(prompt: String, apiKey: String, callback: (String?) -> Unit) {
        // Add logging interceptor
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val url = "https://api.deepai.org/api/text2img"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("text", prompt)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Api-Key", apiKey) // FIX: Use "Api-Key" (capital A)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ImageGen", "Request failed", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyString = response.body?.string()
                    if (!response.isSuccessful) {
                        callback(null)
                    } else {
                        val json = JSONObject(bodyString ?: "")
                        val outputUrl = json.optString("output_url", null)
                        Log.d("ImageGen", "Output URL: $outputUrl")
                        callback(outputUrl)
                    }
                }
            }
        })
    }


    fun generateImageFromCloudflare(
        accountId: String,
        apiToken: String,
        prompt: String,
        callback: (String?) -> Unit
    ) {
        val client = OkHttpClient()

        // API endpoint for Cloudflare Workers AI (using Leonardo model as example)
        val url = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/@cf/leonardo/phoenix-1.0"

        // JSON request body
        val json = JSONObject().apply {
            put("prompt", prompt)
            put("width", 512)       // optional
            put("height", 512)      // optional
            put("steps", 25)        // optional
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        // Build the request
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // Async execution
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("CloudflareAI", "Error: ${response.code}")
                        callback(null)
                    } else {
                        val responseBody = response.body?.string()
                        Log.d("CloudflareAI", "Response: $responseBody")

                        // Parse result
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val result = jsonResponse.optJSONObject("result")
                        val imageBase64 = result?.optString("image_base64", null)

                        // Return base64 image string
                        callback(imageBase64)
                    }
                }
            }
        })
    }

    fun generateImageFromHuggingFace(prompt: String, apiToken: String): ByteArray {
        val apiUrl = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-2"
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $apiToken")
        conn.setRequestProperty("Content-Type", "application/json")

        val jsonInput = """{"inputs": "$prompt", "options": {"wait_for_model": true}}"""
        conn.outputStream.use { os ->
            os.write(jsonInput.toByteArray(Charsets.UTF_8))
            Log.d("Successful","Successful")
        }
        if (conn.responseCode != 200) {
            throw Exception("API call failed: ${conn.responseMessage}")
        }
        return conn.inputStream.readBytes()
    }



}