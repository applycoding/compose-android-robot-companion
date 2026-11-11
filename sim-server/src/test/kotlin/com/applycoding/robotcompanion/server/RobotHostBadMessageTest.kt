// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.sim.Sim
import com.applycoding.robotcompanion.sim.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RobotHostBadMessageTest {
    @Test
    fun badInputReturnsBadMessageWithoutChangingState() = runBlocking {
        val cases = listOf(
            "{not json",
            """{"v":1,"id":1,"type":"nope"}""",
            """{"v":1,"id":1,"type":"start","missionId":"m","name":"n","waypoints":[[NaN,1.0]]}""",
            "a".repeat(9000),
        )
        for (json in cases) {
            val scope = CoroutineScope(Dispatchers.Default)
            val sim = Sim()
            val host = RobotHost(scope, lockstep = true, sim = sim)
            try {
                val before = sim.currentTelemetry()
                val replies = host.onClientMessage(json)
                assertEquals(1, replies.size, json.take(40))
                assertTrue(replies[0].contains("BAD_MESSAGE"), replies[0])
                assertTelemetryUnchanged(before, sim.currentTelemetry())
            } finally {
                scope.cancel()
            }
        }
    }

    private fun assertTelemetryUnchanged(before: Telemetry, after: Telemetry) {
        assertEquals(before, after)
    }
}
