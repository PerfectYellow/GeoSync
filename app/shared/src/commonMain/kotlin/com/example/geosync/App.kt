package com.example.geosync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.geosync.admin.AdminScreen
import com.example.geosync.client.ClientScreen

@Composable
fun AppContent(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    var isMapExpanded by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!isMapExpanded) {
                // Ultra-compact Floating Bottom Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 64.dp, vertical = 12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        tonalElevation = 3.dp,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TabItem(
                                selected = selectedTab == 0,
                                onClick = { onTabSelected(0) },
                                icon = Icons.Default.Settings,
                                label = "Admin",
                                modifier = Modifier.weight(1f)
                            )
                            TabItem(
                                selected = selectedTab == 1,
                                onClick = { onTabSelected(1) },
                                icon = Icons.Default.Person,
                                label = "Client",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> AdminScreen(
                    paddingValues = paddingValues,
                    onMapToggle = { isMapExpanded = it }
                )
                1 -> ClientScreen(paddingValues = paddingValues)
            }
            
            NotificationBanner()
        }
    }
}

@Composable
private fun TabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.fillMaxHeight()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null,
                modifier = Modifier.size(24.dp) // Keeps icon at standard size
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }
        AppContent(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

@Preview
@Composable
fun AppClientTabPreview() {
    MaterialTheme {
        AppContent(
            selectedTab = 1,
            onTabSelected = {}
        )
    }
}