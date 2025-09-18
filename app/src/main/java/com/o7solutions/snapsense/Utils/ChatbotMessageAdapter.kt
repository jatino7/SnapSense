package com.o7solutions.snapsense.Utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.o7solutions.snapsense.databinding.ChatbotChatItemBinding

class ChatbotMessageAdapter(
    private var messageList: List<MessageModel>
) : RecyclerView.Adapter<ChatbotMessageAdapter.MessageViewHolder>() {




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ChatbotChatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }



    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size

    inner class MessageViewHolder(private val binding: ChatbotChatItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            if (message.sentBy == MessageModel.SENT_BY_ME) {
                binding.leftChatView.visibility = View.GONE
                binding.rightChatView.visibility = View.VISIBLE
                binding.rightChatTextView.text = message.message
            } else {
                binding.leftChatView.visibility = View.VISIBLE
                binding.rightChatView.visibility = View.GONE
                binding.leftChatTextView.text = message.message
            }
        }
    }
}