package com.example.policemobiledirectory.model

/**
 * Sealed class representing the different types of search suggestions:
 * - [History]          : A recently typed query (from the last 5 searches).
 * - [ContactSuggestion]: A prefix-matched contact name from the in-memory list.
 * - [FilterSuggestion] : A matching station or rank value for quick filter application.
 */
sealed class SearchSuggestion {
    /** A previously typed search query from history. */
    data class History(val query: String) : SearchSuggestion()

    /** A contact name / subtitle that starts with the current query. */
    data class ContactSuggestion(
        val id: String,
        val name: String,
        val subtitle: String,
        val isOfficer: Boolean
    ) : SearchSuggestion()

    /** A filter value (station or rank) matching the current query. */
    data class FilterSuggestion(
        val text: String,
        val type: String // "Station" or "Rank"
    ) : SearchSuggestion()
}
