"""AutoJs6 scripts against the 3-Macaw Speech system TTS engine and synthesis on one device (roadmap P4.3, P4.4).

    py .python/device_tts_run.py host <serial> [model]       speech.synthesize + media, tts.engines / ready / voices,
                                                            tts(text, {engine}), tts.synthesize(text, path, {engine}),
                                                            speech.speak, speech.synthesizeAsync (model defaults to the
                                                            AISHELL-3 voices)
    py .python/device_tts_run.py fallback <serial>          uninstalls the plugin keeping its data (pm uninstall -k), runs
                                                            the host tts without it, then reinstalls the debug build

Scripts start through AutoJs6's RunIntentActivity like `device_host_scripts.py` and write a JSON result next to
themselves in shared storage. `host` plays two short phrases through the speaker (tts and speech.speak) and one WAV
through `media` at the current media volume. Output: one line per item, no audio content.
A secure lock screen keeps activities from starting: unlock the device first. Nothing here answers a system dialog.
"""
import json
import subprocess
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hs  # noqa: E402

REMOTE = "/sdcard/Download/autojs6-macaw-tts"
PLUGIN = hs.PLUGIN
AISHELL = "tts.vits.icefall.zh-aishell3"
APK_DIR = hs.ROOT / "app" / "build" / "outputs" / "apk"

COMMON = """
var out = "%(out)s";
var dir = "%(dir)s";
var pkg = "%(pkg)s";
var r = { errors: [], steps: {} };
files.write(out + ".started", String(Date.now()));
function done() { files.write(out, JSON.stringify(r)); }
function attempt(name, fn) {
    try { r.steps[name] = fn(); } catch (e) { r.steps[name] = { error: String(e.code || "") + " " + String(e.message || e) }; }
}
function settle(name, value) {
    return Promise.resolve(value).then(function (v) { r.steps[name] = v; }, function (e) { r.steps[name] = { error: String(e.code || "") + " " + String(e.message || e) }; });
}
"""

SCRIPTS = {
    "host": COMMON + """
attempt("capabilities", function () { var c = speech.capabilities(); return { features: c.features.join(","), ttsEnginePackage: c.ttsEnginePackage }; });
attempt("synthesize", function () {
    var t = Date.now();
    var x = speech.synthesize("%(zh)s", dir + "/speech.wav", { model: "%(model)s", speaker: 10 });
    return { ms: Date.now() - t, sampleRate: x.sampleRate, durationMillis: x.durationMillis, modelId: x.modelId, speakerId: x.speakerId, sizeBytes: x.sizeBytes, exists: files.exists(x.path) };
});
attempt("media", function () {
    media.playMusic(dir + "/speech.wav", 0.6, false);
    sleep(400);
    var info = { duration: media.getMusicDuration(), playing: media.isMusicPlaying() };
    sleep(1200);
    info.position = media.getMusicCurrentPosition();
    media.stopMusic();
    return info;
});
settle("engines", tts.engines().then(function (list) { return list.map(function (e) { return e.name + (e["default"] ? " (default)" : ""); }); }))
    .then(function () { return settle("ready", tts.ready(pkg)); })
    .then(function () {
        return settle("voices", tts.voices({ engine: pkg }).then(function (voices) {
            return { count: voices.length, model: voices.filter(function (v) { return v.name.indexOf("%(model)s/") === 0; }).length,
                first: voices.slice(0, 2).map(function (v) { return v.name + " " + v.locale + " q" + v.quality + (v.offline ? " offline" : ""); }) };
        }));
    })
    .then(function () { return settle("languageZh", tts.languageAvailability("zh-CN", pkg)); })
    .then(function () { return settle("tts", tts("%(hello)s", { engine: pkg })); })
    .then(function () { return settle("ttsVoice", tts("%(hello)s", { engine: pkg, voice: "%(model)s/20" })); })
    .then(function () {
        return settle("ttsSynthesize", Promise.resolve(tts.synthesize("%(zh)s", dir + "/tts.wav", { engine: pkg })).then(function (x) {
            return { status: x.status, engine: x.engine, voice: x.voice, bytes: files.exists(dir + "/tts.wav") ? new java.io.File(dir + "/tts.wav").length() : -1 };
        }));
    })
    .then(function () { return settle("speak", speech.speak("%(done)s", { model: "%(model)s", speaker: 3 })); })
    .then(function () {
        return settle("synthesizeAsync", speech.synthesizeAsync("%(zh)s", dir + "/async.wav", { model: "%(model)s", speed: 1.5 }).then(function (x) {
            return { sampleRate: x.sampleRate, durationMillis: x.durationMillis, speakerId: x.speakerId, exists: files.exists(x.path), noPartFile: files.listDir(dir).every(function (n) { return n.indexOf(".part") < 0; }) };
        }));
    })
    .then(function () { done(); }, function (e) { r.errors.push(String(e.code || "") + " " + String(e.message || e)); done(); });
""",
    "fallback": COMMON + """
attempt("available", function () { return speech.isAvailable(); });
settle("engines", tts.engines().then(function (list) { return list.map(function (e) { return e.name + (e["default"] ? " (default)" : ""); }); }))
    .then(function () { return settle("ttsDefault", tts.synthesize("hello", dir + "/default.wav")); })
    .then(function () { return settle("ttsPlugin", tts.synthesize("hello", dir + "/plugin.wav", { engine: pkg })); })
    .then(function () { return settle("speak", speech.speak("hello")); })
    .then(function () { attempt("synthesize", function () { return speech.synthesize("hello", dir + "/speech.wav"); }); done(); },
        function (e) { r.errors.push(String(e.code || "") + " " + String(e.message || e)); done(); });
""",
}

