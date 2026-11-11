// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

class HostActivity : ComponentActivity() {
    companion object {
        var testTransportFactory: TransportFactory? = null
    }

    private val viewModel: RobotViewModel by viewModels(
        factoryProducer = {
            RobotViewModelFactory(
                this,
                testTransportFactory ?: DefaultTransportFactory,
            )
        },
    )

    val robotViewModel: RobotViewModel
        get() = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompanionRoute(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
