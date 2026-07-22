package com.example.geosync.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geosync.NotificationManager
import com.example.geosync.LanguageSelector
import com.example.geosync.localization.LocalStrings
import com.example.geosync.network.ConnectivityStatus
import com.example.geosync.network.StoredLocation
import com.example.geosync.network.rememberConnectivityObserver

@Composable
fun AdminScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onMapToggle: (Boolean) -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val viewModel: AdminViewModel = viewModel { AdminViewModel(isPreview) }
    val isConnected by viewModel.isConnected.collectAsState()
    val trackedClientIds by viewModel.trackedClientIds.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val mapMode by viewModel.mapMode.collectAsState()
    val clientIdInput by viewModel.clientIdInput.collectAsState()
    val isListExpanded by viewModel.isListExpanded.collectAsState()
    val isMapExpanded by viewModel.isMapExpanded.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()

    val connectivityObserver = rememberConnectivityObserver()
    val networkStatus by connectivityObserver.observe().collectAsState(ConnectivityStatus.Online)

    LaunchedEffect(connectivityObserver) {
        connectivityObserver.observe().collect { status ->
            val isOffline = status == ConnectivityStatus.Offline
            if (isOffline) {
                NotificationManager.showOffline()
            } else {
                NotificationManager.dismissOffline()
            }
            viewModel.handleNetworkChange(isOffline)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdminContent(
            isConnected = isConnected,
            networkStatus = networkStatus,
            mapMode = mapMode,
            onMapModeChange = { viewModel.setMapMode(it, networkStatus == ConnectivityStatus.Offline) },
            trackedClientIds = trackedClientIds,
            locations = locations,
            clientIdInput = clientIdInput,
            onClientIdInputChange = { viewModel.setClientIdInput(it) },
            isListExpanded = isListExpanded,
            onListExpandedChange = { viewModel.setListExpanded(it) },
            isMapExpanded = isMapExpanded,
            onMapExpandedChange = { viewModel.setMapExpanded(it) },
            cameraState = cameraState,
            onCameraChanged = { viewModel.updateCameraState(it) },
            onRetryConnection = { viewModel.retryConnection() },
            onAddClient = { viewModel.addClient(it) },
            onRemoveClient = { viewModel.removeClient(it) },
            onMapToggle = { 
                viewModel.setMapExpanded(it)
                onMapToggle(it) 
            }
        )
    }
}

