package com.example.geosync.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geosync.network.StoredLocation

@Composable
fun AdminScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onMapToggle: (Boolean) -> Unit = {}
) {
    val viewModel: AdminViewModel = viewModel { AdminViewModel() }
    val isConnected by viewModel.isConnected.collectAsState()
    val trackedClientIds by viewModel.trackedClientIds.collectAsState()
    val locations by viewModel.locations.collectAsState()

    AdminContent(
        isConnected = isConnected,
        trackedClientIds = trackedClientIds,
        locations = locations,
        paddingValues = paddingValues,
        onRetryConnection = { viewModel.retryConnection() },
        onAddClient = { viewModel.addClient(it) },
        onRemoveClient = { viewModel.removeClient(it) },
        onMapToggle = onMapToggle
    )
}

@Composable
fun AdminContent(
    isConnected: Boolean,
    trackedClientIds: Set<String>,
    locations: Map<String, StoredLocation>,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onRetryConnection: () -> Unit = {},
    onAddClient: (String) -> Unit = {},
    onRemoveClient: (String) -> Unit = {},
    onMapToggle: (Boolean) -> Unit = {}
) {
    var clientIdInput by remember { mutableStateOf("") }
    var isMapExpanded by remember { mutableStateOf(false) }
    var isListExpanded by remember { mutableStateOf(false) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var focusTrigger by remember { mutableStateOf(0L) }
    
    val isInputValid = remember(clientIdInput) {
        clientIdInput.isEmpty() || clientIdInput.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isMapExpanded) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Portal",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ConnectionStatus(isConnected, onRetryConnection)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text("Client ID to Track") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter client ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (clientIdInput.isNotEmpty()) {
                                    onAddClient(clientIdInput)
                                    if (isInputValid) clientIdInput = ""
                                }
                            },
                            enabled = clientIdInput.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Client")
                        }
                    },
                    isError = !isInputValid,
                    singleLine = true,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            if (clientIdInput.isNotEmpty()) {
                                onAddClient(clientIdInput)
                                if (isInputValid) clientIdInput = ""
                            }
                        }
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                AnimatedVisibility(visible = !isInputValid) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Invalid UUID format (e.g. 123e4567-e89b...)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tracked Clients List Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isListExpanded = !isListExpanded }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tracked Clients (${trackedClientIds.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = isListExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        if (trackedClientIds.isEmpty()) {
                            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No clients added yet", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn {
                                items(trackedClientIds.toList()) { id ->
                                    ClientListItem(
                                        id = id,
                                        location = locations[id],
                                        isAdminConnected = isConnected,
                                        onRemove = { onRemoveClient(id) },
                                        onClick = { 
                                            selectedClientId = id
                                            focusTrigger++
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        MapPreview(
            isExpanded = isMapExpanded,
            onToggleExpand = { 
                isMapExpanded = !isMapExpanded
                onMapToggle(isMapExpanded)
            },
            locations = locations,
            selectedClientId = selectedClientId,
            focusTrigger = focusTrigger,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (!isMapExpanded) {
                        Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}

@Composable
fun ConnectionStatus(isConnected: Boolean, onRetry: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = !isConnected) { onRetry() }
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (isConnected) Color.Green else Color.Red, MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isConnected) "Connected to Server" else "Offline - Tap to Retry",
            style = MaterialTheme.typography.labelSmall,
            color = if (isConnected) Color.Unspecified else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun ClientListItem(id: String, location: StoredLocation?, isAdminConnected: Boolean, onRemove: () -> Unit, onClick: () -> Unit) {
    val isOnline = location?.isOnline ?: false
    val shortId = if (id.length > 13) "${id.take(6)}...${id.takeLast(4)}" else id
    
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon with background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when {
                            !isAdminConnected -> MaterialTheme.colorScheme.surfaceVariant
                            location == null -> MaterialTheme.colorScheme.surfaceVariant
                            isOnline -> Color(0xFFE8F5E9)
                            else -> Color(0xFFF5F5F5)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !isAdminConnected -> Icons.Default.Warning
                        isOnline -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when {
                        !isAdminConnected -> Color.Gray
                        location == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        isOnline -> Color(0xFF2E7D32)
                        else -> Color.Gray
                    }
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shortId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    // Status Badge
                    Surface(
                        color = when {
                            !isAdminConnected -> Color.Gray
                            location == null -> Color.LightGray
                            isOnline -> Color(0xFF2E7D32)
                            else -> Color.Gray
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when {
                                !isAdminConnected -> "DISCONNECTED"
                                location == null -> "WAITING"
                                isOnline -> "LIVE"
                                else -> "OFFLINE"
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                Text(
                    text = when {
                        !isAdminConnected -> "Admin connection lost"
                        location != null -> {
                            val lat = location.latitude.toString().take(8)
                            val lng = location.longitude.toString().take(8)
                            val status = if (isOnline) "Live" else "Last seen"
                            "$status: $lat, $lng"
                        }
                        else -> "Waiting for location sync..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!isAdminConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MapPreview(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    locations: Map<String, StoredLocation>,
    selectedClientId: String? = null,
    focusTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = if (isExpanded) RectangleShape else MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 0.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!LocalInspectionMode.current) {
                GoogleMapView(
                    modifier = Modifier.fillMaxSize(),
                    locations = locations,
                    selectedClientId = selectedClientId,
                    focusTrigger = focusTrigger
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Map Preview", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isExpanded) "Collapse Map" else "Expand Map"
                )
            }
        }
    }
}

@Preview
@Composable
fun AdminScreenPreview() {
    MaterialTheme {
        Surface {
            AdminContent(
                isConnected = true,
                trackedClientIds = setOf("Client-1", "Client-2"),
                locations = mapOf(
                    "Client-1" to StoredLocation("Client-1", 37.7749, -122.4194, isOnline = true),
                    "Client-2" to StoredLocation("Client-2", 34.0522, -118.2437, isOnline = false)
                )
            )
        }
    }
}
