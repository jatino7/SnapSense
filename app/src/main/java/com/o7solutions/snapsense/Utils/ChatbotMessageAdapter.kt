package com.o7solutions.snapsense.Utils

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.o7solutions.snapsense.Unsplash.PhotoAdapter
import com.o7solutions.snapsense.databinding.ChatbotChatItemBinding

class ChatbotMessageAdapter(
    private var messageList: List<MessageModel>,val onClickItem: onClick
) : RecyclerView.Adapter<ChatbotMessageAdapter.MessageViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding =
            ChatbotChatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size

    inner class MessageViewHolder(private val binding: ChatbotChatItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            if (message.sentBy == MessageModel.SENT_BY_ME) {
                binding.leftChatView.visibility = View.GONE
                binding.rightChatView.visibility = View.VISIBLE
                binding.rightChatTextView.text = message.message
            } else {
                binding.leftChatView.visibility = View.VISIBLE
                binding.rightChatView.visibility = View.GONE
                val handler = Handler(Looper.getMainLooper())
                var index = 0
                val runnable = object : Runnable {
                    override fun run() {
                        if (index <= message.message.length) {
                            binding.leftChatTextView.text = message.message.substring(0, index)
                            index++
                            (itemView.parent as? RecyclerView)?.post {
                                (itemView.parent as RecyclerView).smoothScrollToPosition(adapterPosition)
                            }

                            onClickItem.move()
                            handler.postDelayed(this, 5) // 40ms per character
                        } else {
                            onClickItem.end()

                        }
                    }


                }
                handler.post(runnable)



                if (message.images.isNotEmpty()) {

                    print("ImagesList in ChatbotMessageAdapter ${message.images}")
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.recyclerView.layoutManager =
                        LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                    binding.recyclerView.adapter = PhotoAdapter(message.images)
                } else {
                    binding.recyclerView.visibility = View.GONE
                }
//                binding.leftChatTextView.text = message.message
            }
        }
    }

    interface onClick {

        fun move()

        fun end()

    }
}