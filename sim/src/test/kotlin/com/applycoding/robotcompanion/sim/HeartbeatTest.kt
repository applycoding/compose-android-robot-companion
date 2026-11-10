// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeartbeatTest {
    @Test
    fun linkOkAtAges12And13LostAt14() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        repeat(12) { sim.step() }
        assertEquals(12, sim.currentTelemetry().tick)
        assertTrue(sim.currentTelemetry().linkOk)
        sim.step()
        assertEquals(13, sim.currentTelemetry().tick)
        assertTrue(sim.currentTelemetry().linkOk)
        sim.step()
        assertEquals(14, sim.currentTelemetry().tick)
        assertFalse(sim.currentTelemetry().linkOk)
    }

    @Test
    fun lostAtTick209() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        for (t in 0..195) {
            if (t % 5 == 0) sim.apply(Command.Heartbeat)
            sim.step()
        }
        var tel = sim.currentTelemetry()
        while (tel.tick < 208) {
            tel = sim.step()
        }
        assertEquals(208, tel.tick)
        assertTrue(tel.linkOk)
        tel = sim.step()
        assertEquals(209, tel.tick)
        assertFalse(tel.linkOk)
        assertEquals(MissionStatus.PAUSED, tel.mission)
        assertEquals(PauseReason.LINK_LOST, tel.pause)
        while (tel.tick < 221) {
            tel = sim.step()
        }
        assertEquals(221, tel.tick)
        assertEquals(0.0, tel.v, 1e-9)
    }
}
