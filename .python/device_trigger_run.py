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
                                                        the plugin's own process when it is listed, then the headset entry
                                                        as below. Refuses while the key trigger is on: an instrumentation
                                                        force-stops the plugin, which may turn its accessibility service off
    py .python/device_trigger_run.py headset <serial>  the headset entry (D65) without an instrumentation: lists the apps
                                                        that answer VOICE_COMMAND (more than one: the headset key shows a
                                                        chooser), starts it for the plugin as the Bluetooth service would
                                                        and checks the sheet and its route; the real headset key is
                                                        pressed by the maintainer under `watch`
    py .python/device_trigger_run.py keys <serial> [gesture]
                                                        the key trigger after the maintainer turned it on (gesture: the one
                                                        chosen in the settings, longVolumeDown by default): the gesture
                                                        opens the sheet (route and time from the log), a short press makes
                                                        one volume step and opens nothing, the other volume key goes to the
                                                        system, nothing opens with the screen off; keys go through the
                                                        kernel (`sendevent`, which needs root: emulators), the sheet closes
                                                        with BACK, every volume is restored
    py .python/device_trigger_run.py watch <serial> [seconds]
                                                        phones (their SELinux policy keeps the shell from writing input
                                                        devices, so `keys` needs a root emulator): the maintainer presses
                                                        the real keys while this prints the plugin's log (trigger, route,
                                                        volume step of a consumed press), volume and screen changes with
                                                        times; volumes are restored at the end unless `--keep-volumes` is
                                                        passed (a headset that connects changes them on purpose)
    py .python/device_trigger_run.py overlay <serial>  the floating microphone and the floating ball of 3-Stove Agent on
                                                        screen at the same time (window list and a screenshot), and the
                                                        notification shade with the floating microphone's notification

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
VOICE_COMMAND = "android.intent.action.VOICE_COMMAND"
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

def key_devices(serial):
    """The built-in input device of each volume key ({code: (path, name)}): phones often put volume down and volume up on
    different devices (gpio-keys, qpnp_pon, pmic_resin); a headset jack, a keyboard cover or a consumer control device is
    never used."""
    blocks = re.split(r"(?m)^add device \d+: ", adb(serial, "shell", "getevent", "-pl", check=False))
    found = {}
    for code, label_name in ((KEY_VOLUME_DOWN, "KEY_VOLUMEDOWN"), (KEY_VOLUME_UP, "KEY_VOLUMEUP")):
        candidates = []
        for block in blocks:
            path = block.split("\n", 1)[0].strip()
            name = re.search(r'name:\s+"([^"]*)"', block)
            name = name.group(1) if name else ""
            if path.startswith("/dev/input/") and re.search(rf"\b{label_name}\b", block):
                external = any(word in name.lower() for word in ("jack", "headset", "keyboard", "consumer"))
                candidates.append((external, path, name))
        candidates.sort()
        if candidates and not candidates[0][0]:
            found[code] = (candidates[0][1], candidates[0][2])
    return found


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
    devices = key_devices(serial)
    if len(devices) < 2:
        print(f"{label(serial)}: keys FAIL: no built-in input device for both volume keys ({devices})")
        return False
    key = KEY_VOLUME_UP if gesture == "longVolumeUp" else KEY_VOLUME_DOWN
    other = KEY_VOLUME_DOWN if key == KEY_VOLUME_UP else KEY_VOLUME_UP
    device, name = devices[key]
    other_device = devices[other][0]
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
        press(serial, other_device, other, 0.1)
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
        press(serial, other_device, other, 0.12)
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


def headset_answerers(serial):
    """The activities that answer VOICE_COMMAND; the Bluetooth service starts it without a package."""
    output = adb(serial, "shell", "cmd", "package", "query-activities", "--brief", "-a", VOICE_COMMAND, check=False)
    return [line.strip() for line in output.splitlines() if "/" in line and " " not in line.strip()]


def headset(serial):
    """The headset entry (D65) in the plugin's own process: who answers VOICE_COMMAND, then the action for the plugin
    (with its package, as the chooser would pass it on) opens the sheet with the route of D60; answers ok."""
    answerers = headset_answerers(serial)
    listed = any(answerer.startswith(PACKAGE + "/") for answerer in answerers)
    print(f"{label(serial)}: VOICE_COMMAND answered by {len(answerers)}: {', '.join(answerers) or 'none'}")
    since = log_since(serial)
    adb(serial, "shell", "am", "start", "-a", VOICE_COMMAND, "-p", PACKAGE, check=False)
    opened = wait_sheet(serial)
    sheet = re.search(r"Sheet opened \(source headset, (session|direct)\)", trigger_log(serial, since))
    closed = close_sheet(serial)
    ok = listed and opened and bool(sheet) and closed
    print(f"{label(serial)}: headset entry opens the sheet: {'PASS' if ok else 'FAIL'} (route {sheet.group(1) if sheet else 'none'})")
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


