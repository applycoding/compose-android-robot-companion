// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

class SimTestSupport(private val sim: Sim = Sim()) {
    fun stepWithHeartbeat(): Telemetry {
        val t = sim.currentTelemetry().tick
        if (t % 5 == 0) {
            sim.apply(Command.Heartbeat)
        }
        return sim.step()
    }

    fun runTo(targetTick: Int): Telemetry {
        var last = sim.currentTelemetry()
        while (last.tick < targetTick) {
            last = stepWithHeartbeat()
        }
        return last
    }

    fun startDemo() {
        sim.apply(Command.Heartbeat)
        sim.apply(DemoMissions.startCommand())
    }
}
