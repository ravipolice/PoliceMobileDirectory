package com.example.policemobiledirectory.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.policemobiledirectory.ui.screens.*
import com.example.policemobiledirectory.viewmodel.*
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onSwitchGoogleAccountClicked: () -> Unit = {},
    onLogout: () -> Unit,
    onDrivePermissionRequest: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBars = currentRoute?.let { route ->
        route.startsWith(Routes.SPLASH) ||
        route.startsWith(Routes.LOGIN) ||
        route.startsWith(Routes.REGISTER) ||
        route.startsWith(Routes.USER_REGISTRATION) ||
        route.startsWith(Routes.FORGOT_PIN) ||
        route.startsWith(Routes.PENDING_APPROVAL)
    } ?: true

    if (!hideBars) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawer(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    viewModel = authViewModel,
                    settingsViewModel = settingsViewModel,
                    onLogout = onLogout,
                    onDrivePermissionRequest = onDrivePermissionRequest
                )
            }
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        drawerState = drawerState, scope = scope
                    )
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                AppNavHostContent(
                    navController = navController,
                    authViewModel = authViewModel,
                    settingsViewModel = settingsViewModel,
                    onThemeToggle = onThemeToggle,
                    onGoogleSignInClicked = onGoogleSignInClicked,
                    onSwitchGoogleAccountClicked = onSwitchGoogleAccountClicked,
                    startDestination = startDestination,
                    onLogout = onLogout,
                    onDrivePermissionRequest = onDrivePermissionRequest,
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding)
                )
            }
        }
    } else {
        AppNavHostContent(
            navController = navController,
            authViewModel = authViewModel,
            settingsViewModel = settingsViewModel,
            onThemeToggle = onThemeToggle,
            onGoogleSignInClicked = onGoogleSignInClicked,
            onSwitchGoogleAccountClicked = onSwitchGoogleAccountClicked,
            startDestination = startDestination,
            onLogout = onLogout,
            onDrivePermissionRequest = onDrivePermissionRequest
        )
    }
}

