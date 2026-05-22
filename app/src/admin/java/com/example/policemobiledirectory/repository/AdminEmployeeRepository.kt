package com.example.policemobiledirectory.repository

import android.util.Log
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.data.local.AdminEmployeeDao
import com.example.policemobiledirectory.data.local.AdminEmployeeEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminEmployeeRepository @Inject constructor(
    private val adminEmployeeDao: AdminEmployeeDao,
    private val firestore: FirebaseFirestore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val TAG = "AdminEmployeeRepository"
    private val adminCollection = firestore.collection("admin_employees")

    // Get live data from local Room Database
    fun getAllAdminEmployees(): Flow<List<AdminEmployeeEntity>> {
        return adminEmployeeDao.getAllEmployees()
    }

    // Force a sync from Firestore to Room
    suspend fun refreshAdminEmployees(): RepoResult<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Fetching admin employees from Firestore...")
            val snapshot = adminCollection.get().await()
            val employees = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val name = (data["name"] as? String)?.trim() ?: ""
                    // Skip records with no name
                    if (name.isBlank()) return@mapNotNull null

                    // Always use Firestore doc.id as KGID — the kgid field in data may be blank
                    val kgid = (data["kgid"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: doc.id

                    AdminEmployeeEntity(
                        kgid = kgid,
                        name = name,
                        email = (data["email"] as? String)?.trim() ?: "",
                        mobile1 = data["mobile1"] as? String,
                        mobile2 = data["mobile2"] as? String,
                        rank = data["rank"] as? String,
                        metalNumber = data["metalNumber"] as? String,
                        district = data["district"] as? String,
                        station = data["station"] as? String,
                        bloodGroup = data["bloodGroup"] as? String,
                        photoUrl = data["photoUrl"] as? String,
                        photoUrlFromGoogle = data["photoUrlFromGoogle"] as? String,
                        fcmToken = data["fcmToken"] as? String,
                        firebaseUid = data["firebaseUid"] as? String,
                        isAdmin = data["isAdmin"] as? Boolean ?: true,
                        isApproved = data["isApproved"] as? Boolean ?: true,
                        unit = data["unit"] as? String,
                        landline = data["landline"] as? String,
                        landline2 = data["landline2"] as? String,
                        gender = (data["gender"] as? String) ?: "Male",
                        subSection = data["subSection"] as? String,
                        dutyRole = data["dutyRole"] as? String,
                        isManualStation = data["isManualStation"] as? Boolean ?: false,
                        isManualSubSection = data["isManualSubSection"] as? Boolean ?: false,
                        searchBlob = "$kgid $name ${data["station"] ?: ""} ${data["rank"] ?: ""} ${data["mobile1"] ?: ""}".trim()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing admin employee document ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Fetched ${employees.size} valid admin employees. Caching to Room...")
            if (employees.isNotEmpty()) {
                adminEmployeeDao.clearEmployees()
                adminEmployeeDao.insertEmployees(employees)
            }
            
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing admin employees", e)
            RepoResult.Error(e, "Failed to sync admin database: ${e.localizedMessage}")
        }
    }
}
