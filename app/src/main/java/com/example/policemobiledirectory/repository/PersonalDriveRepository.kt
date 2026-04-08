package com.example.policemobiledirectory.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalDriveRepository @Inject constructor() {

    companion object {
        const val FOLDER_NAME = "Pay Data Storage"
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    }

    private fun getDriveService(credential: GoogleAccountCredential): Drive {
        val httpTransport = NetHttpTransport()
        return Drive.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName("PMD Payslip Parser")
            .build()
    }

    private fun getSheetsService(credential: GoogleAccountCredential): Sheets {
        val httpTransport = NetHttpTransport()
        return Sheets.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName("PMD Payslip Parser")
            .build()
    }

    suspend fun createOrGetPayslipsFolder(credential: GoogleAccountCredential): RepoResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(credential)
                
                // 1. Check if folder exists
                val query = "mimeType='application/vnd.google-apps.folder' and name='$FOLDER_NAME' and trashed=false"
                val result = drive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                val files = result.files
                if (files != null && files.isNotEmpty()) {
                    return@withContext RepoResult.Success(files[0].id)
                }

                // 2. Create the folder
                val fileMetadata = File().apply {
                    name = FOLDER_NAME
                    mimeType = "application/vnd.google-apps.folder"
                }
                
                val folder = drive.files().create(fileMetadata)
                    .setFields("id")
                    .execute()
                
                RepoResult.Success(folder.id)
            } catch (e: Exception) {
                Log.e("PersonalDriveRepo", "Error getting/creating folder", e)
                RepoResult.Error(e, "Failed to access personal Google Drive. Please ensure you granted permission.")
            }
        }
    }

    suspend fun uploadPayslipImage(
        context: Context,
        credential: GoogleAccountCredential,
        uri: Uri,
        folderId: String,
        fileName: String
    ): RepoResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(credential)
                
                // 1. Check if file already exists to prevent duplicates
                val query = "name='$fileName' and '$folderId' in parents and trashed=false"
                val existingResult = drive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute()
                
                val existingFiles = existingResult.files
                
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Could not open file")
                val mediaContent = InputStreamContent(mimeType, inputStream)

                val fileId = if (existingFiles != null && existingFiles.isNotEmpty()) {
                    // Update existing file
                    val existingId = existingFiles[0].id
                    drive.files().update(existingId, null, mediaContent)
                        .setFields("id")
                        .execute()
                        .id
                } else {
                    // Create new file
                    val fileMetadata = File().apply {
                        name = fileName
                        parents = listOf(folderId)
                    }
                    drive.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute()
                        .id
                }

                RepoResult.Success(fileId)
            } catch (e: Exception) {
                Log.e("PersonalDriveRepo", "Error uploading file", e)
                RepoResult.Error(e, "Failed to upload payslip image to Google Drive.")
            }
        }
    }

    suspend fun appendToPayslipSheet(
        credential: GoogleAccountCredential,
        folderId: String,
        kgid: String,
        headers: List<String>,
        rowData: List<String>
    ): RepoResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val drive = getDriveService(credential)
                val sheets = getSheetsService(credential)
                val sheetName = "Payslips_$kgid"

                // 1. Find if sheet exists in the Payslips folder
                val query = "mimeType='application/vnd.google-apps.spreadsheet' and name='$sheetName' and trashed=false and '$folderId' in parents"
                val result = drive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                val files = result.files
                val spreadsheetId: String

                if (files.isNullOrEmpty()) {
                    // Create new spreadsheet
                    val fileMetadata = File().apply {
                        name = sheetName
                        mimeType = "application/vnd.google-apps.spreadsheet"
                        parents = listOf(folderId)
                    }
                    val newSheetFile = drive.files().create(fileMetadata).setFields("id").execute()
                    spreadsheetId = newSheetFile.id
                } else {
                    spreadsheetId = files[0].id
                }

                // 2. Ensure headers exist by checking if A1 is empty
                val checkResult = sheets.spreadsheets().values()
                    .get(spreadsheetId, "A1")
                    .execute()
                
                if (checkResult.getValues() == null || checkResult.getValues().isEmpty()) {
                    val headerBody = ValueRange().setValues(listOf(headers))
                    sheets.spreadsheets().values()
                        .update(spreadsheetId, "A1", headerBody)
                        .setValueInputOption("USER_ENTERED")
                        .execute()
                }

                // 3. Append the row data
                val rowBody = ValueRange().setValues(listOf(rowData))
                sheets.spreadsheets().values()
                    .append(spreadsheetId, "A1", rowBody)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                RepoResult.Success(spreadsheetId)
            } catch (e: Exception) {
                Log.e("PersonalDriveRepo", "Error appending to sheet", e)
                RepoResult.Error(e, "Google Sheets Error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    data class DriveIdentifiers(val folderId: String?, val spreadsheetId: String?)

    suspend fun findExistingDriveIdentifiers(
        credential: GoogleAccountCredential,
        kgid: String
    ): RepoResult<DriveIdentifiers> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(credential)
            
            // 1. Find Folder
            val folderQuery = "mimeType='application/vnd.google-apps.folder' and name='$FOLDER_NAME' and trashed=false"
            val folderResult = drive.files().list()
                .setQ(folderQuery)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            val folderId = folderResult.files?.firstOrNull()?.id
            if (folderId == null) return@withContext RepoResult.Success(DriveIdentifiers(null, null))

            // 2. Find Spreadsheet
            val sheetName = "Payslips_$kgid"
            val sheetQuery = "mimeType='application/vnd.google-apps.spreadsheet' and name='$sheetName' and trashed=false and '$folderId' in parents"
            val sheetResult = drive.files().list()
                .setQ(sheetQuery)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            val spreadsheetId = sheetResult.files?.firstOrNull()?.id
            
            RepoResult.Success(DriveIdentifiers(folderId, spreadsheetId))
        } catch (e: Exception) {
            Log.e("PersonalDriveRepo", "Error finding identifiers", e)
            RepoResult.Error(e, "Failed to locate your Drive records.")
        }
    }
}
