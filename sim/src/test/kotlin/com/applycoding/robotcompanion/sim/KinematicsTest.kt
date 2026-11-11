// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KinematicsTest {
    @Test
    fun reachesVMaxAtTick40() {
        val support = SimTestSupport()
        support.startDemo()
        val at39 = support.runTo(39)
        assertEquals(39, at39.tick)
        assertTrue(at39.v < SimConfig.standard.vMax)
        val at40 = support.stepWithHeartbeat()
        assertEquals(40, at40.tick)
        assertEquals(SimConfig.standard.vMax, at40.v, 0.001)
        assertEquals(MissionStatus.RUNNING, at40.mission)
    }

    @Test
    fun waypointSwitchAt237And407() {
        val support = SimTestSupport()
        support.startDemo()
        var last = support.runTo(236)
        assertEquals(0, last.waypointIndex)
        last = support.stepWithHeartbeat()
        assertEquals(237, last.tick)
        assertEquals(1, last.waypointIndex)
        support.runTo(406)
        last = support.stepWithHeartbeat()
        assertEquals(407, last.tick)
        assertEquals(2, last.waypointIndex)
    }

    @Test
    fun demoCompletesAtTick627() {
        val support = SimTestSupport()
        support.startDemo()
        val at626 = support.runTo(626)
        assertEquals(MissionStatus.RUNNING, at626.mission)
        val at627 = support.stepWithHeartbeat()
        assertEquals(627, at627.tick)
        assertEquals(MissionStatus.COMPLETED, at627.mission)
        assertEquals(2.0339, at627.x, 0.001)
        assertEquals(6.0031, at627.y, 0.001)
        assertEquals(18.399, at627.travelled, 0.01)
        assertEquals(93.623, at627.batteryPct, 0.01)
    }

    @Test
    fun poseAtTick300() {
        val support = SimTestSupport()
        support.startDemo()
        val t = support.runTo(300)
        assertEquals(8.44, t.x, 0.01)
        assertEquals(2.54, t.y, 0.01)
        assertEquals(0.8, t.v, 0.01)
        assertEquals(MissionStatus.RUNNING, t.mission)
    }
}
