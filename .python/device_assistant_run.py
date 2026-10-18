"""Device runs of roadmap P5.7 (the digital assistant, the recognition service and the takeover of D57); prints one pass /
fail line per run and the key numbers, never a transcript.

    py .python/device_assistant_run.py avd <serial>      emulators only: grants RECORD_AUDIO with `pm grant`, installs the
                                                        wake word and English streaming models if missing (about 79 MB),
                                                        makes 3-Macaw Speech the assistant (`cmd role add-role-holder`
                                                        on API 29+, the assistant settings below), runs the three tests
                                                        with the virtual room (an emulator has no audio input) and the
                                                        lock screen case, then restores the previous assistant
    py .python/device_assistant_run.py phone <serial>    phones: no permission, role or lock change; the recordings play
                                                        through the speaker at MACAW_VOLUME_PERCENT (70 by default) of
                                                        the music volume, restored afterwards. The assist key case runs
                                                        only if the maintainer chose 3-Macaw Speech as the assistant

Tests: AssistantDeviceTest (the assist key, the two VOICE_COMMAND test targets of the test APK, editing, "Speak again",
silence, "Copy text", the lock screen), RecognitionServiceDeviceTest (SpeechRecognizer with the plugin's component:
callback order, partial results, stopListening, cancel, silence, the API 33 support check) and TakeoverDeviceTest (a
wake session silenced by the assistant, the recognition service and the voice input, resumed afterwards; a script
dictation never taken over). Screenshots go to .bak/visual/1.0.0/<serial>/ (not in the repository). Build first:
`.\\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`. Nothing on the device is tapped.
"""
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from device_speech_run import PACKAGE, adb, install, label, run_test  # noqa: E402
from device_ui_run import pull_screens  # noqa: E402

ASSISTANT = f"{PACKAGE}/.assistant.MacawVoiceInteractionService"
RECOGNIZER = f"{PACKAGE}/.recognition.MacawRecognitionService"
ROLE = "android.app.role.ASSISTANT"
MODELS = "kws.zipformer.zh-en.2025-12-20,asr.zipformer-streaming.en.2023-06-26.int8"
SETTINGS = ("assistant", "voice_interaction_service", "voice_recognition_service")


def sdk(serial):
    return int(adb(serial, "shell", "getprop", "ro.build.version.sdk").strip())


def is_emulator(serial):
    return serial.startswith("emulator-") or adb(serial, "shell", "getprop", "ro.kernel.qemu", check=False).strip() == "1"


def assistant_state(serial):
    """The current assistant: role holders (API 29+) and the three secure settings."""
    holders = adb(serial, "shell", "cmd", "role", "get-role-holders", ROLE, check=False).strip() if sdk(serial) >= 29 else ""
    values = {key: adb(serial, "shell", "settings", "get", "secure", key, check=False).strip() for key in SETTINGS}
    return holders, values


def make_assistant(serial):
    if sdk(serial) >= 29:
        adb(serial, "shell", "cmd", "role", "add-role-holder", ROLE, PACKAGE, check=False)
    else:
        for key, value in zip(SETTINGS, (ASSISTANT, ASSISTANT, RECOGNIZER)):
            adb(serial, "shell", "settings", "put", "secure", key, value, check=False)
    _, values = assistant_state(serial)
    active = values.get("voice_interaction_service", "").startswith(PACKAGE)
    print(f"{label(serial)}: assistant {'set' if active else 'NOT set'} (voice_interaction_service={values.get('voice_interaction_service')})")
    return active


def restore_assistant(serial, state):
    holders, values = state
    if sdk(serial) >= 29:
        adb(serial, "shell", "cmd", "role", "remove-role-holder", ROLE, PACKAGE, check=False)
        for holder in [line.strip() for line in holders.splitlines() if line.strip() and line.strip() != PACKAGE]:
            adb(serial, "shell", "cmd", "role", "add-role-holder", ROLE, holder, check=False)
    else:
        for key, value in values.items():
            if value and value != "null":
                adb(serial, "shell", "settings", "put", "secure", key, value, check=False)
            else:
                adb(serial, "shell", "settings", "delete", "secure", key, check=False)
    _, now = assistant_state(serial)
    print(f"{label(serial)}: assistant restored (voice_interaction_service={now.get('voice_interaction_service')})")


def run_all(serial, room):
    common = [f"macawRoom={room}"] if room else []
    ok = run_test(serial, "AssistantDeviceTest", ["macawAssistant=true", "macawPrefix=assistant", *common, *(["macawKeyguard=true"] if room else [])])
    ok = run_test(serial, "RecognitionServiceDeviceTest", ["macawRecognition=true", *common]) and ok
    ok = run_test(serial, "TakeoverDeviceTest", ["macawTakeover=true", "macawPrefix=takeover", *common]) and ok
    return ok


def avd(serial):
    if not is_emulator(serial):
        print(f"{label(serial)}: not an emulator; use `phone` (role, permission and lock changes are for emulators only)")
        return False
    adb(serial, "shell", "pm", "grant", PACKAGE, "android.permission.RECORD_AUDIO", check=False)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "wm", "dismiss-keyguard", check=False)
    if not run_test(serial, "SpeechModelSetupDeviceTest", ["macawModels=true", f"macawModelIds={MODELS}"]):
        return False
    adb(serial, "shell", "rm", "-rf", f"/sdcard/Android/data/{PACKAGE}/files/screens", check=False)
    state = assistant_state(serial)
    try:
        ok = make_assistant(serial) and run_all(serial, "virtual")
    finally:
        restore_assistant(serial, state)
        adb(serial, "shell", "wm", "dismiss-keyguard", check=False)
    return pull_screens(serial, None, ok)


def phone(serial):
    adb(serial, "shell", "rm", "-rf", f"/sdcard/Android/data/{PACKAGE}/files/screens", check=False)
    volume = adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "3", "--get", check=False)
    match = re.search(r"volume is (\d+) in range \[(\d+)\.\.(\d+)\]", volume)
    original = None
    if match:
        original, maximum = int(match.group(1)), int(match.group(3))
        wanted = max(1, maximum * int(os.environ.get("MACAW_VOLUME_PERCENT", "70")) // 100)
        adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "3", "--set", str(max(original, wanted)), check=False)
        print(f"{label(serial)}: music volume {original}/{maximum} -> {max(original, wanted)}")
    _, values = assistant_state(serial)
    print(f"{label(serial)}: 3-Macaw Speech is {'' if values.get('voice_interaction_service', '').startswith(PACKAGE) else 'not '}the assistant")
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    try:
        ok = run_all(serial, None)
    finally:
        if original is not None:
            adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "3", "--set", str(original), check=False)
    return pull_screens(serial, None, ok)


def main(argv):
    if len(argv) < 2 or argv[0] not in ("avd", "phone"):
        print(__doc__)
        return 2
    action, serial = argv
    if not install(serial):
        return 1
    return 0 if (avd(serial) if action == "avd" else phone(serial)) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
