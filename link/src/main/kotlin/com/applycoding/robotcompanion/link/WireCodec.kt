// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.link

import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.Reject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

sealed class WireMessage {
    data class ClientHello(val id: Int) : WireMessage()
    data class ClientHeartbeat(val id: Int) : WireMessage()
    data class ClientStart(
        val id: Int,
        val missionId: String,
        val name: String,
        val waypoints: List<Pair<Double, Double>>,
    ) : WireMessage()
    data class ClientSimple(val id: Int, val type: String) : WireMessage()
    data class ServerWelcome(val tickMs: Int, val mode: String) : WireMessage()
    data class ServerSnapshot(val snapshot: WireSnapshot) : WireMessage()
    data class ServerAck(val id: Int, val result: String, val tick: Int) : WireMessage()
    data class ServerError(val code: String) : WireMessage()
}

class WireCodec {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
        isLenient = false
    }

    fun encodeClient(message: WireMessage): String = when (message) {
        is WireMessage.ClientHello -> simpleClient(message.id, "hello")
        is WireMessage.ClientHeartbeat -> simpleClient(message.id, "heartbeat")
        is WireMessage.ClientSimple -> simpleClient(message.id, message.type)
        is WireMessage.ClientStart -> {
            val wps = message.waypoints.joinToString(",", "[", "]") { "[${it.first},${it.second}]" }
            """{"v":1,"id":${message.id},"type":"start","missionId":"${message.missionId}","name":"${message.name}","waypoints":$wps}"""
        }
        else -> error("client only")
    }

    private fun simpleClient(id: Int, type: String): String = """{"v":1,"id":$id,"type":"$type"}"""

    fun encodeWelcome(tickMs: Int, mode: String): String =
        """{"v":1,"tickMs":$tickMs,"mode":"$mode"}"""

    fun encodeSnapshot(snapshot: WireSnapshot): String {
        val pause = snapshot.pause?.let { "\"$it\"" } ?: "null"
        val missionId = snapshot.missionId?.let { "\"$it\"" } ?: "null"
        return """{"v":1,"tick":${snapshot.tick},"x":${snapshot.x},"y":${snapshot.y},"theta":${snapshot.theta},"speed":${snapshot.speed},"battery":${snapshot.battery},"mission":"${snapshot.mission}","pause":$pause,"wp":${snapshot.wp},"estop":${snapshot.estop},"link":${snapshot.link},"missionId":$missionId}"""
    }

    fun encodeAck(id: Int, result: Reject, tick: Int): String =
        """{"v":1,"id":$id,"result":"${rejectName(result)}","tick":$tick}"""

    fun encodeError(code: String): String = """{"v":1,"code":"$code"}"""

    fun decodeClient(text: String): WireMessage {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_FRAME) { "oversized" }
        val root = json.parseToJsonElement(text).jsonObject
        requireVersion(root)
        val allowedHello = setOf("v", "id", "type")
        val allowedStart = setOf("v", "id", "type", "missionId", "name", "waypoints")
        when (root.requireString("type")) {
            "hello" -> require(root.keys == allowedHello) { "unknown key" }
            "start" -> require(root.keys == allowedStart) { "unknown key" }
            else -> {
                val allowed = setOf("v", "id", "type")
                require(root.keys == allowed) { "unknown key" }
            }
        }
        val id = root.requireInt("id")
        return when (val type = root.requireString("type")) {
            "hello" -> WireMessage.ClientHello(id)
            "heartbeat" -> WireMessage.ClientHeartbeat(id)
            "pause", "resume", "cancel", "estop", "reset_estop", "step" -> WireMessage.ClientSimple(id, type)
            "start" -> {
                val missionId = root.requireString("missionId")
                val name = root.requireString("name")
                val wps = root["waypoints"]?.jsonArray ?: error("waypoints")
                val waypoints = wps.map { arr ->
                    val pair = arr.jsonArray
                    val x = pair[0].jsonPrimitive.double
                    val y = pair[1].jsonPrimitive.double
                    require(x.isFinite() && y.isFinite())
                    x to y
                }
                WireMessage.ClientStart(id, missionId, name, waypoints)
            }
            else -> error("unknown type")
        }
    }

    fun decodeServer(text: String): WireMessage {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_FRAME) { "oversized" }
        val root = json.parseToJsonElement(text).jsonObject
        requireVersion(root)
        return when {
            root.containsKey("tickMs") -> WireMessage.ServerWelcome(
                tickMs = root.requireInt("tickMs"),
                mode = root.requireString("mode"),
            )
            root.containsKey("code") -> WireMessage.ServerError(root.requireString("code"))
            root.containsKey("result") -> WireMessage.ServerAck(
                id = root.requireInt("id"),
                result = root.requireString("result"),
                tick = root.requireInt("tick"),
            )
            root.containsKey("tick") -> WireMessage.ServerSnapshot(
                snapshot = WireSnapshot(
                    tick = root.requireInt("tick"),
                    x = root.requireDouble("x"),
                    y = root.requireDouble("y"),
                    theta = root.requireDouble("theta"),
                    speed = root.requireDouble("speed"),
                    battery = root.requireDouble("battery"),
                    mission = root.requireString("mission"),
                    pause = root["pause"]?.jsonPrimitive?.contentOrNull,
                    wp = root.requireInt("wp"),
                    estop = root.requireBool("estop"),
                    link = root.requireBool("link"),
                    missionId = root["missionId"]?.jsonPrimitive?.contentOrNull,
                ),
            )
            else -> error("bad message")
        }
    }

    fun tryDecodeClient(text: String): WireMessage? = runCatching { decodeClient(text) }.getOrNull()

    private fun baseClient(id: Int, type: String): JsonObject = buildJsonObject {
        put("v", 1)
        put("id", id)
        put("type", type)
    }

    private fun requireVersion(root: JsonObject) {
        val v = root["v"]?.jsonPrimitive?.int ?: error("version")
        require(v == 1) { "bad version" }
    }

    private fun JsonObject.requireString(key: String): String =
        this[key]?.jsonPrimitive?.content ?: error(key)

    private fun JsonObject.requireInt(key: String): Int =
        this[key]?.jsonPrimitive?.int ?: error(key)

    private fun JsonObject.requireDouble(key: String): Double {
        val d = this[key]?.jsonPrimitive?.double ?: error(key)
        require(d.isFinite())
        return d
    }

    private fun JsonObject.requireBool(key: String): Boolean =
        this[key]?.jsonPrimitive?.boolean ?: error(key)

    companion object {
        const val MAX_FRAME = 8 * 1024
    }
}

fun WireMessage.ClientSimple.toCommand(): Command = when (type) {
    "heartbeat" -> Command.Heartbeat
    "pause" -> Command.Pause
    "resume" -> Command.Resume
    "cancel" -> Command.Cancel
    "estop" -> Command.EStop
    "reset_estop" -> Command.ResetEStop
    else -> error(type)
}
