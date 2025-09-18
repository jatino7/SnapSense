package com.o7solutions.snapsense.Utils

class MessageModel(var message: String, var sentBy: String) {
    companion object {
        const val SENT_BY_ME = "Me"
        const val SENT_BY_BOT = "Bot"
    }
}