// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class RealtimeLiveTest {
    @Test
    fun ticksAdvance() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val host = RobotHost(scope, lockstep = false)
        val server = SimServer(0, false, host)
        val client = testHttpClient()
        try {
            server.start()
            val transport = testWebSocketTransport(scope, server.resolvedPort, client)
                var estopK = -1
            val lastTick = AtomicInteger(-1)
            val maxTick = AtomicInteger(0)
            val monotonicViolations = AtomicReference<String?>(null)
            try {
                awaitConnected(transport)
                awaitFirstSnapshot(transport)
                transport.send(Command.Heartbeat)
                transport.sendAndAwaitAck(DemoMissions.startCommand())
                val heartbeat = scope.launch {
                    while (true) {
                        delay(200)
                        transport.send(Command.Heartbeat)
                    }
                }
                val collector = scope.launch {
                    transport.snapshots.collect { snap ->
                        if (snap == null) return@collect
                        val prev = lastTick.get()
                        if (prev >= 0 && snap.tick <= prev) {
                            monotonicViolations.set(
                                "ticks must strictly increase (last=$prev, new=${snap.tick})",
                            )
                        }
                        lastTick.set(snap.tick)
                        maxTick.updateAndGet { cur -> maxOf(cur, snap.tick) }
                        if (
                            estopK < 0 &&
                            snap.tick in 60..180 &&
                            snap.v == 0.8
                        ) {
                            val ack = transport.sendAndAwaitAck(Command.EStop)
                            estopK = ack.tick
                            heartbeat.cancel()
                        }
                    }
                }
                val deadline = System.currentTimeMillis() + 10_000
                while (System.currentTimeMillis() < deadline && estopK < 0) {
                    delay(10)
                }
                assertTrue(
                    monotonicViolations.get() ?: "tick monotonicity",
                    monotonicViolations.get() == null,
                )
                assertTrue(
                    "decoded ticks must reach at least 50 within 10s (last=${maxTick.get()})",
                    maxTick.get() >= 50,
                )
                assertTrue("estop must be sent at speed==0.8 in [60,180]", estopK >= 0)
                val at12 = awaitSnapshotTick(transport, estopK + 12)
                val at13 = awaitSnapshotTick(transport, estopK + 13)
                collector.cancel()
                assertTrue("speed still positive 12 ticks after estop ack (tick ${at12.tick})", at12.v > 0)
                assertEquals("speed zero 13 ticks after estop ack (tick ${at13.tick})", 0.0, at13.v, 0.0)
                assertTrue("estop latched at tick ${at13.tick}", at13.estopLatched)
                assertEquals("mission aborted at tick ${at13.tick}", MissionStatus.ABORTED, at13.mission)
            } finally {
                transport.close()
            }
        } finally {
            client.close()
            server.stop()
            scope.cancel()
        }
    }
}
