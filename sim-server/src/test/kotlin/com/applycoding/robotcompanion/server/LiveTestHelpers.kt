// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.LinkEvent
import com.applycoding.robotcompanion.link.Transport
import com.applycoding.robotcompanion.link.WebSocketTransport
import com.applycoding.robotcompanion.link.WireSnapshot
import com.applycoding.robotcompanion.link.toWire
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.PauseReason
import com.applycoding.robotcompanion.sim.Telemetry
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.withTimeout

fun wireClientTelemetry(snapshot: WireSnapshot): Telemetry = Telemetry(
    tick = snapshot.tick,
    x = snapshot.x,
    y = snapshot.y,
    theta = snapshot.theta,
    v = snapshot.speed,
    w = 0.0,
    batteryPct = snapshot.battery,
    missionId = snapshot.missionId,
    mission = MissionStatus.valueOf(snapshot.mission),
    pause = snapshot.pause?.let { PauseReason.valueOf(it) },
    waypointIndex = snapshot.wp,
    estopLatched = snapshot.estop,
    linkOk = snapshot.link,
    travelled = 0.0,
)

fun Telemetry.asWireClientView(): Telemetry = wireClientTelemetry(toWire())

fun wsUrl(port: Int): String = "ws://127.0.0.1:$port/v1/robot"

fun testHttpClient(): HttpClient = HttpClient(CIO) { install(WebSockets) }

fun testWebSocketTransport(
    scope: CoroutineScope,
    port: Int,
    client: HttpClient,
): WebSocketTransport = WebSocketTransport(wsUrl(port), client, scope)

suspend fun awaitConnected(transport: WebSocketTransport) {
    awaitFirstSnapshot(transport)
}

suspend fun awaitFirstSnapshot(transport: Transport): Telemetry {
    withTimeout(5_000) {
        while (transport.snapshots.value == null) {
            kotlinx.coroutines.delay(5)
        }
    }
    return requireNotNull(transport.snapshots.value)
}

suspend fun awaitSnapshotTick(transport: Transport, tick: Int): Telemetry = withTimeout(15_000) {
    var found: Telemetry? = null
    while (found == null) {
        val snap = transport.snapshots.value
        if (snap != null && snap.tick == tick) {
            found = snap
        } else {
            kotlinx.coroutines.delay(5)
        }
    }
    found
}

suspend fun awaitWelcome(transport: WebSocketTransport): LinkEvent.Welcome = withTimeout(5_000) {
    transport.events.filterIsInstance<LinkEvent.Welcome>().first()
}

suspend fun awaitNextSnapshotAfter(transport: Transport, minTickExclusive: Int): Telemetry = withTimeout(15_000) {
    var found: Telemetry? = null
    while (found == null) {
        val snap = transport.snapshots.value
        if (snap != null && snap.tick > minTickExclusive) {
            found = snap
        } else {
            kotlinx.coroutines.delay(5)
        }
    }
    found
}

suspend fun awaitAck(transport: Transport, commandId: Long): LinkEvent.Ack {
    return withTimeout(5_000) {
        transport.events
            .mapNotNull { event ->
                when (event) {
                    is LinkEvent.Ack -> if (event.id.toLong() == commandId) event else null
                    else -> null
                }
            }
            .first()
    }
}

suspend fun Transport.sendAndAwaitAck(cmd: Command): LinkEvent.Ack = coroutineScope {
    val cmdId = CompletableDeferred<Long>()
    val waiter = async(start = CoroutineStart.UNDISPATCHED) {
        val id = cmdId.await()
        events.mapNotNull { (it as? LinkEvent.Ack)?.takeIf { ack -> ack.id.toLong() == id } }.first()
    }
    val id = send(cmd)
    cmdId.complete(id)
    waiter.await()
}

suspend fun awaitError(transport: WebSocketTransport): LinkEvent.Error {
    return withTimeout(5_000) {
        transport.events.filter { it is LinkEvent.Error }.first() as LinkEvent.Error
    }
}
