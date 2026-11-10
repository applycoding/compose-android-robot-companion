// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryTest {
    @Test
    fun lowBatteryPauseTickMeasured() {
        val cfg = SimConfig.standard.copy(
            startBatteryPct = 20.5,
            drainMovingPctPerTick = 0.05,
            drainBasePctPerTick = 0.0,
        )
        val sim = Sim(cfg)
        sim.apply(Command.Heartbeat)
        sim.apply(DemoMissions.startCommand())
        var tel = sim.currentTelemetry()
        var pauseTick = -1
        while (tel.tick < 500 && tel.mission != MissionStatus.COMPLETED) {
            if (tel.tick % 5 == 0) sim.apply(Command.Heartbeat)
            tel = sim.step()
            if (tel.mission == MissionStatus.PAUSED && tel.pause == PauseReason.LOW_BATTERY) {
                pauseTick = tel.tick
                break
            }
        }
        assertEquals(231, pauseTick)
    }
}
