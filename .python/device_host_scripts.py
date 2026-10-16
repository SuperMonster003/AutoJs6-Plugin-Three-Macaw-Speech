"""AutoJs6 scripts against the real 3-Macaw Speech plugin on one device (roadmap P3.5, D22, appendix D Q5).

    py .python/device_host_scripts.py api <serial>          capabilities, models, recognize, detectVoice, external session
    py .python/device_host_scripts.py listen <serial>       microphone dictation: foreground, background (HOME), screen off
    py .python/device_host_scripts.py late <serial>         a microphone session opened while AutoJs6 is already in the background
    py .python/device_host_scripts.py wake <serial>         wake word with thenListen, the keyword and a command played aloud
    py .python/device_host_scripts.py kill <serial>         the host process dies during a microphone session

Scripts start through AutoJs6's RunIntentActivity (no instrumentation, so every process keeps its real state) and write
a JSON result next to themselves in shared storage, which AutoJs6 reads and writes with all-files access. Audio for the
microphone runs is played by the script with `media.playMusic` through the device speaker at the current media volume
(raised to 40 % for the run when lower, then restored). Every run samples `procState/capability` of AutoJs6 and of the plugin process
and whether the plugin's microphone foreground service is in the foreground. Output: one line per item, no transcript.
A secure lock screen keeps activities from starting: unlock the device first. Nothing here answers a system dialog.
"""
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

HOST = "org.autojs.autojs6"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
REMOTE = "/sdcard/Download/autojs6-macaw-run"
ROOT = Path(__file__).resolve().parent.parent
AUDIO = ROOT / "app" / "src" / "androidTest" / "assets" / "test-audio"
STREAMING = "asr.zipformer-streaming.zh-en.2023-02-20.int8"
SENSE_VOICE = "asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8"
PHASE_SECONDS = 10

COMMON = """
var out = "%(out)s";
var dir = "%(dir)s";
var r = { events: [], errors: [], warnings: [] };
files.write(out + ".started", String(Date.now()));
function done() { files.write(out, JSON.stringify(r)); }
function fail(e) { r.errors.push(String(e.code || "") + " " + String(e.message || e)); done(); }
"""

