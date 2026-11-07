"""Release candidate checks that need the installed release build on one device (roadmap P9.5.4, P9.5.5, D119, D180).

    py .python/device_release_check.py snapshot <serial> <file> [--enroll] [--seed]
                                                                             what the installed build keeps across an update, to <file>
    py .python/device_release_check.py compare <serial> <file> [--remove]    the same after the update, against <file>
    py .python/device_release_check.py language <serial>                     speech.detectLanguage goes sentence by sentence
    py .python/device_release_check.py offer <serial> <archive> <modelId>    a version 1 model is offered its update
    py .python/device_release_check.py remove <serial> <modelId>...          removes models that a check installed
    py .python/device_release_check.py resident <serial>                     one resident wake word hit (AutoJs6 6.8.1+, speaker)

`snapshot` and `compare` cover the catalog version, the installed models with their sizes and the default models, and the
enrolled speakers, and since 1.6.0 (P11.5.2) the plugin's settings document as AutoJs6 exports it (1.5.0+, every
value must stay) and `speech.resident.get()` (6.8.1+, the 1.5.0 wake words must come back in order with the action
"open"). A device without any speaker gets `release-check-reader` from the LibriSpeech reader with `--enroll`
(it needs a host with contract v2 and the speaker model), so that the voiceprint store has something to keep; `compare`
verifies the same recording again and `--remove` then removes that one speaker. Other speakers are never touched.

`language` needs the langid model and contract v2. `offer` imports the mirror archive of version 1 (speech-models-v1, the
file comes from the caller) with speech.models.importFile, which leaves it a catalog model of version 1, and reads
updateAvailable and latestVersionCode. The home screen and the update itself are for the person at the phone: their
dialogs are not answered here.

Output: PASS / FAIL per item and the key numbers. A secure lock screen keeps activities from starting: unlock the device first.
"""
import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hs  # noqa: E402

READER = "release-check-reader"
MIXED = "language-mixed"
MIXED_LANGUAGES = {"zh", "en", "ru"}
MAX_SCORE_DRIFT = 0.01

STATE = hs.COMMON + """
function state() {
    var c = speech.capabilities();
    r.version = c.versionName + " (" + c.versionCode + ")";
    r.catalog = c.catalogVersion;
    r.microphone = c.microphoneGranted;
    r.models = speech.models.list({ installed: true }).map(function (m) {
        return { id: m.id, version: m.version, bytes: m.installedBytes, source: m.source, isDefault: !!m.isDefault };
    });
    r.hasSpeaker = typeof speech.speaker !== "undefined";
    r.speakers = r.hasSpeaker ? speech.speaker.list().map(function (s) { return { name: s.name, samples: s.samples, modelId: s.modelId }; }) : [];
    // The settings document of the plugin's settings transfer provider (1.5.0+), read the way AutoJs6 exports it.
    try {
        var host = context.getPackageName();
        var extras = new android.os.Bundle();
        extras.putInt("protocolVersion", 1);
        extras.putString("hostPackageName", host);
        extras.putLong("hostVersionCode", context.getPackageManager().getPackageInfo(host, 0).getLongVersionCode());
        extras.putString("purpose", "export");
        var uri = android.net.Uri.parse("content://%(package)s.autojs6.settings");
        var answer = context.getContentResolver().call(uri, "exportSettings", null, extras);
        r.settings = answer && answer.getString("payload") !== null ? String(answer.getString("payload")) : null;
    } catch (e) { r.settings = null; r.settingsError = String(e); }
    r.resident = null;
    if (typeof speech.resident !== "undefined" && typeof speech.resident.get === "function") {
        try {
            var got = speech.resident.get();
            r.resident = { enabled: !!got.enabled, rules: got.rules.map(function (x) { return { keyword: String(x.keyword), action: String(x.action) }; }) };
        } catch (e) { r.resident = { error: String(e) }; }
    }
}
"""

