"""Release gate R3 smoke run of 3-Macaw Speech on one device (roadmap R3, P6.3 appendix F, P6.4 E5 candidate).

    py .python/device_smoke.py <serial> [--apk <file>] [--asr <id>] [--tts <id>] [--no-install]

1. install: `adb install -r` of the APK that matches the device ABI, by default the newest set of the current version in
   releases/ (see release_sums.py; release and debug builds share the signing key, so the update keeps data and models).
   ColorOS and MIUI / HyperOS show a confirmation screen for USB installs: the maintainer confirms it on the phone.
2. activation: an AutoJs6 script (RunIntentActivity, like device_host_scripts.py) answers speech.isAvailable() and
   speech.capabilities() (version, features, catalog, microphone permission).
3. download: removes and installs one recognition and one synthesis model through speech.models (defaults: the
   English streaming recognizer, 67 MB, and AISHELL-3, 31 MB), then restores the default models of both kinds.
4. speech.synthesize once, tts(text, {engine}) once, speech.listen once (the downloaded recognizer; en.wav played
   through the speaker, CER against its LibriSpeech transcript) and speech.wake once (keyword 朱丽楠, kws-zh-6 played).

The music volume is raised to MACAW_VOLUME_PERCENT (70 by default) for the run and restored. Acoustic runs must not
overlap on adjacent phones. Output: PASS / FAIL per step and the key numbers; no recognized text is printed.
A secure lock screen keeps activities from starting: unlock the device first. Nothing here answers a system dialog.
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hs  # noqa: E402
import release_sums  # noqa: E402
from device_stove_voice import REFERENCE, cer  # noqa: E402

PLUGIN = hs.PLUGIN
HOST = hs.HOST
REQUIRED_HOST = 5456
ASR = "asr.zipformer-streaming.en.2023-06-26.int8"
TTS = "tts.vits.icefall.zh-aishell3"
MAX_CER = 0.15

SCRIPT = hs.COMMON + """
r.steps = {};
function attempt(name, fn) {
    try { r.steps[name] = fn(); } catch (e) { r.steps[name] = { error: String(e.code || "") + " " + String(e.message || e) }; }
}
function failed(name) { return !r.steps[name] || r.steps[name].error; }
attempt("activation", function () {
    var c = speech.capabilities();
    return { available: speech.isAvailable(), version: c.versionName + " (" + c.versionCode + ")", features: c.features.join(","),
             catalog: c.catalogVersion, microphone: c.microphoneGranted };
});
var defaults = {};
attempt("defaults", function () {
    defaults.asr = (speech.models.getDefault("asr") || {}).id || null;
    defaults.tts = (speech.models.getDefault("tts") || {}).id || null;
    return defaults;
});
function download(id) {
    var present = speech.models.list({ installed: true }).filter(function (m) { return m.id === id; }).length > 0;
    if (present) speech.models.remove(id);
    var t = Date.now(), done = 0, total = 0;
    var model = speech.models.install(id, { acceptLicense: true, onProgress: function (d, all) { done = d; total = all; } });
    return { ms: Date.now() - t, done: done, total: total, installed: !!(model && model.installed), installedBytes: model && model.installedBytes, removedFirst: present };
}
attempt("downloadAsr", function () { return download("%(asr)s"); });
attempt("downloadTts", function () { return download("%(tts)s"); });
attempt("restoreDefaults", function () {
    var restored = [];
    ["asr", "tts"].forEach(function (kind) {
        var now = (speech.models.getDefault(kind) || {}).id || null;
        if (defaults[kind] && now !== defaults[kind]) { speech.models.setDefault(kind, defaults[kind]); restored.push(kind); }
    });
    return { restored: restored.join(",") || "-" };
});
attempt("synthesize", function () {
    var t = Date.now();
    var x = speech.synthesize("这是 3-Macaw Speech 的发布前冒烟测试.", dir + "/smoke.wav", { model: "%(tts)s" });
    return { ms: Date.now() - t, durationMillis: x.durationMillis, sampleRate: x.sampleRate, sizeBytes: x.sizeBytes, modelId: x.modelId, exists: files.exists(x.path) };
});
function step(name, fn) {
    return new Promise(function (resolve) {
        try { fn(resolve); } catch (e) { r.steps[name] = { error: String(e.code || "") + " " + String(e.message || e) }; resolve(); }
    });
}
step("tts", function (resolve) {
    var t = Date.now();
    tts("你好, 这是系统朗读引擎.", { engine: "%(pkg)s" }).then(function (v) {
        r.steps.tts = { ms: Date.now() - t, voice: v.voice, locale: String(v.locale) }; resolve();
    }, function (e) { r.steps.tts = { error: String(e.code || "") + " " + e.message }; resolve(); });
}).then(function () {
    return step("listen", function (resolve) {
        var started = 0, text = "", firstPartial = -1;
        var s = speech.listen({ model: "%(asr)s", timeoutMillis: 5000, maxDurationMillis: 30000 });
        s.on("ready", function () { started = Date.now(); media.playMusic(dir + "/en.wav", 1, false); });
        s.on("partial", function () { if (firstPartial < 0) firstPartial = Date.now() - started; });
        s.on("final", function (f) { text += (text ? " " : "") + f.text; });
        s.on("error", function (e) { r.errors.push("listen " + e.code + " " + e.message); });
        s.on("end", function (e) {
            media.stopMusic();
            r.steps.listen = { reason: e.reason, text: text, firstPartialMs: firstPartial, modelId: s.modelId };
            resolve();
        });
    });
}).then(function () {
    return step("wake", function (resolve) {
        var started = 0;
        var w = speech.wake(["%(keyword)s"]);
        w.on("ready", function () { started = Date.now(); media.playMusic(dir + "/wake.wav", 1, false); });
        w.on("wake", function (e) {
            if (!r.steps.wake) r.steps.wake = { keyword: e.keyword, afterMs: Date.now() - started, score: e.score };
            w.stop();
        });
        w.on("error", function (e) { r.errors.push("wake " + e.code + " " + e.message); });
        w.on("end", function (e) {
            media.stopMusic();
            if (!r.steps.wake) r.steps.wake = { error: "no hit, end " + e.reason };
            resolve();
        });
        setTimeout(function () { if (w.isActive()) w.stop(); }, 25000);
    });
}).then(done, function (e) { fail(e); });
"""


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def pick_apk(serial, given):
    if given:
        return Path(given)
    abi = hs.adb(serial, "shell", "getprop", "ro.product.cpu.abi")
    apks = release_sums.newest_apks(release_sums.version_name())
    for apk in apks:
        if f"-{abi}-" in apk.name:
            return apk
    return next(apk for apk in apks if "-universal-" in apk.name)


def installed_version(serial, package):
    dump = hs.adb(serial, "shell", "dumpsys", "package", package, check=False)
    match = re.search(r"versionCode=(\d+)", dump)
    return int(match.group(1)) if match else None


def install(serial, apk):
    started = time.time()
    try:
        out = subprocess.run(["adb", "-s", serial, "install", "-r", str(apk)], capture_output=True, text=True, timeout=300)
        ok = out.returncode == 0 and "Success" in out.stdout
        detail = (out.stdout + out.stderr).strip().splitlines()[-1] if not ok else "Success"
    except subprocess.TimeoutExpired:
        ok, detail = False, "timed out (a confirmation screen on the phone?)"
    return ok, detail, time.time() - started


def run(serial, args):
    label = hs.label(serial)
    ok = True
    host = installed_version(serial, HOST)
    print(f"{label}: AutoJs6 versionCode {host} ({'ok' if host and host >= REQUIRED_HOST else 'below ' + str(REQUIRED_HOST)})")
    if not host or host < REQUIRED_HOST:
        return False
    apk = pick_apk(serial, args.apk)
    digest = sha256(apk)
    if args.no_install:
        print(f"    install: SKIPPED (--no-install), installed versionCode {installed_version(serial, PLUGIN)}")
    else:
        passed, detail, seconds = install(serial, apk)
        ok = ok and passed
        print(f"    install: {'PASS' if passed else 'FAIL'} {apk.name} (SHA-256 {digest[:16]}...) in {seconds:.1f} s, versionCode "
              f"{installed_version(serial, PLUGIN)}; {detail}")
        if not passed:
            return False
    hs.prepare(serial)
    values = {"out": f"{hs.REMOTE}/smoke.json", "dir": hs.REMOTE, "asr": args.asr, "tts": args.tts, "pkg": PLUGIN, "keyword": "朱丽楠"}
    script = SCRIPT % values
    local = Path(os.environ.get("TEMP", ".")) / f"macaw-smoke-{serial.replace(':', '_')}.js"
    local.write_text(script, encoding="utf-8")
    hs.adb(serial, "push", str(local), f"{hs.REMOTE}/smoke.js")
    hs.adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    hs.adb(serial, "shell", "am", "start", "-n", hs.launcher(serial))
    time.sleep(2)
    hs.adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
           "-d", f"file://{hs.REMOTE}/smoke.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    with hs.Volume(serial) as volume:
        print(f"    media {volume.describe()}")
        r = hs.result(serial, "smoke", 600)
    hs.adb(serial, "shell", "rm", "-rf", hs.REMOTE, check=False)
    local.unlink(missing_ok=True)
    if r is None:
        print("    script: FAIL no result (unlock the device; AutoJs6 needs all-files access)")
        return False
    steps = r.get("steps", {})

    def report(name, passed, text):
        nonlocal ok
        ok = ok and passed
        print(f"    {name}: {'PASS' if passed else 'FAIL'} {text}")

    a = steps.get("activation", {})
    report("activation", bool(a.get("available")) and not a.get("error"),
           f"version {a.get('version')} features {a.get('features')} catalog {a.get('catalog')} microphone {a.get('microphone')} {a.get('error') or ''}")
    for key, model in (("downloadAsr", args.asr), ("downloadTts", args.tts)):
        d = steps.get(key, {})
        report(key, bool(d.get("installed")) and not d.get("error"),
               f"{model}: {d.get('done')} / {d.get('total')} bytes in {(d.get('ms') or 0) / 1000:.1f} s, installed {d.get('installedBytes')} bytes, "
               f"removed first {d.get('removedFirst')} {d.get('error') or ''}")
    report("restoreDefaults", not steps.get("restoreDefaults", {}).get("error"),
           f"restored {steps.get('restoreDefaults', {}).get('restored')} (defaults were {steps.get('defaults')})")
    s = steps.get("synthesize", {})
    report("speech.synthesize", bool(s.get("exists")) and (s.get("durationMillis") or 0) > 0,
           f"{s.get('modelId')}: {s.get('durationMillis')} ms audio, {s.get('sampleRate')} Hz, {s.get('sizeBytes')} bytes in {s.get('ms')} ms {s.get('error') or ''}")
    t = steps.get("tts", {})
    report("tts(text, {engine})", bool(t.get("voice")) and not t.get("error"), f"voice {t.get('voice')} ({t.get('locale')}) in {t.get('ms')} ms {t.get('error') or ''}")
    listen = steps.get("listen", {})
    score = cer(REFERENCE, listen.get("text") or "") if "text" in listen else 1.0
    report("speech.listen", "text" in listen and score <= MAX_CER,
           f"{listen.get('modelId')}: {len(listen.get('text') or '')} chars, CER {score:.3f}, first partial {listen.get('firstPartialMs')} ms, end {listen.get('reason')} {listen.get('error') or ''}")
    w = steps.get("wake", {})
    report("speech.wake", bool(w.get("keyword")) and not w.get("error"),
           f"hit {w.get('afterMs')} ms after ready, score {w.get('score')} {w.get('error') or ''}")
    if r.get("errors"):
        ok = False
        print(f"    errors: {[e[:160] for e in r['errors']]}")
    print(f"{label}: SMOKE {'PASS' if ok else 'FAIL'} ({apk.name}, models {args.asr}, {args.tts})")
    return ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--apk", help="APK to install (default: the newest release set, by the device ABI)")
    parser.add_argument("--asr", default=ASR)
    parser.add_argument("--tts", default=TTS)
    parser.add_argument("--no-install", action="store_true", help="keep the installed build")
    arguments = parser.parse_args(argv)
    os.environ.setdefault("MACAW_VOLUME_PERCENT", "70")
    return 0 if run(arguments.serial, arguments) else 1


if __name__ == "__main__":
    sys.exit(main())
