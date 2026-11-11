// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.applycoding.robot_companion.EditorDraft
import com.applycoding.robot_companion.R
import com.applycoding.robot_companion.UiEvent

@Composable
fun MissionEditor(
    draft: EditorDraft,
    onEvent: (UiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Mission editor")
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onEvent(UiEvent.UpdateEditorName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            singleLine = true,
        )
        draft.waypoints.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.x,
                    onValueChange = { onEvent(UiEvent.UpdateWaypoint(index, it, row.y)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("x") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = row.y,
                    onValueChange = { onEvent(UiEvent.UpdateWaypoint(index, row.x, it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("y") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                if (draft.waypoints.size > 1) {
                    TextButton(onClick = { onEvent(UiEvent.RemoveWaypoint(index)) }) {
                        Text(stringResource(R.string.remove_waypoint))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onEvent(UiEvent.AddWaypoint) }) {
                Text(stringResource(R.string.add_waypoint))
            }
            Button(onClick = { onEvent(UiEvent.SaveMissionToCatalog) }) {
                Text("Save to catalog")
            }
        }
    }
}
