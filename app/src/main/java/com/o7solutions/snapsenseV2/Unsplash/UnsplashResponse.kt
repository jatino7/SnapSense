package com.o7solutions.snapsenseV2.Unsplash


data class UnsplashResponse(
    val results: List<UnsplashPhoto>
)

data class UnsplashPhoto(
    val id: String,
    val urls: Urls,
    val user: User
)

data class Urls(
    val small: String,
    val regular: String
)

data class User(
    val name: String
)
