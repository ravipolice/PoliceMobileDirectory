package com.example.policemobiledirectory.ui.theme.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.data.local.AppIconEntity
import com.example.policemobiledirectory.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

import com.example.policemobiledirectory.repository.AppIconRepository

/**
 * 🔹 Play Store App Icon Loader + Offline Cache
 * Fetches icon from Play Store once, caches it in Room (AppIconEntity),
 * and loads via Coil with offline support.
 */
@Composable
fun PlayStoreAppIcon(
    playStoreUrl: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var iconUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(playStoreUrl) {
        val db = AppDatabase.getInstance(context)
        val dao = db.appIconDao()
        val repo = AppIconRepository(dao)
        iconUrl = repo.getOrFetchAppIcon(playStoreUrl)
        isLoading = false
    }

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            iconUrl != null -> Image(
                painter = rememberAsyncImagePainter(iconUrl),
                contentDescription = "$appName Icon",
                modifier = Modifier.fillMaxSize()
            )
            else -> Text("❌ No Icon", modifier = Modifier.align(Alignment.Center))
        }
    }
}