def top_activity(serial):
    """The resumed activity (component only), to see what a headset key or a gesture opened."""
    for line in adb(serial, "shell", "dumpsys", "activity", "activities", check=False).splitlines():
        if "ResumedActivity" in line:
            match = re.search(r"\{\S+ \S+ (\S+)", line)
            return match.group(1) if match else line.strip()
    return None


def screen_on(serial):
    return "mWakefulness=Awake" in adb(serial, "shell", "dumpsys", "power", check=False)


def watch(serial, seconds=180):
    """Phones, whose SELinux policy keeps the shell from writing input devices: the maintainer presses the real keys
    while this prints, with times, what the plugin logged (trigger, route, volume step of a consumed press), every volume
    change and every screen change; the volumes are restored at the end. Prints the gesture the service reported."""
    enabled, _ = key_service_enabled(serial)
    bound = "label=3-Macaw Speech" in adb(serial, "shell", "dumpsys", "accessibility", check=False)
    _, values = assistant_state(serial)
    assistant = values.get("voice_interaction_service", "").startswith(PACKAGE)
    print(f"{label(serial)}: watch {seconds} s (key trigger {'on' if enabled else 'OFF'}, {'bound' if bound else 'NOT bound'}; "
          f"{'the assistant' if assistant else 'not the assistant'})", flush=True)
    saved = volumes(serial)
    since = log_since(serial)
    seen = set()
    previous, awake, last_top = saved, screen_on(serial), None
    started = time.time()
    try:
        while time.time() - started < seconds:
            elapsed = time.time() - started
            for line in trigger_log(serial, since).splitlines():
                text = line.split(": ", 1)[-1].strip() if ": " in line else ""
                stamp = line.split()[0] if line.split() else ""
                if text and (stamp, text) not in seen and any(word in text for word in ("Key trigger", "Sheet opened", "Trigger ", "Floating microphone", "Assistant session")):
                    seen.add((stamp, text))
                    print(f"  +{elapsed:5.1f} s log: {text}", flush=True)
            current = volumes(serial)
            if current != previous:
                print(f"  +{elapsed:5.1f} s volume: {describe(previous, current)}", flush=True)
                previous = current
            now_awake = screen_on(serial)
            if now_awake != awake:
                print(f"  +{elapsed:5.1f} s screen {'on' if now_awake else 'off'}", flush=True)
                awake = now_awake
            top = top_activity(serial)
            if top != last_top:
                print(f"  +{elapsed:5.1f} s top: {top}", flush=True)
                last_top = top
            if sheet_resumed(serial):
                print(f"  +{elapsed:5.1f} s the sheet is on screen", flush=True)
                while sheet_resumed(serial) and time.time() - started < seconds:
                    time.sleep(0.5)
                print(f"  +{time.time() - started:5.1f} s the sheet closed", flush=True)
            time.sleep(0.3)
    finally:
        keep = "--keep-volumes" in sys.argv
        if not keep:
            for stream, (index, _) in saved.items():
                set_volume(serial, stream, index)
        print(f"{label(serial)}: watch ended, volumes {'kept' if keep else 'restored'}", flush=True)
    return True


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
    ok &= headset(serial)
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
        # After the instrumentation's force stop the system UI binds no tile of the stopped package; an update starts it.
        reinstall(serial)
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
    # The floating microphone's notification (its small icon is not a microphone, so it does not read as a recording).
    notified = "floating_microphone" in adb(serial, "shell", "dumpsys", "notification", check=False)
    adb(serial, "shell", "cmd", "statusbar", "expand-notifications", check=False)
    time.sleep(1.5)
    with open(target / "overlay-shade.png", "wb") as handle:
        handle.write(subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], capture_output=True).stdout)
    adb(serial, "shell", "cmd", "statusbar", "collapse", check=False)
    print(f"{label(serial)}: floating microphone notification {'posted' if notified else 'MISSING'} (screenshot {target / 'overlay-shade.png'})")
    return ok and notified


def main(argv):
    if len(argv) < 2 or argv[0] not in ("avd", "phone", "keys", "watch", "overlay", "headset"):
        print(__doc__)
        return 2
    action, serial = argv[0], argv[1]
    if action == "keys":
        gesture = argv[2] if len(argv) > 2 and argv[2] in GESTURES else "longVolumeDown"
        return 0 if keys(serial, gesture) else 1
    if action == "watch":
        return 0 if watch(serial, int(argv[2]) if len(argv) > 2 and argv[2].isdigit() else 180) else 1
    if action == "overlay":
        return 0 if overlay(serial) else 1
    if action == "headset":
        return 0 if headset(serial) else 1
    if not install(serial):
        return 1
    return 0 if {"avd": avd, "phone": phone}[action](serial) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
