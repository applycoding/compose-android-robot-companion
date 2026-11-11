// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HeartbeatSender(
    private val transport: Transport,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                transport.send(Command.Heartbeat)
                delay(200)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
