# -*- coding: utf-8 -*-
"""Run :app:connectedDebugAndroidTest on the CI emulator of Build integrity, once more after an emulator failure.

Run 39284420735 of b3489b0 (2026-10-19) failed and hung on the emulator, not on the plugin: the first attempt's API 35
16 KB image threw a NullPointerException inside system_server (`SomeArgs.mNext`) while HomeScreenDeviceTest switched
the network, the API 37.1 image failed `package install-commit` ("Failure calling service package") and
DELETE_FAILED_INTERNAL_ERROR; in the second attempt the API 37.1 emulator was gone ("No compatible devices connected")
right after boot and the `adb logcat -d` that followed waited for it until the job's 50 minute limit cancelled the
run. 3-Stove Agent met the same images (its run_main_suite.py).

This script runs the suite with a time limit; when it fails with one of the emulator signatures below (or adb lost the
device), it waits for the device and its boot (bounded), keeps the first attempt's reports in
app/build/reports/androidTests-attempt-1 and runs the suite once more. A failure that repeats stays a failure; a test
failure without such a signature is never retried. Every adb command has a timeout, so a lost emulator ends the job
instead of hanging it. The logcat of each attempt goes to app/build/reports/androidTests/.

Usage (inside reactivecircus/android-emulator-runner, from the repository root):
  python3 .python/ci_connected_tests.py
"""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ["./gradlew", ":app:connectedDebugAndroidTest", "-Pautojs.gradle.build.number.auto.increment.enabled=false",
          "-Pautojs.gradle.build.time.update.enabled=false", "--console=plain", "--stacktrace"]
SUITE_TIMEOUT = 25 * 60
DEVICE_TIMEOUT = 300
EMULATOR_FAILURE = re.compile("|".join([
    r"No compatible devices connected",
    r"System has crashed",
    r"DeadSystemException",
    r"Failed to commit install session",
    r"DELETE_FAILED_INTERNAL_ERROR",
    r"Failure calling service package",
    r"SomeArgs\.mNext",
    r"device offline",
    r"device '[^']*' not found",
    r"ShellCommandUnresponsiveException",
]))
REPORTS = ROOT / "app" / "build" / "reports" / "androidTests"
RESULTS = ROOT / "app" / "build" / "outputs" / "androidTest-results" / "connected"


def adb(*command, timeout=60):
    try:
        return subprocess.run(["adb", *command], capture_output=True, text=True, encoding="utf-8", errors="replace",
                              timeout=timeout).stdout.strip()
    except subprocess.TimeoutExpired:
        return ""


def device_ready():
    """Waits (bounded) for one device in state `device` with sys.boot_completed=1."""
    deadline = time.time() + DEVICE_TIMEOUT
    while time.time() < deadline:
        states = [line.split()[1] for line in adb("devices").splitlines()[1:] if len(line.split()) >= 2]
        if "device" in states and adb("shell", "getprop", "sys.boot_completed", timeout=20) == "1":
            return True
        time.sleep(5)
    return False


def run_suite(attempt):
    print(f"[ci_connected_tests] attempt {attempt}: {' '.join(GRADLE)}", flush=True)
    output = []
    process = subprocess.Popen(GRADLE, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                               encoding="utf-8", errors="replace")
    started = time.time()
    try:
        for line in process.stdout:
            sys.stdout.write(line)
            output.append(line)
            if time.time() - started > SUITE_TIMEOUT:
                print(f"[ci_connected_tests] attempt {attempt} exceeded {SUITE_TIMEOUT} s, stopping Gradle", flush=True)
                process.kill()
                break
        status = process.wait(timeout=60)
    except subprocess.TimeoutExpired:
        process.kill()
        status = 1
    REPORTS.mkdir(parents=True, exist_ok=True)
    logcat = adb("logcat", "-d", "-b", "main", "-b", "system", "-b", "crash", timeout=120)
    (REPORTS / f"logcat-attempt-{attempt}.txt").write_text(logcat, encoding="utf-8")
    return status, "".join(output)


def main():
    status, output = run_suite(1)
    if status == 0:
        return 0
    signature = EMULATOR_FAILURE.search(output)
    lost = not device_ready()
    if not signature and not lost:
        print("[ci_connected_tests] test failure without an emulator signature: no retry", flush=True)
        return status
    reason = signature.group(0) if signature else "the device stopped answering"
    print(f"[ci_connected_tests] emulator failure ({reason}); retrying once", flush=True)
    if lost:
        adb("reconnect", timeout=30)
        if not device_ready():
            print("[ci_connected_tests] the emulator did not come back within the time limit", flush=True)
            return status
    adb("shell", "settings", "put", "global", "hide_error_dialogs", "1", timeout=30)
    for source in (REPORTS, RESULTS):
        if source.exists():
            target = source.with_name(source.name + "-attempt-1")
            shutil.rmtree(target, ignore_errors=True)
            shutil.move(str(source), str(target))
    status, _ = run_suite(2)
    return status


if __name__ == "__main__":
    sys.exit(main())