SCRIPTS = {
    "api": COMMON + """
try {
    r.available = speech.isAvailable();
    var caps = speech.capabilities();
    r.features = caps.features.join(",");
    r.catalog = caps.catalogVersion;
    r.engine = caps.engineName + " " + caps.engineVersion;
    r.microphone = caps.microphoneGranted;
    r.installed = speech.models.list({ installed: true }).map(function (m) { return m.id; }).join(",");
    r.defaultAsr = (speech.models.getDefault("asr") || {}).id;
    var t = Date.now();
    var en = speech.recognize(dir + "/en.wav", { model: "%(streaming)s" });
    r.enMs = Date.now() - t; r.enChars = en.text.length; r.enSegments = en.segments.length; r.enDuration = en.durationMillis; r.enText = en.text;
    t = Date.now();
    var zh = speech.recognize(dir + "/zh.wav", { model: "%(sense)s" });
    r.zhMs = Date.now() - t; r.zhChars = zh.text.length; r.zhLanguage = zh.language; r.zhTags = JSON.stringify(zh.segments[0].tags || {});
    var vad = speech.detectVoice(dir + "/mixed-zh-en.wav");
    r.vadSegments = vad.segments.length; r.vadDuration = vad.durationMillis;
    try { speech.recognize(dir + "/not-a-wav.wav"); r.badFormat = "accepted"; } catch (e) { r.badFormat = e.code; }
    try { speech.recognize(dir + "/en.wav", { model: "asr.no.such.model" }); r.badModel = "accepted"; } catch (e) { r.badModel = e.code; }
    var bytes = files.readBytes(dir + "/en.wav");
    var pcm = java.util.Arrays.copyOfRange(bytes, 44, bytes.length);
    var s = speech.listen({ source: "external", model: "%(streaming)s", timeoutMillis: 0 });
    s.on("ready", function () { r.events.push("ready"); });
    s.on("partial", function () { if (r.events[r.events.length - 1] !== "partial") r.events.push("partial"); });
    s.on("final", function (e) { r.events.push("final"); });
    s.on("endpoint", function () { r.events.push("endpoint"); });
    s.on("warning", function (e) { r.warnings.push(e.code); });
    s.on("error", function (e) { r.errors.push(e.code + " " + e.message); });
    s.on("end", function (e) { r.events.push("end:" + e.reason); r.externalChars = (e.text || "").length; done(); });
    for (var i = 0; i < pcm.length; i += 64000) {
        s.feed(java.util.Arrays.copyOfRange(pcm, i, Math.min(pcm.length, i + 64000)));
    }
    s.stop();
} catch (e) { fail(e); }
""",
    "listen": COMMON + """
try {
    var s = speech.listen({ model: "%(streaming)s", timeoutMillis: 0, maxDurationMillis: %(duration)d });
    s.on("ready", function () {
        r.events.push([Date.now(), "ready"]);
        media.playMusic(dir + "/loop.wav", 1, true);
    });
    s.on("partial", function () { r.events.push([Date.now(), "partial"]); });
    s.on("final", function () { r.events.push([Date.now(), "final"]); });
    s.on("vad", function (e) { r.events.push([Date.now(), e.active ? "speech" : "silence"]); });
    s.on("warning", function (e) { r.events.push([Date.now(), "warning:" + e.code]); r.warnings.push(e.code + " " + e.message); });
    s.on("error", function (e) { r.errors.push(e.code + " " + e.message); });
    s.on("end", function (e) { media.stopMusic(); r.reason = e.reason; done(); });
} catch (e) { fail(e); }
""",
    "late": COMMON + """
setTimeout(function () {
    r.openedAt = Date.now();
    try {
        var s = speech.listen({ model: "%(streaming)s", timeoutMillis: 0, maxDurationMillis: %(duration)d });
        s.on("ready", function () { r.events.push([Date.now(), "ready"]); media.playMusic(dir + "/loop.wav", 1, true); });
        s.on("partial", function () { r.events.push([Date.now(), "partial"]); });
        s.on("vad", function (e) { r.events.push([Date.now(), e.active ? "speech" : "silence"]); });
        s.on("warning", function (e) { r.events.push([Date.now(), "warning:" + e.code]); r.warnings.push(e.code + " " + e.message); });
        s.on("error", function (e) { r.errors.push(e.code + " " + e.message); });
        s.on("end", function (e) { media.stopMusic(); r.reason = e.reason; done(); });
    } catch (e) { fail(e); }
}, %(delay)d);
""",
    "wake": COMMON + """
try {
    var w = speech.wake(["%(keyword)s"], { thenListen: { model: "%(streaming)s", silenceEndMillis: 1200 } });
    w.on("ready", function () { r.events.push("ready"); media.playMusic(dir + "/wake.wav", 1, false); });
    w.on("wake", function (e) {
        r.events.push("wake:" + e.round);
        e.listen.on("final", function (f) { r.events.push("final:" + e.round); r.commandChars = f.text.length; });
        e.listen.on("end", function (f) { r.events.push("round-end:" + f.reason); setTimeout(function () { w.stop(); }, 500); });
    });
    w.on("warning", function (e) { r.warnings.push(e.code + " " + e.message); });
    w.on("error", function (e) { r.errors.push(e.code + " " + e.message); });
    w.on("end", function (e) { media.stopMusic(); r.reason = e.reason; done(); });
    setTimeout(function () { if (w.isActive()) w.stop(); }, 40000);
} catch (e) { fail(e); }
""",
}


def adb(serial, *args, check=True):
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", check=check).stdout.strip()


def device_now(serial):
    return int(adb(serial, "shell", "date", "+%s%3N"))


