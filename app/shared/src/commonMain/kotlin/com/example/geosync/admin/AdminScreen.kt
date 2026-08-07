package com.example.geosync.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Instant as KInstant
import kotlinx.datetime.Clock as KClock
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.number
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import com.example.geosync.NotificationManager
import com.example.geosync.LanguageSelector
import com.example.geosync.localization.LocalStrings
import com.example.geosync.network.*
import com.example.geosync.network.ConnectivityStatus

@Composable
fun AdminScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onMapToggle: (Boolean) -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val viewModel: AdminViewModel = viewModel { AdminViewModel(isPreview) }
    val isConnected by viewModel.isConnected.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val trackedClientIds by viewModel.trackedClientIds.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val mapMode by viewModel.mapMode.collectAsState()
    val clientIdInput by viewModel.clientIdInput.collectAsState()
    val isListExpanded by viewModel.isListExpanded.collectAsState()
    val isMapExpanded by viewModel.isMapExpanded.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val reviewCameraState by viewModel.reviewCameraState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    val reviewSession by viewModel.reviewSession.collectAsState()
    val reviewRange by viewModel.reviewRange.collectAsState()
    val reviewFocusTrigger by viewModel.reviewFocusTrigger.collectAsState()
    val isTimelineMinimized by viewModel.isTimelineMinimized.collectAsState()
    val isTimelinePinned by viewModel.isTimelinePinned.collectAsState()
    val scannedPointIndex by viewModel.scannedPointIndex.collectAsState()
    val isTimeFilterVisible by viewModel.isTimeFilterVisible.collectAsState()
    val startTimeFilterInput by viewModel.startTimeFilterInput.collectAsState()
    val endTimeFilterInput by viewModel.endTimeFilterInput.collectAsState()

    val historyDates by viewModel.historyDates.collectAsState()
    val selectedHistoryDates by viewModel.selectedHistoryDates.collectAsState()
    val isHistoryCalendarExpanded by viewModel.isHistoryCalendarExpanded.collectAsState()

    // Sync Bottom Bar visibility with Map Expansion state
    LaunchedEffect(isMapExpanded) {
        onMapToggle(isMapExpanded)
    }

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
            isConnecting = isConnecting,
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
            },
            historyState = historyState,
            isHistoryLoading = isHistoryLoading,
            onLoadHistory = { viewModel.loadHistory(it) },
            reviewSession = reviewSession,
            reviewCameraState = reviewCameraState,
            reviewFocusTrigger = reviewFocusTrigger,
            reviewRange = reviewRange,
            isTimelineMinimized = isTimelineMinimized,
            isTimelinePinned = isTimelinePinned,
            scannedPointIndex = scannedPointIndex,
            isTimeFilterVisible = isTimeFilterVisible,
            startTimeFilterInput = startTimeFilterInput,
            endTimeFilterInput = endTimeFilterInput,
            onReviewRangeChange = { viewModel.updateReviewRange(it) },
            onTimelineMinimizedChange = { viewModel.setTimelineMinimized(it) },
            onTimelinePinnedChange = { viewModel.setTimelinePinned(it) },
            onScannedPointChange = { viewModel.updateScannedPoint(it) },
            onTimeFilterVisibleChange = { viewModel.setTimeFilterVisible(it) },
            onUpdateTimeFilterInputs = { start, end -> viewModel.updateTimeFilterInputs(start, end) },
            onApplyTimeFilter = { start, end -> viewModel.applyTimeFilter(start, end) },
            onMapInteraction = { viewModel.handleMapInteraction() },
            onReviewCameraChanged = { viewModel.updateReviewCameraState(it) },
            onEnterReview = { viewModel.enterReviewMode(it) },
            onExitReview = { viewModel.exitReviewMode() },
            historyDates = historyDates,
            selectedHistoryDates = selectedHistoryDates,
            isHistoryCalendarExpanded = isHistoryCalendarExpanded,
            onLoadAvailableDates = { viewModel.loadAvailableDates(it) },
            onToggleHistoryDate = { cid, date -> viewModel.toggleHistoryDate(cid, date) },
            onClearHistoryDates = { viewModel.clearHistoryDates(it) },
            onHistoryCalendarExpandedChange = { viewModel.setHistoryCalendarExpanded(it) }
        )
    }
}

