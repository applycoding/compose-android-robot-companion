// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.Sim
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InProcessTransportTest {
    @Test
    fun manualTicksAdvance() = runTest {
        val sim = Sim()
        val manual = ManualTickSource { sim.step() }
        val transport = InProcessTransport(sim, manual, this.backgroundScope, realtime = false)
        transport.send(Command.Heartbeat)
        transport.stepOnce()
        assertEquals(1, transport.snapshots.value?.tick)
        transport.close()
    }

    @Test
    fun realtimeTicksOnVirtualTime() = runTest {
        val sim = Sim()
        val transport = InProcessTransport(sim, null, this, realtime = true)
        advanceTimeBy(200)
        assertTrue((transport.snapshots.value?.tick ?: 0) >= 4)
        transport.close()
    }
}
