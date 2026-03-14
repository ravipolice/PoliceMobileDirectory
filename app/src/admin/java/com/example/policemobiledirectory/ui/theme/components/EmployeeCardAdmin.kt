package com.example.policemobiledirectory.ui.theme.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.theme.*

/**
 * Modernized Admin Employee Card
 * Minimalist Row design with Administrative Toggles.
 */
@Composable
fun EmployeeCardAdmin(
    employee: Employee,
    isAdmin: Boolean,
    fontScale: Float,
    navController: NavController,
    context: Context,
    cardStyle: CardStyle = CardStyle.Vibrant 
) {
    val photoUrl = employee.photoUrl ?: employee.photoUrlFromGoogle
    val placeholderRes = R.drawable.officer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    navController.navigate(Routes.employeeDetailRoute(employee.kgid, false))
                }
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
                        contentDescription = "Employee Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(placeholderRes),
                        error = painterResource(placeholderRes)
                    )
                } else {
                    val initial = employee.name.takeIf { it.isNotBlank() }?.first()?.uppercase() ?: "?"
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
                // Name + Rank 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = employee.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = (15 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    val rankText = employee.displayRank.ifBlank { employee.rank.orEmpty() }
                    if (rankText.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = rankText,
                            fontSize = (11 * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // KGID + Unit / Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KGID: ${employee.kgid}",
                        fontSize = (12 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    
                    val subTitleInfo = listOfNotNull(employee.unit, employee.district).filter { it.isNotBlank() }.joinToString(" • ")
                    if (subTitleInfo.isNotBlank()) {
                        Text(
                            text = " • $subTitleInfo",
                            fontSize = (12 * fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Arrow to indicate clickable
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    }
}
