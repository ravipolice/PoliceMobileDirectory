package com.example.policemobiledirectory.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Employee(
    val kgid: String = "",
    val name: String = "",
    val email: String = "",
    val dateOfBirth: Date? = null, // ✅ Added DOB
    val pin: String? = null,
    val mobile1: String? = null,   // STRING after migration
    val mobile2: String? = null,
    val rank: String? = null,
    @get:PropertyName("metalNumber")
    @set:PropertyName("metalNumber")
    var metalNumber: String? = null,   // Firestore field: "metalNumber" (Google Sheet column: "metal")
    val district: String? = null,
    val station: String? = null,
    val subSection: String? = null,  // Duty role: Writer, Court, Summons, Crime, IO, etc.
    val bloodGroup: String? = null,
    val photoUrl: String? = null,
    val photoUrlFromGoogle: String? = null,
    val fcmToken: String? = null,
    val firebaseUid: String? = null,
    
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,

    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = true,

    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val unit: String? = null,
    val landline: String? = null,
    val landline2: String? = null,
    
    @get:PropertyName("isHidden")
    @set:PropertyName("isHidden")
    var isHidden: Boolean = false,

    @get:PropertyName("isManualStation")
    @set:PropertyName("isManualStation")
    var isManualStation: Boolean = false,

    @get:PropertyName("isManualSubSection")
    @set:PropertyName("isManualSubSection")
    var isManualSubSection: Boolean = false,

    val dutyRole: String? = null,
    val gender: String = "Male",
    val serviceStartDate: Date? = null,
    @get:Exclude
    val searchBlob: String = ""
) {
    // Computed property for display
    val displayRank: String
        get() {
            val currentRank = rank ?: ""
            return if (currentRank.isNotBlank()) {
                // Always show metal number with rank if metal number exists
                if (!metalNumber.isNullOrBlank()) {
                    "$currentRank $metalNumber"
                } else {
                    currentRank
                }
            } else {
                ""
            }
        }

    /**
     * ✅ Effective Unit: Hybrid Strategy
     * 1. Use explicit `unit` field if available.
     * 2. Fallback: Derive from `station` name using keywords.
     */
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

    /**
     * ✅ Case-insensitive search matching
     * Supports filters: name, kgid, rank, mobile, district, station, email
     */
    fun matches(query: String, filter: String): Boolean {
        // Legacy wrapper
        return matchesOptimized(query.trim().lowercase(), filter)
    }

    /**
     * Optimized matching function (query is already lowercase)
     */
    fun matchesOptimized(queryLower: String, filter: String): Boolean {
        // Use rich searchBlob for general searches if available
        // This enables fuzzy matching (e.g. "bmravi" -> "B.M. Ravi")
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
            "kgid" -> kgid.lowercase().contains(queryLower)
            "rank" -> (rank ?: "").lowercase().contains(queryLower)
            "mobile" -> listOfNotNull(mobile1, mobile2).any { it.lowercase().contains(queryLower) }
            "district" -> (district ?: "").lowercase().contains(queryLower)
            "station" -> (station ?: "").lowercase().contains(queryLower)
            "email" -> email.lowercase().contains(queryLower)
            "metal" -> (metalNumber ?: "").lowercase().contains(queryLower)
            "blood" -> (bloodGroup ?: "").lowercase().contains(queryLower)
            else -> listOfNotNull(
                name, kgid, rank, mobile1, mobile2, district, station, email, metalNumber, bloodGroup, unit, effectiveUnit
            ).any { it.lowercase().contains(queryLower) }
        }
    }
}
