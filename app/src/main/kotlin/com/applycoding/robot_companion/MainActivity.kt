// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: RobotViewModel by viewModels(
        factoryProducer = { RobotViewModelFactory(this) },
    )

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
            CompanionRoute(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
