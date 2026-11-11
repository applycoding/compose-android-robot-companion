// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.WireCodec
import com.applycoding.robotcompanion.link.WireMessage
import com.applycoding.robotcompanion.sim.PauseReason
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
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
    private val codec = WireCodec()

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
            val url = wsUrl(server.resolvedPort)
            val busyReady = CompletableDeferred<Unit>()
            val busyDone = CompletableDeferred<Unit>()
            val holder = async {
                primaryClient.webSocket(url) {
                    readPastWelcome()
                    send(Frame.Text("""{"v":1,"id":1,"type":"heartbeat"}"""))
                    skipUntilAck()
                    send(
                        Frame.Text(
                            """{"v":1,"id":2,"type":"start","missionId":"demo","name":"Demo patrol","waypoints":[[8.0,1.0],[8.0,6.0],[2.0,6.0]]}""",
                        ),
                    )
                    skipUntilAck()
                    val hb = launch {
                        var id = 10
                        while (isActive) {
                            delay(200)
                            send(Frame.Text("""{"v":1,"id":$id,"type":"heartbeat"}"""))
                            skipUntilAck()
                            id++
                        }
                    }
                    busyReady.complete(Unit)
                    busyDone.await()
                    hb.cancel()
                    send(Frame.Text("""{"v":1,"id":99,"type":"heartbeat"}"""))
                    val lastHbTick = skipUntilAck().tick
                    val linkLostTick = lastHbTick + HEARTBEAT_TIMEOUT_TICKS
                    var seenLinkLost = false
                    while (!seenLinkLost) {
                        val snap = nextSnapshot() ?: continue
                        if (snap.pause == PauseReason.LINK_LOST) {
                            assertEquals(
                                "LINK_LOST pause at measured tick after heartbeat stops",
                                linkLostTick,
                                snap.tick,
                            )
                            seenLinkLost = true
                        }
                    }
                    send(Frame.Text("""{"v":1,"id":200,"type":"estop"}"""))
                    skipUntilAck()
                }
            }
            busyReady.await()
            delay(200)
            val secondary = testWebSocketTransport(scope, server.resolvedPort, secondaryClient)
            assertEquals("second client must receive BUSY error code", "BUSY", awaitError(secondary).code)
            secondary.close()
            secondaryClient.close()
            busyDone.complete(Unit)
            holder.await()
            delay(500)
            val reconnect = testWebSocketTransport(scope, server.resolvedPort, reconnectClient)
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

    private suspend fun DefaultClientWebSocketSession.readPastWelcome() {
        nextNonWelcome()
        nextNonWelcome()
    }

    private suspend fun DefaultClientWebSocketSession.nextNonWelcome(): String {
        while (true) {
            val text = (incoming.receive() as Frame.Text).readText()
            if (!text.contains("\"tickMs\"")) return text
        }
    }

    private suspend fun DefaultClientWebSocketSession.skipUntilAck(): WireMessage.ServerAck {
        while (true) {
            val text = nextNonWelcome()
            if (text.contains("\"result\"")) {
                return codec.decodeServer(text) as WireMessage.ServerAck
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.nextSnapshot() =
        when (val msg = codec.decodeServer(nextNonWelcome())) {
            is WireMessage.ServerSnapshot -> wireClientTelemetry(msg.snapshot)
            else -> null
        }

}
