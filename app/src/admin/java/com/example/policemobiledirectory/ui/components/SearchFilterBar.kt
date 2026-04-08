package com.example.policemobiledirectory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.example.policemobiledirectory.ui.theme.PrimaryTeal
import com.example.policemobiledirectory.utils.OperationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterBar(
    // Data Sources
    units: List<String>,
    districts: List<String>,
    stations: List<String>,
    ranks: List<String>,
    
    // Selected Values
    selectedUnit: String,
    selectedDistrict: String,
    selectedStation: String,
    selectedRank: String,
    
    // Callbacks
    onUnitChange: (String) -> Unit,
    onDistrictChange: (String) -> Unit,
    onStationChange: (String) -> Unit,
    onRankChange: (String) -> Unit,
    
    // Search Query
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAISearch: (String) -> Unit = {},
    aiStatus: OperationStatus<String> = OperationStatus.Idle,

    // Config
    isDistrictLevelUnit: Boolean,
    isAdmin: Boolean,
    districtLabel: String = "District / HQ",
    stationLabel: String = "Station / Section",
    totalContactsCount: Int = 0,
    showHidden: Boolean = false,
    onShowHiddenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // UI State for Dropdowns (Internal)
    var unitExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }

    // 🔹 COLLAPSIBLE STATE
    var filtersVisible by remember { mutableStateOf(false) }


    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 🔹 SEARCH BAR & TOGGLE ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val placeholderText = "Search by Name, KGID, Mobile, Rank, Station..."
            val keyboardType = KeyboardType.Text

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { 
                    Text(
                        if (totalContactsCount > 0) "Searching from $totalContactsCount contacts..." else placeholderText, 
                        maxLines = 1, 
                        fontSize = 12.sp
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryTeal, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                            }
                        }

                        // 🔹 AI SEARCH BUTTON
                        IconButton(
                            onClick = { onAISearch(searchQuery) },
                            enabled = searchQuery.isNotBlank() && aiStatus !is OperationStatus.Loading
                        ) {
                            when (aiStatus) {
                                is OperationStatus.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryTeal
                                    )
                                }
                                is OperationStatus.Success -> {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Success",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                is OperationStatus.Error -> {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Search",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.width(8.dp))

            // 🔹 FILTER TOGGLE BUTTON
            IconButton(
                onClick = { filtersVisible = !filtersVisible },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (filtersVisible) PrimaryTeal else Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (filtersVisible) PrimaryTeal else Color.LightGray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = if (filtersVisible) Icons.Default.FilterAltOff else Icons.Default.FilterList,
                    contentDescription = "Toggle Filters",
                    tint = if (filtersVisible) Color.White else PrimaryTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 🔹 COLLAPSIBLE FILTERS SECTION
        androidx.compose.animation.AnimatedVisibility(
            visible = filtersVisible,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 🔹 ROW 1: UNIT & DISTRICT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // UNIT Dropdown
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .zIndex(10f)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = !unitExpanded },
                        ) {
                            OutlinedTextField(
                                value = selectedUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Business, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true,
                                maxLines = 1,
                                shape = RoundedCornerShape(15.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryTeal,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                    focusedLabelColor = PrimaryTeal,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                units.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit) },
                                        onClick = {
                                            onUnitChange(unit)
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // DISTRICT Dropdown
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .zIndex(10f)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = districtExpanded,
                            onExpandedChange = { districtExpanded = !districtExpanded },
                        ) {
                            OutlinedTextField(
                                value = selectedDistrict,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(districtLabel, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Map, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true,
                                maxLines = 1,
                                shape = RoundedCornerShape(15.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryTeal,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                    focusedLabelColor = PrimaryTeal,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = districtExpanded,
                                onDismissRequest = { districtExpanded = false }
                            ) {
                                districts.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text(district) },
                                        onClick = {
                                            onDistrictChange(district)
                                            districtExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 🔹 ROW 2: STATION & RANK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isDistrictLevelUnit) {
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .zIndex(9f)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = stationExpanded,
                                onExpandedChange = {
                                    if (selectedDistrict != "All" || stations.isNotEmpty()) stationExpanded = !stationExpanded
                                },
                            ) {
                                OutlinedTextField(
                                    value = selectedStation,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stationLabel, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Security, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = selectedDistrict != "All" || stations.isNotEmpty(),
                                    singleLine = true,
                                    maxLines = 1,
                                    shape = RoundedCornerShape(15.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryTeal,
                                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                        focusedLabelColor = PrimaryTeal,
                                        unfocusedLabelColor = Color.Gray
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = stationExpanded,
                                    onDismissRequest = { stationExpanded = false }
                                ) {
                                    stations.forEach { station ->
                                        DropdownMenuItem(
                                            text = { Text(station) },
                                            onClick = {
                                                onStationChange(station)
                                                stationExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(if (isDistrictLevelUnit) 1f else 0.4f)
                            .zIndex(9f)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = rankExpanded,
                            onExpandedChange = { rankExpanded = !rankExpanded },
                        ) {
                            OutlinedTextField(
                                value = selectedRank,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Rank", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.MilitaryTech, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true,
                                maxLines = 1,
                                shape = RoundedCornerShape(15.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryTeal,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                    focusedLabelColor = PrimaryTeal,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = rankExpanded,
                                onDismissRequest = { rankExpanded = false }
                            ) {
                                ranks.forEach { rank ->
                                    DropdownMenuItem(
                                        text = { Text(rank) },
                                        onClick = {
                                            onRankChange(rank)
                                            rankExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 🔹 ROW 3: HIDDEN CONTACTS TOGGLE
                if (isAdmin) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (showHidden) Color.Gray else PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Show Hidden Contacts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = showHidden,
                            onCheckedChange = onShowHiddenChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (showHidden) Color(0xFF2196F3) else PrimaryTeal,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    }
}
