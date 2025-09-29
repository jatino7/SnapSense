package com.o7solutions.snapsenseV2.UI

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.o7solutions.snapsenseV2.R
import com.o7solutions.snapsenseV2.Utils.AppConstants
import com.o7solutions.snapsenseV2.Utils.AppFunctions
import com.o7solutions.snapsenseV2.Utils.GeminiApi
import com.o7solutions.snapsenseV2.databinding.FragmentCameraBinding
import com.o7solutions.snapsenseV2.databinding.FragmentFaceEmotionBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FaceEmotionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FaceEmotionFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentFaceEmotionBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

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
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFaceEmotionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        initializing camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // check permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

                captureAndUpload()
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
//                    Glide.with(requireActivity())
//                        .load(file)
//                        .into(binding.imageView)
////                    analyzeWithGemini(file)
//                    imageFile = file

                    binding.loader.visibility = View.VISIBLE
                    analyzeWithGemini(file)
                }
            }
        )
    }

    fun retakeImage() {

        lifecycleScope.launch {

            delay(2000)
            captureAndUpload()

        }

    }

    private fun analyzeWithGemini(file: File) {
//        binding.loader.visibility = View.VISIBLE

        val gemini = GeminiApi(AppFunctions.readApiKey(requireActivity()).toString())
        gemini.analyzeImageGetKeyWord(
            file, "You are working as Emotion detector AI bot.You have tell the user expression" +
                    " in word word with emoji and if you not able to see face in image you have to " +
                    "answer -> No Face Detected! and if face detected tell me 5 hindi songs according to my mood"
        ) { result ->

            if(isAdded && context != null) {
                requireActivity().runOnUiThread {
//                Toast.makeText(requireContext(), formatText(result), Toast.LENGTH_SHORT).show()
                    showCustomAlertDialog(requireContext(),"",formatText(result))
                    binding.loader.visibility = View.GONE
                }
            }






        }
    }

    fun formatText(input: String): String {
        return input.replace(Regex("\\*\\*"), " ")
            .replace(Regex("\\*"), "•")
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FaceEmotionFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FaceEmotionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun showCustomAlertDialog(context: Context, title: String, message: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_alert_dialog, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

        tvTitle.text = title
        tvMessage.text = message

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)

        val alertDialog = builder.create()
        alertDialog.setCancelable(false) // disable outside touch

        btnOk.setOnClickListener {
            alertDialog.dismiss()
            retakeImage() // your function call
        }

        alertDialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}