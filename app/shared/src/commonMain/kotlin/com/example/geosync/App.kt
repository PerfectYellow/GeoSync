package com.example.geosync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.geosync.localization.LocalStrings
import com.example.geosync.localization.EnStrings
import com.example.geosync.localization.LocalizationManager
import com.example.geosync.admin.AdminScreen
import com.example.geosync.client.ClientScreen
import com.example.geosync.network.ConnectivityStatus
import com.example.geosync.network.rememberConnectivityObserver

@Composable
fun App() {
    val strings = LocalizationManager.strings
    val layoutDirection = LocalizationManager.layoutDirection

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme {
            var selectedTab by remember { mutableStateOf(0) }
            AppContent(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    var isMapExpanded by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    val connectivityObserver = rememberConnectivityObserver()
    LaunchedEffect(connectivityObserver) {
        connectivityObserver.observe().collect { status ->
            if (status == ConnectivityStatus.Offline) {
                NotificationManager.showOffline()
            } else {
                NotificationManager.dismissOffline()
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!isMapExpanded) {
                // Ultra-compact Floating Bottom Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 64.dp, vertical = 12.dp)
                        .zIndex(100f) // Ensure it's above both layered screens
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
                                label = strings.admin,
                                modifier = Modifier.weight(1f)
                            )
                            TabItem(
                                selected = selectedTab == 1,
                                onClick = { onTabSelected(1) },
                                icon = Icons.Default.Person,
                                label = strings.client,
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
            // Layered screens to preserve native map state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (selectedTab == 0) 1f else 0f)
            ) {
                // We wrap it in a child Box to disable interactions when inactive
                if (selectedTab == 0) {
                    AdminScreen(
                        paddingValues = paddingValues,
                        onMapToggle = { isMapExpanded = it }
                    )
                } else {
                    // When not selected, we still keep the AdminScreen in the tree 
                    // but we don't pass interactions. This preserves the MapView state.
                    Box(Modifier.fillMaxSize()) {
                        AdminScreen(
                            paddingValues = paddingValues,
                            onMapToggle = { isMapExpanded = it }
                        )
                        // Invisible overlay to block touches when in background
                        Box(Modifier.fillMaxSize().clickable(enabled = false) {})
                    }
                }
            }

            if (selectedTab == 1) {
                ClientScreen(paddingValues = paddingValues)
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
@Preview(name = "Admin Tab")
fun AdminTabPreview() {
    CompositionLocalProvider(
        LocalStrings provides EnStrings,
        LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        MaterialTheme {
            AppContent(
                selectedTab = 0,
                onTabSelected = {}
            )
        }
    }
}

@Composable
@Preview(name = "Client Tab")
fun ClientTabPreview() {
    CompositionLocalProvider(
        LocalStrings provides EnStrings,
        LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        MaterialTheme {
            AppContent(
                selectedTab = 1,
                onTabSelected = {}
            )
        }
    }
}
