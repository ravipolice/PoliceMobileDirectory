package com.example.policemobiledirectory.model

import com.google.gson.annotations.SerializedName

data class DocumentUploadRequest(
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

data class DocumentEditRequest(
    @SerializedName("oldTitle")
    val oldTitle: String,
    
    @SerializedName("newTitle")
    val newTitle: String? = null,
    
    @SerializedName("category")
    val category: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("userEmail")
    val userEmail: String? = null
)

data class DocumentDeleteRequest(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("userEmail")
    val userEmail: String? = null
)
