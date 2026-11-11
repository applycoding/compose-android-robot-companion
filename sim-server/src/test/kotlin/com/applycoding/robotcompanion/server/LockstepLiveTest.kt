// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.sim.MissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LockstepLiveTest {
    private lateinit var server: SimServer
    private val scope = CoroutineScope(Dispatchers.Default)
    private val client = testHttpClient()

    @Before
    fun setUp() {
        val host = RobotHost(scope, lockstep = true)
        server = SimServer(0, true, host)
        server.start()
    }

    @After
    fun tearDown() {
        client.close()
        server.stop()
        scope.cancel()
    }

    @Test
    fun traceEqualsInProcess() = runBlocking {
        val reference = LockstepScript.runInProcess()
        val transport = testWebSocketTransport(scope, server.resolvedPort, client)
        try {
            awaitConnected(transport)
            val live = withTimeout(15_000) { LockstepScript.runWithTransport(transport) }
            assertEquals("expected 130 lockstep snapshots", 130, live.size)
            reference.zip(live).forEachIndexed { index, (expected, actual) ->
                assertEquals("snapshot $index tick", expected.tick, actual.tick)
                assertEquals("snapshot $index", expected, actual)
            }
            val rest = live.first { it.tick == 113 }
            assertEquals("rest speed at tick 113", 0.0, rest.v, 0.0)
            assertEquals("mission at tick 113", MissionStatus.ABORTED, rest.mission)
        } finally {
            transport.close()
        }
    }
}
