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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.getContactDisplayName
import com.example.policemobiledirectory.viewmodel.EmployeeListViewModel
import com.example.policemobiledirectory.viewmodel.SettingsViewModel

@Composable
fun EmployeeDetailScreen(
    id: String,
    isOfficer: Boolean,
    navController: NavController,
    viewModel: EmployeeListViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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

    // Instant O(1) lookup from already-loaded in-memory lists — no spinner delay
    val contact = remember(id, isOfficer, employeeStatus, officerStatus) {
        viewModel.findContactById(id, isOfficer)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Contact Details", 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = (18 * fontScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (contact == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Contact not found", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        } else {
            val name = getContactDisplayName(contact as? Employee, contact as? Officer)
            val rank = if (contact is Employee) contact.displayRank else (contact as? Officer)?.rank?.replace(".", "")?.replace("(?i)\\bDy SP\\b".toRegex(), "DySP")?.trim() ?: ""
            val photoUrlVal = if (contact is Employee) (contact.photoUrl ?: contact.photoUrlFromGoogle) else (contact as? Officer)?.photoUrl
            val placeholderRes = if (contact is Employee) R.drawable.officer else R.drawable.ic_officer_building
            
            val mobile = if (contact is Employee) contact.mobile1 else (contact as? Officer)?.mobile
            val landline = if (contact is Employee) contact.landline else (contact as? Officer)?.landline
            val email = if (contact is Employee) contact.email else (contact as? Officer)?.email
            val unit = if (contact is Employee) contact.unit else (contact as? Officer)?.unit
            val station = if (contact is Employee) contact.station else (contact as? Officer)?.station
            val district = if (contact is Employee) contact.district else (contact as? Officer)?.district

            val visible = true // Show content immediately — no artificial delay

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background)
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
                            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { 20 })
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
                            enter = fadeIn(animationSpec = tween(250)) + slideInVertically(initialOffsetY = { 20 })
                        ) {
                            val metalNumber = if (contact is Employee) contact.metalNumber else null
                            val gender = if (contact is Employee) contact.gender else "N/A"

                            // Filter valid details to only render rows containing actual data
                            val hasMetal = !metalNumber.isNullOrBlank()
                            val hasGender = gender != "N/A"
                            val hasMobile = isValidContactInfo(mobile)
                            val hasLandline = isValidContactInfo(landline)
                            val hasEmail = isValidContactInfo(email)

                            if (hasMetal || hasGender || hasMobile || hasLandline || hasEmail) {
                                DetailSection(title = "Contact info", fontScale = fontScale) {
                                    if (hasMetal) {
                                        InfoRow(
                                            label = "Metal Number",
                                            value = metalNumber!!,
                                            icon = Icons.Default.Badge,
                                            fontScale = fontScale,
                                            onAction = { IntentUtils.copyToClipboard(context, "Metal Number", metalNumber) }
                                        )
                                        if (hasGender || hasMobile || hasLandline || hasEmail) CustomDivider()
                                    }

                                    if (hasGender) {
                                        val genderIcon = if (gender.contains("Female", ignoreCase = true)) Icons.Default.Woman else Icons.Default.Man
                                        InfoRow(
                                            label = "Gender",
                                            value = gender,
                                            icon = genderIcon,
                                            fontScale = fontScale
                                        )
                                        if (hasMobile || hasLandline || hasEmail) CustomDivider()
                                    }

                                    if (hasMobile) {
                                        InfoRow(
                                            label = "Mobile",
                                            value = mobile!!,
                                            icon = Icons.Default.Call,
                                            fontScale = fontScale,
                                            onAction = { IntentUtils.dial(context, mobile) },
                                            secondaryActionIcon = painterResource(R.drawable.ic_whatsapp),
                                            onSecondaryAction = { IntentUtils.openWhatsApp(context, mobile) },
                                            onSmsAction = { IntentUtils.sendSms(context, mobile) }
                                        )
                                        if (hasLandline || hasEmail) CustomDivider()
                                    }

                                    if (hasLandline) {
                                        InfoRow(
                                            label = "Landline",
                                            value = landline!!,
                                            icon = Icons.Default.Phone,
                                            fontScale = fontScale,
                                            onAction = { IntentUtils.dial(context, landline) }
                                        )
                                        if (hasEmail) CustomDivider()
                                    }

                                    if (hasEmail) {
                                        InfoRow(
                                            label = "Email",
                                            value = email!!,
                                            icon = Icons.Default.Email,
                                            fontScale = fontScale,
                                            onAction = { IntentUtils.sendEmail(context, email) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (contact is Employee) {
                        val hasPhysicalOrSocial = !contact.height.isNullOrBlank() || 
                                                 !contact.weight.isNullOrBlank() || 
                                                 !contact.caste.isNullOrBlank() || 
                                                 !contact.subCaste.isNullOrBlank()
                        if (hasPhysicalOrSocial) {
                            item {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(animationSpec = tween(270)) + slideInVertically(initialOffsetY = { 20 })
                                ) {
                                    DetailSection(title = "Physical & Social Info", fontScale = fontScale) {
                                        var needsDivider = false
                                        if (!contact.height.isNullOrBlank()) {
                                            InfoRow(
                                                label = "Height",
                                                value = contact.height,
                                                icon = Icons.Default.Person,
                                                fontScale = fontScale
                                            )
                                            needsDivider = true
                                        }
                                        if (!contact.weight.isNullOrBlank()) {
                                            if (needsDivider) CustomDivider()
                                            InfoRow(
                                                label = "Weight",
                                                value = contact.weight,
                                                icon = Icons.Default.Person,
                                                fontScale = fontScale
                                            )
                                            needsDivider = true
                                        }
                                        if (!contact.caste.isNullOrBlank()) {
                                            if (needsDivider) CustomDivider()
                                            InfoRow(
                                                label = "Caste",
                                                value = contact.caste,
                                                icon = Icons.Default.Info,
                                                fontScale = fontScale
                                            )
                                            needsDivider = true
                                        }
                                        if (!contact.subCaste.isNullOrBlank()) {
                                            if (needsDivider) CustomDivider()
                                            InfoRow(
                                                label = "Sub-Caste",
                                                value = contact.subCaste,
                                                icon = Icons.Default.Info,
                                                fontScale = fontScale
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!contact.familyDetails.isNullOrBlank()) {
                            item {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(animationSpec = tween(290)) + slideInVertically(initialOffsetY = { 20 })
                                ) {
                                    DetailSection(title = "Family Details", fontScale = fontScale) {
                                        InfoRow(
                                            label = "Family Info",
                                            value = contact.familyDetails,
                                            icon = Icons.Default.Home,
                                            fontScale = fontScale
                                        )
                                    }
                                }
                            }
                        }

                        if (!contact.educationDetails.isNullOrBlank()) {
                            item {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(animationSpec = tween(310)) + slideInVertically(initialOffsetY = { 20 })
                                ) {
                                    DetailSection(title = "Education Details", fontScale = fontScale) {
                                        InfoRow(
                                            label = "Education Info",
                                            value = contact.educationDetails,
                                            icon = Icons.Default.Info,
                                            fontScale = fontScale
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
}

/**
 * Validates raw contact details to filter out empty/placeholder fields
 */
private fun isValidContactInfo(value: String?): Boolean {
    if (value.isNullOrBlank()) return false
    val trimmed = value.trim()
    return trimmed != "-" && trimmed != "—" && trimmed != "— / —" && trimmed != "N/A" && trimmed != "null"
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
                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(placeholderRes),
                        error = painterResource(placeholderRes)
                    )
                } else {
                    // Premium custom placeholder with dynamic gradients
                    val avatarColors = listOf(
                        listOf(Color(0xFF1E3C72), Color(0xFF2A5298)), // Navy Gradient
                        listOf(Color(0xFF0F9D58), Color(0xFF11998E)), // Green Gradient
                        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), // Purple Gradient
                        listOf(Color(0xFFD32F2F), Color(0xFF9A0007)), // Red Gradient
                        listOf(Color(0xFF00838F), Color(0xFF006064)), // Cyan Gradient
                        listOf(Color(0xFFE65100), Color(0xFFF57C00)), // Orange Gradient
                        listOf(Color(0xFF00796B), Color(0xFF004D40))  // Teal Gradient
                    )
                    val gradient = remember(name) {
                        val index = Math.abs(name.hashCode() % avatarColors.size)
                        avatarColors[index]
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (placeholderRes == R.drawable.ic_officer_building) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                        } else {
                            val initial = name.takeIf { it.isNotBlank() }?.first()?.uppercase() ?: "?"
                            Text(
                                text = initial.toString(),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
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
            fontSize = (26 * fontScale).sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            lineHeight = (32 * fontScale).sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = rank,
            fontSize = (18 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                fontSize = (16 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        // Subtitle Line 2: District - Range
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
            Text(
                text = line2,
                fontSize = (14 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = (18 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label, 
                fontSize = (12 * fontScale).sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                fontSize = if (label.equals("Email", ignoreCase = true)) (14 * fontScale).sp else (16 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            if (onAction != null && (icon == Icons.Default.Call || icon == Icons.Default.Phone)) {
                IconButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (onAction != null && !label.equals("Email", ignoreCase = true)) {
                 IconButton(onClick = onAction) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Action",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}
