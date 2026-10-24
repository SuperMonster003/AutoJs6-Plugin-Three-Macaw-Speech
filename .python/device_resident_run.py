"""Device runs of roadmap P7.4 (the resident wake word); prints one pass / fail line per run and the key numbers, never a
transcript or the recognized text.

    py .python/device_resident_run.py avd <serial>       emulators only: grants RECORD_AUDIO and POST_NOTIFICATIONS with
                                                        `pm grant`, installs the wake word and English streaming models
                                                        if missing, makes 3-Macaw Speech the assistant (restored after)
                                                        and runs ResidentWakeDeviceTest with the virtual room and the
                                                        screen-off case; then, not the assistant and with the overlay
                                                        permission denied through appops (restored after), the switch
                                                        case again, which must take the "Tap to speak" route (D71)
    py .python/device_resident_run.py phone <serial>     phones: no permission, role or lock change; the keyword plays
                                                        through the speaker at MACAW_VOLUME_PERCENT (40 by default) of
                                                        the music volume, restored afterwards; `dumpsys battery` is reset
    py .python/device_resident_run.py stove <serial>     phones with 3-Stove Agent whose resident listener the maintainer
                                                        turned on in its settings (shell may not switch another app's
                                                        component; when it is off the script opens those settings and
                                                        stops): checks the conflict dialog and "Open 3-Stove Agent
                                                        settings"; the maintainer turns the listener off afterwards
    py .python/device_resident_run.py background <serial>
                                                        D76 / D77 without instrumentation: on from a visible start with
                                                        "only while charging", home, `dumpsys battery unplug`, then
                                                        `set ac 1` from the background; prints whether the reopened
                                                        session records live or waits for a tap; puts the two options it
                                                        changes back to their defaults
    py .python/device_resident_run.py battery <serial> [minutes] [on]
                                                        the screen-off measurement of D73 / P7.4.8: the same number of
                                                        minutes (30 by default) with the switch off, then on ("only
                                                        while charging" off, no time limit), on battery as batterystats
                                                        sees it (`dumpsys battery unplug`); prints the batterystats
                                                        estimate (mAh), the plugin's CPU time, its busiest threads, the
                                                        voice gate's share and whether it still records at the end of
                                                        both periods (`on`: the second period only)
    py .python/device_resident_run.py gate <serial> [plays]
                                                        roadmap P8.1.1: records the room and the keyword through the
                                                        speaker (3 plays at the full and at a quarter of the level by
                                                        default, 30 s of the room alone) and compares the plain spotter
                                                        with voice gate settings on the same recordings

Power saving (roadmap P8.1): `--power off|gate|step|both` picks a measured variant of P8.1.1 for `phone` and `battery`
(the debug switch or the instrumentation argument sets it in the plugin's process); without it the stored switch
decides, which `--power-saving on|off` sets (on by default, D123). `phone --hit-rate <plays>` adds the hit count at the
full and at a quarter of the level, and the late hits (ResidentWakeDeviceTest#theWakeWordIsCountedAtTwoLevels).

`background` and `battery` switch through the debug build's ResidentDebugActivity (adb only, DUMP permission), because an
instrumented process keeps a foreground state of its own; nothing is tapped.

Build first: `.\\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`. Screenshots go to
.bak/visual/1.2.0/<serial>/ (not in the repository). Never run two acoustic runs on neighbouring phones at once.
"""
import contextlib
import io
import re
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hs  # noqa: E402
from device_assistant_run import MODELS, assistant_state, is_emulator, make_assistant, restore_assistant  # noqa: E402
import device_speech_run  # noqa: E402
from device_speech_run import PACKAGE, adb, install, label  # noqa: E402
from device_ui_run import pull_screens  # noqa: E402

TEST = "ResidentWakeDeviceTest"
STOVE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
STOVE_RESIDENT = f"{STOVE}/.speech.ResidentListenerService"
STOVE_SETTINGS = f"{STOVE}/.speech.ResidentListenerSettings"
SERVICE = f"{PACKAGE}/.resident.ResidentWakeService"
SCREENS = Path(__file__).resolve().parents[1] / ".bak" / "visual" / "1.2.0"
# The defaults of the options that `background` and `battery` change (D77, D69, D123), and no measured variant.
DEFAULT_OPTIONS = ("--ez", "chargingOnly", "false", "--ez", "readAloud", "true", "--es", "duration", "always", "--ez", "powerSaving", "true",
                   "--es", "power", "setting")
POWER_VARIANTS = ("off", "gate", "step", "both")


