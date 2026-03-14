package com.example.policemobiledirectory.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.ui.theme.components.DeleteEmployeeDialog

/**
 * Unified contact card that works for both Employee and Officer (Admin Version)
 * Modernized with minimalist row-based design.
 */
@Composable
fun ContactCard(
    employee: Employee? = null,
    officer: Officer? = null,
    fontScale: Float = 1.0f,
    isAdmin: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    val name = employee?.name ?: officer?.name ?: ""
    val rank = employee?.displayRank ?: officer?.rank
    val unit = employee?.unit ?: officer?.unit
    val district = employee?.district ?: officer?.district
    val photoUrl = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    val placeholderRes = if (employee != null) R.drawable.officer else R.drawable.ic_officer_building

    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🔹 Circular Avatar or Initial Letter
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Contact Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(placeholderRes),
                    error = painterResource(placeholderRes)
                )
            } else {
                val initial = name.takeIf { it.isNotBlank() }?.first()?.uppercase() ?: "?"
                Text(
                    text = initial.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Name + Rank (Bold & Clean)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = (15 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                if (!rank.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = rank,
                        fontSize = (11 * fontScale).sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Unit / Section + District (Subtitle)
            val subTitleParts = listOfNotNull(unit, district).filter { it.isNotBlank() }
            if (subTitleParts.isNotEmpty()) {
                Text(
                    text = subTitleParts.joinToString(" • "),
                    fontSize = (13 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        // 🔹 Actions (Admin Edit/Delete or Chevron)
        if (isAdmin && (onEdit != null || onDelete != null)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = PrimaryTeal.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                if (onDelete != null) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    DeleteEmployeeDialog(
                        showDialog = showDeleteDialog,
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            showDeleteDialog = false
                            onDelete()
                        },
                        title = if (officer != null) "Delete Officer/Unit" else "Delete Contact",
                        text = "Are you sure you want to delete this Officer/Unit? This action cannot be undone."
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
