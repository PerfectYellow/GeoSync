package com.example.geosync.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geosync.NotificationManager
import com.example.geosync.NotificationType
import com.example.geosync.SettingsManager
import com.example.geosync.localization.LocalizationManager
import com.example.geosync.network.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.*

enum class MapMode {
    OPEN_STREET, MAP_IR, INTERNAL, OFFLINE
}

data class MapCameraState(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double
)

class AdminViewModel(private val isPreview: Boolean = false) : ViewModel() {
    private val client = geoHttpClient

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _trackedClientIds = MutableStateFlow<Set<String>>(SettingsManager.trackedClientIds)
    val trackedClientIds: StateFlow<Set<String>> = _trackedClientIds.asStateFlow()

    private val _locations = MutableStateFlow<Map<String, StoredLocation>>(emptyMap())
    val locations: StateFlow<Map<String, StoredLocation>> = combine(_locations, _trackedClientIds) { locs, ids ->
        locs.filterKeys { it in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _mapMode = MutableStateFlow(MapMode.OPEN_STREET)
    val mapMode: StateFlow<MapMode> = _mapMode.asStateFlow()

    private val _clientIdInput = MutableStateFlow("")
    val clientIdInput: StateFlow<String> = _clientIdInput.asStateFlow()

    private val _isListExpanded = MutableStateFlow(false)
    val isListExpanded: StateFlow<Boolean> = _isListExpanded.asStateFlow()

    private val _isMapExpanded = MutableStateFlow(false)
    val isMapExpanded: StateFlow<Boolean> = _isMapExpanded.asStateFlow()

    private val _cameraState = MutableStateFlow(MapCameraState(35.6994, 51.3377, 11.0))
    val cameraState: StateFlow<MapCameraState> = _cameraState.asStateFlow()

    private val _reviewCameraState = MutableStateFlow(MapCameraState(35.6994, 51.3377, 11.0))
    val reviewCameraState: StateFlow<MapCameraState> = _reviewCameraState.asStateFlow()

    private val _historyState = MutableStateFlow<Map<String, List<TrackingSessionHistory>>>(emptyMap())
    val historyState: StateFlow<Map<String, List<TrackingSessionHistory>>> = _historyState.asStateFlow()

    private val _historyDates = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val historyDates: StateFlow<Map<String, Set<String>>> = _historyDates.asStateFlow()

    private val _selectedHistoryDates = MutableStateFlow<Set<String>>(emptySet())
    val selectedHistoryDates: StateFlow<Set<String>> = _selectedHistoryDates.asStateFlow()

    private val _isHistoryCalendarExpanded = MutableStateFlow(false)
    val isHistoryCalendarExpanded: StateFlow<Boolean> = _isHistoryCalendarExpanded.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _reviewSession = MutableStateFlow<TrackingSessionHistory?>(null)
    val reviewSession: StateFlow<TrackingSessionHistory?> = _reviewSession.asStateFlow()

    private val _reviewRange = MutableStateFlow<ClosedFloatingPointRange<Float>>(0f..1f)
    val reviewRange: StateFlow<ClosedFloatingPointRange<Float>> = _reviewRange.asStateFlow()

    private val _isTimelineMinimized = MutableStateFlow(false)
    val isTimelineMinimized: StateFlow<Boolean> = _isTimelineMinimized.asStateFlow()

    private val _isTimelinePinned = MutableStateFlow(false)
    val isTimelinePinned: StateFlow<Boolean> = _isTimelinePinned.asStateFlow()

    private val _scannedPointIndex = MutableStateFlow<Int?>(null)
    val scannedPointIndex: StateFlow<Int?> = _scannedPointIndex.asStateFlow()

    private val _isTimeFilterVisible = MutableStateFlow(false)
    val isTimeFilterVisible: StateFlow<Boolean> = _isTimeFilterVisible.asStateFlow()

    private val _startTimeFilterInput = MutableStateFlow("")
    val startTimeFilterInput: StateFlow<String> = _startTimeFilterInput.asStateFlow()

    private val _endTimeFilterInput = MutableStateFlow("")
    val endTimeFilterInput: StateFlow<String> = _endTimeFilterInput.asStateFlow()

    private val _reviewFocusTrigger = MutableStateFlow(0L)
    val reviewFocusTrigger: StateFlow<Long> = _reviewFocusTrigger.asStateFlow()

    private val _activeHistoryClientId = MutableStateFlow<String?>(null)
    val activeHistoryClientId: StateFlow<String?> = _activeHistoryClientId.asStateFlow()

    private var lastOnlineMode = MapMode.OPEN_STREET

    private var connectionJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    
    // To track if we've already shown the connection error banner
    private var errorNotified = false

    init {
        if (!isPreview) {
            connect()
            
            // Persist tracked clients whenever they change
            viewModelScope.launch {
                _trackedClientIds.collect { ids ->
                    SettingsManager.trackedClientIds = ids
                }
            }
        }
    }

    fun retryConnection() {
        connect()
    }

    fun setActiveHistoryClientId(clientId: String?) {
        if (_activeHistoryClientId.value != clientId) {
            _activeHistoryClientId.value = clientId
            // Clear history and filters for previous client when switching
            if (clientId != null) {
                _selectedHistoryDates.value = emptySet()
                _historyState.update { it - it.keys.filter { key -> key != clientId }.toSet() }
            }
        }
    }

    fun setMapMode(mode: MapMode, isOffline: Boolean = false) {
        val strings = LocalizationManager.strings
        if (isOffline && mode != MapMode.OFFLINE) {
            NotificationManager.show(strings.offlineMapChangeError, NotificationType.ERROR)
            return
        }
        
        _mapMode.value = mode
        if (mode == MapMode.OFFLINE) {
            // Apply requested 14.6 zoom for offline map
            if (_trackedClientIds.value.isEmpty()) {
                _cameraState.value = MapCameraState(35.6994, 51.3377, 14.6)
            } else {
                // If we are tracking someone, keep their position but increase zoom to 14.6
                _cameraState.update { it.copy(zoom = 14.6) }
            }
        } else if (mode == MapMode.INTERNAL && _trackedClientIds.value.isEmpty()) {
            _cameraState.value = MapCameraState(35.6994, 51.3377, 14.0)
        }
        
        if (!isOffline && mode != MapMode.OFFLINE) {
            lastOnlineMode = mode
        }
    }

    fun handleNetworkChange(isOffline: Boolean) {
        if (isOffline) {
            if (_mapMode.value != MapMode.OFFLINE) {
                setMapMode(MapMode.OFFLINE, true)
            }
        } else {
            // Return to previous online mode if it was swapped to OFFLINE due to connection loss
            if (_mapMode.value == MapMode.OFFLINE) {
                setMapMode(lastOnlineMode, false)
            }
        }
    }

    fun setClientIdInput(input: String) {
        _clientIdInput.value = input
    }

    fun setListExpanded(expanded: Boolean) {
        _isListExpanded.value = expanded
    }

    fun setMapExpanded(expanded: Boolean) {
        _isMapExpanded.value = expanded
    }

    fun updateCameraState(state: MapCameraState) {
        // Filter out "Null Island" initialization reports from map engines
        if (state.latitude == 0.0 && state.longitude == 0.0 && state.zoom <= 1.0) {
            return
        }
        _cameraState.value = state
    }

    private fun connect() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    _isConnecting.value = true
                    val strings = LocalizationManager.strings
                    client.geoLiveWebSocket {
                        session = this
                        _isConnected.value = true
                        _isConnecting.value = false
                        errorNotified = false // Reset error notification state on success
                        NotificationManager.show(strings.connectedToServer, NotificationType.SUCCESS)

                        // 1. Register as admin
                        sendSerialized(LiveLocationMessage(type = "admin.register"))

                        // 2. Re-subscribe to existing clients if any
                        val currentIds = _trackedClientIds.value.toList()
                        if (currentIds.isNotEmpty()) {
                            sendSerialized(LiveLocationMessage(
                                type = "admin.subscribe",
                                clientIds = currentIds
                            ))
                        }

                        // 3. Listen for updates
                        while (isActive) {
                            val event = receiveDeserialized<ServerEvent>()
                            when (event.type) {
                                "location.update" -> {
                                    event.location?.let { loc ->
                                        println("AdminViewModel: Received location update for ${loc.clientId}, online: ${loc.isOnline}")
                                        val normalizedLoc = loc.copy(clientId = loc.clientId.lowercase())
                                        if (_trackedClientIds.value.contains(normalizedLoc.clientId)) {
                                            val oldLoc = _locations.value[normalizedLoc.clientId]
                                            _locations.update { it + (normalizedLoc.clientId to normalizedLoc) }

                                            // If the client's online status changed and they are the one we're looking at, refresh history
                                            if (normalizedLoc.clientId == _activeHistoryClientId.value) {
                                                if (oldLoc?.isOnline != normalizedLoc.isOnline) {
                                                    println("AdminViewModel: Refreshing history for active client ${normalizedLoc.clientId} due to online status change")
                                                    loadHistory(normalizedLoc.clientId, _selectedHistoryDates.value)
                                                }
                                            }
                                        }
                                    }
                                }
                                "admin.subscribed" -> {
                                    // Successfully subscribed to client(s)
                                    event.clientIds?.forEach { id ->
                                        NotificationManager.show(strings.subscribedTo(id), NotificationType.SUCCESS)
                                    }
                                }
                                "admin.unsubscribed" -> {
                                    // Successfully unsubscribed
                                }
                                "error" -> {
                                    NotificationManager.show(strings.serverError(event.message), NotificationType.ERROR)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val strings = LocalizationManager.strings
                    _isConnected.value = false
                    _isConnecting.value = false
                    // Mark all clients as offline when Admin connection is lost
                    _locations.update { current ->
                        current.mapValues { it.value.copy(isOnline = false) }
                    }
                    if (!errorNotified) {
                        NotificationManager.show(strings.connectionLost(e.message), NotificationType.ERROR)
                        errorNotified = true
                    }
                    delay(5000)
                } finally {
                    _isConnected.value = false
                    session = null
                }
            }
        }
    }

    fun addClient(rawClientId: String) {
        val clientId = rawClientId.trim().lowercase()
        println("AdminViewModel: Adding client $clientId") // Logging for debugging
        val strings = LocalizationManager.strings
        if (clientId.isBlank()) return
        
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        val isCustomId = clientId.startsWith("@") && clientId.length >= 3
        val isUuid = clientId.matches(uuidRegex)

        if (!isUuid && !isCustomId) {
            NotificationManager.show(strings.invalidClientIdUuid, NotificationType.ERROR)
            return
        }
        
        if (_trackedClientIds.value.contains(clientId)) {
            NotificationManager.show(strings.clientAlreadyTracked(clientId), NotificationType.INFO)
            return
        }

        _trackedClientIds.update { it + clientId }
        
        viewModelScope.launch {
            try {
                if (session != null) {
                    session?.sendSerialized(LiveLocationMessage(
                        type = "admin.subscribe",
                        clientIds = listOf(clientId)
                    ))
                } else {
                    NotificationManager.show(strings.waitingForConnection, NotificationType.INFO)
                }
            } catch (e: Exception) {
                NotificationManager.show(strings.failedToSubscribe(e.message), NotificationType.ERROR)
            }
        }
    }

    fun removeClient(rawClientId: String) {
        // ... (existing logic)
        val clientId = rawClientId.lowercase()
        val strings = LocalizationManager.strings
        _trackedClientIds.update { it - clientId }
        _locations.update { it - clientId }

        viewModelScope.launch {
            try {
                session?.sendSerialized(LiveLocationMessage(
                    type = "admin.unsubscribe",
                    clientIds = listOf(clientId)
                ))
                NotificationManager.show(strings.removedClient(clientId), NotificationType.INFO)
            } catch (e: Exception) {
                NotificationManager.show(strings.failedToUnsubscribe(e.message), NotificationType.ERROR)
            }
        }
    }

    fun loadHistory(clientId: String, dates: Set<String> = emptySet()) {
        viewModelScope.launch {
            _isHistoryLoading.value = true
            try {
                var from: String? = null
                var to: String? = null
                
                if (dates.isNotEmpty()) {
                    val sorted = dates.toList().sorted()
                    from = "${sorted.first()}T00:00:00Z"
                    to = "${sorted.last()}T23:59:59Z"
                }

                val allHistory = client.fetchClientHistory(clientId, from, to)
                
                // Filter locally for disjoint days if multiple days are selected
                val history = allHistory.filter { session ->
                    if (dates.isEmpty()) true
                    else {
                        val sessionDate = session.startTime?.substringBefore("T")
                        sessionDate in dates
                    }
                }.map { session ->
                    // Ensure points within each session are sorted by time for consistent slider/map logic
                    session.copy(
                        clientId = clientId,
                        points = session.points.sortedBy { it.timestamp }
                    )
                }
                _historyState.update { it + (clientId to history) }
            } catch (e: Exception) {
                println("❌ Failed to load history for $clientId: ${e.message}")
            } finally {
                _isHistoryLoading.value = false
            }
        }
    }

    fun loadAvailableDates(clientId: String) {
        viewModelScope.launch {
            try {
                val dates = client.fetchAvailableHistoryDates(clientId).toSet()
                _historyDates.update { it + (clientId to dates) }
            } catch (e: Exception) {
                println("❌ Failed to load available dates for $clientId: ${e.message}")
            }
        }
    }

    fun toggleHistoryDate(clientId: String, date: String) {
        val current = _selectedHistoryDates.value
        val next = if (date in current) current - date else current + date
        _selectedHistoryDates.value = next
        loadHistory(clientId, next)
    }

    fun clearHistoryDates(clientId: String) {
        _selectedHistoryDates.value = emptySet()
        loadHistory(clientId, emptySet())
    }

    fun setHistoryCalendarExpanded(expanded: Boolean) {
        _isHistoryCalendarExpanded.value = expanded
    }

    fun enterReviewMode(session: TrackingSessionHistory) {
        val sortedSession = session.copy(points = session.points.sortedBy { it.timestamp ?: it.receivedAt })
        _reviewSession.value = sortedSession
        _reviewRange.value = 0f..1f
        _isMapExpanded.value = true
        
        // Initialize input fields with actual session boundaries (converted to Local)
        val firstPoint = sortedSession.points.firstOrNull()
        val lastPoint = sortedSession.points.lastOrNull()
        
        _startTimeFilterInput.value = AdminUtils.formatToLocalTime(firstPoint?.timestamp ?: firstPoint?.receivedAt ?: sortedSession.startTime)
        _endTimeFilterInput.value = AdminUtils.formatToLocalTime(lastPoint?.timestamp ?: lastPoint?.receivedAt ?: sortedSession.endTime)
    }

    fun exitReviewMode() {
        _reviewSession.value = null
        _reviewRange.value = 0f..1f
        _isMapExpanded.value = false
        _isListExpanded.value = true
    }

    fun updateReviewRange(range: ClosedFloatingPointRange<Float>) {
        _reviewRange.value = range
        
        // SYNC: Update text fields as the user slides
        val session = _reviewSession.value ?: return
        val points = session.points
        if (points.size < 2) return
        
        try {
            val total = points.size - 1
            val sIdx = (range.start * total).toInt().coerceIn(0, total)
            val eIdx = (range.endInclusive * total).toInt().coerceIn(0, total)
            
            val startTs = points[sIdx].timestamp ?: points[sIdx].receivedAt
            val endTs = points[eIdx].timestamp ?: points[eIdx].receivedAt
            
            _startTimeFilterInput.value = AdminUtils.formatToLocalTime(startTs)
            _endTimeFilterInput.value = AdminUtils.formatToLocalTime(endTs)
        } catch(e: Exception) {}
    }

    fun setTimelineMinimized(minimized: Boolean) {
        _isTimelineMinimized.value = minimized
    }

    fun setTimelinePinned(pinned: Boolean) {
        _isTimelinePinned.value = pinned
    }

    fun updateScannedPoint(index: Int?) {
        _scannedPointIndex.value = index
    }

    fun setTimeFilterVisible(visible: Boolean) {
        _isTimeFilterVisible.value = visible
    }

    fun updateTimeFilterInputs(start: String, end: String) {
        _startTimeFilterInput.value = start
        _endTimeFilterInput.value = end
    }

    fun applyTimeFilter(startTime: String, endTime: String) {
        val session = _reviewSession.value ?: return
        val points = session.points
        if (points.isEmpty()) return

        val strings = LocalizationManager.strings

        fun parseInputToSeconds(input: String): Int? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null
            val parts = trimmed.split(":", ".", " ", "-")
            return try {
                val hour: Int
                val minute: Int
                if (parts.size == 1) {
                    val v = parts[0].toInt()
                    if (v in 0..24) { hour = v; minute = 0 }
                    else { hour = v / 100; minute = v % 100 }
                } else {
                    hour = parts[0].toInt()
                    val mPart = parts[1]
                    // Fix: Treated single digit as literal minute (e.g. "3" -> 03), not 10x (30)
                    minute = mPart.toInt()
                }
                if (hour in 0..23 && minute in 0..59) {
                    hour * 3600 + minute * 60
                } else null
            } catch (e: Exception) { null }
        }

        val targetStartSecs = parseInputToSeconds(startTime)
        val targetEndSecs = parseInputToSeconds(endTime)

        if (targetStartSecs == null || targetEndSecs == null) {
            NotificationManager.show(strings.invalidTimeFormat, NotificationType.ERROR)
            return
        }

        if (targetStartSecs >= targetEndSecs) {
            NotificationManager.show(strings.startTimeAfterEndTime, NotificationType.ERROR)
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val allPoints = points.sortedBy { it.timestamp ?: it.receivedAt }
                
                fun getLocalPointSecs(p: com.example.geosync.network.HistoryPoint): Int {
                    val ts = p.timestamp ?: p.receivedAt
                    if (ts == null || ts.isBlank()) return -1
                    
                    // Handle Unix timestamp (number as string)
                    if (ts.all { it.isDigit() || it == '.' }) {
                        return try {
                            val seconds = ts.toDouble().toLong()
                            val instant = Instant.fromEpochSeconds(seconds)
                            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                            local.hour * 3600 + local.minute * 60 + local.second
                        } catch (e: Exception) { -1 }
                    }

                    return try {
                        val iso = ts.replace(" ", "T").let { 
                            if (!it.contains("+") && !it.endsWith("Z")) "${it}Z" else it 
                        }
                        val inst = Instant.parse(iso)
                        val local = inst.toLocalDateTime(TimeZone.currentSystemDefault())
                        local.hour * 3600 + local.minute * 60 + local.second
                    } catch (e: Exception) { -1 }
                }

                val sessionStartSecs = getLocalPointSecs(allPoints.first())
                val sessionEndSecs = getLocalPointSecs(allPoints.last())

                // "In Range" Check with midnight-crossing awareness
                if (sessionStartSecs != -1 && sessionEndSecs != -1) {
                    val sStart = sessionStartSecs
                    val sEnd = if (sessionEndSecs < sessionStartSecs) sessionEndSecs + 86400 else sessionEndSecs
                    
                    var rStart = targetStartSecs
                    var rEnd = if (targetEndSecs < targetStartSecs) targetEndSecs + 86400 else targetEndSecs
                    
                    // If the requested range seems to be shifted by a day (e.g. session 23:00-01:00, req 23:30-00:30)
                    // We need to normalize them.
                    if (rEnd < sStart && (rEnd + 86400) >= sStart) {
                        rStart += 86400
                        rEnd += 86400
                    }

                    // Strict range check: the requested window must be WITHIN the session
                    if (rStart < sStart || rEnd > sEnd) {
                        withContext(Dispatchers.Main) {
                            NotificationManager.show(strings.timeRangeOutOfSession, NotificationType.ERROR)
                        }
                        return@launch
                    }
                }

                var startIdx = 0
                var endIdx = allPoints.size - 1
                var minStartDiff = Int.MAX_VALUE
                var minEndDiff = Int.MAX_VALUE
                
                var foundStartLocalSecs = -1
                var foundEndLocalSecs = -1

                // Nearest Point Algorithm
                for (i in allPoints.indices) {
                    val pSecs = getLocalPointSecs(allPoints[i])
                    if (pSecs == -1) continue
                    
                    val sDiff = kotlin.math.abs(pSecs - targetStartSecs)
                    if (sDiff < minStartDiff) {
                        minStartDiff = sDiff
                        startIdx = i
                        foundStartLocalSecs = pSecs
                    }
                    
                    val eDiff = kotlin.math.abs(pSecs - targetEndSecs)
                    if (eDiff < minEndDiff) {
                        minEndDiff = eDiff
                        endIdx = i
                        foundEndLocalSecs = pSecs
                    }
                }

                withContext(Dispatchers.Main) {
                    if (foundStartLocalSecs != -1 && foundEndLocalSecs != -1) {
                        val total = (allPoints.size - 1).toFloat().coerceAtLeast(1.0f)
                        val newStart = (startIdx.toFloat() / total).coerceIn(0f, 1f)
                        val newEnd = (endIdx.toFloat() / total).coerceIn(newStart, 1f)
                        
                        _reviewRange.value = newStart..newEnd
                        
                        val finalStartTs = allPoints[startIdx].timestamp ?: allPoints[startIdx].receivedAt
                        val finalEndTs = allPoints[endIdx].timestamp ?: allPoints[endIdx].receivedAt
                        
                        _startTimeFilterInput.value = AdminUtils.formatToLocalTime(finalStartTs)
                        _endTimeFilterInput.value = AdminUtils.formatToLocalTime(finalEndTs)
                        
                        _reviewSession.update { it?.copy(sessionTag = "f_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}") }
                        _reviewFocusTrigger.value += 1
                        
                        NotificationManager.show(strings.timeFilterApplied, NotificationType.SUCCESS)
                    } else {
                        NotificationManager.show(strings.noDataInTimeRange, NotificationType.ERROR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    NotificationManager.show(strings.failedToApplyTimeFilter(e.message), NotificationType.ERROR)
                }
            }
        }
    }

    fun handleMapInteraction() {
        if (!_isTimelinePinned.value && !_isTimelineMinimized.value) {
            _isTimelineMinimized.value = true
        }
    }

    fun updateReviewCameraState(state: MapCameraState) {
        _reviewCameraState.value = state
    }

    override fun onCleared() {
        super.onCleared()
        connectionJob?.cancel()
    }
}
