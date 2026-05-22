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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
import com.example.policemobiledirectory.utils.getBloodGroupColor
import com.example.policemobiledirectory.utils.getFormattedBloodGroup
import com.example.policemobiledirectory.utils.getRankColor

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
    onToggleApproval: (() -> Unit)? = null,
    onToggleVisibility: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    val rawName = employee?.name ?: officer?.name ?: ""
    val rank = employee?.displayRank ?: officer?.rank
    val station = employee?.station ?: officer?.station ?: officer?.unit // station/section for subtitle
    val district = employee?.district ?: officer?.district
    
    val displayName = if (rawName.isNotBlank() && rawName.equals(rank, ignoreCase = true)) {
        val parts = mutableListOf(rawName)
        if (!station.isNullOrBlank()) {
            val cleanStation = station.replace("(?i)\\bPS\\b".toRegex(), "").replace("(?i)\\bPolice Station\\b".toRegex(), "").trim()
            if (cleanStation.isNotBlank()) parts.add(cleanStation)
        }
        if (!district.isNullOrBlank() && !district.equals(station, ignoreCase = true)) {
            parts.add(district)
        }
        parts.joinToString(" ")
    } else {
        rawName
    }

    val photoUrl = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    val bloodGroup = employee?.bloodGroup ?: officer?.bloodGroup
    val placeholderRes = if (employee != null) R.drawable.officer else R.drawable.ic_officer_building

    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔹 Circular Avatar or Initial Letter
            val avatarColors = listOf(
                Color(0xFF1565C0), // Deep Blue
                Color(0xFF2E7D32), // Deep Green
                Color(0xFF6A1B9A), // Deep Purple
                Color(0xFFC62828), // Deep Red
                Color(0xFF00838F), // Deep Cyan
                Color(0xFF4527A0), // Indigo
                Color(0xFF00695C), // Teal
                Color(0xFFAD1457), // Pink
                Color(0xFF283593), // Navy
                Color(0xFF558B2F), // Olive Green
                Color(0xFF1565C0), // Royal Blue
                Color(0xFF6D4C41), // Brown
            )
            val avatarBg = avatarColors[Math.abs(displayName.hashCode()) % avatarColors.size]

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
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
                    val initial = displayName.takeIf { it.isNotBlank() }?.first()?.uppercase() ?: "?"
                    Text(
                        text = initial.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Name + Rank (Top Row)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = (15 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!rank.isNullOrBlank() && !rawName.equals(rank, ignoreCase = true)) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = rank,
                            fontSize = (11 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = getRankColor(rank)
                        )
                    }
                }

                // Unified Subtitle: Duty Role, UNIT, Station
                val subtitleParts = mutableListOf<String>()
                
                // 1. Duty Role (Only if not blank and not "Others")
                if (!employee?.subSection.isNullOrBlank() && employee?.subSection != "Others") {
                    subtitleParts.add(employee?.subSection!!)
                }
                
                // 2. UNIT (Show only if different from district)
                val unitVal = employee?.unit ?: officer?.unit
                if (!unitVal.isNullOrBlank() && unitVal != district) {
                    subtitleParts.add(unitVal)
                }
                
                // 3. Station
                if (!station.isNullOrBlank()) {
                    subtitleParts.add(station)
                }

                // 4. District
                if (!district.isNullOrBlank()) {
                    subtitleParts.add(district)
                }
                
                val finalSubtitleParts = subtitleParts.distinct()

                if (finalSubtitleParts.isNotEmpty()) {
                    Text(
                        text = finalSubtitleParts.joinToString(", "),
                        fontSize = (11 * fontScale).sp,
                        lineHeight = (14 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }


            // 🔹 Actions (Admin Edit/Delete/Toggle or Chevron)
            if (isAdmin && (onEdit != null || onDelete != null || onToggleApproval != null || onToggleVisibility != null)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Approval Toggle (App Access)
                    if (onToggleApproval != null) {
                        val isApproved = employee?.isApproved ?: true
                        IconButton(
                            onClick = { onToggleApproval.invoke() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = if (isApproved) "Revoke Access" else "Grant Access",
                                tint = if (isApproved) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Visibility Toggle (Hide from Home)
                    if (onToggleVisibility != null) {
                        val isHidden = employee?.isHidden ?: officer?.isHidden ?: false
                        IconButton(
                            onClick = { onToggleVisibility.invoke() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isHidden) "Show on Home" else "Hide from Home",
                                tint = if (isHidden) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else PrimaryTeal.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onEdit != null) {
                        IconButton(
                            onClick = { onEdit.invoke() },
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
                                onDelete.invoke()
                            },
                            title = if (officer != null) "Delete Officer" else "Delete Contact",
                            text = "Are you sure you want to delete this ${if (officer != null) "Officer" else "Contact"}? This action cannot be undone."
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

        // 🔹 Blood Group Badge in top-right
        if (!bloodGroup.isNullOrBlank()) {
            val formattedBG = getFormattedBloodGroup(bloodGroup)
            val bgColor = getBloodGroupColor(formattedBG)
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 6.dp)
                    .size(24.dp) // Fixed size for perfect circle
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedBG,
                    fontSize = (8 * fontScale).sp, // Slightly smaller font
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
