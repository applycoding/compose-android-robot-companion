// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.os.Bundle
import android.os.Parcel
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.ManualTickSource
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ProcessDeathTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        HostActivity.testTransportFactory = null
    }

    @Test
    fun catalogSurvives() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        HostActivity.testTransportFactory = transportFactory(scope)

        val first = Robolectric.buildActivity(HostActivity::class.java).setup()
        val vm1 = first.get().robotViewModel
        vm1.onEvent(UiEvent.SelectMission("patrol_b"))
        vm1.onEvent(UiEvent.UpdateEditorName("Night patrol"))
        vm1.onEvent(UiEvent.SaveMissionToCatalog)
        vm1.onEvent(UiEvent.SetTransportMode(TransportMode.SIM_SERVER))

        val saved = Bundle()
        first.saveInstanceState(saved)
        first.pause().stop().destroy()

        HostActivity.testTransportFactory = transportFactory(CoroutineScope(UnconfinedTestDispatcher()))
        val restoredBundle = parcelCopy(saved)
        val second = Robolectric.buildActivity(HostActivity::class.java)
            .create(restoredBundle)
            .start()
            .resume()
        val vm2 = second.get().robotViewModel

        val state = vm2.uiState.value
        assertEquals("Night patrol", state.editorDraft.name)
        assertEquals("patrol_b", state.selectedMissionId)
        assertEquals(TransportMode.SIM_SERVER, state.transportMode)
        assertEquals("Night patrol", state.missions.first { it.id == "patrol_b" }.name)

        val tel = state.telemetry
        assertEquals(0, tel?.tick)
        assertFalse(tel?.estopLatched == true)
        assertEquals(MissionStatus.IDLE, tel?.mission)
    }

    private fun parcelCopy(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        bundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val copy = Bundle.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        return copy
    }

    private fun transportFactory(scope: CoroutineScope): TransportFactory = TransportFactory { _, _ ->
        val sim = Sim()
        InProcessTransport(
            sim = sim,
            tickSource = ManualTickSource { sim.step() },
            scope = scope,
            realtime = false,
        )
    }
}
