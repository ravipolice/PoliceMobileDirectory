package com.example.policemobiledirectory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.repository.GalleryRepository
import com.example.policemobiledirectory.utils.ErrorHandler
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

import com.example.policemobiledirectory.viewmodel.BaseMediaViewModel

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore
) : BaseMediaViewModel<GalleryImage>("GalleryViewModel", "gallery") {

    val galleryImages: StateFlow<List<GalleryImage>> = items
    val galleryStatus: StateFlow<OperationStatus<List<GalleryImage>>> = status

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()

    private val _deleteStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val deleteStatus: StateFlow<OperationStatus<String>> = _deleteStatus.asStateFlow()

    /**
     * Get a unique identifier for a gallery image (its resolved or display URL)
     */
    override fun getItemIdentifier(item: GalleryImage): String = 
        item.resolvedUrl ?: item.displayUrl ?: ""

    /**
     * Fetch gallery images from the repository
     */
    override suspend fun fetchFromRepository(): List<GalleryImage>? = repository.fetchGalleryImages()

    /**
     * Backward-compatible fetch method
     */
    fun fetchGalleryImages(forceRefresh: Boolean = false) {
        fetchItems(forceRefresh)
    }

    fun clearStatus() {
        _uploadStatus.value = OperationStatus.Idle
        _deleteStatus.value = OperationStatus.Idle
    }

    /**
     * Upload gallery image with error handling and performance tracking
     */
    fun uploadGalleryImage(
        title: String,
        fileBase64: String,
        mimeType: String,
        category: String?,
        description: String?
    ) {
        viewModelScope.launch {
            _uploadStatus.value = OperationStatus.Loading
            
            try {
                val userEmail = sessionManager.userEmail.first()
                val request = GalleryUploadRequest(
                    title = title,
                    fileBase64 = fileBase64,
                    mimeType = mimeType,
                    category = category,
                    description = description,
                    userEmail = userEmail
                )
                
                val response = PerformanceLogger.measureNetworkOperation<ApiResponse<GalleryUploadResponse>>("gallery/upload", "POST") {
                    repository.uploadGalleryImage(request)
                }
                
                if (response.success) {
                    _uploadStatus.value = OperationStatus.Success("Image uploaded successfully")
                    
                    // Sync to Firestore for real-time updates (non-blocking)
                    val imageUrl = response.url ?: ""
                    if (imageUrl.isNotBlank()) {
                        launch {
                            syncToFirestore(imageUrl)
                        }
                    }
                    
                    // Invalidate cache and refresh
                    invalidateCache()
                    // Retry fetch with exponential backoff if needed
                    retryFetchWithBackoff()
                } else {
                    val errorMsg = response.error ?: "Upload failed"
                    _uploadStatus.value = OperationStatus.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "GalleryViewModel.uploadGalleryImage")
                _uploadStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }

    /**
     * Delete gallery image with optimistic update and error handling
     */
    fun deleteGalleryImage(title: String) {
        viewModelScope.launch {
            _deleteStatus.value = OperationStatus.Loading
            
            // Optimistic update
            val imageToDelete = _items.value.find { getItemIdentifier(it) == title }
            val urlToDelete = imageToDelete?.let { getItemIdentifier(it) }
            
            // Mark as pending deletion in repository (Singleton)
            urlToDelete?.let { repository.markAsDeleting(it) }
            
            hideItem(title)
            
            try {
                val userEmail = sessionManager.userEmail.first()
                val request = GalleryDeleteRequest(
                    title = title,
                    url = urlToDelete,
                    userEmail = userEmail
                )
                
                val response = PerformanceLogger.measureNetworkOperation<ApiResponse<Unit>>("gallery/delete", "POST") {
                    repository.deleteGalleryImage(request)
                }
                
                if (response.success) {
                    _deleteStatus.value = OperationStatus.Success("Image deleted successfully")
                    
                    // Delete from Firestore (non-blocking)
                    urlToDelete?.let { url ->
                        launch {
                            deleteFromFirestore(url)
                        }
                    }
                    
                    // Invalidate cache and refresh
                    invalidateCache()
                    fetchGalleryImages(forceRefresh = true)
                } else {
                    // Revert optimistic update on failure
                    urlToDelete?.let { repository.clearDeletionMarker(it) }
                    hiddenItemIdentifiers.remove(title)
                    applyFilterAndEmit(cachedItems ?: emptyList())
                    
                    val errorMsg = response.error ?: "Delete failed"
                    _deleteStatus.value = OperationStatus.Error(errorMsg)
                }
            } catch (e: Exception) {
                // Revert optimistic update on failure
                urlToDelete?.let { repository.clearDeletionMarker(it) }
                hiddenItemIdentifiers.remove(title)
                applyFilterAndEmit(cachedItems ?: emptyList())
                
                val errorInfo = ErrorHandler.handleException(e, "GalleryViewModel.deleteGalleryImage")
                _deleteStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }
    
    /**
     * Retry fetch with exponential backoff
     */
    private suspend fun retryFetchWithBackoff(maxRetries: Int = 3) {
        var retryCount = 0
        var delayMs = 1000L
        
        while (retryCount < maxRetries) {
            delay(delayMs)
            try {
                fetchGalleryImages(forceRefresh = true)
                return // Success, exit retry loop
            } catch (e: Exception) {
                retryCount++
                delayMs *= 2 // Exponential backoff
            }
        }
    }

    // ✅ Sync uploaded image to Firestore for real-time updates
    private suspend fun syncToFirestore(imageUrl: String) {
        try {
            val data = hashMapOf(
                "imageUrl" to imageUrl,
                "uploadedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("gallery").add(data).await()
        } catch (e: Exception) {
            // Non-critical, just log
            android.util.Log.e("GalleryViewModel", "Failed to sync to Firestore: ${e.message}")
        }
    }

    // ✅ Delete from Firestore (search by URL)
    private suspend fun deleteFromFirestore(imageUrl: String) {
        try {
            val snapshot = firestore.collection("gallery")
                .whereEqualTo("imageUrl", imageUrl)
                .get()
                .await()
            snapshot.documents.forEach { it.reference.delete().await() }
        } catch (e: Exception) {
            android.util.Log.e("GalleryViewModel", "Failed to delete from Firestore: ${e.message}")
        }
    }
}

