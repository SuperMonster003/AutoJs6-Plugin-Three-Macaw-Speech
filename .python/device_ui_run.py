"""Device runs of roadmap P5 for the screens of 3-Macaw Speech; prints one pass / fail line per run and the key numbers.

    py .python/device_ui_run.py home <serial>            HomeScreenDeviceTest (P5.1: list online / offline / empty,
                                                        license confirm / cancel, default after deleting the default)
    py .python/device_ui_run.py contract <serial>        ThreeMacawSpeechContractTest (launcher alias, exported audit)
    py .python/device_ui_run.py download <serial> <id>   HomeDownloadDeviceTest (P5.1): downloads one commercial-use model
                                                        from its home screen row on an unmetered network
    py .python/device_ui_run.py tryout <serial> [out]    TryoutDeviceTest (P5.3): dictation, wake word trial and synthesis
                                                        playback on the real microphone, the test recordings played
                                                        through the speaker at MACAW_VOLUME_PERCENT (70 by default) of
                                                        the music volume, restored afterwards; screenshots as below
    py .python/device_ui_run.py voice <serial> [out]    VoiceInputDeviceTest (P5.6): ACTION_RECOGNIZE_SPEECH sent with
                                                        startActivityForResult, the English recording played through the
                                                        speaker (volume as for tryout); EXTRA_RESULTS and its CER, Stop,
                                                        Cancel, silence and the busy guidance; screenshots as below
    py .python/device_ui_run.py screens <serial> [out]   ScreenCaptureDeviceTest in four appearances (P5.2 / P5.3):
                                                        English light, Simplified Chinese dark, Arabic light (RTL) and
                                                        English dark at font scale 1.3; screenshots are pulled to
                                                        out (default .bak/visual/1.0.0/<serial>/, not in the repository)

The debug APK and the test APK are installed with `adb install -r` first (data and models stay; build them with
`.\\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`). The font scale and the plugin's appearance
preferences are restored afterwards. Nothing on the device is tapped: permission dialogs stay with the maintainer.
"""
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from device_speech_run import PACKAGE, ROOT, adb, install, label, run_test  # noqa: E402
import device_host_scripts as hs  # noqa: E402

VARIANTS = [
    # (prefix, language, night, font scale)
    ("en-light", "en", "false", "1.0"),
    ("zh-dark", "zh-Hans", "true", "1.0"),
    ("ar-light", "ar", "false", "1.0"),
    ("en-dark-large", "en", "true", "1.3"),
]


def screens(serial, out):
    remote = f"/sdcard/Android/data/{PACKAGE}/files/screens"
    adb(serial, "shell", "rm", "-rf", remote, check=False)
    original = adb(serial, "shell", "settings", "get", "system", "font_scale", check=False).strip()
    ok = True
    try:
        for prefix, language, night, scale in VARIANTS:
            adb(serial, "shell", "settings", "put", "system", "font_scale", scale)
            passed = run_test(serial, "ScreenCaptureDeviceTest", [
                "macawScreens=true", f"macawLanguage={language}", f"macawNight={night}", f"macawPrefix={prefix}",
                # The manual update check asks GitHub once, in the first appearance only.
                f"macawNetwork={'true' if prefix == VARIANTS[0][0] else 'false'}",
            ])
            ok = ok and passed
    finally:
        restored = original if original and original != "null" else "1.0"
        adb(serial, "shell", "settings", "put", "system", "font_scale", restored, check=False)
    return pull_screens(serial, out, ok)


def pull_screens(serial, out, ok):
    remote = f"/sdcard/Android/data/{PACKAGE}/files/screens"
    target = Path(out) if out else ROOT / ".bak" / "visual" / "1.0.0" / serial.replace(":", "_")
    staging = Path(tempfile.mkdtemp(prefix="macaw-screens-"))
    try:
        subprocess.run(["adb", "-s", serial, "pull", remote, str(staging)], capture_output=True, text=True)
        pulled = sorted((staging / "screens").glob("*.png"))
        target.mkdir(parents=True, exist_ok=True)
        for path in pulled:
            shutil.copy2(path, target / path.name)
    finally:
        shutil.rmtree(staging, ignore_errors=True)
    print(f"{label(serial)}: screens {'PASS' if ok else 'FAIL'}: {len(pulled)} screenshots -> {target}")
    return ok and len(pulled) > 0


def tryout(serial, out, test="TryoutDeviceTest", arguments=("macawTryout=true", "macawPrefix=tryout")):
    adb(serial, "shell", "rm", "-rf", f"/sdcard/Android/data/{PACKAGE}/files/screens", check=False)
    # device_host_scripts.Volume also covers HyperOS / MIUI, which ignore `cmd media_session volume --set` from the shell
    # and keep a muted music stream muted (roadmap P6.3 smoke run).
    os.environ.setdefault("MACAW_VOLUME_PERCENT", "70")
    with hs.Volume(serial) as volume:
        print(f"{label(serial)}: music {volume.describe()}")
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        ok = run_test(serial, test, list(arguments))
    return pull_screens(serial, out, ok)


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    action, serial = argv[0], argv[1]
    if not install(serial):
        return 1
    if action == "home":
        return 0 if run_test(serial, "HomeScreenDeviceTest") else 1
    if action == "contract":
        return 0 if run_test(serial, "ThreeMacawSpeechContractTest") else 1
    if action == "download" and len(argv) > 2:
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        return 0 if run_test(serial, "HomeDownloadDeviceTest", [f"macawDownload={argv[2]}"]) else 1
    if action == "tryout":
        return 0 if tryout(serial, argv[2] if len(argv) > 2 else None) else 1
    if action == "voice":
        return 0 if tryout(serial, argv[2] if len(argv) > 2 else None, "VoiceInputDeviceTest", ("macawVoice=true", "macawPrefix=voice")) else 1
    if action == "screens":
        return 0 if screens(serial, argv[2] if len(argv) > 2 else None) else 1
    print(__doc__)
    return 2


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