hs.SCRIPTS["release-state"] = STATE + """
try {
    if ("%(seed)s") {
        // A 1.5.0 settings document with custom wake words, imported the way AutoJs6 imports it, so that the update has
        // stored words to carry over (a phone on the default words has none).
        var seedHost = context.getPackageName();
        var seedExtras = new android.os.Bundle();
        seedExtras.putInt("protocolVersion", 1);
        seedExtras.putString("hostPackageName", seedHost);
        seedExtras.putLong("hostVersionCode", context.getPackageManager().getPackageInfo(seedHost, 0).getLongVersionCode());
        seedExtras.putString("purpose", "import");
        seedExtras.putString("payload", JSON.stringify({
            format: "autojs6-plugin-settings", formatVersion: 1, packageName: "%(package)s", versionCode: 177, versionName: "1.5.0",
            settings: { "resident-wake": { keywords: { t: "string", v: "%(seed)s".split("|").join("\\n") } } }
        }));
        var seeded = context.getContentResolver().call(android.net.Uri.parse("content://%(package)s.autojs6.settings"), "importSettings", null, seedExtras);
        r.seeded = seeded ? seeded.getInt("written", -1) : -1;
    }
    state();
    if (%(enroll)s && r.hasSpeaker) {
        if (!r.speakers.length) {
            var files = [1, 2, 3].map(function (i) { return dir + "/libri-a-enroll-" + i + ".wav"; });
            speech.speaker.enroll("%(reader)s", files, { replace: true });
            state();
        }
    }
    if (r.speakers.some(function (s) { return s.name === "%(reader)s"; })) {
        var v = speech.speaker.verify("%(reader)s", dir + "/libri-a-trial-1.wav");
        r.readerScore = v.score; r.readerMatched = v.matched;
        if (%(remove)s) r.readerRemoved = speech.speaker.remove("%(reader)s");
    }
    done();
} catch (e) { fail(e); }
"""

hs.SCRIPTS["release-language"] = hs.COMMON + """
r.hasAnalysis = typeof speech.detectLanguage === "function";
if (!r.hasAnalysis) { done(); exit(); }
try {
    var t = Date.now();
    var x = speech.detectLanguage(dir + "/%(mixed)s.wav");
    r.ms = Date.now() - t; r.language = x.language; r.modelId = x.modelId; r.durationMillis = x.durationMillis;
    r.segments = x.segments.map(function (s) { return { start: s.startMillis, end: s.endMillis, language: s.language }; });
    r.candidates = x.candidates.map(function (c) { return { language: c.language, score: c.score }; });
    done();
} catch (e) { fail(e); }
"""

hs.SCRIPTS["release-offer"] = hs.COMMON + """
function row() {
    return speech.models.list({ kind: "tts" }).filter(function (m) { return m.id === "%(model)s"; })[0];
}
try {
    r.before = (row() || {}).version;
    var m = speech.models.importFile(dir + "/model.tar", { acceptLicense: true });
    r.imported = m.id; r.importedVersion = m.version;
    var now = row();
    r.version = now.version; r.update = now.updateAvailable; r.latest = now.latestVersionCode; r.source = now.source;
    done();
} catch (e) { fail(e); }
"""


hs.SCRIPTS["release-remove"] = hs.COMMON + """
try {
    r.removed = [%(models)s].map(function (id) { return [id, speech.models.remove(id)]; });
    done();
} catch (e) { fail(e); }
"""


def fresh(serial):
    hs.adb(serial, "shell", "rm", "-rf", hs.REMOTE, check=False)
    hs.adb(serial, "shell", "mkdir", "-p", hs.REMOTE)


