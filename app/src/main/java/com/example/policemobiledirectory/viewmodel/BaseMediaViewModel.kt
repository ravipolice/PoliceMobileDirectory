package com.example.policemobiledirectory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.utils.ErrorHandler
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for Media-related screens (Documents, Gallery).
 * Handles common functionality like caching, state management, and item filtering.
 */
abstract class BaseMediaViewModel<T>(
    protected val tag: String,
    protected val operationPrefix: String
) : ViewModel() {

    protected val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    protected val _status = MutableStateFlow<OperationStatus<List<T>>>(OperationStatus.Idle)
    val status: StateFlow<OperationStatus<List<T>>> = _status.asStateFlow()

    // Caching
    protected var cachedItems: List<T>? = null
    protected var cacheTimestamp: Long = 0
    protected open val cacheDurationMs = 5 * 60 * 1000L // Default 5 minutes

    // Hidden/Broken items identifiers (e.g., titles or IDs)
    protected val hiddenItemIdentifiers = mutableSetOf<String>()

    // Computed properties for convenience
    val isLoading: Boolean get() = _status.value is OperationStatus.Loading
    val error: String? get() = (_status.value as? OperationStatus.Error)?.message

    /**
     * Get a unique identifier for an item (e.g., title or URL)
     */
    protected abstract fun getItemIdentifier(item: T): String

    /**
     * Fetch items from the repository
     */
    protected abstract suspend fun fetchFromRepository(): List<T>?

    /**
     * Fetch items with caching, error handling, and performance tracking
     */
    fun fetchItems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Return cached data if available and not expired
            if (!forceRefresh && cachedItems != null && 
                (System.currentTimeMillis() - cacheTimestamp) < cacheDurationMs) {
                applyFilterAndEmit(cachedItems!!)
                _status.value = OperationStatus.Success(_items.value)
                return@launch
            }

            // Show loading state if we have no data
            if (_items.value.isEmpty()) {
                _status.value = OperationStatus.Loading
            }
            
            try {
                val fetched = PerformanceLogger.measureNetworkOperation(operationPrefix, "GET") {
                    fetchFromRepository()
                }
                
                val itemList = fetched ?: emptyList()
                
                // Update cache
                cachedItems = itemList
                cacheTimestamp = System.currentTimeMillis()
                
                applyFilterAndEmit(itemList)
                _status.value = OperationStatus.Success(_items.value)
                
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e, "$tag.fetchItems")
                
                // Return cached data if available, even if expired
                if (cachedItems != null) {
                    applyFilterAndEmit(cachedItems!!)
                    _status.value = OperationStatus.Error(
                        "Using cached data. ${errorInfo.userFriendlyMessage}"
                    )
                } else {
                    _status.value = OperationStatus.Error(errorInfo.userFriendlyMessage)
                }
                
                // Retry if error is retryable
                if (errorInfo.shouldRetry) {
                    delay(errorInfo.retryDelay)
                    fetchItems(forceRefresh = true)
                }
            }
        }
    }

    /**
     * Filters the raw list by hiding items in the hidden set and updates the state
     */
    protected fun applyFilterAndEmit(rawList: List<T>) {
        _items.value = rawList.filter { !hiddenItemIdentifiers.contains(getItemIdentifier(it)) }
    }

    /**
     * Invalidate cache to force refresh on next fetch
     */
    protected fun invalidateCache() {
        cachedItems = null
        cacheTimestamp = 0
    }

    /**
     * Mark an item as hidden/broken
     */
    protected fun hideItem(identifier: String) {
        if (identifier.isBlank()) return
        if (hiddenItemIdentifiers.add(identifier)) {
            applyFilterAndEmit(cachedItems ?: _items.value)
        }
    }
}
