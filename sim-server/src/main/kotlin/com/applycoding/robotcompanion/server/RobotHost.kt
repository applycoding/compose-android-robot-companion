// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.WireCodec
import com.applycoding.robotcompanion.link.WireMessage
import com.applycoding.robotcompanion.link.toCommand
import com.applycoding.robotcompanion.link.toWire
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.Sim
import com.applycoding.robotcompanion.sim.SimConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed class OwnerMsg {
    data class Inbound(val json: String, val done: CompletableDeferred<List<String>>) : OwnerMsg()
    data object Tick : OwnerMsg()
}

class RobotHost(
    private val scope: CoroutineScope,
    val lockstep: Boolean,
    val sim: Sim = Sim(),
    private val codec: WireCodec = WireCodec(),
    var dropEstop: Boolean = false,
) {
    val tickMs: Int = SimConfig.standard.dtMs
    private val inbound = Channel<OwnerMsg>(Channel.UNLIMITED)
    private val sessionMutex = Mutex()
    private var sessionOut: Channel<String>? = null
    private var ticker: Job? = null

    init {
        scope.launch { ownerLoop() }
        if (!lockstep) {
            ticker = scope.launch {
                while (isActive) {
                    delay(tickMs.toLong())
                    inbound.send(OwnerMsg.Tick)
                }
            }
        }
    }

    suspend fun tryAttach(outbound: Channel<String>): Boolean = sessionMutex.withLock {
        if (sessionOut != null) return false
        sessionOut = outbound
        outbound.send(codec.encodeWelcome(tickMs, if (lockstep) "lockstep" else "realtime"))
        outbound.send(codec.encodeSnapshot(sim.currentTelemetry().toWire()))
        true
    }

    fun detach(outbound: Channel<String>) {
        if (sessionOut === outbound) sessionOut = null
    }

    suspend fun onClientMessage(json: String): List<String> {
        val done = CompletableDeferred<List<String>>()
        inbound.send(OwnerMsg.Inbound(json, done))
        return done.await()
    }

    private suspend fun ownerLoop() {
        for (msg in inbound) {
            when (msg) {
                is OwnerMsg.Tick -> {
                    val snap = codec.encodeSnapshot(sim.step().toWire())
                    pushOutbound(snap)
                }
                is OwnerMsg.Inbound -> {
                    val out = handleInbound(msg.json)
                    msg.done.complete(out)
                }
            }
        }
    }

    private fun handleInbound(json: String): List<String> = try {
        when (val wire = codec.decodeClient(json)) {
            is WireMessage.ClientHello -> emptyList()
            is WireMessage.ClientSimple -> when (wire.type) {
                "step" -> if (!lockstep) {
                    listOf(codec.encodeError("BAD_MESSAGE"))
                } else {
                    listOf(codec.encodeSnapshot(sim.step().toWire()))
                }
                "estop" -> if (dropEstop) {
                    emptyList()
                } else {
                    ackFor(wire.id, Command.EStop)
                }
                else -> ackFor(wire.id, wire.toCommand())
            }
            is WireMessage.ClientStart -> ackFor(
                wire.id,
                Command.StartMission(wire.missionId, wire.name, wire.waypoints),
            )
            is WireMessage.ClientHeartbeat -> ackFor(wire.id, Command.Heartbeat)
            else -> listOf(codec.encodeError("BAD_MESSAGE"))
        }
    } catch (_: Exception) {
        listOf(codec.encodeError("BAD_MESSAGE"))
    }

    private fun ackFor(id: Int, cmd: Command): List<String> {
        val result = sim.apply(cmd)
        return listOf(codec.encodeAck(id, result.reject, result.tick))
    }

    private fun pushOutbound(frame: String) {
        sessionOut?.trySend(frame)
    }
}
