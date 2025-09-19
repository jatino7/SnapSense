package com.o7solutions.snapsense.UI

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.o7solutions.snapsense.R
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.AppFunctions
import com.o7solutions.snapsense.Utils.GeminiApi
import com.o7solutions.snapsense.databinding.FragmentFirstBinding
import java.io.File

class FirstFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding

    // Launcher to pick image
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            binding.imageView.setImageURI(it) // Show selected image
            val file = uriToFile(it)

            if (file != null) {
                binding.loader.visibility = View.VISIBLE
                analyzeWithGemini(file)
//                Toast.makeText(requireContext(), "Image selected: ${file.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to get image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher to request permission
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPickImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    // Convert URI → File
    private fun uriToFile(uri: Uri): File? {
        val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("picked_image", ".jpg", requireContext().cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile
    }

    fun formatText(input: String): String {
        return input
            // Replace double asterisks first
            .replace(Regex("\\*\\*"), " ")
            // Then replace single asterisk
            .replace(Regex("\\*"), "•")
    }

    private fun analyzeWithGemini(file: File) {
        val gemini = GeminiApi(AppFunctions.readApiKey(requireActivity()).toString())
        gemini.analyzeImage(file, AppConstants.prompt) { result ->
            requireActivity().runOnUiThread {
                Log.d("ApiResult", result)
                binding.loader.visibility = View.GONE

                val uri = Uri.fromFile(file)

                val bundle = Bundle()
                bundle.putParcelable("imageUri", uri)
                bundle.putString("title", formatText(result))
                findNavController().navigate(R.id.viewFragment,bundle)
//                showResultBottomSheet(formatText(result))
            }
        }
    }

    private fun showResultBottomSheet(response: String) {
        val bottomSheetDialog = BottomSheetDialog(requireActivity())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val txtResponse = view.findViewById<TextView>(R.id.txtResponse)

        txtResponse.text = response

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
}