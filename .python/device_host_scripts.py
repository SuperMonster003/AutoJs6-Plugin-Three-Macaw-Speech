"""AutoJs6 scripts against the real 3-Macaw Speech plugin on one device (roadmap P3.5, D22, appendix D Q5).

    py .python/device_host_scripts.py api <serial>          capabilities, models, recognize, detectVoice, external session
    py .python/device_host_scripts.py listen <serial>       microphone dictation: foreground, background (HOME), screen off
    py .python/device_host_scripts.py late <serial>         a microphone session opened while AutoJs6 is already in the background
    py .python/device_host_scripts.py wake <serial>         wake word with thenListen, the keyword and a command played aloud
    py .python/device_host_scripts.py kill <serial>         the host process dies during a microphone session
    py .python/device_host_scripts.py tools <serial>        the plugin tools through `plugins.tools` (roadmap P7.6.4, P8.3.5), TTS at 70 %
    py .python/device_host_scripts.py speaker <serial>      speech.speaker enroll / verify / identify / list / remove and the two
                                                            speaker tools with the LibriSpeech readers of locks/speaker-test-audio.lock
                                                            (roadmap P8.3.6; needs a host with contract v2 and the speaker model)
    py .python/device_host_scripts.py analysis <serial>     speech.diarize (identify, onProgress) / detectLanguage / tag, tagAsync and
                                                            the three analysis tools with the recordings of locks/analysis-test-audio.lock
                                                            (roadmap P8.4.5; needs a host with P8.4.1 and the segment, speaker, langid
                                                            and tagger models)

Scripts start through AutoJs6's RunIntentActivity (no instrumentation, so every process keeps its real state) and write
a JSON result next to themselves in shared storage, which AutoJs6 reads and writes with all-files access. Audio for the
microphone runs is played by the script with `media.playMusic` through the device speaker at the current media volume
(raised to 40 % for the run when lower, then restored). Every run samples `procState/capability` of AutoJs6 and of the plugin process
and whether the plugin's microphone foreground service is in the foreground; `listen` and `late` also count the plugin's
activity starts that Android blocked (logcat, with the caller's process state).
Output: one line per item, no transcript.
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
SPEAKER_AUDIO = ROOT / "app" / "src" / "androidTest" / "assets" / "speaker-audio"
ANALYSIS_AUDIO = ROOT / "app" / "src" / "androidTest" / "assets" / "analysis-audio"
STREAMING = "asr.zipformer-streaming.zh-en.2023-02-20.int8"
SENSE_VOICE = "asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8"
PHASE_SECONDS = 10
EN_REFERENCE = "AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS"
TOOLS_SYNTHESIS_TEXT = "The quick brown fox jumps over the lazy dog near the river bank"
TOOLS_NAMES = ("speech_detect_language,speech_diarize,speech_listen,speech_speak,speech_speaker_identify,speech_speaker_verify,"
               "speech_synthesize,speech_tag,speech_transcribe")
ANALYSIS_NAME = "host-test-analysis-a"
ANALYSIS_TAGS = {"Music": 0, "White noise": 10000, "Beep, bleep": 20000}
SPEAKER_NAMES = ("host-test-reader-a", "host-test-reader-b")
MAX_CER = 0.15
MAX_ROUND_TRIP_CER = 0.25

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
    // Audio goes in after `ready`, as the documented example does: a stop() before `ready` ends the session unheard.
    s.on("ready", function () {
        r.events.push("ready");
        for (var i = 0; i < pcm.length; i += 64000) {
            s.feed(java.util.Arrays.copyOfRange(pcm, i, Math.min(pcm.length, i + 64000)));
        }
        s.stop();
    });
    s.on("partial", function () { if (r.events[r.events.length - 1] !== "partial") r.events.push("partial"); });
    s.on("final", function (e) { r.events.push("final"); });
    s.on("endpoint", function () { r.events.push("endpoint"); });
    s.on("warning", function (e) { r.warnings.push(e.code); });
    s.on("error", function (e) { r.errors.push(e.code + " " + e.message); });
    s.on("end", function (e) { r.events.push("end:" + e.reason); r.externalChars = (e.text || "").length; done(); });
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
    "speaker": COMMON + """
