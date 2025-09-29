package com.o7solutions.snapsenseV2.UI

import android.os.Bundle
import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.o7solutions.snapsenseV2.R
import com.o7solutions.snapsenseV2.Utils.AppConstants
import com.o7solutions.snapsenseV2.Utils.AppFunctions
import com.o7solutions.snapsenseV2.Utils.BeautifulMessageDialog
import com.o7solutions.snapsenseV2.Utils.GeminiApi
import com.o7solutions.snapsenseV2.databinding.FragmentCameraBinding
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.activity.result.contract.ActivityResultContracts

class CameraFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var binding: FragmentCameraBinding
    private lateinit var imageFile: File
    var keyword = ""

    // modern permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textToSpeech = TextToSpeech(requireContext(), this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // check permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnCapture.setOnClickListener {
            binding.btnCapture.isEnabled = false
            captureAndUpload()
            binding.previewView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.loader.visibility = View.VISIBLE
        }
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
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
                    Glide.with(requireActivity())
                        .load(file)
                        .into(binding.imageView)
                    analyzeWithGemini(file)
                    imageFile = file
                }
            }
        )
    }

    private fun analyzeWithGemini(file: File) {
        binding.loader.visibility = View.VISIBLE

        val gemini = GeminiApi(AppFunctions.readApiKey(requireActivity()).toString())
        gemini.analyzeImageGetKeyWord(
            file, "Analyze image and give me product name in single word with its" +
                    " company or if you are not able to detect the product company than suggest other company" +
                    " i want only single word answer, For example if you see main product in image as Iphone than" +
                    " give response iphone"+"You have to focus on main utensil or product in the image" +
                    ""
        ) { result ->

            keyword = result
            gemini.analyzeImage(file, AppConstants.prompt) { result ->
                requireActivity().runOnUiThread {
                    Log.d("ApiResult", result)

                    val uri = Uri.fromFile(file)
                    val bundle = Bundle().apply {
                        putParcelable("imageUri", uri)
                        putString("title", formatText(result))
                        putString("ques", "Image insights")
                        putString("keyword",keyword)
                    }

                    findNavController().navigate(R.id.resultFragment, bundle)
                }
            }
        }
    }

    fun formatText(input: String): String {
        return input.replace(Regex("\\*\\*"), " ")
            .replace(Regex("\\*"), "•")
    }

    private fun showZoomDialog(imageFile: File) {
        val zoomView = layoutInflater.inflate(R.layout.zoom_layout, null)
        val zoomImage = zoomView.findViewById<ImageView>(R.id.zoomImage)

        Glide.with(requireActivity())
            .load(imageFile)
            .fitCenter()
            .into(zoomImage)

        val dialog = Dialog(requireActivity())
        dialog.setContentView(zoomView)
        dialog.setCancelable(true)

        val closeButton = zoomView.findViewById<Button>(R.id.close)
        closeButton?.setOnClickListener { dialog.dismiss() }

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }

    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            val voices = textToSpeech.voices
            textToSpeech.setPitch(0.8f)
            textToSpeech.setSpeechRate(1.2f)
            voices?.forEach { voice ->
                if (voice.name.contains("male", ignoreCase = true)) {
                    textToSpeech.voice = voice
                    return@forEach
                }
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported")
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}

private fun CameraFragment.showBeautifulMessageDialog(
    title: String,
    message: Any,
    function: () -> Unit
) {
    val dialog = BeautifulMessageDialog(requireActivity(), title, message.toString(), function)
    dialog.show()
}
