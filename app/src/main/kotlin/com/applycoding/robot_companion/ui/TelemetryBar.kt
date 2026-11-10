// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.applycoding.robot_companion.R
import com.applycoding.robotcompanion.sim.Telemetry
import java.util.Locale

@Composable
fun TelemetryBar(
    telemetry: Telemetry?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val tick = telemetry?.tick ?: 0
        Text(
            text = String.format(Locale.ROOT, "tick %d", tick),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("telemetry_tick"),
        )
        val pose = telemetry?.let {
            String.format(Locale.ROOT, "pose (%.2f, %.2f) θ %.2f", it.x, it.y, it.theta)
        } ?: "pose —"
        val speed = telemetry?.let {
            String.format(Locale.ROOT, "speed %.3f", it.v)
        } ?: "speed —"
        val battery = telemetry?.let {
            String.format(Locale.ROOT, "battery %.1f%%", it.batteryPct)
        } ?: "battery —"
        Text(text = pose, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = speed, style = MaterialTheme.typography.bodyMedium)
            Text(text = battery, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val linkLabel = if (telemetry?.linkOk == true) {
                stringResource(R.string.link_ok)
            } else {
                stringResource(R.string.link_lost)
            }
            AssistChip(
                onClick = {},
                label = { Text(linkLabel) },
                modifier = Modifier.testTag("link_chip"),
            )
            val mission = telemetry?.mission?.name ?: "IDLE"
            val pause = telemetry?.pause?.name
            val status = if (pause != null) "$mission ($pause)" else mission
            AssistChip(
                onClick = {},
                label = { Text(status) },
                modifier = Modifier.testTag("mission_status_chip"),
            )
        }
    }
}