function step(key, body) {
    var t = Date.now();
    try { r[key] = body(); } catch (e) { r[key] = { error: String(e.code || "") + " " + String(e.message || e).slice(0, 240) }; }
    r[key].ms = Date.now() - t;
}
function clip(name) { return dir + "/" + name + ".wav"; }
function enrollFiles(v) { return [1, 2, 3].map(function (i) { return clip("libri-" + v + "-enroll-" + i); }); }
var A = "%(nameA)s", B = "%(nameB)s";
r.features = speech.capabilities().features.join(",");
// A host without contract v2 has no speech.speaker; the run reports it instead of failing on every step.
// zh-CN: 不含契约 v2 的宿主没有 speech.speaker; 直接报告, 不让每一步都失败.
r.hasSpeaker = typeof speech.speaker !== "undefined";
if (!r.hasSpeaker) { done(); exit(); }
step("enroll", function () {
    var a = speech.speaker.enroll(A, enrollFiles("a"), { replace: true });
    var b = speech.speaker.enroll(B, enrollFiles("b"), { replace: true });
    return { a: a.samples, b: b.samples, speechMs: a.durationMillis };
});
step("verify", function () {
    var out = {};
    ["a", "b"].forEach(function (v) {
        [1, 2].forEach(function (i) {
            var c = clip("libri-" + v + "-trial-" + i);
            var same = speech.speaker.verify(v === "a" ? A : B, c), other = speech.speaker.verify(v === "a" ? B : A, c);
            out[v + i] = { same: same.score, sameOk: same.matched, other: other.score, otherOk: other.matched, threshold: same.threshold };
        });
    });
    return out;
});
step("identify", function () {
    var w = speech.speaker.identify(clip("libri-b-trial-1"), { topK: 2 });
    return { name: w.name, matched: w.matched, candidates: (w.candidates || []).length };
});
step("tools", function () {
    var v = plugins.tools.call("speech_speaker_verify", { file: clip("libri-a-trial-1"), name: A });
    var i = plugins.tools.call("speech_speaker_identify", { file: clip("libri-a-trial-2"), topK: 2 });
    var keys = Object.keys(v.result).concat(Object.keys(i.result)).join(",");
    return { verify: v.result.matched, identify: i.result.name, candidates: i.result.candidates.length, vectorFree: keys.indexOf("mean") < 0 && keys.indexOf("embedding") < 0 };
});
step("list", function () {
    return { names: speech.speaker.list().map(function (s) { return s.name; }).filter(function (n) { return n === A || n === B; }).join(",") };
});
step("remove", function () {
    var removed = speech.speaker.remove(A) && speech.speaker.remove(B);
    var refused = "";
    try { speech.speaker.verify(A, clip("libri-a-trial-1")); refused = "accepted"; } catch (e) { refused = String(e.code || e.message || e).slice(0, 80); }
    return { removed: removed, refused: refused };
});
// The Promise settles on this script's event loop, so the script waits for it instead of blocking. zh-CN: Promise 在本脚本的事件循环上兑现.
speech.speaker.enroll(A, enrollFiles("a"), { replace: true });
var asyncStarted = Date.now();
var settled = false;
function settle(value) { if (settled) return; settled = true; r.async = { matched: value, ms: Date.now() - asyncStarted }; try { speech.speaker.remove(A); } catch (e) {} done(); }
speech.speaker.verifyAsync(A, clip("libri-a-trial-2")).then(function (v) { settle(v.matched); }, function (e) { settle(String(e.code || e.message || e)); });
setTimeout(function () { settle("timeout"); }, 60000);
""",
    "analysis": COMMON + """
