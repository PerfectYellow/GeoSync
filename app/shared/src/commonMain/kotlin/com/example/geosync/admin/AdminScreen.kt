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
    val viewModel: AdminViewModel = viewModel { AdminViewModel() }
    val isConnected by viewModel.isConnected.collectAsState()
    val trackedClientIds by viewModel.trackedClientIds.collectAsState()
    val locations by viewModel.locations.collectAsState()

    val connectivityObserver = rememberConnectivityObserver()
    val networkStatus by connectivityObserver.observe().collectAsState(ConnectivityStatus.Online)

    LaunchedEffect(connectivityObserver) {
        connectivityObserver.observe().collect { status ->
            if (status == ConnectivityStatus.Offline) {
                NotificationManager.showOffline()
            } else {
                NotificationManager.dismissOffline()
            }
        }
    }

    AdminContent(
        isConnected = isConnected,
        networkStatus = networkStatus,
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
    networkStatus: ConnectivityStatus,
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
    val strings = LocalStrings.current
    
    val isInputValid = remember(clientIdInput) {
        clientIdInput.isEmpty() || clientIdInput.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = paddingValues.calculateTopPadding()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isMapExpanded) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.adminPortal,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        LanguageSelector()
                    }
                    ConnectionStatus(isConnected, networkStatus, onRetryConnection)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text(strings.clientIdToTrack) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.enterClientId) },
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
                            Icon(Icons.Default.Add, contentDescription = strings.addClient)
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
                        .clickable { isListExpanded = !isListExpanded }
                        .padding(vertical = 8.dp),
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

                AnimatedVisibility(visible = isListExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                    shape = MaterialTheme.shapes.extraSmall
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
                            isDeviceOffline -> MaterialTheme.colorScheme.surfaceVariant
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
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    locations: Map<String, StoredLocation>,
    selectedClientId: String? = null,
    focusTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
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
                    focusTrigger = focusTrigger,
                    defaultLatitude = 35.744722,
                    defaultLongitude = 51.375278
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

            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isExpanded) strings.collapseMap else strings.expandMap
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
                networkStatus = ConnectivityStatus.Online,
                trackedClientIds = setOf("Client-1", "Client-2"),
                locations = mapOf(
                    "Client-1" to StoredLocation("Client-1", 37.7749, -122.4194, isOnline = true),
                    "Client-2" to StoredLocation("Client-2", 34.0522, -118.2437, isOnline = false)
                )
            )
        }
    }
}
