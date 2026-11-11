// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.WebSocketTransport
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.PauseReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    companion object {
        private const val HEARTBEAT_TIMEOUT_TICKS = 14
    }

    @Test
    fun busyReconnectAndLatch() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val host = RobotHost(scope, lockstep = false)
        val server = SimServer(0, false, host)
        val primaryClient = testHttpClient()
        val secondaryClient = testHttpClient()
        val reconnectClient = testHttpClient()
        try {
            server.start()
            val busyReady = CompletableDeferred<Unit>()
            val busyDone = CompletableDeferred<Unit>()
            val primary = testWebSocketTransport(scope, server.resolvedPort, primaryClient)
            val holder = async {
                awaitWelcome(primary)
                awaitFirstSnapshot(primary)
                primary.sendAndAwaitAck(Command.Heartbeat)
                primary.sendAndAwaitAck(DemoMissions.startCommand())
                val hb = scope.launch {
                    while (isActive) {
                        delay(200)
                        primary.send(Command.Heartbeat)
                    }
                }
                busyReady.complete(Unit)
                busyDone.await()
                hb.cancel()
                val ack = primary.sendAndAwaitAck(Command.Heartbeat)
                val linkLostTick = ack.tick + HEARTBEAT_TIMEOUT_TICKS
                var seenLinkLost = false
                while (!seenLinkLost) {
                    val snap = primary.snapshots.value
                    if (snap != null && snap.tick == linkLostTick && snap.pause == PauseReason.LINK_LOST) {
                        assertEquals(
                            "LINK_LOST pause at measured tick after heartbeat stops",
                            linkLostTick,
                            snap.tick,
                        )
                        seenLinkLost = true
                    } else {
                        delay(5)
                    }
                }
                primary.sendAndAwaitAck(Command.EStop)
            }
            busyReady.await()
            delay(200)
            val secondary = testWebSocketTransport(scope, server.resolvedPort, secondaryClient)
            assertEquals("second client must receive BUSY error code", "BUSY", awaitError(secondary).code)
            secondary.close()
            secondaryClient.close()
            busyDone.complete(Unit)
            holder.await()
            primary.close()
            delay(500)
            val reconnect = testWebSocketTransport(scope, server.resolvedPort, reconnectClient)
            val welcome = awaitWelcome(reconnect)
            assertEquals(40, welcome.tickMs)
            val latched = withTimeout(15_000) { awaitFirstSnapshot(reconnect) }
            assertTrue("reconnect welcome must be followed by latched snapshot", latched.estopLatched)
            reconnect.close()
        } finally {
            primaryClient.close()
            reconnectClient.close()
            server.stop()
            scope.cancel()
        }
    }
}
