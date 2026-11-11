// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.Reject
import com.applycoding.robotcompanion.sim.Telemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class LinkEvent {
    data object Connected : LinkEvent()
    data object Disconnected : LinkEvent()
    data class Welcome(val tickMs: Int, val mode: String) : LinkEvent()
    data class Ack(val id: Int, val result: Reject, val tick: Int) : LinkEvent()
    data class Error(val code: String) : LinkEvent()
}

interface Transport {
    val snapshots: StateFlow<Telemetry?>
    val events: Flow<LinkEvent>
    suspend fun send(cmd: Command): Long
    fun close()
}

interface TickSource {
    suspend fun stepSim(): Telemetry
}

class ManualTickSource(private val stepper: () -> Telemetry) : TickSource {
    override suspend fun stepSim(): Telemetry = stepper()
}
