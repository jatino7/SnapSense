package com.o7solutions.snapsense.UI

import android.content.Context
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
import com.o7solutions.snapsense.Utils.Chatbot
import com.o7solutions.snapsense.Utils.ChatbotMessageAdapter
import com.o7solutions.snapsense.Utils.MessageModel
import com.o7solutions.snapsense.databinding.FragmentChatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.view.inputmethod.InputMethodManager
import com.o7solutions.snapsense.Utils.AppFunctions


class ChatFragment : Fragment() {

    var  apiKey = ""
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val messageList = mutableListOf<MessageModel>()
    private lateinit var messageAdapter: ChatbotMessageAdapter
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
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

        apiKey = AppFunctions.readApiKey(requireActivity()).toString()
        messageAdapter = ChatbotMessageAdapter(messageList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = messageAdapter

        val editText = binding.messageEt

        // Request focus and show keyboard
        editText.requestFocus()

        // Show keyboard with a slight delay
        editText.postDelayed({
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

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
                    generativeModel.generateContent(buildFinalPrompt(question)).text
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

//    private fun callAPI(question: String) {
//
//
//        val response = Chatbot.getKeywordValues(question).toString()
//        val finalOutput = removeDoubleAsterisks(response)
//        Log.d("ChatFragment", response.toString())
//        addResponse(finalOutput ?: "Oops! I couldn’t understand that.")
//
//        binding.loader.visibility = View.GONE
//
//
//    }

    fun buildFinalPrompt(prompt: String): String {
        return """
You are a virtual support assistant for O7 Services, a training and IT solutions company.
Your role is to answer user queries accurately, concisely, and in a professional tone.
### Knowledge Base
O7 Services (ISO 9001:2015 Certified) was founded in 2015.
They specialize in:
- Web & Mobile Application Development
- Custom Software Development
- UI/UX Designing, Hosting, Digital Marketing
- Domain Registration, AMC & MMC Services, Bulk SMS & Voice Calls
They provide end-to-end IT solutions: consulting → development → deployment → QA → 24/7 support.
With over 9 years of experience, they focus on long-term partnerships, affordable pricing, timely delivery, and measurable results.
Headquarters: Jalandhar | Branch: Hoshiarpur
Products: Vehicle Tracking System, Invoice Software, School & Hospital Management Systems, Parent–Teacher App, Fee & Task Management, Online Food Ordering, Security App, Admission & Inventory Software, Car Servicing App, and more.
Training Programs:
- 6 Weeks/6 Months Industrial Training
- Project-Based, Corporate & Job-Oriented Training
- Covering: Full Stack (MEAN/MERN), Flutter, Kotlin, Swift UI, Firebase, Python, Angular, React, Vue, Node.js, ASP.NET, PHP (Laravel, CodeIgniter), Testing, Cloud, Blockchain, DevOps, Data Science, AI/ML, UI/UX, Digital Marketing, WordPress, Linux, Networking (CCNP, CCNA), Cyber Security, Java (Spring, Hibernate), C/C++, Photoshop, Illustrator, Figma, CorelDraw, etc.
Contact:
+91-8437365007, +91-181-5015007
enquiry@o7services.com | hr@o7services.com
www.o7services.com
---
### Instructions
0. Try to give detailed answer.
1. This response will be converted into audio, so give response accordingly, do not include any emojis.
2. Do not include the special symbols on the response like #,*
3. Always use the knowledge base to answer queries, You can give detailed and creative answers to queries related to tech.
4. If the answer is not explicitly in the knowledge base, say than you can answer according to your knowledge.
5. Keep answers clear, professional, and detailed.
6. Where relevant, include contact info or website links for follow-up.
7. You can give short and creative answers to queries related to tech.
---
### User Query
$prompt
""".trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
