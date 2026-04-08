package com.example.policemobiledirectory.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

/**
 * Officer - Read-only contact information for police officers
 * Admin-managed, no authentication required
 * Separate from Employee collection
 */
data class Officer(
    val agid: String = "",           // Auto-generated ID (from Apps Script)
    val name: String = "",
    val email: String? = null,
    val bloodGroup: String? = null,
    val mobile: String? = null,
    val mobile2: String? = null,
    val landline: String? = null,
    val landline2: String? = null,
    val rank: String? = null,
    val station: String? = null,
    val district: String? = null,
    val photoUrl: String? = null,
    val unit: String? = null,
    val office: String? = null,
    @ServerTimestamp
    val createdAt: java.util.Date? = null,
    @ServerTimestamp
    val updatedAt: java.util.Date? = null,
    @get:PropertyName("isHidden")
    val isHidden: Boolean = false,
    val subDivision: String? = null,
    @get:Exclude
    val searchBlob: String = ""
) {
    /**
     * ✅ Effective Unit: Hybrid Strategy
     */
    @get:Exclude
    val effectiveUnit: String
        get() {
            if (!unit.isNullOrBlank()) return unit
            
            val stationName = station ?: ""
            return when {
                listOf("Traffic").any { stationName.contains(it, ignoreCase = true) } -> "Traffic"
                Regex("\\b(Control Room)\\b", RegexOption.IGNORE_CASE).containsMatchIn(stationName) -> "C Room"
                Regex("\\b(CEN|Cyber)\\b", RegexOption.IGNORE_CASE).containsMatchIn(stationName) -> "CEN"
                listOf("Women").any { stationName.contains(it, ignoreCase = true) } -> "Women"
                listOf("DPO", "Computer", "Admin", "Office").any { stationName.contains(it, ignoreCase = true) } -> "Admin"
                Regex("\\bDAR\\b", RegexOption.IGNORE_CASE).containsMatchIn(stationName) -> "DAR"
                listOf("DCRB").any { stationName.contains(it, ignoreCase = true) } -> "DCRB"
                Regex("\\b(DSB|Intelligence|INT)\\b", RegexOption.IGNORE_CASE).containsMatchIn(stationName) -> "DSB"
                listOf("FPB", "MCU", "SMMC", "DCRE", "Lokayukta", "ESCOM").any { stationName.contains(it, ignoreCase = true) } -> "Special"
                else -> "L&O"
            }
        }

    @get:Exclude
    val primaryPhone: String?
        get() = mobile ?: landline

    @get:Exclude
    val secondaryPhone: String?
        get() = if (!mobile.isNullOrBlank() && !landline.isNullOrBlank()) landline else null

    /**
     * Optimized matching function (query is already lowercase)
     * Supports filters: name, agid, rank, mobile, district, station, email
     */
    fun matchesOptimized(queryLower: String, filter: String): Boolean {
        // Use rich searchBlob for general searches if available
        if (filter.equals("name", ignoreCase = true) || filter.equals("all", ignoreCase = true)) {
            if (searchBlob.isNotEmpty() && searchBlob.contains(queryLower)) {
                return true
            }
        }

        return when (filter.lowercase()) {
            "name" -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            "agid" -> {
                val agidLower = agid.lowercase()
                agidLower.startsWith(queryLower) || agidLower.contains(queryLower)
            }
            "rank" -> (rank ?: "").lowercase().contains(queryLower)
            "mobile" -> listOfNotNull(mobile, landline).any { it.lowercase().contains(queryLower) }
            "district" -> (district ?: "").lowercase().contains(queryLower)
            "station" -> (station ?: "").lowercase().contains(queryLower)
            "email" -> (email ?: "").lowercase().contains(queryLower)
            "blood" -> (bloodGroup ?: "").lowercase().contains(queryLower)
            else -> listOfNotNull(
                name, agid, rank, mobile, landline, district, station, subDivision, email, bloodGroup, unit, effectiveUnit
            ).any { it.lowercase().contains(queryLower) }
        }
    }
    
    /**
     * Legacy function for backward compatibility
     */
    fun matches(query: String, filter: String): Boolean {
        return matchesOptimized(query.trim().lowercase(), filter)
    }
}
