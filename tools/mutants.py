#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class Mutant:
    mid: str
    search: str
    replace: str
    module: str
    test_class: str
    test_method: str


TEST_TASK = {
    ":sim": "test",
    ":link": "test",
    ":sim-server": "test",
    ":app": "testDebugUnitTest",
}


def test_result_dir(workdir: Path, module: str) -> Path:
    task = TEST_TASK[module]
    return workdir / module.lstrip(":") / "build/test-results" / task


def compile_task(module: str) -> str:
    if module == ":app":
        return ":app:compileDebugKotlin"
    return f"{module}:compileKotlin"


MUTANTS = [
    Mutant("S1", "val brake: Double = 1.6", "val brake: Double = 0.4", ":sim", "EStopTest", "restsAtTick113"),
    Mutant("S2", "heartbeatTimeoutTicks: Int = 13", "heartbeatTimeoutTicks: Int = 130", ":sim", "HeartbeatTest", "lostAtTick209"),
    Mutant("S3", "if (v != 0.0) return CommandResult(Reject.MOVING", "if (false && v != 0.0) return CommandResult(Reject.MOVING", ":sim", "MissionRulesTest", "resetRejectedWhileMoving"),
    Mutant("S4", "if (estopLatched) return CommandResult(Reject.ESTOP_LATCHED, appliedTick)", "if (false) return CommandResult(Reject.ESTOP_LATCHED, appliedTick)", ":sim", "MissionRulesTest", "startRejectedWhileLatched"),
    Mutant("S5", "vt = min(vt, cap)", "vt = vt", ":sim", "KinematicsTest", "demoCompletesAtTick627"),
    Mutant("S6", "if (!wx.isFinite() || !wy.isFinite()) return Reject.INVALID_MISSION", "if (false) return Reject.INVALID_MISSION", ":sim", "MissionRulesTest", "nonFiniteWaypointRejected"),
    Mutant("L1", "var dropEstop: Boolean = false", "var dropEstop: Boolean = true", ":sim-server", "LockstepLiveTest", "traceEqualsInProcess"),
    Mutant("L2", '"estop":${snapshot.estop}', '"estop":false', ":link", "WireCodecTest", "snapshotGolden"),
    Mutant(
        "L3",
        "is OwnerMsg.Tick -> {\n                    val snap = codec.encodeSnapshot(sim.step().toWire())",
        "is OwnerMsg.Tick -> {\n                    val snap = codec.encodeSnapshot(sim.currentTelemetry().toWire())",
        ":sim-server",
        "RealtimeLiveTest",
        "ticksAdvance",
    ),
    Mutant("A1", "onEvent(UiEvent.EStop)", "onEvent(UiEvent.Pause)", ":app", "ControlsUiTest", "estopLatchesAndRenders"),
    Mutant("A2", "radius = 14f,", "radius = 16f,", ":app", "ScreenshotTest", "midMission"),
    Mutant(
        "A3",
        "val state by vm.uiState.collectAsStateWithLifecycle()",
        "val state = remember(vm) { vm.uiState.value }",
        ":app",
        "ControlsUiTest",
        "estopLatchesAndRenders",
    ),
    Mutant("A4", "savedStateHandle[KEY_CATALOG] =", "if (false) savedStateHandle[KEY_CATALOG] =", ":app", "ProcessDeathTest", "catalogSurvives"),
]


def gradle_test(workdir: Path, module: str, class_name: str, method: str) -> tuple[int, list[str]]:
    env = os.environ.copy()
    task = TEST_TASK[module]
    cmd = ["./gradlew", f"{module}:{task}", f"--tests={class_name}.{method}", "--rerun-tasks", "-q"]
    proc = subprocess.run(cmd, cwd=workdir, env=env, capture_output=True, text=True)
    failed: list[str] = []
    result_dir = test_result_dir(workdir, module)
    if result_dir.is_dir():
        for xml in result_dir.glob("TEST-*.xml"):
            if not xml.name.endswith(f"{class_name}.xml"):
                continue
            tree = ET.parse(xml)
            for case in tree.findall("testcase"):
                if case.find("failure") is not None or case.find("error") is not None:
                    failed.append(case.get("name", ""))
    return proc.returncode, failed


def apply_patch(workdir: Path, mutant: Mutant) -> None:
    module_dir = workdir / mutant.module.lstrip(":")
    matches = 0
    for path in module_dir.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        if mutant.search in text:
            path.write_text(text.replace(mutant.search, mutant.replace, 1), encoding="utf-8")
            matches += 1
    if matches != 1:
        raise SystemExit(f"{mutant.mid}: expected one match, found {matches}")


def main() -> int:
    print("=== mutant baseline ===")
    for m in MUTANTS:
        code, failed = gradle_test(ROOT, m.module, m.test_class, m.test_method)
        if code != 0:
            print(f"baseline FAIL {m.test_class}.{m.test_method}: {failed}", file=sys.stderr)
            return 1
    print("=== mutant matrix ===")
    for m in MUTANTS:
        with tempfile.TemporaryDirectory() as tmp:
            work = Path(tmp) / "repo"
            shutil.copytree(
                ROOT,
                work,
                ignore=shutil.ignore_patterns(".git", "build", ".gradle"),
                dirs_exist_ok=True,
            )
            apply_patch(work, m)
            comp = subprocess.run(["./gradlew", compile_task(m.module), "-q"], cwd=work)
            if comp.returncode != 0:
                print(f"{m.mid} COMPILE_FAIL", file=sys.stderr)
                return 1
            code, failed = gradle_test(work, m.module, m.test_class, m.test_method)
            killed = code != 0 and m.test_method in failed
            status = "KILL" if killed else "SURVIVE"
            print(f"{m.mid}\t{m.test_class}.{m.test_method}\t{status}\tfailed={failed}")
            if not killed:
                return 1
    print("all mutants killed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
