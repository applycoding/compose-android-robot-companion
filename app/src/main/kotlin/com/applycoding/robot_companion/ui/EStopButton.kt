// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applycoding.robot_companion.R
import com.applycoding.robot_companion.UiEvent

@Composable
fun EStopButton(
    latched: Boolean,
    onEvent: (UiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (latched) {
        stringResource(R.string.estop_latched)
    } else {
        stringResource(R.string.estop)
    }
    Button(
        onClick = { onEvent(UiEvent.EStop) },
        enabled = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("estop_button"),
        colors = ButtonDefaults.buttonColors(),
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
    }
}
