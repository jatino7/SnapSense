package com.o7solutions.snapsense.Utils

import java.net.HttpURLConnection
import android.util.Base64
import java.io.File

import java.net.URL


data class ImageResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val errorMessage: String? = null
)
class TextToImageGenerator {

    // OpenAI DALL-E API
    fun generateImageWithOpenAI(prompt: String, apiKey: String): ImageResult {
        return try {
            val url = URL("https://api.openai.com/v1/images/generations")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonPayload = """
                {
                    "prompt": "$prompt",
                    "n": 1,
                    "size": "1024x1024"
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                val input = jsonPayload.toByteArray()
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val imageUrl = extractImageUrl(response)
                ImageResult(success = true, imageUrl = imageUrl)
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                ImageResult(success = false, errorMessage = "HTTP $responseCode: $errorResponse")
            }

        } catch (e: Exception) {
            ImageResult(success = false, errorMessage = "Error: ${e.message}")
        }
    }

    // Hugging Face API (free alternative)
    fun generateImageWithHuggingFace(prompt: String, apiKey: String): ImageResult {
        return try {
            val url = URL("https://api-inference.huggingface.co/models/runwayml/stable-diffusion-v1-5")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonPayload = """{"inputs": "$prompt"}"""

            connection.outputStream.use { os ->
                val input = jsonPayload.toByteArray()
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode

            if (responseCode == 200) {
                // Hugging Face returns image binary data
                val imageData = connection.inputStream.readBytes()
                val base64Image = Base64.encodeToString(imageData, Base64.DEFAULT)
                ImageResult(success = true, imageUrl = "data:image/png;base64,$base64Image")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                ImageResult(success = false, errorMessage = "HTTP $responseCode: $errorResponse")
            }

        } catch (e: Exception) {
            ImageResult(success = false, errorMessage = "Error: ${e.message}")
        }
    }

    // Stability AI API
    fun generateImageWithStabilityAI(prompt: String, apiKey: String): ImageResult {
        return try {
            val url = URL("https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            // Create form data
            val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val formData = buildString {
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"text_prompts[0][text]\"\r\n\r\n")
                append("$prompt\r\n")
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"cfg_scale\"\r\n\r\n")
                append("7\r\n")
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"height\"\r\n\r\n")
                append("1024\r\n")
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"width\"\r\n\r\n")
                append("1024\r\n")
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"samples\"\r\n\r\n")
                append("1\r\n")
                append("--$boundary--\r\n")
            }

            connection.outputStream.use { os ->
                os.write(formData.toByteArray())
            }

            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val base64Image = extractBase64Image(response)
                ImageResult(success = true, imageUrl = "data:image/png;base64,$base64Image")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                ImageResult(success = false, errorMessage = "HTTP $responseCode: $errorResponse")
            }

        } catch (e: Exception) {
            ImageResult(success = false, errorMessage = "Error: ${e.message}")
        }
    }

    // Generic function to generate image with different providers
    fun generateImage(prompt: String, provider: String, apiKey: String): ImageResult {
        return when (provider.lowercase()) {
            "openai" -> generateImageWithOpenAI(prompt, apiKey)
            "huggingface" -> generateImageWithHuggingFace(prompt, apiKey)
            "stability" -> generateImageWithStabilityAI(prompt, apiKey)
            else -> ImageResult(success = false, errorMessage = "Unsupported provider: $provider")
        }
    }

    // Helper function to extract image URL from OpenAI response
    private fun extractImageUrl(jsonResponse: String): String? {
        val urlPattern = """"url":\s*"([^"]+)"""".toRegex()
        return urlPattern.find(jsonResponse)?.groupValues?.get(1)
    }

    // Helper function to extract base64 image from Stability AI response
    private fun extractBase64Image(jsonResponse: String): String? {
        val base64Pattern = """"base64":\s*"([^"]+)"""".toRegex()
        return base64Pattern.find(jsonResponse)?.groupValues?.get(1)
    }

    // Save image from URL to local file
    fun saveImageFromUrl(imageUrl: String, filename: String): Boolean {
        return try {
            if (imageUrl.startsWith("data:image")) {
                // Handle base64 data URL
                val base64Data = imageUrl.substringAfter("base64,")
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                File(filename).writeBytes(imageBytes)
            } else {
                // Handle regular URL
                val url = URL(imageUrl)
                url.openStream().use { input ->
                    File(filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            true
        } catch (e: Exception) {
            println("Error saving image: ${e.message}")
            false
        }
    }
}

// Simple utility functions
fun generateImageSimple(prompt: String, apiKey: String): String? {
    val generator = TextToImageGenerator()
    val result = generator.generateImageWithHuggingFace(prompt, apiKey)
    return if (result.success) result.imageUrl else null
}

fun generateAndSaveImage(prompt: String, apiKey: String, filename: String): Boolean {
    val generator = TextToImageGenerator()
    val result = generator.generateImageWithHuggingFace(prompt, apiKey)

    return if (result.success && result.imageUrl != null) {
        generator.saveImageFromUrl(result.imageUrl, filename)
    } else {
        println("Failed to generate image: ${result.errorMessage}")
        false
    }
}