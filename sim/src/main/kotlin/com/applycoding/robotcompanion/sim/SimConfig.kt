// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

data class SimConfig(
    val dtMs: Int = 40,
    val vMax: Double = 0.8,
    val aMax: Double = 0.5,
    val brake: Double = 1.6,
    val wMax: Double = 1.2,
    val kHeading: Double = 2.0,
    val arriveTol: Double = 0.10,
    val heartbeatTimeoutTicks: Int = 13,
    val roomWidth: Double = 10.0,
    val roomHeight: Double = 8.0,
    val marginMinX: Double = 0.5,
    val marginMaxX: Double = 9.5,
    val marginMinY: Double = 0.5,
    val marginMaxY: Double = 7.5,
    val startX: Double = 1.0,
    val startY: Double = 1.0,
    val startTheta: Double = 0.0,
    val startBatteryPct: Double = 100.0,
    val drainBasePctPerTick: Double = 0.001,
    val drainMovingPctPerTick: Double = 0.01,
    val lowBatteryStartPct: Double = 20.0,
    val lowBatteryPausePct: Double = 10.0,
    val maxWaypoints: Int = 16,
    val minMissionNameLen: Int = 1,
    val maxMissionNameLen: Int = 32,
) {
    val dt: Double get() = dtMs / 1000.0

    companion object {
        val standard = SimConfig()
    }
}
