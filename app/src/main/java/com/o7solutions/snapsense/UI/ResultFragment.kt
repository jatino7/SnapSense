package com.o7solutions.snapsense.UI

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.o7solutions.snapsense.Unsplash.UnsplashPhoto
import com.o7solutions.snapsense.Utils.AppFunctions
import com.o7solutions.snapsense.Utils.ChatbotMessageAdapter
import com.o7solutions.snapsense.Utils.GeminiApi
import com.o7solutions.snapsense.Utils.MessageModel
import com.o7solutions.snapsense.Utils.WebViewBottomSheet
import com.o7solutions.snapsense.compose.BottomSheet
import com.o7solutions.snapsense.databinding.FragmentResultBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultFragment : Fragment(), ChatbotMessageAdapter.onClick {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val showSheetState = mutableStateOf(false)
    private var webViewBottomSheet: WebViewBottomSheet? = null
    var sheetShown = false


    var imagesList: List<UnsplashPhoto> = emptyList()
    var keyword = ""
    private val messageList = mutableListOf<MessageModel>()
    private lateinit var messageAdapter: ChatbotMessageAdapter
    var apiKey = ""
    var isScrolling = false
    private lateinit var binding: FragmentResultBinding
    var title = ""

    // Permission launcher for RECORD_AUDIO
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startSpeechToText()
            } else {
                Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    // Launcher for speech recognition
    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data
                val recognizedText =
                    data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                recognizedText?.let {
                    binding.messageEt.setText(it) // Fill EditText
                    binding.messageEt.setSelection(it.length) // Move cursor to end
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onResume() {
        super.onResume()
//        requireActivity().window.setSoftInputMode(
////            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
//        )
    }

    override fun onPause() {
        super.onPause()
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_result, container, false)
        binding = FragmentResultBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.messageEt.requestFocus()
//        val imm =
//            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(binding.messageEt, InputMethodManager.SHOW_IMPLICIT)


        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                binding.btnScrollUp.visibility = View.GONE
                lifecycleScope.launch {
                    binding.btnScrollDown.visibility = View.VISIBLE
                    delay(2000)
                    binding.btnScrollDown.visibility = View.GONE
                }

                println("Scrolling Down")
            } else if (scrollY < oldScrollY) {
                binding.btnScrollDown.visibility = View.GONE

                lifecycleScope.launch {
                    binding.btnScrollUp.visibility = View.VISIBLE
                    delay(2000)
                    binding.btnScrollUp.visibility = View.GONE
                }
                println("Scrolling Up")

            }


        }

        binding.btnScrollUp.setOnClickListener {
            isScrolling = true
            binding.scrollView.smoothScrollTo(0, 0) // x = 0, y = 0 → top
        }

        binding.btnScrollDown.setOnClickListener {
            isScrolling = true
            binding.scrollView.post {
                binding.scrollView.fullScroll(View.FOCUS_DOWN) // scrolls to bottom
            }
        }


        binding.micBtn.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
//        if (title != null) {
//

//            val handler = Handler(Looper.getMainLooper())
//            var index = 0
//            val runnable = object : Runnable {
//                override fun run() {
//                    if (index <= title.length) {
//                        binding.txtResponse.text = title.substring(0, index)
//                        index++
//                        handler.postDelayed(this, 5) // 40ms per character
//                    }
//                }
//            }
//            handler.post(runnable)

//        }

