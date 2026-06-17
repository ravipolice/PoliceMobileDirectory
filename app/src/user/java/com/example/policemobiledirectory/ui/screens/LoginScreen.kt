package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import com.example.policemobiledirectory.utils.ToastUtil
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.AuthViewModel
import com.example.policemobiledirectory.viewmodel.SettingsViewModel
import com.example.policemobiledirectory.ui.screens.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onLoginSuccess: (Boolean) -> Unit,
    onRegisterNewUser: (String?, String?) -> Unit,
    onForgotPinClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onSwitchGoogleAccountClicked: () -> Unit = {},
    onThemeToggle: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context.findActivity())?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var emailToRegister by remember { mutableStateOf<String?>(null) }
    var nameToRegister by remember { mutableStateOf<String?>(null) }
    var showRejectionDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    val authStatus by viewModel.authStatus.collectAsState()
    val googleSignInEvent by viewModel.googleSignInUiEvent.collectAsState()
    val isAccountPickerLoading by viewModel.isGoogleAccountPickerLoading.collectAsState()
    var isEmailPinExpanded by remember { mutableStateOf(false) }

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val encryptedPin by viewModel.encryptedPin.collectAsState()
    val biometricIv by viewModel.biometricIv.collectAsState()
    val storedEmail by viewModel.sessionManager.userEmail.collectAsState(initial = "")

    var loggedInWithPin by remember { mutableStateOf(false) }
    var showBiometricEnrollDialog by remember { mutableStateOf(false) }
    var pendingNavigationAdminState by remember { mutableStateOf(false) }
    var hasAutoTriggeredBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(storedEmail) {
        if (storedEmail.isNotBlank() && email.isBlank()) {
            email = storedEmail
        }
    }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            isEmailPinExpanded = true
        }
    }

    val failedAttempts by viewModel.failedPinAttempts.collectAsState()
    val lockoutTimestamp by viewModel.pinLockoutTimestamp.collectAsState()
    var lockoutTimeRemaining by remember { mutableStateOf(0L) }
    val isLockedOut = failedAttempts >= 5 && lockoutTimeRemaining > 0

    LaunchedEffect(failedAttempts, lockoutTimestamp) {
        if (failedAttempts >= 5 && lockoutTimestamp > 0L) {
            while (true) {
                val elapsed = System.currentTimeMillis() - lockoutTimestamp
                val remaining = 600000L - elapsed
                if (remaining > 0) {
                    lockoutTimeRemaining = remaining
                    kotlinx.coroutines.delay(1000L)
                } else {
                    lockoutTimeRemaining = 0L
                    break
                }
            }
        } else {
            lockoutTimeRemaining = 0L
        }
    }

    LaunchedEffect(isBiometricEnabled, encryptedPin, biometricIv, storedEmail, isLockedOut) {
        if (isBiometricEnabled && encryptedPin != null && biometricIv != null && !hasAutoTriggeredBiometric && !isLockedOut) {
            val emailToUse = email.ifBlank { storedEmail }
            if (emailToUse.isNotBlank()) {
                hasAutoTriggeredBiometric = true
                val activity = com.example.policemobiledirectory.utils.BiometricHelper.findActivity(context)
                if (activity != null) {
                    com.example.policemobiledirectory.utils.BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            val decrypted = com.example.policemobiledirectory.utils.BiometricHelper.decryptPin(
                                encryptedPin!!,
                                biometricIv!!
                            )
                            if (decrypted != null) {
                                viewModel.loginWithPin(emailToUse, decrypted)
                            }
                        },
                        onError = { _, _ -> }
                    )
                }
            }
        }
    }

    // --- STATE OBSERVERS ---

    // Observer for standard PIN-based login
    LaunchedEffect(authStatus) {
        when (val status = authStatus) {
            is OperationStatus.Loading -> isLoading = true
            is OperationStatus.Success<*> -> {
                isLoading = false
                val user = status.data as? Employee
                if (user != null) {
                    ToastUtil.showToast(context, "Welcome ${user.name}")
                    val isAdmin = viewModel.isAdmin.value
                    val isBioAvailable = com.example.policemobiledirectory.utils.BiometricHelper.isBiometricAvailable(context)
                    
                    if (loggedInWithPin && isBioAvailable && !isBiometricEnabled) {
                        pendingNavigationAdminState = isAdmin
                        showBiometricEnrollDialog = true
                    } else {
                        onLoginSuccess(isAdmin)
                    }
                }
            }
            is OperationStatus.Error -> {
                isLoading = false
                ToastUtil.showToast(context, status.message, Toast.LENGTH_LONG)
            }
            else -> isLoading = false
        }
    }

    // --- Listen for Google Sign-In Events ---
    LaunchedEffect(googleSignInEvent) {
        when (val event = googleSignInEvent) {
            is GoogleSignInUiEvent.Loading -> {
                isLoading = true
            }

            is GoogleSignInUiEvent.SignInSuccess -> {
                isLoading = false
                ToastUtil.showToast(context, "Welcome ${event.user.name}")
                onLoginSuccess(event.user.isAdmin)
            }

            is GoogleSignInUiEvent.RegistrationRequired -> {
                isLoading = false
                emailToRegister = event.email
                nameToRegister = event.name
                showRegisterDialog = true
            }

            is GoogleSignInUiEvent.RegistrationPending -> {
                isLoading = false
                ToastUtil.showToast(
                    context, 
                    "Registration for ${event.email} is pending approval. Please wait for an administrator to approve your account.",
                    Toast.LENGTH_LONG
                )
            }

            is GoogleSignInUiEvent.RegistrationRejected -> {
                isLoading = false
                emailToRegister = event.email
                rejectionReason = event.reason
                showRejectionDialog = true
            }

            is GoogleSignInUiEvent.Error -> {
                isLoading = false
                ToastUtil.showToast(context, event.message, Toast.LENGTH_LONG)
            }

            else -> isLoading = false
        }
    }

