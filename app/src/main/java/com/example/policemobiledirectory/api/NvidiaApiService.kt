package com.example.policemobiledirectory.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NvidiaApiService {

    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: NvidiaChatRequest
    ): Response<NvidiaChatResponse>
}