//        Initialization of all components
        apiKey = AppFunctions.readApiKey(requireActivity()).toString()
        messageAdapter = ChatbotMessageAdapter(messageList, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = messageAdapter


//receiving components

        val uri = arguments?.getParcelable<Uri>("imageUri")
        title = arguments?.getString("title").toString()
        val ques = arguments?.getString("ques")
        keyword = arguments?.getString("keyword").toString()



        if (uri != null) {
            binding.imageFile.setImageURI(uri)
        }
        if (ques != null) {
            addToChat(ques, MessageModel.SENT_BY_ME)
        }


        if (title != null) {
//            getImages()
            addResponse(title, imagesList)
        }


        if (keyword != null) {
//            val url = "https://www.google.com/search?tbm=shop&q=$keyword"
//            webViewBottomSheet = WebViewBottomSheet.newInstance(url)
//            webViewBottomSheet?.show(parentFragmentManager, "WebViewBottomSheet")


        }
//        binding.composeView?.setContent {
////             Use the state objects directly inside composition
//            val showSheet = showSheetState
//
//            if (showSheet.value) {
//                BottomSheet(
//                    onDismiss = { showSheet.value = false },
//                    url = "https://www.google.com/search?tbm=shop&q=$keyword"
//                )
//            }
//        }


        binding.fab?.setOnClickListener {
            val url = "https://www.bing.com/shop?q=$keyword"
            showWebViewSheet(url)

        }



        if (keyword.isNotEmpty()) {


            print("Keyword is not empty")
            showSheetState.value = true
        }
//        Adding question
//        val editText = binding.messageEt
//
//        // Request focus and show keyboard
//        editText.requestFocus()
//
//        // Show keyboard with a slight delay
//        editText.postDelayed({
//            val imm =
//                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//        }, 0)


        binding.sendBtn.setOnClickListener {

            isScrolling = false
            val question = binding.messageEt.text.toString().trim()
            if (question.isNotEmpty()) {

//                binding.loader.visibility = View.VISIBLE
                binding.messageEt.text?.clear()

                addToChat(question, MessageModel.SENT_BY_ME)
                addTypingIndicator()
//                analyze with gemini implementation
                uriToFile(requireActivity(), uri)?.let { file -> analyzeWithGemini(file, question) }


            }
        }


    }


    //    Recycler Scroll
    private fun addToChat(message: String, sentBy: String) {
        messageList.add(MessageModel(message, sentBy))
        messageAdapter.notifyItemInserted(messageList.size - 1)
        binding.recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
    }

    private fun addTypingIndicator() {
        messageList.add(MessageModel("Typing...", MessageModel.SENT_BY_BOT))
        messageAdapter.notifyItemInserted(messageList.size - 1)
        binding.recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
    }

    private fun addResponse(response: String?, imagesList: List<UnsplashPhoto>?) {
        var fileList: List<UnsplashPhoto> = if (imagesList?.isNotEmpty() == true) {
            imagesList
        } else {
            emptyList()
        }
        Log.d("Response", response.toString())
        if (messageList.isNotEmpty() && messageList.last().message == "Typing...") {
            val removePosition = messageList.size - 1
            messageList.removeAt(removePosition)
            messageAdapter.notifyItemRemoved(removePosition)
        }
        response?.takeIf { it.isNotBlank() }?.let {
            messageList.add(MessageModel(it, MessageModel.SENT_BY_BOT, fileList))
            messageAdapter.notifyItemInserted(messageList.size - 1)
        }
        binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
//        binding.scrollView.post {
//            binding.scrollView.fullScroll(View.FOCUS_DOWN) // scrolls to bottom
//        }

        if (!sheetShown) {
            sheetShown = false
        }

        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)

    }


    //    gemini

    fun uriToFile(context: Context, uri: Uri?): File? {

        if (uri == null) return null
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_file_${System.currentTimeMillis()}")
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {

            addResponse("Unable to process your query", emptyList())
            e.printStackTrace()
            null
        }
    }

    private fun analyzeWithGemini(file: File, prompt: String) {
        val gemini = GeminiApi(AppFunctions.readApiKey(requireActivity()).toString())
        gemini.analyzeImage(
            file, "You are working as AI agent and your task is to give" +
                    "info about the image according to user query if user query is about more details " +
                    "you have to tell all object present in image here is user query=>$prompt"
        ) { result ->
            requireActivity().runOnUiThread {
                Log.d("ApiResult", result)
                binding.loader.visibility = View.GONE
                addResponse(formatText(result), emptyList())
            }
        }

    }


    private fun startSpeechToText() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }

        speechLauncher.launch(intent)
    }

    fun formatText(input: String): String {
        return input.replace(Regex("\\*\\*"), " ")
            .replace(Regex("\\*"), "•")
    }

    override fun move() {

        if (!isScrolling) {


            binding.scrollView.post {
                binding.scrollView.fullScroll(View.FOCUS_DOWN) // scrolls to bottom
            }

        }
    }

    override fun end() {
        val url = "https://www.bing.com/shop?q=$keyword"
        showWebViewSheet(url)
    }

    private fun showWebViewSheet(url: String) {
        val existing = parentFragmentManager.findFragmentByTag("WebViewBottomSheet")
        if (existing == null) {
            webViewBottomSheet = WebViewBottomSheet.newInstance(url)
            webViewBottomSheet?.show(parentFragmentManager, "WebViewBottomSheet")
        } else {
            // Reuse existing one
            (existing as? WebViewBottomSheet)?.dialog?.show()
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ResultFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun getImages() {

        lifecycleScope.launch {
            imagesList = AppFunctions.searchPhotos(query = keyword.toString())
            Log.d("Images List", imagesList.toString())

        }


    }
}