package com.example.policemobiledirectory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.ui.theme.ChipSelectedEnd
import com.example.policemobiledirectory.ui.theme.ChipSelectedStart
import com.example.policemobiledirectory.ui.theme.ChipUnselected
import com.example.policemobiledirectory.ui.theme.PrimaryTeal

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
    
    // Search Filter Type
    searchFilter: SearchFilter,
    onSearchFilterChange: (SearchFilter) -> Unit,
    
    // Config
    isDistrictLevelUnit: Boolean,
    isAdmin: Boolean,
    districtLabel: String = "District / HQ",
    stationLabel: String = "Station / Section",
    totalContactsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // UI State for Dropdowns (Internal)
    var unitExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }

    // 🔹 COLLAPSIBLE STATE
    var filtersVisible by remember { mutableStateOf(false) }

    val searchFields = SearchFilter.values().toList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 🔹 SEARCH BAR & TOGGLE ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val searchLabel = when (searchFilter) {
                SearchFilter.ALL -> "Power Search"
                SearchFilter.RANK -> "Rank"
                SearchFilter.NAME -> "Name"
                SearchFilter.BLOOD_GROUP -> "Blood"
                else -> searchFilter.name.lowercase().replaceFirstChar { it.uppercase() }
            }

            val placeholderText = if (searchFilter == SearchFilter.ALL) {
                "Search by Name, KGID, Mobile, Rank, Station..."
            } else {
                "Search by $searchLabel"
            }

            val keyboardType = when (searchFilter) {
                SearchFilter.MOBILE, SearchFilter.METAL_NUMBER -> KeyboardType.Number
                else -> KeyboardType.Text
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { 
                    Text(
                        if (totalContactsCount > 0 && searchFilter == SearchFilter.ALL) "Searching from $totalContactsCount contacts..." else placeholderText, 
                        maxLines = 1, 
                        fontSize = 12.sp
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryTeal, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PrimaryTeal, modifier = Modifier.size(18.dp))
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
            }
        }

        // 🔹 FILTER CHIPS
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        ) {
            items(searchFields) { filter ->
                if (filter == SearchFilter.KGID && !isAdmin) return@items
                if (filter == SearchFilter.RANK) return@items

                val selected = searchFilter == filter
                val labelText = when (filter) {
                    SearchFilter.METAL_NUMBER -> "Metal"
                    SearchFilter.KGID -> "KGID"
                    SearchFilter.BLOOD_GROUP -> "Blood"
                    else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
                }

                Box(modifier = Modifier.shadow(elevation = if (selected) 4.dp else 0.dp, shape = RoundedCornerShape(20.dp))) {
                    FilterChip(
                        selected = selected,
                        onClick = { onSearchFilterChange(filter) },
                        enabled = true,
                        label = { Text(labelText, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (selected) Color.Transparent else ChipUnselected,
                            selectedLabelColor = Color.White,
                            containerColor = ChipUnselected,
                            labelColor = if (selected) Color.White else PrimaryTeal
                        ),
                        modifier = if (selected) {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    listOf(ChipSelectedStart, ChipSelectedEnd)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}
