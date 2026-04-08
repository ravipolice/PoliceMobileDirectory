package com.example.policemobiledirectory.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.BuildConfig
import com.example.policemobiledirectory.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME

    // Handle back button to navigate to home screen
    BackHandler {
        // Navigate to home screen, clearing back stack up to (but not including) EMPLOYEE_LIST
        navController.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("About App") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 🔹 App Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 App Name
            Text(
                text = "Police Mobile Directory",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 🔹 App Version & Package Info
            Text(
                text = "Version: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = "Package: ${BuildConfig.APPLICATION_ID}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 App Description / Disclaimer
            Text(
                text = """
                    Police Mobile Directory – Important Notice

                    This application is developed as an independent utility tool to help police personnel communicate easily and quickly.

                    DISCLAIMER:
                    • This application is NOT an official Government of Karnataka or Karnataka Police Department application.
                    • The app is developed as a personal initiative to simplify internal communication.
                    • All information displayed in this application is collected from publicly available official sources and internal authorized records.
                    • This app does not represent or claim to represent any government entity.

                    Official Data Sources:
                    The information used in this application is referenced from official government sources such as:

                    • Karnataka State Police Official Website:
                    https://ksp.karnataka.gov.in

                    • District Police Official Websites and Public Directories

                    Users are advised to verify any critical information directly from the above official government websites.
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Official Source Button
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ksp.karnataka.gov.in"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("👉 Official Source Website")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Key Features
            Text(
                text = """
                    Key Features:
                    • Employee Directory with search and filters  
                    • Admin management tools  
                    • Notifications and approvals  
                    • Useful links and document uploads
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Developer Info
            Text(
                text = "Developed by",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Ravikumar J, Nandija Tech Group",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Clickable Email
            val email = "noreply.pmdapp@gmail.com"
            val annotatedText = buildAnnotatedString {
                append("For feedback or support:\n")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                ) {
                    append(email)
                }
            }

            Text(
                text = annotatedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                        putExtra(Intent.EXTRA_SUBJECT, "Police Mobile Directory - Feedback")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Clickable Phone
            val phoneNumber = "9844610264"
            val phoneText = buildAnnotatedString {
                append("Call Support:\n")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                ) {
                    append(phoneNumber)
                }
            }

            Text(
                text = phoneText,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Affiliation Statement
            Text(
                text = "Affiliation Statement:\nThis app is NOT affiliated with, endorsed, sponsored, or officially connected with any government organization.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
