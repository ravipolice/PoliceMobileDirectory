package com.example.policemobiledirectory.repository

import android.net.Uri
import android.util.Log
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.data.local.PendingRegistrationDao
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.toEmployee
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.policemobiledirectory.utils.PinHasher
import com.example.policemobiledirectory.repository.RepoResult
import java.util.Date

@Singleton
class PendingRegistrationRepository @Inject constructor(
    private val dao: PendingRegistrationDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val employeeRepository: EmployeeRepository,
    private val ioDispatcher: CoroutineDispatcher
) {

    /* ----------------------------------------------------------
        FETCH PENDING (Firestore → Room)
    ----------------------------------------------------------- */
    suspend fun fetchPendingFromFirestore(): RepoResult<List<PendingRegistrationEntity>> {
        android.util.Log.d("PendingReg", "🔍 fetchPendingFromFirestore: Starting fetch...")
        return try {
            val snapshot = firestore.collection("pending_registrations")
                .get().await()

            android.util.Log.d("PendingReg", "📡 Firestore returned ${snapshot.size()} documents")

            // Map all documents to entities with case-insensitive filtering
            val rawList = snapshot.documents.mapNotNull { doc ->
                try {
                    val status = doc.getString("status")?.lowercase() ?: "pending"
                    // Check for pending status (now normalized to lowercase)
                    val isPending = status == "pending"
                    
                    if (!isPending) return@mapNotNull null
                    
                    val entity = doc.toObject(PendingRegistrationEntity::class.java)
                    
                    // Defensive date mapping: ensure we have valid dates if toObject missed them
                    val submittedAt = doc.getDate("submittedAt") ?: doc.getTimestamp("submittedAt")?.toDate() ?: entity?.submittedAt
                    val createdAt = doc.getDate("createdAt") ?: doc.getTimestamp("createdAt")?.toDate() ?: entity?.createdAt
                    val dateOfBirth = doc.getDate("dateOfBirth") ?: doc.getTimestamp("dateOfBirth")?.toDate() ?: entity?.dateOfBirth
                    val serviceStartDate = doc.getDate("serviceStartDate") ?: doc.getTimestamp("serviceStartDate")?.toDate() ?: entity?.serviceStartDate

                    entity?.copy(
                        firestoreId = doc.id,
                        status = status, // Use normalized status
                        submittedAt = submittedAt,
                        createdAt = createdAt,
                        dateOfBirth = dateOfBirth,
                        serviceStartDate = serviceStartDate
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PendingReg", "❌ Failed to map document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("PendingReg", "📋 Mapped ${rawList.size} entities successfully from ${snapshot.size()} Firestore docs")

            // Standardize the "Pending" registration count calculation on Android to match the web dashboard's deduplication logic.
            // Using lowercase for KGID and Email ensures case-insensitive deduplication.
            val deduplicated = rawList
                .sortedByDescending { it.createdAt?.time ?: it.submittedAt?.time ?: 0L }
                .distinctBy { it.kgid.trim().lowercase().ifBlank { it.email.trim().lowercase() } }

            android.util.Log.d("PendingReg", "✅ Final deduplicated count: ${deduplicated.size}")
            RepoResult.Success(deduplicated)
        } catch (e: Exception) {
            android.util.Log.e("PendingReg", "💥 fetchPendingFromFirestore failed", e)
            RepoResult.Error(e, "Failed to load pending registrations: ${e.message}")
        }
    }

    suspend fun saveAllToLocal(list: List<PendingRegistrationEntity>) =
        withContext(ioDispatcher) { 
            android.util.Log.d("PendingReg", "💾 Saving ${list.size} items to Room (clean sync)")
            dao.deletePending()
            dao.insertAll(list) 
        }

    fun getLocalPending(): Flow<List<PendingRegistrationEntity>> =
        dao.getAllPending()

    suspend fun getPendingByEmail(email: String): PendingRegistrationEntity? =
        withContext(ioDispatcher) {
            // First check local DB
            val local = dao.getByEmail(email)
            if (local != null) return@withContext local

            // Then check Firestore as fallback
            try {
                val snapshot = firestore.collection("pending_registrations")
                    .whereEqualTo("email", email)
                    .whereEqualTo("status", "pending")
                    .get().await()
                
                snapshot.toObjects(PendingRegistrationEntity::class.java).firstOrNull()
            } catch (e: Exception) {
                null
            }
        }


    /* ----------------------------------------------------------
        SUBMIT NEW REGISTRATION
    ----------------------------------------------------------- */
    suspend fun addPendingRegistration(entity: PendingRegistrationEntity): Flow<RepoResult<String>> =
        flow {
            emit(RepoResult.Loading)

            try {
                val docRef = firestore.collection("pending_registrations").document()

                val prepared = entity.copy(
                    firestoreId = docRef.id,
                    status = "pending",
                    createdAt = java.util.Date(),
                    isApproved = false
                )

                // Firestore save
                docRef.set(prepared).await()

                // Local save
                dao.insert(prepared)

                emit(RepoResult.Success("Registration submitted for approval"))
            } catch (e: Exception) {
                emit(RepoResult.Error(e, "Failed: ${e.message}"))
            }
        }

    // ✅ Merged update function to resolve ambiguity and fix references
    suspend fun updatePendingRegistration(entity: PendingRegistrationEntity): RepoResult<String> = withContext(ioDispatcher) {
        val docId = entity.firestoreId ?: return@withContext RepoResult.Error(null, "Missing document id")
        try {
            // ✅ Use the cleaned mapper that excludes protected fields
            val updateData = entity.toAdminUpdateMap()

            Log.d("PendingRepo", "📡 Path: pending_registrations/$docId")
            
            firestore.collection("pending_registrations")
                .document(docId)
                .update(updateData)
                .await()

            Log.d("PendingRepo", "✅ Update Successful in Firestore!")

            withContext(ioDispatcher) { 
                val existing = if (entity.kgid.isNotBlank()) {
                    dao.getByKgid(entity.kgid)
                } else {
                    dao.findByFirestoreId(docId)
                }
                
                val entityToSave = if (existing != null) {
                    entity.copy(roomId = existing.roomId)
                } else {
                    entity
                }
                dao.insert(entityToSave) 
                Log.d("PendingRepo", "💾 Saved to Local DB: ${entityToSave.kgid}")
            }

            RepoResult.Success("Pending registration updated.")
        } catch (e: Exception) {
            Log.e("PendingRepo", "❌ Update FAILED for DocID: $docId", e)
            RepoResult.Error(e, "Update failed: ${e.message}")
        }
    }


    /* ----------------------------------------------------------
        MARK AS VIEWED
    ----------------------------------------------------------- */
    suspend fun markAsViewed(entity: PendingRegistrationEntity): RepoResult<String> {
        return try {
            val docId = entity.firestoreId ?: return RepoResult.Error(message = "Missing document id")

            firestore.collection("pending_registrations")
                .document(docId)
                .update("viewedByAdmin", true)
                .await()
            
            val updated = entity.copy(viewedByAdmin = true)
            withContext(ioDispatcher) { dao.update(updated) }
            
            RepoResult.Success("Marked as viewed")
        } catch (e: Exception) {
            RepoResult.Error(e, "Failed to mark as viewed")
        }
    }


    /* ----------------------------------------------------------
        UPLOAD PHOTO FOR APPROVAL
    ----------------------------------------------------------- */
    suspend fun uploadPhoto(entity: PendingRegistrationEntity, uri: Uri): String {
        val ref = storage.reference.child(
            "pending_photos/${entity.kgid}_${System.currentTimeMillis()}.jpg"
        )

        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }


    /* ----------------------------------------------------------
        APPROVE REGISTRATION → CREATE EMPLOYEE
    ----------------------------------------------------------- */
    suspend fun approve(entity: PendingRegistrationEntity): RepoResult<String> {
        return try {
            // ✅ CRITICAL FIX: Validate kgid is not empty before approval
            val kgid = entity.kgid.trim().takeIf { it.isNotBlank() }
                ?: return RepoResult.Error(message = "KGID is required for approval")
            
            val photoUrl = entity.photoUrl ?: entity.photoUrlFromGoogle

            var finalPin = entity.pin
            if (finalPin.length == 6 && !finalPin.contains(":")) {
                finalPin = PinHasher.hashPassword(finalPin)
            }

            // ✅ CRITICAL FIX: Ensure kgid is explicitly set in Employee object
            val employee = entity.toEmployee(photoUrl).copy(
                kgid = kgid,
                pin = finalPin
            )

            // 1️⃣ Save employee (Google Sheet + Room)
            val result = employeeRepository.addOrUpdateEmployee(employee).last()
            if (result !is RepoResult.Success)
                return RepoResult.Error(message = "Failed to save employee")

            // 2️⃣ Firestore update
            firestore.collection("pending_registrations")
                .document(entity.firestoreId!!)
                .update(
                    mapOf(
                        "status" to "approved",
                        "isApproved" to true,
                        "updatedAt" to java.util.Date()
                    )
                ).await()

            // 3️⃣ Local DB update
            entity.roomId?.let { dao.approve(it) }

            RepoResult.Success("Approved successfully")
        } catch (e: Exception) {
            RepoResult.Error(e, "Approval failed: ${e.message}")
        }
    }


    /* ----------------------------------------------------------
        REJECT REGISTRATION
    ----------------------------------------------------------- */
    suspend fun reject(entity: PendingRegistrationEntity, reason: String): RepoResult<String> {
        return try {
            firestore.collection("pending_registrations")
                .document(entity.firestoreId!!)
                .update(
                    mapOf(
                        "status" to "rejected",
                        "rejectionReason" to reason,
                        "updatedAt" to java.util.Date()
                    )
                ).await()

            entity.roomId?.let { dao.reject(it, reason) }

            RepoResult.Success("Registration rejected")
        } catch (e: Exception) {
            RepoResult.Error(e, "Rejection failed: ${e.message}")
        }
    }
}

/**
 * ✅ Custom Admin Update Mappers
 * Exclude protected fields (kgid, email, isAdmin) to prevent PERMISSION_DENIED.
 */
private fun Employee.toAdminUpdateMap(): Map<String, Any?> {
    return mapOf(
        "name" to this.name,
        "rank" to this.rank,
        "station" to this.station,
        "district" to this.district,
        "unit" to this.unit,
        "subSection" to this.subSection,
        "dutyRole" to this.dutyRole,
        "mobile1" to this.mobile1,
        "mobile2" to this.mobile2,
        "landline" to this.landline,
        "landline2" to this.landline2,
        "bloodGroup" to this.bloodGroup,
        "gender" to this.gender,
        "isManualStation" to this.isManualStation,
        "isManualSubSection" to this.isManualSubSection,
        "serviceStartDate" to this.serviceStartDate,
        "dateOfBirth" to this.dateOfBirth,
        "metalNumber" to this.metalNumber,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

private fun PendingRegistrationEntity.toAdminUpdateMap(): Map<String, Any?> {
    return mapOf(
        "name" to this.name,
        "rank" to this.rank,
        "station" to this.station,
        "district" to this.district,
        "unit" to this.unit,
        "subSection" to this.subSection,
        "dutyRole" to (this.dutyRole ?: this.subSection),
        "mobile1" to this.mobile1,
        "mobile2" to this.mobile2,
        "landline" to this.landline,
        "landline2" to this.landline2,
        "bloodGroup" to this.bloodGroup,
        "gender" to this.gender,
        "isManualStation" to this.isManualStation,
        "isManualSubSection" to this.isManualSubSection,
        "serviceStartDate" to this.serviceStartDate,
        "dateOfBirth" to this.dateOfBirth,
        "metalNumber" to this.metalNumber,
        "isAdmin" to (this.isAdmin ?: false),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
