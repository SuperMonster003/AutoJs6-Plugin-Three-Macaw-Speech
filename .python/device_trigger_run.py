"""Device runs of roadmap P7.1 (the trigger entries, D58 - D65); prints one pass / fail line per check and the key numbers,
never a transcript.

    py .python/device_trigger_run.py avd <serial>      emulators only: grants RECORD_AUDIO and the overlay with `pm grant` /
                                                        `appops`, installs the English streaming model if missing (about
                                                        70 MB), adds the tile (`cmd statusbar add-tile`), runs
                                                        TriggerEntryDeviceTest as a plain app and again as the assistant
                                                        (`cmd role` on API 29+, the secure settings below), then the key
                                                        trigger in all three gestures (accessibility turned on with
                                                        `settings put`, keys through the kernel with `sendevent` as root); restores
                                                        the tile list, the assistant, the accessibility services and the
                                                        music volume
    py .python/device_trigger_run.py phone <serial>    phones: no permission, role, accessibility or tile change; runs
                                                        TriggerEntryDeviceTest in the current state (the tile case only if
                                                        the tile is in Quick Settings, the floating case only if the overlay
                                                        is allowed) with screenshots for the microphone indicator, then
                                                        reinstalls the same APK (an instrumentation leaves the assistant
                                                        service unbound; an update binds it again, a force stop would
                                                        clear the default assistant on Android 12) and clicks the tile in
                                                        the plugin's own process when it is listed. Refuses while the key
                                                        trigger is on: an instrumentation force-stops the plugin, which
                                                        may turn its accessibility service off
    py .python/device_trigger_run.py keys <serial> [gesture]
                                                        the key trigger after the maintainer turned it on (gesture: the one
                                                        chosen in the settings, longVolumeDown by default): the gesture
                                                        opens the sheet (route and time from the log), a short press makes
                                                        one volume step and opens nothing, the other volume key goes to the
                                                        system, nothing opens with the screen off; keys go through the
                                                        kernel (`sendevent`), the sheet closes with BACK, the music volume
                                                        is restored
    py .python/device_trigger_run.py overlay <serial>  the floating microphone and the floating ball of 3-Stove Agent on
                                                        screen at the same time (window list and a screenshot)

Screenshots go to .bak/visual/1.1.0/<serial>/ (not in the repository). Build first:
`.\\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`. Nothing on the screen is tapped by coordinates.
"""
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from device_assistant_run import assistant_state, is_emulator, make_assistant, restore_assistant, sdk  # noqa: E402
from device_host_scripts import media_volume, music_muted, set_music_volume  # noqa: E402
from device_speech_run import PACKAGE, ROOT, adb, install, label, run_test  # noqa: E402
from device_ui_run import pull_screens  # noqa: E402

TILE = f"{PACKAGE}/{PACKAGE}.trigger.VoiceTileService"
KEY_SERVICE = f"{PACKAGE}/{PACKAGE}.trigger.KeyTriggerService"
SHEET = f"{PACKAGE}/.ui.assistant.AssistantActivity"
MODEL = "asr.zipformer-streaming.en.2023-06-26.int8"
GESTURES = ("longVolumeDown", "longVolumeUp", "doubleVolumeDown")
KEY_VOLUME_UP, KEY_VOLUME_DOWN = 115, 114
SCREENS = f"/sdcard/Android/data/{PACKAGE}/files/screens"


def screens_dir(serial):
    return ROOT / ".bak" / "visual" / "1.1.0" / serial.replace(":", "_")


def tile_listed(serial):
    tiles = adb(serial, "shell", "settings", "get", "secure", "sysui_qs_tiles", check=False)
    return f"custom({PACKAGE}/" in tiles or f"custom({TILE})" in tiles, tiles


def key_service_enabled(serial):
    enabled = adb(serial, "shell", "settings", "get", "secure", "enabled_accessibility_services", check=False)
    return any(item.strip() in (KEY_SERVICE, f"{PACKAGE}/.trigger.KeyTriggerService") for item in enabled.split(":")), enabled


def overlay_allowed(serial):
    return "allow" in adb(serial, "shell", "appops", "get", PACKAGE, "SYSTEM_ALERT_WINDOW", check=False)


