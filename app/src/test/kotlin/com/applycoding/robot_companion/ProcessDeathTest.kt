// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessDeathTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun catalogSurvives() = runTest {
        val owner1 = RegistryOwner()
        val handle1 = SavedStateHandle()
        val vm1 = viewModel(this, handle1)
        vm1.onEvent(UiEvent.UpdateEditorName("Edited patrol"))
        vm1.onEvent(UiEvent.SelectMission(DemoMissions.DEMO_ID))
        vm1.onEvent(UiEvent.SetTransportMode(TransportMode.SIM_SERVER))

        val bundle = Bundle()
        mergeHandleIntoBundle(handle1, bundle)
        owner1.save(bundle)

        val parcel = Parcel.obtain()
        bundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = Bundle.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        val owner2 = RegistryOwner()
        owner2.restore(restored)
        val handle2 = handleFromBundle(restored)
        val vm2 = viewModel(this, handle2)

        val state = vm2.uiState.value
        assertEquals("Edited patrol", state.editorDraft.name)
        assertEquals(DemoMissions.DEMO_ID, state.selectedMissionId)
        assertEquals(TransportMode.SIM_SERVER, state.transportMode)
        assertEquals(DemoMissions.DEMO_NAME, state.missions.first().name)

        val tel = state.telemetry
        assertEquals(0, tel?.tick)
        assertFalse(tel?.estopLatched == true)
        assertEquals(MissionStatus.IDLE, tel?.mission)
    }

    private fun mergeHandleIntoBundle(handle: SavedStateHandle, bundle: Bundle) {
        handle.keys().forEach { key ->
            handle.get<String>(key)?.let { bundle.putString(key, it) }
        }
    }

    private fun handleFromBundle(bundle: Bundle): SavedStateHandle {
        val map = bundle.keySet().associateWith { bundle.getString(it) as Any }
        return SavedStateHandle(map)
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

    private class RegistryOwner : SavedStateRegistryOwner {
        private val controller = SavedStateRegistryController.create(this)
        override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry
        private val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = lifecycleRegistry

        init {
            controller.performAttach()
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            controller.performRestore(null)
        }

        fun save(bundle: Bundle) {
            controller.performSave(bundle)
        }

        fun restore(bundle: Bundle?) {
            controller.performRestore(bundle)
        }
    }
}
