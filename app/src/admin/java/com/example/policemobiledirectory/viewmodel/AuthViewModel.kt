package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.data.mapper.toEmployee
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.screens.GoogleSignInUiEvent
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
    val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : ViewModel() {

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

    private val _googleAccountPickerLoading = MutableStateFlow(false)
    val googleAccountPickerLoading: StateFlow<Boolean> = _googleAccountPickerLoading.asStateFlow()

    fun setGoogleAccountPickerLoading(loading: Boolean) {
        _googleAccountPickerLoading.value = loading
    }

    // Google Drive Permission State
    private val _hasGoogleDriveAccess = MutableStateFlow(false)
    val hasGoogleDriveAccess: StateFlow<Boolean> = _hasGoogleDriveAccess.asStateFlow()

    fun checkGoogleDriveAccess(context: android.content.Context) {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            _hasGoogleDriveAccess.value = false
            return
        }
        val hasAppData = com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(
            account, com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata")
        )
        val hasDriveFile = com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(
            account, com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")
        )
        val hasSpreadsheets = com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(
            account, com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/spreadsheets")
        )
        
        _hasGoogleDriveAccess.value = hasAppData && hasDriveFile && hasSpreadsheets
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

    init {
        Log.d("AuthViewModel", "🟢 AuthViewModel initialized")
        val options = com.google.firebase.FirebaseApp.getInstance().options
        Log.d("LoginFlow", "🚀 PROJECT ID: ${options.projectId}")
        
        val firebaseUser = auth.currentUser
        Log.d("LoginFlow", "👤 CURRENT USER ID: ${firebaseUser?.uid}")
        Log.d("LoginFlow", "📧 CURRENT EMAIL: ${firebaseUser?.email}")
        
        loadSession()

        // 1️⃣ Observe login state from DataStore
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                Log.d("Session", "🔄 isLoggedIn = $loggedIn")
            }
        }

        // 2️⃣ Observe admin status from DataStore
        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdminValue ->
                _isAdmin.value = isAdminValue
                Log.d("Session", "🔄 isAdmin = $isAdminValue")
                
                // ✅ CRITICAL: Sync UID if admin to enable Firestore rules
                if (isAdminValue) {
                    viewModelScope.launch {
                        // Use session email if auth email is null (common for PIN/Anonymous logins)
                        val email = auth.currentUser?.email ?: sessionManager.userEmail.first()
                        val uid = auth.currentUser?.uid
                        
                        if (email.isNotBlank() && !uid.isNullOrBlank()) {
                            val kgid = employeeRepo.getEmployeeByEmail(email)?.kgid ?: ""
                            employeeRepo.syncAdminUid(email, uid, kgid)
                            
                            // ✅ Pre-fetch pending registrations after sync
                            val result = pendingRepo.fetchPendingFromFirestore()
                            if (result is RepoResult.Success) {
                                pendingRepo.saveAllToLocal(result.data ?: emptyList())
                            }
                        }
                    }
                }
            }
        }

        // 3️⃣ Ensure signed in if needed
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
            try {
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                if (authResult.user != null) {
                    val existingUser = employeeRepo.getEmployeeByEmail(email)
                    if (existingUser != null) {
                        val user = existingUser.toEmployee()
                        if (!user.isApproved) {
                            _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Your app access has been disabled. Please contact an admin.")
                            auth.signOut()
                            return@launch
                        }
                        sessionManager.saveLogin(user.email, user.isAdmin)
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.SignInSuccess(user)
                    } else {
                        // User not found in employees -> Check Pending/Rejected
                        checkPendingRegistration(email, authResult.user?.displayName)
                    }
                } else {
                    _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Sign-in failed: Firebase user is null.")
                }
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "❌ Failed", e)
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Error(e.localizedMessage ?: "Unknown error")
                logout()
            }
        }
    }

    private suspend fun checkPendingRegistration(email: String, name: String?) {
        try {
            val pending = pendingRepo.getPendingByEmail(email)
            if (pending != null) {
                if (pending.status.lowercase() == "rejected") {
                    _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationRejected(
                        email,
                        pending.rejectionReason ?: "No reason provided"
                    )
                } else {
                    _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationPending(email)
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
    // SESSION MANAGEMENT
    // =========================================================

    fun loadSession() {
        viewModelScope.launch {
            val isLoggedIn = sessionManager.isLoggedIn.first()
            _isLoggedIn.value = isLoggedIn

            if (isLoggedIn) {
                val email = sessionManager.userEmail.first()
                val persistedAdmin = sessionManager.isAdmin.first()
                val firebaseUser = auth.currentUser

                // ✅ CRITICAL: If we have a local session but Firebase Auth is NULL, force logout
                if (firebaseUser == null) {
                    Log.e("Session", "❌ SESSION MISMATCH: Local session exists but Firebase Auth is NULL. Forcing logout.")
                    logout()
                    return@launch
                }
                
                _isAdmin.value = persistedAdmin

                if (email.isNotBlank()) {
                    try {
                        // 1. Load from Local Room DB
                        val userEntity = employeeRepo.getEmployeeByEmail(email)
                        val user = userEntity?.toEmployee()

                        if (user != null) {
                            if (!user.isApproved) {
                                Log.w("Session", "⚠️ User access disabled during load. Forcing logout.")
                                logout()
                                return@launch
                            }
                            _currentUser.value = user
                            _isAdmin.value = user.isAdmin || persistedAdmin
                            Log.d("Session", "✅ Session restored for user: ${user.name}, admin=${_isAdmin.value}")

                            // 🔴 2. Background live Firestore check & Admin Sync
                            viewModelScope.launch {
                                // Live check for account approval
                                val liveApproved = employeeRepo.checkIsApprovedFromFirestore(email)
                                if (liveApproved == false) {
                                    Log.w("Session", "🔴 Account DISABLED in Firestore. Logging out.")
                                    logout()
                                    return@launch
                                }

                                // Live check and sync for Admin status
                                val liveAdmin = employeeRepo.isAdminInFirestore(email)
                                val finalAdminState = liveAdmin || user.isAdmin
                                
                                if (finalAdminState) {
                                    Log.d("Session", "🛡️ Admin status confirmed. Syncing UID handshake...")
                                    _isAdmin.value = true
                                    
                                    // Persist if it was a promotion or confirmation
                                    if (liveAdmin && !persistedAdmin) {
                                        sessionManager.saveLogin(email, true)
                                    }
                                    
                                    // satisfy Firestore security rules
                                    auth.currentUser?.uid?.let { uid ->
                                        val kgid = userEntity?.kgid ?: ""
                                        employeeRepo.syncAdminUid(email, uid, kgid)
                                        // ✅ Pre-fetch pending registrations after sync
                                        val result = pendingRepo.fetchPendingFromFirestore()
                                        if (result is RepoResult.Success) {
                                            pendingRepo.saveAllToLocal(result.data ?: emptyList())
                                        }
                                    }
                                } else {
                                    // If live check says NOT admin but we thought we were, demote locally
                                    if (persistedAdmin) {
                                        Log.w("Session", "🚫 Admin status revoked in Firestore. Updating local session.")
                                        _isAdmin.value = false
                                        sessionManager.saveLogin(email, false)
                                    }
                                }
                            }
                        } else {
                            // 3. Fallback to Remote fetch if Room is empty
                            Log.d("Session", "ℹ️ Room empty for $email, fetching from remote...")
                            when (val remoteResult = employeeRepo.getUserByEmail(email)) {
                                is RepoResult.Success -> {
                                    remoteResult.data?.let { remoteUser ->
                                        if (!remoteUser.isApproved) {
                                            Log.w("Session", "🚫 User is not approved. Logging out.")
                                            logout()
                                            return@let
                                        }
                                        _currentUser.value = remoteUser
                                        _isAdmin.value = remoteUser.isAdmin || persistedAdmin
                                        Log.d("Session", "✅ Remote user loaded: ${remoteUser.name}")
                                    }
                                }
                                is RepoResult.Error -> {
                                    // ✅ FIX: Don't logout on error if we might be an admin!
                                    Log.w("Session", "⚠️ Failed to fetch remote profile: ${remoteResult.message}. Continuing session.")
                                    // If we are an admin, we can survive without a profile
                                    if (persistedAdmin) {
                                        _isAdmin.value = true
                                    } else {
                                        Log.w("Session", "Lenient mode: Staying logged in despite profile error.")
                                    }
                                }
                                else -> {
                                    // Loading or other states - do nothing
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Session", "❌ Error during session restore: ${e.message}")
                        logout()
                    }
                } else {
                    logout()
                }
            } else {
                _isAdmin.value = false
                _currentUser.value = null
                Log.d("Session", "ℹ️ Guest Mode (No session)")
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
}


