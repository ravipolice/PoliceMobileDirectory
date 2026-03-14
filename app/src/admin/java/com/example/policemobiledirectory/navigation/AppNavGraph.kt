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
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.policemobiledirectory.ui.screens.*
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.net.Uri
import com.example.policemobiledirectory.viewmodel.AddEditEmployeeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.policemobiledirectory.ui.screens.AddEditEmployeeScreen
import com.example.policemobiledirectory.viewmodel.AdminDocumentsViewModel
import com.example.policemobiledirectory.ui.screens.DocumentsScreen

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

    val hideBars = currentRoute?.let { route ->
        route.startsWith(Routes.SPLASH) ||
        route.startsWith(Routes.LOGIN) ||
        route.startsWith(Routes.REGISTER) ||
        route.startsWith(Routes.USER_REGISTRATION) ||
        route.startsWith(Routes.FORGOT_PIN)
    } ?: true

    // ✅ Global Navigation Drawer (only wraps if not hidden)
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
                    onLogout = onLogout,
                    onGoogleSignInClicked = onGoogleSignInClicked,
                    startDestination = startDestination,
                    scope = scope,
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding)
                )
            }
        }
    } else {
        AppNavHostContent(
            navController = navController,
            employeeViewModel = employeeViewModel,
            onThemeToggle = onThemeToggle,
            onLogout = onLogout,
            onGoogleSignInClicked = onGoogleSignInClicked,
            startDestination = startDestination,
            scope = scope
        )
    }
}
@Composable
private fun AppNavHostContent(
    navController: NavHostController,
    employeeViewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    startDestination: String,
    scope: kotlinx.coroutines.CoroutineScope,
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
                onLoginSuccess = { isAdmin ->
                    scope.launch {
                        var target = Routes.EMPLOYEE_LIST
                        if (isAdmin) {
                            employeeViewModel.refreshPendingRegistrations()
                            delay(500)
                            if (employeeViewModel.pendingApprovalsTotalCount.value > 0) {
                                target = Routes.PENDING_APPROVALS
                            }
                        }
                        navController.navigate(target) {
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
                onThemeToggle = onThemeToggle,
                onLogout = onLogout
            )
        }

        // --- USER REGISTRATION ---
        composable(
            route = "${Routes.USER_REGISTRATION}?email={email}&name={name}",
            arguments = listOf(
                androidx.navigation.navArgument("email") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                },
                androidx.navigation.navArgument("name") {
                    type = androidx.navigation.NavType.StringType
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
            val viewModel: AdminDocumentsViewModel = hiltViewModel<AdminDocumentsViewModel>()
            DocumentsScreen(
                navController = navController,
                viewModel = viewModel,
                isAdmin = isAdmin
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

        // --- ADMIN PANEL ---
        composable(Routes.ADMIN_PANEL) {
            AdminPanelScreen(
                navController = navController,
                viewModel = employeeViewModel
            )
        }

        // --- EMPLOYEE STATS ---
        composable(Routes.EMPLOYEE_STATS) {
            EmployeeStatsScreen(navController = navController, viewModel = hiltViewModel())
        }

        // --- OFFICER STATS ---
        composable(Routes.OFFICER_STATS) {
            OfficerStatsScreen(navController = navController, viewModel = hiltViewModel())
        }

        // --- PENDING APPROVALS ---
        composable(Routes.PENDING_APPROVALS) {
            PendingApprovalsScreen(navController = navController, viewModel = hiltViewModel())
        }

        // --- SEND NOTIFICATION ---
        composable(Routes.SEND_NOTIFICATION) {
            SendNotificationScreen(navController = navController, viewModel = hiltViewModel())
        }

        // --- UPLOAD CSV ---
        composable(Routes.UPLOAD_CSV) {
            UploadCsvScreen(navController = navController, viewModel = hiltViewModel())
        }

        // --- ADD USEFUL LINK ---
        composable(Routes.ADD_USEFUL_LINK) {
            AddUsefulLinkScreen(navController = navController, viewModel = hiltViewModel())
        }

        // --- UPLOAD DOCUMENT ---
        composable(Routes.UPLOAD_DOCUMENT) {
            UploadDocumentScreen(
                navController = navController,
                isAdmin = isAdmin
            )
        }

        // --- ADD / EDIT EMPLOYEE ---
        composable(
            route = "${Routes.ADD_EMPLOYEE}?employeeId={employeeId}",
            arguments = listOf(
                navArgument("employeeId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getString("employeeId")
            val addEditViewModel: AddEditEmployeeViewModel = hiltViewModel()
            // Use hiltViewModel() to get a fresh instance for this screen (not the shared parameter)
            val addEditScreenEmployeeViewModel: EmployeeViewModel = hiltViewModel()

            AddEditEmployeeScreen(
                employeeId = employeeId,
                navController = navController,
                addEditViewModel = addEditViewModel,
                employeeViewModel = addEditScreenEmployeeViewModel
            )
        }


        // --- ADD OFFICER ---
        composable(
            route = "${Routes.ADD_OFFICER}?officerId={officerId}",
            arguments = listOf(
                navArgument("officerId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val officerId = backStackEntry.arguments?.getString("officerId")
            val addEditOfficerViewModel: com.example.policemobiledirectory.viewmodel.AddEditOfficerViewModel = hiltViewModel()
            val employeeViewModelForRefresh: EmployeeViewModel = hiltViewModel()

            AddEditOfficerScreen(
                officerId = officerId,
                navController = navController,
                viewModel = addEditOfficerViewModel,
                employeeViewModel = employeeViewModelForRefresh
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
            EmployeeDetailScreen(
                id = id,
                isOfficer = isOfficer,
                navController = navController,
                viewModel = employeeViewModel
            )
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

        // --- NUDI CONVERTER ---
        composable(Routes.NUDI_CONVERTER) {
            NudiConverterScreen(navController = navController)
        }

        // --- MY PROFILE ---
        composable(Routes.MY_PROFILE) {
            MyProfileEditScreen(navController = navController)
        }

        // --- NOTIFICATIONS ---
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(navController = navController, viewModel = employeeViewModel)
        }

        // --- Gallery Screen ---
        composable(Routes.GALLERY_SCREEN) {
            val isAdmin by employeeViewModel.isAdmin.collectAsStateWithLifecycle()
            GalleryScreen(
                navController = navController,
                isAdmin = isAdmin
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

        // --- MANAGE CONSTANTS ---
        composable(
            route = "${Routes.MANAGE_CONSTANTS}?initialTab={initialTab}",
            arguments = listOf(
                navArgument("initialTab") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: -1
            // Using hiltViewModel() to get scoped instance of ConstantsViewModel
            val constantsViewModel: com.example.policemobiledirectory.viewmodel.ConstantsViewModel = hiltViewModel()
            com.example.policemobiledirectory.ui.screens.ManageConstantsScreen(
                navController = navController,
                viewModel = constantsViewModel,
                initialTab = initialTab
            )
        }

        // --- LEAVE MANAGER ---
        composable(Routes.LEAVE_DASHBOARD) {
            val leaveViewModel: com.example.policemobiledirectory.viewmodel.LeaveViewModel = hiltViewModel()
            LeaveDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCl = { navController.navigate(Routes.LEAVE_CL) },
                onNavigateToEl = { navController.navigate(Routes.LEAVE_EL) },
                onNavigateToHpl = { navController.navigate(Routes.LEAVE_HPL) },
                onNavigateToWo = { navController.navigate(Routes.LEAVE_WO) },
                onNavigateToCcl = { navController.navigate(Routes.LEAVE_CCL) },
                onNavigateToMcl = { navController.navigate(Routes.LEAVE_MCL) },
                onNavigateToOther = { navController.navigate(Routes.LEAVE_OTHER) },
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
            arguments = listOf(androidx.navigation.navArgument("type") {
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
            arguments = listOf(androidx.navigation.navArgument("entryId") { defaultValue = "" })
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

        composable(Routes.LEAVE_MANAGER_ADMIN) {
            LeaveManagerAdminScreen(navController = navController)
        }
    }
}
