// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.applycoding.robot_companion.ui.Banner
import com.applycoding.robot_companion.ui.ControlRow
import com.applycoding.robot_companion.ui.EStopButton
import com.applycoding.robot_companion.ui.MapPanel
import com.applycoding.robot_companion.ui.MissionEditor
import com.applycoding.robot_companion.ui.MissionList
import com.applycoding.robot_companion.ui.TelemetryBar

@Composable
fun CompanionApp(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val snackbarHostState = remember { SnackbarHostState() }
        val message = uiState.snackbarMessage
        LaunchedEffect(message) {
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                onEvent(UiEvent.DismissSnackbar)
            }
        }
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(modifier = Modifier, action = data.visuals.actionLabel?.let {
                        {
                            androidx.compose.material3.TextButton(onClick = { data.dismiss() }) {
                                Text(it)
                            }
                        }
                    }) {
                        Text(data.visuals.message)
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .then(Modifier),
            ) {
                Banner()
                TransportToggle(uiState.transportMode, onEvent)
                TelemetryBar(telemetry = uiState.telemetry)
                val waypointPairs = uiState.editorDraft.waypoints.mapNotNull { row ->
                    val x = row.x.toDoubleOrNull()
                    val y = row.y.toDoubleOrNull()
                    if (x != null && y != null) x to y else null
                }
                MapPanel(
                    telemetry = uiState.telemetry,
                    waypoints = if (waypointPairs.isNotEmpty()) waypointPairs else uiState.activeMissionWaypoints,
                )
                MissionList(
                    missions = uiState.missions,
                    selectedMissionId = uiState.selectedMissionId,
                    onSelect = { onEvent(UiEvent.SelectMission(it)) },
                )
                MissionEditor(draft = uiState.editorDraft, onEvent = onEvent)
                ControlRow(telemetry = uiState.telemetry, onEvent = onEvent)
                EStopButton(
                    latched = uiState.telemetry?.estopLatched == true,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun TransportToggle(
    mode: TransportMode,
    onEvent: (UiEvent) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier) {
        TransportMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = TransportMode.entries.size),
                onClick = { onEvent(UiEvent.SetTransportMode(entry)) },
                selected = mode == entry,
            ) {
                val label = when (entry) {
                    TransportMode.IN_APP -> stringResource(R.string.transport_in_app)
                    TransportMode.SIM_SERVER -> stringResource(R.string.transport_server)
                }
                Text(label)
            }
        }
    }
}
