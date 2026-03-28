package com.example.policemobiledirectory

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.policemobiledirectory.navigation.AppNavGraph
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.services.MyFirebaseMessagingService
import com.example.policemobiledirectory.ui.theme.PMDTheme
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import android.content.pm.PackageManager
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import com.example.policemobiledirectory.model.Employee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EmployeeViewModel by viewModels()
    private var wasLoggedOut = false
    private lateinit var googleSignInClient: GoogleSignInClient

    private val legacyGoogleSignInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Always try to extract the account/task result info
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleLegacySignInResult(task)
        }

    // ✅ Permission launcher for notifications (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("Permission", "POST_NOTIFICATIONS granted = $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ TEST LOG - This should ALWAYS appear when app starts
        Log.e("TEST_LOG", "═══════════════════════════════════════")
        Log.e("TEST_LOG", "🚀🚀🚀 MAINACTIVITY ONCREATE CALLED 🚀🚀🚀")
        Log.e("TEST_LOG", "═══════════════════════════════════════")
        android.util.Log.e("TEST_LOG2", "Android Log test - MainActivity started")
        System.out.println("SYSOUT: MainActivity onCreate called")

        // 🚫 1️⃣ Clean up any leftover anonymous Firebase sessions
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null && (currentUser.isAnonymous || currentUser.email.isNullOrBlank())) {
            Log.w("StartupAuth", "⚠️ Clearing anonymous Firebase session on startup")
            auth.signOut()
        }

        // ✅ Initialize Legacy Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(com.example.policemobiledirectory.R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ✅ 2️⃣ Continue normal setup
        setupContent()
        askNotificationPermission()
        observeUserLoginForFCM()
    }

    /**
     * ✅ Launch Google Sign-In (only called if user selects Google login)
     */
    /**
     * ✅ Launch Google Sign-In (only called if user selects Google login)
     */
    private suspend fun launchGoogleSignIn() {
        val clientId = getString(com.example.policemobiledirectory.R.string.default_web_client_id)
        Log.d("Auth", "🚀 Launching Google Sign-In with Client ID: $clientId")
        viewModel.setGoogleAccountPickerLoading(true)

        val credentialManager = CredentialManager.create(this)
        
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d("Auth", "Requesting credential with 8s timeout...")

            // ✅ Wrap in timeout to prevent indefinite hangs
            val result: GetCredentialResponse? = kotlinx.coroutines.withTimeoutOrNull(8000) {
                credentialManager.getCredential(this@MainActivity, request)
            }
            
            if (result == null) {
                Log.e("Auth", "❌ CredentialManager timed out (8s). Falling back to Legacy.")
                launchLegacyGoogleSignIn()
                return
            }

            val credential = result.credential
            
            val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            val email = googleIdTokenCredential.id
            
            Log.d("Auth", "✅ Google Sign-In success for email: $email")
            
            if (googleIdToken.isNotEmpty()) {
                viewModel.handleGoogleSignIn(email, googleIdToken)
                wasLoggedOut = false
            } else {
                Log.e("Auth", "❌ No ID token found in credential data. Trying legacy.")
                launchLegacyGoogleSignIn()
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d("Auth", "⚠️ Sign-In cancelled by user")
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Log.e("Auth", "❌ No credentials available: ${e.message}")
            Toast.makeText(this, "No Google Accounts Found. Trying fallback...", Toast.LENGTH_LONG).show()
            launchLegacyGoogleSignIn()
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            Log.e("Auth", "❌ Google Sign-In failed: ${e.type} - ${e.message}", e)
            Toast.makeText(this, "Sign-In Error. Trying fallback...", Toast.LENGTH_LONG).show()
            launchLegacyGoogleSignIn()
        } catch (e: Exception) {
            Log.e("Auth", "❌ Google Sign-In unexpected error", e)
            launchLegacyGoogleSignIn()
        } finally {
            viewModel.setGoogleAccountPickerLoading(false)
        }
    }

    private fun launchLegacyGoogleSignIn() {
        Log.d("Auth", "🚀 Launching Legacy Google Sign-In flow")
        val signInIntent = googleSignInClient.signInIntent
        legacyGoogleSignInLauncher.launch(signInIntent)
    }

    private fun handleLegacySignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            val email = account.email
            
            if (idToken != null && email != null) {
                Log.d("Auth", "✅ Legacy Google Sign-In success: $email")
                viewModel.handleGoogleSignIn(email, idToken)
                wasLoggedOut = false
            } else {
                Log.e("Auth", "❌ Legacy Sign-In: ID Token or Email is null")
                Toast.makeText(this, "Legacy Error: Missing account info", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("Auth", "❌ Legacy Sign-In failed with status code: ${e.statusCode}")
            val errorMsg = when (e.statusCode) {
                12501 -> "Sign-In Cancelled"
                10 -> "Configuration Error (Code 10)\nCheck SHA-1/Package Name"
                12500 -> "Configuration Error (Code 12500)"
                7 -> "Network Error (Check Internet)"
                else -> "Sign-In Error (Code: ${e.statusCode})"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        } finally {
            viewModel.setGoogleAccountPickerLoading(false)
        }
    }

    /**
     * ✅ Sets up Compose UI with automatic navigation based on login session.
     */
    private fun setupContent() {
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            PMDTheme(darkTheme = isDarkTheme) {

                // ✅ Always start with splash video
                val startDestination = Routes.SPLASH

                // ✅ Define logout action (manual only)
                val logoutAction: () -> Unit = {
                    scope.launch(Dispatchers.Main) {
                        viewModel.logout {
                            // ✅ Clear Credential Manager state
                            val credentialManager = CredentialManager.create(this@MainActivity)
                            scope.launch(Dispatchers.Main) {
                                try {
                                    credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
                                    Log.d("Auth", "Credential Manager state cleared")
                                } catch (e: Exception) {
                                    Log.e("Auth", "Error clearing credential state", e)
                                }
                            }

                            Toast.makeText(
                                this@MainActivity,
                                "Logged out successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            // ✅ Navigate to login only after clearing session
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // ✅ Observe session changes (only after logout/login actions)
                // Use a key to track logout state and prevent auto-navigation after logout
                var lastLoggedInState by remember { mutableStateOf(isLoggedIn) }
                
                LaunchedEffect(isLoggedIn, viewModel.currentUser.collectAsState().value) {
                    val currentUser = viewModel.currentUser.value
                    
                    // ✅ If user just logged out, don't navigate
                    if (lastLoggedInState && !isLoggedIn) {
                        Log.d("MainActivity", "🔒 User logged out, staying on current screen")
                        lastLoggedInState = false
                        return@LaunchedEffect
                    }
                    
                    // Skip auto-navigation while splash is showing; splash handles routing
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute == Routes.SPLASH) return@LaunchedEffect

                    // 🆕 Handle Notification Action: view_pending_approvals
                    val notificationAction = intent.getStringExtra("notification_action")
                    if (notificationAction == "view_pending_approvals") {
                        intent.removeExtra("notification_action") // Clear once handled
                        Log.d("MainActivity", "🔔 Notification action: view_pending_approvals")
                        navController.navigate(Routes.PENDING_APPROVALS) {
                            popUpTo(Routes.EMPLOYEE_LIST) { saveState = true }
                            launchSingleTop = true
                        }
                        return@LaunchedEffect
                    }

                    if (isLoggedIn && currentUser != null) {
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute == Routes.SPLASH) return@LaunchedEffect

                        // ✅ Calculate target screen
                        val isAdmin = viewModel.isAdmin.value
                        var targetRoute = Routes.EMPLOYEE_LIST
                        
                        if (isAdmin) {
                            // Fetch latest pending count
                            viewModel.refreshPendingRegistrations()
                            delay(500) // Small delay for state to update
                            if (viewModel.pendingApprovalsTotalCount.value > 0) {
                                targetRoute = Routes.PENDING_APPROVALS
                            }
                        }

                        // Only navigate if we're not already on the target or pending screen
                        if (currentRoute != targetRoute && currentRoute != Routes.PENDING_APPROVALS) {
                            Log.d("MainActivity", "🏠 Navigating to $targetRoute")
                            navController.navigate(targetRoute) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                        lastLoggedInState = true
                    } else {
                        Log.d("MainActivity", "🔒 No valid session, staying on login")
                        lastLoggedInState = false
                    }
                }

                // ✅ App Navigation
                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    employeeViewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onGoogleSignInClicked = { scope.launch(Dispatchers.Main) { launchGoogleSignIn() } },
                    onThemeToggle = { viewModel.toggleTheme() },
                    onLogout = logoutAction
                )
            }
        }
    }

    /**
     * ✅ Ask permission for notifications (Android 13+)
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> Log.d("Permission", "Granted")

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * ✅ Sync FCM Token whenever a new user logs in
     */
    private fun observeUserLoginForFCM() {
        var previousUserUid: String? = viewModel.currentUser.value?.firebaseUid
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.currentUser.collectLatest { employeeUser ->
                val currentUserUid = employeeUser?.firebaseUid
                if (currentUserUid != null && currentUserUid != previousUserUid) {
                    MyFirebaseMessagingService.syncPendingToken(this@MainActivity)
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                            return@addOnCompleteListener
                        }
                        val token = task.result
                        MyFirebaseMessagingService.sendRegistrationToServer(token)
                    }
                }
                previousUserUid = currentUserUid
            }
        }
    }
}
