package com.example.policemobiledirectory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.model.*
import com.example.policemobiledirectory.repository.DocumentsRepository
import com.example.policemobiledirectory.utils.ErrorHandler
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import kotlinx.coroutines.flow.first

@HiltViewModel
class UserDocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // State management with OperationStatus
    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _documentsStatus = MutableStateFlow<OperationStatus<List<Document>>>(OperationStatus.Idle)
    val documentsStatus: StateFlow<OperationStatus<List<Document>>> = _documentsStatus.asStateFlow()



    // In-memory cache with timestamp
    private var cachedDocuments: List<Document>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 30 * 1000L // Reduced to 30 seconds for faster sync

    // 🚫 Track broken documents that failed to load
    private val brokenDocTitles = mutableSetOf<String>()

    // Computed properties for convenience
    val isLoading: Boolean get() = _documentsStatus.value is OperationStatus.Loading
    val error: String? get() = (_documentsStatus.value as? OperationStatus.Error)?.message

    fun clearStatus() {
        // Only document load status clearing if needed
    }

    /**
     * Fetch documents with caching, error handling, and performance tracking
     */
    fun fetchDocuments(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Return cached data if available and not expired
            if (!forceRefresh && cachedDocuments != null && 
                (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS) {
                // Apply broken doc filter to cached data
                val filteredCache = cachedDocuments!!.filter { !brokenDocTitles.contains(it.resolvedTitle) }
                _documents.value = filteredCache
                _documentsStatus.value = OperationStatus.Success(filteredCache)
                return@launch
            }

            _documentsStatus.value = OperationStatus.Loading
            
            try {
                val docs = PerformanceLogger.measureNetworkOperation("documents", "GET") {
                    repository.fetchDocuments()
                }
                
                val docList = docs ?: emptyList()
                
                // Update cache
                cachedDocuments = docList
                cacheTimestamp = System.currentTimeMillis()
                
                // ✅ Filter out broken docs
                val filteredList = docList.filter { !brokenDocTitles.contains(it.resolvedTitle) }
                
                _documents.value = filteredList
                _documentsStatus.value = OperationStatus.Success(filteredList)
                
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "UserDocumentsViewModel.fetchDocuments")
                
                // Return cached data if available, even if expired
                if (cachedDocuments != null) {
                    val filteredCache = cachedDocuments!!.filter { !brokenDocTitles.contains(it.resolvedTitle) }
                    _documents.value = filteredCache
                    _documentsStatus.value = OperationStatus.Error(
                        "Using cached data. ${errorInfo.userFriendlyMessage}"
                    )
                } else {
                    _documentsStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
                }
                
                // Retry if error is retryable
                if (errorInfo.shouldRetry) {
                    delay(errorInfo.retryDelay)
                    fetchDocuments(forceRefresh = true)
                }
            }
        }
    }

    /**
     * Mark a document as broken (failed to load). This will hide it from the UI.
     */
    fun markDocumentAsBroken(title: String) {
        if (title.isBlank()) return
        
        if (brokenDocTitles.add(title)) {
            android.util.Log.d("UserDocumentsViewModel", "🚫 Document marked as broken: $title")
            // Re-filter the current list
            val currentList = _documents.value
            _documents.value = currentList.filter { it.resolvedTitle != title }
        }
    }

}
