package com.example.policemobiledirectory.utils

import android.content.Context
import android.util.Log
import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.model.LeaveCreditLog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "GDriveSync"
    private val BACKUP_FILE_NAME = "leave_manager_backup.json"
    private val gson = Gson()

    data class LeaveBackupData(
        val balance: LeaveBalance?,
        val entries: List<LeaveEntry>,
        val creditLogs: List<LeaveCreditLog> = emptyList()
    )

    sealed class SyncResult {
        data class Success(val data: LeaveBackupData? = null) : SyncResult()
        data class AccountMismatch(val expected: String, val actual: String) : SyncResult()
        data class NotGmail(val email: String) : SyncResult()
        object NoGoogleAccount : SyncResult()
        object PermissionDenied : SyncResult()
        object NoBackupFound : SyncResult()
        data class Error(val exception: Exception) : SyncResult()
    }

    private fun getDriveService(syncEmail: String?): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        
        // Identity Management
        val resolvedEmail = syncEmail ?: account.email ?: ""
        if (syncEmail != null) {
            val accountEmail = account.email ?: syncEmail
            if (!accountEmail.equals(syncEmail, ignoreCase = true)) {
                Log.w(TAG, "Identity mismatch: Sync requested for $syncEmail but signed-in Google account is $accountEmail")
                return null
            }
            if (!syncEmail.lowercase().endsWith("@gmail.com")) {
                Log.w(TAG, "Sync skipped: Profile email $syncEmail is not a Gmail address")
                return null
            }
        }

        // Check for required scope
        if (!GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            Log.w(TAG, "Permission denied: Missing DRIVE_APPDATA scope")
            return null
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        if (resolvedEmail.isNotBlank()) {
            credential.selectedAccount = android.accounts.Account(resolvedEmail, "com.google")
        } else {
            credential.selectedAccount = account.account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Police Mobile Directory").build()
    }

    fun hasDrivePermission(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
    }

    private fun getSyncStatus(syncEmail: String?): SyncResult? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return SyncResult.NoGoogleAccount
        if (syncEmail != null) {
            val accountEmail = account.email ?: syncEmail
            if (!accountEmail.equals(syncEmail, ignoreCase = true)) {
                return SyncResult.AccountMismatch(syncEmail, accountEmail)
            }
            if (!syncEmail.lowercase().endsWith("@gmail.com")) {
                return SyncResult.NotGmail(syncEmail)
            }
        }
        if (!GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            return SyncResult.PermissionDenied
        }
        return null // Status is OK
    }

    suspend fun uploadBackup(
        balance: LeaveBalance?,
        entries: List<LeaveEntry>,
        creditLogs: List<LeaveCreditLog>,
        syncEmail: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val status = getSyncStatus(syncEmail)
            if (status != null) return@withContext status

            val service = getDriveService(syncEmail) ?: return@withContext SyncResult.Error(Exception("Failed to initialize Drive service"))
            
            val backupData = LeaveBackupData(balance, entries, creditLogs)
            val jsonString = gson.toJson(backupData)
            
            val tempFile = java.io.File(context.cacheDir, BACKUP_FILE_NAME)
            tempFile.writeText(jsonString)

            val existingFileId = findBackupFile(service)
            val metadata = File().apply {
                name = BACKUP_FILE_NAME
                parents = Collections.singletonList("appDataFolder")
            }
            val content = FileContent("application/json", tempFile)

            if (existingFileId != null) {
                service.files().update(existingFileId, null, content).execute()
                Log.d(TAG, "Backup updated in Google Drive")
            } else {
                service.files().create(metadata, content).execute()
                Log.d(TAG, "New backup created in Google Drive")
            }
            SyncResult.Success()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading backup to Drive", e)
            SyncResult.Error(e)
        }
    }

    suspend fun downloadBackup(syncEmail: String? = null): SyncResult = withContext(Dispatchers.IO) {
        try {
            val status = getSyncStatus(syncEmail)
            if (status != null) return@withContext status

            val service = getDriveService(syncEmail) ?: return@withContext SyncResult.Error(Exception("Failed to initialize Drive service"))
            
            val fileId = findBackupFile(service) ?: return@withContext SyncResult.NoBackupFound

            val outputStream = java.io.ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            
            val jsonString = outputStream.toString()
            val data = gson.fromJson(jsonString, LeaveBackupData::class.java)
            SyncResult.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading backup from Drive", e)
            SyncResult.Error(e)
        }
    }

    private fun findBackupFile(service: Drive): String? {
        val result = service.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id, name)")
            .execute()

        val files = result.files
        return if (files == null || files.isEmpty()) null else files[0].id
    }
}
