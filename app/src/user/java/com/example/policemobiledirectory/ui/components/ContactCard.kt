package com.example.policemobiledirectory.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit // ✅ Added Edit icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
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
import com.example.policemobiledirectory.utils.IntentUtils
import com.example.policemobiledirectory.utils.getBloodGroupColor
import com.example.policemobiledirectory.utils.getFormattedBloodGroup
import com.example.policemobiledirectory.utils.getRankColor
import com.example.policemobiledirectory.utils.getContactDisplayName
import com.example.policemobiledirectory.utils.getShortRangeName

/**
 * Unified contact card that works for both Employee and Officer
 */
@Composable
fun ContactCard(
    employee: Employee? = null,
    officer: Officer? = null,
    fontScale: Float = 1.0f,
    isAdmin: Boolean = false, // ✅ Added isAdmin
    onEdit: (() -> Unit)? = null, // ✅ Added onEdit callback
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    val rawName = employee?.name ?: officer?.name ?: ""
    val rank = employee?.displayRank ?: officer?.rank?.replace(".", "")?.replace("(?i)\\bDy SP\\b".toRegex(), "DySP")?.trim()
    val station = employee?.station ?: officer?.station ?: officer?.unit // station/section for subtitle
    val district = employee?.district ?: officer?.district
    val displayName = getContactDisplayName(employee, officer)

    val photoUrl = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    val placeholderRes = if (employee != null) R.drawable.officer else R.drawable.ic_officer_building

    val bloodGroup = employee?.bloodGroup ?: officer?.bloodGroup

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ... (Avatar code remains same)
            // --- Avatar Section (Updated to match Admin's vibrant style) ---
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
                Color(0xFF6D4C41), // Brown
            )
            val avatarBgColor = remember(displayName) {
                if (displayName.isBlank()) Color(0xFFE0E0E0)
                else avatarColors[Math.abs(displayName.hashCode() % avatarColors.size)]
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBgColor),
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
                    // Initial Letter Avatar (Vibrant Style with White Text)
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
                
                val finalSubtitleParts = subtitleParts.distinct().map { getShortRangeName(it) }

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


            // 🔹 Actions (Edit or Chevron)
            if (isAdmin && onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Officer",
                        tint = PrimaryTeal.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Subtle up-arrow or indicator (Kerala Style)
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
