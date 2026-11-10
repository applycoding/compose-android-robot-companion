// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.WireCodec
import com.applycoding.robotcompanion.link.WireMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    private val codec = WireCodec()

    @Test
    fun busyReconnectAndLatch() = runBlocking {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        val host = RobotHost(scope, lockstep = false)
        val server = SimServer(9881, false, host)
        val client = HttpClient(CIO) { install(WebSockets) }
        try {
            server.start()
            val hold = Job()
            val holder = launch(hold) {
                client.webSocket("ws://127.0.0.1:9881/v1/robot") {
                    readFrame()
                    readFrame()
                    send(Frame.Text("""{"v":1,"id":1,"type":"estop"}"""))
                    delay(60_000)
                }
            }
            delay(200)
            var busy = false
            client.webSocket("ws://127.0.0.1:9881/v1/robot") {
                val msg = readFrame()
                busy = codec.decodeServer(msg) is WireMessage.ServerError
            }
            assertTrue(busy)
            hold.cancel()
            holder.join()
            delay(300)
            client.webSocket("ws://127.0.0.1:9881/v1/robot") {
                readFrame()
                val snap = codec.decodeServer(readFrame()) as WireMessage.ServerSnapshot
                assertTrue(snap.snapshot.estop)
            }
        } finally {
            client.close()
            server.stop()
        }
    }

    private suspend fun DefaultClientWebSocketSession.readFrame(): String {
        while (true) {
            val t = (incoming.receive() as Frame.Text).readText()
            if (!t.contains("\"tickMs\"")) return t
        }
    }
}
