package com.example.policemobiledirectory.repository

import android.net.Uri
import com.example.policemobiledirectory.data.local.PendingRegistrationDao
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.toEmployee
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
    private fun mapDocumentToEntity(doc: com.google.firebase.firestore.DocumentSnapshot): PendingRegistrationEntity? {
        return try {
            val entity = doc.toObject(PendingRegistrationEntity::class.java) ?: return null
            
            // Support both Firestore Timestamp/Date and Long types safely
            val submittedLong = try { doc.getLong("submittedAt") } catch (e: Exception) { null }
            val submittedAt = if (submittedLong != null) java.util.Date(submittedLong)
                              else doc.getDate("submittedAt") ?: doc.getTimestamp("submittedAt")?.toDate() ?: entity.submittedAt

            val createdLong = try { doc.getLong("createdAt") } catch (e: Exception) { null }
            val createdAt = if (createdLong != null) java.util.Date(createdLong)
                            else doc.getDate("createdAt") ?: doc.getTimestamp("createdAt")?.toDate() ?: entity.createdAt

            val dobLong = try { doc.getLong("dateOfBirth") } catch (e: Exception) { null }
            val dateOfBirth = if (dobLong != null) java.util.Date(dobLong)
                              else doc.getDate("dateOfBirth") ?: doc.getTimestamp("dateOfBirth")?.toDate() ?: entity.dateOfBirth

            val serviceStartLong = try { doc.getLong("serviceStartDate") } catch (e: Exception) { null }
            val serviceStartDate = if (serviceStartLong != null) java.util.Date(serviceStartLong)
                                   else doc.getDate("serviceStartDate") ?: doc.getTimestamp("serviceStartDate")?.toDate() ?: entity.serviceStartDate

            val status = doc.getString("status")?.lowercase() ?: "pending"

            entity.copy(
                firestoreId = doc.id,
                status = status,
                submittedAt = submittedAt,
                createdAt = createdAt,
                dateOfBirth = dateOfBirth,
                serviceStartDate = serviceStartDate
            )
        } catch (e: Exception) {
            android.util.Log.e("PendingRegRepo", "❌ Error mapping document ${doc.id}: ${e.message}")
            null
        }
    }

    /* ----------------------------------------------------------
        FETCH PENDING (Firestore → Room)
    ----------------------------------------------------------- */
    suspend fun fetchPendingFromFirestore(): RepoResult<List<PendingRegistrationEntity>> {
        return try {
            val snapshot = firestore.collection("pending_registrations")
                .whereEqualTo("status", "pending")
                .get().await()

            val list = snapshot.documents.mapNotNull { mapDocumentToEntity(it) }
            RepoResult.Success(list)
        } catch (e: Exception) {
            RepoResult.Error(e, "Failed to load pending registrations: ${e.message}")
        }
    }

    suspend fun saveAllToLocal(list: List<PendingRegistrationEntity>) =
        withContext(ioDispatcher) { dao.insertAll(list) }

    fun getLocalPending(): Flow<List<PendingRegistrationEntity>> =
        dao.getAllPending()

    suspend fun getPendingByEmail(email: String): PendingRegistrationEntity? =
        withContext(ioDispatcher) {
            val normalizedEmail = email.trim().lowercase()
            val rawEmail = email.trim()

            // 1️⃣ ALWAYS check Firestore first for the live status (source of truth)
            try {
                // Try normalized first
                var snapshot = firestore.collection("pending_registrations")
                    .whereEqualTo("email", normalizedEmail)
                    .get().await()
                
                if (snapshot.isEmpty && normalizedEmail != rawEmail) {
                    // Try raw fallback
                    snapshot = firestore.collection("pending_registrations")
                        .whereEqualTo("email", rawEmail)
                        .get().await()
                }
                
                val remoteList = snapshot.documents.mapNotNull { mapDocumentToEntity(it) }
                val remoteLatest = remoteList.maxByOrNull { it.submittedAt?.time ?: it.createdAt?.time ?: 0L }
                if (remoteLatest != null) {
                    // Sync the fresh status to local Room DB
                    dao.insert(remoteLatest)
                    return@withContext remoteLatest
                }
            } catch (e: Exception) {
                android.util.Log.w("PendingRegRepo", "Failed to check Firestore for pending registration: ${e.message}")
            }

            // 2️⃣ Fallback to local Room cache if offline/network fails
            val local = dao.getByEmail(normalizedEmail) ?: if (normalizedEmail != rawEmail) dao.getByEmail(rawEmail) else null
            local
        }


    /* ----------------------------------------------------------
        SUBMIT NEW REGISTRATION
    ----------------------------------------------------------- */
    suspend fun addPendingRegistration(entity: PendingRegistrationEntity): Flow<RepoResult<String>> =
        flow {
            emit(RepoResult.Loading)

            try {
                val docRef = firestore.collection("pending_registrations").document(entity.kgid)

                var finalPin = entity.pin
                if (finalPin.length == 6 && !finalPin.contains(":")) {
                    finalPin = PinHasher.hashPassword(finalPin)
                }

                val prepared = entity.copy(
                    pin = finalPin,
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

    suspend fun updatePendingRegistration(entity: PendingRegistrationEntity): RepoResult<String> {
        return try {
            val docId = entity.firestoreId ?: return RepoResult.Error(message = "Missing document id")

            firestore.collection("pending_registrations")
                .document(docId)
                .set(entity, SetOptions.merge())
                .await()

            withContext(ioDispatcher) { dao.update(entity) }

            RepoResult.Success("Pending registration updated.")
        } catch (e: Exception) {
            RepoResult.Error(e, "Update failed: ${e.message}")
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

            // ✅ CRITICAL FIX: Ensure kgid is explicitly set in Employee object
            val employee = entity.toEmployee(photoUrl).copy(kgid = kgid)

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
