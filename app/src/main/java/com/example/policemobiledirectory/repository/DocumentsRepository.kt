package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.DocumentsApiService
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentsRepository @Inject constructor(
    private val api: DocumentsApiService,
    private val securityConfig: SecurityConfig
) {
    private fun token() = securityConfig.getSecretToken()
    private val gson = Gson()

    private val deletingUrls = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun markAsDeleting(url: String) {
        deletingUrls.add(url)
    }

    fun clearDeletionMarker(url: String) {
        deletingUrls.remove(url)
    }

    suspend fun fetchDocuments(): List<Document> {
        val response: Response<ResponseBody> = api.getDocumentsRaw(
            token = token(),
            nocache = System.currentTimeMillis().toString()
        )
        
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw IllegalStateException("HTTP ${response.code()}: $errorBody")
        }
        
        val bodyStr = response.body()?.string()
            ?: throw IllegalStateException("Empty documents response")

        // Try parse as array first
        try {
            val listType = object : TypeToken<List<Document>>() {}.type
            val docs = gson.fromJson<List<Document>>(bodyStr, listType)
            // Filter by isValid AND !isDeleted for consistency AND optimistic deletion markers
            return docs.filter { doc -> 
                doc.isValid && !doc.isDeleted && run {
                    val url = doc.resolvedUrl
                    url == null || !deletingUrls.contains(url)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DocumentsRepository", "Failed to parse as array: ${e.message}")
        }

        data class DocumentsApiResponse(
            val success: Boolean? = null,
            val data: List<Document>? = null,
            val error: String? = null,
            val message: String? = null
        )

        val obj = try {
            gson.fromJson(bodyStr, DocumentsApiResponse::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Unable to parse documents response: ${e.message}")
        }

        if (obj.success == false || (obj.error != null || obj.message != null)) {
            val err = obj.error ?: obj.message ?: "Documents load failed"
            throw IllegalStateException(err)
        }

        return obj.data?.filter { doc -> 
            doc.isValid && !doc.isDeleted && run {
                val url = doc.resolvedUrl
                url == null || !deletingUrls.contains(url)
            }
        } ?: emptyList()
    }

    suspend fun uploadDocument(request: DocumentUploadRequest) =
        api.uploadDocument(token = token(), request = request)

    suspend fun editDocument(request: DocumentEditRequest) =
        api.editDocument(token = token(), request = request)

    suspend fun deleteDocument(request: DocumentDeleteRequest) =
        api.deleteDocument(token = token(), request = request)
}
