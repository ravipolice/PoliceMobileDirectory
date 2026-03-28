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
class AdminDocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository,
    private val sessionManager: SessionManager
) : BaseMediaViewModel<Document>("AdminDocumentsViewModel", "documents") {

    val documents: StateFlow<List<Document>> = items
    val documentsStatus: StateFlow<OperationStatus<List<Document>>> = status

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()

    private val _deleteStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val deleteStatus: StateFlow<OperationStatus<String>> = _deleteStatus.asStateFlow()

    private val _editStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val editStatus: StateFlow<OperationStatus<String>> = _editStatus.asStateFlow()

    /**
     * Get a unique identifier for a document (its resolved title)
     */
    override fun getItemIdentifier(item: Document): String = item.resolvedTitle

    /**
     * Fetch documents from the repository
     */
    override suspend fun fetchFromRepository(): List<Document>? = repository.fetchDocuments()

    /**
     * Backward-compatible fetch method
     */
    fun fetchDocuments(forceRefresh: Boolean = false) {
        fetchItems(forceRefresh)
    }

    fun clearStatus() {
        _uploadStatus.value = OperationStatus.Idle
        _deleteStatus.value = OperationStatus.Idle
        _editStatus.value = OperationStatus.Idle
    }

    /**
     * Upload document with error handling and performance tracking
     */
    fun uploadDocument(
        title: String,
        fileBase64: String,
        mimeType: String,
        category: String?,
        description: String?
    ) {
        viewModelScope.launch {
            _uploadStatus.value = OperationStatus.Loading
            
            try {
                // Remove from hide list if re-uploading
                hiddenItemIdentifiers.remove(title)

                val userEmail = sessionManager.userEmail.first()
                val request = DocumentUploadRequest(
                    title = title,
                    fileBase64 = fileBase64,
                    mimeType = mimeType,
                    category = category,
                    description = description,
                    userEmail = userEmail
                )
                
                val response = PerformanceLogger.measureNetworkOperation<ApiResponse<Unit>>("documents/upload", "POST") {
                    repository.uploadDocument(request)
                }
                
                if (response.success) {
                    _uploadStatus.value = OperationStatus.Success("Document uploaded successfully")
                    
                    // Invalidate cache and refresh
                    invalidateCache()
                    retryFetchWithBackoff()
                } else {
                    val errorMsg = response.error ?: "Upload failed"
                    _uploadStatus.value = OperationStatus.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "AdminDocumentsViewModel.uploadDocument")
                _uploadStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }

    /**
     * Edit document with optimistic update and error handling
     */
    fun editDocument(
        oldTitle: String,
        newTitle: String?,
        category: String?,
        description: String?
    ) {
        viewModelScope.launch {
            _editStatus.value = OperationStatus.Loading
            
            try {
                // Update local delete list if title changed
                if (newTitle != null && newTitle != oldTitle) {
                     hiddenItemIdentifiers.remove(newTitle)
                }

                val userEmail = sessionManager.userEmail.first()
                val request = DocumentEditRequest(
                    oldTitle = oldTitle,
                    newTitle = newTitle,
                    category = category,
                    description = description,
                    userEmail = userEmail
                )
                
                PerformanceLogger.measureNetworkOperation<ApiResponse<Unit>>("documents/edit", "POST") {
                    repository.editDocument(request)
                }
                
                _editStatus.value = OperationStatus.Success("Document updated successfully")
                
                // Invalidate cache and refresh
                invalidateCache()
                fetchDocuments(forceRefresh = true)
            } catch (e: Exception) {
                // Revert by refreshing from server
                fetchDocuments(forceRefresh = true)
                val errorInfo = ErrorHandler.handleException(e, "AdminDocumentsViewModel.editDocument")
                _editStatus.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
            }
        }
    }

    /**
     * Delete document with optimistic update and error handling
     */
    fun deleteDocument(title: String) {
        viewModelScope.launch {
            _deleteStatus.value = OperationStatus.Loading
            
            // Optimistic update
            val documentToDelete = _items.value.find { getItemIdentifier(it) == title }
            hideItem(title)
            
            try {
                val userEmail = sessionManager.userEmail.first()
                val request = DocumentDeleteRequest(
                    title = title,
                    userEmail = userEmail
                )
                
                PerformanceLogger.measureNetworkOperation<ApiResponse<Unit>>("documents/delete", "POST") {
                    repository.deleteDocument(request)
                }
                
                _deleteStatus.value = OperationStatus.Success("Document deleted successfully")
                
                // Invalidate cache
                invalidateCache()

            } catch (e: Exception) {
                // Revert optimistic update on failure - tricky since we use hiddenItemIdentifiers
                hiddenItemIdentifiers.remove(title)
                applyFilterAndEmit(cachedItems ?: emptyList())
                
                val errorInfo = ErrorHandler.handleException(e, "AdminDocumentsViewModel.deleteDocument")
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
                fetchDocuments(forceRefresh = true)
                return // Success, exit retry loop
            } catch (e: Exception) {
                retryCount++
                delayMs *= 2 // Exponential backoff
            }
        }
    }
}
