// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.ManualTickSource
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.Sim
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ControlsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun estopLatchesAndRenders() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val sim = Sim()
        val transport = InProcessTransport(
            sim = sim,
            tickSource = ManualTickSource { sim.step() },
            scope = scope,
            realtime = false,
        )
        val factory = TransportFactory { _, _ -> transport }
        val vm = RobotViewModel(SavedStateHandle(), factory, heartbeatEnabled = false)

        composeRule.setContent {
            androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
            val state = vm.uiState.collectAsStateWithLifecycle()
            CompanionApp(uiState = state.value, onEvent = vm::onEvent)
        }

        runBlocking {
            transport.send(Command.Heartbeat)
            transport.send(DemoMissions.startCommand())
        }
        runToTick(transport, 100)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("estop_button").performClick()
        runBlocking {
            repeat(13) { transport.stepOnce() }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("E-STOP LATCHED").assertExists()
        composeRule.onNodeWithText("tick 113").assertExists()
        composeRule.onNodeWithText("ABORTED").assertExists()
        composeRule.onNodeWithTag("reset_estop_button").assertIsEnabled()

        composeRule.onNodeWithTag("start_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ESTOP_LATCHED").assertExists()
        val tel = vm.uiState.value.telemetry
        assert(tel?.tick == 113)
        assert(tel?.mission == MissionStatus.ABORTED)
    }

    private fun runToTick(transport: InProcessTransport, target: Int) {
        runBlocking {
            var tick = transport.snapshots.value?.tick ?: 0
            while (tick < target) {
                if (tick % 5 == 0) {
                    transport.send(Command.Heartbeat)
                }
                transport.stepOnce()
                tick = transport.snapshots.value?.tick ?: tick
            }
        }
    }
}
