package com.o7solutions.snapsense.UI

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.common.sdkinternal.Constants
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.o7solutions.snapsense.R
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.AppFunctions
import com.o7solutions.snapsense.Utils.GeminiApi
import com.o7solutions.snapsense.databinding.FragmentQRBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A simple [androidx.fragment.app.Fragment] subclass.
 * Use the [QRFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QRFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
//    private lateinit var cameraExecutor: ExecutorService
    private val TAG = "QrScanner"
    private lateinit var binding: FragmentQRBinding
//    private var scannedCode: String? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {

            showResult(result.contents)
//            val gemini = GeminiApi(AppConstants.apiKey)
//            gemini.analyzeText(result.contents) { result ->
//
//                requireActivity().runOnUiThread {
//                    showR
//                }
//            }

            // you can now pass result.contents to Gemini API
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_q_r, container, false)
        binding = FragmentQRBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        cameraExecutor = Executors.newSingleThreadExecutor()
//        if (allPermissionsGranted()) {
////            startCamera()
//
//        } else {
//            ActivityCompat.requestPermissions(
//                requireActivity(),
//                arrayOf(Manifest.permission.CAMERA),
//                101
//            )
//        }

        binding.startBTN.setOnClickListener {
            if (allPermissionsGranted()) {
                startQRScanner()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA),
                    101
                )
            }
        }


    }

    private fun startQRScanner() {
        val options = ScanOptions()
        options.setPrompt("Scan a QR Code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        options.captureActivity = CustomCaptureActivity::class.java
        barcodeLauncher.launch(options)
    }

    fun formatText(input: String): String {
        return input
            // Replace double asterisks first
            .replace(Regex("\\*\\*"), " ")
            // Then replace single asterisk
            .replace(Regex("\\*"), "•")
    }
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
//        cameraExecutor.shutdown()
    }

    private fun showResult(message: String) {


        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton("Copy") { dialog, _ ->
//            scannedCode = null
            binding.qrLottie.visibility = View.VISIBLE
            copyTextToClipboard(message, requireActivity())
            dialog.dismiss()
        }
        builder.setNegativeButton("Ok") { dialog, _ ->
//            scannedCode = null
            binding.qrLottie.visibility = View.VISIBLE
            dialog.dismiss()
        }

        builder.show()
    }

    fun copyTextToClipboard(text: String, context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Url copied", Toast.LENGTH_SHORT).show()
    }


//    MLkit

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
//                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
//                    it.setAnalyzer(cameraExecutor) { imageProxy ->
//                        processImageProxy(imageProxy)
//                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_URL -> {

                                val codeValue = barcode.rawValue ?: continue

                                // Skip if already scanned
//                                if (scannedCode == codeValue) {
//                                    continue
//                                }
//                                scannedCode = codeValue
                                binding.qrLottie.visibility = View.GONE

                                showResult(codeValue)
                                return@addOnSuccessListener
                            }

                            else -> {
                                val codeValue = barcode.rawValue ?: continue

                                // Skip if already scanned
//                                if (scannedCode == codeValue) {
//                                    continue
//                                }
//                                scannedCode = codeValue
                                binding.qrLottie.visibility = View.GONE

                                val gemini = GeminiApi(AppFunctions.readApiKey(requireActivity()).toString())
//                                gemini.analyzeText(scannedCode.toString()) { result ->
//
//                                    requireActivity().runOnUiThread {
//
//
//                                        showResult(result)
//                                    }
//                                }

//                                Toast.makeText(requireContext(), "Scanned: ${barcode.rawValue}", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Barcode scanning failed", it)
                    return@addOnFailureListener
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }


}