def process_state(serial, package, process):
    dump = adb(serial, "shell", "dumpsys", "activity", "processes", package, check=False).splitlines()
    for index, line in enumerate(dump):
        if "ProcessRecord{" in line and f":{process}/" in line and "*APP*" in line:
            state = capability = "?"
            for detail in dump[index + 1:index + 80]:
                detail = detail.strip()
                if "*APP*" in detail:
                    break
                if detail.startswith("curProcState="):
                    state = detail.split()[0].split("=")[1]
                if detail.startswith("curCapability="):
                    capability = detail.split()[0].split("=")[1]
            return f"{state}/{capability}"
    return "-"


def foreground_service(serial):
    dump = adb(serial, "shell", "dumpsys", "activity", "services", PLUGIN, check=False)
    blocks = dump.split("* ServiceRecord{")
    for block in blocks:
        if "MicrophoneService" in block.split("\n", 1)[0]:
            return "on" if "isForeground=true" in block else "started"
    return "off"


def states(serial):
    return f"host {process_state(serial, HOST, HOST)}, plugin {process_state(serial, PLUGIN, PLUGIN)}, fgs {foreground_service(serial)}"


def media_volume(serial):
    """(command, current, maximum) of the music stream, or None when the shell has no volume command."""
    for command in (["cmd", "media_session", "volume"], ["media", "volume"]):
        out = adb(serial, "shell", *command, "--stream", "3", "--get", check=False)
        match = re.search(r"volume is (\d+) in range \[(\d+)\.\.(\d+)\]", out)
        if match:
            return command, int(match.group(1)), int(match.group(3))
    return None