def entries(serial, prefix, extra=()):
    """TriggerEntryDeviceTest in the current assistant state."""
    listed, _ = tile_listed(serial)
    arguments = ["macawTriggers=true", f"macawPrefix={prefix}", "macawIndicator=true", *(["macawTile=true"] if listed else []), *extra]
    _, values = assistant_state(serial)
    assistant = values.get("voice_interaction_service", "").startswith(PACKAGE)
    print(f"{label(serial)}: entries as {'the assistant' if assistant else 'a plain app'} (tile {'listed' if listed else 'not listed'}, "
          f"overlay {'allowed' if overlay_allowed(serial) else 'not allowed'})")
    return run_test(serial, "TriggerEntryDeviceTest", arguments)


# Keys -----------------------------------------------------------------------------------------------------------------

def key_device(serial):
    """The input device with the volume keys: the built-in keys (gpio-keys and the like), never a headset jack."""
    blocks = re.split(r"(?m)^add device \d+: ", adb(serial, "shell", "getevent", "-pl", check=False))
    candidates = []
    for block in blocks:
        path = block.split("\n", 1)[0].strip()
        name = re.search(r'name:\s+"([^"]*)"', block)
        if path.startswith("/dev/input/") and "KEY_VOLUMEDOWN" in block and "KEY_VOLUMEUP" in block:
            candidates.append((path, name.group(1) if name else ""))
    candidates.sort(key=lambda item: ("jack" in item[1].lower() or "headset" in item[1].lower(), item[0]))
    return candidates[0] if candidates else (None, None)


def press(serial, device, code, hold, times=1, gap=0.12):
    """Kernel key events through sendevent: `times` presses of `hold` seconds, `gap` seconds apart."""
    steps = []
    for index in range(times):
        if index:
            steps.append(f"sleep {gap}")
        steps += [f"sendevent {device} 1 {code} 1", f"sendevent {device} 0 0 0", f"sleep {hold}", f"sendevent {device} 1 {code} 0", f"sendevent {device} 0 0 0"]
    adb(serial, "shell", " && ".join(steps), check=False, timeout=60)


def device_millis(serial):
    """The device clock in epoch milliseconds (toybox date prints %N; older ones fall back to whole seconds)."""
    match = re.search(r"(\d+)(?:\.(\d+))?", adb(serial, "shell", "date", "+%s.%N", check=False))
    if not match:
        return 0.0
    return int(match.group(1)) * 1000 + (int((match.group(2) or "0")[:3].ljust(3, "0")) if match.group(2) else 0)


def trigger_log(serial, since):
    return adb(serial, "logcat", "-d", "-v", "epoch", "-T", since, "-s", "MacawTrigger:I", "MacawAssistant:I", check=False, timeout=60)


