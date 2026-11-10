// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WireCodecTest {
    private val codec = WireCodec()

    @Test
    fun snapshotGolden() {
        val snap = WireSnapshot(
            tick = 113,
            x = 8.44,
            y = 2.54,
            theta = 1.57,
            speed = 0.0,
            battery = 99.5,
            mission = "ABORTED",
            pause = null,
            wp = 0,
            estop = true,
            link = true,
            missionId = "demo",
        )
        val encoded = codec.encodeSnapshot(snap)
        val decoded = codec.decodeServer(encoded) as WireMessage.ServerSnapshot
        assertEquals(snap, decoded.snapshot)
    }

    @Test
    fun rejectsUnknownKey() {
        assertFailsWith<Exception> {
            codec.decodeClient("""{"v":1,"id":1,"type":"hello","extra":1}""")
        }
    }

    @Test
    fun rejectsBadVersion() {
        assertFailsWith<Exception> {
            codec.decodeClient("""{"v":2,"id":1,"type":"hello"}""")
        }
    }

    @Test
    fun rejectsNan() {
        assertFailsWith<Exception> {
            codec.decodeClient(
                """{"v":1,"id":1,"type":"start","missionId":"m","name":"n","waypoints":[[NaN,1.0]]}""",
            )
        }
    }

    @Test
    fun rejectsOversize() {
        val big = "a".repeat(9000)
        assertFailsWith<IllegalArgumentException> {
            codec.decodeClient(big)
        }
    }
}
