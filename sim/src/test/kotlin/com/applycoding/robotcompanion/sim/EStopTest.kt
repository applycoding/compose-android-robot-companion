// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EStopTest {
    @Test
    fun restsAtTick113() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        support.runTo(100)
        val atEstop = sim.currentTelemetry()
        assertEquals(0.8, atEstop.v, 0.001)
        sim.apply(Command.EStop)
        var tel = sim.step()
        assertEquals(101, tel.tick)
        assertTrue(tel.v > 0)
        while (tel.tick < 112) {
            if (tel.tick % 5 == 0) sim.apply(Command.Heartbeat)
            tel = sim.step()
        }
        assertEquals(112, tel.tick)
        assertTrue(tel.v > 0)
        if (tel.tick % 5 == 0) sim.apply(Command.Heartbeat)
        tel = sim.step()
        assertEquals(113, tel.tick)
        assertEquals(0.0, tel.v, 1e-9)
        assertEquals(MissionStatus.ABORTED, tel.mission)
        assertTrue(tel.estopLatched)
        val dist = hypot(tel.x - atEstop.x, tel.y - atEstop.y)
        assertEquals(0.1843, dist, 0.001)
        assertTrue(dist < 0.2)
    }

    @Test
    fun restsAtTick163AfterEstopAt150() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        support.runTo(150)
        sim.apply(Command.EStop)
        sim.step()
        var tel = sim.currentTelemetry()
        while (tel.tick < 163) {
            if (tel.tick % 5 == 0) sim.apply(Command.Heartbeat)
            tel = sim.step()
        }
        assertEquals(163, tel.tick)
        assertEquals(0.0, tel.v, 1e-9)
    }

    @Test
    fun restsAtTick313AfterEstopAt300() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        support.runTo(300)
        assertEquals(0.7997, sim.currentTelemetry().v, 0.001)
        sim.apply(Command.EStop)
        sim.step()
        var tel = sim.currentTelemetry()
        while (tel.tick < 313) {
            if (tel.tick % 5 == 0) sim.apply(Command.Heartbeat)
            tel = sim.step()
        }
        assertEquals(313, tel.tick)
        assertEquals(0.0, tel.v, 1e-9)
    }

    @Test
    fun idempotentEstop() {
        val sim = Sim()
        sim.apply(Command.EStop)
        sim.apply(Command.EStop)
        assertTrue(sim.currentTelemetry().estopLatched)
    }
}