def run_script(serial, name, values, seconds):
    if not hs.start(serial, name, values):
        print(f"{hs.label(serial)}: {name}: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
        return None
    r = hs.result(serial, name, seconds)
    hs.adb(serial, "shell", "rm", "-rf", hs.REMOTE, check=False)
    if r is None:
        print(f"{hs.label(serial)}: {name}: FAIL no result")
    elif r["errors"]:
        print(f"{hs.label(serial)}: {name}: FAIL {' '.join(r['errors'])}")
        return None
    return r


def state(serial, enroll, remove, seed=""):
    fresh(serial)
    for wav in sorted(hs.SPEAKER_AUDIO.glob("libri-a-enroll-*.wav")) + [hs.SPEAKER_AUDIO / "libri-a-trial-1.wav"]:
        hs.adb(serial, "push", str(wav), f"{hs.REMOTE}/{wav.name}")
    values = {"enroll": "true" if enroll else "false", "remove": "true" if remove else "false", "reader": READER, "package": hs.PLUGIN, "seed": seed}
    return run_script(serial, "release-state", values, 360)


def flatten(value, prefix=""):
    """Path -> leaf value of a parsed settings document."""
    if isinstance(value, dict):
        out = {}
        for key, item in value.items():
            out.update(flatten(item, f"{prefix}/{key}"))
        return out
    if isinstance(value, list):
        out = {}
        for index, item in enumerate(value):
            out.update(flatten(item, f"{prefix}[{index}]"))
        return out
    return {prefix: value}


def resident_words(settings):
    """The wake words of the 1.5.0 `keywords` entry of a settings document (one per line), or None."""
    for path, value in flatten(json.loads(settings)).items():
        if path == "/settings/resident-wake/keywords/v" and isinstance(value, str):
            return [line.strip() for line in value.splitlines() if line.strip()]
    return None


def run_snapshot(serial, path, enroll, seed=""):
    r = state(serial, enroll, False, seed)
    if r is None:
        return False
    kept = {key: r.get(key) for key in ("version", "catalog", "microphone", "models", "hasSpeaker", "speakers", "readerScore", "readerMatched",
                                        "settings", "resident")}
    if r.get("settings") is None:
        print(f"{hs.label(serial)}: snapshot: no settings document ({r.get('settingsError', 'the plugin has no settings transfer provider')})")
    Path(path).write_text(json.dumps(kept, ensure_ascii=False, indent=1), encoding="utf-8")
    defaults = [m["id"] for m in r["models"] if m["isDefault"]]
    if seed:
        print(f"{hs.label(serial)}: snapshot: seeded {len(seed.split('|'))} wake words, {r.get('seeded')} written")
    print(f"{hs.label(serial)}: snapshot: {r['version']}, catalog {r['catalog']}, {len(r['models'])} models, {len(defaults)} defaults, "
          f"{len(r['speakers'])} speakers{' (' + READER + ' score %.3f)' % r['readerScore'] if 'readerScore' in r else ''}")
    return True


def run_compare(serial, path, remove):
    base = json.loads(Path(path).read_text(encoding="utf-8"))
    r = state(serial, False, remove)
    if r is None:
        return False
    print(f"{hs.label(serial)}: compare: {base['version']} -> {r['version']}")
    rows = [
        ("catalog", base["catalog"] == r["catalog"], f"{base['catalog']} -> {r['catalog']}"),
        ("microphone", base["microphone"] == r["microphone"], str(r["microphone"])),
        ("models", base["models"] == r["models"], f"{len(base['models'])} -> {len(r['models'])}, {sum(m['bytes'] or 0 for m in r['models'])} bytes"),
        ("speakers", base["speakers"] == r["speakers"], f"{len(base['speakers'])} -> {len(r['speakers'])}"),
    ]
    if "readerScore" in base:
        drift = abs(base["readerScore"] - r.get("readerScore", -9))
        rows.append(("voiceprint", drift <= MAX_SCORE_DRIFT and r.get("readerMatched") is True, f"score {base['readerScore']:.3f} -> {r.get('readerScore', -9):.3f}"))
    if remove and "readerRemoved" in r:
        rows.append((f"remove {READER}", r["readerRemoved"] is True, ""))
    if base.get("settings") and r.get("settings"):
        # Every value of the older document is still there after the update (the newer one may add entries, such as `rules`).
        before = flatten(json.loads(base["settings"])["settings"], "/settings")
        after = flatten(json.loads(r["settings"])["settings"], "/settings")
        changed = sorted(path for path, value in before.items() if after.get(path, object()) != value)
        rows.append(("settings", not changed, f"{len(before) - len(changed)} / {len(before)} values kept" + (f", changed {', '.join(changed)}" if changed else "")))
        words = resident_words(base["settings"])
        resident = r.get("resident")
        if words is not None and resident is not None:
            rules = resident.get("rules") or []
            kept_words = [rule["keyword"] for rule in rules]
            actions = sorted({rule["action"] for rule in rules})
            rows.append(("resident wake words", kept_words == words and actions in ([], ["open"]),
                         f"{len(words)} -> {len(kept_words)} words, actions {actions}, switch {'on' if resident.get('enabled') else 'off'}"))
    elif base.get("settings") is not None or r.get("settings") is not None:
        rows.append(("settings", False, f"document missing ({r.get('settingsError', 'before')})"))
    for name, passed, detail in rows:
        print(f"    {name}: {'PASS' if passed else 'FAIL'} {detail}")
    return all(row[1] for row in rows)


def run_language(serial):
    fresh(serial)
    hs.adb(serial, "push", str(hs.ANALYSIS_AUDIO / f"{MIXED}.wav"), f"{hs.REMOTE}/{MIXED}.wav")
    r = run_script(serial, "release-language", {"mixed": MIXED}, 300)
    if r is None:
        return False
    if not r["hasAnalysis"]:
        print(f"{hs.label(serial)}: language: BLOCKED this AutoJs6 build has no speech.detectLanguage")
        return False
    heard = [s["language"] for s in r["segments"]]
    share = abs(sum(c["score"] for c in r["candidates"]) - 1) < 0.01
    ok = MIXED_LANGUAGES <= set(heard) and share and len(heard) >= len(MIXED_LANGUAGES)
    print(f"{hs.label(serial)}: language: {'PASS' if ok else 'FAIL'} {', '.join(heard)} in {r['ms']} ms ({r['durationMillis']} ms of audio), "
          f"overall {r['language']}, {r['modelId']}")
    return ok


def run_offer(serial, archive, model):
    fresh(serial)
    hs.adb(serial, "push", str(archive), f"{hs.REMOTE}/model.tar")
    r = run_script(serial, "release-offer", {"model": model}, 300)
    if r is None:
        return False
    ok = r["imported"] == model and r["version"] == 1 and r["update"] is True and r["latest"] == 2 and r["source"] == "catalog"
    print(f"{hs.label(serial)}: offer: {'PASS' if ok else 'FAIL'} {model} imported as v{r['version']} ({r['source']}), updateAvailable {r['update']}, latestVersionCode {r['latest']}")
    return ok


# 1.5.0 wake words for `snapshot --seed` (kept in the source: non-ASCII arguments do not survive Git Bash on Windows).
SEED_WORDS = "小鹦小鹦|Hey Macaw|开始打卡"
RESIDENT_KEYWORD = "朱丽楠"

hs.SCRIPTS["release-resident"] = hs.COMMON + """
if (typeof speech.resident === "undefined" || typeof speech.resident.get !== "function") { r.blocked = "no speech.resident"; done(); exit(); }
var original = null, wasOn = false, restored = false, finished = false, hit = null, started = 0;
function restore() {
    if (original === null || restored) return;
    restored = true;
    try { speech.resident.set({ rules: original }); } catch (e) { r.errors.push("restore rules " + e.code); }
    try { if (!wasOn) speech.resident.disable(); } catch (e) { r.errors.push("restore switch " + e.code); }
    try {
        var back = speech.resident.get();
        r.restoredRules = JSON.stringify(back.rules.map(function (x) { return String(x.keyword) + "/" + String(x.action); })) ===
            JSON.stringify(original.map(function (x) { return x.keyword + "/" + x.action; }));
        r.restoredOn = !!back.enabled;
    } catch (e) { r.errors.push("read back " + e.code); }
}
function finish() {
    if (finished) return;
    finished = true;
    try { media.stopMusic(); } catch (e) {}
    try { speech.resident.off("hit"); } catch (e) {}
    r.hit = hit;
    restore();
    done();
    exit();
}
try {
    var got = speech.resident.get();
    wasOn = !!got.enabled;
    original = got.rules.map(function (x) {
        var o = { keyword: String(x.keyword), action: String(x.action) };
        if (x.script) o.script = String(x.script);
        if (x.arguments) o.arguments = String(x.arguments);
        return o;
    });
    r.before = { on: wasOn, rules: original.length };
    var rules = original.filter(function (x) { return x.keyword !== "%(keyword)s"; }).slice(0, 9).concat([{ keyword: "%(keyword)s", action: "notify" }]);
    speech.resident.set({ rules: rules });
    speech.resident.on("hit", function (h) {
        if (hit === null && String(h.keyword) === "%(keyword)s") { hit = { afterMs: Date.now() - started, action: String(h.action) }; finish(); }
    });
    var sw = speech.resident.enable();
    r.enable = { started: !!sw.started, restartNeeded: !!sw.restartNeeded };
    setTimeout(function () { started = Date.now(); media.playMusic(dir + "/wake.wav", 1, false); }, 4000);
    setTimeout(finish, 22000);
} catch (e) { fail(e); restore(); done(); exit(); }
"""


def run_resident(serial):
    """One resident wake word hit on the installed build: a test word with the action "notify" is added for the run, the
    keyword recording plays through the speaker, `on('hit')` must receive it; the rules and the switch are put back."""
    os.environ.setdefault("MACAW_VOLUME_PERCENT", "70")
    fresh(serial)
    hs.adb(serial, "push", str(hs.AUDIO / "kws-zh-6.wav"), f"{hs.REMOTE}/wake.wav")
    with hs.Volume(serial):
        r = run_script(serial, "release-resident", {"keyword": RESIDENT_KEYWORD}, 60)
    if r is None:
        return False
    if r.get("blocked"):
        print(f"{hs.label(serial)}: resident: BLOCKED {r['blocked']}")
        return False
    hit = r.get("hit")
    restored = r.get("restoredRules") is True and r.get("restoredOn") == r["before"]["on"]
    ok = bool(hit) and restored
    print(f"{hs.label(serial)}: resident: {'PASS' if ok else 'FAIL'} "
          f"{'hit after %d ms (action %s)' % (hit['afterMs'], hit['action']) if hit else 'no hit'}, enable {r.get('enable')}, "
          f"{r['before']['rules']} rules and switch {'on' if r['before']['on'] else 'off'} {'restored' if restored else 'NOT restored'}")
    return ok


def run_remove(serial, models):
    fresh(serial)
    r = run_script(serial, "release-remove", {"models": ",".join(json.dumps(m) for m in models)}, 60)
    if r is None:
        return False
    print(f"{hs.label(serial)}: remove: {r['removed']}")
    return True


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    snapshot = commands.add_parser("snapshot")
    snapshot.add_argument("serial")
    snapshot.add_argument("file")
    snapshot.add_argument("--enroll", action="store_true")
    snapshot.add_argument("--seed", action="store_true", help=f"store the wake words {SEED_WORDS!r} first through the settings import")
    compare = commands.add_parser("compare")
    compare.add_argument("serial")
    compare.add_argument("file")
    compare.add_argument("--remove", action="store_true")
    language = commands.add_parser("language")
    language.add_argument("serial")
    offer = commands.add_parser("offer")
    offer.add_argument("serial")
    offer.add_argument("archive")
    offer.add_argument("model")
    resident = commands.add_parser("resident")
    resident.add_argument("serial")
    remove = commands.add_parser("remove")
    remove.add_argument("serial")
    remove.add_argument("models", nargs="+")
    args = parser.parse_args(argv)
    if args.command == "snapshot":
        return 0 if run_snapshot(args.serial, args.file, args.enroll, SEED_WORDS if args.seed else "") else 1
    if args.command == "compare":
        return 0 if run_compare(args.serial, args.file, args.remove) else 1
    if args.command == "language":
        return 0 if run_language(args.serial) else 1
    if args.command == "resident":
        return 0 if run_resident(args.serial) else 1
    if args.command == "remove":
        return 0 if run_remove(args.serial, args.models) else 1
    return 0 if run_offer(args.serial, args.archive, args.model) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
