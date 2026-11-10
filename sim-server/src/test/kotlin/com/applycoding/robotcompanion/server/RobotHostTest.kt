// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import kotlinx.coroutines.runBlocking
import org.junit.Test

class RobotHostTest {
    @Test
    fun lockstepStepReturnsSnapshot() = runBlocking {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        val host = RobotHost(scope, lockstep = true)
        val replies = host.onClientMessage("""{"v":1,"id":1,"type":"step"}""")
        println(replies)
        assert(replies.any { it.contains("\"speed\"") })
    }
}
