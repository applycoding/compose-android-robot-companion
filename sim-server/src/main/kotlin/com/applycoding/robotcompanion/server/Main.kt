// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val lockstep = args.contains("--lockstep")
    val port = args.firstOrNull { it.startsWith("--port=") }?.substringAfter("=")?.toIntOrNull() ?: 8787
    val scope = CoroutineScope(Dispatchers.Default)
    val host = RobotHost(scope, lockstep = lockstep)
    val server = SimServer(port, lockstep, host)
    server.start()
    println("sim-server on 127.0.0.1:${server.resolvedPort} lockstep=$lockstep")
    runBlocking { kotlinx.coroutines.delay(Long.MAX_VALUE) }
}
