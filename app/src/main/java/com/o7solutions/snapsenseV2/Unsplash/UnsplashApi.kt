package com.o7solutions.snapsenseV2.Unsplash

import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApi {
    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("client_id") clientId: String = "BufxYkX4X2YgwFxhMCjR5TTfgwRz2xciMZDIj0iWf7c"
    ): UnsplashResponse
}