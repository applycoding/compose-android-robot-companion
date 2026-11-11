// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.PauseReason
import com.applycoding.robotcompanion.sim.Reject
import com.applycoding.robotcompanion.sim.Telemetry

data class WireSnapshot(
    val tick: Int,
    val x: Double,
    val y: Double,
    val theta: Double,
    val speed: Double,
    val battery: Double,
    val mission: String,
    val pause: String?,
    val wp: Int,
    val estop: Boolean,
    val link: Boolean,
    val missionId: String?,
)

fun Telemetry.toWire(): WireSnapshot = WireSnapshot(
    tick = tick,
    x = x,
    y = y,
    theta = theta,
    speed = v,
    battery = batteryPct,
    mission = mission.name,
    pause = pause?.name,
    wp = waypointIndex,
    estop = estopLatched,
    link = linkOk,
    missionId = missionId,
)

fun missionFromWire(name: String): MissionStatus = MissionStatus.valueOf(name)

fun pauseFromWire(name: String?): PauseReason? = name?.let { PauseReason.valueOf(it) }

fun rejectName(r: Reject): String = when (r) {
    Reject.NONE -> "NONE"
    Reject.ESTOP_LATCHED -> "ESTOP_LATCHED"
    Reject.LINK_LOST -> "LINK_LOST"
    Reject.BUSY -> "BUSY"
    Reject.INVALID_MISSION -> "INVALID_MISSION"
    Reject.LOW_BATTERY -> "LOW_BATTERY"
    Reject.NOT_RUNNING -> "NOT_RUNNING"
    Reject.NOT_PAUSED -> "NOT_PAUSED"
    Reject.NOT_LATCHED -> "NOT_LATCHED"
    Reject.MOVING -> "MOVING"
}
