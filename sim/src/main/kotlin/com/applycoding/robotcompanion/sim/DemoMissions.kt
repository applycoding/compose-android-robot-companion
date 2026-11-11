// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

object DemoMissions {
    const val DEMO_ID = "demo"
    const val DEMO_NAME = "Demo patrol"

    val waypoints: List<Pair<Double, Double>> = listOf(
        8.0 to 1.0,
        8.0 to 6.0,
        2.0 to 6.0,
    )

    fun startCommand(): Command.StartMission = Command.StartMission(DEMO_ID, DEMO_NAME, waypoints)
}
