package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.data.remote.GalleryApiService
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Response
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    private val api: GalleryApiService,
    private val securityConfig: SecurityConfig,
    private val firestore: FirebaseFirestore
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

    suspend fun fetchGalleryImages(): List<GalleryImage> = withContext(Dispatchers.IO) {
        val firestoreImages = try {
            val images = fetchFromFirestore()
            android.util.Log.d("GalleryRepository", "✅ Fetched ${images.size} images from Firestore")
            images
        } catch (e: Exception) {
            android.util.Log.e("GalleryRepository", "❌ Firestore fetch failed: ${e.message}")
            emptyList()
        }

        val appsScriptImages = try {
            val images = fetchFromAppsScript()
            android.util.Log.d("GalleryRepository", "✅ Fetched ${images.size} images from Apps Script")
            images
        } catch (e: Exception) {
            android.util.Log.e("GalleryRepository", "❌ Apps Script fetch failed: ${e.message}")
            emptyList()
        }

        // Merge and deduplicate by URL
        val combined = (appsScriptImages + firestoreImages)
            .filter { it.isValid && !it.isDeleted }
            .distinctBy { (it.resolvedUrl ?: it.displayUrl ?: it.imageUrl)?.trim()?.lowercase() }

        android.util.Log.d("GalleryRepository", "📊 Total merged images: ${combined.size}")
        
        // Final filter for optimistic deletion markers
        combined.filter { image ->
            val url = image.resolvedUrl ?: image.displayUrl ?: image.imageUrl
            url == null || !deletingUrls.contains(url)
        }
    }

    private suspend fun fetchFromAppsScript(): List<GalleryImage> {
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

        android.util.Log.v("GalleryRepository", "Raw Apps Script response: $bodyStr")

        return try {
            // Try parsing as raw array first
            val listType = object : TypeToken<List<GalleryImage>>() {}.type
            val images: List<GalleryImage> = gson.fromJson<List<GalleryImage>>(bodyStr, listType)
            images.map { it.copy(source = it.source ?: "gdrive") }
        } catch (e: Exception) {
            android.util.Log.w("GalleryRepository", "Failed to parse as raw array, trying object wrapper: ${e.message}")
            
            // Try parsing as { success: true, data: [...] } or { images: [...] }
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root: Map<String, Any> = gson.fromJson(bodyStr, mapType)
            
            val dataElement = root["data"] ?: root["images"] ?: root["items"]
            if (dataElement != null) {
                val dataJson = gson.toJson(dataElement)
                val listType = object : TypeToken<List<GalleryImage>>() {}.type
                val images: List<GalleryImage> = gson.fromJson(dataJson, listType)
                images.map { it.copy(source = it.source ?: "gdrive") }
            } else {
                android.util.Log.e("GalleryRepository", "Could not find image array in object response")
                emptyList()
            }
        }
    }

    private suspend fun fetchFromFirestore(): List<GalleryImage> {
        return try {
            val snapshot = firestore.collection("gallery")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
                    GalleryImage(
                        id = doc.id,
                        imageUrl = imageUrl,
                        title = doc.getString("title"),
                        description = doc.getString("description"),
                        category = doc.getString("category") ?: "Gallery",
                        uploadedBy = doc.getString("uploadedBy"),
                        uploadedDate = doc.getTimestamp("createdAt")?.toDate()?.toString(),
                        delete = if (doc.getBoolean("isDeleted") == true) "deleted" else null,
                        source = "firebase"
                    )
                } catch (e: Exception) {
                    android.util.Log.w("GalleryRepository", "Error parsing Firestore image ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GalleryRepository", "Error fetching from Firestore", e)
            emptyList()
        }
    }

    suspend fun uploadGalleryImage(request: GalleryUploadRequest) =
        api.uploadGalleryImage(token = token(), request = request)

    suspend fun deleteGalleryImage(request: GalleryDeleteRequest) =
        api.deleteGalleryImage(token = token(), request = request)
}