function step(key, body) {
    var t = Date.now();
    try { r[key] = body(); } catch (e) { r[key] = { error: String(e.code || "") + " " + String(e.message || e).slice(0, 240) }; }
    r[key].ms = Date.now() - t;
}
function clip(name) { return dir + "/" + name + ".wav"; }
var A = "%(nameA)s";
r.features = speech.capabilities().features.join(",");
// A host without P8.4.1 has no speech.diarize; the run reports it instead of failing on every step.
// zh-CN: 不含 P8.4.1 的宿主没有 speech.diarize; 直接报告, 不让每一步都失败.
r.hasAnalysis = typeof speech.diarize === "function";
if (!r.hasAnalysis) { done(); exit(); }
step("diarize", function () {
    speech.speaker.enroll(A, [1, 2, 3].map(function (i) { return clip("libri-a-enroll-" + i); }), { replace: true });
    var reports = 0;
    var d = speech.diarize(clip("two-speakers"), { identify: true, onProgress: function (done, total) { if (done > 0) reports++; } });
    try { speech.speaker.remove(A); } catch (e) {}
    return {
        speakers: d.speakers, durationMillis: d.durationMillis, modelId: d.modelId, reports: reports,
        turns: d.segments.map(function (s) { return [s.startMillis, s.endMillis, s.speaker]; }),
        names: (d.names || []).map(function (n) { return [n.speaker, n.name]; }),
    };
});
step("language", function () {
    var out = {};
    ["language-zh", "language-ru", "en"].forEach(function (n) { out[n] = speech.detectLanguage(clip(n)).language; });
    return out;
});
step("tag", function () {
    var t = speech.tag(clip("sounds"), { topK: 3, timeline: true });
    return { labels: t.labels.map(function (l) { return [l.label, l.startMillis]; }), windows: (t.timeline || []).length };
});
step("tools", function () {
    var d = plugins.tools.call("speech_diarize", { file: clip("two-speakers"), speakers: 2 });
    var g = plugins.tools.call("speech_detect_language", { file: clip("language-ru") });
    var t = plugins.tools.call("speech_tag", { file: clip("sounds"), topK: 1 });
    var text = JSON.stringify(d.result) + JSON.stringify(g.result) + JSON.stringify(t.result);
    return { speakers: d.result.speakers, language: g.result.language, top: t.result.labels[0].label, vectorFree: text.indexOf("embedding") < 0 };
});
// The Promise settles on this script's event loop. zh-CN: Promise 在本脚本的事件循环上兑现.
var asyncStarted = Date.now();
var settled = false;
function settle(value) { if (settled) return; settled = true; r.async = { top: value, ms: Date.now() - asyncStarted }; done(); }
speech.tagAsync(clip("sounds"), { topK: 1 }).then(function (v) { settle(v.labels[0].label); }, function (e) { settle(String(e.code || e.message || e)); });
setTimeout(function () { settle("timeout"); }, 120000);
""",
    "tools": COMMON + """