@Composable
fun AdminContent(
    isConnected: Boolean,
    networkStatus: ConnectivityStatus,
    mapMode: MapMode,
    onMapModeChange: (MapMode) -> Unit,
    trackedClientIds: Set<String>,
    locations: Map<String, StoredLocation>,
    clientIdInput: String,
    onClientIdInputChange: (String) -> Unit,
    isListExpanded: Boolean,
    onListExpandedChange: (Boolean) -> Unit,
    isMapExpanded: Boolean,
    onMapExpandedChange: (Boolean) -> Unit,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit,
    onRetryConnection: () -> Unit = {},
    onAddClient: (String) -> Unit = {},
    onRemoveClient: (String) -> Unit = {},
    onMapToggle: (Boolean) -> Unit = {}
) {
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var focusTrigger by remember { mutableStateOf(0L) }
    val strings = LocalStrings.current
    
    val isInputValid = remember(clientIdInput) {
        val trimmed = clientIdInput.trim()
        trimmed.isEmpty() || 
        trimmed.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()) ||
        (trimmed.startsWith("@") && trimmed.length >= 3)
    }

    val filteredLocations = remember(locations, trackedClientIds) {
        locations.filterKeys { it in trackedClientIds }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 1. Map in the background
        MapPreview(
            locations = filteredLocations,
            mapMode = mapMode,
            selectedClientId = selectedClientId,
            focusTrigger = focusTrigger,
            cameraState = cameraState,
            onCameraChanged = onCameraChanged,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Controls in the foreground
        if (!isMapExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                // Main Header Card - STABLE SHADOW
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(24.dp)
                ) {
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
                                    text = strings.adminPortal,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                ConnectionStatus(isConnected, networkStatus, onRetryConnection)
                            }

                            MapModeSelector(
                                currentMode = mapMode,
                                isOffline = networkStatus == ConnectivityStatus.Offline,
                                onModeSelected = onMapModeChange
                            )
                            LanguageSelector()
                            
                            IconButton(
                                onClick = { 
                                    onMapExpandedChange(true)
                                    onMapToggle(true)
                                },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = strings.expandMap,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = clientIdInput,
                            onValueChange = onClientIdInputChange,
                            label = { Text(strings.clientIdToTrack) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(strings.enterClientId) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (clientIdInput.isNotEmpty()) {
                                            onAddClient(clientIdInput)
                                            if (isInputValid) onClientIdInputChange("")
                                        }
                                    },
                                    enabled = clientIdInput.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = strings.addClient)
                                }
                            },
                            isError = !isInputValid,
                            singleLine = true,
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    if (clientIdInput.isNotEmpty()) {
                                        onAddClient(clientIdInput)
                                        if (isInputValid) onClientIdInputChange("")
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
                                    text = strings.invalidUuidFormat,
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
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onListExpandedChange(!isListExpanded) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.trackedClients(trackedClientIds.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (isListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }
                }

                // Separated List - Smoothly expands without moving the main card's shadow
                AnimatedVisibility(
                    visible = isListExpanded,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f), // More opaque
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        if (trackedClientIds.isEmpty()) {
                            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(strings.noClientsAdded, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn {
                                items(trackedClientIds.toList()) { id ->
                                    ClientListItem(
                                        id = id,
                                        location = locations[id],
                                        isAdminConnected = isConnected,
                                        networkStatus = networkStatus,
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
        } else {
            // Restore UI Button (Floating)
            IconButton(
                onClick = { 
                    onMapExpandedChange(false)
                    onMapToggle(false)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.collapseMap,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ConnectionStatus(isConnected: Boolean, networkStatus: ConnectivityStatus, onRetry: () -> Unit) {
    val isOffline = networkStatus == ConnectivityStatus.Offline
    val strings = LocalStrings.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = !isConnected && !isOffline) { onRetry() }
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = when {
                        isConnected -> Color.Green
                        isOffline -> Color.Gray
                        else -> Color.Red
                    }, 
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = when {
                isConnected -> strings.connectedToServer
                isOffline -> strings.youAreOffline
                else -> strings.offlineTapToRetry
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isConnected) Color.Unspecified else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun ClientListItem(
    id: String, 
    location: StoredLocation?, 
    isAdminConnected: Boolean, 
    networkStatus: ConnectivityStatus,
    onRemove: () -> Unit, 
    onClick: () -> Unit
) {
    val isOnline = location?.isOnline ?: false
    val shortId = if (id.length > 13) "${id.take(6)}...${id.takeLast(4)}" else id
    val isDeviceOffline = networkStatus == ConnectivityStatus.Offline
    val strings = LocalStrings.current
    val clientColor = AdminUtils.getClientColor(id)
    
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when {
                            isDeviceOffline -> MaterialTheme.colorScheme.surfaceVariant
                            !isAdminConnected -> MaterialTheme.colorScheme.surfaceVariant
                            location == null -> MaterialTheme.colorScheme.surfaceVariant
                            else -> clientColor.copy(alpha = 0.15f) // Subtle background of client's color
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isDeviceOffline -> Icons.Default.Warning
                        !isAdminConnected -> Icons.Default.Warning
                        isOnline -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when {
                        isDeviceOffline -> Color.Gray
                        !isAdminConnected -> Color.Gray
                        location == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else -> clientColor // Primary client color for the icon
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
                    Surface(
                        color = when {
                            isDeviceOffline -> Color.Gray
                            !isAdminConnected -> Color.Gray
                            location == null -> Color.LightGray
                            isOnline -> Color(0xFF2E7D32)
                            else -> Color.Gray
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when {
                                isDeviceOffline -> strings.statusOffline
                                !isAdminConnected -> strings.statusDisconnected
                                location == null -> strings.statusWaiting
                                isOnline -> strings.statusLive
                                else -> strings.statusOffline
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
                        isDeviceOffline -> strings.youAreOffline
                        !isAdminConnected -> strings.adminConnectionLost
                        location != null -> {
                            val lat = location.latitude.toString().take(8)
                            val lng = location.longitude.toString().take(8)
                            val status = if (isOnline) strings.statusLive else strings.lastSeen
                            "$status: $lat, $lng"
                        }
                        else -> strings.waitingForLocationSync
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!isAdminConnected || isDeviceOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = strings.remove,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MapPreview(
    locations: Map<String, StoredLocation>,
    mapMode: MapMode,
    selectedClientId: String? = null,
    focusTrigger: Long = 0L,
    cameraState: MapCameraState,
    onCameraChanged: (MapCameraState) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    
    Box(modifier = modifier.fillMaxSize()) {
        if (!LocalInspectionMode.current) {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                locations = locations,
                mapMode = mapMode,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                defaultLatitude = cameraState.latitude,
                defaultLongitude = cameraState.longitude,
                cameraState = cameraState,
                onCameraChanged = onCameraChanged
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.mapPreview, style = MaterialTheme.typography.labelSmall)
                }
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
                networkStatus = ConnectivityStatus.Online,
                mapMode = MapMode.OPEN_STREET,
                onMapModeChange = {},
                trackedClientIds = setOf("Client-1", "Client-2"),
                locations = mapOf(
                    "Client-1" to StoredLocation("Client-1", 37.7749, -122.4194, isOnline = true),
                    "Client-2" to StoredLocation("Client-2", 34.0522, -118.2437, isOnline = false)
                ),
                clientIdInput = "",
                onClientIdInputChange = {},
                isListExpanded = false,
                onListExpandedChange = {},
                isMapExpanded = false,
                onMapExpandedChange = {},
                cameraState = MapCameraState(35.6994, 51.3377, 14.0),
                onCameraChanged = {}
            )
        }
    }
}
