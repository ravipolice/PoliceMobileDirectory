package com.example.policemobiledirectory.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("error")
    val error: String? = null,
    
    @SerializedName("data")
    val data: T? = null,

    @SerializedName("url")
    val url: String? = null,
    
    @SerializedName("id")
    val id: String? = null
)
