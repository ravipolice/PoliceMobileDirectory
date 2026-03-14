package com.example.policemobiledirectory.api

/**
 * Data class matching PRD format:
 * {
 *   "success": true,
 *   "data": {
 *     "ranks": [...],
 *     "districts": [...],
 *     "stationsbydistrict": { "Bagalkot": [...], ... },
 *     "bloodgroups": [...],
 *     "lastupdated": "2025-12-01T12:00:00Z"
 *   }
 * }
 */
data class ConstantsData(
    val ranks: List<String>,
    val districts: List<String>,
    val stationsbydistrict: Map<String, List<String>>,
    val bloodgroups: List<String>,
    val lastupdated: String,
    val version: Int // Version number from server - should match LOCAL_CONSTANTS_VERSION
)
