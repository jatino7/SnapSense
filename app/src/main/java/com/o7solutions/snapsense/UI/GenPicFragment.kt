package com.o7solutions.snapsense.UI

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.o7solutions.snapsense.R
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.AppFunctions
import com.o7solutions.snapsense.Utils.AppFunctions.generateImageFromCloudflare
import com.o7solutions.snapsense.Utils.AppFunctions.generateImageFromHuggingFace
import com.o7solutions.snapsense.Utils.GeminiApi
import com.o7solutions.snapsense.Utils.TextToImageGenerator
import com.o7solutions.snapsense.Utils.generateAndSaveImage
import com.o7solutions.snapsense.Utils.generateImageSimple
import com.o7solutions.snapsense.databinding.FragmentGenPicBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GenPicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GenPicFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentGenPicBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentGenPicBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }


//        AppFunctions.generateImageFromText("Sky image", AppConstants.deepSeekApiKey) { result ->
//            requireActivity().runOnUiThread {
//                if (result != null) {
//                    Log.d("Image", "Generated URL = $result")
//                    Glide.with(requireContext())
//                        .load(result)
//                        .into(binding.imageView)
//                } else {
//                    Log.e("Image", "Failed to generate image")
//                }
//            }
//        }

//        generateImageFromCloudflare(
//            accountId = AppConstants.cfAccountId,
//            apiToken = AppConstants.cfToken,
//            prompt = "A cyberpunk city at night"
//        ) { base64Image ->
//            if (base64Image != null) {
//                Log.d("CloudflareAI", "Got image in Base64 format")
//                // You can decode base64 -> Bitmap and show in ImageView
//            } else {
//                Log.e("CloudflareAI", "Failed to generate image")
//            }
//        }

//       generateImageFromPromptHF(
//            "A cyberpunk city at night",
//            AppConstants.huggingFaceAPI
//        )


//
//        CoroutineScope(Dispatchers.Main).launch {
//            val imageBytes = withContext(Dispatchers.IO) {
//                generateImageFromHuggingFace("Hand", AppConstants.huggingFaceAPI)
//            }
//            Log.d("Image Bytes",imageBytes.toString())
//            // Now you can handle imageBytes and update UI safely here
//        }

//        claude()

        binding.btnCapture.setOnClickListener {
            binding.btnCapture.isEnabled = false
            captureAndUpload()
            binding.previewView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.loader.visibility = View.VISIBLE
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureAndUpload() {
        val file = File(requireContext().externalCacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                    Log.d("CameraX", "Photo capture succeeded: ${file.absolutePath}")
                    Glide.with(requireActivity())
                        .load(file)
                        .into(binding.imageView)
                    analyzeWithGemini(file)


                }
            }
        )
    }

    fun formatText(input: String): String {
        return input
            // Replace double asterisks first
            .replace(Regex("\\*\\*"), " ")
            // Then replace single asterisk
            .replace(Regex("\\*"), "•")
    }


    private fun analyzeWithGemini(file: File) {
        val gemini = GeminiApi(AppConstants.apiKey)
        gemini.generateContentFromPrompt(AppConstants.prompt, file) { result, img ->
            requireActivity().runOnUiThread {
                Log.d("ApiResult", result.toString())
                Log.d("Byte Array", "Size = ${img?.size}")
                binding.loader.visibility = View.GONE
                img?.let { showResultDialog(requireActivity(), it, formatText(result.toString())) }

//                showResultBottomSheet(cleanResponse(result))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment GenPicFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            GenPicFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


    fun showResultDialog(context: Context, img: ByteArray, message: String = "Default message") {
        val dialog = Dialog(context)

        // Remove default styling
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set layout
        val view = LayoutInflater.from(context).inflate(R.layout.result_dialog, null)
        dialog.setContentView(view)

        // Configure content

        val imgView = view.findViewById<ImageView>(R.id.dialog_image)

        val bitmap = BitmapFactory.decodeByteArray(img, 0, img.size)
        imgView.setImageBitmap(bitmap)
//        Glide.with(context)
//            .load(ByteArrayInputStream(img))
//            .into(imgView)

        view.findViewById<TextView>(R.id.dialog_text).text = message
        view.findViewById<Button>(R.id.ok_button).setOnClickListener {
            binding.btnCapture.isEnabled = true
            binding.imageView.visibility = View.GONE
            binding.previewView.visibility = View.VISIBLE
            binding.imageView.setImageDrawable(null)
            dialog.dismiss()
        }

        // Show dialog
        dialog.setCancelable(false)
        dialog.show()
    }

    fun generateImageFromPromptHF(
        prompt: String,
        apiKey: String
    ) {
        val url = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-2"

        val json = """
        {
            "inputs": "$prompt"
        }
    """.trimIndent()

        val client = OkHttpClient()
        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(), json
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.d("HF API issue", "HF API Error: ${response.message}")
                    return
                }

                val bytes = response.body?.bytes()
                if (bytes != null) {
                    // Convert bytes to Base64 string for Glide
                    val imageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                    val imageDataUrl = "data:image/png;base64,$imageBase64"
                    Toast.makeText(
                        requireActivity(),
                        "Image generated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Load image into ImageView on main thread
//                    imageView.post {
//                        Glide.with(imageView.context)
//                            .load(imageDataUrl)
//                            .into(imageView)
//                    }
                }
            }
        })
    }

    fun claude() {
        val generator = TextToImageGenerator()

        // Example 1: Generate with OpenAI
        val openAIResult = generator.generateImage(
            prompt = "A beautiful landscape with mountains and lakes",
            provider = "openai",
            apiKey = "your-openai-api-key-here"
        )

        if (openAIResult.success) {
            println("OpenAI Image generated: ${openAIResult.imageUrl}")
        } else {
            println("OpenAI Error: ${openAIResult.errorMessage}")
        }

        // Example 2: Generate with Hugging Face (free)
        val hfResult = generator.generateImage(
            prompt = "A cute robot painting a picture",
            provider = "huggingface",
            apiKey = AppConstants.huggingFaceAPI
        )

        if (hfResult.success) {
            println("Hugging Face Image generated successfully")
            // Save the image
            generator.saveImageFromUrl(hfResult.imageUrl!!, "robot_painting.png")
        } else {
            println("Hugging Face Error: ${hfResult.errorMessage}")
        }

        // Example 3: Simple utility function
        val imageUrl = generateImageSimple("A sunset over the ocean", "your-api-key")
        println("Simple generation result: $imageUrl")

        // Example 4: Generate and save directly
        val saved = generateAndSaveImage(
            "A futuristic city at night",
            "your-api-key",
            "futuristic_city.png"
        )
        println("Image saved: $saved")
    }


//    private fun generateImage(prompt: String) {
//        lifecycleScope.launch {
//            val generatedBitmap = withContext(Dispatchers.IO) {
//                val config = GenerationConfig {
//                    responseModalities = listOf(ResponseModality.IMAGE)
//                    numberOfImages = 1
//                }
//
//                val model: ImagenModel = FirebaseAI
//                    .ai(backend = GenerativeBackend.googleAI())
//                    .imagenModel(
//                        modelName = "imagen-3.0-generate-002",
//                        generationConfig = config
//                    )
//
//                val response = model.generateContent(prompt)
//                val imagePart = response.candidates.first().content.parts
//                    .filterIsInstance<com.google.firebase.ml.custom.generation.ImagePart>()
//                    .firstOrNull()
//
//                imagePart?.image
//            }
//
//            generatedBitmap?.let {
//                resultImage.setImageBitmap(it)
//            }
//        }
//    }
}