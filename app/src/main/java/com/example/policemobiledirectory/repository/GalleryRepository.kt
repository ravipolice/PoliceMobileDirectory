package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.GalleryApiService
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    private val api: GalleryApiService,
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

    suspend fun fetchGalleryImages(): List<GalleryImage> {
        val response: Response<ResponseBody> = api.getGalleryImagesRaw(
            token = token(),
            nocache = System.currentTimeMillis().toString()
        )
        
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw IllegalStateException("HTTP ${response.code()}: $errorBody")
        }
        
        val bodyStr = response.body()?.string()
            ?: throw IllegalStateException("Empty gallery response")

        // Try parse as array first
        val images: List<GalleryImage> = try {
            val listType = object : TypeToken<List<GalleryImage>>() {}.type
            gson.fromJson<List<GalleryImage>>(bodyStr, listType)
        } catch (e: Exception) {
            android.util.Log.w("GalleryRepository", "Failed to parse as array: ${e.message}")
            
            data class GalleryApiResponse(
                val success: Boolean? = null,
                val data: List<GalleryImage>? = null,
                val error: String? = null,
                val message: String? = null
            )

            val obj = try {
                gson.fromJson(bodyStr, GalleryApiResponse::class.java)
            } catch (e2: Exception) {
                throw IllegalStateException("Unable to parse gallery response: ${e2.message}")
            }

            if (obj.success == false || (obj.error != null || obj.message != null)) {
                val err = obj.error ?: obj.message ?: "Gallery load failed"
                throw IllegalStateException(err)
            }
            obj.data ?: emptyList()
        }

        // ✅ Filter by isValid, !isDeleted AND optimistic deletion markers
        return images.filter { image ->
            image.isValid && !image.isDeleted && run {
                val url = image.resolvedUrl ?: image.displayUrl
                url == null || !deletingUrls.contains(url)
            }
        }
    }

    suspend fun uploadGalleryImage(request: GalleryUploadRequest) =
        api.uploadGalleryImage(token = token(), request = request)

    suspend fun deleteGalleryImage(request: GalleryDeleteRequest) =
        api.deleteGalleryImage(token = token(), request = request)
}
