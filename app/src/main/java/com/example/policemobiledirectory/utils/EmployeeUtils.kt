package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
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
        r in setOf("DG & IGP", "DG", "ADGP", "IGP", "DIG", "DCP", "SP", "ADDL_SP", "DSP", "DYSP", "DY.SP", "ACP", "ASST.CMDT", "DEPT.CMDT", "CMDT") ||
        r.contains("DYSP") || r.contains("DY.SP") || r.contains("DY SP") -> {
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

private fun getRankAbbreviation(rank: String): String {
    val r = rank.trim().uppercase()
        .replace(".", "")
        .replace(" ", "")
        .replace("-", "")
    return when {
        r.contains("SUBINSPECTOR") || r == "PSI" -> "PSI"
        r.contains("CIRCLEPOLICEINSPECTOR") || r.contains("CIRCLEINSPECTOR") || r.contains("CIRCLEPI") || r == "CPI" -> "CPI"
        r.contains("POLICEINSPECTOR") || r == "PI" -> "PI"
        r.contains("DEPUTYSUPERINTENDENT") || r.contains("DYSP") || r.contains("DSP") -> "DySP"
        r.contains("ASSISTANTSUPERINTENDENT") || r == "ASP" -> "ASP"
        r.contains("ASSISTANTCOMMISSIONER") || r == "ACP" -> "ACP"
        r.contains("SUPERINTENDENTOFPOLICE") || r == "SP" -> "SP"
        r.contains("DEPUTYCOMMISSIONER") || r == "DCP" -> "DCP"
        r.contains("DEPUTYINSPECTORGENERAL") || r == "DIG" -> "DIG"
        r.contains("INSPECTORGENERAL") || r == "IGP" -> "IGP"
        r.contains("ADDITIONALDIRECTORGENERAL") || r == "ADGP" -> "ADGP"
        r.contains("DIRECTORGENERAL") || r == "DGP" -> "DGP"
        r.contains("ASSISTANTSUB") || r == "ASI" -> "ASI"
        r.contains("HEADCONSTABLE") || r == "HC" -> "HC"
        r.contains("POLICECONSTABLE") || r == "PC" -> "PC"
        r.contains("ADMINISTRATIVEOFFICER") || r == "AO" -> "AO"
        r.contains("ASSISTANTADMINISTRATIVE") || r == "AAO" -> "AAO"
        else -> rank
    }
}

/**
 * Super Model to format and resolve Officer display names cleanly based on rank, station, district, range and unit.
 */
class OfficerNameFormatter(private val officer: Officer) {
    val rawName: String = officer.name.replace(".", "").replace("(?i)\\bDy SP\\b".toRegex(), "DySP").trim()
    val rank: String = (officer.rank ?: "").replace(".", "").replace("(?i)\\bDy SP\\b".toRegex(), "DySP").trim()
    val rAbbr: String = getRankAbbreviation(rank)
    val effectiveRawName: String = if (rawName.isBlank()) rAbbr else rawName

    val station: String = officer.station ?: officer.unit ?: ""
    val district: String = officer.district ?: ""
    val unitVal: String = officer.unit ?: ""
    val email: String = officer.email ?: ""
    val section: String = officer.subDivision ?: ""

    val isAAO: Boolean = rank.equals("AAO", ignoreCase = true) || rAbbr.equals("AAO", ignoreCase = true)
    val isAO: Boolean = rank.equals("AO", ignoreCase = true) || rAbbr.equals("AO", ignoreCase = true)

    fun format(): String {
        val result = when {
            isAAO && isAAOGeneric() -> formatAAO()
            isAO && isAOGeneric() -> formatAO()
            isGeneric() -> formatGeneric()
            else -> rawName
        }
        return getShortRangeName(result)
    }

    private fun isAAOGeneric(): Boolean {
        val normalizedEffRawName = effectiveRawName.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        val normalizedRank = rank.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        val normalizedRAbbr = rAbbr.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        return effectiveRawName.isBlank() || 
               effectiveRawName.equals(rank, ignoreCase = true) || 
               effectiveRawName.equals(rAbbr, ignoreCase = true) || 
               effectiveRawName.startsWith("AAO", ignoreCase = true) ||
               normalizedEffRawName == normalizedRank ||
               normalizedEffRawName == normalizedRAbbr ||
               normalizedEffRawName.startsWith("AAO")
    }

    private fun formatAAO(): String {
        return when {
            unitVal.equals("Training", ignoreCase = true) -> {
                if (email.contains("kpa", ignoreCase = true) || effectiveRawName.contains("KPA", ignoreCase = true) || section.contains("KPA", ignoreCase = true)) {
                    "AAO KPA"
                } else {
                    "AAO Training"
                }
            }
            unitVal.equals("Computer", ignoreCase = true) || unitVal.contains("PCW", ignoreCase = true) || unitVal.contains("SCRB", ignoreCase = true) -> {
                "AAO PCW / SCRB"
            }
            unitVal.equals("Wireless", ignoreCase = true) || unitVal.equals("CLM", ignoreCase = true) || email.contains("clm", ignoreCase = true) -> {
                "AAO CLM"
            }
            unitVal.equals("KSPH", ignoreCase = true) -> {
                "AAO KSPH & IDCL"
            }
            unitVal.equals("Recruitment", ignoreCase = true) -> {
                "AAO Recruitment"
            }
            unitVal.equals("Intelligence", ignoreCase = true) -> {
                "AAO Intelligence"
            }
            unitVal.equals("Railway", ignoreCase = true) || unitVal.equals("Railways", ignoreCase = true) -> {
                "AAO Railways"
            }
            unitVal.equals("KSRP", ignoreCase = true) -> {
                if (effectiveRawName.contains("XII", ignoreCase = true) || section.contains("XII", ignoreCase = true)) {
                    "AAO XII-BN KSRP"
                } else {
                    "AAO KSRP"
                }
            }
            section.equals("GC, BUILDING, ABY SECTION", ignoreCase = true) || section.contains("GC", ignoreCase = true) || section.contains("BUILDING", ignoreCase = true) -> {
                "AAO GC"
            }
            unitVal.equals("L&O", ignoreCase = true) -> {
                if (district.isNotBlank()) {
                    if (district.contains("Range", ignoreCase = true)) {
                        val rangePart = district.split(Regex("[-–,/]")).firstOrNull()?.trim() ?: ""
                        "AAO $rangePart"
                    } else {
                        "AAO $district"
                    }
                } else {
                    "AAO"
                }
            }
            else -> {
                if (effectiveRawName.startsWith("AAO", ignoreCase = true)) {
                    effectiveRawName.replace(Regex(",\\s*"), " ").replace(Regex("\\s+"), " ").trim()
                } else if (unitVal.isNotBlank()) {
                    "AAO $unitVal"
                } else {
                    "AAO"
                }
            }
        }
    }

    private fun isAOGeneric(): Boolean {
        val normalizedEffRawName = effectiveRawName.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        val normalizedRank = rank.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        val normalizedRAbbr = rAbbr.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        return effectiveRawName.isBlank() || 
               effectiveRawName.equals(rank, ignoreCase = true) || 
               effectiveRawName.equals(rAbbr, ignoreCase = true) || 
               effectiveRawName.startsWith("AO", ignoreCase = true) ||
               normalizedEffRawName == normalizedRank ||
               normalizedEffRawName == normalizedRAbbr ||
               normalizedEffRawName.startsWith("AO")
    }

    private fun formatAO(): String {
        return when {
            unitVal.equals("CID", ignoreCase = true) -> "AO CID"
            unitVal.equals("DCRE", ignoreCase = true) -> "AO DCRE"
            section.equals("CAO", ignoreCase = true) -> "CAO"
            district.equals("Bengaluru City", ignoreCase = true) && (effectiveRawName.contains("COP", ignoreCase = true) || section.contains("COP", ignoreCase = true)) -> {
                "AO COP"
            }
            else -> {
                if (district.isNotBlank()) {
                    "AO $district"
                } else if (unitVal.isNotBlank() && !unitVal.equals("L&O", ignoreCase = true)) {
                    "AO $unitVal"
                } else {
                    "AO"
                }
            }
        }
    }

    private fun isGeneric(): Boolean {
        val effAbbr = getRankAbbreviation(effectiveRawName)
        val normalizedEffRawName = effectiveRawName.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        val normalizedRank = rank.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        val normalizedRAbbr = rAbbr.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")
        val normalizedEffAbbr = effAbbr.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "")

        val genericRanks = setOf(
            "PSI", "PI", "CPI", "DYSP", "DSP", "ASP", "ACP", "SP", "DCP", "DIG", "IGP", "ADGP", "DGP", 
            "ASI", "HC", "PC", "AO", "AAO", "RSI", "RPI", "WPSI", "WPI", "ARSI", "WHC", "WPC", "APC", 
            "AHC", "CHC", "CPC", "PCW", "HCW", "PIW", "PSIW", "ASIW"
        )

        return effectiveRawName.isNotBlank() && (
            effectiveRawName.equals(rank, ignoreCase = true) ||
            effectiveRawName.equals(rAbbr, ignoreCase = true) ||
            effAbbr.equals(rAbbr, ignoreCase = true) ||
            normalizedEffRawName == normalizedRank ||
            normalizedEffRawName == normalizedRAbbr ||
            normalizedEffAbbr == normalizedRAbbr ||
            normalizedEffRawName in genericRanks ||
            normalizedEffAbbr in genericRanks
        )
    }

    private fun formatGeneric(): String {
        val parts = mutableListOf(effectiveRawName)
        if (station.isNotBlank()) {
            val cleanStation = station
                .replace("(?i)\\bPS\\b".toRegex(), "")
                .replace("(?i)\\bPolice Station\\b".toRegex(), "")
                .trim()
            if (cleanStation.isNotBlank() && 
                !cleanStation.equals(effectiveRawName, ignoreCase = true) && 
                !cleanStation.equals(rank, ignoreCase = true) && 
                !cleanStation.equals(rAbbr, ignoreCase = true) &&
                cleanStation.lowercase() != getRankAbbreviation(effectiveRawName).lowercase()) {
                parts.add(cleanStation)
            }
        }
        if (district.isNotBlank() && 
            !district.equals(station, ignoreCase = true) && 
            !district.equals(effectiveRawName, ignoreCase = true) && 
            !district.equals(rank, ignoreCase = true) && 
            !district.equals(rAbbr, ignoreCase = true) &&
            district.lowercase() != getRankAbbreviation(effectiveRawName).lowercase()) {
            parts.add(district)
        }
        return parts.joinToString(" ")
    }
}