class LogTap:
    """
    Streams the MacawDeviceRun and MacawResident lines while a run lasts: a busy phone's 256 KiB log buffer can drop them
    before `logcat -d` reads them afterwards (the XQ-DQ72 did).
    """

    def __init__(self, serial):
        self.serial = serial
        self.lines = []

    def __enter__(self):
        self.process = subprocess.Popen(["adb", "-s", self.serial, "logcat", "-v", "tag", "-T", "1", "-s", "MacawDeviceRun:I", "MacawResident:I"],
                                        stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, encoding="utf-8", errors="replace")
        self.reader = threading.Thread(target=lambda: self.lines.extend(line.rstrip() for line in self.process.stdout), daemon=True)
        self.reader.start()
        return self

    def __exit__(self, *exc):
        time.sleep(1)
        self.process.terminate()
        self.reader.join(5)

    def tagged(self, tag):
        prefix = f"I/{tag}"
        return [line.split(":", 1)[1].strip() for line in self.lines if line.startswith(prefix) and ":" in line]


READ_ALOUD = []


def run_test(serial, target, arguments=()):
    """device_speech_run.run_test, plus the key lines the log buffer dropped and the read-aloud lines of D69."""
    printed = io.StringIO()
    with LogTap(serial) as tap, contextlib.redirect_stdout(printed):
        ok = device_speech_run.run_test(serial, target, arguments)
    text = printed.getvalue()
    sys.stdout.write(text)
    for line in tap.tagged("MacawDeviceRun"):
        if line not in text:
            print("    " + line[:300])
    READ_ALOUD.extend(line for line in tap.tagged("MacawResident") if line.startswith("Read aloud"))
    return ok


def read_aloud(serial, since):
    """The read-aloud lines of D69 (lengths, language and engine only)."""
    lines = list(READ_ALOUD)
    log = adb(serial, "logcat", "-d", "-v", "raw", "-T", since, "-s", "MacawResident:I", check=False)
    lines += [line.strip() for line in log.splitlines() if line.startswith("Read aloud") and line.strip() not in lines]
    for line in dict.fromkeys(lines):
        print(f"    {line[:200]}")
    READ_ALOUD.clear()


