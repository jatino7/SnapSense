package com.o7solutions.snapsense.UI

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.o7solutions.snapsense.Cloudinary.UploadImage
import com.o7solutions.snapsense.R
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.AppFunctions
import com.o7solutions.snapsense.Utils.GeminiApi
import com.o7solutions.snapsense.databinding.FragmentTestingBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TestingFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var binding: FragmentTestingBinding
    private lateinit var fileToUpload: File

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    private val REQUEST_CODE_PERMISSIONS = 200

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textToSpeech = TextToSpeech(requireContext(), this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())

        // Permissions check
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnCapture.setOnClickListener {
            binding.btnCapture.isEnabled = false
            binding.btnCapture.visibility = View.GONE
            binding.btnMic.visibility = View.VISIBLE
            captureAndUpload()
            binding.previewView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.loader.visibility = View.VISIBLE
        }

        binding.btnMic.setOnClickListener {
            binding.btnMic.visibility = View.GONE
            if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
                startListening()
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_PERMISSIONS)
            }
        }

        // Speech recognizer listener
        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                binding.mic.visibility = View.VISIBLE
                Log.d("SpeechRecognizer", "Ready for speech")
            }

            override fun onResults(result: Bundle?) {

                binding.bottomLayout.visibility = View.VISIBLE
                binding.mic.visibility = View.GONE

                val matches = result?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
                binding.quesLayout.text = "Ques: ${matches.toString()}"

                binding.tryAgainBtn.setOnClickListener {
                    tryAgain()
                }

                binding.continueBtn.setOnClickListener {
                    continueTask(matches.toString())
                }
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error code: $error")
                binding.mic.visibility = View.GONE
                binding.loader.visibility = View.GONE
                binding.btnMic.visibility = View.VISIBLE
                Toast.makeText(requireActivity(), "Internal Issue. Try Again!", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onRmsChanged(p0: Float) {}
        })

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechRecognizer.startListening(intent)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    fun tryAgain() {

        binding.bottomLayout.visibility = View.GONE
        binding.btnMic.visibility = View.VISIBLE

    }


    fun continueTask(ques: String) {
        binding.loader.visibility = View.VISIBLE
        analyzeWithGemini(fileToUpload, ques ?: AppConstants.prompt)
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
                    Glide.with(requireActivity()).load(file).into(binding.imageView)
                    fileToUpload = file
                    binding.loader.visibility = View.GONE
                    Toast.makeText(requireActivity(), "Press the mic button to give prompt", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun analyzeWithGemini(file: File, prompt: String) {
        val gemini = GeminiApi(AppFunctions.readApiKey(requireActivity()).toString())
        gemini.analyzeImage(file, prompt) { result ->
            requireActivity().runOnUiThread {
                Log.d("ApiResult", result)
                binding.loader.visibility = View.GONE
                val uri = Uri.fromFile(file)
                val bundle = Bundle().apply {
                    putParcelable("imageUri", uri)
                    putString("title", formatText(result))
                }
                findNavController().navigate(R.id.viewFragment, bundle)
            }
        }
    }

    private fun formatText(input: String): String {
        return input.replace(Regex("\\*\\*"), " ").replace(Regex("\\*"), "•")
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
            textToSpeech.setPitch(0.8f)
            textToSpeech.setSpeechRate(1.2f)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TestingFragment()
    }
}
