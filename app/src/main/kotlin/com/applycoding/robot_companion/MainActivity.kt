// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: RobotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.bindHeartbeat(this)
                try {
                    awaitCancellation()
                } finally {
                    viewModel.unbindHeartbeat()
                }
            }
        }
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompanionApp(
                uiState = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
