// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import androidx.lifecycle.SavedStateHandle
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.ManualTickSource
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.Reject
import com.applycoding.robotcompanion.sim.Sim
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithDemoCatalogAndTransportInApp() = runTest {
        val vm = viewModel(this)
        val state = vm.uiState.value
        assertEquals(TransportMode.IN_APP, state.transportMode)
        assertEquals(DemoMissions.DEMO_ID, state.selectedMissionId)
        assertEquals(1, state.missions.size)
        assertEquals(DemoMissions.DEMO_NAME, state.missions.first().name)
    }

    @Test
    fun estopLatchReflectsInStateAfterManualTicks() = runTest {
        val vm = viewModel(this)
        val transport = vm.transportForTests() as InProcessTransport
        transport.send(Command.Heartbeat)
        transport.send(DemoMissions.startCommand())
        repeat(100) {
            if (it % 5 == 0) {
                transport.send(Command.Heartbeat)
            }
            transport.stepOnce()
        }
        transport.send(Command.EStop)
        repeat(13) { transport.stepOnce() }
        val tel = vm.uiState.value.telemetry
        assertEquals(113, tel?.tick)
        assertEquals(MissionStatus.ABORTED, tel?.mission)
        assertEquals(true, tel?.estopLatched)
    }

    @Test
    fun startRejectedWhileLatchedSetsSnackbar() = runTest {
        val vm = viewModel(this)
        val transport = vm.transportForTests() as InProcessTransport
        transport.send(Command.EStop)
        vm.onEvent(UiEvent.Start)
        transport.stepOnce()
        assertEquals(Reject.ESTOP_LATCHED.name, vm.uiState.value.snackbarMessage)
    }

    @Test
    fun editorUpdatesPersistInHandle() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(this, handle)
        vm.onEvent(UiEvent.UpdateEditorName("Custom name"))
        val json = handle.get<String>("editor_draft")
        assertEquals(true, json?.contains("Custom name"))
    }

  private fun viewModel(testScope: kotlinx.coroutines.test.TestScope, handle: SavedStateHandle = SavedStateHandle()): RobotViewModel {
        val dispatcher = UnconfinedTestDispatcher(testScope.testScheduler)
        val scope = CoroutineScope(dispatcher)
        lateinit var sim: Sim
        val factory = TransportFactory { _, _ ->
            sim = Sim()
            InProcessTransport(
                sim = sim,
                tickSource = ManualTickSource { sim.step() },
                scope = scope,
                realtime = false,
            )
        }
        return RobotViewModel(handle, factory, heartbeatEnabled = false)
    }
}
