package com.o7solutions.snapsense.UI

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.ai.client.generativeai.GenerativeModel
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.ChatbotMessageAdapter
import com.o7solutions.snapsense.Utils.MessageModel
import com.o7solutions.snapsense.databinding.FragmentChatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class ChatFragment : Fragment() {

    private val apiKey = AppConstants.apiKey
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val messageList = mutableListOf<MessageModel>()
    private lateinit var messageAdapter: ChatbotMessageAdapter
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageAdapter = ChatbotMessageAdapter(messageList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = messageAdapter

        binding.sendBtn.setOnClickListener {
            val question = binding.messageEt.text.toString().trim()
            if (question.isNotEmpty()) {

                binding.loader.visibility = View.VISIBLE
                binding.messageEt.text?.clear()

                addToChat(question, MessageModel.SENT_BY_ME)
                addTypingIndicator()
                callAPI(question)
            }
        }
    }

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

    private fun addResponse(response: String?) {
        if (messageList.isNotEmpty() && messageList.last().message == "Typing...") {
            val removePosition = messageList.size - 1
            messageList.removeAt(removePosition)
            messageAdapter.notifyItemRemoved(removePosition)
        }
        response?.takeIf { it.isNotBlank() }?.let {
            messageList.add(MessageModel(it, MessageModel.SENT_BY_BOT))
            messageAdapter.notifyItemInserted(messageList.size - 1)
        }
        binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
    }

    private fun removeDoubleAsterisks(input: String?): String {
        if (input == null) return ""
        val regex = "\\*\\*+".toRegex()
        return regex.replace(input, "")
    }

    private fun callAPI(question: String) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // run API call in background
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(question).text
                }

                val finalOutput = removeDoubleAsterisks(response)
                Log.d("ChatFragment", response.toString())
                addResponse(finalOutput ?: "Oops! I couldn’t understand that.")

                binding.loader.visibility = View.GONE

            } catch (e: Exception) {
                binding.loader.visibility = View.GONE

                addResponse("Error: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
