"""Device runs of roadmap P3.5 for 3-Macaw Speech: installs the debug build, runs the recognition instrumentation and
summarizes the results without transcripts.

    py .python/device_speech_run.py install <serial>
    py .python/device_speech_run.py test <serial> <TestClass>[#method] [key=value ...]
    py .python/device_speech_run.py models <serial>          installs the three recognition models (about 430 MB)
    py .python/device_speech_run.py device <serial>          models, then every recognition device test
    py .python/device_speech_run.py tts-models <serial> [ids] installs the five synthesis models (about 740 MB, roadmap P4.1)
                                                            or the comma separated model ids given
    py .python/device_speech_run.py tts <serial>             every synthesis device test (SpeechSynthesisDeviceTest)

`install` installs the debug split of the device's primary ABI and the test APK with `adb install -r` (data and models
are kept; nothing is uninstalled afterwards, unlike connectedDebugAndroidTest). `test` runs one class through
`am instrument` and prints the JUnit outcome plus the `MacawDeviceRun` lines of the run with their `text=` part cut
off. Build first: `.\\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`. Permission dialogs are never
answered here: RECORD_AUDIO is granted by the maintainer in MicrophonePermissionActivity (`grant <serial>` opens it).
"""
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
TEST_PACKAGE = PACKAGE + ".test"
RUNNER = TEST_PACKAGE + "/androidx.test.runner.AndroidJUnitRunner"
APK_DIR = ROOT / "app" / "build" / "outputs" / "apk"
TESTS = ["SpeechRecognitionDeviceTest", "KeywordSpottingDeviceTest", "MicrophoneSessionDeviceTest", "ThreeMacawSpeechContractTest", "SherpaRuntimeDeviceTest"]


def adb(serial, *args, check=True, timeout=None):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
    if check and result.returncode != 0:
        raise SystemExit(f"adb {' '.join(args)} failed: {(result.stderr or result.stdout).strip()[:300]}")
    return result.stdout.strip()


def label(serial):
    sdk = adb(serial, "shell", "getprop", "ro.build.version.sdk")
    model = adb(serial, "shell", "getprop", "ro.product.model")
    return f"{model} ({serial}, API {sdk})"


def install(serial):
    abi = adb(serial, "shell", "getprop", "ro.product.cpu.abi")
    apk = next((APK_DIR / "debug").glob(f"*-{abi}.apk"), None) or next((APK_DIR / "debug").glob("*-universal.apk"))
    test_apk = APK_DIR / "androidTest" / "debug" / "app-debug-androidTest.apk"
    for path, flags in ((apk, ["-r"]), (test_apk, ["-r", "-t"])):
        out = subprocess.run(["adb", "-s", serial, "install", *flags, str(path)], capture_output=True, text=True, encoding="utf-8", errors="replace")
        status = "OK" if "Success" in out.stdout else "FAIL " + (out.stdout + out.stderr).strip().splitlines()[-1][:200]
        print(f"{label(serial)}: install {path.name}: {status}")
        if not status.startswith("OK"):
            return False
    return True


def device_time(serial):
    return adb(serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")


def run_test(serial, target, arguments=()):
    since = device_time(serial)
    name = target if "." in target.split("#")[0] else f"{PACKAGE}.{target}"
    command = ["shell", "am", "instrument", "-w", "-r", "-e", "class", name]
    for item in arguments:
        key, _, value = item.partition("=")
        command += ["-e", key, value]
    started = time.time()
    output = adb(serial, *command, RUNNER, check=False, timeout=7200)
    seconds = time.time() - started
    passed = len(re.findall(r"INSTRUMENTATION_STATUS_CODE: 0", output))
    failed = len(re.findall(r"INSTRUMENTATION_STATUS_CODE: -2", output))
    skipped = len(re.findall(r"INSTRUMENTATION_STATUS_CODE: -3", output)) + len(re.findall(r"INSTRUMENTATION_STATUS_CODE: -4", output))
    ok = "OK (" in output and failed == 0
    print(f"{label(serial)}: {target}: {'PASS' if ok else 'FAIL'} (passed {passed}, failed {failed}, skipped {skipped}, {seconds:.0f} s)")
    if not ok:
        for match in re.finditer(r"INSTRUMENTATION_STATUS: stack=(.+?)(?:\n\S|\Z)", output, re.S):
            print("    " + match.group(1).strip().splitlines()[0][:400])
        if "INSTRUMENTATION_RESULT" in output and "OK (" not in output:
            tail = [line for line in output.splitlines() if line.startswith("INSTRUMENTATION_RESULT") or "shortMsg" in line or "longMsg" in line]
            for line in tail[:3]:
                print("    " + line[:300])
    log = adb(serial, "logcat", "-d", "-v", "raw", "-T", since, "-s", "MacawDeviceRun:I", check=False)
    for line in log.splitlines():
        line = line.strip()
        if not line or line.startswith("-----"):
            continue
        print("    " + re.sub(r" text=.*$", "", re.sub(r" log=.*$", "", line))[:300])
    return ok


def grant(serial):
    """Opens MicrophonePermissionActivity the way a script does it: the activity requires the plugin permission, which
    adb lacks and AutoJs6 holds, so a one-line AutoJs6 script (RunIntentActivity, shared storage) starts it."""
    remote = "/sdcard/Download/autojs6-macaw-grant"
    script = f'app.startActivity({{ packageName: "{PACKAGE}", className: "{PACKAGE}.service.MicrophonePermissionActivity" }});\n'
    local = Path(tempfile.gettempdir()) / "macaw-grant.js"
    local.write_text(script, encoding="utf-8")
    adb(serial, "shell", "mkdir", "-p", remote)
    adb(serial, "push", str(local), f"{remote}/grant.js")
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{remote}/grant.js", "-n", "org.autojs.autojs6/org.autojs.autojs.external.open.RunIntentActivity")
    time.sleep(4)
    adb(serial, "shell", "rm", "-rf", remote, check=False)
    top = adb(serial, "shell", "dumpsys", "activity", "activities", check=False)
    resumed = [line for line in top.splitlines() if "ResumedActivity" in line]
    shown = any("GrantPermissionsActivity" in line for line in resumed)
    print(f"{label(serial)}: permission dialog {'shown' if shown else 'not on top (unlock the device and retry)'}; the maintainer answers it on the device")


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    action, serial, rest = argv[0], argv[1], argv[2:]
    if action == "install":
        return 0 if install(serial) else 1
    if action == "test":
        return 0 if run_test(serial, rest[0], rest[1:]) else 1
    if action == "models":
        return 0 if run_test(serial, "SpeechModelSetupDeviceTest", ["macawModels=true"]) else 1
    if action == "tts-models":
        wanted = rest[0] if rest else "true"
        return 0 if run_test(serial, "SpeechSynthesisDeviceTest#test0InstallsTheSynthesisModels", [f"macawTtsModels={wanted}"]) else 1
    if action == "tts":
        return 0 if run_test(serial, "SpeechSynthesisDeviceTest", rest) else 1
    if action == "grant":
        grant(serial)
        return 0
    if action == "device":
        ok = run_test(serial, "SpeechModelSetupDeviceTest", ["macawModels=true"])
        for test in TESTS:
            ok = run_test(serial, test) and ok
        return 0 if ok else 1
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
