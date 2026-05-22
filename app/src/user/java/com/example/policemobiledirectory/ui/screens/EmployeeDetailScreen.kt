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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.utils.IntentUtils
import com.example.policemobiledirectory.viewmodel.EmployeeListViewModel
import com.example.policemobiledirectory.viewmodel.SettingsViewModel
import com.example.policemobiledirectory.utils.OperationStatus

@Composable
fun EmployeeDetailScreen(
    id: String,
    isOfficer: Boolean,
    navController: NavController,
    viewModel: EmployeeListViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val officerStatus by viewModel.officerStatus.collectAsStateWithLifecycle()

    val isLoading = remember(isOfficer, employeeStatus, officerStatus) {
        if (isOfficer) {
            officerStatus is OperationStatus.Loading
        } else {
            employeeStatus is OperationStatus.Loading
        }
    }

    val contact = remember(id, isOfficer, allContacts) {
        if (isOfficer) {
            allContacts.find { it.officer?.agid == id }?.officer
        } else {
            allContacts.find { it.employee?.kgid == id }?.employee
        }
    }

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
                        contact.let {
                            val nameStr = if (it is Employee) it.name else (it as? Officer)?.name ?: ""
                            val mobileStr = if (it is Employee) it.mobile1 else (it as? Officer)?.mobile
                            val emailStr = if (it is Employee) it.email else (it as? Officer)?.email
                            val rankStr = if (it is Employee) it.displayRank else (it as? Officer)?.rank ?: ""
                            val unitStr = if (it is Employee) it.unit ?: "" else (it as? Officer)?.unit ?: ""
                            IntentUtils.addToContacts(context, nameStr, mobileStr ?: "", emailStr ?: "", "$rankStr, $unitStr")
                        }
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Save Contact")
                    }
                    IconButton(onClick = { 
                        contact.let {
                            val nameStr = if (it is Employee) it.name else (it as? Officer)?.name ?: ""
                            val rankStr = if (it is Employee) it.displayRank else (it as? Officer)?.rank ?: ""
                            val mobileStr = if (it is Employee) it.mobile1 else (it as? Officer)?.mobile ?: ""
                            val districtStr = if (it is Employee) it.district ?: "" else (it as? Officer)?.district ?: ""
                            val shareText = "Name: $nameStr\nRank: $rankStr\nMobile: $mobileStr\nDistrict: $districtStr"
                            IntentUtils.shareText(context, shareText)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Contact")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
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

            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFFBFBFB))
                            )
                        )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 100.dp, bottom = 40.dp)
                ) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                        ) {
                            ProfileHeader(
                                name = name,
                                rank = rank,
                                photoUrl = photoUrlVal,
                                placeholderRes = placeholderRes,
                                unit = unit,
                                station = station,
                                district = district,
                                bloodGroup = if (contact is Employee) contact.bloodGroup else (contact as? Officer)?.bloodGroup,
                                fontScale = fontScale
                            )
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 60 })
                        ) {
                            val metalNumber = if (contact is Employee) contact.metalNumber else null
                            val gender = if (contact is Employee) contact.gender else "N/A"

                            DetailSection(title = "Contact info", fontScale = fontScale) {
                                if (!metalNumber.isNullOrBlank()) {
                                    InfoRow(
                                        label = "Metal Number",
                                        value = metalNumber,
                                        icon = Icons.Default.Badge,
                                        fontScale = fontScale,
                                        onAction = { IntentUtils.copyToClipboard(context, "Metal Number", metalNumber) }
                                    )
                                    CustomDivider()
                                }

                                if (gender != "N/A") {
                                    val genderIcon = if (gender.contains("Female", ignoreCase = true)) Icons.Default.Woman else Icons.Default.Man
                                    InfoRow(
                                        label = "Gender",
                                        value = gender,
                                        icon = genderIcon,
                                        fontScale = fontScale
                                    )
                                    CustomDivider()
                                }

                                if (!mobile.isNullOrBlank()) {
                                    InfoRow(
                                        label = "Mobile",
                                        value = mobile,
                                        icon = Icons.Default.Call,
                                        fontScale = fontScale,
                                        onAction = { IntentUtils.dial(context, mobile) },
                                        secondaryActionIcon = painterResource(R.drawable.ic_whatsapp),
                                        onSecondaryAction = { IntentUtils.openWhatsApp(context, mobile) },
                                        onSmsAction = { IntentUtils.sendSms(context, mobile) }
                                    )
                                    if (!landline.isNullOrBlank() || !email.isNullOrBlank()) {
                                        CustomDivider()
                                    }
                                }

                                if (!landline.isNullOrBlank()) {
                                    InfoRow(
                                        label = "Landline",
                                        value = landline,
                                        icon = Icons.Default.Phone,
                                        fontScale = fontScale,
                                        onAction = { IntentUtils.dial(context, landline) }
                                    )
                                    if (!email.isNullOrBlank()) {
                                        CustomDivider()
                                    }
                                }

                                if (!email.isNullOrBlank()) {
                                    InfoRow(
                                        label = "Email",
                                        value = email,
                                        icon = Icons.Default.Email,
                                        fontScale = fontScale,
                                        onAction = { IntentUtils.sendEmail(context, email) }
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
private fun ProfileHeader(
    name: String,
    rank: String,
    photoUrl: String?,
    placeholderRes: Int,
    unit: String?,
    station: String?,
    district: String?,
    bloodGroup: String?,
    fontScale: Float
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
                modifier = Modifier
                    .size(150.dp)
                    .shadow(12.dp, CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(placeholderRes),
                    error = painterResource(placeholderRes)
                )
            }

            if (!bloodGroup.isNullOrBlank() && bloodGroup != "??") {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp),
                    color = Color(0xFFC62828),
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = bloodGroup,
                            color = Color.White,
                            fontSize = (13 * fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = name,
            fontSize = (28 * fontScale).sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF8B1A1A),
            textAlign = TextAlign.Center,
            lineHeight = (34 * fontScale).sp
        )
        Text(
            text = rank,
            fontSize = (20 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF455A64),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        // Subtitle Line 1: Unit / Station
        val line1 = buildString {
            if (!unit.isNullOrBlank()) append(unit)
            if (!station.isNullOrBlank() && station != unit && !unit?.contains(station, ignoreCase = true)!!) {
                if (this.isNotEmpty()) append(" / ")
                append(station)
            }
        }
        
        if (line1.isNotBlank()) {
            Text(
                text = line1,
                fontSize = (17 * fontScale).sp,
                color = Color(0xFF607D8B),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        // Subtitle Line 2: District - Range
        val line2 = buildString {
            val d = district?.trim() ?: ""
            val u = unit?.trim() ?: ""
            
            // For state units, we usually don't need to show "Bengaluru City" if they are HQ
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
            Text(
                text = line2,
                fontSize = (16 * fontScale).sp,
                color = Color(0xFF78909C),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    fontScale: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        color = Color.White,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = (18 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    fontScale: Float,
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
                onLongClick = { IntentUtils.copyToClipboard(context, label, value) }
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = Color(0xFFF5F7F8),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF37474F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = (12 * fontScale).sp, color = Color.Gray)
            Text(
                text = value,
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (secondaryActionIcon != null && onSecondaryAction != null) {
                IconButton(onClick = onSecondaryAction) {
                    Icon(
                        painter = secondaryActionIcon,
                        contentDescription = "WhatsApp",
                        tint = Color.Unspecified, 
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            if (onSmsAction != null) {
                IconButton(onClick = onSmsAction) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS",
                        tint = Color(0xFF546E7A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            if (onAction != null && icon != Icons.Default.Call) {
                 IconButton(onClick = onAction) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Action",
                        tint = Color(0xFF546E7A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else if (onAction != null && icon == Icons.Default.Call) {
                IconButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, top = 12.dp, bottom = 12.dp),
        thickness = 0.5.dp,
        color = Color.LightGray.copy(alpha = 0.3f)
    )
}
