// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.ManualTickSource
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.Sim
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ProcessDeathTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun catalogSurvives() = runTest {
        val handle1 = SavedStateHandle(emptyMap())
        val vm1 = viewModel(this, handle1)
        vm1.onEvent(UiEvent.SelectMission(DemoMissions.DEMO_ID))
        vm1.onEvent(UiEvent.UpdateEditorName("Edited patrol"))
        vm1.onEvent(UiEvent.SaveMissionToCatalog)
        vm1.onEvent(UiEvent.SetTransportMode(TransportMode.SIM_SERVER))

        val bundle = Bundle()
        handle1.keys().forEach { key ->
            handle1.get<String>(key)?.let { bundle.putString(key, it) }
        }

        val parcel = Parcel.obtain()
        bundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = Bundle.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        val map = restored.keySet().associateWith { restored.getString(it) as Any }
        val handle2 = SavedStateHandle(map)
        val vm2 = viewModel(this, handle2)

        val state = vm2.uiState.value
        assertEquals("Edited patrol", state.editorDraft.name)
        assertEquals(DemoMissions.DEMO_ID, state.selectedMissionId)
        assertEquals(TransportMode.SIM_SERVER, state.transportMode)
        assertEquals("Edited patrol", state.missions.first { it.id == DemoMissions.DEMO_ID }.name)

        val tel = state.telemetry
        assertEquals(0, tel?.tick)
        assertFalse(tel?.estopLatched == true)
        assertEquals(MissionStatus.IDLE, tel?.mission)
    }

    private fun viewModel(testScope: kotlinx.coroutines.test.TestScope, handle: SavedStateHandle): RobotViewModel {
        val dispatcher = UnconfinedTestDispatcher(testScope.testScheduler)
        val scope = CoroutineScope(dispatcher)
        val factory = TransportFactory { _, _ ->
            val sim = Sim()
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