/**
 * Calculates display name according to the name display rules:
 * - If it is an employee, returns the raw employee name directly without formatting.
 * - If it is an officer, delegates to OfficerNameFormatter to build a descriptive name if generic.
 * - Otherwise, returns the raw name.
 */
fun getContactDisplayName(employee: Employee?, officer: Officer?): String {
    if (employee != null) {
        return employee.name
    }
    if (officer != null) {
        return OfficerNameFormatter(officer).format()
    }
    return ""
}

/**
 * Utility function to abbreviate range names (e.g. "Central Range" -> "CR", "Northern Range" -> "NR").
 */
fun getShortRangeName(text: String?): String {
    if (text == null) return ""
    return text.trim()
        .replace("(?i)\\bNorth[- ]?Eastern\\s+Range\\b".toRegex(), "NER")
        .replace("(?i)\\bCentral\\s+Range\\b".toRegex(), "CR")
        .replace("(?i)\\bNorthern\\s+Range\\b".toRegex(), "NR")
        .replace("(?i)\\bSouthern\\s+Range\\b".toRegex(), "SR")
        .replace("(?i)\\bWestern\\s+Range\\b".toRegex(), "WR")
        .replace("(?i)\\b(Eastern|Davangere)\\s+Range\\b".toRegex(), "ER")
        .replace("(?i)\\bBallari\\s+Range\\b".toRegex(), "BR")
}


