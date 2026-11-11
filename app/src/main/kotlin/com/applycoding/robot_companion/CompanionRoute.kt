// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CompanionRoute(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompanionApp(
        uiState = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
