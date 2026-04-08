package com.example.policemobiledirectory.ui.screens

import android.accounts.Account
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.PayslipViewModel
import com.example.policemobiledirectory.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import android.content.Intent
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.TableChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipUploadScreen(
    onNavigateBack: () -> Unit,
    onDrivePermissionRequest: () -> Unit,
    viewModel: PayslipViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val selectedUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val parseStatus by viewModel.parseStatus.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val parsedData by viewModel.parsedData.collectAsStateWithLifecycle()
    val driveAccountEmail by authViewModel.driveAccountEmail.collectAsStateWithLifecycle()
    val folderId by viewModel.folderId.collectAsStateWithLifecycle()
    val spreadsheetId by viewModel.spreadsheetId.collectAsStateWithLifecycle()

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var newFieldName by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setSelectedImage(uri)
        if (uri != null) {
            viewModel.parseImage(context, uri)
        }
    }

    // Auto-trigger permission request if an auth error occurs during upload
    LaunchedEffect(uploadStatus) {
        when (uploadStatus) {
            is OperationStatus.Success -> {
                Toast.makeText(context, "Data saved successfully!", Toast.LENGTH_SHORT).show()
            }
            is OperationStatus.Error -> {
                val msg = (uploadStatus as OperationStatus.Error).message.lowercase()
                if (msg.contains("permission") || msg.contains("auth") || msg.contains("credential") || msg.contains("403")) {
                    onDrivePermissionRequest()
                } else {
                    Toast.makeText(context, (uploadStatus as OperationStatus.Error).message, Toast.LENGTH_LONG).show()
                }
            }
            else -> Unit
        }
    }

    // Fetch existing identifiers if we have a KGID
    LaunchedEffect(currentUser) {
        currentUser?.kgid?.let { kgid ->
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                val scopes = listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA, SheetsScopes.SPREADSHEETS)
                if (GoogleSignIn.hasPermissions(account, *scopes.map { com.google.android.gms.common.api.Scope(it) }.toTypedArray())) {
                    val email = account.email ?: ""
                    val credential = GoogleAccountCredential.usingOAuth2(context, scopes).apply {
                        selectedAccountName = email
                    }
                    viewModel.fetchDriveIdentifiers(credential, kgid)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payslip Keeper", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (folderId != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/folders/$folderId"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Folder, contentDescription = "View Folder", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (spreadsheetId != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/d/$spreadsheetId"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.TableChart, contentDescription = "View Sheet", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (parsedData.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddFieldDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Field")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step 1: Image Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (selectedUri != null) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Selected Payslip",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Change", tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("Tap to Select Payslip Image", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Supported: JPG, PNG, PDF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Step 2: Parsing & Data Table
            when (parseStatus) {
                is OperationStatus.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Scanning Payslip Data...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is OperationStatus.Success, is OperationStatus.Idle -> {
                    if (parsedData.isNotEmpty()) {
                        Text(
                            text = "Review & Edit Extracted Data",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                        )

                        // Compact Table View
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            items(parsedData.keys.toList()) { key ->
                                CompactFieldRow(
                                    label = key,
                                    value = parsedData[key] ?: "",
                                    onValueChange = { newValue ->
                                        viewModel.updateParsedField(key, newValue)
                                    }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (uploadStatus is OperationStatus.Success) {
                            // Post-Save Options
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Successfully Saved!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Your payslip is now in Google Drive and the Sheet has been updated.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setSelectedImage(null)
                                            viewModel.resetUploadStatus()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Scan Another")
                                    }
                                    
                                    if (spreadsheetId != null) {
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/d/$spreadsheetId"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Open Sheet")
                                        }
                                    }
                                }
                            }
                        } else {
                            // Upload Button
                            Button(
                                onClick = {
                                    val savedEmail = driveAccountEmail
                                    val account = if (!savedEmail.isNullOrBlank()) {
                                        GoogleSignIn.getAccountForScopes(context, Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA), Scope(SheetsScopes.SPREADSHEETS))
                                            .takeIf { it.email == savedEmail } ?: GoogleSignIn.getLastSignedInAccount(context)
                                    } else {
                                        GoogleSignIn.getLastSignedInAccount(context)
                                    }

                                    if (account != null) {
                                        val scopes = listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA, SheetsScopes.SPREADSHEETS)
                                        val hasPermission = GoogleSignIn.hasPermissions(account, *scopes.map { com.google.android.gms.common.api.Scope(it) }.toTypedArray())
                                        
                                        if (hasPermission) {
                                            val email = account.email ?: ""
                                            val credential = GoogleAccountCredential.usingOAuth2(context, scopes).apply {
                                                selectedAccountName = email
                                            }
                                            viewModel.uploadToDriveAndSheets(context, credential, selectedUri!!)
                                        } else {
                                            onDrivePermissionRequest()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = uploadStatus !is OperationStatus.Loading && selectedUri != null
                            ) {
                                if (uploadStatus is OperationStatus.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                } else {
                                    val isSaved = uploadStatus is OperationStatus.Success
                                    Icon(
                                        if (isSaved) Icons.Default.CheckCircle else Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (isSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isSaved) "Saved to Drive & Sheet" else "Save to My Drive & Sheet")
                                }
                            }
                        }
                    } else if (parseStatus is OperationStatus.Idle && selectedUri == null) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                "No payslip selected.\nPick an image to start parsing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                is OperationStatus.Error -> {
                    Text(
                        text = "Error: ${(parseStatus as OperationStatus.Error).message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { selectedUri?.let { viewModel.parseImage(context, it) } }) {
                        Text("Retry Scan")
                    }
                }
                else -> {}
            }

            // Success Message
            if (uploadStatus is OperationStatus.Success) {
                AlertDialog(
                    onDismissRequest = { viewModel.resetUploadStatus() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resetUploadStatus(); onNavigateBack() }) {
                            Text("Done")
                        }
                    },
                    title = { Text("Success") },
                    text = { Text((uploadStatus as OperationStatus.Success).data) }
                )
            }

            // Add Field Dialog
            if (showAddFieldDialog) {
                AlertDialog(
                    onDismissRequest = { showAddFieldDialog = false },
                    title = { Text("Add Custom Field") },
                    text = {
                        OutlinedTextField(
                            value = newFieldName,
                            onValueChange = { newFieldName = it },
                            label = { Text("Field Name (e.g. BONUS)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newFieldName.isNotBlank()) {
                                viewModel.updateParsedField(newFieldName.uppercase(), "")
                                showAddFieldDialog = false
                                newFieldName = ""
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddFieldDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun CompactFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label with 0.45 weight
        Text(
            text = label.replace("_", " "),
            modifier = Modifier
                .weight(0.45f)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Divider
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

        // Value with 0.55 weight using BasicTextField for compactness
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(0.55f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (label.any { it.isDigit() } || label == "NET" || label == "GROSS") KeyboardType.Number else KeyboardType.Text
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text("-", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}