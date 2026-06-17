package com.example.policemobiledirectory.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.AuthViewModel
import com.example.policemobiledirectory.viewmodel.SettingsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NavigationDrawer(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    employeeViewModel: EmployeeViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onDrivePermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val uploadStatus by employeeViewModel.uploadStatus.collectAsStateWithLifecycle()
    val isUploading = uploadStatus is OperationStatus.Loading
    val saveStatus by employeeViewModel.saveStatus.collectAsStateWithLifecycle()

    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val uCropResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let { uri ->
                viewModel.currentUser.value?.let { emp ->
                    employeeViewModel.saveEmployee(emp, uri)
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val err = result.data?.let { UCrop.getError(it) }
            Toast.makeText(context, err?.message ?: "Crop failed", Toast.LENGTH_SHORT).show()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCameraUri?.let { src ->
                launchUCrop(context, src, uCropResultLauncher)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { launchUCrop(context, it, uCropResultLauncher) }
    }

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is RepoResult.Success<*> -> {
                Toast.makeText(context, "Profile photo updated successfully", Toast.LENGTH_SHORT).show()
                employeeViewModel.resetSaveStatus()
                viewModel.loadSession()
            }
            is RepoResult.Error -> {
                val errMsg = (saveStatus as RepoResult.Error).message ?: "Failed to update profile photo"
                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                employeeViewModel.resetSaveStatus()
            }
            else -> {}
        }
    }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val hasFullDriveAccess by viewModel.hasFullDriveAccess.collectAsStateWithLifecycle()
    val hasAppData by viewModel.hasAppDataAccess.collectAsStateWithLifecycle()
    val hasDriveFile by viewModel.hasDriveFileAccess.collectAsStateWithLifecycle()
    val hasSpreadsheets by viewModel.hasSpreadsheetsAccess.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    
    val currentRoute = navController.currentDestination?.route

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkGoogleDriveAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentUser) {
        viewModel.checkGoogleDriveAccess(context)
    }




    ModalDrawerSheet(
        modifier = Modifier
            .width(280.dp),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
        ) {

            // ============================================================
            // 🔹 TOP SECTION: PROFILE CARD
            // ============================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF008080),
                                Color(0xFF00E676)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image with Blood Group in top right corner
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable(enabled = !isUploading) { showSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(currentUser?.photoUrl)
                                    .placeholder(R.drawable.officer)
                                    .error(R.drawable.officer)
                                    .crossfade(true)
                                    .scale(Scale.FILL)
                                    .build()
                            )

                            Image(
                                painter = painter,
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize()
                            )

                             // Loading Overlay
                             if (isUploading) {
                                 Box(
                                     modifier = Modifier
                                         .fillMaxSize()
                                         .background(Color.Black.copy(alpha = 0.6f)),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     CircularProgressIndicator(
                                         color = Color.White,
                                         modifier = Modifier.size(32.dp)
                                     )
                                 }
                             }
                         }

                         // Edit camera badge in bottom right corner
                         Box(
                             modifier = Modifier
                                 .align(Alignment.BottomEnd)
                                 .size(28.dp)
                                 .offset(x = 2.dp, y = 2.dp)
                                 .clip(CircleShape)
                                 .background(MaterialTheme.colorScheme.primary)
                                 .border(1.5.dp, Color.White, CircleShape)
                                 .clickable(enabled = !isUploading) { showSourceDialog = true },
                             contentAlignment = Alignment.Center
                         ) {
                             Icon(
                                 imageVector = Icons.Default.PhotoCamera,
                                 contentDescription = "Edit photo",
                                 tint = Color.White,
                                 modifier = Modifier.size(16.dp)
                             )
                         }
                     }
 
                     Spacer(modifier = Modifier.height(8.dp))
                     
                     // Name + Rank
                     Text(
                         text = buildAnnotatedString {
                             withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                 append(currentUser?.name ?: "")
                             }
                             currentUser?.displayRank?.takeIf { it.isNotBlank() }?.let { rank ->
                                 append("  ")
                                 withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.9f))) {
                                     append(rank)
                                 }
                             }
                         },
                         style = MaterialTheme.typography.titleLarge.copy(
                             color = Color.White
                         ),
                         textAlign = TextAlign.Center
                     )

                     Spacer(modifier = Modifier.height(6.dp))
                    
                    // Compact Metadata Row 1: KGID
                    currentUser?.kgid?.takeIf { it.isNotBlank() }?.let { kgid ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KGID: $kgid",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Compact Metadata Row 2: Station
                    currentUser?.station?.takeIf { it.isNotBlank() }?.let { station ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = station,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.95f)
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Compact Metadata Row 3: Email
                    currentUser?.email?.takeIf { it.isNotBlank() }?.let { email ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.95f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Blood Group badge in the top-right corner of the header Box
                currentUser?.bloodGroup?.takeIf { it.isNotBlank() }?.let { bg ->
                    val formattedBg = if (bg.trim() == "??") {
                        "??"
                    } else {
                        bg.uppercase()
                            .replace("POSITIVE", "+")
                            .replace("NEGATIVE", "–")
                            .replace("VE", "")
                            .replace("(", "")
                            .replace(")", "")
                            .trim()
                            .let { clean ->
                                when (clean) {
                                    "A" -> "A+"
                                    "B" -> "B+"
                                    "O" -> "O+"
                                    "AB" -> "AB+"
                                    "A-" -> "A–"
                                    "B-" -> "B–"
                                    "O-" -> "O–"
                                    "AB-" -> "AB–"
                                    else -> clean
                                }
                            }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 12.dp, end = 16.dp)
                            .size(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = formattedBg,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (showSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showSourceDialog = false },
                    title = { Text("Update Profile Photo") },
                    text = { Text("Select a photo source to update your profile picture.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showSourceDialog = false
                            val uri = createTempCameraUri(context)
                            if (uri != null) {
                                tempCameraUri = uri
                                takePictureLauncher.launch(uri)
                            } else {
                                Toast.makeText(context, "Failed to initialize camera", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Camera")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }) {
                            Text("Gallery")
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ============================================================
            // 🔹 MIDDLE SECTION: MENU ITEMS
            // ============================================================
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DrawerItem(
                    icon = Icons.Default.Person,
                    text = "My Profile",
                    selected = currentRoute == Routes.MY_PROFILE,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.MY_PROFILE) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
                            }
                        }
                    }
                )

                // Admin Panel link removed




                DrawerItem(
                    icon = Icons.Default.Info,
                    text = "About App",
                    selected = currentRoute == Routes.ABOUT,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.ABOUT) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
                            }
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ============================================================
            // 🔹 BOTTOM SECTION: LOGOUT + CONTACT
            // ============================================================
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                var showLogoutDialog by remember { mutableStateOf(false) }
                var isLoggingOut by remember { mutableStateOf(false) }
                var showSupportDialog by remember { mutableStateOf(false) }
                val clipboardManager = LocalClipboardManager.current

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
                        confirmButton = {
                            TextButton(
                                enabled = !isLoggingOut,
                                onClick = {
                                    isLoggingOut = true
                                    scope.launch {
                                        drawerState.close()
                                        onLogout()
                                        isLoggingOut = false
                                        showLogoutDialog = false
                                    }
                                }
                            ) {
                                if (isLoggingOut) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Logout", color = Color.Red)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = !isLoggingOut,
                                onClick = { showLogoutDialog = false }
                            ) { Text("Cancel") }
                        },
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        title = { Text("Confirm Logout") },
                        text = { Text("Are you sure you want to log out?") }
                    )
                }

                if (showSupportDialog) {
                    AlertDialog(
                        onDismissRequest = { showSupportDialog = false },
                        icon = { Icon(Icons.Default.Email, contentDescription = null) },
                        title = { Text("Contact Support") },
                        text = { Text("Email: noreply.pmdapp@gmail.com\nWe usually respond quickly.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:noreply.pmdapp@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "App Support Request")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                }
                                showSupportDialog = false
                            }) {
                                Text("Email")
                            }
                        },
                        dismissButton = {
                            Row {
                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString("noreply.pmdapp@gmail.com"))
                                    Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                                }) { Text("Copy") }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(onClick = { showSupportDialog = false }) { Text("Close") }
                            }
                        }
                    )
                }

                val driveStatusText = when {
                    hasFullDriveAccess -> "Google Drive Access: Full"
                    hasAppData || hasDriveFile || hasSpreadsheets -> "Google Drive: Partial Access"
                    else -> "Google Drive Access"
                }
                
                val driveStatusColor = when {
                    hasFullDriveAccess -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
                    hasAppData || hasDriveFile || hasSpreadsheets -> Color(0xFFFFA000) // Amber for partial
                    else -> MaterialTheme.colorScheme.error
                }

                // Biometric Login Switch Row
                val isBiometricAvailable = remember(context) { com.example.policemobiledirectory.utils.BiometricHelper.isBiometricAvailable(context) }
                var showPinDialog by remember { mutableStateOf(false) }

                if (showPinDialog) {
                    var pinText by remember { mutableStateOf("") }
                    var pinError by remember { mutableStateOf<String?>(null) }
                    var isEnrolling by remember { mutableStateOf(false) }
                    val activity = remember(context) { com.example.policemobiledirectory.utils.BiometricHelper.findActivity(context) }

                    AlertDialog(
                        onDismissRequest = { if (!isEnrolling) showPinDialog = false },
                        title = { Text("Enable Biometric Login") },
                        text = {
                            Column {
                                Text(
                                    text = "Please enter your 6-digit PIN to verify your identity and enable biometric login.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                OutlinedTextField(
                                    value = pinText,
                                    onValueChange = { input ->
                                        if (input.length <= 6 && input.all { it.isDigit() }) {
                                            pinText = input
                                            pinError = null
                                        }
                                    },
                                    label = { Text("6-Digit PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    isError = pinError != null,
                                    supportingText = {
                                        if (pinError != null) {
                                            Text(pinError!!, color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = !isEnrolling && pinText.length == 6,
                                onClick = {
                                    val email = currentUser?.email ?: ""
                                    if (email.isBlank()) {
                                        pinError = "User email not found. Please log in again."
                                        return@TextButton
                                    }
                                    if (activity == null) {
                                        pinError = "Activity context not found."
                                        return@TextButton
                                    }
                                    isEnrolling = true
                                    viewModel.verifyAndEnableBiometric(
                                        email = email,
                                        pin = pinText,
                                        activity = activity,
                                        onSuccess = {
                                            isEnrolling = false
                                            showPinDialog = false
                                            Toast.makeText(context, "Biometric login enabled successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { errorMsg ->
                                            isEnrolling = false
                                            pinError = errorMsg
                                        }
                                    )
                                }
                            ) {
                                if (isEnrolling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Enable")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = !isEnrolling,
                                onClick = { showPinDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Login",
                        tint = if (isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biometric Login",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (!isBiometricAvailable) {
                            Text(
                                text = "Not supported on this device",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        enabled = isBiometricAvailable,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                viewModel.disableBiometrics()
                                Toast.makeText(context, "Biometric login disabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Google Drive Access",
                        tint = if (hasFullDriveAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Drive Access",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        val driveSubText = when {
                            hasFullDriveAccess -> "Connected (Full Access)"
                            hasAppData || hasDriveFile || hasSpreadsheets -> "Partial Access"
                            else -> "Disconnected"
                        }
                        val driveSubColor = when {
                            hasFullDriveAccess -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
                            hasAppData || hasDriveFile || hasSpreadsheets -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                        Text(
                            text = driveSubText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = driveSubColor
                            )
                        )
                    }
                    Switch(
                        checked = hasFullDriveAccess,
                        onCheckedChange = { checked ->
                            if (checked) {
                                scope.launch {
                                    drawerState.close()
                                    onDrivePermissionRequest()
                                }
                            } else {
                                scope.launch {
                                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                    ).build()
                                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                    client.signOut().addOnCompleteListener {
                                        viewModel.checkGoogleDriveAccess(context)
                                        Toast.makeText(context, "Google Drive disconnected", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    text = "Logout",
                    textColor = Color.Red,
                    onClick = { showLogoutDialog = true }
                )

                DrawerItem(
                    icon = Icons.Default.Star,
                    text = "Rate App",
                    onClick = {
                        val packageName = "com.pmd.userapp"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=$packageName")
                            setPackage("com.android.vending")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to browser if Play Store app is not installed
                            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            }
                            context.startActivity(browserIntent)
                        }
                    }
                )

                DrawerItem(
                    icon = Icons.Default.Email,
                    text = "Contact Support",
                    onClick = { showSupportDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // App Version Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Version: ${com.example.policemobiledirectory.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

// ============================================================
// 🔹 Reusable Drawer Item Composable
// ============================================================
@Composable
fun DrawerItem(
    icon: ImageVector,
    text: String,
    selected: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        Color.Transparent

    val iconColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val textStyle = if (selected) {
        MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    } else {
        MaterialTheme.typography.bodyLarge.copy(
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = textStyle
        )
    }
}

private fun launchUCrop(context: Context, sourceUri: Uri, launcher: ActivityResultLauncher<Intent>) {
    try {
        val destFile = File(context.cacheDir, "ucrop_${UUID.randomUUID()}.jpg")
        val destUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destFile
        )
        val options = UCrop.Options().apply {
            setToolbarTitle("Crop Image")
            setCircleDimmedLayer(true)
            setFreeStyleCropEnabled(false)
            setCompressionQuality(90)
            setToolbarColor(androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_light_primary))
            setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_light_primary))
        }
        val intent = UCrop.of(sourceUri, destUri).withAspectRatio(1f, 1f).withOptions(options).getIntent(context)
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to launch image cropper", Toast.LENGTH_SHORT).show()
    }
}

private fun createTempCameraUri(context: Context): Uri? {
    return try {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        null
    }
}