// --- Registration Required Dialog ---
    if (showRegisterDialog && emailToRegister != null) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Registration Authorization") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This Google account is not currently authorized to access the Police Mobile Directory. Do you wish to submit a registration request for this account?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = emailToRegister ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRegisterDialog = false
                    onRegisterNewUser(emailToRegister!!, nameToRegister)
                }) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRegisterDialog = false
                    onLogout() // 1️⃣ Clear session and Google state
                    onGoogleSignInClicked() // 2️⃣ Re-trigger account picker
                }) {
                    Text("Use another account")
                }
            }
        )
    }

    // --- Registration Rejected Dialog ---
    if (showRejectionDialog) {
        AlertDialog(
            onDismissRequest = { showRejectionDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Registration Rejected") 
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your registration for $emailToRegister was rejected by an administrator.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Reason: $rejectionReason",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                    Text(
                        "You can register again by correcting the information above.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRejectionDialog = false
                    onRegisterNewUser(emailToRegister!!, null)
                }) {
                    Text("Register Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectionDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (showBiometricEnrollDialog) {
        AlertDialog(
            onDismissRequest = {
                showBiometricEnrollDialog = false
                onLoginSuccess(pendingNavigationAdminState)
            },
            title = { Text("Enable Biometric Login") },
            text = {
                Text("Would you like to enable fingerprint or face recognition for faster, secure logins in the future?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showBiometricEnrollDialog = false
                    val activity = com.example.policemobiledirectory.utils.BiometricHelper.findActivity(context)
                    if (activity != null) {
                        com.example.policemobiledirectory.utils.BiometricHelper.showBiometricPrompt(
                            activity = activity,
                            title = "Verify Biometric",
                            subtitle = "Verify to enable biometric login",
                            onSuccess = {
                                val success = viewModel.enableBiometric(pin)
                                if (success) {
                                    ToastUtil.showToast(context, "Biometric login enabled successfully!")
                                } else {
                                    ToastUtil.showToast(context, "Failed to enable biometric login.")
                                }
                                onLoginSuccess(pendingNavigationAdminState)
                            },
                            onError = { _, err ->
                                ToastUtil.showToast(context, "Verification failed: $err")
                                onLoginSuccess(pendingNavigationAdminState)
                            }
                        )
                    } else {
                        onLoginSuccess(pendingNavigationAdminState)
                    }
                }) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBiometricEnrollDialog = false
                    onLoginSuccess(pendingNavigationAdminState)
                }) {
                    Text("Skip")
                }
            }
        )
    }

    // --- UI ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- Top Gradient Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF004D40), // Deep Midnight Teal
                            Color(0xFF00796B), // Primary Teal Dark
                            Color(0xFF009688)  // Primary Teal
                        )
                    )
                )
        )

        // --- Main Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo in the header
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(125.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(6.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(24.dp))

            // White Card for the form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(20.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Police Mobile Directory",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00796B),
                            fontSize = 26.sp, letterSpacing = 0.5.sp
                        )
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Exclusively for Karnataka State Police Department personnel.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(32.dp))

                    // Google Login Button
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGoogleSignInClicked()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .border(1.5.dp, Color(0xFF00796B), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF37474F)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google / Register",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwitchGoogleAccountClicked()
                        }
                    ) {
                        Text(
                            text = "Switch Google Account",
                            color = Color(0xFF00796B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Divider(Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                        Text("  or  ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Divider(Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "For Offline Use",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Offline Login Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEmailPinExpanded = !isEmailPinExpanded },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00796B))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Login with Email and PIN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    imageVector = if (isEmailPinExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }

                            if (isEmailPinExpanded) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    if (isLockedOut) {
                                        val minutes = (lockoutTimeRemaining / 1000) / 60
                                        val seconds = (lockoutTimeRemaining / 1000) % 60
                                        Text(
                                            text = "Too many failed attempts. Try again in ${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address") },
                                        enabled = !isLockedOut,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = pin,
                                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                                        label = { Text("6-Digit PIN") },
                                        enabled = !isLockedOut,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        trailingIcon = {
                                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                                Icon(
                                                    imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                focusManager.clearFocus()
                                                loggedInWithPin = true
                                                viewModel.loginWithPin(email, pin)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isLockedOut && email.isNotBlank() && pin.length == 6,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF00796B),
                                                contentColor = Color.White
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                        ) {
                                            Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }

                                        if (isBiometricEnabled && encryptedPin != null && biometricIv != null) {
                                            Spacer(Modifier.width(12.dp))
                                            IconButton(
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    val activity = com.example.policemobiledirectory.utils.BiometricHelper.findActivity(context)
                                                    if (activity != null) {
                                                        com.example.policemobiledirectory.utils.BiometricHelper.showBiometricPrompt(
                                                            activity = activity,
                                                            onSuccess = {
                                                                val decrypted = com.example.policemobiledirectory.utils.BiometricHelper.decryptPin(
                                                                    encryptedPin!!,
                                                                    biometricIv!!
                                                                )
                                                                if (decrypted != null) {
                                                                    viewModel.loginWithPin(email.ifBlank { storedEmail }, decrypted)
                                                                } else {
                                                                    ToastUtil.showToast(context, "Decryption failed. Please enter PIN.")
                                                                }
                                                            },
                                                            onError = { _, _ -> }
                                                        )
                                                    } else {
                                                        ToastUtil.showToast(context, "Activity context invalid.")
                                                    }
                                                },
                                                enabled = !isLockedOut,
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .background(Color(0xFF00796B).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                    .border(1.dp, Color(0xFF00796B).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Biometric Login",
                                                    tint = Color(0xFF00796B)
                                                )
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = onForgotPinClicked,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Forgot PIN?", color = Color(0xFF00796B))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))

            // Footer
            Text(
                text = "Developed By Ravikumar J, AHC, DAR\nChikkaballapura",
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    letterSpacing = 0.8.sp
                ),
                textAlign = TextAlign.Center,
                color = Color.DarkGray.copy(alpha = 0.6f),
            )
        }

        // --- Loading Overlay ---
        if (isLoading || isAccountPickerLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    if (isAccountPickerLoading) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Please wait...\nLoading Google accounts",
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}