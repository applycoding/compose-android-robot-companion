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
    fun startRejectedWhenLinkLost() {
        val sim = Sim()
        var tel = sim.currentTelemetry()
        while (tel.tick < 14) {
            tel = sim.step()
        }
        assertEquals(Reject.LINK_LOST, sim.apply(DemoMissions.startCommand()).reject)
    }

    @Test
    fun startRejectedWhenBusy() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        sim.apply(DemoMissions.startCommand())
        assertEquals(Reject.BUSY, sim.apply(DemoMissions.startCommand()).reject)
    }

    @Test
    fun startRejectedEmptyWaypoints() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "ok", emptyList())).reject,
        )
    }

    @Test
    fun startRejectedTooManyWaypoints() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        val wps = List(17) { 1.0 to 1.0 }
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "ok", wps)).reject,
        )
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
    fun outOfRoomWaypointRejected() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "bad", listOf(0.1 to 1.0))).reject,
        )
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "bad", listOf(9.9 to 1.0))).reject,
        )
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "bad", listOf(5.0 to 0.1))).reject,
        )
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "bad", listOf(5.0 to 7.9))).reject,
        )
    }

    @Test
    fun invalidMissionNameRejected() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "", listOf(1.0 to 1.0))).reject,
        )
        assertEquals(
            Reject.INVALID_MISSION,
            sim.apply(Command.StartMission("x", "x".repeat(33), listOf(1.0 to 1.0))).reject,
        )
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
    fun resetRejectedWhenNotLatched() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        assertEquals(Reject.NOT_LATCHED, sim.apply(Command.ResetEStop).reject)
    }

    @Test
    fun resetRejectedWhenLinkLost() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        support.runTo(100)
        sim.apply(Command.EStop)
        support.runTo(113)
        var tel = sim.currentTelemetry()
        while (tel.tick < 127) {
            tel = sim.step()
        }
        assertEquals(Reject.LINK_LOST, sim.apply(Command.ResetEStop).reject)
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
    fun cancelRejectedWhenIdle() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        assertEquals(Reject.NOT_RUNNING, sim.apply(Command.Cancel).reject)
    }

    @Test
    fun cancelSucceedsWhilePaused() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        sim.apply(DemoMissions.startCommand())
        sim.apply(Command.Pause)
        assertEquals(Reject.NONE, sim.apply(Command.Cancel).reject)
        assertEquals(MissionStatus.CANCELLED, sim.currentTelemetry().mission)
    }

    @Test
    fun resumeRejectedWhenNotPaused() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        sim.apply(DemoMissions.startCommand())
        assertEquals(Reject.NOT_PAUSED, sim.apply(Command.Resume).reject)
    }

    @Test
    fun resumeRejectedWhenLinkLost() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        support.runTo(20)
        sim.apply(Command.Pause)
        var tel = sim.currentTelemetry()
        while (tel.tick < 44) {
            tel = sim.step()
        }
        assertEquals(Reject.LINK_LOST, sim.apply(Command.Resume).reject)
    }

    @Test
    fun estopLatchedRejectsOperatorCommands() {
        val sim = Sim()
        sim.apply(Command.Heartbeat)
        sim.apply(Command.EStop)
        assertEquals(Reject.ESTOP_LATCHED, sim.apply(DemoMissions.startCommand()).reject)
        assertEquals(Reject.ESTOP_LATCHED, sim.apply(Command.Pause).reject)
        assertEquals(Reject.ESTOP_LATCHED, sim.apply(Command.Resume).reject)
        assertEquals(Reject.ESTOP_LATCHED, sim.apply(Command.Cancel).reject)
        assertEquals(Reject.NONE, sim.apply(Command.Heartbeat).reject)
        assertEquals(Reject.NONE, sim.apply(Command.EStop).reject)
    }

    @Test
    fun lowBatteryStartRejected() {
        val sim = Sim(SimConfig.standard.copy(startBatteryPct = 19.0))
        sim.apply(Command.Heartbeat)
        assertEquals(Reject.LOW_BATTERY, sim.apply(DemoMissions.startCommand()).reject)
    }

    @Test
    fun resumeRejectedWhenStillLowBattery() {
        val cfg = SimConfig.standard.copy(
            startBatteryPct = 20.5,
            drainMovingPctPerTick = 0.05,
            drainBasePctPerTick = 0.0,
        )
        val sim = Sim(cfg)
        sim.apply(Command.Heartbeat)
        sim.apply(DemoMissions.startCommand())
        var tel = sim.currentTelemetry()
        while (tel.tick < 500 && tel.mission != MissionStatus.COMPLETED) {
            if (tel.tick % 5 == 0) sim.apply(Command.Heartbeat)
            tel = sim.step()
            if (tel.mission == MissionStatus.PAUSED && tel.pause == PauseReason.LOW_BATTERY) {
                break
            }
        }
        assertEquals(PauseReason.LOW_BATTERY, sim.currentTelemetry().pause)
        assertEquals(Reject.LOW_BATTERY, sim.apply(Command.Resume).reject)
    }
}
