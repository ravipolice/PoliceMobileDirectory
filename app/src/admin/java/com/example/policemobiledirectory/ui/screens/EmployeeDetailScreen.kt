@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import com.example.policemobiledirectory.navigation.Routes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.ui.theme.PrimaryTeal
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.utils.IntentUtils
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

/**
 * Modernized Employee Detail Screen for Admin Flavor
 */
@Composable
fun EmployeeDetailScreen(
    id: String,
    isOfficer: Boolean,
    navController: NavController,
    viewModel: EmployeeViewModel
) {
    val context = LocalContext.current
    val allEmployees by viewModel.filteredEmployees.collectAsState()
    val allOfficers by viewModel.filteredContacts.collectAsState()

    // Find the contact
    val contact = remember(id, isOfficer, allEmployees, allOfficers) {
        if (isOfficer) {
            allOfficers.find { it.officer?.agid == id }?.officer
        } else {
            allEmployees.find { it.kgid == id }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit Button
                    IconButton(onClick = { 
                        if (contact is Employee) {
                            navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=${contact.kgid}")
                        } else if (contact is Officer) {
                            navController.navigate("${Routes.ADD_OFFICER}?officerId=${contact.agid}")
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Contact")
                    }

                    // Delete Button
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Contact")
                    }

                    IconButton(onClick = { 
                        contact.let {
                            val name = if (it is Employee) it.name else (it as? Officer)?.name ?: ""
                            val mobile = if (it is Employee) it.mobile1 else (it as? Officer)?.mobile
                            val email = if (it is Employee) it.email else (it as? Officer)?.email
                            val rankStr = if (it is Employee) it.displayRank else (it as? Officer)?.rank ?: ""
                            val unitStr = if (it is Employee) it.unit ?: "" else (it as? Officer)?.unit ?: ""
                            IntentUtils.addToContacts(context, name, mobile ?: "", email ?: "", "$rankStr, $unitStr")
                        }
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Save Contact")
                    }
                    IconButton(onClick = { 
                        contact.let {
                            val name = if (it is Employee) it.name else (it as? Officer)?.name ?: ""
                            val rankStr = if (it is Employee) it.displayRank else (it as? Officer)?.rank ?: ""
                            val mobileStr = if (it is Employee) it.mobile1 else (it as? Officer)?.mobile ?: ""
                            val districtStr = if (it is Employee) it.district ?: "" else (it as? Officer)?.district ?: ""
                            val shareText = "Name: $name\nRank: $rankStr\nMobile: $mobileStr\nDistrict: $districtStr"
                            IntentUtils.shareText(context, shareText)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Contact")
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Contact") },
                            text = { Text("Are you sure you want to delete ${if (contact is Employee) contact.name else (contact as? Officer)?.name ?: ""}? This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                        if (contact is Employee) {
                                            viewModel.deleteEmployee(contact.kgid, contact.photoUrl ?: contact.photoUrlFromGoogle)
                                        } else if (contact is Officer) {
                                            viewModel.deleteOfficer(contact.agid)
                                        }
                                        navController.popBackStack()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTeal,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (contact == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Contact not found")
            }
        } else {
            val name = if (contact is Employee) contact.name else (contact as? Officer)?.name ?: ""
            val rank = if (contact is Employee) contact.displayRank else (contact as? Officer)?.rank ?: ""
            val photoUrlVal = if (contact is Employee) (contact.photoUrl ?: contact.photoUrlFromGoogle) else (contact as? Officer)?.photoUrl
            val placeholderRes = if (contact is Employee) R.drawable.officer else R.drawable.ic_officer_building
            
            val mobile = if (contact is Employee) contact.mobile1 else (contact as? Officer)?.mobile
            val landline = if (contact is Employee) contact.landline else (contact as? Officer)?.landline
            val email = if (contact is Employee) contact.email else (contact as? Officer)?.email
            val unit = if (contact is Employee) contact.unit else (contact as? Officer)?.unit
            val station = if (contact is Employee) contact.station else (contact as? Officer)?.station
            val district = if (contact is Employee) contact.district else (contact as? Officer)?.district

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp), // Increased spacing
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
            ) {
                // 🔹 Profile Header
                item {
                    val bloodGroup = if (contact is Employee) contact.bloodGroup else (contact as Officer).bloodGroup
                    val range = if (district != null) Constants.getRangeForDistrict(district) else ""

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 🩸 Blood Group Badge (Top Right)
                        if (!bloodGroup.isNullOrBlank() && bloodGroup != "??") {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 4.dp, end = 4.dp)
                                    .size(40.dp)
                                    .align(Alignment.TopEnd),
                                color = Color(0xFFC62828), // Deep Red
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = bloodGroup,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp) // Slightly larger
                                    .shadow(8.dp, CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                AsyncImage(
                                    model = photoUrlVal,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(placeholderRes),
                                    error = painterResource(placeholderRes)
                                )
                            }
                            
                            Spacer(Modifier.height(20.dp))
                            
                            // Name and Rank on same line
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B1A1A), // Maroon
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = rank,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))

                            // Subtitle Line 1: Duty Role, Unit / Station
                            val line1 = buildString {
                                val dr = if (contact is Employee) contact.subSection else ""
                                if (!dr.isNullOrBlank()) {
                                    append(dr)
                                    append(", ")
                                }
                                if (!unit.isNullOrBlank()) {
                                    append(unit)
                                }
                                if (!station.isNullOrBlank()) {
                                    append(" / ")
                                    append(station)
                                }
                            }
                            
                            if (line1.isNotBlank()) {
                                Text(
                                    text = line1,
                                    fontSize = 17.sp,
                                    color = Color(0xFF546E7A), // Blue-Grey
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Subtitle Line 2: District - Range
                            val line2 = buildString {
                                if (!district.isNullOrBlank()) {
                                    append(district)
                                }
                                if (range.isNotBlank()) {
                                    append(" - ")
                                    append(range)
                                }
                            }

                            if (line2.isNotBlank()) {
                                Text(
                                    text = line2,
                                    fontSize = 17.sp,
                                    color = Color(0xFF546E7A),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 🔹 Administration Section (Approval & Visibility)
                item {
                    DetailSection(title = "Administration") {
                        if (contact is Employee) {
                            AdminToggleRow(
                                label = "App Access",
                                checked = contact.isApproved,
                                onCheckedChange = { viewModel.updateEmployeeStatus(contact.kgid, it) },
                                checkedColor = PrimaryTeal
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }
                        
                        val isHidden = if (contact is Employee) contact.isHidden else if (contact is Officer) contact.isHidden else false
                        val id = if (contact is Employee) contact.kgid else if (contact is Officer) contact.agid else ""
                        val isOff = contact is Officer

                        AdminToggleRow(
                            label = "Hidden from Home",
                            checked = isHidden,
                            onCheckedChange = { viewModel.updateEmployeeVisibility(id, it, isOff) },
                            checkedColor = Color(0xFF2196F3)
                        )
                    }
                }

                item {
                    val metalNumber = if (contact is Employee) contact.metalNumber else null
                    val gender = if (contact is Employee) contact.gender else "N/A"

                    DetailSection(title = "Contact info") {
                        if (!metalNumber.isNullOrBlank()) {
                            InfoRow(
                                label = "Metal Number",
                                value = metalNumber,
                                icon = Icons.Default.Badge,
                                onAction = { IntentUtils.copyToClipboard(context, "Metal Number", metalNumber) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }

                        if (gender != "N/A" && gender.isNotBlank()) {
                            val genderIcon = if (gender.contains("Female", ignoreCase = true)) Icons.Default.Woman else Icons.Default.Man
                            InfoRow(
                                label = "Gender",
                                value = gender,
                                icon = genderIcon
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }

                        if (!mobile.isNullOrBlank()) {
                            InfoRow(
                                label = "Mobile",
                                value = mobile,
                                icon = Icons.Default.Call,
                                onAction = { IntentUtils.dial(context, mobile) },
                                secondaryActionIcon = painterResource(R.drawable.ic_whatsapp_logo),
                                onSecondaryAction = { IntentUtils.openWhatsApp(context, mobile) },
                                onSmsAction = { IntentUtils.sendSms(context, mobile) }
                            )
                            if (!landline.isNullOrBlank() || !email.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }

                        if (!landline.isNullOrBlank()) {
                            InfoRow(
                                label = "Landline",
                                value = landline,
                                icon = Icons.Default.Phone,
                                onAction = { IntentUtils.dial(context, landline) }
                            )
                            if (!email.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }

                        if (!email.isNullOrBlank()) {
                            InfoRow(
                                label = "Email",
                                value = email,
                                icon = Icons.Default.Email,
                                onAction = { IntentUtils.sendEmail(context, email) }
                            )
                        }
                    }
                }



            }
        }
    }
}

@Composable
private fun AdminToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (checked) "Enabled" else "Disabled",
                fontSize = 13.sp,
                color = (if (checked) checkedColor else Color.Gray).copy(alpha = 0.8f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = checkedColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: (() -> Unit)? = null,
    secondaryActionIcon: androidx.compose.ui.graphics.painter.Painter? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onSmsAction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onAction?.invoke() },
                onLongClick = { 
                    IntentUtils.copyToClipboard(context, label, value)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (secondaryActionIcon != null && onSecondaryAction != null) {
                IconButton(onClick = onSecondaryAction) {
                    Icon(
                        painter = secondaryActionIcon,
                        contentDescription = "WhatsApp",
                        tint = Color.Unspecified, 
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            
            if (onSmsAction != null) {
                IconButton(onClick = onSmsAction) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            
            IconButton(onClick = { onAction?.invoke() }) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