@Composable
fun AdminContent(
    isConnected: Boolean,
    isConnecting: Boolean = false,
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
    onMapToggle: (Boolean) -> Unit = {},
    historyState: Map<String, List<TrackingSessionHistory>> = emptyMap(),
    isHistoryLoading: Boolean = false,
    onLoadHistory: (String) -> Unit = {},
    reviewSession: TrackingSessionHistory? = null,
    reviewCameraState: MapCameraState = MapCameraState(35.6994, 51.3377, 11.0),
    reviewFocusTrigger: Long = 0L,
    reviewRange: ClosedFloatingPointRange<Float> = 0f..1f,
    isTimelineMinimized: Boolean = false,
    isTimelinePinned: Boolean = false,
    scannedPointIndex: Int? = null,
    isTimeFilterVisible: Boolean = false,
    startTimeFilterInput: String = "",
    endTimeFilterInput: String = "",
    onReviewRangeChange: (ClosedFloatingPointRange<Float>) -> Unit = {},
    onTimelineMinimizedChange: (Boolean) -> Unit = {},
    onTimelinePinnedChange: (Boolean) -> Unit = {},
    onScannedPointChange: (Int?) -> Unit = {},
    onTimeFilterVisibleChange: (Boolean) -> Unit = {},
    onUpdateTimeFilterInputs: (String, String) -> Unit = { _, _ -> },
    onApplyTimeFilter: (String, String) -> Unit = { _, _ -> },
    onMapInteraction: () -> Unit = {},
    onReviewCameraChanged: (MapCameraState) -> Unit = {},
    onEnterReview: (TrackingSessionHistory) -> Unit = {},
    onExitReview: () -> Unit = {},
    historyDates: Map<String, Set<String>> = emptyMap(),
    selectedHistoryDates: Set<String> = emptySet(),
    isHistoryCalendarExpanded: Boolean = false,
    onLoadAvailableDates: (String) -> Unit = {},
    onToggleHistoryDate: (String, String) -> Unit = { _, _ -> },
    onClearHistoryDates: (String) -> Unit = {},
    onHistoryCalendarExpandedChange: (Boolean) -> Unit = {}
) {
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var clientToRemove by remember { mutableStateOf<String?>(null) }
    var clientForHistory by remember { mutableStateOf<String?>(null) }
    var focusTrigger by remember { mutableStateOf(0L) }
    val strings = LocalStrings.current

    // Pulsating animation for Full Screen button
    val infiniteTransition = rememberInfiniteTransition(label = "FullScreenPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    // Zoom level indicator logic
    var showZoomIndicator by remember { mutableStateOf(false) }
    val currentZoom = if (reviewSession != null) reviewCameraState.zoom else cameraState.zoom
    
    LaunchedEffect(currentZoom) {
        showZoomIndicator = true
        kotlinx.coroutines.delay(2000)
        showZoomIndicator = false
    }

    // Confirmation Dialog
    clientToRemove?.let { id ->
        RemoveClientDialog(
            clientId = id,
            onConfirm = {
                onRemoveClient(id)
                clientToRemove = null
            },
            onDismiss = { clientToRemove = null }
        )
    }

    // History Sheet
    clientForHistory?.let { id ->
        HistoryBottomSheet(
            clientId = id,
            history = historyState[id] ?: emptyList(),
            isLoading = isHistoryLoading,
            isClientOnline = locations[id]?.isOnline == true,
            availableDates = historyDates[id] ?: emptySet(),
            selectedDates = selectedHistoryDates,
            isCalendarExpanded = isHistoryCalendarExpanded,
            onDateToggled = { onToggleHistoryDate(id, it) },
            onClearDates = { onClearHistoryDates(id) },
            onCalendarToggle = { onHistoryCalendarExpandedChange(it) },
            onLoadDates = { onLoadAvailableDates(id) },
            onDismiss = { clientForHistory = null },
            onSessionClick = { session ->
                onEnterReview(session)
                clientForHistory = null
            }
        )
    }

    // Auto-focus on map mode change if clients exist
    LaunchedEffect(mapMode) {
        if (trackedClientIds.isNotEmpty()) {
            // Find the last client in the list that actually has a location
            val targetId = trackedClientIds.reversed().firstOrNull { id -> locations.containsKey(id) }
                ?: trackedClientIds.lastOrNull() // Fallback to last added client even if no location yet (will center when location arrives)

            if (targetId != null) {
                selectedClientId = targetId
                focusTrigger++
            }
        }
    }
    
    val isInputValid = remember(clientIdInput) {
        val trimmed = clientIdInput.trim()
        trimmed.isEmpty() || 
        trimmed.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()) ||
        (trimmed.startsWith("@") && trimmed.length >= 3)
    }

    val filteredLocations = remember(locations, trackedClientIds) {
        locations.filterKeys { it in trackedClientIds }
    }

    // Auto-focus on new clients when they appear for the first time
    var knownClientIds by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(filteredLocations.keys) {
        val newClients = filteredLocations.keys - knownClientIds
        if (newClients.isNotEmpty()) {
            selectedClientId = newClients.first()
            focusTrigger++
        }
        knownClientIds = filteredLocations.keys
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Prevent black flash
    ) {
        // 1. Map in the background (Only visible when not in review mode)
        if (reviewSession == null) {
            MapPreview(
                locations = filteredLocations,
                mapMode = mapMode,
                selectedClientId = selectedClientId,
                focusTrigger = focusTrigger,
                cameraState = cameraState,
                onCameraChanged = onCameraChanged,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Controls in the foreground
        if (!isMapExpanded && reviewSession == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
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
                                ConnectionStatus(isConnected, isConnecting, networkStatus, onRetryConnection)
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
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha), CircleShape)
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
                                        onRemove = { clientToRemove = id },
                                        onHistory = {
                                            clientForHistory = id
                                            onLoadHistory(id)
                                            onLoadAvailableDates(id)
                                        },
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

        // 3. Review Mode (History Map + Full Screen controls)
        if (reviewSession != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val filteredSession = remember(reviewSession, reviewRange) {
                    val totalPoints = reviewSession.points.size
                    if (totalPoints <= 1) reviewSession
                    else {
                        val startIndex = (reviewRange.start * (totalPoints - 1)).toInt().coerceIn(0, totalPoints - 1)
                        val endIndex = (reviewRange.endInclusive * (totalPoints - 1)).toInt().coerceIn(startIndex, totalPoints - 1)
                        val subPoints = reviewSession.points.subList(startIndex, endIndex + 1)
                        
                        reviewSession.copy(
                            points = subPoints,
                            startTime = subPoints.firstOrNull()?.timestamp ?: reviewSession.startTime,
                            endTime = subPoints.lastOrNull()?.timestamp ?: reviewSession.endTime,
                            totalDistanceKm = AdminUtils.calculatePathDistance(subPoints)
                        )
                    }
                }

                // Dedicated Full-Screen History Map (Isolated high-performance instance)
                filteredSession.let { session ->
                    HistoryReviewMapView(
                        modifier = Modifier.fillMaxSize(),
                        session = session,
                        cameraState = reviewCameraState,
                        focusTrigger = reviewFocusTrigger,
                        onCameraChanged = onReviewCameraChanged,
                        onMapInteraction = onMapInteraction,
                        onScannedPointChange = onScannedPointChange
                    )
                }

                // Overlay Controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(16.dp)
                ) {
                    // Exit Review Button
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = CircleShape,
                        shadowElevation = 0.dp
                    ) {
                        IconButton(
                            onClick = { 
                                if (reviewSession != null) {
                                    onExitReview()
                                } else {
                                    onMapExpandedChange(false)
                                    onMapToggle(false)
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, strings.close, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Timeline Slider (Modernized Shared-Element Style Morph)
                    reviewSession.let { session ->
                        if (session.points.size > 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 24.dp)
                            ) {
                                AnimatedContent(
                                    targetState = isTimelineMinimized,
                                    modifier = Modifier.align(Alignment.BottomStart),
                                    transitionSpec = {
                                        if (targetState) { // MINIMIZING (Card -> Bubble)
                                            (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)))
                                                .togetherWith(slideOutVertically { it } + fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.5f, transformOrigin = TransformOrigin(0f, 1f)))
                                        } else { // EXPANDING (Bubble -> Card)
                                            (slideInVertically { it } + fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.5f, transformOrigin = TransformOrigin(0f, 1f)))
                                                .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)))
                                        }.using(SizeTransform(clip = false))
                                    },
                                    label = "TimelineTransition"
                                ) { minimized ->
                                    if (minimized) {
                                        // Minimized Bubble - Truly Bottom Left
                                        Surface(
                                            onClick = { onTimelineMinimizedChange(false) },
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(20.dp),
                                            shadowElevation = 0.dp,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.History, null, tint = Color.White)
                                            }
                                        }
                                    } else {
                                        // Expanded Slider Card - Grows from the same Bottom Left origin
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp)
                                                .widthIn(max = 600.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                            shape = RoundedCornerShape(28.dp),
                                            shadowElevation = 0.dp,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            onClick = { onTimelineMinimizedChange(true) },
                                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                                            shape = CircleShape,
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                            }
                                                        }
                                                        Spacer(Modifier.width(12.dp))
                                                        Text(
                                                            text = strings.trackingHistory,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            onClick = { onTimeFilterVisibleChange(!isTimeFilterVisible) },
                                                            color = if (isTimeFilterVisible) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                            shape = CircleShape,
                                                            modifier = Modifier.size(32.dp),
                                                            border = if (!isTimeFilterVisible) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Schedule,
                                                                    contentDescription = null,
                                                                    tint = if (isTimeFilterVisible) Color.White else MaterialTheme.colorScheme.outline,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }

                                                        Spacer(Modifier.width(8.dp))

                                                        Surface(
                                                            onClick = { onTimelinePinnedChange(!isTimelinePinned) },
                                                            color = if (isTimelinePinned) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                            shape = CircleShape,
                                                            modifier = Modifier.size(32.dp),
                                                            border = if (!isTimelinePinned) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = if (isTimelinePinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                                                    contentDescription = null,
                                                                    tint = if (isTimelinePinned) Color.White else MaterialTheme.colorScheme.outline,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                AnimatedVisibility(
                                                    visible = isTimeFilterVisible,
                                                    enter = expandVertically() + fadeIn(),
                                                    exit = shrinkVertically() + fadeOut()
                                                ) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                        shape = RoundedCornerShape(24.dp),
                                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                                    ) {
                                                        val initialSessionStartTime = remember(session) {
                                                            AdminUtils.formatToLocalTime(session.points.firstOrNull()?.timestamp ?: session.points.firstOrNull()?.receivedAt ?: session.startTime).let { if (it == "--:--") "00:00" else it }
                                                        }
                                                        val initialSessionEndTime = remember(session) {
                                                            AdminUtils.formatToLocalTime(session.points.lastOrNull()?.timestamp ?: session.points.lastOrNull()?.receivedAt ?: session.endTime).let { if (it == "--:--") "00:00" else it }
                                                        }

                                                        Row(
                                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            val commonTextFieldColors = TextFieldDefaults.colors(
                                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                                focusedIndicatorColor = Color.Transparent,
                                                                unfocusedIndicatorColor = Color.Transparent,
                                                                cursorColor = MaterialTheme.colorScheme.primary
                                                            )

                                                            // Start Time Pill
                                                            TextField(
                                                                value = startTimeFilterInput,
                                                                onValueChange = { 
                                                                    if (it.length <= 5) onUpdateTimeFilterInputs(it, endTimeFilterInput) 
                                                                },
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(54.dp)
                                                                    .border(
                                                                        width = 3.dp,
                                                                        color = Color(0xFF4CAF50),
                                                                        shape = RoundedCornerShape(16.dp)
                                                                    ),
                                                                placeholder = { 
                                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                        Text(initialSessionStartTime, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50).copy(alpha = 0.6f)) 
                                                                    }
                                                                },
                                                                singleLine = true,
                                                                shape = RoundedCornerShape(16.dp),
                                                                colors = commonTextFieldColors,
                                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.Black, 
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                                    color = Color(0xFF4CAF50)
                                                                ),
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                                            )

                                                            // End Time Pill
                                                            TextField(
                                                                value = endTimeFilterInput,
                                                                onValueChange = { 
                                                                    if (it.length <= 5) onUpdateTimeFilterInputs(startTimeFilterInput, it) 
                                                                },
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(54.dp)
                                                                    .border(
                                                                        width = 3.dp,
                                                                        color = Color(0xFFF44336),
                                                                        shape = RoundedCornerShape(16.dp)
                                                                    ),
                                                                placeholder = { 
                                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                        Text(initialSessionEndTime, style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336).copy(alpha = 0.6f)) 
                                                                    }
                                                                },
                                                                singleLine = true,
                                                                shape = RoundedCornerShape(16.dp),
                                                                colors = commonTextFieldColors,
                                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.Black, 
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                                    color = Color(0xFFF44336)
                                                                ),
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                                            )

                                                            // Apply Action
                                                            FilledIconButton(
                                                                onClick = { onApplyTimeFilter(startTimeFilterInput, endTimeFilterInput) },
                                                                modifier = Modifier.size(54.dp),
                                                                shape = RoundedCornerShape(16.dp),
                                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                                    containerColor = MaterialTheme.colorScheme.primary
                                                                )
                                                            ) {
                                                                Icon(Icons.Default.Check, null, tint = Color.White)
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(Modifier.height(12.dp))
                                                
                                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                                    Column {
                                                        Box(modifier = Modifier.fillMaxWidth()) {
                                                            RangeSlider(
                                                                value = reviewRange,
                                                                onValueChange = { 
                                                                    onReviewRangeChange(it)
                                                                    onScannedPointChange(null) // Clear scanner when manually sliding
                                                                },
                                                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                                                startThumb = {
                                                                    val startText = remember(session, reviewRange.start) {
                                                                        val idx = (reviewRange.start * (session.points.size - 1)).toInt().coerceIn(0, session.points.size - 1)
                                                                        val point = session.points[idx]
                                                                        val ts = point.timestamp ?: point.receivedAt
                                                                        AdminUtils.formatToLocalTime(ts).let { if (it == "--:--") "" else it }
                                                                    }

                                                                    // FORCE TRANSPARENT CONTAINER & EXACT SIZE
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(width = 6.dp, height = 48.dp)
                                                                            .background(Color.Transparent)
                                                                    ) {
                                                                        Column(
                                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                                            modifier = Modifier.fillMaxSize()
                                                                        ) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .size(width = 6.dp, height = 28.dp)
                                                                                    .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                                                                            )
                                                                            if (reviewRange.start > 0.02f) {
                                                                                Text(
                                                                                    text = startText,
                                                                                    color = Color(0xFF4CAF50),
                                                                                    style = MaterialTheme.typography.labelSmall,
                                                                                    fontWeight = FontWeight.ExtraBold,
                                                                                    modifier = Modifier.offset(y = 4.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                endThumb = {
                                                                    val endText = remember(session, reviewRange.endInclusive) {
                                                                        val idx = (reviewRange.endInclusive * (session.points.size - 1)).toInt().coerceIn(0, session.points.size - 1)
                                                                        val point = session.points[idx]
                                                                        val ts = point.timestamp ?: point.receivedAt
                                                                        AdminUtils.formatToLocalTime(ts).let { if (it == "--:--") "" else it }
                                                                    }

                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(width = 6.dp, height = 48.dp)
                                                                            .background(Color.Transparent)
                                                                    ) {
                                                                        Column(
                                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                                            modifier = Modifier.fillMaxSize()
                                                                        ) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .size(width = 6.dp, height = 28.dp)
                                                                                    .background(Color(0xFFF44336), RoundedCornerShape(3.dp))
                                                                            )
                                                                            if (reviewRange.endInclusive < 0.98f) {
                                                                                Text(
                                                                                    text = endText,
                                                                                    color = Color(0xFFF44336),
                                                                                    style = MaterialTheme.typography.labelSmall,
                                                                                    fontWeight = FontWeight.ExtraBold,
                                                                                    modifier = Modifier.offset(y = 4.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                track = { rangeSliderState ->
                                                                    SliderDefaults.Track(
                                                                        rangeSliderState = rangeSliderState,
                                                                        modifier = Modifier.height(6.dp),
                                                                        colors = SliderDefaults.colors(
                                                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                                                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                                        )
                                                                    )
                                                                }
                                                            )

                                                            // 4. SCANNER MARKER (Overlays the Slider)
                                                            scannedPointIndex?.let { idx ->
                                                                val total = session.points.size - 1
                                                                val progress = if (total > 0) idx.toFloat() / total else 0f
                                                                val scanTimeText = remember(session, idx) {
                                                                    try {
                                                                        val ts = (session.points[idx].timestamp ?: "").replace(" ", "T")
                                                                        val inst = KInstant.parse(ts.let { if (!it.contains("+") && !it.endsWith("Z")) "${it}Z" else it })
                                                                        val local = inst.toLocalDateTime(TimeZone.currentSystemDefault())
                                                                        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
                                                                    } catch(_: Exception) { "" }
                                                                }

                                                                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                                                                    val xOffset = maxWidth * progress
                                                                    Column(
                                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                                        modifier = Modifier.offset(x = xOffset - 3.dp)
                                                                    ) {
                                                                        // Scanner Bar
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(width = 4.dp, height = 32.dp)
                                                                                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp))
                                                                        )
                                                                        // Scanner Time Popup (Centered under the bar)
                                                                        Surface(
                                                                            color = MaterialTheme.colorScheme.tertiary,
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            modifier = Modifier.offset(y = 4.dp)
                                                                        ) {
                                                                            Text(
                                                                                text = scanTimeText,
                                                                                color = Color.White,
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                fontWeight = FontWeight.Black,
                                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        
                                                        // Static Session Time Labels Row
                                                        Box(modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 2.dp)) {
                                                            val sessionStartTime = remember(session) {
                                                                try {
                                                                    val ts = (session.points.firstOrNull()?.timestamp ?: session.startTime ?: "").replace(" ", "T")
                                                                    val inst = KInstant.parse(ts.let { if (!it.contains("+") && !it.endsWith("Z")) "${it}Z" else it })
                                                                    val local = inst.toLocalDateTime(TimeZone.currentSystemDefault())
                                                                    "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
                                                                } catch(_: Exception) { "--:--" }
                                                            }
                                                            val sessionEndTime = remember(session) {
                                                                try {
                                                                    val ts = (session.points.lastOrNull()?.timestamp ?: session.endTime ?: "").replace(" ", "T")
                                                                    val inst = KInstant.parse(ts.let { if (!it.contains("+") && !it.endsWith("Z")) "${it}Z" else it })
                                                                    val local = inst.toLocalDateTime(TimeZone.currentSystemDefault())
                                                                    "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
                                                                } catch(_: Exception) { "--:--" }
                                                            }

                                                            Text(
                                                                text = sessionStartTime,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                                modifier = Modifier.align(Alignment.CenterStart)
                                                            )

                                                            Text(
                                                                text = sessionEndTime,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                                modifier = Modifier.align(Alignment.CenterEnd)
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

                    // Review Info Banner (Redesigned Floating Style - Ultra Modern)
                    filteredSession.let { session ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 8.dp), 
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(32.dp),
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Route, null, modifier = Modifier.size(20.dp), tint = Color.White)
                                    }
                                }
                                
                                Spacer(Modifier.width(16.dp))
                                
                                val distanceText = if (session.totalDistanceKm < 1.0) {
                                    "${(session.totalDistanceKm * 1000).toInt()} m"
                                } else {
                                    "${session.totalDistanceKm.toString().take(4)} km"
                                }
                                
                                val durationText = try {
                                    val sStr = (session.startTime ?: "").replace(" ", "T")
                                    val eStr = (session.endTime ?: session.points.lastOrNull()?.timestamp ?: "").replace(" ", "T")
                                    if (sStr.isEmpty() || eStr.isEmpty()) "---"
                                    else {
                                        val start = KInstant.parse(sStr)
                                        val end = KInstant.parse(eStr)
                                        val diff = end - start
                                        val totalSecs = diff.inWholeSeconds
                                        if (totalSecs >= 3600) "${totalSecs / 3600}h ${(totalSecs % 3600) / 60}m"
                                        else if (totalSecs >= 60) "${totalSecs / 60} min"
                                        else "${totalSecs}s"
                                    }
                                } catch(_: Exception) { "---" }

                                Column {
                                    Text(
                                        text = "$distanceText Visible Path",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = durationText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Expanded Live Map Overlay (ONLY the button, no full-screen Box to block touches)
        if (isMapExpanded && reviewSession == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = CircleShape,
                    shadowElevation = 0.dp
                ) {
                    IconButton(
                        onClick = { 
                            onMapExpandedChange(false)
                            onMapToggle(false)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, strings.close, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 5. Zoom Level Indicator
        AnimatedVisibility(
            visible = showZoomIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isMapExpanded || reviewSession != null) 40.dp else 40.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "Zoom: ${currentZoom.toString().take(4)}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ConnectionStatus(isConnected: Boolean, isConnecting: Boolean, networkStatus: ConnectivityStatus, onRetry: () -> Unit) {
    val isOffline = networkStatus == ConnectivityStatus.Offline
    val strings = LocalStrings.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "ConnectingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = !isConnected && !isConnecting && !isOffline) { onRetry() }
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    alpha = if (isConnecting) pulseAlpha else 1f
                }
                .background(
                    color = when {
                        isConnected -> Color.Green
                        isConnecting -> Color(0xFFFFB300) // Orange for connecting
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
                isConnecting -> strings.connecting
                isOffline -> strings.youAreOffline
                else -> strings.offlineTapToRetry
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isConnected || isConnecting) Color.Unspecified else MaterialTheme.colorScheme.error
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
    onHistory: () -> Unit,
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
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1
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
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
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

            Row {
                IconButton(onClick = onHistory) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = strings.viewHistory,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
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
}

@Composable
fun HistorySheetDragHandle() {
    val purplePrimary = Color(0xFF8E24AA)
    val purpleLight = Color(0xFFCE93D8)
    
    Column(
        modifier = Modifier
            .padding(vertical = 14.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            purpleLight.copy(alpha = 0.4f),
                            purplePrimary,
                            purpleLight.copy(alpha = 0.4f)
                        )
                    )
                )
                .shadow(1.dp, CircleShape)
        )
    }
}

/**
 * A beautiful, modern, and accurate vertical scrollbar for LazyColumn.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    color: Color = Color(0xFF8E24AA),
    width: androidx.compose.ui.unit.Dp = 4.dp
): Modifier = drawWithContent {
    drawContent()
    
    val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: return@drawWithContent
    val totalItemsCount = state.layoutInfo.totalItemsCount
    if (totalItemsCount == 0) return@drawWithContent

    val viewportHeight = size.height
    val scrollbarHeight = (viewportHeight / totalItemsCount) * state.layoutInfo.visibleItemsInfo.size
    val scrollbarOffsetY = (viewportHeight / totalItemsCount) * firstVisibleElementIndex

    // Draw rounded modern scrollbar with a slight offset from edge
    drawRoundRect(
        color = color.copy(alpha = 0.7f),
        topLeft = Offset(size.width - width.toPx() - 4f, scrollbarOffsetY),
        size = Size(width.toPx(), scrollbarHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    clientId: String,
    history: List<TrackingSessionHistory>,
    isLoading: Boolean,
    isClientOnline: Boolean,
    availableDates: Set<String>,
    selectedDates: Set<String>,
    isCalendarExpanded: Boolean,
    onDateToggled: (String) -> Unit,
    onClearDates: () -> Unit,
    onCalendarToggle: (Boolean) -> Unit,
    onLoadDates: () -> Unit,
    onDismiss: () -> Unit,
    onSessionClick: (TrackingSessionHistory) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    val indicatorPurple = Color(0xFF8E24AA)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { HistorySheetDragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        val coroutineScope = rememberCoroutineScope()
        val showButton by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 0 }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Modern Accurate Progress Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .padding(horizontal = 24.dp)
                            .clip(CircleShape),
                        color = indicatorPurple,
                        trackColor = indicatorPurple.copy(alpha = 0.1f)
                    )
                } else {
                    Spacer(Modifier.height(3.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (history.isNotEmpty()) "${strings.trackingHistory} (${history.size})" else strings.trackingHistory,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { onCalendarToggle(!isCalendarExpanded) }
                    ) {
                        Icon(
                            imageVector = if (isCalendarExpanded) Icons.Default.Close else Icons.Default.DateRange,
                            contentDescription = null,
                            tint = indicatorPurple
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isCalendarExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    HistoryCalendar(
                        availableDates = availableDates,
                        selectedDates = selectedDates,
                        onDateToggled = onDateToggled,
                        onClearDates = onClearDates,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clientId,
                        style = MaterialTheme.typography.bodySmall,
                        color = indicatorPurple
                    )
                    
                    if (!isCalendarExpanded && selectedDates.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = indicatorPurple.copy(alpha = 0.1f),
                            border = BorderStroke(0.5.dp, indicatorPurple.copy(alpha = 0.2f)),
                            onClick = { onCalendarToggle(true) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(12.dp), tint = indicatorPurple)
                                Text(
                                    text = if (selectedDates.size == 1) selectedDates.first() else "${selectedDates.size} Days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = indicatorPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isLoading && history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = indicatorPurple)
                    }
                } else if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.noHistoryFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScrollbar(listState, indicatorPurple),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(history) { index, session ->
                            HistorySessionItem(
                                session = session,
                                isLive = index == 0 && isClientOnline,
                                onClick = { onSessionClick(session) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Floating Scroll-to-Top Button
            androidx.compose.animation.AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 48.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = indicatorPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.shadow(8.dp, CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        }
    }
}

@Composable
fun HistorySessionItem(
    session: TrackingSessionHistory,
    isLive: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val startPoint = session.points.firstOrNull()
    val endPoint = session.points.lastOrNull()
    
    val distance = session.totalDistanceKm
    val distanceText = if (distance < 1.0) {
        "${(distance * 1000).toInt()} m"
    } else {
        "${distance.toString().take(4)} km"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            // LEFT: Info Column
            Column(modifier = Modifier.weight(1.3f).padding(20.dp)) {
                // Header: Distance and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Route,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (isLive) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = strings.statusLive,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timeline-like Start and End
                Column(modifier = Modifier.fillMaxWidth()) {
                    // START row
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // Timeline part
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 4.dp, end = 16.dp).fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .shadow(2.dp, CircleShape)
                            )
                            // Vertical line
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(2.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                            )
                        }
                        
                        // Info part
                        HistoryTimeLocation(
                            label = strings.sessionStart,
                            time = session.startTime,
                            lat = startPoint?.latitude,
                            lng = startPoint?.longitude,
                            isStart = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // END row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Timeline part
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 4.dp, end = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(if (session.endTime != null) Color(0xFFF44336) else Color.Gray, CircleShape)
                                    .shadow(2.dp, CircleShape)
                            )
                        }

                        // Info part
                        HistoryTimeLocation(
                            label = strings.sessionEnd,
                            time = session.endTime,
                            lat = endPoint?.latitude,
                            lng = endPoint?.longitude,
                            isStart = false
                        )
                    }
                }
            }

            // RIGHT: Map Preview Placeholder
            Box(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .drawWithContent {
                        drawContent()
                        // Draw a subtle grid to simulate map lines
                        val gridSize = 20.dp.toPx()
                        val lineColor = Color.Gray.copy(alpha = 0.1f)
                        for (x in 0 until (size.width / gridSize).toInt()) {
                            drawLine(lineColor, Offset(x * gridSize, 0f), Offset(x * gridSize, size.height), 1f)
                        }
                        for (y in 0 until (size.height / gridSize).toInt()) {
                            drawLine(lineColor, Offset(0f, y * gridSize), Offset(size.width, y * gridSize), 1f)
                        }
                    }
                    .clickable { onClick() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp).shadow(6.dp, CircleShape),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.viewHistory.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "${session.points.size} points",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Expand icon to indicate interaction
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun HistoryTimeLocation(label: String, time: String?, lat: Double?, lng: Double?, isStart: Boolean) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isStart) Color(0xFF2E7D32) else if (time != null) Color(0xFFC62828) else Color.Gray,
            fontWeight = FontWeight.Black,
            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = AdminUtils.formatToLocalTime(time),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = AdminUtils.formatToLocalDate(time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (lat != null && lng != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${lat.toString().take(8)}, ${lng.toString().take(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1
                    )
                }
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
    modifier: Modifier = Modifier,
    reviewSession: TrackingSessionHistory? = null
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
                onCameraChanged = onCameraChanged,
                reviewSession = reviewSession
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

@Composable
fun RemoveClientDialog(
    clientId: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val shortId = if (clientId.length > 13) "${clientId.take(6)}...${clientId.takeLast(4)}" else clientId

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        title = {
            Text(
                text = strings.confirmRemoval,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = shortId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.removeClientMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.remove, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.cancel, fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun HistoryCalendar(
    availableDates: Set<String>,
    selectedDates: Set<String>,
    onDateToggled: (String) -> Unit,
    onClearDates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorPurple = Color(0xFF8E24AA)
    
    val parsedDates = remember(availableDates) {
        availableDates.mapNotNull { 
            try { LocalDate.parse(it) } catch (_: Exception) { null } 
        }.sortedDescending()
    }
    
    if (parsedDates.isEmpty()) {
        Box(modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("No dates available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Select Days", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = indicatorPurple)
            if (selectedDates.isNotEmpty()) {
                TextButton(
                    onClick = onClearDates,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(parsedDates) { date ->
                val dateStr = date.toString()
                val isSelected = dateStr in selectedDates
                
                Surface(
                    onClick = { onDateToggled(dateStr) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) indicatorPurple else Color.Transparent,
                    border = if (isSelected) null else BorderStroke(1.dp, indicatorPurple.copy(alpha = 0.3f)),
                    modifier = Modifier.width(70.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else indicatorPurple
                        )
                        Text(
                            text = date.month.name.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = date.year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f)
                        )
                    }
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
                onCameraChanged = {},
                historyDates = emptyMap(),
                selectedHistoryDates = emptySet(),
                isHistoryCalendarExpanded = false,
                onLoadAvailableDates = {},
                onToggleHistoryDate = { _, _ -> },
                onClearHistoryDates = {},
                onHistoryCalendarExpandedChange = {}
            )
        }
    }
}
