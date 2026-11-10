// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.applycoding.robotcompanion.sim.SimConfig
import com.applycoding.robotcompanion.sim.Telemetry
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapPanel(
    telemetry: Telemetry?,
    waypoints: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
) {
    val cfg = SimConfig.standard
    Text(
        text = "Map",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .aspectRatio((cfg.roomWidth / cfg.roomHeight).toFloat()),
    ) {
        val scaleX = size.width / cfg.roomWidth.toFloat()
        val scaleY = size.height / cfg.roomHeight.toFloat()
        fun mapX(x: Double) = (x * scaleX).toFloat()
        fun mapY(y: Double) = size.height - (y * scaleY).toFloat()

        drawRect(
            color = androidx.compose.ui.graphics.Color(0xFFECEFF1),
            size = size,
        )
        drawRect(
            color = androidx.compose.ui.graphics.Color(0xFF546E7A),
            style = Stroke(width = 3f),
            size = size,
        )

        if (waypoints.isNotEmpty()) {
            val path = Path()
            val first = waypoints.first()
            path.moveTo(mapX(first.first), mapY(first.second))
            for (wp in waypoints.drop(1)) {
                path.lineTo(mapX(wp.first), mapY(wp.second))
            }
            drawPath(
                path = path,
                color = androidx.compose.ui.graphics.Color(0xFF1E88E5),
                style = Stroke(width = 4f, cap = StrokeCap.Round),
            )
        }

        waypoints.forEachIndexed { index, (x, y) ->
            val center = Offset(mapX(x), mapY(y))
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFF1565C0),
                radius = 10f,
                center = center,
            )
            drawCircle(
                color = androidx.compose.ui.graphics.Color.White,
                radius = 4f,
                center = center,
            )
        }

        telemetry?.let { tel ->
            val cx = mapX(tel.x)
            val cy = mapY(tel.y)
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFFE53935),
                radius = 14f,
                center = Offset(cx, cy),
            )
            val hx = cx + cos(tel.theta).toFloat() * 22f
            val hy = cy - sin(tel.theta).toFloat() * 22f
            drawLine(
                color = androidx.compose.ui.graphics.Color(0xFF212121),
                start = Offset(cx, cy),
                end = Offset(hx, hy),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}
