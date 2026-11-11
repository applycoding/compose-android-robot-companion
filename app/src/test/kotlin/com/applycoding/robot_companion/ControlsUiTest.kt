// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.ManualTickSource
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.Sim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
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
    val composeRule = createAndroidComposeRule<HostActivity>()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun estopLatchesAndRenders() {
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined)
        HostActivity.testTransportFactory = TransportFactory { _, _ ->
            val sim = Sim()
            InProcessTransport(
                sim = sim,
                tickSource = ManualTickSource { sim.step() },
                scope = scope,
                realtime = false,
            )
        }
        composeRule.activityRule.scenario.recreate()

        val vm = composeRule.activity.robotViewModel
        val transport = vm.transportForTests() as InProcessTransport
        runBlocking {
            transport.send(Command.Heartbeat)
            transport.send(DemoMissions.startCommand())
        }
        runToTick(transport, 100)
        composeRule.waitForIdle()

        runBlocking {
            transport.send(Command.Heartbeat)
            transport.send(Command.EStop)
            var tick = transport.stepOnce().tick
            while (tick < 113) {
                if (tick % 5 == 0) {
                    transport.send(Command.Heartbeat)
                }
                tick = transport.stepOnce().tick
            }
        }
        vm.syncFromTransportForTest()
        assertEquals(113, transport.snapshots.value?.tick)
        assertEquals(113, vm.uiState.value.telemetry?.tick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("estop_button").performScrollTo()
        composeRule.onNodeWithTag("estop_button").assertExists()
        composeRule.onNodeWithText("E-STOP LATCHED", substring = true).assertExists()
        composeRule.onNodeWithText("ABORTED").assertExists()
        composeRule.onNodeWithTag("reset_estop_button").assertIsEnabled()
        composeRule.onNodeWithTag("estop_button").performClick()

        vm.onEvent(UiEvent.Start)
        composeRule.waitForIdle()
        assert(vm.uiState.value.snackbarMessage == "ESTOP_LATCHED")
        assert(vm.uiState.value.telemetry?.mission == MissionStatus.ABORTED)
    }

    private fun runToTick(transport: InProcessTransport, target: Int) {
        runBlocking {
            var tick = transport.snapshots.value?.tick ?: 0
            while (tick < target) {
                if (tick % 5 == 0) {
                    transport.send(Command.Heartbeat)
                }
                tick = transport.stepOnce().tick
            }
        }
    }
}
