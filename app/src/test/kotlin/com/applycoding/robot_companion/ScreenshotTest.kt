// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.ManualTickSource
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.Sim
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule()

    @Test
    fun fullAppFrames() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val sim = Sim()
        val transport = InProcessTransport(
            sim = sim,
            tickSource = ManualTickSource { sim.step() },
            scope = scope,
            realtime = false,
        )
        val factory = TransportFactory { _, _ -> transport }
        val vm = RobotViewModel(androidx.lifecycle.SavedStateHandle(), factory, heartbeatEnabled = false)

        composeRule.setContent {
            androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
            val state = vm.uiState.collectAsStateWithLifecycle()
            CompanionApp(uiState = state.value, onEvent = vm::onEvent)
        }

        captureAtTick(transport, vm, 0, "screenshot_tick_0.png")

        runBlocking {
            transport.send(Command.Heartbeat)
            transport.send(DemoMissions.startCommand())
        }
        captureAtTick(transport, vm, 150, "screenshot_tick_150.png")
        captureAtTick(transport, vm, 300, "screenshot_tick_300.png")
        val mid = vm.uiState.value.telemetry
        assertEquals(MissionStatus.RUNNING, mid?.mission)

        captureAtTick(transport, vm, 450, "screenshot_tick_450.png")
        captureAtTick(transport, vm, 627, "screenshot_tick_627.png")

        val simEstop = Sim()
        val transportEstop = InProcessTransport(
            sim = simEstop,
            tickSource = ManualTickSource { simEstop.step() },
            scope = scope,
            realtime = false,
        )
        val vmEstop = RobotViewModel(
            androidx.lifecycle.SavedStateHandle(),
            TransportFactory { _, _ -> transportEstop },
            heartbeatEnabled = false,
        )
        composeRule.setContent {
            androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
            val state = vmEstop.uiState.collectAsStateWithLifecycle()
            CompanionApp(uiState = state.value, onEvent = vmEstop::onEvent)
        }
        runBlocking {
            transportEstop.send(Command.Heartbeat)
            transportEstop.send(DemoMissions.startCommand())
        }
        runToTick(transportEstop, 300)
        runBlocking { transportEstop.send(Command.EStop) }
        runBlocking { transportEstop.stepOnce() }
        runToTick(transportEstop, 313)
        composeRule.waitForIdle()
        assertEquals(true, vmEstop.uiState.value.telemetry?.estopLatched)
        captureFrame("screenshot_tick_313_estop.png")
    }

    @Test
    fun midMission() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val sim = Sim()
        val transport = InProcessTransport(
            sim = sim,
            tickSource = ManualTickSource { sim.step() },
            scope = scope,
            realtime = false,
        )
        val vm = RobotViewModel(
            androidx.lifecycle.SavedStateHandle(),
            TransportFactory { _, _ -> transport },
            heartbeatEnabled = false,
        )
        composeRule.setContent {
            androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
            val state = vm.uiState.collectAsStateWithLifecycle()
            CompanionApp(uiState = state.value, onEvent = vm::onEvent)
        }
        runBlocking {
            transport.send(Command.Heartbeat)
            transport.send(DemoMissions.startCommand())
        }
        runToTick(transport, 300)
        composeRule.waitForIdle()
        captureFrame("screenshot_mid_mission.png")
        assertEquals(300, vm.uiState.value.telemetry?.tick)
    }

    private fun captureAtTick(transport: InProcessTransport, vm: RobotViewModel, tick: Int, fileName: String) {
        runToTick(transport, tick)
        composeRule.waitForIdle()
        assertEquals(tick, vm.uiState.value.telemetry?.tick)
        captureFrame(fileName)
    }

    private fun runToTick(transport: InProcessTransport, target: Int) {
        runBlocking {
            var tick = transport.snapshots.value?.tick ?: 0
            while (tick < target) {
                if (tick % 5 == 0) {
                    transport.send(Command.Heartbeat)
                }
                transport.stepOnce()
                tick = transport.snapshots.value?.tick ?: tick
            }
        }
    }

    private fun captureFrame(fileName: String) {
        val file = File("src/test/screenshots/$fileName")
        file.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(file)
        val image = ImageIO.read(file)
        assertEquals(1080, image.width)
        assertEquals(2400, image.height)
        BitmapFactory.decodeFile(file.absolutePath)
    }
}
