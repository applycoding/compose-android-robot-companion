// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import com.applycoding.robotcompanion.sim.Telemetry
import kotlinx.serialization.Serializable

enum class TransportMode {
    IN_APP,
    SIM_SERVER,
}

@Serializable
data class WaypointRow(val x: String, val y: String)

@Serializable
data class MissionEntry(
    val id: String,
    val name: String,
    val waypoints: List<WaypointRow>,
    val lastResult: String? = null,
)

@Serializable
data class EditorDraft(
    val name: String = "",
    val waypoints: List<WaypointRow> = listOf(WaypointRow("1.0", "1.0")),
)

data class UiState(
    val telemetry: Telemetry? = null,
    val missions: List<MissionEntry> = emptyList(),
    val selectedMissionId: String? = null,
    val editorDraft: EditorDraft = EditorDraft(),
    val transportMode: TransportMode = TransportMode.IN_APP,
    val snackbarMessage: String? = null,
    val connected: Boolean = false,
    val activeMissionWaypoints: List<Pair<Double, Double>> = emptyList(),
)

sealed interface UiEvent {
    data class SelectMission(val id: String) : UiEvent
    data class UpdateEditorName(val name: String) : UiEvent
    data class UpdateWaypoint(val index: Int, val x: String, val y: String) : UiEvent
    data object AddWaypoint : UiEvent
    data class RemoveWaypoint(val index: Int) : UiEvent
    data object SaveMissionToCatalog : UiEvent
    data object Start : UiEvent
    data object Pause : UiEvent
    data object Resume : UiEvent
    data object Cancel : UiEvent
    data object EStop : UiEvent
    data object ResetEStop : UiEvent
    data class SetTransportMode(val mode: TransportMode) : UiEvent
    data object DismissSnackbar : UiEvent
}
