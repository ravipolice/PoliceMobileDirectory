package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.data.local.SearchFilter
import androidx.compose.ui.graphics.Color

/**
 * Utility functions for Employee data (Top-level functions for compatibility)
 */

/**
 * Filter employees based on query and search field
 */
fun filterEmployees(
    employees: List<Employee>,
    query: String,
    field: SearchFilter
): List<Employee> {
    if (query.isBlank()) return employees

    val lowerQuery = query.lowercase()

    return employees.filter { emp ->
        when (field) {
            SearchFilter.ALL -> {
                emp.name.lowercase().contains(lowerQuery) ||
                emp.kgid.lowercase().contains(lowerQuery) ||
                emp.station?.lowercase()?.contains(lowerQuery) == true ||
                emp.rank?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile1?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile2?.lowercase()?.contains(lowerQuery) == true ||
                emp.metalNumber?.lowercase()?.contains(lowerQuery) == true ||
                emp.unit?.lowercase()?.contains(lowerQuery) == true ||
                emp.effectiveUnit.lowercase().contains(lowerQuery) ||
                (emp.bloodGroup ?: "").lowercase().contains(lowerQuery) || 
                getFormattedBloodGroup(emp.bloodGroup).lowercase().contains(lowerQuery)
            }
            SearchFilter.NAME -> emp.name.lowercase().contains(lowerQuery)
            SearchFilter.KGID -> emp.kgid.lowercase().contains(lowerQuery)
            SearchFilter.STATION -> emp.station?.lowercase()?.contains(lowerQuery) == true
            SearchFilter.RANK -> emp.rank?.lowercase()?.contains(lowerQuery) == true
            SearchFilter.MOBILE -> {
                emp.mobile1?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile2?.lowercase()?.contains(lowerQuery) == true
            }
            SearchFilter.BLOOD_GROUP -> {
                getFormattedBloodGroup(emp.bloodGroup).lowercase().contains(lowerQuery) ||
                (emp.bloodGroup ?: "").lowercase().contains(lowerQuery)
            }
            SearchFilter.METAL_NUMBER -> emp.metalNumber?.lowercase()?.contains(lowerQuery) == true
        }
    }
}

/**
 * Normalizes blood group string for display and matching.
 * E.g., "O Positive" -> "O+", "o-" -> "O–"
 */
fun getFormattedBloodGroup(bloodGroup: String?): String {
    val bg = bloodGroup ?: return "??"
    if (bg.trim() == "??" || bg.isBlank()) return "??"
    
    return bg.uppercase()
        .replace("POSITIVE", "+")
        .replace("NEGATIVE", "–")
        .replace("VE", "")
        .replace("(", "")
        .replace(")", "")
        .trim()
        .let { clean ->
            when (clean) {
                "A" -> "A+"
                "B" -> "B+"
                "O" -> "O+"
                "AB" -> "AB+"
                "A-" -> "A–"
                "B-" -> "B–"
                "O-" -> "O–"
                "AB-" -> "AB–"
                else -> clean
            }
        }
}

/**
 * Returns color based on blood group priority/rarity.
 */
fun getBloodGroupColor(bloodGroup: String?): Color {
    val formatted = getFormattedBloodGroup(bloodGroup)
    return when (formatted) {
        "O–" -> Color(0xFFD32F2F)    // Red
        "AB–" -> Color(0xFFF57C00)   // Deep Orange
        "A–", "B–" -> Color(0xFFFB8C00) // Orange
        "??", "?" -> Color(0xFF9E9E9E)  // Grey
        else -> {
            if (formatted.contains("+")) {
                Color(0xFF1976D2) // Blue for all positive groups
            } else {
                Color(0xFF9E9E9E) // Default Grey
            }
        }
    }
}

/**
 * Returns color based on Rank seniority and category.
 */
fun getRankColor(rank: String?): Color {
    val r = rank?.uppercase() ?: return Color(0xFF9E9E9E) // Default Gray

    return when {
        // Level 1: Top Officers (Gold/Amber)
        r in setOf("DG & IGP", "DG", "ADGP", "IGP", "DIG", "DCP", "SP", "ADDL_SP", "DSP", "ACP", "ASST.CMDT", "DEPT.CMDT", "CMDT") -> {
            Color(0xFFB8860B) // DarkGoldenrod (More readable than pure Gold)
        }
        
        // Level 2: Inspectors & Sub-Inspectors (Purple/Indigo)
        r.contains("PI") || r.contains("PSI") || r.contains("RPI") || r.contains("RSI") || r.contains("CPI") -> {
            Color(0xFF5E35B1) // Deep Purple
        }

        // Level 3: Frontline Force (ASI, HC, PC) - Teal/Blue
        r.contains("ASI") || r.contains("HC") || r.contains("PC") -> {
            Color(0xFF00796B) // Teal
        }

        // Level 4: Administrative / Ministerial (Slate/Gray)
        r in setOf("FDA", "SDA", "SS", "STENO", "TYPIST", "PA", "FOLLOWER", "GHA") -> {
            Color(0xFF607D8B) // Slate Gray
        }

        else -> Color(0xFF616161) // Default Dark Gray
    }
}
