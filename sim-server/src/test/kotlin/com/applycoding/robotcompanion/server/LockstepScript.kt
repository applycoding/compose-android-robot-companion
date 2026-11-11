// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.WebSocketTransport
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.Sim
import com.applycoding.robotcompanion.sim.Telemetry

object LockstepScript {
    fun runInProcess(): List<Telemetry> {
        val sim = Sim()
        val snaps = mutableListOf(sim.currentTelemetry())
        sim.apply(Command.Heartbeat)
        sim.apply(DemoMissions.startCommand())
        while (snaps.size < 130) {
            val tick = sim.currentTelemetry().tick
            if (tick % 5 == 0) {
                sim.apply(Command.Heartbeat)
            }
            if (tick == 100) {
                sim.apply(Command.EStop)
            }
            snaps.add(sim.step())
        }
        return snaps.map { it.asWireClientView() }
    }

    suspend fun runWithTransport(transport: WebSocketTransport): List<Telemetry> {
        awaitConnected(transport)
        val snaps = mutableListOf(awaitFirstSnapshot(transport))
        transport.sendAndAwaitAck(DemoMissions.startCommand())
        while (snaps.size < 130) {
            val tick = snaps.last().tick
            if (tick % 5 == 0) {
                transport.sendAndAwaitAck(Command.Heartbeat)
            }
            if (tick == 100) {
                transport.sendAndAwaitAck(Command.EStop)
            }
            transport.sendStep()
            snaps.add(awaitNextSnapshotAfter(transport, tick))
        }
        return snaps
    }
}
