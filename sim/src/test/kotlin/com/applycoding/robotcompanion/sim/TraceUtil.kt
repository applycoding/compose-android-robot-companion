// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robotcompanion.sim

import java.util.Locale

fun canonicalLine(t: Telemetry): String = String.format(
    Locale.ROOT,
    "%d %.6f %.6f %.6f %.6f",
    t.tick,
    t.x,
    t.y,
    t.theta,
    t.v,
)
