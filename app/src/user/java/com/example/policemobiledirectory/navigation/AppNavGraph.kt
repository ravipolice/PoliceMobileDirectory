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
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Schedule
import com.example.policemobiledirectory.viewmodel.UserDocumentsViewModel
import com.example.policemobiledirectory.viewmodel.NotificationsViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    employeeViewModel: EmployeeViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBars = currentRoute in listOf(
        Routes.SPLASH,
        Routes.LOGIN,
        Routes.USER_REGISTRATION,
        Routes.FORGOT_PIN
    )

    if (!hideBars) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawer(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    viewModel = employeeViewModel,
                    onLogout = onLogout
                )
            }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        drawerState = drawerState, scope = scope
                    )
                }
            ) { innerPadding ->
                AppNavHostContent(
                    navController = navController,
                    employeeViewModel = employeeViewModel,
                    onThemeToggle = onThemeToggle,
                    onGoogleSignInClicked = onGoogleSignInClicked,
                    startDestination = startDestination,
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding)
                )
            }
        }
    } else {
        AppNavHostContent(
            navController = navController,
            employeeViewModel = employeeViewModel,
            onThemeToggle = onThemeToggle,
            onGoogleSignInClicked = onGoogleSignInClicked,
            startDestination = startDestination
        )
    }
}

@Composable
private fun AppNavHostContent(
    navController: NavHostController,
    employeeViewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    startDestination: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val isAdmin by employeeViewModel.isAdmin.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        // --- SPLASH ---
        composable(Routes.SPLASH) {
            SplashVideoScreen(
                navController = navController,
                viewModel = employeeViewModel
            )
        }

        // --- LOGIN ---
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = employeeViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.EMPLOYEE_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
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
                onThemeToggle = onThemeToggle
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
                viewModel = employeeViewModel,
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
                viewModel = employeeViewModel,
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
            EmployeeListScreen(
                navController = navController,
                viewModel = employeeViewModel,
                onThemeToggle = onThemeToggle
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
            UsefulLinksScreen(navController = navController, viewModel = employeeViewModel)
        }

        // --- NUDI CONVERTER (Coming Soon) ---
        composable(Routes.NUDI_CONVERTER) {
            ComingSoonScreen(
                navController = navController,
                title = "Nudi Converter",
                icon = Icons.Default.Translate,
                description = "Convert text between Nudi and Unicode Kannada fonts. This feature is under development."
            )
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
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            LeaveDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=CL") },
                onNavigateToEl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=EL") },
                onNavigateToHpl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=HPL") },
                onNavigateToWo = { navController.navigate("${Routes.LEAVE_ENTRY}?type=WO") },
                onNavigateToCcl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=CCL") },
                onNavigateToMcl = { navController.navigate("${Routes.LEAVE_ENTRY}?type=MCL") },
                onNavigateToOther = { navController.navigate("${Routes.LEAVE_ENTRY}?type=ML") },
                onNavigateToReports = { navController.navigate(Routes.LEAVE_REPORTS) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_CL) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            CLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_EL) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            ELLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_HPL) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            HPLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_WO) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            WeeklyOffScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_CCL) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            CCLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_MCL) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            MCLLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_OTHER) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            OtherLeaveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { type -> navController.navigate("${Routes.LEAVE_ENTRY}?type=$type") },
                onNavigateToEdit = { id -> navController.navigate(Routes.leaveEditRoute(id)) },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(
            route = "${Routes.LEAVE_ENTRY}?type={type}",
            arguments = listOf(navArgument("type") {
                defaultValue = "CL"
            })
        ) { backStackEntry ->
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            val type = backStackEntry.arguments?.getString("type") ?: "CL"
            LeaveEntryScreen(
                onNavigateBack = { navController.popBackStack() },
                preselectedType = type,
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(
            route = Routes.LEAVE_EDIT,
            arguments = listOf(navArgument("entryId") { defaultValue = "" })
        ) { backStackEntry ->
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            LeaveEditScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }

        composable(Routes.LEAVE_REPORTS) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            LeaveReportsScreen(
                onNavigateBack = { navController.popBackStack() },
                employeeViewModel = employeeViewModel,
                leaveViewModel = leaveViewModel
            )
        }
    }
}
