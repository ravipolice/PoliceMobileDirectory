package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LeaveRule(
    val id: Int,
    val name: String,
    val description: String,
    val ruleDetails: String
)

private val leaveRulesData = listOf(
    LeaveRule(1, "Earned Leave", "Can be availed on personal or Medical Grounds", "Maximum earned leave grantable at a time is subject to service rules and balance."),
    LeaveRule(2, "Half Pay Leave", "Can be availed on personal or Medical Grounds", "Half pay leave is credited periodically and debited as per admissible conditions."),
    LeaveRule(3, "Commuted Leave", "Can be availed only on Medical Grounds", "Commuted leave is generally granted on medical certificate against available HPL."),
    LeaveRule(4, "Leave Not Due", "Limited and usually medical", "Leave Not Due can be sanctioned under prescribed conditions and service limits."),
    LeaveRule(5, "Extraordinary Leave", "Leave without pay under special cases", "Granted when no other leave is admissible or as specifically approved."),
    LeaveRule(6, "Maternity Leave", "Applicable as per service rules", "Granted to eligible women employees as per the applicable leave rules."),
    LeaveRule(7, "Paternity Leave", "Applicable as per service rules", "Granted to eligible male employees as per policy conditions."),
    LeaveRule(8, "Child Care Leave", "For eligible employees and children", "Granted under policy limits and subject to eligibility and approval."),
    LeaveRule(9, "Leave Salary", "Based on leave type", "Leave salary is governed by the type of leave and the applicable pay rules."),
    LeaveRule(10, "Combination of Leave", "Different leave types can be combined", "Combination is allowed unless specifically restricted by rule.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRulesScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Leave Rules & Provisions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(leaveRulesData) { rule ->
                LeaveRuleItem(rule = rule)
            }
        }
    }
}

@Composable
private fun LeaveRuleItem(rule: LeaveRule) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${rule.id}. ${rule.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rule.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = rule.ruleDetails,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
