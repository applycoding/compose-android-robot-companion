// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

enum class MissionStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    ABORTED,
}

enum class PauseReason {
    OPERATOR,
    LINK_LOST,
    LOW_BATTERY,
}

enum class Reject {
  NONE,
    ESTOP_LATCHED,
    LINK_LOST,
    BUSY,
    INVALID_MISSION,
    LOW_BATTERY,
    NOT_RUNNING,
    NOT_PAUSED,
    NOT_LATCHED,
    MOVING,
}

sealed class Command {
    data object Heartbeat : Command()
    data class StartMission(val missionId: String, val name: String, val waypoints: List<Pair<Double, Double>>) : Command()
    data object Pause : Command()
    data object Resume : Command()
    data object Cancel : Command()
    data object EStop : Command()
    data object ResetEStop : Command()
}

data class CommandResult(val reject: Reject, val tick: Int)

data class Telemetry(
    val tick: Int,
    val x: Double,
    val y: Double,
    val theta: Double,
    val v: Double,
    val w: Double,
    val batteryPct: Double,
    val missionId: String?,
    val mission: MissionStatus,
    val pause: PauseReason?,
    val waypointIndex: Int,
    val estopLatched: Boolean,
    val linkOk: Boolean,
    val travelled: Double,
)
