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
    
    val name = employee?.name ?: officer?.name ?: ""
    val rank = employee?.displayRank ?: officer?.rank
    val unit = employee?.unit ?: officer?.unit
    val district = employee?.district ?: officer?.district
    val photoUrl = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    val placeholderRes = if (employee != null) R.drawable.officer else R.drawable.ic_officer_building

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ... (Avatar code remains same)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)), // Themed lavender-like background
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
                // Initial Letter Avatar (Kerala Police Style)
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
}
