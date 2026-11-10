// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class QuickWsTest {
    private lateinit var server: SimServer
    private lateinit var client: HttpClient

    @Before
    fun setUp() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        server = SimServer(9877, true, RobotHost(scope, lockstep = true))
        server.start()
        client = HttpClient(CIO) { install(WebSockets) }
    }

    @After
    fun tearDown() {
        client.close()
        server.stop()
    }

    @Test
    fun stepAfterStart() = runBlocking {
        client.webSocket("ws://127.0.0.1:9877/v1/robot") {
            println((incoming.receive() as Frame.Text).readText())
            println((incoming.receive() as Frame.Text).readText())
            send(Frame.Text("""{"v":1,"id":2,"type":"start","missionId":"demo","name":"Demo patrol","waypoints":[[8.0,1.0],[8.0,6.0],[2.0,6.0]]}"""))
            println((incoming.receive() as Frame.Text).readText())
            send(Frame.Text("""{"v":1,"id":5,"type":"step"}"""))
            println((incoming.receive() as Frame.Text).readText())
        }
    }
}
