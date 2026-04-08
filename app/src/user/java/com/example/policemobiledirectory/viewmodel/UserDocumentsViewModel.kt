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
) : BaseMediaViewModel<Document>("UserDocumentsViewModel", "documents") {

    val documents: StateFlow<List<Document>> = items
    val documentsStatus: StateFlow<OperationStatus<List<Document>>> = status

    /**
     * Get a unique identifier for a document (its resolved URL)
     */
    override fun getItemIdentifier(item: Document): String = item.resolvedUrl ?: ""


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

    /**
     * Mark a document as broken. This will hide it from the UI.
     */
    fun markDocumentAsBroken(url: String) {
        hideItem(url)
    }


    fun clearStatus() {
        // No extra status to clear in User version
    }
}
