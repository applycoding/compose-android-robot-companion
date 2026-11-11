// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class GoldenTraceTest {
    @Test
    fun matchesGoldenFile() {
        val sim = Sim()
        val support = SimTestSupport(sim)
        support.startDemo()
        val lines = mutableListOf<String>()
        lines.add(canonicalLine(sim.currentTelemetry()))
        run {
            var tel = sim.currentTelemetry()
            while (tel.tick < 627) {
                tel = support.stepWithHeartbeat()
                lines.add(canonicalLine(tel))
            }
        }
        val golden = File("src/test/resources/golden/demo_trace.txt")
        if (!golden.isFile) {
            fail("missing golden trace: ${golden.path}")
        }
        val expected = golden.readText().trimEnd().lines()
        assertEquals(expected.size, lines.size)
        expected.zip(lines).forEachIndexed { i, (e, a) ->
            assertEquals(e, a, "line $i")
        }
    }
}
