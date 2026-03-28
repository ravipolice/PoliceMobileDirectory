package com.example.policemobiledirectory.model

import com.google.gson.annotations.SerializedName

data class GalleryUploadRequest(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("fileBase64")
    val fileBase64: String,
    
    @SerializedName("mimeType")
    val mimeType: String,
    
    @SerializedName("category")
    val category: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("userEmail")
    val userEmail: String? = null
)

data class GalleryUploadResponse(
    @SerializedName("url")
    val url: String? = null,
    
    @SerializedName("id")
    val id: String? = null
)

data class GalleryDeleteRequest(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("url")
    val url: String? = null,
    
    @SerializedName("userEmail")
    val userEmail: String? = null
)
