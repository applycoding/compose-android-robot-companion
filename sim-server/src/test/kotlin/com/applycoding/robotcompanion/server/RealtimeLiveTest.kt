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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeLiveTest {
    private val codec = WireCodec()

    @Test
    fun ticksAdvance() = runBlocking {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        val host = RobotHost(scope, lockstep = false)
        val server = SimServer(9880, false, host)
        val client = HttpClient(CIO) { install(WebSockets) }
        try {
            server.start()
            withTimeout(15_000) {
                client.webSocket("ws://127.0.0.1:9880/v1/robot") {
                    val inbox = Channel<String>(Channel.UNLIMITED)
                    launch {
                        for (frame in incoming) {
                            if (frame is Frame.Text) inbox.send(frame.readText())
                        }
                    }
                    readSkipWelcome(inbox)
                    send(Frame.Text("""{"v":1,"id":1,"type":"heartbeat"}"""))
                    readAck(inbox)
                    send(
                        Frame.Text(
                            """{"v":1,"id":2,"type":"start","missionId":"demo","name":"Demo patrol","waypoints":[[8.0,1.0],[8.0,6.0],[2.0,6.0]]}""",
                        ),
                    )
                    readAck(inbox)
                    val snaps = mutableListOf<Telemetry>()
                    var estopK = -1
                    val hbJob = launch {
                        while (estopK < 0) {
                            delay(200)
                            send(Frame.Text("""{"v":1,"id":99,"type":"heartbeat"}"""))
                        }
                    }
                    val deadline = System.currentTimeMillis() + 9_000
                    while (System.currentTimeMillis() < deadline && estopK < 0) {
                        val text = inbox.tryReceive().getOrNull()
                        if (text != null) {
                            parseSnapshot(text)?.let { snap ->
                                if (snaps.isEmpty() || snap.tick > snaps.last().tick) {
                                    snaps.add(snap)
                                }
                                if (snap.tick in 60..180 && snap.v >= 0.75) {
                                    send(Frame.Text("""{"v":1,"id":3,"type":"estop"}"""))
                                    estopK = readAck(inbox)
                                }
                            }
                        } else {
                            delay(10)
                        }
                    }
                    hbJob.cancel()
                    val end = System.currentTimeMillis() + 1_500
                    while (System.currentTimeMillis() < end) {
                        inbox.tryReceive().getOrNull()?.let { text ->
                            parseSnapshot(text)?.let { snap ->
                                if (snaps.isEmpty() || snap.tick > snaps.last().tick) snaps.add(snap)
                            }
                        }
                        delay(10)
                    }
                    assertTrue((snaps.lastOrNull()?.tick ?: 0) >= 50)
                    assertTrue(estopK >= 0)
                    val at12 = snaps.first { it.tick == estopK + 12 }
                    val at13 = snaps.first { it.tick == estopK + 13 }
                    assertTrue(at12.v > 0)
                    assertEquals(0.0, at13.v, 1e-6)
                    assertTrue(at13.estopLatched)
                    assertEquals(MissionStatus.ABORTED, at13.mission)
                }
            }
        } finally {
            client.close()
            server.stop()
        }
    }

    private suspend fun readSkipWelcome(inbox: Channel<String>) {
        while (true) {
            val text = inbox.receive()
            if (text.contains("\"tick\"") && text.contains("\"speed\"")) return
        }
    }

    private suspend fun readAck(inbox: Channel<String>): Int {
        while (true) {
            val text = inbox.receive()
            if (text.contains("\"result\"")) {
                return (codec.decodeServer(text) as WireMessage.ServerAck).tick
            }
        }
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
