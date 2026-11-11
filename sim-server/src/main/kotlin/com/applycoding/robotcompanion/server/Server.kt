// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.server

import com.applycoding.robotcompanion.link.WireCodec
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SimServer(
    val port: Int,
    val lockstep: Boolean,
    val host: RobotHost,
) {
    private val codec = WireCodec()
    private var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    var resolvedPort: Int = port
        private set

    fun start() {
        engine = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            install(WebSockets)
            routing {
                webSocket("/v1/robot") {
                    val outbound = Channel<String>(capacity = 64)
                    if (!host.tryAttach(outbound)) {
                        send(Frame.Text(codec.encodeError("BUSY")))
                        close(CloseReason(CloseReason.Codes.NORMAL, "busy"))
                        return@webSocket
                    }
                    val forward = launch {
                        for (msg in outbound) {
                            send(Frame.Text(msg))
                        }
                    }
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val replies = host.onClientMessage(frame.readText())
                                for (r in replies) {
                                    send(Frame.Text(r))
                                }
                            }
                        }
                    } finally {
                        forward.cancel()
                        host.detach(outbound)
                    }
                }
            }
        }.start(wait = false)
        val bound = requireNotNull(engine)
        resolvedPort = runBlocking {
            bound.engine.resolvedConnectors().firstOrNull()?.port ?: port
        }
    }

    fun stop() {
        engine?.stop(1000, 2000)
        engine = null
    }
}
