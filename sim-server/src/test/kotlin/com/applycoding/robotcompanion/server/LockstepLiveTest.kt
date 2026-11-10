// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.WireCodec
import com.applycoding.robotcompanion.link.WireMessage
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.PauseReason
import com.applycoding.robotcompanion.sim.Telemetry
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LockstepLiveTest {
    private val codec = WireCodec()
    private lateinit var server: SimServer
    private lateinit var client: HttpClient

    @Before
    fun setUp() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        val host = RobotHost(scope, lockstep = true)
        server = SimServer(9876, true, host)
        server.start()
        client = HttpClient(CIO) { install(WebSockets) }
    }

    @After
    fun tearDown() {
        client.close()
        server.stop()
    }

    @Test
    fun traceEqualsInProcess() = runBlocking {
        val reference = LockstepScript.runInProcess()
        val live = withTimeout(15_000) { runLockstepClient() }
        assertEquals(130, live.size)
        reference.zip(live).forEach { (a, b) ->
            assertEquals(a.tick, b.tick)
            assertEquals(a.x, b.x, 1e-6)
            assertEquals(a.y, b.y, 1e-6)
            assertEquals(a.v, b.v, 1e-6)
            assertEquals(a.mission, b.mission)
            assertEquals(a.estopLatched, b.estopLatched)
        }
        val rest = live.first { it.tick == 113 }
        assertEquals(0.0, rest.v, 1e-9)
        assertEquals(MissionStatus.ABORTED, rest.mission)
    }

    private suspend fun io.ktor.client.plugins.websocket.DefaultClientWebSocketSession.readFrame(): String {
        while (true) {
            val frame = incoming.receive()
            if (frame is Frame.Text) return frame.readText()
        }
    }

    private suspend fun runLockstepClient(): List<Telemetry> {
        val snaps = mutableListOf<Telemetry>()
        client.webSocket("ws://127.0.0.1:9876/v1/robot") {
            val greet = readFrame()
            require(greet.contains("tickMs"))
            val first = parseSnapshot(readFrame()) ?: error("missing snapshot")
            snaps.add(first)
            send(
                Frame.Text(
                    """{"v":1,"id":2,"type":"start","missionId":"demo","name":"Demo patrol","waypoints":[[8.0,1.0],[8.0,6.0],[2.0,6.0]]}""",
                ),
            )
            val startAck = readFrame()
            require(startAck.contains("\"result\""))
            while (snaps.size < 130) {
                val tick = snaps.last().tick
                if (tick % 5 == 0) {
                    send(Frame.Text("""{"v":1,"id":3,"type":"heartbeat"}"""))
                    val ack = readFrame()
                    require(ack.contains("\"result\""))
                }
                if (tick == 100) {
                    send(Frame.Text("""{"v":1,"id":4,"type":"estop"}"""))
                    val ack = readFrame()
                    require(ack.contains("\"result\""))
                }
                send(Frame.Text("""{"v":1,"id":5,"type":"step"}"""))
                val snap = parseSnapshot(readFrame()) ?: error("no snapshot at tick after $tick")
                snaps.add(snap)
            }
        }
        return snaps
    }

    private fun parseSnapshot(text: String): Telemetry? = try {
        when (val msg = codec.decodeServer(text)) {
            is WireMessage.ServerSnapshot -> {
                val s = msg.snapshot
                Telemetry(
                    tick = s.tick,
                    x = s.x,
                    y = s.y,
                    theta = s.theta,
                    v = s.speed,
                    w = 0.0,
                    batteryPct = s.battery,
                    missionId = s.missionId,
                    mission = MissionStatus.valueOf(s.mission),
                    pause = s.pause?.let { PauseReason.valueOf(it) },
                    waypointIndex = s.wp,
                    estopLatched = s.estop,
                    linkOk = s.link,
                    travelled = 0.0,
                )
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
