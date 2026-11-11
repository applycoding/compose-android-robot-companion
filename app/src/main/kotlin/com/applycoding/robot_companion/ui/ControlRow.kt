// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.applycoding.robot_companion.R
import com.applycoding.robot_companion.UiEvent
import com.applycoding.robotcompanion.sim.Telemetry

@Composable
fun ControlRow(
    telemetry: Telemetry?,
    onEvent: (UiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resetEnabled = telemetry?.estopLatched == true &&
        telemetry.v == 0.0 &&
        telemetry.linkOk
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Button(
            onClick = { onEvent(UiEvent.Start) },
            modifier = Modifier
                .weight(1f)
                .testTag("start_button"),
        ) {
            Text(stringResource(R.string.start))
        }
        TextButton(onClick = { onEvent(UiEvent.Pause) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.pause))
        }
        TextButton(onClick = { onEvent(UiEvent.Resume) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.resume))
        }
        TextButton(onClick = { onEvent(UiEvent.Cancel) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.cancel))
        }
        Button(
            onClick = { onEvent(UiEvent.ResetEStop) },
            enabled = resetEnabled,
            modifier = Modifier
                .weight(1f)
                .testTag("reset_estop_button"),
        ) {
            Text(stringResource(R.string.reset_estop))
        }
    }
}
