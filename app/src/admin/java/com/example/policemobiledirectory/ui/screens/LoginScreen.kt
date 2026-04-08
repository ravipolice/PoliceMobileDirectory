package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import com.example.policemobiledirectory.utils.ToastUtil
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
import com.example.policemobiledirectory.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (Boolean) -> Unit,
    onRegisterNewUser: (String?, String?) -> Unit,
    onForgotPinClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onThemeToggle: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current

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
    val isAccountPickerLoading by viewModel.googleAccountPickerLoading.collectAsState()

    // --- STATE OBSERVERS ---

    LaunchedEffect(authStatus) {
        when (val status = authStatus) {
            is OperationStatus.Loading -> isLoading = true
            is OperationStatus.Success<*> -> {
                isLoading = false
                val user = status.data as? Employee
                if (user != null) {
                    ToastUtil.showToast(context, "Welcome ${user.name}")
                    onLoginSuccess(viewModel.isAdmin.value)
                }
            }
            is OperationStatus.Error -> {
                isLoading = false
                ToastUtil.showToast(context, status.message, Toast.LENGTH_LONG)
            }
            else -> isLoading = false
        }
    }

    LaunchedEffect(googleSignInEvent) {
        when (val event = googleSignInEvent) {
            is GoogleSignInUiEvent.Loading -> isLoading = true
            is GoogleSignInUiEvent.SignInSuccess -> {
                isLoading = false
                ToastUtil.showToast(context, "Welcome ${event.user.name}")
                onLoginSuccess(event.user.isAdmin)
            }
            is GoogleSignInUiEvent.RegistrationRequired -> {
                isLoading = false
                emailToRegister = event.email
                nameToRegister = event.name
                onRegisterNewUser(event.email, event.name)
            }
            is GoogleSignInUiEvent.RegistrationPending -> {
                isLoading = false
                ToastUtil.showToast(context, "Registration for ${event.email} is pending approval.", Toast.LENGTH_LONG)
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

    if (showRegisterDialog && emailToRegister != null) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Account Not Registered") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The Google account below isn’t registered. You can register it now or choose another account.", style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = emailToRegister ?: "", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRegisterDialog = false
                    onRegisterNewUser(emailToRegister!!, nameToRegister)
                }) { Text("Register") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRegisterDialog = false
                    onLogout()
                    onGoogleSignInClicked()
                }) { Text("Use another account") }
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
                        imageVector = Icons.Default.Error,
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.95f
        )

        if (isLoading || isAccountPickerLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                if (isAccountPickerLoading) {
                    Spacer(Modifier.height(16.dp))
                    Text("Loading Google accounts...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(110.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Police Mobile Directory",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF8D722),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                val infiniteTransition = rememberInfiniteTransition(label = "zoom")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Reverse),
                    label = "scale"
                )
                Text(
                    text = "Exclusive for Karnataka State Police Department personnel.\nPlease uninstall if you are not a member of KSP.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    color = Color.Red,
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { onGoogleSignInClicked() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sign in with Google / Register", fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.5f))
                    Text(" or ", modifier = Modifier.padding(horizontal = 12.dp), color = Color.White)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.5f))
                }
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "For Offline use",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                var isEmailPinExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { isEmailPinExpanded = !isEmailPinExpanded },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Login with email and pin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Icon(imageVector = if (isEmailPinExpanded) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                        
                        AnimatedVisibility(visible = isEmailPinExpanded) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it.trim() },
                                    label = { Text("Email") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                                    label = { Text("6-digit PIN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { pinVisible = !pinVisible }) {
                                            Icon(if (pinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) viewModel.loginWithPin(email, pin)
                                        else ToastUtil.showToast(context, "Enter a valid email")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = email.isNotBlank() && pin.length == 6
                                ) { Text("Login") }

                                Text(
                                    "Forgot PIN?",
                                    modifier = Modifier.fillMaxWidth().clickable { onForgotPinClicked() },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "Developed By Ravikumar J\nAHC, DAR Chikkaballapura",
                    fontSize = 15.sp,
                    color = Color(0xFFF8D722),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.login_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.95f
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(110.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Police Mobile Directory",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF8D722),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Exclusive for Karnataka State Police Department personnel.\nPlease uninstall if you are not a member of KSP.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    color = Color.Red
                )
                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sign in with Google / Register", fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.5f))
                    Text(" or ", modifier = Modifier.padding(horizontal = 12.dp), color = Color.White)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.5f))
                }
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "For Offline use",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Login with email and pin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Icon(imageVector = Icons.Filled.Visibility, contentDescription = null)
                        }
                    }
                }
                
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "Developed By Ravikumar J\nAHC, DAR Chikkaballapura",
                    fontSize = 15.sp,
                    color = Color(0xFFF8D722),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