# "Hello", "Hello, this is a test of offline speech synthesis." and "The task is done." (written with escapes: the script
# is ASCII, AutoJs6 reads the escapes).
HELLO = "\\u4f60\\u597d"
ZH = "\\u4f60\\u597d, \\u8fd9\\u662f\\u79bb\\u7ebf\\u8bed\\u97f3\\u5408\\u6210\\u7684\\u6d4b\\u8bd5."
DONE = "\\u4efb\\u52a1\\u5df2\\u5b8c\\u6210"


def start(serial, name, values):
    script = SCRIPTS[name] % {"out": f"{REMOTE}/{name}.json", "dir": REMOTE, "pkg": PLUGIN, "hello": HELLO, "zh": ZH, "done": DONE, **values}
    local = Path(tempfile.gettempdir()) / f"macaw-tts-{name}-{serial.replace(':', '_')}.js"
    local.write_text(script, encoding="utf-8")
    hs.adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    hs.adb(serial, "shell", "mkdir", "-p", REMOTE)
    hs.adb(serial, "push", str(local), f"{REMOTE}/{name}.js")
    hs.adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    hs.adb(serial, "shell", "am", "start", "-n", hs.launcher(serial))
    time.sleep(2)
    hs.adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
           "-d", f"file://{REMOTE}/{name}.js", "-n", f"{hs.HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    for _ in range(10):
        time.sleep(1)
        if hs.adb(serial, "shell", "ls", f"{REMOTE}/{name}.json.started", check=False).endswith(".started"):
            return True
    return False


def result(serial, name, seconds):
    deadline = time.time() + seconds
    while time.time() < deadline:
        raw = hs.adb(serial, "shell", "cat", f"{REMOTE}/{name}.json", check=False)
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(2)
    return None


def failed(value):
    return isinstance(value, dict) and "error" in value


def show(serial, name, r):
    for key, value in r["steps"].items():
        print(f"    {key}: {json.dumps(value, ensure_ascii=True)[:400]}")
    if r["errors"]:
        print(f"    errors: {r['errors']}")


def run_host(serial, model):
    with hs.Volume(serial) as volume:
        if not start(serial, "host", {"model": model}):
            print(f"{hs.label(serial)}: host: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
            return False
        r = result(serial, "host", 300)
    if r is None:
        print(f"{hs.label(serial)}: host: FAIL no result")
        return False
    steps = r["steps"]
    engines = steps.get("engines") or []
    voices = steps.get("voices") or {}
    ok = (
        not r["errors"]
        and not any(failed(v) for v in steps.values())
        and any(str(e).startswith(PLUGIN) for e in engines)
        and voices.get("model", 0) > 0
        and steps.get("synthesize", {}).get("exists") is True
        and steps.get("media", {}).get("duration", 0) > 0
        and steps.get("ttsSynthesize", {}).get("bytes", 0) > 44
        and steps.get("synthesizeAsync", {}).get("exists") is True
    )
    print(f"{hs.label(serial)}: host: {'PASS' if ok else 'FAIL'} ({volume.describe()})")
    show(serial, "host", r)
    hs.adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    return ok


def run_fallback(serial):
    abi = hs.adb(serial, "shell", "getprop", "ro.product.cpu.abi")
    apk = next((APK_DIR / "debug").glob(f"*-{abi}.apk"), None) or next((APK_DIR / "debug").glob("*-universal.apk"))
    removed = hs.adb(serial, "shell", "pm", "uninstall", "-k", PLUGIN, check=False)
    print(f"{hs.label(serial)}: pm uninstall -k: {removed}")
    try:
        if not start(serial, "fallback", {}):
            print(f"{hs.label(serial)}: fallback: BLOCKED the script did not start")
            return False
        r = result(serial, "fallback", 180)
    finally:
        out = subprocess.run(["adb", "-s", serial, "install", "-r", str(apk)], capture_output=True, text=True, encoding="utf-8", errors="replace")
        print(f"{hs.label(serial)}: reinstall {apk.name}: {'OK' if 'Success' in out.stdout else (out.stdout + out.stderr).strip()[-200:]}")
    if r is None:
        print(f"{hs.label(serial)}: fallback: FAIL no result")
        return False
    steps = r["steps"]
    engines = steps.get("engines") or []
    ok = (
        not r["errors"]
        and steps.get("available") is False
        and not any(str(e).startswith(PLUGIN) for e in engines)
        and not failed(steps.get("ttsDefault"))
        and failed(steps.get("ttsPlugin"))
    )
    print(f"{hs.label(serial)}: fallback: {'PASS' if ok else 'FAIL'}")
    show(serial, "fallback", r)
    hs.adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    return ok


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    action, serial = argv[0], argv[1]
    if action == "host":
        return 0 if run_host(serial, argv[2] if len(argv) > 2 else AISHELL) else 1
    if action == "fallback":
        return 0 if run_fallback(serial) else 1
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