def log_since(serial):
    return adb(serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")


def sheet_resumed(serial):
    activities = adb(serial, "shell", "dumpsys", "activity", "activities", check=False)
    return any(SHEET in line or "AssistantActivity" in line for line in activities.splitlines() if "ResumedActivity" in line)


def wait_sheet(serial, seconds=8):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if sheet_resumed(serial):
            return True
        time.sleep(0.3)
    return False


def close_sheet(serial):
    for _ in range(3):
        if not sheet_resumed(serial):
            return True
        adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK", check=False)
        time.sleep(1.0)
    return not sheet_resumed(serial)


STREAMS = (0, 1, 2, 3, 4, 5)  # voice call, system, ring, music, alarm, notification


def volumes(serial):
    """The index of every stream the volume keys may change, read with the command the device answers."""
    state = media_volume(serial)
    if not state:
        return {}
    command = state[0]
    found = {}
    for stream in STREAMS:
        out = adb(serial, "shell", *command, "--stream", str(stream), "--get", check=False)
        match = re.search(r"volume is (\d+) in range \[(\d+)\.\.(\d+)\]", out)
        if match:
            found[stream] = (int(match.group(1)), int(match.group(3)))
    return found


def set_volume(serial, stream, index):
    state = media_volume(serial)
    if state:
        adb(serial, "shell", *state[0], "--stream", str(stream), "--set", str(index), check=False)
    adb(serial, "shell", "cmd", "audio", "set-volume", str(stream), str(index), check=False)


def step(before, after):
    """The sum of the index changes of all streams (a volume key changes one stream by one step)."""
    return sum(after.get(stream, (0, 0))[0] - before.get(stream, (0, 0))[0] for stream in before)


def describe(before, after):
    changed = [f"stream {stream} {before[stream][0]}->{after.get(stream, before[stream])[0]}" for stream in before if after.get(stream, before[stream])[0] != before[stream][0]]
    return ", ".join(changed) or "no stream changed"


def key_checks(serial, gesture):
    """The checks of one gesture with the key trigger on; answers ok. Ring and music start in the middle of their range
    (so that a step never reaches silent or vibrate) and every stream is restored afterwards."""
    device, name = key_device(serial)
    if not device:
        print(f"{label(serial)}: keys FAIL: no input device with the volume keys")
        return False
    key = KEY_VOLUME_UP if gesture == "longVolumeUp" else KEY_VOLUME_DOWN
    other = KEY_VOLUME_DOWN if key == KEY_VOLUME_UP else KEY_VOLUME_UP
    saved = volumes(serial)
    muted = music_muted(serial)
    ok = True
    try:
        for stream in (2, 3):
            if stream in saved:
                set_volume(serial, stream, max(2, saved[stream][1] // 2))
        if muted:
            adb(serial, "shell", "cmd", "audio", "adj-unmute", "3", check=False)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        time.sleep(1.0)
        # 1. The gesture opens the sheet and changes no volume.
        since = log_since(serial)
        before = volumes(serial)
        started = device_millis(serial)
        if gesture == "doubleVolumeDown":
            press(serial, device, key, 0.08, times=2)
        else:
            press(serial, device, key, 1.1)
        opened = wait_sheet(serial)
        log = trigger_log(serial, since)
        sheet = re.search(r"^\s*(\d+\.\d+).*Sheet opened \(source key, (session|direct)\)", log, re.M)
        millis = f"{float(sheet.group(1)) * 1000 - started:.0f} ms" if sheet else "?"
        after = volumes(serial)
        good = opened and bool(sheet) and step(before, after) == 0
        ok &= good
        print(f"{label(serial)}: key {gesture} opens the sheet: {'PASS' if good else 'FAIL'} (input {name}, sheet {millis} after the key down, "
              f"route {sheet.group(2) if sheet else 'none'}, {describe(before, after)})")
        ok &= close_sheet(serial)
        time.sleep(1.0)
        # 2. A short press opens nothing and makes the one volume step of the key. The system shows the volume panel on
        # a first press without a step on some devices, so the other key (which goes to the system) shows it first.
        press(serial, device, other, 0.1)
        time.sleep(0.6)
        since = log_since(serial)
        before = volumes(serial)
        press(serial, device, key, 0.12)
        time.sleep(1.6 if gesture == "doubleVolumeDown" else 1.0)
        after = volumes(serial)
        opened = sheet_resumed(serial) or "Sheet opened" in trigger_log(serial, since)
        expected = 1 if key == KEY_VOLUME_UP else -1
        good = not opened and step(before, after) == expected
        ok &= good
        print(f"{label(serial)}: key {gesture} short press: {'PASS' if good else 'FAIL'} ({describe(before, after)}, sheet {'opened' if opened else 'not opened'})")
        close_sheet(serial)
        # 3. The other volume key goes to the system (the panel is still on screen).
        since = log_since(serial)
        before = volumes(serial)
        press(serial, device, other, 0.12)
        time.sleep(1.0)
        after = volumes(serial)
        opened = sheet_resumed(serial) or "Sheet opened" in trigger_log(serial, since)
        good = not opened and step(before, after) == -expected
        ok &= good
        print(f"{label(serial)}: key {gesture} other volume key goes to the system: {'PASS' if good else 'FAIL'} ({describe(before, after)})")
        close_sheet(serial)
        # 4. Nothing opens with the screen off (the press goes to the system, which may change the volume).
        adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP", check=False)
        time.sleep(1.5)
        since = log_since(serial)
        if gesture == "doubleVolumeDown":
            press(serial, device, key, 0.08, times=2)
        else:
            press(serial, device, key, 1.1)
        time.sleep(1.5)
        log = trigger_log(serial, since)
        good = "Key trigger" not in log and "Sheet opened" not in log
        ok &= good
        print(f"{label(serial)}: key {gesture} screen off: {'PASS' if good else 'FAIL'} (trigger {'logged' if not good else 'none'})")
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        time.sleep(1.0)
        close_sheet(serial)
    finally:
        for stream, (index, _) in saved.items():
            set_volume(serial, stream, index)
        if muted:
            adb(serial, "shell", "cmd", "audio", "adj-mute", "3", check=False)
    return ok


def tile_check(serial):
    """The tile in the plugin's own process (no instrumentation): Quick Settings opens, the tile is clicked, the sheet
    shows with the route of D60; answers ok."""
    since = log_since(serial)
    adb(serial, "shell", "cmd", "statusbar", "expand-settings", check=False)
    time.sleep(1.5)
    adb(serial, "shell", "cmd", "statusbar", "click-tile", TILE, check=False)
    opened = wait_sheet(serial)
    sheet = re.search(r"Sheet opened \(source tile, (session|direct)\)", trigger_log(serial, since))
    closed = close_sheet(serial)
    adb(serial, "shell", "cmd", "statusbar", "collapse", check=False)
    ok = opened and bool(sheet) and closed
    print(f"{label(serial)}: tile opens the sheet: {'PASS' if ok else 'FAIL'} (route {sheet.group(1) if sheet else 'none'})")
    return ok


def reinstall(serial):
    """Replaces the plugin with the same APK. After an instrumentation run the system leaves the assistant service
    unbound (`dumpsys voiceinteraction`: mBound=true mService=null); a package update binds it and the accessibility
    service again. Never `am force-stop`: on Android 12 that clears the user's default assistant ("Force stopping current
    voice interactor"). Answers whether the assistant service is ready when 3-Macaw Speech is the assistant."""
    since = log_since(serial)
    abi = adb(serial, "shell", "getprop", "ro.product.cpu.abi")
    apk = next((ROOT / "app" / "build" / "outputs" / "apk" / "debug").glob(f"*-{abi}.apk"))
    subprocess.run(["adb", "-s", serial, "install", "-r", str(apk)], capture_output=True, text=True)
    _, values = assistant_state(serial)
    if not values.get("voice_interaction_service", "").startswith(PACKAGE):
        return False
    deadline = time.time() + 15
    while time.time() < deadline:
        if "Digital assistant ready" in trigger_log(serial, since):
            print(f"{label(serial)}: assistant service bound again after the reinstall")
            return True
        time.sleep(0.5)
    print(f"{label(serial)}: assistant service NOT bound again after the reinstall")
    return False


def wait_connected(serial, gesture, since, seconds=20):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if f"Key trigger connected ({gesture})" in trigger_log(serial, since):
            return True
        time.sleep(0.5)
    return False


def keys(serial, gesture="longVolumeDown"):
    enabled, _ = key_service_enabled(serial)
    if not enabled:
        print(f"{label(serial)}: keys FAIL: the maintainer turns 3-Macaw Speech key trigger on in the accessibility settings first")
        return False
    return key_checks(serial, gesture)


# Runs -----------------------------------------------------------------------------------------------------------------

def phone(serial):
    enabled, _ = key_service_enabled(serial)
    if enabled and "--force" not in sys.argv:
        print(f"{label(serial)}: phone FAIL: the key trigger is on; an instrumentation would turn it off (run `keys` first, or pass --force)")
        return False
    adb(serial, "shell", "rm", "-rf", SCREENS, check=False)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    ok = entries(serial, "trigger")
    # The plugin's own process again (as after a normal start), for the routes the instrumentation cannot show.
    assistant = reinstall(serial)
    listed, _ = tile_listed(serial)
    if listed:
        ok &= tile_check(serial)
    elif assistant:
        print(f"{label(serial)}: tile not in Quick Settings; the session route is checked with `keys`")
    return pull_screens(serial, screens_dir(serial), ok)


def avd(serial):
    if not is_emulator(serial):
        print(f"{label(serial)}: not an emulator; use `phone` and `keys` (permission, role, tile and accessibility changes are for emulators only)")
        return False
    # Root only for sendevent: the emulator's SELinux policy keeps the shell from writing input devices.
    adb(serial, "root", check=False)
    adb(serial, "wait-for-device", check=False)
    time.sleep(2)
    adb(serial, "shell", "pm", "grant", PACKAGE, "android.permission.RECORD_AUDIO", check=False)
    adb(serial, "shell", "appops", "set", PACKAGE, "SYSTEM_ALERT_WINDOW", "allow", check=False)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "wm", "dismiss-keyguard", check=False)
    if not run_test(serial, "SpeechModelSetupDeviceTest", ["macawModels=true", f"macawModelIds={MODEL}"]):
        return False
    adb(serial, "shell", "rm", "-rf", SCREENS, check=False)
    _, tiles = tile_listed(serial)
    _, services = key_service_enabled(serial)
    accessibility = adb(serial, "shell", "settings", "get", "secure", "accessibility_enabled", check=False)
    state = assistant_state(serial)
    ok = True
    try:
        adb(serial, "shell", "cmd", "statusbar", "add-tile", TILE, check=False)
        listed, _ = tile_listed(serial)
        print(f"{label(serial)}: tile {'added' if listed else 'NOT added'}")
        # As a plain app first: the assistant role goes back to its previous holders other than 3-Macaw Speech.
        restore_assistant(serial, state)
        ok &= entries(serial, "trigger-direct", ["macawKeyguard=true"])
        ok &= tile_check(serial)
        ok &= make_assistant(serial) and entries(serial, "trigger-session", ["macawRebindAssistant=true"])
        ok &= reinstall(serial) and tile_check(serial)
        # The key trigger as the assistant (default gesture), then all gestures as a plain app.
        for gesture in GESTURES:
            since = log_since(serial)
            run_test(serial, "TriggerSetupDeviceTest", [f"macawKeyGesture={gesture}"])
            # The instrumentation force-stopped the plugin: the update binds its services again (and the assistant's).
            reinstall(serial)
            # On systems where the force stop turned the accessibility service off, it goes on again.
            adb(serial, "shell", "settings", "put", "secure", "enabled_accessibility_services", KEY_SERVICE, check=False)
            adb(serial, "shell", "settings", "put", "secure", "accessibility_enabled", "1", check=False)
            connected = wait_connected(serial, gesture, since)
            print(f"{label(serial)}: key trigger {gesture} {'connected' if connected else 'NOT connected'}")
            ok &= connected and key_checks(serial, gesture)
            if gesture == GESTURES[0]:
                restore_assistant(serial, state)
    finally:
        if tiles and tiles != "null":
            adb(serial, "shell", "settings", "put", "secure", "sysui_qs_tiles", tiles, check=False)
        if services and services != "null":
            adb(serial, "shell", "settings", "put", "secure", "enabled_accessibility_services", services, check=False)
        else:
            adb(serial, "shell", "settings", "delete", "secure", "enabled_accessibility_services", check=False)
        if accessibility and accessibility != "null":
            adb(serial, "shell", "settings", "put", "secure", "accessibility_enabled", accessibility, check=False)
        restore_assistant(serial, state)
        adb(serial, "shell", "appops", "set", PACKAGE, "SYSTEM_ALERT_WINDOW", "default", check=False)
        adb(serial, "unroot", check=False)
        adb(serial, "wait-for-device", check=False)
    return pull_screens(serial, screens_dir(serial), ok)


def overlay(serial):
    windows = adb(serial, "shell", "dumpsys", "window", "windows", check=False)
    macaw = "3-Macaw Speech floating microphone" in windows
    stove = "3-Stove Agent floating" in windows
    target = screens_dir(serial)
    target.mkdir(parents=True, exist_ok=True)
    with open(target / "overlay-both.png", "wb") as handle:
        handle.write(subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], capture_output=True).stdout)
    ok = macaw and stove
    print(f"{label(serial)}: overlay {'PASS' if ok else 'FAIL'} (floating microphone {'shown' if macaw else 'missing'}, "
          f"3-Stove Agent ball {'shown' if stove else 'missing'}, screenshot {target / 'overlay-both.png'})")
    return ok


def main(argv):
    if len(argv) < 2 or argv[0] not in ("avd", "phone", "keys", "overlay"):
        print(__doc__)
        return 2
    action, serial = argv[0], argv[1]
    if action == "keys":
        gesture = argv[2] if len(argv) > 2 and argv[2] in GESTURES else "longVolumeDown"
        return 0 if keys(serial, gesture) else 1
    if action == "overlay":
        return 0 if overlay(serial) else 1
    if not install(serial):
        return 1
    return 0 if {"avd": avd, "phone": phone}[action](serial) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
