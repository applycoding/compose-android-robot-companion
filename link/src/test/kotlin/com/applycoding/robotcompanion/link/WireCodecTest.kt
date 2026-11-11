// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Reject
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun clientMessageGoldens() {
        val files = listOf(
            "client_hello.json",
            "client_heartbeat.json",
            "client_start.json",
            "client_pause.json",
            "client_resume.json",
            "client_cancel.json",
            "client_estop.json",
            "client_reset_estop.json",
            "client_step.json",
        )
        for (file in files) {
            val golden = loadGolden(file)
            val decoded = codec.decodeClient(golden)
            val encoded = codec.encodeClient(decoded)
            assertEquals(golden, encoded, file)
        }
    }

    @Test
    fun serverMessageGoldens() {
        val welcomeGolden = loadGolden("server_welcome.json")
        val welcome = codec.decodeServer(welcomeGolden) as WireMessage.ServerWelcome
        assertEquals(welcomeGolden, codec.encodeWelcome(welcome.tickMs, welcome.mode))

        val snapGolden = loadGolden("server_snapshot.json")
        val snapMsg = codec.decodeServer(snapGolden) as WireMessage.ServerSnapshot
        assertEquals(snapGolden, codec.encodeSnapshot(snapMsg.snapshot))

        val ackGolden = loadGolden("server_ack.json")
        val ack = codec.decodeServer(ackGolden) as WireMessage.ServerAck
        assertEquals(ackGolden, codec.encodeAck(ack.id, Reject.NONE, ack.tick))

        val errGolden = loadGolden("server_error.json")
        val err = codec.decodeServer(errGolden) as WireMessage.ServerError
        assertEquals(errGolden, codec.encodeError(err.code))
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

    private fun loadGolden(name: String): String {
        val bytes = javaClass.getResourceAsStream("/golden/$name")!!.readAllBytes()
        return String(bytes, StandardCharsets.UTF_8).trimEnd()
    }
}
