// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissionRulesTest {
    @Test
    fun startRejectedWhileLatched() {
        val sim = Sim()
        sim.apply(Command.EStop)
        val result = sim.apply(DemoMissions.startCommand())
        assertEquals(Reject.ESTOP_LATCHED, result.reject)
    }

    @Test
    fun nonFiniteWaypointRejected() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        val result = sim.apply(
            Command.StartMission("x", "bad", listOf(Double.NaN to 1.0)),
        )
        assertEquals(Reject.INVALID_MISSION, result.reject)
    }

    @Test
    fun resetRejectedWhileMoving() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        support.runTo(20)
        sim.apply(Command.EStop)
        val result = sim.apply(Command.ResetEStop)
        assertEquals(Reject.MOVING, result.reject)
        support.runTo(113)
        val cleared = sim.apply(Command.ResetEStop)
        assertEquals(Reject.NONE, cleared.reject)
        assertTrue(!sim.currentTelemetry().estopLatched)
        assertEquals(MissionStatus.ABORTED, sim.currentTelemetry().mission)
    }

    @Test
    fun pauseResumeCancelPrecedence() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        assertEquals(Reject.NOT_RUNNING, sim.apply(Command.Pause).reject)
        assertEquals(Reject.NOT_PAUSED, sim.apply(Command.Resume).reject)
        sim.apply(DemoMissions.startCommand())
        sim.apply(Command.Pause)
        assertEquals(Reject.BUSY, sim.apply(DemoMissions.startCommand()).reject)
        sim.apply(Command.Resume)
        sim.apply(Command.Cancel)
        assertEquals(MissionStatus.CANCELLED, sim.currentTelemetry().mission)
    }

    @Test
    fun lowBatteryStartRejected() {
        val sim = Sim(SimConfig.standard.copy(startBatteryPct = 19.0))
        sim.apply(Command.Heartbeat)
        assertEquals(Reject.LOW_BATTERY, sim.apply(DemoMissions.startCommand()).reject)
    }
}
