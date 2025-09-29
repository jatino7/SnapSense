package com.o7solutions.snapsenseV2.Utils

import com.o7solutions.snapsenseV2.Unsplash.UnsplashPhoto

class MessageModel(
    var message: String,
    var sentBy: String,
    val images: List<UnsplashPhoto> = emptyList()
) {
    companion object {
        const val SENT_BY_ME = "Me"
        const val SENT_BY_BOT = "Bot"
    }
}