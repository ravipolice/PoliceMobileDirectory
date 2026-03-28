package com.example.policemobiledirectory.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(title: String, navController: NavController) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (navController.previousBackStackEntry != null) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                Spacer(modifier = Modifier.width(0.dp))
            }
        },
        actions = {
            IconButton(onClick = { shareAppLink(context) }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share App",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
fun ErrorSection(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Connection Issue", 
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error, 
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp)
        ) { 
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Retry") 
        }
    }
}

@Composable
fun EmptySection(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Description,
    message: String = "No items found"
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            message, 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

suspend fun uriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun uriToBase64Compressed(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1920,
    quality: Int = 85
): String? = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, boundsOptions) 
        }
        
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            val inputStream: InputStream? = resolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            return@withContext bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        }
        
        val inSampleSize = calculateInSampleSize(boundsOptions, maxDimension)
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        
        val decodedBitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return@withContext null
        
        val needsScaling = decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension
        val processedBitmap: Bitmap = if (needsScaling) {
            val ratio = kotlin.math.min(
                maxDimension.toFloat() / decodedBitmap.width.toFloat(),
                maxDimension.toFloat() / decodedBitmap.height.toFloat()
            )
            val targetWidth = (decodedBitmap.width * ratio).roundToInt().coerceAtLeast(1)
            val targetHeight = (decodedBitmap.height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
        } else {
            decodedBitmap
        }
        
        val outputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val compressedBytes = outputStream.toByteArray()
        
        if (processedBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }
        processedBitmap.recycle()
        outputStream.close()
        
        Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    var inSampleSize = 1
    val height = options.outHeight
    val width = options.outWidth
    
    if (height > maxDimension || width > maxDimension) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        
        while ((halfHeight / inSampleSize) >= maxDimension && 
               (halfWidth / inSampleSize) >= maxDimension) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun convertDriveUrlToDirectImageUrl(driveUrl: String?): String {
    if (driveUrl.isNullOrBlank()) {
        return ""
    }
    
    return try {
        if (driveUrl.contains("drive.google.com/uc?export=view")) {
            return driveUrl
        }
        
        if (driveUrl.contains("firebasestorage.googleapis.com") || 
            driveUrl.contains("firebasestorage.app") ||
            driveUrl.contains("storage.googleapis.com")) {
            return driveUrl
        }
        
        val fileId = when {
            driveUrl.contains("/file/d/") -> {
                val startIndex = driveUrl.indexOf("/file/d/") + 8
                val endIndex = driveUrl.indexOf("/", startIndex).let { 
                    if (it == -1) driveUrl.indexOf("?", startIndex).let { q -> if (q == -1) driveUrl.length else q }
                    else it
                }
                driveUrl.substring(startIndex, endIndex)
            }
            driveUrl.contains("?id=") -> {
                driveUrl.substringAfter("?id=").substringBefore("&")
            }
            else -> {
                val match = Regex("/([a-zA-Z0-9_-]{25,})").find(driveUrl)
                match?.groupValues?.get(1) ?: return driveUrl
            }
        }
        
        "https://drive.google.com/uc?export=view&id=$fileId"
    } catch (e: Exception) {
        driveUrl
    }
}

@Composable
fun GoogleDriveNoticeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Info")
            }
        },
        text = {
            Text("Data for this page is being loaded from Google Drive. You may experience a small delay while files are fetched.")
        }
    )
}

@Composable
fun GoogleDriveDisclaimerBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Note: Fetched from Google Drive; loading may take a few moments.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun shareAppLink(context: Context) {
    val packageName = context.packageName
    val playStoreLink = "https://play.google.com/store/apps/details?id=$packageName"
    val shareText = "Check out the Police Mobile Directory app: $playStoreLink"
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    
    context.startActivity(Intent.createChooser(intent, "Share App Via"))
}

fun downloadFile(context: Context, url: String, title: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setDescription("Downloading file...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading $title...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
    }
}
