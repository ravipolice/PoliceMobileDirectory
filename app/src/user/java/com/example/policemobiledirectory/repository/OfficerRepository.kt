package com.example.policemobiledirectory.repository

import android.util.Log
import com.example.policemobiledirectory.data.local.OfficerDao
import com.example.policemobiledirectory.data.local.OfficerEntity
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.utils.SearchUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfficerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val officerDao: OfficerDao
) {
    private val TAG = "OfficerRepository"
    private val officersCollection = firestore.collection("officers")
    private val ioDispatcher = Dispatchers.IO

    fun getOfficers(): Flow<RepoResult<List<Officer>>> = officerDao.getAllOfficers()
        .map { entities -> 
            RepoResult.Success(entities.map { it.toOfficer() })
        }
        .flowOn(ioDispatcher)

    private fun safeOfficerFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): Officer? {
        try {
            val officer = doc.toObject(Officer::class.java)
            if (officer != null) {
                return if (officer.isHidden) null else officer.copy(agid = doc.id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Standard parsing failed for ${doc.id}, attempting manual fallback: ${e.message}")
        }

        try {
            val data = doc.data ?: return null

            fun getString(key: String): String? {
                val value = data[key] ?: return null
                return value.toString().trim()
            }
            
            fun getBoolean(key: String, default: Boolean): Boolean {
                 val value = data[key] ?: return default
                 return when(value) {
                     is Boolean -> value
                     is String -> value.toBoolean()
                     else -> default
                 }
            }

            val isHidden = getBoolean("isHidden", false)
            if (isHidden) return null

            return Officer(
                agid = doc.id,
                name = getString("name") ?: "",
                email = getString("email"),
                bloodGroup = getString("bloodGroup"),
                mobile = getString("mobile"), 
                landline = getString("landline"),
                rank = getString("rank"),
                station = getString("station"),
                district = getString("district"),
                subDivision = getString("subDivision"),
                photoUrl = getString("photoUrl"),
                unit = getString("unit"),
                isHidden = isHidden
            )
        } catch (e: Exception) {
            Log.e(TAG, "Manual parsing failed completely for ${doc.id}: ${e.message}")
            return null
        }
    }

    /**
     * Sync all officers from Firestore to Room
     */
    suspend fun syncAllOfficers(): RepoResult<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "🔄 Syncing Unified Directory (Officers + Employees)...")
            
            // 1. Fetch Directory Officers
            val officerSnapshot = firestore.collection("officers").get().await()
            val directoryOfficers = officerSnapshot.documents.mapNotNull { doc ->
                safeOfficerFromDoc(doc)
            }
            Log.d(TAG, "Fetched ${directoryOfficers.size} directory officers")

            // 2. Fetch Registered Employees
            val employeeSnapshot = firestore.collection("employees")
                .whereEqualTo("isApproved", true)
                .get().await()
            
            val registeredEmployees = employeeSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(com.example.policemobiledirectory.model.Employee::class.java)?.copy(kgid = doc.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse employee ${doc.id}: ${e.message}")
                    null
                }
            }
            Log.d(TAG, "Fetched ${registeredEmployees.size} registered employees")

            // 3. Merge Logic (Deduplicate by normalized mobile number)
            val unifiedMap = mutableMapOf<String, OfficerEntity>()

            // Helper to normalize mobile numbers for matching
            fun normalizeMobile(m: String?): String? {
                if (m.isNullOrBlank()) return null
                val digits = m.replace(Regex("\\D"), "")
                return if (digits.length >= 10) digits.takeLast(10) else digits
            }

            // Process Directory first
            directoryOfficers.forEach { off ->
                val entity = off.toEntity()
                unifiedMap[off.agid] = entity // Use AGID as primary key
                
                // Track by normalized mobile for deduplication
                val normMobile = normalizeMobile(off.mobile)
                if (normMobile != null) {
                    unifiedMap["TEL_$normMobile"] = entity
                }
            }

            // Process Employees (they take priority)
            registeredEmployees.forEach { emp ->
                if (emp.isHidden) return@forEach
                
                val blob = SearchUtils.generateSearchBlob(
                    emp.kgid, emp.name, emp.mobile1, emp.rank, emp.unit, emp.district, "", emp.station, emp.email, emp.bloodGroup
                )
                val entity = OfficerEntity(
                    agid = emp.kgid,
                    name = emp.name,
                    email = emp.email,
                    rank = emp.rank,
                    mobile = emp.mobile1,
                    landline = emp.landline,
                    station = emp.station,
                    district = emp.district,
                    unit = emp.unit,
                    photoUrl = emp.photoUrl ?: emp.photoUrlFromGoogle,
                    bloodGroup = emp.bloodGroup,
                    isHidden = emp.isHidden,
                    searchBlob = blob
                )

                // Overwrite any directory record with the same normalized mobile
                val normMobile = normalizeMobile(emp.mobile1)
                if (normMobile != null && unifiedMap.containsKey("TEL_$normMobile")) {
                    val existingEntity = unifiedMap["TEL_$normMobile"]
                    if (existingEntity != null) {
                        // Remove the directory record by its AGID to prevent double-entry
                        unifiedMap.remove(existingEntity.agid)
                    }
                }
                
                // Ensure the Employee record exists by KGID
                unifiedMap[emp.kgid] = entity
            }

            // Final set of entities
            val finalEntities = unifiedMap.filter { !it.key.startsWith("TEL_") }.values.toList()
            
            Log.d(TAG, "Final Unified Directory Size: ${finalEntities.size}")

            officerDao.insertOfficers(finalEntities)
            
            // Cleanup stale officers in Room
            if (finalEntities.size > 100) {
                 val allIds = finalEntities.map { it.agid }
                 val roomCount = officerDao.getOfficerCount()
                 if (roomCount > finalEntities.size) {
                     Log.d(TAG, "Cleaning up stale records in Room...")
                     officerDao.deleteOfficersNotInList(allIds)
                 }
            }

            RepoResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during unified sync: ${e.message}", e)
            RepoResult.Error(e, "Failed to sync directory")
        }
    }

    fun searchByBlob(query: String): Flow<RepoResult<List<Officer>>> {
        val trimmedQuery = query.trim().lowercase()
        if (trimmedQuery.contains(" ")) {
            val keywords = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (keywords.isNotEmpty()) {
                return officerDao.getAllOfficers().map { entities ->
                    val filtered = entities.filter { entity ->
                        val searchBlob = entity.searchBlob.lowercase()
                        keywords.all { keyword -> searchBlob.contains(keyword) }
                    }.map { it.toOfficer() }
                    RepoResult.Success(filtered)
                }.flowOn(ioDispatcher)
            }
        }

        val normalizedQuery = query.trim().lowercase()
        return officerDao.smartSearch(normalizedQuery)
            .map { entities -> RepoResult.Success(entities.map { it.toOfficer() }) }
            .flowOn(ioDispatcher)
    }

    /**
     * Legacy Search: Search officers by query and filter (Hits Room via searchBlob fallback or just stay Room-based)
     * For now, we redirect text search to Room.
     */
    fun searchOfficers(query: String, filter: String): Flow<RepoResult<List<Officer>>> = searchByBlob(query)

    suspend fun addOrUpdateOfficer(officer: Officer): Flow<RepoResult<Boolean>> = flow {
        emit(RepoResult.Loading)
        try {
            val docId = officer.agid.takeIf { it.isNotBlank() } ?: officersCollection.document().id
            val officerToSave = officer.copy(agid = docId)

            officersCollection.document(docId).set(officerToSave).await()
            
            // Immediately update local cache
            officerDao.insertOfficer(officerToSave.toEntity())
            
            emit(RepoResult.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving officer: ${e.message}", e)
            emit(RepoResult.Error(e, "Failed to save officer: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    // -------------------------------------------------------------------
    // MAPPERS
    // -------------------------------------------------------------------

    private fun Officer.toEntity(): OfficerEntity {
        val blob = SearchUtils.generateSearchBlob(
            agid, name, mobile, rank, unit, district, subDivision, station, email, bloodGroup
        )
        return OfficerEntity(
            agid = agid,
            name = name,
            email = email,
            rank = rank,
            mobile = mobile,
            landline = landline,
            station = station,
            district = district,
            unit = unit,
            subDivision = subDivision,
            photoUrl = photoUrl,
            bloodGroup = bloodGroup,
            isHidden = isHidden ?: false,
            searchBlob = blob
        )
    }

    private fun OfficerEntity.toOfficer(): Officer {
        return Officer(
            agid = agid,
            name = name,
            email = email,
            rank = rank,
            mobile = mobile,
            landline = landline,
            station = station,
            district = district,
            unit = unit,
            subDivision = subDivision,
            photoUrl = photoUrl,
            bloodGroup = bloodGroup,
            isHidden = isHidden,
            searchBlob = searchBlob
        )
    }
}

