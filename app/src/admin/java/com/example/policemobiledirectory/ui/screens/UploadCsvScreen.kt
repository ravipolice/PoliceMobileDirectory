package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadCsvScreen(
    navController: NavController, 
    viewModel: EmployeeViewModel
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> 
            if (uri != null) {
                selectedUri = uri
                uploadProgress = null
                uploadStatus = null
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Roster Database", fontWeight = FontWeight.Bold) },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF008080), // Premium Teal
                    scrolledContainerColor = Color(0xFF008080),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF2F9F9), Color(0xFFE6F2F2))
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Header card describing upload process
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF008080))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Database Sync Manager",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF004D4D)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Import the complete Admin Roster from a Tab-Separated Values (.tsv) or Comma-Separated Values (.csv) file directly to the Firestore database. Wiping the remote collection is automated.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // File picker upload zone
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedUri != null) Color(0xFF008080) else Color(0xFFB2DFDB)
                    ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedUri == null) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Icon",
                                modifier = Modifier.height(64.dp).width(64.dp),
                                tint = Color(0xFF008080)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No database file selected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Supports standard roster .tsv or .csv files",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Choose File", color = Color.White)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Check Circle Icon",
                                modifier = Modifier.height(64.dp).width(64.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "File Selected Successfully",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                selectedUri?.lastPathSegment ?: "Selected File",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F9F9)),
                                border = BorderStroke(1.dp, Color(0xFF008080)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Change File", color = Color(0xFF008080))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Indicator
                uploadStatus?.let { status ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (status.startsWith("Error")) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                status,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status.startsWith("Error")) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                            
                            uploadProgress?.let { progress ->
                                if (progress >= 0f) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF008080),
                                        trackColor = Color(0xFFB2DFDB)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${(progress * 100).toInt()}% Completed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.DarkGray,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Sync Button
                Button(
                    onClick = {
                        selectedUri?.let { uri ->
                            viewModel.uploadCsv(uri) { progress, status ->
                                uploadProgress = progress
                                uploadStatus = status
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedUri != null && (uploadProgress == null || uploadProgress == -1.0f || uploadProgress == 1.0f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF008080),
                        disabledContainerColor = Color(0xFFB2DFDB)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (uploadProgress != null && uploadProgress!! < 1.0f && uploadProgress!! >= 0f) "Processing..." else "Upload and Process",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Guidance section
                CsvParsingPreview()
            }
        }
    }
}

@Composable
fun CsvParsingPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Required Column Mapping Guidance",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004D4D)
            )
            Spacer(modifier = Modifier.height(12.dp))
            val guidanceRows = listOf(
                "kgid" to "Primary unique identifier for the employee record.",
                "name" to "Full legal name of the admin employee.",
                "email" to "Used for authentication mapping/fallback.",
                "mobile1" to "Primary contact phone number.",
                "rank" to "Designation/Rank of employee.",
                "station" to "Assigned police station or office location.",
                "district" to "Assigned district/division command.",
                "unit" to "The organizational parent unit (e.g. Ministerial)."
            )
            guidanceRows.forEach { (field, description) ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "• $field: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF008080),
                        modifier = Modifier.width(90.dp)
                    )
                    Text(
                        description,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

