package com.o7solutions.snapsense.UI

import android.os.Bundle
import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.o7solutions.snapsense.Cloudinary.UploadImage
import com.o7solutions.snapsense.R
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.BeautifulMessageDialog
import com.o7solutions.snapsense.Utils.GeminiApi
import com.o7solutions.snapsense.databinding.FragmentCameraBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CameraFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CameraFragment : Fragment(), TextToSpeech.OnInitListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    private lateinit var textToSpeech: TextToSpeech

    lateinit var imageFile: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var binding: FragmentCameraBinding

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
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_camera, container, false)

        binding = FragmentCameraBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textToSpeech = TextToSpeech(requireContext(), this)

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

                    imageFile = file


                }
            }
        )
    }

    fun analyzeWithGeminiUrl(url: String) {
        val gemini = GeminiApi(AppConstants.apiKey)
        lifecycleScope.launch {


            gemini.analyzeImageFromUrl(url, AppConstants.prompt) { result ->

                requireActivity().runOnUiThread {
                    Log.d("ApiResult", result)
                    binding.loader.visibility = View.GONE
//                binding.loader.visibility = View.GONE
//                showAlert(formatText(result))

//                showResultBottomSheet(cleanResponse(result))
                }

            }
        }

    }

    fun showBeautifulDialog(context: Context, title: String, message: String,file: File) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_message_dialog)
        dialog.setCancelable(false)


        speakOut(message)
        val dialogIcon = dialog.findViewById<ImageView>(R.id.dialogIcon)
        val dialogTitle = dialog.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialog.findViewById<TextView>(R.id.dialogMessage)
        val okButton = dialog.findViewById<Button>(R.id.dialogOkBtn)

        Glide.with(requireActivity())
            .load(imageFile)
            .into(dialogIcon)


        dialogIcon.setOnClickListener {

            showZoomDialog(file)
        }
        dialogTitle.text = title
        dialogMessage.text = ""

        val handler = Handler(Looper.getMainLooper())
        var index = 0
        val runnable = object : Runnable {
            override fun run() {
                if (index <= message.length) {
                    dialogMessage.text = message.substring(0, index)
                    index++
                    handler.postDelayed(this, 5) // 40ms per character
                }
            }
        }
        handler.post(runnable)

        okButton.setOnClickListener {
            binding.btnCapture.isEnabled = true
            binding.imageView.visibility = View.GONE
            binding.previewView.visibility = View.VISIBLE
            binding.imageView.setImageDrawable(null)

            textToSpeech.stop()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun showZoomDialog(imageFile: File) {
        // Inflate custom layout
        val zoomView = layoutInflater.inflate(R.layout.zoom_layout, null)
        val zoomImage = zoomView.findViewById<ImageView>(R.id.zoomImage)

        // Load image using Glide
        Glide.with(requireActivity())
            .load(imageFile)
            .fitCenter()
            .into(zoomImage)

        // Create dialog
        val dialog = Dialog(requireActivity())
        dialog.setContentView(zoomView)
        dialog.setCancelable(true) // allow closing by tapping outside

        // Optional: close on click (if you want a close button)
        val closeButton = zoomView.findViewById<Button>(R.id.close)
        closeButton?.setOnClickListener { dialog.dismiss() }

        // Make dialog full screen
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dialog.show()
    }



    private fun analyzeWithGemini(file: File) {
        val gemini = GeminiApi(AppConstants.apiKey)
        gemini.analyzeImage(file, AppConstants.prompt) { result ->
            requireActivity().runOnUiThread {
                Log.d("ApiResult", result)
                binding.loader.visibility = View.GONE
//                showAlert(formatText(result))


                val uri = Uri.fromFile(file)

                val bundle = Bundle()
                bundle.putParcelable("imageUri", uri)
                bundle.putString("title", formatText(result))

                findNavController().navigate(R.id.viewFragment,bundle)
//                showBeautifulDialog(requireActivity(),"Message",formatText(result),file)

//                showBeautifulMessageDialog(
//                    title = "Important Notice",
//                    message = result
//                ) {
//
//                    binding.btnCapture.isEnabled = true
//                    binding.imageView.visibility = View.GONE
//                    binding.previewView.visibility = View.VISIBLE
//                    binding.imageView.setImageDrawable(null)
//
//                    // Handle OK button click
////                    finish() // or any other action
//                }

//                showResultBottomSheet(cleanResponse(result))
            }
        }
    }

    fun formatText(input: String): String {
        return input
            // Replace double asterisks first
            .replace(Regex("\\*\\*"), " ")
            // Then replace single asterisk
            .replace(Regex("\\*"), "•")
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
         * @return A new instance of fragment CameraFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CameraFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun showAlert(response: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(response)
        builder.setCancelable(false)

        // Positive button
        builder.setPositiveButton("Ok") { dialog, which ->
            binding.btnCapture.isEnabled = true
            binding.imageView.visibility = View.GONE
            binding.previewView.visibility = View.VISIBLE
            binding.imageView.setImageDrawable(null)

            dialog.dismiss()
        }


        // Create and show the dialog
        val dialog = builder.create()
//        dialog.show()
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

    // Extension function for easy usage

}


private fun CameraFragment.showBeautifulMessageDialog(
    title: String,
    message: Any,
    function: () -> Unit
) {

    val dialog = BeautifulMessageDialog(requireActivity(), title, message.toString(), function)
    dialog.show()
}
