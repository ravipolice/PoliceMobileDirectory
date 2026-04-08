package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.model.ExternalLinkInfo
import com.example.policemobiledirectory.repository.AppIconRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UsefulLinksViewModel @Inject constructor(
    private val appIconRepository: AppIconRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _usefulLinks = MutableStateFlow<List<ExternalLinkInfo>>(emptyList())
    val usefulLinks: StateFlow<List<ExternalLinkInfo>> = _usefulLinks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchUsefulLinks()
    }

    fun fetchUsefulLinks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val collection = firestore.collection("useful_links")
                val snapshot = try {
                    collection.get(Source.SERVER).await()
                } catch (serverError: Exception) {
                    Log.w("UsefulLinks", "Server fetch failed, falling back to cache")
                    collection.get(Source.CACHE).await()
                }

                _usefulLinks.value = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null
                    link.copy(documentId = doc.id)
                }

                // Fetch icons in background
                val updatedLinks = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null

                    val icon = if (!link.playStoreUrl.isNullOrBlank()) {
                        try {
                            val fetched = appIconRepository.getOrFetchAppIcon(link.playStoreUrl)
                            if (!fetched.isNullOrBlank() && link.iconUrl != fetched) {
                                try {
                                    collection.document(doc.id).update("iconUrl", fetched).await()
                                } catch (e: Exception) {
                                    Log.w("IconUpdate", "Failed to save icon for ${link.name}")
                                }
                                fetched
                            } else {
                                link.iconUrl
                            }
                        } catch (e: Exception) {
                            link.iconUrl
                        }
                    } else {
                        link.iconUrl
                    }

                    link.copy(iconUrl = icon, documentId = doc.id)
                }

                _usefulLinks.value = updatedLinks
            } catch (e: Exception) {
                Log.e("Firestore", "Failed to fetch useful links: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