@Composable
private fun AppNavHostContent(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onThemeToggle: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onSwitchGoogleAccountClicked: () -> Unit,
    startDestination: String,
    onLogout: () -> Unit,
    onDrivePermissionRequest: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val isAdmin by authViewModel.isAdmin.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        // --- SPLASH ---
        composable(Routes.SPLASH) {
            SplashVideoScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        // --- LOGIN ---
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                settingsViewModel = settingsViewModel,
                onLoginSuccess = { isAdmin ->
                    val user = authViewModel.currentUser.value
                    if (user != null && !user.isApproved && !isAdmin) {
                        navController.navigate(Routes.PENDING_APPROVAL) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.EMPLOYEE_LIST) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onRegisterNewUser = { email, name ->
                    val encodedName = if (name != null) Uri.encode(name) else ""
                    navController.navigate("${Routes.USER_REGISTRATION}?email=$email&name=$encodedName")
                },
                onForgotPinClicked = {
                    navController.navigate(Routes.FORGOT_PIN)
                },
                onGoogleSignInClicked = onGoogleSignInClicked,
                onSwitchGoogleAccountClicked = onSwitchGoogleAccountClicked,
                onThemeToggle = onThemeToggle,
                onLogout = onLogout
            )
        }

        // --- PENDING APPROVAL ---
        composable(Routes.PENDING_APPROVAL) {
            PendingApprovalScreen(
                viewModel = authViewModel,
                onLogout = onLogout
            )
        }

        // --- USER REGISTRATION ---
        composable(
            route = "${Routes.USER_REGISTRATION}?email={email}&name={name}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val nameArg = backStackEntry.arguments?.getString("name") ?: ""
            val initialName = if (nameArg.isNotEmpty()) Uri.decode(nameArg) else ""
            UserRegistrationScreen(
                navController = navController,
                viewModel = authViewModel,
                initialEmail = email,
                initialName = initialName
            )
        }

        // --- DOCUMENTS ---
        composable(Routes.DOCUMENTS) {
            val viewModel: UserDocumentsViewModel = hiltViewModel()
            DocumentsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        // --- FORGOT PIN ---
        composable(Routes.FORGOT_PIN) {
            ForgotPinScreen(
                viewModel = authViewModel,
                onPinResetSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.FORGOT_PIN) { inclusive = true }
                    }
                }
            )
        }

        // --- EMPLOYEE STATS ---
        composable(Routes.EMPLOYEE_STATS) {
            EmployeeStatsScreen(navController = navController, viewModel = hiltViewModel())
        }

        // --- EMPLOYEE LIST (HOME) ---
        composable(Routes.EMPLOYEE_LIST) {
            val employeeListViewModel: EmployeeListViewModel = hiltViewModel()
            EmployeeListScreen(
                navController = navController,
                viewModel = employeeListViewModel,
                authViewModel = authViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        // --- DIRECTORY SEARCH ---
        composable(Routes.DIRECTORY_SEARCH) {
            val employeeListViewModel: EmployeeListViewModel = hiltViewModel()
            DirectorySearchScreen(
                navController = navController,
                viewModel = employeeListViewModel,
                authViewModel = authViewModel,
                settingsViewModel = settingsViewModel
            )
        }
        
        // --- EMPLOYEE DETAIL ---
        composable(
            route = Routes.EMPLOYEE_DETAIL,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("isOfficer") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val isOfficer = backStackEntry.arguments?.getBoolean("isOfficer") ?: false
            val employeeListViewModel: EmployeeListViewModel = hiltViewModel()
            EmployeeDetailScreen(
                id = id,
                isOfficer = isOfficer,
                navController = navController,
                viewModel = employeeListViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        // --- ABOUT ---
        composable(Routes.ABOUT) {
            AboutScreen(navController = navController)
        }

        // --- MY PROFILE ---
        composable(Routes.MY_PROFILE) {
            MyProfileEditScreen(navController = navController)
        }

        // --- NOTIFICATIONS ---
        composable(Routes.NOTIFICATIONS) {
            val notificationsViewModel: NotificationsViewModel = hiltViewModel()
            NotificationsScreen(navController = navController, viewModel = notificationsViewModel)
        }

        // --- Gallery Screen ---
        composable(Routes.GALLERY_SCREEN) {
            GalleryScreen(
                navController = navController
            )
        }

        // --- Terms & Conditions ---
        composable(Routes.TERMS_AND_CONDITIONS) {
            TermsAndConditionsScreen(navController = navController)
        }

        // --- USEFUL LINKS ---
        composable(Routes.USEFUL_LINKS) {
            val usefulLinksViewModel: UsefulLinksViewModel = hiltViewModel()
            UsefulLinksScreen(navController = navController, viewModel = usefulLinksViewModel)
        }

        // --- NUDI CONVERTER ---
        composable(Routes.NUDI_CONVERTER) {
            NudiConverterScreen(navController = navController)
        }

        // --- DUTY REGISTER (Coming Soon) ---
        composable(Routes.DUTY_REGISTER) {
            ComingSoonScreen(
                navController = navController,
                title = "Duty Register",
                icon = Icons.Default.Schedule,
                description = "Manage and track duty schedules, shifts, and assignments. This feature is under development."
            )
        }

        // --- LEAVE MANAGER ---
        composable(Routes.LEAVE_DASHBOARD) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            LeaveDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=CL") },
                onNavigateToEl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=EL") },
                onNavigateToHpl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=HPL") },
                onNavigateToWo = { navController.navigate("${Routes.LEAVE_ENTRY}?type=WO") },
                onNavigateToCcl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=CCL") },
                onNavigateToMcl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=MCL") },
                onNavigateToOther = { navController.navigate("${Routes.LEAVE_ENTRY}?type=ML") },
                onNavigateToApplyLeave = { navController.navigate(Routes.APPLY_LEAVE) },
                onNavigateToReports = { navController.navigate(Routes.LEAVE_REPORTS) },
                onNavigateToRules = { navController.navigate(Routes.LEAVE_RULES) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel,
                onGoogleSignInClicked = onGoogleSignInClicked,
                onDrivePermissionRequest = onDrivePermissionRequest
            )
        }

        composable(Routes.LEAVE_CL) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            CLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_EL) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            ELLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_HPL) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            HPLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_WO) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            WeeklyOffScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_CCL) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            CCLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_MCL) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            MCLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        // --- LEAVE OTHER ---
        composable(Routes.LEAVE_OTHER) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            OtherLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(
            route = "${Routes.LEAVE_ENTRY}?type={type}",
            arguments = listOf(navArgument("type") {
                defaultValue = "CL"
            })
        ) { backStackEntry ->
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            val type = backStackEntry.arguments?.getString("type") ?: "CL"
            LeaveEntryScreen(
                onNavigateBack = { navController.popBackStack() },
                preselectedType = type,
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.APPLY_LEAVE) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            ApplyLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(
            route = Routes.LEAVE_EDIT,
            arguments = listOf(navArgument("entryId") { defaultValue = "" })
        ) { backStackEntry ->
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            LeaveEditScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_REPORTS) {
            val leaveViewModel: LeaveViewModel = hiltViewModel()
            LeaveReportsScreen(
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_RULES) {
            LeaveRulesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("payslip_upload") {
            PayslipUploadScreen(
                onNavigateBack = { navController.popBackStack() },
                onDrivePermissionRequest = onDrivePermissionRequest,
                authViewModel = authViewModel
            )
        }
    }
}
