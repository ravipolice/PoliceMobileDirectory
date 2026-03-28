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
import com.example.policemobiledirectory.viewmodel.BaseMediaViewModel

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val sessionManager: SessionManager
) : BaseMediaViewModel<GalleryImage>("GalleryViewModel", "gallery") {

    val galleryImages: StateFlow<List<GalleryImage>> = items
    val galleryStatus: StateFlow<OperationStatus<List<GalleryImage>>> = status

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

    /**
     * Mark an image as broken. This will hide it from the UI.
     */
    fun markImageAsBroken(url: String) {
        hideItem(url)
    }
}

