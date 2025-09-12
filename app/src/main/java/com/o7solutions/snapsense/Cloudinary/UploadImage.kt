package com.o7solutions.snapsense.Cloudinary

import android.net.Uri
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object UploadImage {

    fun uploadImage(fileUri: Uri,callback: (String?)-> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(fileUri.path!!) // Or use a file path
                val result =
                    CloudinaryConfig.instance.uploader().upload(file, ObjectUtils.emptyMap())

                val imageUrl = result["secure_url"] as String
                val publicUrl = result["url"] as String
//                println("✅ Uploaded Image URL: $imageUrl")


                callback(publicUrl)
                println(publicUrl.toString())

            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }
}