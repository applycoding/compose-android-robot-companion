// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.applycoding.robotcompanion.link.InProcessTransport
import com.applycoding.robotcompanion.link.ManualTickSource
import com.applycoding.robotcompanion.sim.Command
import com.applycoding.robotcompanion.sim.DemoMissions
import com.applycoding.robotcompanion.sim.MissionStatus
import com.applycoding.robotcompanion.sim.Sim
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
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
    val composeRule = createAndroidComposeRule<HostActivity>()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun fullAppFrames() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        HostActivity.testTransportFactory = transportFactory(scope)
        composeRule.activityRule.scenario.recreate()
        val vm = composeRule.activity.robotViewModel
        val transport = vm.transportForTests() as InProcessTransport

        captureAtTick(transport, vm, 0, "screenshot_tick_0.png")

        runBlocking {
            transport.send(Command.Heartbeat)
            transport.send(DemoMissions.startCommand())
        }
        captureAtTick(transport, vm, 150, "screenshot_tick_150.png")
        runToTick(transport, 300)
        composeRule.waitForIdle()
        assertEquals(300, vm.uiState.value.telemetry?.tick)
        assertEquals(MissionStatus.RUNNING, vm.uiState.value.telemetry?.mission)
        captureFrame("screenshot_tick_300.png")
        captureAtTick(transport, vm, 450, "screenshot_tick_450.png")
        runToTick(transport, 627)
        composeRule.waitForIdle()
        assertEquals(627, vm.uiState.value.telemetry?.tick)
        assertEquals(MissionStatus.COMPLETED, vm.uiState.value.telemetry?.mission)
        captureFrame("screenshot_tick_627.png")
    }

    @Test
    fun estopLatchedFrame() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        HostActivity.testTransportFactory = transportFactory(scope)
        composeRule.activityRule.scenario.recreate()
        val vmEstop = composeRule.activity.robotViewModel
        val transportEstop = vmEstop.transportForTests() as InProcessTransport
        runBlocking {
            transportEstop.send(Command.Heartbeat)
            transportEstop.send(DemoMissions.startCommand())
        }
        runToTick(transportEstop, 300)
        assertEquals(300, transportEstop.snapshots.value?.tick)
        val endTick = runBlocking {
            transportEstop.send(Command.Heartbeat)
            transportEstop.send(Command.EStop)
            var tick = transportEstop.stepOnce().tick
            while (tick < 313) {
                if (tick % 5 == 0) {
                    transportEstop.send(Command.Heartbeat)
                }
                tick = transportEstop.stepOnce().tick
            }
            tick
        }
        vmEstop.syncFromTransportForTest()
        composeRule.waitForIdle()
        assertEquals(313, endTick)
        assertEquals(true, transportEstop.snapshots.value?.estopLatched)
        composeRule.onNodeWithTag("estop_button").performScrollTo()
        composeRule.onNodeWithText("E-STOP LATCHED", substring = true).assertExists()
        captureFrame("screenshot_tick_313_estop.png")
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
                tick = transport.stepOnce().tick
            }
        }
    }

    private fun transportFactory(scope: CoroutineScope): TransportFactory = TransportFactory { _, _ ->
        val sim = Sim()
        InProcessTransport(
            sim = sim,
            tickSource = ManualTickSource { sim.step() },
            scope = scope,
            realtime = false,
        )
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
