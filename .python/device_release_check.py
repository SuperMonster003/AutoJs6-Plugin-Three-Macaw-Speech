"""Release candidate checks that need the installed release build on one device (roadmap P9.5.4, P9.5.5, D119, D180).

    py .python/device_release_check.py snapshot <serial> <file> [--enroll]   what the installed build keeps across an update, to <file>
    py .python/device_release_check.py compare <serial> <file> [--remove]    the same after the update, against <file>
    py .python/device_release_check.py language <serial>                     speech.detectLanguage goes sentence by sentence
    py .python/device_release_check.py offer <serial> <archive> <modelId>    a version 1 model is offered its update

`snapshot` and `compare` cover the catalog version, the installed models with their sizes and the default models, and the
enrolled speakers. A device without any speaker gets `release-check-reader` from the LibriSpeech reader with `--enroll`
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
}
"""

hs.SCRIPTS["release-state"] = STATE + """
try {
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


def state(serial, enroll, remove):
    fresh(serial)
    for wav in sorted(hs.SPEAKER_AUDIO.glob("libri-a-enroll-*.wav")) + [hs.SPEAKER_AUDIO / "libri-a-trial-1.wav"]:
        hs.adb(serial, "push", str(wav), f"{hs.REMOTE}/{wav.name}")
    values = {"enroll": "true" if enroll else "false", "remove": "true" if remove else "false", "reader": READER}
    return run_script(serial, "release-state", values, 360)


def run_snapshot(serial, path, enroll):
    r = state(serial, enroll, False)
    if r is None:
        return False
    kept = {key: r.get(key) for key in ("version", "catalog", "microphone", "models", "hasSpeaker", "speakers", "readerScore", "readerMatched")}
    Path(path).write_text(json.dumps(kept, ensure_ascii=False, indent=1), encoding="utf-8")
    defaults = [m["id"] for m in r["models"] if m["isDefault"]]
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


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    snapshot = commands.add_parser("snapshot")
    snapshot.add_argument("serial")
    snapshot.add_argument("file")
    snapshot.add_argument("--enroll", action="store_true")
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
    args = parser.parse_args(argv)
    if args.command == "snapshot":
        return 0 if run_snapshot(args.serial, args.file, args.enroll) else 1
    if args.command == "compare":
        return 0 if run_compare(args.serial, args.file, args.remove) else 1
    if args.command == "language":
        return 0 if run_language(args.serial) else 1
    return 0 if run_offer(args.serial, args.archive, args.model) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
