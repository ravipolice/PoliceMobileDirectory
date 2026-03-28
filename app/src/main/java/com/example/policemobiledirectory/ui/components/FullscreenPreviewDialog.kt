@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.policemobiledirectory.ui.components

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay

@Composable
fun FullscreenPreviewDialog(
    title: String,
    url: String,
    mimeType: String,
    onDismiss: () -> Unit,
    onDocumentBroken: ((String) -> Unit)? = null,
    onDownload: (String, String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Auto-hide loader after 10 seconds (fallback for stuck Google Docs viewer)
    LaunchedEffect(Unit) {
        delay(10_000)
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (mimeType.startsWith("image/")) Color.Black else MaterialTheme.colorScheme.background
        ) {
            Column {
                // ✅ Top Bar
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = title, 
                            maxLines = 1, 
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = if (mimeType.startsWith("image/")) Color.White else MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Close",
                                tint = if (mimeType.startsWith("image/")) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (mimeType.startsWith("image/")) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { onDownload(url, title) }) {
                            Icon(
                                Icons.Default.FileDownload, 
                                contentDescription = "Download",
                                tint = if (mimeType.startsWith("image/")) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )

                // ✅ Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (mimeType.startsWith("image/")) 0.dp else 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        hasError -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "⚠️ Unable to preview this file.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "It may have been moved or deleted.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (mimeType.startsWith("image/")) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                // Try to let the user know we're removing it
                                SideEffect {
                                    onDocumentBroken?.invoke(title)
                                }
                            }
                        }

                        // ✅ PDF, Drive & Office Docs
                        mimeType.contains("pdf", ignoreCase = true) ||
                        url.contains("drive.google.com", ignoreCase = true) ||
                        mimeType.contains("word", ignoreCase = true) ||
                        mimeType.contains("excel", ignoreCase = true) ||
                        mimeType.contains("powerpoint", ignoreCase = true) -> {

                            AndroidView(factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            errorCode: Int,
                                            description: String?,
                                            failingUrl: String?
                                        ) {
                                            hasError = true
                                            isLoading = false
                                        }
                                    }

                                    // ✅ Smarter PDF/Drive Viewer Handling
                                    val viewerUrl = when {
                                        url.contains("drive.google.com") -> {
                                            if (url.contains("view?usp=sharing")) {
                                                url.replace("view?usp=sharing", "preview")
                                            } else if (url.contains("/view")) {
                                                url.substringBeforeLast("/view") + "/preview"
                                            } else {
                                                url
                                            }
                                        }
                                        else -> "https://docs.google.com/gview?embedded=true&url=${Uri.encode(url)}"
                                    }

                                    loadUrl(viewerUrl)
                                }
                            }, modifier = Modifier.fillMaxSize())
                        }

                        // ✅ Images
                        mimeType.startsWith("image/") -> {
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = "Preview Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            isLoading = false
                        }

                        // ❌ Other file types
                        else -> {
                            Text(
                                text = "Preview not available for this file type.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mimeType.startsWith("image/")) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            isLoading = false
                        }
                    }

                    // ✅ Loader overlay
                    if (isLoading && !hasError) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    (if (mimeType.startsWith("image/")) Color.Black else MaterialTheme.colorScheme.surface).copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = if (mimeType.startsWith("image/")) Color.White else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Loading preview...", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (mimeType.startsWith("image/")) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
