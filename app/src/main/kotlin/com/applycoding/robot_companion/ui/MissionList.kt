// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
        missions.forEach { mission ->
            val selected = mission.id == selectedMissionId
            val suffix = buildString {
                append(" - ")
                append(mission.waypoints.size)
                append(" wp")
                if (mission.lastResult != null) {
                    append(", ")
                    append(mission.lastResult)
                }
            }
            Text(
                text = mission.name + suffix,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mission.id) }
                    .padding(vertical = 6.dp),
                style = if (selected) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
