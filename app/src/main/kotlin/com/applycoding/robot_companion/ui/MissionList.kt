// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.applycoding.robot_companion.MissionEntry

@Composable
fun MissionList(
    missions: List<MissionEntry>,
    selectedMissionId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Text(text = "Missions", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(missions, key = { it.id }) { mission ->
                val selected = mission.id == selectedMissionId
                val suffix = buildString {
                    append(" • ")
                    append(mission.waypoints.size)
                    append(" wp")
                    if (selected) append(" • selected")
                    mission.lastResult?.let { append(" • ").append(it) }
                }
                Text(
                    text = mission.name + suffix,
                    style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(mission.id) }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
