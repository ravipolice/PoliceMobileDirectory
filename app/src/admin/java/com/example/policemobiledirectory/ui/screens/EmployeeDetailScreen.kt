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
                    IconButton(onClick = { 
                        if (contact != null) {
                            val name = if (contact is Employee) contact.name else (contact as Officer).name
                            val mobile = if (contact is Employee) contact.mobile1 else (contact as Officer).mobile
                            val email = if (contact is Employee) contact.email else (contact as Officer).email
                            val rank = if (contact is Employee) contact.displayRank else (contact as Officer).rank ?: ""
                            val unit = if (contact is Employee) contact.unit else (contact as Officer).unit
                            IntentUtils.addToContacts(context, name, mobile, email, "$rank, $unit")
                        }
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Save Contact")
                    }
                    IconButton(onClick = { 
                        if (contact != null) {
                            val name = if (contact is Employee) contact.name else (contact as Officer).name
                            val rank = if (contact is Employee) contact.displayRank else (contact as Officer).rank ?: ""
                            val mobile = if (contact is Employee) contact.mobile1 else (contact as Officer).mobile
                            val district = if (contact is Employee) contact.district else (contact as Officer).district
                            val shareText = "Name: $name\nRank: $rank\nMobile: $mobile\nDistrict: $district"
                            IntentUtils.shareText(context, shareText)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Contact")
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
            val name = if (contact is Employee) contact.name else (contact as Officer).name
            val rank = if (contact is Employee) contact.displayRank else (contact as Officer).rank ?: ""
            val photoUrl = if (contact is Employee) (contact.photoUrl ?: contact.photoUrlFromGoogle) else (contact as Officer).photoUrl
            val placeholderRes = if (contact is Employee) R.drawable.officer else R.drawable.ic_officer_building
            
            val mobile = if (contact is Employee) contact.mobile1 else (contact as Officer).mobile
            val landline = if (contact is Employee) contact.landline else (contact as Officer).landline
            val email = if (contact is Employee) contact.email else (contact as Officer).email
            val unit = if (contact is Employee) contact.unit else (contact as Officer).unit
            val station = if (contact is Employee) contact.station else (contact as Officer).station
            val district = if (contact is Employee) contact.district else (contact as Officer).district

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
            ) {
                // 🔹 Profile Header
                item {
                    val seniorRanks = remember { setOf("SP", "DCP", "DIG", "IGP", "ADGP", "DG", "CP", "DG & IGP") }
                    val isSenior = remember(rank) { seniorRanks.any { rank.trim().uppercase().contains(it) } }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 🏅 Senior Rank Badge
                        if (isSenior) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .align(Alignment.TopStart),
                                color = Color(0xFF2E7D32), 
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = rank,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .shadow(8.dp, CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(placeholderRes),
                                    error = painterResource(placeholderRes)
                                )
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, // Using primary color for name
                                textAlign = TextAlign.Center
                            )
                            
                            val rankUnit = buildString {
                                append(rank)
                                if (!unit.isNullOrBlank()) {
                                    append(" • ")
                                    append(unit)
                                }
                            }
                            Text(
                                text = rankUnit,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 🔹 Contact Information Section
                item {
                    DetailSection(title = "Contact info") {
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

                // 🔹 Work Details Section
                item {
                    DetailSection(title = "Work Info") {
                        InfoRow(
                            label = "District / HQ",
                            value = district ?: "N/A",
                            icon = Icons.Default.LocationCity
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                        InfoRow(
                            label = "Station / Section",
                            value = station ?: "N/A",
                            icon = Icons.Default.Security
                        )
                    }
                }

                // 🔹 Administration Section (Admin Only)
                if (contact != null) {
                    item {
                        DetailSection(title = "Administration") {
                            if (contact is Employee) {
                                // App Access
                                AdminToggleRow(
                                    label = "App Access",
                                    checked = contact.isApproved,
                                    onCheckedChange = { viewModel.updateEmployeeStatus(contact.kgid, it) },
                                    checkedColor = PrimaryTeal
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                                // Hidden status
                                AdminToggleRow(
                                    label = "Hidden from Home",
                                    checked = contact.isHidden,
                                    onCheckedChange = { viewModel.updateEmployeeVisibility(contact.kgid, it) },
                                    checkedColor = MaterialTheme.colorScheme.error
                                )

                                Spacer(Modifier.height(20.dp))
                            }

                            // Edit & Delete Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        if (contact is Employee) {
                                            navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=${contact.kgid}")
                                        } else if (contact is Officer) {
                                            navController.navigate("${Routes.ADD_OFFICER}?officerId=${contact.agid}")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit Info")
                                }

                                var showDeleteDialog by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Delete")
                                }

                                if (showDeleteDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteDialog = false },
                                        title = { Text("Delete Contact") },
                                        text = { Text("Are you sure you want to delete $name? This action cannot be undone.") },
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
                            }
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