class Volume:
    """Raises the music volume to MACAW_VOLUME_PERCENT (40 by default) for a playback run when it is lower, then restores it."""

    def __init__(self, serial):
        self.serial = serial
        self.state = media_volume(serial)
        self.percent = int(os.environ.get("MACAW_VOLUME_PERCENT", "40"))

    def __enter__(self):
        if self.state:
            command, current, maximum = self.state
            target = max(1, maximum * self.percent // 100)
            if current < target:
                adb(self.serial, "shell", *command, "--stream", "3", "--set", str(target), check=False)
        return self

    def __exit__(self, *exc):
        if self.state:
            command, current, _ = self.state
            adb(self.serial, "shell", *command, "--stream", "3", "--set", str(current), check=False)

    def describe(self):
        if not self.state:
            return "volume unknown"
        _, current, maximum = self.state
        return f"volume {current}/{maximum} (raised to {max(current, max(1, maximum * self.percent // 100))} for the run)"


def launcher(serial):
    return adb(serial, "shell", "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", HOST).splitlines()[-1].strip()


def prepare(serial):
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    for name in ("en", "zh", "mixed-zh-en", "kws-zh-6"):
        adb(serial, "push", str(AUDIO / f"{name}.wav"), f"{REMOTE}/{name}.wav")
    adb(serial, "push", str(AUDIO / "mixed-zh-en.wav"), f"{REMOTE}/loop.wav")
    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as handle:
        handle.write(b"ID3" + bytes(4096))
    adb(serial, "push", handle.name, f"{REMOTE}/not-a-wav.wav")
    wake = AUDIO / "kws-zh-6.wav"
    # The keyword recording twice (an acoustic miss gets a second chance), one second of silence after each, then the
    # mixed command (all 16 kHz mono PCM16 with canonical headers).
    keyword, command = wake.read_bytes(), (AUDIO / "mixed-zh-en.wav").read_bytes()
    data = keyword[44:] + bytes(32000) + keyword[44:] + bytes(32000) + command[44:]
    header = bytearray(keyword[:44])
    header[4:8] = (36 + len(data)).to_bytes(4, "little")
    header[40:44] = len(data).to_bytes(4, "little")
    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as handle:
        handle.write(bytes(header) + data)
    adb(serial, "push", handle.name, f"{REMOTE}/wake.wav")


def start(serial, name, values):
    script = SCRIPTS[name] % {"out": f"{REMOTE}/{name}.json", "dir": REMOTE, "streaming": STREAMING, "sense": SENSE_VOICE, **values}
    local = Path(tempfile.gettempdir()) / f"macaw-{name}-{serial.replace(':', '_')}.js"
    local.write_text(script, encoding="utf-8")
    adb(serial, "push", str(local), f"{REMOTE}/{name}.js")
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", launcher(serial))
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/{name}.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    for _ in range(10):
        time.sleep(1)
        if adb(serial, "shell", "ls", f"{REMOTE}/{name}.json.started", check=False).endswith(".started"):
            return True
    return False


def result(serial, name, seconds):
    deadline = time.time() + seconds
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{REMOTE}/{name}.json", check=False)
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(2)
    return None


def label(serial):
    return f"{adb(serial, 'shell', 'getprop', 'ro.product.model')} ({serial}, API {adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')})"


def run_api(serial):
    prepare(serial)
    if not start(serial, "api", {}):
        print(f"{label(serial)}: api: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
        return False
    r = result(serial, "api", 300)
    if r is None:
        print(f"{label(serial)}: api: FAIL no result")
        return False
    ok = not r["errors"] and r.get("available") and r.get("badFormat") == "UNSUPPORTED_FORMAT" and r.get("badModel") == "MODEL_MISSING" and r["events"][0] == "ready" and r["events"][-1] == "end:stopped"
    print(f"{label(serial)}: api: {'PASS' if ok else 'FAIL'} available={r.get('available')} features={r.get('features')} catalog={r.get('catalog')} engine={r.get('engine')} microphone={r.get('microphone')}")
    print(f"    installed={r.get('installed')} defaultAsr={r.get('defaultAsr')}")
    print(f"    recognize en: {r.get('enMs')} ms, {r.get('enChars')} chars, {r.get('enSegments')} segments, {r.get('enDuration')} ms audio; zh (SenseVoice): {r.get('zhMs')} ms, {r.get('zhChars')} chars, language {r.get('zhLanguage')}, tags {r.get('zhTags')}")
    print(f"    detectVoice segments={r.get('vadSegments')} duration={r.get('vadDuration')}; refusals format={r.get('badFormat')} model={r.get('badModel')}")
    print(f"    external session events={','.join(r['events'])} chars={r.get('externalChars')} warnings={r['warnings']} errors={r['errors']}")
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    return ok


def summarize(events, bounds, names):
    for index, name in enumerate(names):
        inside = [e for e in events if bounds[index] + 1000 <= e[0] < bounds[index + 1]]
        kinds = {}
        for e in inside:
            kinds[e[1]] = kinds.get(e[1], 0) + 1
        verdict = "audio" if kinds.get("partial", 0) + kinds.get("speech", 0) > 0 else ("silenced" if any(k.startswith("warning:PERMISSION_DENIED") for k in kinds) else "no events")
        yield name, verdict, kinds


def run_listen(serial, late=False):
    name = "late" if late else "listen"
    prepare(serial)
    total = PHASE_SECONDS * 3 + 6
    values = {"duration": total * 1000 + (8000 if late else 0), "delay": 6000}
    if not start(serial, name, values):
        print(f"{label(serial)}: {name}: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
        return False
    sampled = {}
    marks = {}
    if late:
        # AutoJs6 leaves the foreground before the session opens (the script waits 6 s).
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(8)
        marks["background"] = device_now(serial)
        time.sleep(PHASE_SECONDS / 2)
        sampled["background"] = states(serial)
        time.sleep(PHASE_SECONDS / 2)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
        marks["screen-off"] = device_now(serial)
        time.sleep(PHASE_SECONDS / 2)
        sampled["screen-off"] = states(serial)
        time.sleep(PHASE_SECONDS / 2)
        phases = ["background", "screen-off"]
    else:
        time.sleep(4)
        marks["foreground"] = device_now(serial)
        time.sleep(PHASE_SECONDS / 2)
        sampled["foreground"] = states(serial)
        time.sleep(PHASE_SECONDS / 2)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        marks["background"] = device_now(serial)
        time.sleep(PHASE_SECONDS / 2)
        sampled["background"] = states(serial)
        time.sleep(PHASE_SECONDS / 2)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
        marks["screen-off"] = device_now(serial)
        time.sleep(PHASE_SECONDS / 2)
        sampled["screen-off"] = states(serial)
        time.sleep(PHASE_SECONDS / 2)
        phases = ["foreground", "background", "screen-off"]
    end = device_now(serial)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    r = result(serial, name, 60)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    volume = adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "3", "--get", check=False).splitlines()
    volume = next((line.split("volume is")[1].strip() for line in volume if "volume is" in line), "?")
    if r is None:
        print(f"{label(serial)}: {name}: FAIL no result")
        return False
    if r["errors"]:
        print(f"{label(serial)}: {name}: FAIL {r['errors'][0]}")
        return False
    bounds = [marks[p] for p in phases] + [end]
    print(f"{label(serial)}: {name}: end reason {r.get('reason')}, media volume {volume}, warnings {sorted(set(w.split(' ')[0] for w in r['warnings']))}")
    for phase, verdict, kinds in summarize(r["events"], bounds, phases):
        print(f"    {phase}: {verdict} {kinds}; {sampled[phase]}")
    for warning in sorted(set(r["warnings"])):
        print(f"    warning: {warning[:240]}")
    return True


def run_wake(serial):
    prepare(serial)
    if not start(serial, "wake", {"keyword": "朱丽楠"}):
        print(f"{label(serial)}: wake: BLOCKED the script did not start")
        return False
    r = result(serial, "wake", 90)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    if r is None:
        print(f"{label(serial)}: wake: FAIL no result")
        return False
    # The host hands the round to the script as the wake event's listen session; the event itself has no round field.
    ok = not r["errors"] and any(e.startswith("wake:") for e in r["events"]) and any(e.startswith("final:") for e in r["events"]) and "round-end:stopped" in r["events"]
    print(f"{label(serial)}: wake: {'PASS' if ok else 'FAIL'} events={','.join(r['events'])} commandChars={r.get('commandChars')} warnings={r['warnings']} errors={r['errors']}")
    return ok


def last_recording_event(serial):
    """The plugin's last line in the recording event log of `dumpsys audio` (`rec start` / `rec update` / `rec stop`)."""
    dump = adb(serial, "shell", "dumpsys", "audio", check=False)
    events = [line.strip() for line in dump.splitlines() if f"pack:{PLUGIN}" in line and " rec " in line]
    return events[-1] if events else ""


def run_kill(serial):
    prepare(serial)
    if not start(serial, "listen", {"duration": 60_000}):
        print(f"{label(serial)}: kill: BLOCKED the script did not start")
        return False
    time.sleep(6)
    before = states(serial)
    event = last_recording_event(serial)
    recording = bool(event) and " rec stop " not in event
    stopped = time.time()
    adb(serial, "shell", "am", "force-stop", HOST)
    released = None
    while time.time() - stopped < 10:
        if " rec stop " in last_recording_event(serial):
            released = time.time() - stopped
            break
        time.sleep(0.05)
    service = None
    while time.time() - stopped < 10:
        if foreground_service(serial) == "off":
            service = time.time() - stopped
            break
        time.sleep(0.05)
    after = states(serial)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    ok = recording and released is not None and released <= 1.0
    print(f"{label(serial)}: kill: {'PASS' if ok else 'FAIL'} recording before={recording}; {before}; recorder stopped after "
          f"{'-' if released is None else round(released, 2)} s (rec stop in dumpsys audio, polled over adb), microphone service off after "
          f"{'-' if service is None else round(service, 2)} s; {after}")
    return ok


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 2
    action, serial = argv
    runs = {"api": run_api, "listen": run_listen, "late": lambda s: run_listen(s, late=True), "wake": run_wake, "kill": run_kill}
    if action in ("listen", "late", "wake"):
        with Volume(serial) as volume:
            print(f"{label(serial)}: media {volume.describe()}")
            return 0 if runs[action](serial) else 1
    if action not in runs:
        print(__doc__)
        return 2
    return 0 if runs[action](serial) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
