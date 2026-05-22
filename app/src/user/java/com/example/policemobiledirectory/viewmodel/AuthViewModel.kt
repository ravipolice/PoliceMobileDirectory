package com.example.policemobiledirectory.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.data.mapper.toEmployee
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.screens.GoogleSignInUiEvent
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PinHasher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel responsible for authentication operations:
 * - Login/Logout
 * - Google Sign-In
 * - OTP/PIN management
 * - Session management
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val pendingRepo: PendingRegistrationRepository,
    private val imageRepo: ImageRepository,
    val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // Authentication State
    private val _currentUser = MutableStateFlow<Employee?>(null)
    val currentUser: StateFlow<Employee?> = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authStatus = MutableStateFlow<OperationStatus<Employee>>(OperationStatus.Idle)
    val authStatus: StateFlow<OperationStatus<Employee>> = _authStatus.asStateFlow()

    private val _googleSignInUiEvent = MutableStateFlow<GoogleSignInUiEvent>(GoogleSignInUiEvent.Idle)
    val googleSignInUiEvent: StateFlow<GoogleSignInUiEvent> = _googleSignInUiEvent.asStateFlow()

    private val _isGoogleAccountPickerLoading = MutableStateFlow(false)
    val isGoogleAccountPickerLoading: StateFlow<Boolean> = _isGoogleAccountPickerLoading.asStateFlow()

    fun setGoogleAccountPickerLoading(loading: Boolean) {
        _isGoogleAccountPickerLoading.value = loading
    }

    // Google Drive Permission State
    private val _hasAppDataAccess = MutableStateFlow(false)
    val hasAppDataAccess: StateFlow<Boolean> = _hasAppDataAccess.asStateFlow()

    private val _hasDriveFileAccess = MutableStateFlow(false)
    val hasDriveFileAccess: StateFlow<Boolean> = _hasDriveFileAccess.asStateFlow()

    private val _hasSpreadsheetsAccess = MutableStateFlow(false)
    val hasSpreadsheetsAccess: StateFlow<Boolean> = _hasSpreadsheetsAccess.asStateFlow()

    val driveAccountEmail: StateFlow<String?> = sessionManager.driveAccountEmail
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Combined status for Navigation Drawer display
    val hasFullDriveAccess = combine(
        _hasAppDataAccess, _hasDriveFileAccess, _hasSpreadsheetsAccess
    ) { appData, driveFile, sheets -> appData && driveFile && sheets }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun checkGoogleDriveAccess(context: android.content.Context) {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            _hasAppDataAccess.value = false
            _hasDriveFileAccess.value = false
            _hasSpreadsheetsAccess.value = false
            return
        }
        
        _hasAppDataAccess.value = com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(
            account, com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata")
        )
        _hasDriveFileAccess.value = com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(
            account, com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")
        )
        _hasSpreadsheetsAccess.value = com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(
            account, com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/spreadsheets")
        )
        
        Log.d("Auth", "Drive Permissions: AppData=${_hasAppDataAccess.value}, File=${_hasDriveFileAccess.value}, Sheets=${_hasSpreadsheetsAccess.value}")
    }

    fun saveDriveAccountEmail(email: String) {
        viewModelScope.launch {
            sessionManager.saveDriveAccountEmail(email)
        }
    }

    // OTP/PIN State
    private val _otpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val otpUiState: StateFlow<OperationStatus<String>> = _otpUiState

    private val _verifyOtpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val verifyOtpUiState: StateFlow<OperationStatus<String>> = _verifyOtpUiState

    private val _pinResetUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinResetUiState: StateFlow<OperationStatus<String>> = _pinResetUiState

    private val _pinChangeState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinChangeState: StateFlow<OperationStatus<String>> = _pinChangeState.asStateFlow()

    private var otpSentTime: Long? = null
    private val otpValidityDuration = 5 * 60 * 1000L
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime

    // Registration State
    private val _pendingStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pendingStatus: StateFlow<OperationStatus<String>> = _pendingStatus.asStateFlow()

    init {
        Log.d("AuthViewModel", "🟢 AuthViewModel initialized")
        loadSession()

        // Observe login state from DataStore
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                Log.d("Session", "🔄 isLoggedIn = $loggedIn")
            }
        }

        // Observe admin flag from DataStore
        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdmin ->
                _isAdmin.value = isAdmin
                Log.d("Session", "🔄 isAdmin = $isAdmin")
            }
        }

        // Restore current user session from Room or Firestore (Robust Combine Flow)
        viewModelScope.launch {
            combine(
                sessionManager.isLoggedIn,
                sessionManager.isAdmin,
                sessionManager.userEmail
            ) { loggedIn, admin, email ->
                Triple(loggedIn, admin, email)
            }.collect { (loggedIn, admin, email) ->
                Log.d("Session", "🔄 Auth Session Sync: isLoggedIn=$loggedIn, isAdmin=$admin, email=$email")

                if (!loggedIn || email.isBlank()) {
                    if (_isLoggedIn.value) {
                        Log.d("Session", "🔒 Clearing session in memory")
                        _isLoggedIn.value = false
                        _isAdmin.value = false
                        _currentUser.value = null
                    }
                    return@collect
                }

                // Restore profile if needed
                if (_currentUser.value?.email != email) {
                    Log.d("Session", "🔁 Restoring profile for $email")
                    val localUser = employeeRepo.getEmployeeByEmail(email)
                    if (localUser != null) {
                        _currentUser.value = localUser.toEmployee()
                        _isAdmin.value = localUser.isAdmin
                        _isLoggedIn.value = true
                    } else {
                        // Fallback to Firestore lookup
                        when (val remoteResult = employeeRepo.getUserByEmail(email)) {
                            is RepoResult.Success -> {
                                remoteResult.data?.let { user ->
                                    _currentUser.value = user
                                    _isAdmin.value = user.isAdmin
                                    _isLoggedIn.value = true
                                }
                            }
                            else -> Unit
                        }
                    }
                } else {
                    // Just sync flags if email already matches
                    _isAdmin.value = admin
                    _isLoggedIn.value = true
                }
            }
        }

        // Ensure signed in if needed
        viewModelScope.launch {
            try {
                ensureSignedInIfNeeded()
            } catch (e: Exception) {
                Log.e("Startup", "Startup failed: ${e.message}", e)
            }
        }
    }

    // =========================================================
    // AUTHENTICATION METHODS
    // =========================================================

    fun loginWithPin(email: String, pin: String) {
        viewModelScope.launch {
            employeeRepo.loginUser(email, pin).collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val user = result.data
                        if (user != null) {
                            // Instantly update UI before waiting for DataStore
                            _currentUser.value = user
                            _isAdmin.value = user.isAdmin
                            _isLoggedIn.value = true

                            // Save to DataStore for persistence
                            sessionManager.saveLogin(email, user.isAdmin)

                            // Fetch a fresh version from local DB
                            val refreshed = employeeRepo.getEmployeeDirect(email)
                            if (refreshed != null) {
                                _currentUser.value = refreshed
                                _isAdmin.value = refreshed.isAdmin
                            }

                            _authStatus.value = OperationStatus.Success(user)
                            Log.d("Login", "✅ Logged in as ${user.name}, Admin=${user.isAdmin}")
                        } else {
                            _authStatus.value = OperationStatus.Error("User not found")
                        }
                    }
                    is RepoResult.Error -> {
                        _authStatus.value = OperationStatus.Error(result.message ?: "Login failed")
                    }
                    is RepoResult.Loading -> {
                        _authStatus.value = OperationStatus.Loading
                    }
                }
            }
        }
    }

    fun handleGoogleSignIn(email: String, googleIdToken: String) {
        viewModelScope.launch {
            _googleSignInUiEvent.value = GoogleSignInUiEvent.Loading
            Log.d("LoginFlow", "🚀 STARTING Google Sign-In for: '$email'")
            try {
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    Log.d("LoginFlow", "✅ Firebase Auth Success: UID=${firebaseUser.uid}, Email=${firebaseUser.email}")
                    
                    // Check if user exists in our database
                    Log.d("LoginFlow", "📡 Querying database for: '$email'...")
                    when (val result = employeeRepo.getUserByEmail(email)) {
                        is RepoResult.Success -> {
                            val user = result.data
                            if (user != null) {
                                Log.d("LoginFlow", "✅ User found in database: ${user.kgid}, Approved=${user.isApproved}, Admin=${user.isAdmin}")
                                // User exists -> Login
                                if (!user.isApproved) {
                                    Log.w("LoginFlow", "❌ User access is DISABLED")
                                    _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Your app access has been disabled. Please contact an admin.")
                                    auth.signOut()
                                    return@launch
                                }
                                Log.d("LoginFlow", "🎯 Login Successful!")
                                sessionManager.saveLogin(user.email, user.isAdmin)
                                _currentUser.value = user
                                _isLoggedIn.value = true
                                _googleSignInUiEvent.value = GoogleSignInUiEvent.SignInSuccess(user)
                            } else {
                                Log.w("LoginFlow", "❓ User NOT found in database. Checking pending...")
                                // User fetch success but null -> Check Pending/Rejected
                                checkPendingRegistration(email, firebaseUser.displayName)
                            }
                        }
                        is RepoResult.Error -> {
                            Log.e("LoginFlow", "❌ User lookup FAILED: ${result.message}")
                            checkPendingRegistration(email, firebaseUser.displayName)
                        }
                        else -> {
                            Log.e("LoginFlow", "❌ Unknown repository state")
                            _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Unknown state during user lookup")
                        }
                    }
                } else {
                    Log.e("LoginFlow", "❌ Firebase user is NULL after success")
                    _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Sign-in failed: Firebase user is null.")
                }
            } catch (e: Exception) {
                Log.e("LoginFlow", "❌ Exception during Google Sign-In", e)
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Error(e.localizedMessage ?: "Unknown error")
                logout()
            }
        }
    }

    private suspend fun checkPendingRegistration(email: String, name: String?) {
        try {
            val pending = pendingRepo.getPendingByEmail(email)
            if (pending != null) {
                when (pending.status.lowercase()) {
                    "rejected" -> {
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationRejected(
                            email,
                            pending.rejectionReason ?: "No reason provided"
                        )
                    }
                    else -> {
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationPending(email)
                    }
                }
            } else {
                _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationRequired(email, name)
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "❌ Pending check failed", e)
            _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationRequired(email, name)
        }
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                Log.d("Logout", "🚪 Starting logout...")

                // Clear local session FIRST
                sessionManager.clearSession()

                // Reset in-memory session IMMEDIATELY
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null

                // Reset auth/UI state
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle

                // Sign out of Firebase
                FirebaseAuth.getInstance().signOut()
                auth.signOut()

                // Clear repository data
                employeeRepo.logout()

                Log.d("Logout", "✅ Logout complete")

                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e("Logout", "❌ Logout failed: ${e.message}")
                // Even if there's an error, ensure state is cleared
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    // =========================================================
    // OTP / PIN FLOW
    // =========================================================

    fun sendOtp(email: String) {
        viewModelScope.launch {
            Log.d("ForgotPinFlow", "🟢 sendOtp() for $email")
            _otpUiState.value = OperationStatus.Loading

            try {
                when (val result = employeeRepo.sendOtp(email)) {
                    is RepoResult.Success -> {
                        _otpUiState.value = OperationStatus.Success(result.data ?: "OTP sent to $email")
                        startOtpCountdown()
                    }
                    is RepoResult.Error -> {
                        _otpUiState.value = OperationStatus.Error(result.message ?: "Failed to send OTP")
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                _otpUiState.value = OperationStatus.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }

    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _verifyOtpUiState.value = OperationStatus.Loading
            try {
                when (val result = employeeRepo.verifyLoginCode(email, code)) {
                    is RepoResult.Success -> _verifyOtpUiState.value = OperationStatus.Success("OTP verified successfully")
                    is RepoResult.Error -> _verifyOtpUiState.value = OperationStatus.Error(result.message ?: "Invalid OTP")
                    else -> Unit
                }
            } catch (e: Exception) {
                _verifyOtpUiState.value = OperationStatus.Error(e.message ?: "Error verifying OTP")
            }
        }
    }

    fun updatePinAfterOtp(email: String, newPin: String) {
        viewModelScope.launch {
            _pinResetUiState.value = OperationStatus.Loading
            try {
                val result = employeeRepo.updateUserPin(email, null, newPin, true)
                when (result) {
                    is RepoResult.Success -> _pinResetUiState.value = OperationStatus.Success("PIN reset successful")
                    is RepoResult.Error -> _pinResetUiState.value = OperationStatus.Error(result.message ?: "Failed to reset PIN")
                    else -> Unit
                }
            } catch (e: Exception) {
                _pinResetUiState.value = OperationStatus.Error(e.message ?: "Error updating PIN")
            }
        }
    }

    fun changePin(email: String, oldPin: String, newPin: String) {
        viewModelScope.launch {
            _pinChangeState.value = OperationStatus.Loading
            when (val result = employeeRepo.updateUserPin(email, oldPin, newPin, false)) {
                is RepoResult.Success -> _pinChangeState.value = OperationStatus.Success("PIN changed successfully")
                is RepoResult.Error -> _pinChangeState.value = OperationStatus.Error(result.message ?: "Failed to change PIN")
                else -> Unit
            }
        }
    }

    private fun startOtpCountdown() {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            otpSentTime = start
            while (System.currentTimeMillis() - start < otpValidityDuration) {
                _remainingTime.value = otpValidityDuration - (System.currentTimeMillis() - start)
                delay(1000)
            }
            _remainingTime.value = 0L
            resetForgotPinFlow()
        }
    }

    fun resetForgotPinFlow() {
        _otpUiState.value = OperationStatus.Idle
        _verifyOtpUiState.value = OperationStatus.Idle
        _pinResetUiState.value = OperationStatus.Idle
    }

    fun resetPinChangeState() {
        _pinChangeState.value = OperationStatus.Idle
    }

    fun setPinResetError(message: String) {
        _pinResetUiState.value = OperationStatus.Error(message)
    }

    // =========================================================
    // REGISTRATION METHODS
    // =========================================================

    fun registerNewUser(entity: PendingRegistrationEntity) {
        // Prevent duplicate submissions
        if (_pendingStatus.value is OperationStatus.Loading) return
        _pendingStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                // 1️⃣ Check active employees first (User has read access!)
                val activeDuplicateReason = try {
                    val activeKgidCount = firestore.collection("employees")
                        .whereEqualTo("kgid", entity.kgid)
                        .limit(1)
                        .get()
                        .await()
                        .size()
                    if (activeKgidCount > 0) "User with this KGID is already registered and active. Please login."
                    else {
                        val activeEmailCount = firestore.collection("employees")
                            .whereEqualTo("email", entity.email)
                            .limit(1)
                            .get()
                            .await()
                            .size()
                        if (activeEmailCount > 0) "User with this Email is already registered and active. Please login." else null
                    }
                } catch (e: Exception) {
                    Log.w("RegisterUser", "Active employee check failed: ${e.message}")
                    null
                }

                if (activeDuplicateReason != null) {
                    _pendingStatus.value = OperationStatus.Error(activeDuplicateReason)
                    return@launch
                }

                // 2️⃣ ⭐ UPDATED: PREVENT REGISTRATION WITH DEPARTMENTAL NUMBERS
                // Rule: If the number exists in 'officers' OR 'officers_v2', it's departmental.
                if (entity.email.lowercase().trim() != "ravipolice@gmail.com") {
                    val mobileToMatch = entity.mobile1.trim()
                    if (mobileToMatch.isNotBlank()) {
                        try {
                            Log.d("RegisterUser", "🔍 Checking '$mobileToMatch' against 'officers'...")
                            
                            // Check production officers
                            val officerMatch = firestore.collection("officers")
                                .whereEqualTo("mobile", mobileToMatch)
                                .limit(1).get().await()

                            if (!officerMatch.isEmpty) {
                                Log.w("RegisterUser", "❌ Blocked departmental number: $mobileToMatch")
                                _pendingStatus.value = OperationStatus.Error(
                                    "Registration Blocked: The mobile number ($mobileToMatch) is an official Departmental number. " +
                                    "Please register using your personal mobile number."
                                )
                                return@launch
                            }
                            Log.d("RegisterUser", "✅ Mobile number cleared (not departmental)")
                        } catch (e: Exception) {
                            Log.e("RegisterUser", "⚠️ Departmental check error: ${e.message}")
                        }
                    }
                }

                // 2️⃣ Prepare safe PendingRegistration object
                var finalPin = entity.pin
                if (finalPin.length == 6 && !finalPin.contains(":")) {
                    finalPin = PinHasher.hashPassword(finalPin)
                }

                val pending = entity.copy(
                    pin = finalPin,
                    isApproved = false,
                    firebaseUid = entity.firebaseUid.takeIf { it.isNotBlank() } ?: "",
                    status = "pending",
                    rejectionReason = null,
                    photoUrlFromGoogle = null
                )

                // 3️⃣ Submit to Firestore + Room
                pendingRepo.addPendingRegistration(pending).collect { result ->
                    when (result) {
                        is RepoResult.Loading ->
                            _pendingStatus.value = OperationStatus.Loading

                        is RepoResult.Success -> {
                            _pendingStatus.value =
                                OperationStatus.Success("Registration submitted for admin approval.")
                        }

                        is RepoResult.Error -> {
                            val msg = result.message ?: "Registration failed."
                            val finalMsg = if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("permission", ignoreCase = true)) {
                                "Registration failed: This KGID/Email is already pending approval."
                            } else msg
                            
                            _pendingStatus.value =
                                OperationStatus.Error(finalMsg)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("RegisterUser", "❌ Registration failed", e)
                _pendingStatus.value =
                    OperationStatus.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }

    fun resetPendingStatus() {
        _pendingStatus.value = OperationStatus.Idle
    }

    /**
     * Upload photo for registration
     */
    suspend fun uploadOfficerImageSuspend(uri: Uri, kgid: String): RepoResult<String> {
        return try {
            val result = imageRepo.uploadOfficerImage(uri, kgid)
                .filter { it is OperationStatus.Success || it is OperationStatus.Error }
                .first()
                
            when (result) {
                is OperationStatus.Success -> RepoResult.Success(result.data)
                is OperationStatus.Error -> RepoResult.Error(null, result.message)
                else -> RepoResult.Error(null, "Upload failed")
            }
        } catch (e: Exception) {
            RepoResult.Error(e)
        }
    }

    // =========================================================
    // SESSION MANAGEMENT
    // =========================================================

    fun loadSession() {
        viewModelScope.launch {
            val isLoggedIn = sessionManager.isLoggedIn.first()
            _isLoggedIn.value = isLoggedIn

            if (isLoggedIn) {
                val email = sessionManager.userEmail.first()
                val isAdmin = sessionManager.isAdmin.first()
                _isAdmin.value = isAdmin

                if (email.isNotBlank()) {
                    try {
                        val userEntity = employeeRepo.getEmployeeByEmail(email)
                        val user = userEntity?.toEmployee()

                        if (user != null) {
                            if (!user.isApproved) {
                                Log.w("Session", "⚠️ User access disabled during load. Forcing logout.")
                                logout()
                                return@launch
                            }
                            _currentUser.value = user
                            Log.d("Session", "✅ Session restored for user: ${user.name}, admin=$isAdmin")

                            // 🔴 Background live Firestore check on startup
                            viewModelScope.launch {
                                val liveApproved = employeeRepo.checkIsApprovedFromFirestore(email)
                                if (liveApproved == false) {
                                    Log.w("Session", "🔴 Firestore says user is DISABLED. Forcing logout.")
                                    logout()
                                }
                            }
                        } else {
                            Log.e("Session", "❌ Session exists for $email but user not found in DB. Forcing logout.")
                            logout()
                        }
                    } catch (e: Exception) {
                        Log.e("Session", "❌ DB error during session restore: ${e.message}. Forcing logout.")
                        logout()
                    }
                } else {
                    Log.e("Session", "❌ Invalid session state. Forcing logout.")
                    logout()
                }
            } else {
                _isAdmin.value = false
                _currentUser.value = null
                Log.d("Session", "ℹ️ No active session. App is in Guest mode.")
            }
        }
    }

    /**
     * Prevents unwanted Firebase guest auto-login
     */
    private suspend fun ensureSignedInIfNeeded() {
        val hasValidSession = sessionManager.isLoggedIn.first()
        if (!hasValidSession) {
            val user = auth.currentUser
            if (user != null) {
                Log.w("AuthCheck", "⚠️ Firebase user exists but no valid session — signing out.")
                try {
                    auth.signOut()
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Log.e("AuthCheck", "❌ Failed to sign out: ${e.message}")
                }
            }
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Log.d("AuthCheck", "🔒 No Firebase user — not signing in automatically.")
            return
        }

        if (user.isAnonymous) {
            Log.d("AuthCheck", "✅ Valid Anonymous Firebase session (PIN Login)")
        } else if (user.email.isNullOrBlank()) {
            Log.w("AuthCheck", "⚠️ Firebase user has no email — checking session consistency.")
        } else {
            Log.d("AuthCheck", "✅ Valid Firebase user: ${user.email}")
        }
    }

    fun checkIfAdmin() {
        viewModelScope.launch {
            try {
                val email = _currentUser.value?.email
                if (email.isNullOrBlank()) {
                    val sessionEmail = sessionManager.userEmail.first()
                    if (sessionEmail.isNotBlank()) {
                        val user = employeeRepo.getEmployeeByEmail(sessionEmail)
                        _isAdmin.value = user?.isAdmin ?: false
                        Log.d("AdminCheck", "✅ Admin status from session: ${user?.isAdmin}")
                    } else {
                        _isAdmin.value = false
                    }
                    return@launch
                }

                val currentUser = _currentUser.value
                if (currentUser != null) {
                    _isAdmin.value = currentUser.isAdmin
                    Log.d("AdminCheck", "✅ Admin status from currentUser: ${currentUser.isAdmin}")
                } else {
                    val user = employeeRepo.getEmployeeByEmail(email)
                    _isAdmin.value = user?.isAdmin ?: false
                    Log.d("AdminCheck", "✅ Admin status from repository: ${user?.isAdmin}")
                }
            } catch (e: Exception) {
                Log.e("AdminCheck", "❌ Error checking admin status: ${e.message}")
            }
        }
    }

    fun checkUserRegistrationDiagnostics(email: String) {
        viewModelScope.launch {
            val normalizedEmail = email.trim().lowercase()
            Log.d("DIAGNOSTIC", "════════════════════════════════════════════════")
            Log.d("DIAGNOSTIC", "🔍 STARTING REGISTRATION DIAGNOSTICS FOR: $normalizedEmail")
            Log.d("DIAGNOSTIC", "════════════════════════════════════════════════")

            try {
                // 1. Check Auth State
                val currentUser = auth.currentUser
                Log.d("DIAGNOSTIC", "👤 Current Auth UID: ${currentUser?.uid}")
                Log.d("DIAGNOSTIC", "👤 Current Auth Email: ${currentUser?.email}")
                Log.d("DIAGNOSTIC", "👤 Is Anonymous: ${currentUser?.isAnonymous}")

                // 2. Check Employees Collection
                Log.d("DIAGNOSTIC", "📡 Checking 'employees' collection...")
                val empQuery = firestore.collection("employees").whereEqualTo("email", normalizedEmail).get().await()
                if (!empQuery.isEmpty) {
                    val doc = empQuery.documents.first()
                    Log.d("DIAGNOSTIC", "✅ FOUND in 'employees' (Field Search)")
                    Log.d("DIAGNOSTIC", "   Doc ID: ${doc.id}")
                    Log.d("DIAGNOSTIC", "   isApproved: ${doc.getBoolean("isApproved")}")
                    Log.d("DIAGNOSTIC", "   firebaseUid: ${doc.getString("firebaseUid")}")
                } else {
                    Log.d("DIAGNOSTIC", "❌ NOT FOUND in 'employees' by field 'email'")
                }

                // 3. Check Pending Registrations
                Log.d("DIAGNOSTIC", "📡 Checking 'pending_registrations' collection...")
                val pendingQuery = firestore.collection("pending_registrations").whereEqualTo("email", normalizedEmail).get().await()
                if (!pendingQuery.isEmpty) {
                    val doc = pendingQuery.documents.first()
                    Log.d("DIAGNOSTIC", "✅ FOUND in 'pending_registrations'")
                    Log.d("DIAGNOSTIC", "   Status: ${doc.getString("status")}")
                    Log.d("DIAGNOSTIC", "   isApproved: ${doc.getBoolean("isApproved")}")
                } else {
                    Log.d("DIAGNOSTIC", "❌ NOT FOUND in 'pending_registrations'")
                }

                // 4. Check Admins Collection
                Log.d("DIAGNOSTIC", "📡 Checking 'admins' collection...")
                // Path A: Email Field
                val adminEmailField = firestore.collection("admins").whereEqualTo("email", normalizedEmail).get().await()
                if (!adminEmailField.isEmpty) {
                    Log.d("DIAGNOSTIC", "✅ FOUND in 'admins' by email field")
                }

                // Path B: Email as ID
                val adminEmailId = firestore.collection("admins").document(normalizedEmail).get().await()
                if (adminEmailId.exists()) {
                    Log.d("DIAGNOSTIC", "✅ FOUND in 'admins' by Document ID (Email)")
                }

                // Path C: UID as ID
                if (currentUser != null) {
                    val adminUidId = firestore.collection("admins").document(currentUser.uid).get().await()
                    if (adminUidId.exists()) {
                        Log.d("DIAGNOSTIC", "✅ FOUND in 'admins' by Document ID (UID: ${currentUser.uid})")
                    }
                }

                Log.d("DIAGNOSTIC", "════════════════════════════════════════════════")
                Log.d("DIAGNOSTIC", "🏁 DIAGNOSTICS COMPLETE")
                Log.d("DIAGNOSTIC", "════════════════════════════════════════════════")

            } catch (e: Exception) {
                Log.e("DIAGNOSTIC", "❌ DIAGNOSTIC FAILED: ${e.message}", e)
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    Log.e("DIAGNOSTIC", "🚨 CRITICAL: Firestore Rules are BLOCKING the diagnostic read!")
                }
            }
        }
    }
}


