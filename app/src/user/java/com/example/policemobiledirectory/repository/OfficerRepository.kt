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
    private val officersCollection = firestore.collection("officers_v2")
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
            Log.d(TAG, "🔄 Syncing Officers from Firestore to Room...")
            val snapshot = officersCollection.get().await()
            val entities = snapshot.documents.mapNotNull { doc ->
                val off = safeOfficerFromDoc(doc)
                if (off != null) {
                    off.toEntity()
                } else null
            }
            
            officerDao.insertOfficers(entities)
            
            // Cleanup stale officers: if Firestore list is significantly large, assume it's the source of truth
            if (entities.size > 100) {
                 val firestoreAgids = entities.map { it.agid }
                 officerDao.deleteStaleOfficers(firestoreAgids)
            }
                
            Log.d(TAG, "✅ Synced ${entities.size} officers to Room")
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing officers: ${e.message}", e)
            RepoResult.Error(e, "Failed to sync officers")
        }
    }

    fun searchByBlob(query: String): Flow<RepoResult<List<Officer>>> {
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

