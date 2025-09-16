package com.o7solutions.snapsense.UI

import android.os.Bundle
import android.Manifest
import android.app.VoiceInteractor
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.o7solutions.snapsense.Cloudinary.UploadImage
import com.o7solutions.snapsense.R
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.GeminiApi
import com.o7solutions.snapsense.databinding.FragmentCameraBinding
import com.o7solutions.snapsense.databinding.FragmentTestingBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TestingFragment : Fragment(), TextToSpeech.OnInitListener {


    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textView: TextView
    private lateinit var startButton: Button
    private lateinit var textToSpeech: TextToSpeech
    lateinit var fileToUpload : File

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var binding: FragmentTestingBinding // keep same binding if layout is same

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTestingBinding.inflate(layoutInflater) // same layout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textToSpeech = TextToSpeech(requireContext(), this)

//        initializing speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireActivity())

//        binding.startButton.setOnClickListener {
//            if (checkAudioPermission()) {
//                startListening()
//            } else {
//                requestAudioPermission()
//            }
//        }




        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Speech started")
            }

            override fun onBufferReceived(p0: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "Speech ended")
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error code: $error")
                binding.mic.visibility = View.GONE
                binding.loader.visibility = View.GONE
                binding.btnMic.visibility = View.VISIBLE
                Toast.makeText(requireActivity(), "Internal Issue Try Again!", Toast.LENGTH_SHORT).show()
//                Toast.makeText(requireContext(), "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onEvent(p0: Int, p1: Bundle?) {}

            override fun onPartialResults(p0: Bundle?) {}

            override fun onReadyForSpeech(p0: Bundle?) {
                binding.mic.visibility = View.VISIBLE

                Log.d("SpeechRecognizer", "Ready for speech")
            }

            override fun onResults(result: Bundle?) {
                binding.loader.visibility = View.VISIBLE
                binding.mic.visibility = View.GONE

                val matches = result?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                matches?.let { binding.textView.text = it[0] }
                Toast.makeText(requireActivity(), "Ques:${matches?.get(0)}", Toast.LENGTH_SHORT).show()

                analyzeWithGemini(fileToUpload, matches?.get(0) ?: AppConstants.prompt)
            }

            override fun onRmsChanged(p0: Float) {}
        })

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

        binding.btnMic.setOnClickListener {

            binding.btnMic.visibility = View.GONE
            if (checkAudioPermission()) {
                startListening()
            } else {
                requestAudioPermission()
            }
        }
//
        binding.btnCapture.setOnClickListener {
            binding.btnCapture.isEnabled = false
            binding.btnCapture.visibility = View.GONE
            binding.btnMic.visibility = View.VISIBLE

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
                    Glide.with(requireActivity())
                        .load(file)
                        .into(binding.imageView)
                    fileToUpload = file
                    binding.loader.visibility = View.GONE
                    Toast.makeText(requireActivity(), "Press the mic button to give prompt", Toast.LENGTH_SHORT).show()

                }
            }
        )
    }

    private fun analyzeWithGemini(file: File,prompt: String) {
        val gemini = GeminiApi(AppConstants.apiKey)
        gemini.analyzeImage(file, prompt) { result ->
            requireActivity().runOnUiThread {
                Log.d("ApiResult", result)
                binding.loader.visibility = View.GONE

                val uri = Uri.fromFile(file)

                val bundle = Bundle()
                bundle.putParcelable("imageUri", uri)
                bundle.putString("title", formatText(result))

                findNavController().navigate(R.id.viewFragment,bundle)
//                showAlert(formatText(result))
            }
        }
    }

    fun formatText(input: String): String {
        return input
            .replace(Regex("\\*\\*"), " ")
            .replace(Regex("\\*"), "•")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    private fun showAlert(response: String) {
        val builder = AlertDialog.Builder(requireActivity())
        speakOut(response)
        builder.setMessage(response)
        builder.setCancelable(false)
        builder.setPositiveButton("Ok") { dialog, _ ->

            if (::textToSpeech.isInitialized) {
                textToSpeech.stop()
            }
            binding.btnCapture.isEnabled = true
            binding.btnCapture.visibility = View.VISIBLE
            binding.btnMic.visibility = View.GONE
            binding.imageView.visibility = View.GONE
            binding.previewView.visibility = View.VISIBLE
            binding.imageView.setImageDrawable(null)
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechRecognizer.startListening(intent)

    }
    companion object {
        @JvmStatic
        fun newInstance() = TestingFragment()
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            101
        )
    }

    private fun speakOut(text: String) {
          // faster, robotic feel
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            val voices = textToSpeech.voices
            textToSpeech.setPitch(0.8f)       // lower pitch
            textToSpeech.setSpeechRate(1.2f)
            voices?.forEach { voice ->
                if (voice.name.contains("male", ignoreCase = true)) {
                    textToSpeech.voice = voice   // Pick male voice
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

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

}
