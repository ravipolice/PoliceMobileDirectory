package com.example.policemobiledirectory.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.repository.PersonalDriveRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PayslipParser
import com.example.policemobiledirectory.utils.AIPayslipParser
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayslipViewModel @Inject constructor(
    private val driveRepository: PersonalDriveRepository,
    private val aiPayslipParser: AIPayslipParser
) : ViewModel() {

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _parseStatus = MutableStateFlow<OperationStatus<Map<String, String>>>(OperationStatus.Idle)
    val parseStatus: StateFlow<OperationStatus<Map<String, String>>> = _parseStatus.asStateFlow()

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()

    private val _parsedData = MutableStateFlow<Map<String, String>>(emptyMap())
    val parsedData: StateFlow<Map<String, String>> = _parsedData.asStateFlow()

    private val _folderId = MutableStateFlow<String?>(null)
    val folderId: StateFlow<String?> = _folderId.asStateFlow()

    private val _spreadsheetId = MutableStateFlow<String?>(null)
    val spreadsheetId: StateFlow<String?> = _spreadsheetId.asStateFlow()

    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
        if (uri == null) {
            _parseStatus.value = OperationStatus.Idle
            _uploadStatus.value = OperationStatus.Idle
            _parsedData.value = emptyMap()
        }
    }

    fun parseImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _parseStatus.value = OperationStatus.Loading
            
            // 1. First try fast local Regex parsing
            val regexResult = PayslipParser.parsePayslip(context, uri)
            
            if (regexResult is PayslipParser.ParseResult.Success) {
                val data = regexResult.data
                // Check if critical fields are missing
                val isWeak = data["GROSS_SALARY"].isNullOrBlank() || data["NET_SALARY"].isNullOrBlank()
                
                if (!isWeak) {
                    _parsedData.value = data
                    _parseStatus.value = OperationStatus.Success(data)
                    return@launch
                }
                
                // 2. If regex is weak, use high-accuracy AI parsing
                val aiData = aiPayslipParser.parseWithAI(regexResult.rawText)
                if (aiData != null) {
                    _parsedData.value = aiData
                    _parseStatus.value = OperationStatus.Success(aiData)
                } else {
                    // Fallback to what regex found if AI fails
                    _parsedData.value = data
                    _parseStatus.value = OperationStatus.Success(data)
                }
            } else if (regexResult is PayslipParser.ParseResult.Error) {
                _parseStatus.value = OperationStatus.Error(regexResult.message)
            }
        }
    }

    fun updateParsedField(key: String, value: String) {
        val currentMap = _parsedData.value.toMutableMap()
        currentMap[key] = value
        _parsedData.value = currentMap
    }

    fun uploadToDriveAndSheets(
        context: Context,
        credential: GoogleAccountCredential,
        uri: Uri
    ) {
        val data = _parsedData.value
        if (data.isEmpty()) {
            _uploadStatus.value = OperationStatus.Error("No data to upload. Please parse a payslip first.")
            return
        }

        val kgid = data["KGID"]
        val month = data["MONTH"]

        if (kgid.isNullOrBlank()) {
            _uploadStatus.value = OperationStatus.Error("KGID is missing. Please enter the KGID in the parsed data table.")
            return
        }

        val fileName = if (!month.isNullOrBlank()) "Payslip_${kgid}_$month.jpg" else "Payslip_${kgid}_${System.currentTimeMillis()}.jpg"

        viewModelScope.launch {
            _uploadStatus.value = OperationStatus.Loading
            
            // 1. Get/Create Folder
            val folderResult = driveRepository.createOrGetPayslipsFolder(credential)
            if (folderResult is RepoResult.Error) {
                _uploadStatus.value = OperationStatus.Error(folderResult.message ?: "Failed to get Drive folder")
                return@launch
            }
            val folderId = (folderResult as RepoResult.Success).data!!

            // 2. Upload Image
            val uploadResult = driveRepository.uploadPayslipImage(context, credential, uri, folderId, fileName)
            if (uploadResult is RepoResult.Error) {
                _uploadStatus.value = OperationStatus.Error(uploadResult.message ?: "Failed to upload image")
                return@launch
            }

            // 3. Prepare data for Sheets
            val headers = PayslipParser.MASTER_COLUMNS
            val masterSet = headers.toSet()
            
            // Collect any custom fields added by the user that aren't in the master list
            val customFields = data.filter { it.key !in masterSet && it.value.isNotBlank() }
            val customString = customFields.entries.joinToString(" | ") { "${it.key}: ${it.value}" }
            
            val rowData = headers.map { header ->
                when {
                    header == "REMARKS" -> {
                        val existingRemarks = data["REMARKS"] ?: ""
                        if (customString.isNotEmpty()) {
                            if (existingRemarks.isNotEmpty()) "$existingRemarks | $customString" else customString
                        } else existingRemarks
                    }
                    else -> data[header] ?: ""
                }
            }

            // 4. Append to Sheet
            val sheetResult = driveRepository.appendToPayslipSheet(credential, folderId, kgid, headers, rowData)
            if (sheetResult is RepoResult.Error) {
                _uploadStatus.value = OperationStatus.Error(sheetResult.message ?: "Failed to write to Google Sheet")
                return@launch
            }
            val spreadsheetId = (sheetResult as RepoResult.Success).data!!

            _folderId.value = folderId
            _spreadsheetId.value = spreadsheetId
            _uploadStatus.value = OperationStatus.Success("Payslip saved to your Google Drive successfully!")
        }
    }

    fun fetchDriveIdentifiers(credential: GoogleAccountCredential, kgid: String) {
        viewModelScope.launch {
            when (val result = driveRepository.findExistingDriveIdentifiers(credential, kgid)) {
                is RepoResult.Success -> {
                    _folderId.value = result.data?.folderId
                    _spreadsheetId.value = result.data?.spreadsheetId
                }
                else -> Unit
            }
        }
    }

    fun resetUploadStatus() {
        _uploadStatus.value = OperationStatus.Idle
    }
}
