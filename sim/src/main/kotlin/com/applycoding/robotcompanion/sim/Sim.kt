// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Sim(
    private val config: SimConfig = SimConfig.standard,
) {
    private var tick: Int = 0
    private var x: Double = config.startX
    private var y: Double = config.startY
    private var theta: Double = config.startTheta
    private var v: Double = 0.0
    private var w: Double = 0.0
    private var batteryPct: Double = config.startBatteryPct
    private var travelled: Double = 0.0
    private var lastHb: Int = 0
    private var estopLatched: Boolean = false
    private var missionId: String? = null
    private var missionName: String = ""
    private var mission: MissionStatus = MissionStatus.IDLE
    private var pause: PauseReason? = null
    private var waypoints: List<Pair<Double, Double>> = emptyList()
    private var waypointIndex: Int = 0

    fun currentTelemetry(): Telemetry = buildTelemetry()

    fun apply(cmd: Command): CommandResult {
        val appliedTick = tick
        when (cmd) {
            is Command.Heartbeat -> {
                lastHb = tick
            }
            is Command.EStop -> {
                estopLatched = true
                if (mission == MissionStatus.RUNNING || mission == MissionStatus.PAUSED) {
                    mission = MissionStatus.ABORTED
                    pause = null
                }
            }
            is Command.ResetEStop -> {
                if (!estopLatched) return CommandResult(Reject.NOT_LATCHED, appliedTick)
                if (v != 0.0) return CommandResult(Reject.MOVING, appliedTick)
                if (!linkOk()) return CommandResult(Reject.LINK_LOST, appliedTick)
                estopLatched = false
            }
            is Command.StartMission -> {
                if (estopLatched) return CommandResult(Reject.ESTOP_LATCHED, appliedTick)
                if (!linkOk()) return CommandResult(Reject.LINK_LOST, appliedTick)
                if (mission == MissionStatus.RUNNING || mission == MissionStatus.PAUSED) {
                    return CommandResult(Reject.BUSY, appliedTick)
                }
                val invalid = validateMission(cmd)
                if (invalid != null) return CommandResult(invalid, appliedTick)
                if (batteryPct < config.lowBatteryStartPct) return CommandResult(Reject.LOW_BATTERY, appliedTick)
                missionId = cmd.missionId
                missionName = cmd.name
                waypoints = cmd.waypoints
                waypointIndex = 0
                mission = MissionStatus.RUNNING
                pause = null
            }
            is Command.Pause -> {
                if (estopLatched) return CommandResult(Reject.ESTOP_LATCHED, appliedTick)
                if (mission != MissionStatus.RUNNING) return CommandResult(Reject.NOT_RUNNING, appliedTick)
                mission = MissionStatus.PAUSED
                pause = PauseReason.OPERATOR
            }
            is Command.Resume -> {
                if (estopLatched) return CommandResult(Reject.ESTOP_LATCHED, appliedTick)
                if (mission != MissionStatus.PAUSED) return CommandResult(Reject.NOT_PAUSED, appliedTick)
                if (!linkOk()) return CommandResult(Reject.LINK_LOST, appliedTick)
                if (pause == PauseReason.LOW_BATTERY && batteryPct < config.lowBatteryPausePct) {
                    return CommandResult(Reject.LOW_BATTERY, appliedTick)
                }
                mission = MissionStatus.RUNNING
                pause = null
            }
            is Command.Cancel -> {
                if (estopLatched) return CommandResult(Reject.ESTOP_LATCHED, appliedTick)
                if (mission != MissionStatus.RUNNING && mission != MissionStatus.PAUSED) {
                    return CommandResult(Reject.NOT_RUNNING, appliedTick)
                }
                mission = MissionStatus.CANCELLED
                pause = null
            }
        }
        val reject = when (cmd) {
            is Command.Heartbeat, is Command.EStop, is Command.ResetEStop,
            is Command.StartMission, is Command.Pause, is Command.Resume, is Command.Cancel,
            -> Reject.NONE
        }
        return CommandResult(reject, appliedTick)
    }

    fun step(): Telemetry {
        tick += 1
        val linkOk = linkOk()
        if (mission == MissionStatus.RUNNING) {
            when {
                !linkOk -> {
                    mission = MissionStatus.PAUSED
                    pause = PauseReason.LINK_LOST
                }
                batteryPct < config.lowBatteryPausePct -> {
                    mission = MissionStatus.PAUSED
                    pause = PauseReason.LOW_BATTERY
                }
            }
        }

        val latchedOrNotRunning = estopLatched || mission != MissionStatus.RUNNING
        if (latchedOrNotRunning) {
            w = 0.0
            val d = when {
                estopLatched || pause == PauseReason.LINK_LOST -> config.brake
                else -> config.aMax
            }
            v = max(0.0, v - d * config.dt)
        } else {
            integrateRunningMotion()
        }

        val prevX = x
        val prevY = y
        theta = wrapAngle(theta + w * config.dt)
        x = (x + v * cos(theta) * config.dt).coerceIn(0.0, config.roomWidth)
        y = (y + v * sin(theta) * config.dt).coerceIn(0.0, config.roomHeight)
        travelled += hypot(x - prevX, y - prevY)

        batteryPct -= config.drainBasePctPerTick +
            config.drainMovingPctPerTick * (v / config.vMax)
        batteryPct = max(0.0, batteryPct)

        return buildTelemetry(linkOk)
    }

    private fun integrateRunningMotion() {
        if (waypoints.isEmpty()) {
            w = 0.0
            v = max(0.0, v - config.aMax * config.dt)
            return
        }
        val target = waypoints[waypointIndex]
        val dx = target.first - x
        val dy = target.second - y
        val dist = hypot(dx, dy)
        val lastWp = waypointIndex >= waypoints.lastIndex

        if (dist <= config.arriveTol && !lastWp) {
            waypointIndex += 1
        }

        val distAfterSwitch = hypot(
            waypoints[waypointIndex].first - x,
            waypoints[waypointIndex].second - y,
        )
        val onLast = waypointIndex >= waypoints.lastIndex

        if (onLast && distAfterSwitch <= config.arriveTol && v <= config.aMax * config.dt) {
            mission = MissionStatus.COMPLETED
            v = 0.0
            w = 0.0
            return
        }

        val tdx = waypoints[waypointIndex].first - x
        val tdy = waypoints[waypointIndex].second - y
        val tdist = hypot(tdx, tdy)
        val headingToTarget = atan2(tdy, tdx)
        val e = wrapAngle(headingToTarget - theta)
        w = clamp(config.kHeading * e, -config.wMax, config.wMax)
        var vt = config.vMax * max(0.0, cos(e))
        if (onLast) {
            val cap = sqrt(2.0 * config.aMax * max(0.0, tdist - config.arriveTol / 2.0))
            vt = min(vt, cap)
        }
        v += clamp(vt - v, -config.aMax * config.dt, config.aMax * config.dt)
    }

    private fun validateMission(cmd: Command.StartMission): Reject? {
        if (cmd.waypoints.isEmpty() || cmd.waypoints.size > config.maxWaypoints) {
            return Reject.INVALID_MISSION
        }
        if (cmd.name.length !in config.minMissionNameLen..config.maxMissionNameLen) {
            return Reject.INVALID_MISSION
        }
        for ((wx, wy) in cmd.waypoints) {
            if (!wx.isFinite() || !wy.isFinite()) return Reject.INVALID_MISSION
            if (wx.isFinite() && wy.isFinite()) {
                if (wx !in config.marginMinX..config.marginMaxX || wy !in config.marginMinY..config.marginMaxY) {
                    return Reject.INVALID_MISSION
                }
            }
        }
        return null
    }

    private fun linkOk(): Boolean = tick - lastHb <= config.heartbeatTimeoutTicks

    private fun buildTelemetry(forcedLinkOk: Boolean? = null): Telemetry = Telemetry(
        tick = tick,
        x = x,
        y = y,
        theta = theta,
        v = v,
        w = w,
        batteryPct = batteryPct,
        missionId = missionId,
        mission = mission,
        pause = pause,
        waypointIndex = waypointIndex,
        estopLatched = estopLatched,
        linkOk = forcedLinkOk ?: linkOk(),
        travelled = travelled,
    )

    companion object {
        fun wrapAngle(a: Double): Double {
            var r = a
            while (r > Math.PI) r -= 2.0 * Math.PI
            while (r < -Math.PI) r += 2.0 * Math.PI
            return r
        }

        fun clamp(value: Double, lo: Double, hi: Double): Double = max(lo, min(hi, value))

        private fun hypot(dx: Double, dy: Double): Double = sqrt(dx * dx + dy * dy)
    }
}
