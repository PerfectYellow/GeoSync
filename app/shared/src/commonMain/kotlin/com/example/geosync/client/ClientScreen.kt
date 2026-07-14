package com.example.geosync.client

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.geosync.permissions.PermissionNames
import com.example.geosync.permissions.rememberPermissionState
import com.example.geosync.network.ConnectionStatus
import com.example.geosync.NotificationBanner

@Composable
fun ClientScreen(
    viewModel: ClientViewModel = viewModel { ClientViewModel() },
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val trackingId by viewModel.trackingId.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()

    val locationPermissionState = rememberPermissionState(
        permission = PermissionNames.LOCATION,
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleTracking()
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ClientScreenContent(
            trackingId = trackingId,
            connectionStatus = connectionStatus,
            connectionError = connectionError,
            onToggleTracking = {
                if (locationPermissionState.hasPermission) {
                    viewModel.toggleTracking()
                } else {
                    locationPermissionState.launchPermissionRequest()
                }
            },
            paddingValues = paddingValues
        )
        
        // Floating notification at the top
        NotificationBanner()
    }
}

@Composable
fun ClientScreenContent(
    trackingId: String,
    connectionStatus: ConnectionStatus,
    connectionError: String?,
    onToggleTracking: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val clipboardManager = LocalClipboardManager.current
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Descriptive Header
        Text(
            text = "Client Portal",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Location Synchronization",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isConnected,
                label = "TrackingState"
            ) { connected ->
                if (!connected) {
                    IdleView(
                        onStart = onToggleTracking,
                        isLoading = connectionStatus == ConnectionStatus.CONNECTING
                    )
                } else {
                    TrackingView(
                        trackingId = trackingId,
                        connectionStatus = connectionStatus,
                        onStop = onToggleTracking,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(trackingId))
                        }
                    )
                }
            }
        }

        // Info card at the bottom
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Your data is only visible to authorized administrators.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IdleView(onStart: () -> Unit, isLoading: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Ready to Sync?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Press the button below to start broadcasting your position to the network.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Start Tracking", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TrackingView(
    trackingId: String, 
    connectionStatus: ConnectionStatus,
    onStop: () -> Unit, 
    onCopy: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            val statusColor = Color(0xFF2E7D32)
            
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f)
            ) {}
            
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = statusColor
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "now your location tracking",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AnimatedVisibility(
            visible = connectionStatus == ConnectionStatus.CONNECTED,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { onCopy() }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SESSION UUID",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = trackingId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tap to copy ID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Text("Stop Tracking")
        }
    }
}

@Preview
@Composable
fun ClientScreenPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClientScreenContent(
                trackingId = "123e4567-e89b-12d3-a456-426614174000",
                connectionStatus = ConnectionStatus.IDLE,
                connectionError = null,
                onToggleTracking = {}
            )
        }
    }
}

@Preview
@Composable
fun ClientScreenTrackingPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClientScreenContent(
                trackingId = "123e4567-e89b-12d3-a456-426614174000",
                connectionStatus = ConnectionStatus.CONNECTED,
                connectionError = null,
                onToggleTracking = {}
            )
        }
    }
}

@Preview
@Composable
fun ClientScreenErrorPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClientScreenContent(
                trackingId = "",
                connectionStatus = ConnectionStatus.FAILED,
                connectionError = "Connection refused: Server is unreachable",
                onToggleTracking = {}
            )
        }
    }
}
