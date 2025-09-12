package com.o7solutions.snapsense.Cloudinary

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils

object CloudinaryConfig {
    private const val CLOUD_NAME = "drjppkkrp"
    private const val API_KEY = "563857781683946"
    private const val API_SECRET = "QOYApxCq0fs2YfKLWXSX7FIE_GA"

    val instance: Cloudinary by lazy {
        Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME,
                "api_key", API_KEY,
                "api_secret", API_SECRET
            )
        )
    }
}
