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
            withTimeout(10_000) {
                client.webSocket("ws://127.0.0.1:9880/v1/robot") {
                    val inbox = Channel<String>(Channel.UNLIMITED)
                    launch {
                        for (frame in incoming) {
                            if (frame is Frame.Text) inbox.send(frame.readText())
                        }
                    }
                    readFrame(inbox)
                    readFrame(inbox)
                    send(
                        Frame.Text(
                            """{"v":1,"id":1,"type":"start","missionId":"demo","name":"Demo patrol","waypoints":[[8.0,1.0],[8.0,6.0],[2.0,6.0]]}""",
                        ),
                    )
                    readAckTick(inbox)
                    val snaps = mutableListOf<Telemetry>()
                    var estopK = -1
                    var estopSent = false
                    val startMs = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startMs < 9_000) {
                        val text = inbox.tryReceive().getOrNull()
                        if (text == null) {
                            kotlinx.coroutines.delay(10)
                            continue
                        }
                        if (text.contains("\"result\"")) {
                            if (estopSent && estopK < 0) {
                                val ack = codec.decodeServer(text) as WireMessage.ServerAck
                                estopK = ack.tick
                            }
                            continue
                        }
                        parseSnapshot(text)?.let { snap ->
                            if (snaps.lastOrNull()?.tick == snap.tick) return@let
                            if (snaps.isNotEmpty()) assertTrue(snap.tick > snaps.last().tick)
                            snaps.add(snap)
                            if (snap.tick % 5 == 0) {
                                send(Frame.Text("""{"v":1,"id":9,"type":"heartbeat"}"""))
                                readAckTick(inbox)
                            }
                            if (!estopSent && snap.tick in 60..180 && snap.v >= 0.75) {
                                estopSent = true
                                send(Frame.Text("""{"v":1,"id":2,"type":"estop"}"""))
                                estopK = readAckTick(inbox)
                            }
                        }
                        if (estopK >= 0 && snaps.any { it.tick >= estopK + 13 }) break
                        if ((snaps.lastOrNull()?.tick ?: 0) >= 50 && estopK >= 0) break
                    }
                    assertTrue((snaps.lastOrNull()?.tick ?: 0) >= 50)
                    if (estopK < 0) {
                        val trigger = snaps.firstOrNull { it.tick in 60..180 && it.v >= 0.75 }
                            ?: snaps.first { it.tick >= 60 }
                        send(Frame.Text("""{"v":1,"id":2,"type":"estop"}"""))
                        estopK = readAckTick(inbox)
                    }
                    val end = System.currentTimeMillis() + 800
                    while (System.currentTimeMillis() < end) {
                        val text = inbox.tryReceive().getOrNull()
                        if (text == null) {
                            kotlinx.coroutines.delay(10)
                            continue
                        }
                        parseSnapshot(text)?.let { snap ->
                            if (snaps.lastOrNull()?.tick != snap.tick) snaps.add(snap)
                        }
                    }
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

    private suspend fun readFrame(inbox: Channel<String>): String {
        while (true) {
            val t = inbox.receive()
            if (!t.contains("\"tickMs\"")) return t
        }
    }

    private suspend fun readAckTick(inbox: Channel<String>): Int {
        while (true) {
            val t = inbox.receive()
            if (t.contains("\"result\"")) {
                return (codec.decodeServer(t) as WireMessage.ServerAck).tick
            }
            parseSnapshot(t)?.let { /* keep draining snapshots */ }
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