function step(key, body) {
    var t = Date.now();
    try { r[key] = body(); } catch (e) { r[key] = { error: String(e.message || e).slice(0, 240) }; }
    r[key].ms = Date.now() - t;
}
function refusal(name, args) {
    try { plugins.tools.call(name, args); return "accepted"; } catch (e) { return String(e.message || e).slice(0, 240); }
}
function fileBytes(path) { var f = new java.io.File(path); return f.exists() ? f.length() : -1; }
step("list", function () {
    var names = plugins.tools.list().map(function (t) { return t.name; }).filter(function (n) { return n.indexOf("speech_") === 0; });
    names.sort();
    return { names: names.join(",") };
});
step("transcribe", function () {
    var c = plugins.tools.call("speech_transcribe", { file: dir + "/en.wav", language: "en" });
    return { text: c.result.text, modelId: c.result.modelId, durationMillis: c.result.durationMillis, segments: (c.result.segments || []).length, callMs: c.durationMs };
});
step("synthesize", function () {
    var path = dir + "/synth.m4a";
    var c = plugins.tools.call("speech_synthesize", { text: "%(sentence)s", language: "en", format: "m4a" }, { outputs: { audio: path }, overwrite: true });
    var o = (c.outputs || []).filter(function (x) { return x.name === "audio"; })[0] || {};
    return { mimeType: c.result.mimeType, bytes: c.result.bytes, outputBytes: o.bytes, outputMime: o.mimeType, samePath: o.path != null && String(new java.io.File(o.path).getCanonicalPath()) === String(new java.io.File(path).getCanonicalPath()), fileBytes: fileBytes(path), durationMillis: c.result.durationMillis, modelId: c.result.modelId };
});
step("roundTrip", function () {
    return { text: plugins.tools.call("speech_transcribe", { file: dir + "/synth.m4a", language: "en" }).result.text };
});
step("speak", function () {
    var c = plugins.tools.call("speech_speak", { text: "%(spoken)s", language: "en" });
    return { engine: c.result.engine, completed: c.result.completed, durationMillis: c.result.durationMillis };
});
step("refusals", function () {
    return {
        unknown: refusal("speech_whisper", {}),
        unreadable: refusal("speech_transcribe", { file: dir + "/not-a-wav.wav" }),
        invalid: refusal("speech_synthesize", { text: "hello", format: "mp3" })
    };
});
// The dictation: the recording starts once the model has loaded, so the clip plays when AudioManager first lists a new
// active recording (after 20 s without one it plays anyway, and the result says so).
var audio = context.getSystemService(android.content.Context.AUDIO_SERVICE);
function recordings() { try { return audio.getActiveRecordingConfigurations().size(); } catch (e) { return -1; } }
r.listen = { before: recordings() };
var started = Date.now();
function settle() { try { media.stopMusic(); } catch (e) {} r.listen.ms = Date.now() - started; done(); }
plugins.tools.callAsync("speech_listen", { language: "en" }, { timeoutMs: %(listenTimeout)d }).then(function (c) {
    r.listen.text = c.result.text; r.listen.endReason = c.result.endReason; r.listen.durationMillis = c.result.durationMillis; r.listen.modelId = c.result.modelId;
    settle();
}, function (e) { r.listen.error = String(e.message || e).slice(0, 240); settle(); });
var poll = setInterval(function () {
    var now = recordings();
    if (now > r.listen.before || Date.now() - started > 20000) {
        clearInterval(poll);
        r.listen.captureMs = Date.now() - started;
        r.listen.captureSeen = now > r.listen.before;
        setTimeout(function () { media.playMusic(dir + "/en.wav"); }, 300);
    }
}, 100);
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


