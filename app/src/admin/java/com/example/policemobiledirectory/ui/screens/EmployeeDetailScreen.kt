@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.utils.OperationStatus

/**
 * Premium modernized Employee Detail Screen for Admin Flavor
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
    val employeeStatus by viewModel.employeeStatus.collectAsState()
    val officerStatus by viewModel.officerStatus.collectAsState()

    val isLoading = remember(isOfficer, employeeStatus, officerStatus) {
        if (isOfficer) {
            officerStatus is OperationStatus.Loading
        } else {
            employeeStatus is OperationStatus.Loading
        }
    }

    val contact = remember(id, isOfficer, allEmployees, allOfficers) {
        if (isOfficer) {
            allOfficers.find { it.officer?.agid == id }?.officer
        } else {
            allEmployees.find { it.kgid == id }
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Contact Details", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (contact is Employee) {
                            navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=${contact.kgid}")
                        } else if (contact is Officer) {
                            navController.navigate("${Routes.ADD_OFFICER}?officerId=${contact.agid}")
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                        Icon(Icons.Default.PersonAdd, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFBFBFB)
    ) { innerPadding ->
        if (contact == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Contact not found")
                }
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
            val bloodGroup = if (contact is Employee) contact.bloodGroup else (contact as? Officer)?.bloodGroup

            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Brush.verticalGradient(listOf(PrimaryTeal, Color(0xFFFBFBFB))))
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 100.dp, bottom = 40.dp)
                ) {
                    item {
                        AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })) {
                            ProfileHeader(
                                name = name,
                                rank = rank,
                                photoUrl = photoUrlVal,
                                placeholderRes = placeholderRes,
                                unit = unit,
                                station = station,
                                district = district,
                                bloodGroup = bloodGroup
                            )
                        }
                    }

                    // Admin Controls
                    item {
                        AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 50 })) {
                            DetailSection(title = "Administration") {
                                if (contact is Employee) {
                                    AdminToggleRow(
                                        label = "App Access",
                                        checked = contact.isApproved,
                                        onCheckedChange = { viewModel.updateEmployeeStatus(contact.kgid, it) },
                                        checkedColor = PrimaryTeal
                                    )
                                    CustomDivider()
                                }
                                
                                val isHidden = if (contact is Employee) contact.isHidden else if (contact is Officer) contact.isHidden else false
                                val idToUpdate = if (contact is Employee) contact.kgid else if (contact is Officer) contact.agid else ""
                                val isOff = contact is Officer

                                AdminToggleRow(
                                    label = "Hidden from Home",
                                    checked = isHidden,
                                    onCheckedChange = { viewModel.updateEmployeeVisibility(idToUpdate, it, isOff) },
                                    checkedColor = Color(0xFF2196F3)
                                )
                            }
                        }
                    }

                    // Contact Info
                    item {
                        AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(700)) + slideInVertically(initialOffsetY = { 70 })) {
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
                                    CustomDivider()
                                }

                                if (gender != "N/A" && gender.isNotBlank()) {
                                    val genderIcon = if (gender.contains("Female", ignoreCase = true)) Icons.Default.Woman else Icons.Default.Man
                                    InfoRow(label = "Gender", value = gender, icon = genderIcon)
                                    CustomDivider()
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
                                    if (!landline.isNullOrBlank() || !email.isNullOrBlank()) CustomDivider()
                                }

                                if (!landline.isNullOrBlank()) {
                                    InfoRow(label = "Landline", value = landline, icon = Icons.Default.Phone, onAction = { IntentUtils.dial(context, landline) })
                                    if (!email.isNullOrBlank()) CustomDivider()
                                }

                                if (!email.isNullOrBlank()) {
                                    InfoRow(label = "Email", value = email, icon = Icons.Default.Email, onAction = { IntentUtils.sendEmail(context, email) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete ${if (contact is Employee) (contact as Employee).name else (contact as? Officer)?.name ?: ""}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        if (contact is Employee) {
                            viewModel.deleteEmployee((contact as Employee).kgid, (contact as Employee).photoUrl ?: (contact as Employee).photoUrlFromGoogle)
                        } else if (contact is Officer) {
                            viewModel.deleteOfficer((contact as Officer).agid)
                        }
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    rank: String,
    photoUrl: String?,
    placeholderRes: Int,
    unit: String?,
    station: String?,
    district: String?,
    bloodGroup: String?
) {
    val isCid = remember(unit, name) {
        val u = unit?.uppercase() ?: ""
        val n = name.uppercase()
        u.contains("CID") || n.contains("CID")
    }

    val range = remember(district, isCid) {
        if (isCid) "" else (if (district != null) Constants.getRangeForDistrict(district) else "")
    }

    val isStateHq = remember(unit) {
        val u = unit?.uppercase() ?: ""
        u.contains("INTELLIGENCE") || u.contains("KSRP") || u.contains("ISD") || u.contains("SCRB") || u.contains("INT.")
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier.size(150.dp).shadow(12.dp, CircleShape).border(4.dp, Color.White, CircleShape).clip(CircleShape).background(Color.White)
            ) {
                AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, placeholder = painterResource(placeholderRes), error = painterResource(placeholderRes))
            }

            if (!bloodGroup.isNullOrBlank() && bloodGroup != "??") {
                Surface(
                    modifier = Modifier.size(44.dp).align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp),
                    color = Color(0xFFC62828), shape = CircleShape, shadowElevation = 6.dp, border = BorderStroke(2.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = bloodGroup, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(text = name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B1A1A), textAlign = TextAlign.Center, lineHeight = 34.sp)
        Text(text = rank, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF455A64), textAlign = TextAlign.Center)

        Spacer(Modifier.height(12.dp))

        val line1 = buildString {
            if (!unit.isNullOrBlank()) append(unit)
            if (!station.isNullOrBlank() && station != unit && !unit?.contains(station, ignoreCase = true)!!) {
                if (this.isNotEmpty()) append(" / ")
                append(station)
            }
        }
        if (line1.isNotBlank()) {
            Text(text = line1, fontSize = 17.sp, color = Color(0xFF607D8B), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }

        val line2 = buildString {
            val d = district?.trim() ?: ""
            val u = unit?.trim() ?: ""
            
            val showDistrict = if (isStateHq && d.contains("Bengaluru", ignoreCase = true)) {
                false
            } else if (d.isNotBlank() && !u.contains(d, ignoreCase = true) && !d.contains(u, ignoreCase = true)) {
                true
            } else {
                false
            }

            if (showDistrict) {
                append(d)
            }

            if (range.isNotBlank() && !u.contains(range, ignoreCase = true) && !d.contains(range, ignoreCase = true)) {
                if (this.isNotEmpty()) append(" - ")
                append(range)
            }
        }
        if (line2.isNotBlank()) {
            Text(text = line2, fontSize = 16.sp, color = Color(0xFF78909C), textAlign = TextAlign.Center)
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
            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Text(text = if (checked) "Enabled" else "Disabled", fontSize = 13.sp, color = (if (checked) checkedColor else Color.Gray).copy(alpha = 0.8f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = checkedColor)
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp)), color = Color.White, shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 16.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    onAction: (() -> Unit)? = null,
    secondaryActionIcon: androidx.compose.ui.graphics.painter.Painter? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onSmsAction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onAction?.invoke() }, onLongClick = { IntentUtils.copyToClipboard(context, label, value) }).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), color = Color(0xFFF5F7F8), shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF37474F), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (secondaryActionIcon != null && onSecondaryAction != null) {
                IconButton(onClick = onSecondaryAction) {
                    Icon(painter = secondaryActionIcon, contentDescription = "WhatsApp", tint = Color.Unspecified, modifier = Modifier.size(26.dp))
                }
            }
            if (onSmsAction != null) {
                IconButton(onClick = onSmsAction) {
                    Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS", tint = Color(0xFF546E7A), modifier = Modifier.size(22.dp))
                }
            }
            if (onAction != null) {
                IconButton(onClick = onAction) {
                    Icon(imageVector = if (icon == Icons.Default.Call) Icons.Default.Call else icon, contentDescription = "Action", tint = if (icon == Icons.Default.Call) Color(0xFF2E7D32) else Color(0xFF546E7A), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp, top = 12.dp, bottom = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
}
