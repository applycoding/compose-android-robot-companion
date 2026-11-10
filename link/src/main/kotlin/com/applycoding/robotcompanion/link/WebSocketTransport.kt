// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.PauseReason
import com.applycoding.robotcompanion.sim.Reject
import com.applycoding.robotcompanion.sim.Telemetry
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WebSocketTransport(
    private val url: String,
    private val client: HttpClient,
    private val scope: CoroutineScope,
    private val codec: WireCodec = WireCodec(),
) : Transport {
    private val _snapshots = MutableStateFlow<Telemetry?>(null)
    override val snapshots: StateFlow<Telemetry?> = _snapshots
    private val _events = MutableSharedFlow<LinkEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<LinkEvent> = _events
    private var nextId = 1
    private var session: DefaultClientWebSocketSession? = null
    private var connectJob: Job? = null

    init {
        connectJob = scope.launch { connectLoop() }
    }

    private suspend fun connectLoop() {
        while (scope.isActive) {
            try {
                val ws = client.webSocketSession(url)
                session = ws
                ws.send(Frame.Text(codec.encodeClient(WireMessage.ClientHello(nextId++))))
                _events.emit(LinkEvent.Connected)
                for (frame in ws.incoming) {
                    if (frame is Frame.Text) {
                        handleIncoming(frame.readText())
                    }
                }
            } catch (_: Exception) {
                _events.emit(LinkEvent.Disconnected)
            } finally {
                session = null
            }
            delay(1000)
        }
    }

    private suspend fun handleIncoming(text: String) {
        when (val msg = codec.decodeServer(text)) {
            is WireMessage.ServerSnapshot -> {
                val s = msg.snapshot
                _snapshots.value = Telemetry(
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
            is WireMessage.ServerAck -> _events.emit(
                LinkEvent.Ack(msg.id, Reject.valueOf(if (msg.result == "NONE") "NONE" else msg.result), msg.tick),
            )
            is WireMessage.ServerError -> _events.emit(LinkEvent.Error(msg.code))
            is WireMessage.ServerWelcome -> Unit
            else -> Unit
        }
    }

    override suspend fun send(cmd: Command): Long {
        val id = nextId++
        val wire = when (cmd) {
            Command.Heartbeat -> WireMessage.ClientHeartbeat(id)
            Command.Pause -> WireMessage.ClientSimple(id, "pause")
            Command.Resume -> WireMessage.ClientSimple(id, "resume")
            Command.Cancel -> WireMessage.ClientSimple(id, "cancel")
            Command.EStop -> WireMessage.ClientSimple(id, "estop")
            Command.ResetEStop -> WireMessage.ClientSimple(id, "reset_estop")
            is Command.StartMission -> WireMessage.ClientStart(id, cmd.missionId, cmd.name, cmd.waypoints)
        }
        val json = when (wire) {
            is WireMessage.ClientStart -> codec.encodeClient(wire)
            is WireMessage.ClientHeartbeat -> """{"v":1,"id":$id,"type":"heartbeat"}"""
            is WireMessage.ClientSimple -> """{"v":1,"id":$id,"type":"${wire.type}"}"""
            else -> codec.encodeClient(wire)
        }
        session?.send(Frame.Text(json))
        return id.toLong()
    }

    suspend fun sendStep(): Long {
        val id = nextId++
        session?.send(Frame.Text("""{"v":1,"id":$id,"type":"step"}"""))
        return id.toLong()
    }

    override fun close() {
        connectJob?.cancel()
    }
}
