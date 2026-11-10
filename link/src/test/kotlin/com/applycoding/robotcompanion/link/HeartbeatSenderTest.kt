// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.Telemetry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HeartbeatSenderTest {
    @Test
    fun sendsEvery200ms() = runTest {
        val transport = FakeTransport()
        val sender = HeartbeatSender(transport, this as TestScope)
        sender.start()
        advanceTimeBy(1000)
        sender.stop()
        assertEquals(5, transport.heartbeats)
    }
}

private class FakeTransport : Transport {
    var heartbeats = 0
    override val snapshots = MutableStateFlow<Telemetry?>(null)
    override val events: Flow<LinkEvent> = emptyFlow()
    override suspend fun send(cmd: Command): Long {
        if (cmd is Command.Heartbeat) heartbeats++
        return 1L
    }
    override fun close() {}
}