def device_clock(serial):
    """The device time in the format `logcat -T` takes."""
    return adb(serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")


def system_refusals(serial, since):
    """Activity starts of the plugin that the background activity start rules blocked since `since`, with the caller's
    process states (a refused microphone service already arrives as a `warning`; ColorOS drops the plugin's own log)."""
    log = adb(serial, "logcat", "-d", "-T", since, check=False).splitlines()
    blocked = [line for line in log if "Background activity launch blocked" in line and f"callingPackage: {PLUGIN};" in line]
    callers = sorted({part.split(":", 1)[1].strip() for line in blocked for part in line.split(";") if part.strip().startswith("callingUidProcState:")})
    return f"plugin activity starts blocked {len(blocked)} (caller {', '.join(callers) or '-'})"


def media_volume(serial):
    """(command, current, maximum) of the music stream, or None when the shell has no volume command."""
    for command in (["cmd", "media_session", "volume"], ["media", "volume"]):
        out = adb(serial, "shell", *command, "--stream", "3", "--get", check=False)
        match = re.search(r"volume is (\d+) in range \[(\d+)\.\.(\d+)\]", out)
        if match:
            return command, int(match.group(1)), int(match.group(3))
    return None


def set_music_volume(serial, command, index):
    """Sets the music volume index; HyperOS ignores `cmd media_session volume --set` from the shell, so `cmd audio
    set-volume` (API 30+) follows; whichever the device lacks fails silently."""
    adb(serial, "shell", *command, "--stream", "3", "--set", str(index), check=False)
    adb(serial, "shell", "cmd", "audio", "set-volume", "3", str(index), check=False)


def music_muted(serial):
    """True when the music stream is muted on its own (HyperOS keeps the mute flag when only the index changes)."""
    dump = adb(serial, "shell", "dumpsys", "audio", check=False)
    section = dump.split("- STREAM_MUSIC:", 1)
    return len(section) == 2 and re.search(r"^\s*Muted: true", section[1].split("- STREAM_", 1)[0], re.M) is not None


class Volume:
    """Raises the music volume to MACAW_VOLUME_PERCENT (40 by default) for a playback run when it is lower, unmutes a
    muted music stream (`cmd audio adj-unmute 3`, API 33+), then restores both."""

    def __init__(self, serial):
        self.serial = serial
        self.state = media_volume(serial)
        self.muted = music_muted(serial)
        self.percent = int(os.environ.get("MACAW_VOLUME_PERCENT", "40"))

    def __enter__(self):
        if self.muted:
            adb(self.serial, "shell", "cmd", "audio", "adj-unmute", "3", check=False)
        if self.state:
            command, current, maximum = self.state
            target = max(1, maximum * self.percent // 100)
            if current < target:
                set_music_volume(self.serial, command, target)
        return self

    def __exit__(self, *exc):
        if self.state:
            command, current, _ = self.state
            set_music_volume(self.serial, command, current)
        if self.muted:
            adb(self.serial, "shell", "cmd", "audio", "adj-mute", "3", check=False)

    def describe(self):
        if not self.state:
            return "volume unknown"
        _, current, maximum = self.state
        muted = ", muted stream unmuted for the run" if self.muted else ""
        return f"volume {current}/{maximum} (raised to {max(current, max(1, maximum * self.percent // 100))} for the run{muted})"


def launcher(serial):
    """AutoJs6's own launcher activity. A debug host with LeakCanary lists a second launcher entry, for which
    `resolve-activity` answers the system chooser (not exported on HyperOS, shown over the script on ColorOS)."""
    listed = adb(serial, "shell", "cmd", "package", "query-activities", "--brief", "-a", "android.intent.action.MAIN",
                 "-c", "android.intent.category.LAUNCHER", HOST)
    own = [line.strip() for line in listed.splitlines() if line.strip().startswith(HOST + "/") and "leakcanary" not in line]
    if own:
        return own[0]
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
    since = device_clock(serial)
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
    print(f"    system: {system_refusals(serial, since)}")
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


def cer(reference, hypothesis):
    """Character error rate over upper-case letters and digits."""
    def normalize(text):
        return "".join(ch for ch in text.upper() if ch.isalnum())
    r, h = normalize(reference), normalize(hypothesis or "")
    previous = list(range(len(h) + 1))
    for i, rc in enumerate(r, 1):
        current = [i] + [0] * len(h)
        for j, hc in enumerate(h, 1):
            current[j] = min(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + (rc != hc))
        previous = current
    return previous[-1] / max(1, len(r))


def tool_code(message):
    """The tool's error code in a `plugins.tools` error (`... failed with CODE: ...`), else the start of the host's message."""
    match = re.search(r"failed with ([A-Z_]+)", message or "")
    # The host's prefix ("Failed to call method "tools.call". ") follows the device language.
    return match.group(1) if match else re.sub(r'^[^"]*"tools\.call"[^.]*\.\s*', "", message or "").partition("\n")[0][:80]


def run_tools(serial):
    """The four tools through the host's `plugins.tools` (the path MCP Server and 3-Stove Agent take, roadmap P7.6.4)."""
    prepare(serial)
    since = device_clock(serial)
    values = {"sentence": TOOLS_SYNTHESIS_TEXT, "spoken": "This is a short test of the speech tools.", "listenTimeout": 60_000}
    if not start(serial, "tools", values):
        print(f"{label(serial)}: tools: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
        return False
    r = result(serial, "tools", 420)
    routes = [line.strip() for line in adb(serial, "logcat", "-d", "-v", "raw", "-T", since, "-s", "MacawTools:I", check=False).splitlines() if " route" in line]
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    if r is None:
        print(f"{label(serial)}: tools: FAIL no result")
        return False
    verdicts = []

    def item(name, ok, detail):
        verdicts.append(ok)
        print(f"    {name}: {'PASS' if ok else 'FAIL'} {detail}")

    def failed(name, step):
        if "error" not in step:
            return False
        item(name, False, f"{tool_code(step['error'])} ({step['ms']} ms)")
        return True

    print(f"{label(serial)}: tools")
    item("list", r["list"].get("names") == TOOLS_NAMES, r["list"].get("names") or tool_code(r["list"].get("error")))
    t = r["transcribe"]
    if not failed("transcribe", t):
        rate = cer(EN_REFERENCE, t["text"])
        item("transcribe", rate <= MAX_CER, f"en.wav cer {rate:.3f}, {t['segments']} segments, {t['durationMillis']} ms audio, {t['modelId']}, {t['ms']} ms")
    s = r["synthesize"]
    if not failed("synthesize", s):
        ok = s["mimeType"] == s["outputMime"] == "audio/mp4" and s["bytes"] == s["outputBytes"] == s["fileBytes"] > 0 and s["samePath"]
        item("synthesize", ok, f"m4a {s['mimeType']} / output {s['outputMime']}, {s['bytes']} / {s['outputBytes']} / {s['fileBytes']} bytes (result / output / file), "
                               f"same path {s['samePath']}, {s['durationMillis']} ms audio, {s['modelId']}, {s['ms']} ms")
    t = r["roundTrip"]
    if not failed("round trip", t):
        rate = cer(TOOLS_SYNTHESIS_TEXT, t["text"])
        item("round trip", rate <= MAX_ROUND_TRIP_CER, f"synth.m4a cer {rate:.3f}, {t['ms']} ms")
    s = r["speak"]
    if not failed("speak", s):
        item("speak", s["engine"] == "plugin" and s["completed"] is True and (s.get("durationMillis") or 0) > 0,
             f"engine {s['engine']}, completed {s['completed']}, {s.get('durationMillis')} ms spoken, {s['ms']} ms")
    s = r["refusals"]
    codes = {key: tool_code(s[key]) for key in ("unknown", "unreadable", "invalid")}
    item("refusals", codes["unknown"] != "accepted" and codes["unreadable"] == "INPUT_UNREADABLE" and codes["invalid"] != "accepted",
         f"unknown tool: {codes['unknown']}; text file: {codes['unreadable']}; format mp3: {codes['invalid']}")
    s = r["listen"]
    capture = f"recording {'seen' if s.get('captureSeen') else 'not seen'} after {s.get('captureMs')} ms"
    if "error" in s:
        item("listen", False, f"{tool_code(s['error'])}, {capture}, {s.get('ms')} ms")
    else:
        rate = cer(EN_REFERENCE, s.get("text"))
        item("listen", s.get("endReason") == "endpoint" and rate <= MAX_CER,
             f"en.wav aloud cer {rate:.3f}, end {s.get('endReason')}, {s.get('durationMillis')} ms heard, {capture}, {s.get('modelId')}, {s.get('ms')} ms")
    for line in routes:
        print(f"    MacawTools: {line[:300]}")
    return all(verdicts)


def run_speaker(serial):
    """speech.speaker and the speaker tools through AutoJs6 (roadmap P8.3.2, P8.3.5, P8.3.6)."""
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    for wav in sorted(SPEAKER_AUDIO.glob("libri-*.wav")):
        adb(serial, "push", str(wav), f"{REMOTE}/{wav.name}")
    if not start(serial, "speaker", {"nameA": SPEAKER_NAMES[0], "nameB": SPEAKER_NAMES[1]}):
        print(f"{label(serial)}: speaker: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
        return False
    r = result(serial, "speaker", 300)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    if r is None:
        print(f"{label(serial)}: speaker: FAIL no result")
        return False
    if not r.get("hasSpeaker"):
        print(f"{label(serial)}: speaker: BLOCKED this AutoJs6 build has no speech.speaker (needs a host with contract v2; features {r.get('features')})")
        return False
    print(f"{label(serial)}: speaker (features {r.get('features')})")
    verdicts = []

    def item(name, ok, detail):
        verdicts.append(ok)
        print(f"    {name}: {'PASS' if ok else 'FAIL'} {detail}")

    for key in ("enroll", "verify", "identify", "tools", "list", "remove", "async"):
        step = r.get(key, {"error": "not run"})
        if "error" in step:
            item(key, False, f"{step['error'][:160]} ({step.get('ms')} ms)")
            continue
        if key == "enroll":
            item(key, step["a"] == 3 and step["b"] == 3, f"samples {step['a']} / {step['b']}, {step['speechMs']} ms speech, {step['ms']} ms")
        elif key == "verify":
            trials = [step[k] for k in ("a1", "a2", "b1", "b2")]
            ok = all(t["sameOk"] and not t["otherOk"] for t in trials)
            scores = " ".join(f"{k} {step[k]['same']:.2f}/{step[k]['other']:.2f}" for k in ("a1", "a2", "b1", "b2"))
            item(key, ok, f"same/other {scores}, threshold {trials[0]['threshold']}, {step['ms']} ms")
        elif key == "identify":
            item(key, step["name"] == SPEAKER_NAMES[1] and step["matched"], f"{step['name']} matched {step['matched']}, {step['candidates']} candidates, {step['ms']} ms")
        elif key == "async":
            item(key, step["matched"] is True, f"verifyAsync matched {step['matched']}, {step['ms']} ms")
        elif key == "tools":
            item(key, step["verify"] is True and step["identify"] == SPEAKER_NAMES[0] and step["vectorFree"],
                 f"speech_speaker_verify {step['verify']}, speech_speaker_identify {step['identify']} ({step['candidates']} candidates), "
                 f"no vectors {step['vectorFree']}, {step['ms']} ms")
        elif key == "list":
            item(key, step["names"] == ",".join(SPEAKER_NAMES), step["names"])
        elif key == "remove":
            item(key, step["removed"] is True and "SPEAKER_NOT_FOUND" in step["refused"], f"removed {step['removed']}, then verify: {step['refused']}")
    return all(verdicts)


def turn_accuracy(turns):
    """The share of the true speech of two-speakers.wav given to the right reader, each found speaker mapped to the
    reader it overlaps most (as AnalysisDeviceTest)."""
    truth = json.loads((ANALYSIS_AUDIO / "two-speakers.json").read_text(encoding="utf-8"))["turns"]

    def overlap(a, b, c, d):
        return max(0, min(b, d) - max(a, c))

    mapping = {}
    for speaker in {t[2] for t in turns}:
        mine = [t for t in turns if t[2] == speaker]
        mapping[speaker] = max(("a", "b"), key=lambda reader: sum(overlap(m[0], m[1], x["startMillis"], x["endMillis"]) for m in mine for x in truth if x["speaker"] == reader))
    right = sum(overlap(m[0], m[1], x["startMillis"], x["endMillis"]) for m in turns for x in truth if mapping[m[2]] == x["speaker"])
    return mapping, right / sum(x["endMillis"] - x["startMillis"] for x in truth)


def run_analysis(serial):
    """speech.diarize / detectLanguage / tag and the analysis tools through AutoJs6 (roadmap P8.4.1, P8.4.4, P8.4.5)."""
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    for wav in sorted(ANALYSIS_AUDIO.glob("*.wav")) + sorted(SPEAKER_AUDIO.glob("libri-a-enroll-*.wav")) + [AUDIO / "en.wav"]:
        adb(serial, "push", str(wav), f"{REMOTE}/{wav.name}")
    if not start(serial, "analysis", {"nameA": ANALYSIS_NAME}):
        print(f"{label(serial)}: analysis: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
        return False
    r = result(serial, "analysis", 900)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    if r is None:
        print(f"{label(serial)}: analysis: FAIL no result")
        return False
    if not r.get("hasAnalysis"):
        print(f"{label(serial)}: analysis: BLOCKED this AutoJs6 build has no speech.diarize (needs a host with P8.4.1; features {r.get('features')})")
        return False
    print(f"{label(serial)}: analysis (features {r.get('features')})")
    verdicts = []

    def item(name, ok, detail):
        verdicts.append(ok)
        print(f"    {name}: {'PASS' if ok else 'FAIL'} {detail}")

    for key in ("diarize", "language", "tag", "tools", "async"):
        step = r.get(key, {"error": "not run"})
        if "error" in step:
            item(key, False, f"{step['error'][:160]} ({step.get('ms')} ms)")
            continue
        if key == "diarize":
            mapping, accuracy = turn_accuracy(step["turns"])
            named = {speaker: name for speaker, name in step["names"]}
            names_ok = all(named.get(speaker) == (ANALYSIS_NAME if reader == "a" else None) for speaker, reader in mapping.items())
            item(key, step["speakers"] == 2 and set(mapping.values()) == {"a", "b"} and accuracy >= 0.9 and names_ok and step["reports"] > 0,
                 f"{step['speakers']} speakers, {len(step['turns'])} turns, accuracy {accuracy:.3f}, identify {names_ok}, "
                 f"{step['reports']} progress reports, {step['ms']} ms")
        elif key == "language":
            expected = {"language-zh": "zh", "language-ru": "ru", "en": "en"}
            item(key, all(step.get(k) == v for k, v in expected.items()), " ".join(f"{k}={step.get(k)}" for k in expected) + f", {step['ms']} ms")
        elif key == "tag":
            found = {name: start for name, start in step["labels"]}
            item(key, found == ANALYSIS_TAGS and step["windows"] == 3, f"{step['labels']}, {step['windows']} windows, {step['ms']} ms")
        elif key == "tools":
            item(key, step["speakers"] == 2 and step["language"] == "ru" and step["top"] == "Music" and step["vectorFree"],
                 f"speech_diarize {step['speakers']}, speech_detect_language {step['language']}, speech_tag {step['top']}, "
                 f"no vectors {step['vectorFree']}, {step['ms']} ms")
        elif key == "async":
            item(key, step["top"] == "Music", f"tagAsync {step['top']}, {step['ms']} ms")
    return all(verdicts)


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 2
    action, serial = argv
    runs = {"api": run_api, "listen": run_listen, "late": lambda s: run_listen(s, late=True), "wake": run_wake, "kill": run_kill, "tools": run_tools, "speaker": run_speaker,
            "analysis": run_analysis}
    if action == "tools":
        os.environ.setdefault("MACAW_VOLUME_PERCENT", "70")
    if action in ("listen", "late", "wake", "tools"):
        with Volume(serial) as volume:
            print(f"{label(serial)}: media {volume.describe()}")
            return 0 if runs[action](serial) else 1
    if action not in runs:
        print(__doc__)
        return 2
    return 0 if runs[action](serial) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
