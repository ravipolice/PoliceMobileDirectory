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
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // State management with OperationStatus
    private val _galleryImages = MutableStateFlow<List<GalleryImage>>(emptyList())
    val galleryImages: StateFlow<List<GalleryImage>> = _galleryImages.asStateFlow()

    private val _galleryStatus = MutableStateFlow<OperationStatus<List<GalleryImage>>>(OperationStatus.Idle)
    val galleryStatus: StateFlow<OperationStatus<List<GalleryImage>>> = _galleryStatus.asStateFlow()

    // In-memory cache with timestamp
    private var cachedImages: List<GalleryImage>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 60 * 1000L // Reduced to 1 minute for better sync

    // 🚫 Track broken images that failed to load
    private val brokenImageUrls = mutableSetOf<String>()

    // Computed properties for convenience
    val isLoading: Boolean get() = _galleryStatus.value is OperationStatus.Loading
    val error: String? get() = (_galleryStatus.value as? OperationStatus.Error)?.message

    /**
     * Fetch gallery images with caching, error handling, and performance tracking
     */
    fun fetchGalleryImages(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Return cached data if available and not expired
            if (!forceRefresh && cachedImages != null && 
                (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS) {
                _galleryImages.value = cachedImages!!
                _galleryStatus.value = OperationStatus.Success(cachedImages!!)
                return@launch
            }

            _galleryStatus.value = OperationStatus.Loading
            
            try {
                // 1. Fetch from Apps Script
                val images = PerformanceLogger.measureNetworkOperation("gallery", "GET") {
                    repository.fetchGalleryImages()
                }
                
                // 2. Filter out broken images
                val imageList = (images ?: emptyList()).filter { image ->
                    val url = image.resolvedUrl ?: image.displayUrl
                    val isNotBroken = url == null || !brokenImageUrls.contains(url)
                    isNotBroken
                }
                
                // Update cache
                cachedImages = imageList
                cacheTimestamp = System.currentTimeMillis()
                
                _galleryImages.value = imageList
                _galleryStatus.value = OperationStatus.Success(imageList)
                
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "GalleryViewModel.fetchGalleryImages")
                
                // Return cached data if available, even if expired
                if (cachedImages != null) {
                    _galleryImages.value = cachedImages!!
                    _galleryStatus.value = OperationStatus.Error(
                        "Using cached data. ${errorInfo.userFriendlyMessage}"
                    )
                } else {
                    _galleryStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
                }
                
                // Retry if error is retryable
                if (errorInfo.shouldRetry) {
                    delay(errorInfo.retryDelay)
                    fetchGalleryImages(forceRefresh = true)
                }
            }
        }
    }

    /**
     * Mark an image as broken (failed to load). This will hide it from the UI.
     */
    fun markImageAsBroken(url: String) {
        if (url.isBlank()) return
        
        if (brokenImageUrls.add(url)) {
            android.util.Log.d("GalleryViewModel", "🚫 Image marked as broken: $url")
            // Re-filter the current list
            val currentList = _galleryImages.value
            _galleryImages.value = currentList.filter { 
                val imgUrl = it.resolvedUrl ?: it.displayUrl
                imgUrl != url 
            }
        }
    }
}