def avd(serial):
    if not is_emulator(serial):
        print(f"{label(serial)}: not an emulator; use `phone`")
        return False
    for permission in ("android.permission.RECORD_AUDIO", "android.permission.POST_NOTIFICATIONS"):
        adb(serial, "shell", "pm", "grant", PACKAGE, permission, check=False)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "wm", "dismiss-keyguard", check=False)
    if not run_test(serial, "SpeechModelSetupDeviceTest", ["macawModels=true", f"macawModelIds={MODELS}"]):
        return False
    adb(serial, "shell", "rm", "-rf", f"/sdcard/Android/data/{PACKAGE}/files/screens", check=False)
    state = assistant_state(serial)
    since = adb(serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")
    try:
        ok = make_assistant(serial) and run_test(serial, TEST, ["macawResident=true", "macawRoom=virtual", "macawKeyguard=true"])
    finally:
        restore_assistant(serial, state)
        adb(serial, "shell", "dumpsys", "battery", "reset", check=False)
        adb(serial, "shell", "wm", "dismiss-keyguard", check=False)
    read_aloud(serial, since)
    # D71 without the assistant role or the overlay permission: the wake word posts "Tap to speak".
    overlay = re.search(r"SYSTEM_ALERT_WINDOW: (\w+)", adb(serial, "shell", "appops", "get", PACKAGE, "SYSTEM_ALERT_WINDOW", check=False))
    adb(serial, "shell", "appops", "set", PACKAGE, "SYSTEM_ALERT_WINDOW", "ignore", check=False)
    try:
        ok = run_test(serial, f"{TEST}#theSwitchListensAndTheWakeWordOpensTheSheet",
                      ["macawResident=true", "macawRoom=virtual", "macawRoute=notify", "macawPrefix=notify"]) and ok
    finally:
        adb(serial, "shell", "appops", "set", PACKAGE, "SYSTEM_ALERT_WINDOW", overlay.group(1) if overlay else "default", check=False)
        adb(serial, "shell", "dumpsys", "battery", "reset", check=False)
    return pull_screens(serial, SCREENS / serial.replace(":", "_"), ok)


def power_arguments(options):
    """The instrumentation arguments of --power / --power-saving / --hit-rate (roadmap P8.1)."""
    arguments = []
    if options.get("power"):
        arguments.append(f"macawPower={options['power']}")
    if options.get("power_saving"):
        arguments.append(f"macawPowerSaving={'true' if options['power_saving'] == 'on' else 'false'}")
    if options.get("hit_rate"):
        arguments.append(f"macawHitRate={options['hit_rate']}")
    return arguments


def phone(serial, options=None):
    options = options or {}
    adb(serial, "shell", "rm", "-rf", f"/sdcard/Android/data/{PACKAGE}/files/screens", check=False)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    since = adb(serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")
    # device_host_scripts.Volume also covers HyperOS / MIUI; MACAW_VOLUME_PERCENT (40 by default) only after the
    # maintainer agreed to a louder playback.
    try:
        with hs.Volume(serial) as volume:
            print(f"{label(serial)}: music {volume.describe()}; power {options.get('power') or 'setting'}, power saving "
                  f"{options.get('power_saving') or 'on (default)'}")
            ok = run_test(serial, TEST, ["macawResident=true", *power_arguments(options)])
    finally:
        adb(serial, "shell", "dumpsys", "battery", "reset", check=False)
    read_aloud(serial, since)
    return pull_screens(serial, SCREENS / serial.replace(":", "_"), ok)


def gate(serial, plays):
    """P8.1.1: the voice gate settings against the plain spotter on live recordings (one line per recording)."""
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    with hs.Volume(serial) as volume:
        print(f"{label(serial)}: music {volume.describe()}")
        return run_test(serial, f"{TEST}#theVoiceGateIsComparedOnRecordings", ["macawResident=true", f"macawGateRecordings={plays}"])


def component_enabled(serial, component):
    """Whether `pm` lists the component as explicitly enabled (the switch of the D31 convention)."""
    package = component.split("/")[0]
    dump = adb(serial, "shell", "dumpsys", "package", package, check=False)
    match = re.search(r"enabledComponents:\s*\n((?:\s+\S+\n)+)", dump + "\n")
    name = component.split("/")[1]
    full = name if not name.startswith(".") else package + name
    return bool(match) and any(line.strip() in (full, name) for line in match.group(1).splitlines())


def stove(serial):
    if "package:" not in adb(serial, "shell", "pm", "path", STOVE, check=False):
        print(f"{label(serial)}: stove SKIP: 3-Stove Agent is not installed")
        return False
    # Shell may only switch the components of a test-only package: 3-Stove Agent's own switch turns its listener on.
    if not component_enabled(serial, STOVE_RESIDENT):
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        adb(serial, "shell", "am", "start", "-n", STOVE_SETTINGS, check=False)
        print(f"{label(serial)}: stove WAIT: 3-Stove Agent's resident listener is off; its settings are open for the maintainer "
              "to turn it on, then run this again")
        return False
    print(f"{label(serial)}: 3-Stove Agent resident listener is on")
    adb(serial, "shell", "rm", "-rf", f"/sdcard/Android/data/{PACKAGE}/files/screens", check=False)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    try:
        ok = run_test(serial, f"{TEST}#theStoveListenerTurnsTheSwitchAway", ["macawResident=true", "macawStove=true", "macawPrefix=stove"])
    finally:
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME", check=False)
    print(f"{label(serial)}: 3-Stove Agent resident listener still on={component_enabled(serial, STOVE_RESIDENT)} "
          "(the maintainer turns it off in its settings)")
    return pull_screens(serial, SCREENS / serial.replace(":", "_"), ok)


def debug_switch(serial, *extras):
    """
    The debug build's adb switch (ResidentDebugActivity, DUMP only): a visible start, as the user's tap is. No `am start
    -W`: the translucent activity finishes in onCreate, and Android 9 then never reports the launch as complete.
    """
    adb(serial, "shell", "am", "start", "-n", f"{PACKAGE}/.ResidentDebugActivity", *extras, check=False)
    time.sleep(2)


def service_state(serial):
    """(running, foreground) of the resident service."""
    dump = adb(serial, "shell", "dumpsys", "activity", "services", SERVICE, check=False)
    return "ServiceRecord" in dump, "isForeground=true" in dump


def recording(serial):
    """
    The plugin's recording as the audio service reports it: None (none), True (silenced) or False (live). Android 9
    lists the active recordings without the silenced flag (added in Android 10); there one means live.
    """
    dump = adb(serial, "shell", "dumpsys", "audio", check=False)
    lines = [line for line in dump.splitlines() if PACKAGE in line and "silenced:" in line]
    if lines:
        return "silenced:true" in lines[-1]
    monitor = dump.split("RecordActivityMonitor", 1)[-1].split("Audio event log", 1)[0] if "RecordActivityMonitor" in dump else ""
    active = [line for line in monitor.splitlines() if f"pack:{PACKAGE}" in line and "session:" in line]
    return False if active else None


def resident_log(serial, since):
    """The service's own lines (states, no text)."""
    log = adb(serial, "logcat", "-d", "-v", "raw", "-T", since, "-s", "MacawResident:I", check=False)
    return [line.strip() for line in log.splitlines() if line.strip() and not line.startswith("-----")]


def background(serial):
    """
    D76 / D77 without instrumentation (an instrumented process keeps a foreground state of its own): on from a visible
    start with "only while charging", home, unplugged (the session ends), plugged again from the background; then the log
    tells whether the reopened session records live or waits for a tap ("Tap to continue listening").
    """
    since = adb(serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")
    adb(serial, "shell", "dumpsys", "battery", "set", "ac", "1", check=False)
    try:
        debug_switch(serial, "--es", "resident", "on", "--ez", "chargingOnly", "true", "--ez", "readAloud", "false")
        time.sleep(8)
        on = service_state(serial)
        live_on = recording(serial)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME", check=False)
        time.sleep(2)
        adb(serial, "shell", "dumpsys", "battery", "unplug", check=False)
        time.sleep(5)
        paused = service_state(serial)
        live_paused = recording(serial)
        adb(serial, "shell", "dumpsys", "battery", "set", "ac", "1", check=False)
        time.sleep(20)
        resumed = service_state(serial)
        live_resumed = recording(serial)
        log = resident_log(serial, since)
    finally:
        debug_switch(serial, "--es", "resident", "off", *DEFAULT_OPTIONS)
        adb(serial, "shell", "dumpsys", "battery", "reset", check=False)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME", check=False)
    blocked = any("without a microphone in the background" in line for line in log)
    reopened = any("Wake session opened" in line and "background" in line for line in log)
    paused_logged = any("paused: not charging" in line for line in log)
    print(f"{label(serial)}: background on={on} recording={live_on}; unplugged paused={paused_logged} service={paused} recording={live_paused}; "
          f"plugged reopened={reopened} service={resumed} recording={live_resumed} blocked={blocked}")
    ok = on == (True, True) and paused_logged and live_paused is None and paused[1] and reopened and (blocked or live_resumed is False)
    print(f"{label(serial)}: background {'PASS' if ok else 'FAIL'} ({'waits for a tap' if blocked else 'records in the background'})")
    return ok


def plugin_cpu_seconds(serial):
    """utime + stime of the plugin's process in seconds (proc stat, 100 clock ticks per second), or None."""
    pid = adb(serial, "shell", "pidof", PACKAGE, check=False).strip().split()
    if not pid:
        return None
    fields = adb(serial, "shell", "cat", f"/proc/{pid[0]}/stat", check=False).rsplit(")", 1)[-1].split()
    return (int(fields[11]) + int(fields[12])) / 100 if len(fields) > 12 else None


def plugin_threads(serial):
    """{tid: (name, utime + stime in seconds)} of the plugin's threads, or {} without a process."""
    pid = adb(serial, "shell", "pidof", PACKAGE, check=False).strip().split()
    if not pid:
        return {}
    dump = adb(serial, "shell", f"for t in /proc/{pid[0]}/task/*; do cat $t/stat; done 2>/dev/null", check=False)
    threads = {}
    for line in dump.splitlines():
        head, _, tail = line.rpartition(")")
        fields = tail.split()
        if "(" not in head or len(fields) < 13:
            continue
        tid, _, name = head.partition(" (")
        threads[tid.strip()] = (name, (int(fields[11]) + int(fields[12])) / 100)
    return threads


def busiest(before, after, count=5):
    """The threads that used the most CPU between two plugin_threads samples, as `name s` (threads of one name summed)."""
    used = {}
    for tid, (name, seconds) in after.items():
        used[name] = used.get(name, 0) + seconds - before.get(tid, (name, 0))[1]
    top = sorted(used.items(), key=lambda item: -item[1])[:count]
    return ", ".join(f"{name} {seconds:.1f} s" for name, seconds in top if seconds >= 0.05)


def power_lines(serial, since):
    """The wake session's power line and the last voice gate line of the period (roadmap P8.1, MacawSessions)."""
    log = adb(serial, "logcat", "-d", "-v", "raw", "-T", since, "-s", "MacawSessions:I", check=False)
    mode = [line.split(": ", 1)[-1].strip() for line in log.splitlines() if "voice gate o" in line]
    gate = [line.split(": ", 1)[-1].strip() for line in log.splitlines() if line.startswith("Voice gate of")]
    return (mode[-1] if mode else "no wake session line"), (gate[-1] if gate else None)


def estimated_mah(serial):
    """The plugin's estimated power use since the batterystats reset in mAh, or None."""
    uid = re.search(r"userId=(\d+)", adb(serial, "shell", "dumpsys", "package", PACKAGE, check=False))
    if not uid:
        return None
    stats = adb(serial, "shell", "dumpsys", "batterystats", check=False)
    match = re.search(rf"UID u0a{int(uid.group(1)) - 10000}:\s*([\d.]+)", stats)
    return float(match.group(1)) if match else None


def battery_period(serial, minutes, resident_on, power=None, power_saving=None):
    """
    One screen-off period on battery as batterystats sees it (`dumpsys battery unplug`); answers (mAh, CPU seconds). On:
    "only while charging" off, or the unplug pauses the session, and no time limit; [power] a measured variant of P8.1.1
    or the stored switch ([power_saving] `on` / `off`, else unchanged).
    """
    since = adb(serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")
    if resident_on:
        extras = ["--es", "power", power or "setting"]
        if power_saving:
            extras += ["--ez", "powerSaving", "true" if power_saving == "on" else "false"]
        debug_switch(serial, "--es", "resident", "on", "--ez", "chargingOnly", "false", "--es", "duration", "always", *extras)
        time.sleep(10)
        if service_state(serial) != (True, True):
            print(f"{label(serial)}: battery FAIL: the resident service did not start")
            return None
    else:
        debug_switch(serial, "--es", "resident", "off")
        time.sleep(3)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME", check=False)
    adb(serial, "shell", "dumpsys", "batterystats", "--reset", check=False)
    adb(serial, "shell", "dumpsys", "battery", "unplug", check=False)
    cpu = plugin_cpu_seconds(serial)
    threads = plugin_threads(serial)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP", check=False)
    time.sleep(minutes * 60)
    after = plugin_cpu_seconds(serial)
    threads_after = plugin_threads(serial)
    mah = estimated_mah(serial)
    running = service_state(serial)
    live = recording(serial)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "dumpsys", "battery", "reset", check=False)
    used = None if after is None else round(after - (cpu or 0), 1)
    print(f"{label(serial)}: battery {'on ' if resident_on else 'off'} {minutes} min screen off: estimate {mah} mAh, plugin CPU {used} s"
          f"{' (no process)' if after is None else ''}, service {running}, recording {live}")
    if threads_after:
        print(f"    busiest threads: {busiest(threads, threads_after)}")
    if resident_on:
        mode, gate = power_lines(serial, since)
        print(f"    power {power or 'setting'}: {mode}" + (f"; {gate}" if gate else ""))
    if resident_on and live is not False:
        print(f"{label(serial)}: battery FAIL: the resident wake word did not record to the end")
        return None
    return mah, used


def battery(serial, minutes, on_only=False, power=None, power_saving=None):
    """D73 / P7.4.8: off first (the baseline), then on; the switch goes off and the options back to their defaults at the end."""
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    try:
        off = None if on_only else battery_period(serial, minutes, False)
        on = battery_period(serial, minutes, True, power, power_saving)
    finally:
        debug_switch(serial, "--es", "resident", "off", *DEFAULT_OPTIONS)
        adb(serial, "shell", "dumpsys", "battery", "reset", check=False)
    ok = (on_only or off is not None) and on is not None
    print(f"{label(serial)}: battery {'PASS' if ok else 'FAIL'} (off {off}, on {on})")
    return ok


def options_of(argv):
    """--power / --power-saving / --hit-rate (roadmap P8.1) and the remaining positional arguments, or None when invalid."""
    options, rest = {}, []
    names = {"--power": "power", "--power-saving": "power_saving", "--hit-rate": "hit_rate"}
    items = iter(argv)
    for item in items:
        if item in names:
            options[names[item]] = next(items, None)
        else:
            rest.append(item)
    if options.get("power") not in (None, *POWER_VARIANTS) or options.get("power_saving") not in (None, "on", "off") \
            or not (options.get("hit_rate") or "1").isdigit():
        return None, rest
    return options, rest


def main(argv):
    options, argv = options_of(argv)
    if options is None or len(argv) < 2 or argv[0] not in ("avd", "phone", "stove", "background", "battery", "gate"):
        print(__doc__)
        return 2
    action, serial = argv[0], argv[1]
    if not install(serial):
        return 1
    if action == "gate":
        return 0 if gate(serial, int(argv[2]) if len(argv) > 2 else 3) else 1
    if action == "battery":
        return 0 if battery(serial, int(argv[2]) if len(argv) > 2 else 30, "on" in argv[3:], options.get("power"), options.get("power_saving")) else 1
    if action == "phone":
        return 0 if phone(serial, options) else 1
    return 0 if {"avd": avd, "stove": stove, "background": background}[action](serial) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
