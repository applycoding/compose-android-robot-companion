// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.Sim
import com.applycoding.robotcompanion.sim.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class InProcessTransport(
    private val sim: Sim,
    private val tickSource: TickSource? = null,
    private val scope: CoroutineScope,
    private val realtime: Boolean = false,
) : Transport {
  private val _snapshots = MutableStateFlow<Telemetry?>(sim.currentTelemetry())
    override val snapshots: StateFlow<Telemetry?> = _snapshots
    private val _events = MutableSharedFlow<LinkEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<LinkEvent> = _events
    private var nextId = 1L
    private var tickJob: Job? = null

    init {
        _events.tryEmit(LinkEvent.Connected)
        if (realtime) {
            tickJob = scope.launch {
                while (isActive) {
                    delay(40)
                    val tel = sim.step()
                    _snapshots.value = tel
                }
            }
        } else if (tickSource == null) {
            tickJob = scope.launch {
                while (isActive) {
                    delay(40)
                    val tel = sim.step()
                    _snapshots.value = tel
                }
            }
        }
    }

    suspend fun stepOnce(): Telemetry {
        val tel = tickSource?.stepSim() ?: sim.step()
        _snapshots.value = tel
        return tel
    }

    override suspend fun send(cmd: Command): Long {
        val id = nextId++
        val result = sim.apply(cmd)
        _events.emit(LinkEvent.Ack(id.toInt(), result.reject, result.tick))
        _snapshots.value = sim.currentTelemetry()
        return id
    }

    override fun close() {
        tickJob?.cancel()
        _events.tryEmit(LinkEvent.Disconnected)
    }
}
