// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applycoding.robotcompanion.link.HeartbeatSender
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.LinkEvent
import com.applycoding.robotcompanion.link.Transport
import com.applycoding.robotcompanion.link.WebSocketTransport
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.Reject
import com.applycoding.robotcompanion.sim.Sim
import com.applycoding.robotcompanion.sim.SimConfig
import com.applycoding.robotcompanion.sim.Telemetry
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val KEY_CATALOG = "mission_catalog"
private const val KEY_SELECTED = "selected_mission_id"
private const val KEY_EDITOR = "editor_draft"
private const val KEY_TRANSPORT = "transport_mode"
private const val SERVER_URL = "ws://127.0.0.1:8787/v1/robot"
private const val MAX_MISSIONS = 16

private val stateJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun interface TransportFactory {
    fun create(scope: kotlinx.coroutines.CoroutineScope, mode: TransportMode): Transport
}

object DefaultTransportFactory : TransportFactory {
    override fun create(scope: kotlinx.coroutines.CoroutineScope, mode: TransportMode): Transport {
        return when (mode) {
            TransportMode.IN_APP -> InProcessTransport(Sim(), tickSource = null, scope = scope, realtime = true)
            TransportMode.SIM_SERVER -> {
                val client = HttpClient(CIO) { install(WebSockets) }
                WebSocketTransport(SERVER_URL, client, scope)
            }
        }
    }
}

class RobotViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val transportFactory: TransportFactory = DefaultTransportFactory,
) : ViewModel() {
    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var transport: Transport = openTransport(_uiState.value.transportMode)
    private var heartbeatSender: HeartbeatSender? = null
    private var activeWaypoints: List<Pair<Double, Double>> = emptyList()
    private var snapshotJob: Job? = null
    private var eventsJob: Job? = null

    init {
        attachTransportCollectors(transport)
    }

    fun bindHeartbeat(lifecycleScope: kotlinx.coroutines.CoroutineScope) {
        heartbeatSender?.stop()
        heartbeatSender = HeartbeatSender(transport, lifecycleScope)
        heartbeatSender?.start()
    }

    fun unbindHeartbeat() {
        heartbeatSender?.stop()
        heartbeatSender = null
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.SelectMission -> selectMission(event.id)
            is UiEvent.UpdateEditorName -> updateEditorName(event.name)
            is UiEvent.UpdateWaypoint -> updateWaypoint(event.index, event.x, event.y)
            UiEvent.AddWaypoint -> addWaypoint()
            is UiEvent.RemoveWaypoint -> removeWaypoint(event.index)
            UiEvent.SaveMissionToCatalog -> saveMissionToCatalog()
            UiEvent.Start -> startMission()
            UiEvent.Pause -> sendSimple(Command.Pause)
            UiEvent.Resume -> sendSimple(Command.Resume)
            UiEvent.Cancel -> sendSimple(Command.Cancel)
            UiEvent.EStop -> sendSimple(Command.EStop)
            UiEvent.ResetEStop -> sendSimple(Command.ResetEStop)
            is UiEvent.SetTransportMode -> setTransportMode(event.mode)
            UiEvent.DismissSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    fun transportForTests(): Transport = transport

    fun syncFromTransportForTest() {
        onSnapshot(transport.snapshots.value)
    }

    override fun onCleared() {
        unbindHeartbeat()
        detachTransportCollectors()
        transport.close()
        super.onCleared()
    }

    private fun buildInitialState(): UiState {
        val defaultCatalog = defaultCatalog()
        val catalog = savedStateHandle.get<String>(KEY_CATALOG)?.let {
            stateJson.decodeFromString(ListSerializer(MissionEntry.serializer()), it)
        } ?: defaultCatalog
        val selected = savedStateHandle.get<String>(KEY_SELECTED) ?: DemoMissions.DEMO_ID
        val editor = savedStateHandle.get<String>(KEY_EDITOR)?.let {
            stateJson.decodeFromString(EditorDraft.serializer(), it)
        } ?: catalog.firstOrNull { it.id == selected }?.toEditorDraft() ?: EditorDraft(
            name = DemoMissions.DEMO_NAME,
            waypoints = DemoMissions.waypoints.map { WaypointRow(it.first.toString(), it.second.toString()) },
        )
        val transportMode = savedStateHandle.get<String>(KEY_TRANSPORT)?.let { TransportMode.valueOf(it) }
            ?: TransportMode.IN_APP
        return UiState(
            missions = catalog,
            selectedMissionId = selected,
            editorDraft = editor,
            transportMode = transportMode,
        )
    }

    private fun defaultCatalog(): List<MissionEntry> = listOf(
        MissionEntry(
            id = DemoMissions.DEMO_ID,
            name = DemoMissions.DEMO_NAME,
            waypoints = DemoMissions.waypoints.map { WaypointRow(it.first.toString(), it.second.toString()) },
        ),
    )

    private fun persistCatalog(missions: List<MissionEntry>) {
        savedStateHandle[KEY_CATALOG] = stateJson.encodeToString(ListSerializer(MissionEntry.serializer()), missions)
    }

    private fun persistSelected(id: String?) {
        if (id != null) {
            savedStateHandle[KEY_SELECTED] = id
        } else {
            savedStateHandle.remove<String>(KEY_SELECTED)
        }
    }

    private fun persistEditor(draft: EditorDraft) {
        savedStateHandle[KEY_EDITOR] = stateJson.encodeToString(EditorDraft.serializer(), draft)
    }

    private fun persistTransport(mode: TransportMode) {
        savedStateHandle[KEY_TRANSPORT] = mode.name
    }

    private fun openTransport(mode: TransportMode): Transport {
        return transportFactory.create(viewModelScope, mode)
    }

    private fun setTransportMode(mode: TransportMode) {
        if (mode == _uiState.value.transportMode) return
        unbindHeartbeat()
        detachTransportCollectors()
        transport.close()
        transport = openTransport(mode)
        attachTransportCollectors(transport)
        persistTransport(mode)
        _uiState.update { it.copy(transportMode = mode, connected = false, telemetry = null) }
    }

    private fun attachTransportCollectors(active: Transport) {
        snapshotJob = viewModelScope.launch {
            active.snapshots.collect { tel -> onSnapshot(tel) }
        }
        eventsJob = viewModelScope.launch {
            active.events.collect { event -> onLinkEvent(event) }
        }
    }

    private fun detachTransportCollectors() {
        snapshotJob?.cancel()
        eventsJob?.cancel()
        snapshotJob = null
        eventsJob = null
    }

    private fun onSnapshot(tel: Telemetry?) {
        _uiState.update { state ->
            state.copy(
                telemetry = tel,
                activeMissionWaypoints = if (tel != null) activeWaypoints else state.activeMissionWaypoints,
            )
        }
    }

    private fun onLinkEvent(event: LinkEvent) {
        when (event) {
            LinkEvent.Connected -> _uiState.update { it.copy(connected = true) }
            LinkEvent.Disconnected -> _uiState.update { it.copy(connected = false) }
            is LinkEvent.Ack -> handleAck(event)
            is LinkEvent.Error -> _uiState.update { it.copy(snackbarMessage = event.code) }
        }
    }

    private fun handleAck(ack: LinkEvent.Ack) {
        val resultText = if (ack.result == Reject.NONE) "NONE" else ack.result.name
        val selected = _uiState.value.selectedMissionId
        val missions = _uiState.value.missions.map { entry ->
            if (entry.id == selected) entry.copy(lastResult = resultText) else entry
        }
        if (missions != _uiState.value.missions) {
            persistCatalog(missions)
        }
        _uiState.update {
            it.copy(
                missions = missions,
                snackbarMessage = if (ack.result == Reject.NONE) it.snackbarMessage else ack.result.name,
            )
        }
    }

    private fun selectMission(id: String) {
        persistSelected(id)
        val mission = _uiState.value.missions.firstOrNull { it.id == id }
        val draft = mission?.toEditorDraft() ?: _uiState.value.editorDraft
        persistEditor(draft)
        _uiState.update { it.copy(selectedMissionId = id, editorDraft = draft) }
    }

    private fun updateEditorName(name: String) {
        val draft = _uiState.value.editorDraft.copy(name = name)
        persistEditor(draft)
        _uiState.update { it.copy(editorDraft = draft) }
    }

    private fun updateWaypoint(index: Int, x: String, y: String) {
        val rows = _uiState.value.editorDraft.waypoints.toMutableList()
        if (index !in rows.indices) return
        rows[index] = WaypointRow(x, y)
        val draft = _uiState.value.editorDraft.copy(waypoints = rows)
        persistEditor(draft)
        _uiState.update { it.copy(editorDraft = draft) }
    }

    private fun addWaypoint() {
        val cfg = SimConfig.standard
        val rows = _uiState.value.editorDraft.waypoints.toMutableList()
        if (rows.size >= cfg.maxWaypoints) return
        rows.add(WaypointRow("5.0", "4.0"))
        val draft = _uiState.value.editorDraft.copy(waypoints = rows)
        persistEditor(draft)
        _uiState.update { it.copy(editorDraft = draft) }
    }

    private fun removeWaypoint(index: Int) {
        val rows = _uiState.value.editorDraft.waypoints.toMutableList()
        if (rows.size <= 1 || index !in rows.indices) return
        rows.removeAt(index)
        val draft = _uiState.value.editorDraft.copy(waypoints = rows)
        persistEditor(draft)
        _uiState.update { it.copy(editorDraft = draft) }
    }

    private fun saveMissionToCatalog() {
        val selected = _uiState.value.selectedMissionId ?: return
        val draft = _uiState.value.editorDraft
        val updated = MissionEntry(selected, draft.name, draft.waypoints, lastResult = null)
        val missions = _uiState.value.missions.toMutableList()
        val idx = missions.indexOfFirst { it.id == selected }
        if (idx >= 0) {
            missions[idx] = updated
        } else if (missions.size < MAX_MISSIONS) {
            missions.add(updated)
        }
        persistCatalog(missions)
        _uiState.update { it.copy(missions = missions) }
    }

    private fun startMission() {
        val selected = _uiState.value.selectedMissionId ?: return
        val draft = _uiState.value.editorDraft
        val parsed = parseWaypoints(draft.waypoints)
        if (parsed == null) {
            _uiState.update { it.copy(snackbarMessage = Reject.INVALID_MISSION.name) }
            return
        }
        if (!validateClientBounds(parsed)) {
            _uiState.update { it.copy(snackbarMessage = Reject.INVALID_MISSION.name) }
            return
        }
        activeWaypoints = parsed
        viewModelScope.launch {
            transport.send(Command.StartMission(selected, draft.name.ifBlank { "Mission" }, parsed))
        }
    }

    private fun sendSimple(cmd: Command) {
        viewModelScope.launch { transport.send(cmd) }
    }

    private fun parseWaypoints(rows: List<WaypointRow>): List<Pair<Double, Double>>? {
        val out = mutableListOf<Pair<Double, Double>>()
        for (row in rows) {
            val x = row.x.toDoubleOrNull() ?: return null
            val y = row.y.toDoubleOrNull() ?: return null
            if (!x.isFinite() || !y.isFinite()) return null
            out.add(x to y)
        }
        return out
    }

    private fun validateClientBounds(waypoints: List<Pair<Double, Double>>): Boolean {
        val cfg = SimConfig.standard
        if (waypoints.isEmpty() || waypoints.size > cfg.maxWaypoints) return false
        return waypoints.all { (x, y) ->
            x in cfg.marginMinX..cfg.marginMaxX && y in cfg.marginMinY..cfg.marginMaxY
        }
    }
}

private fun MissionEntry.toEditorDraft(): EditorDraft = EditorDraft(name = name, waypoints = waypoints)
