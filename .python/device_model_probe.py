"""Candidate model probe of roadmap P8.2.1 (D124) for 3-Macaw Speech: measures a sherpa-onnx model on a device before it
enters the catalog, through ModelProbeDeviceTest (peak resident memory, PSS, load time, first-answer latency, and the
character error rate or the audio length).

    py -I .python/device_model_probe.py list
    py -I .python/device_model_probe.py probe <serial> <preset> --archives <dir> [--wav <file>] [--ref <text>]
                                          [--text <text>] [--sid <n>] [--lang <code>] [--out <name>] [--head <chars>]
    py -I .python/device_model_probe.py clean <serial>

`--archives` is a directory outside the repository with the pinned upstream assets as `<tag>/<asset>` (the layout of
`prepare_sources.py --cache-dir`); each archive is checked against the size and SHA-256 in PRESETS, unpacked with the
`data` filter into `<archives>/probe-unpacked/` and pushed to `/data/local/tmp/macaw-probe/<preset>` (kept for the
next run; `clean` removes it and the plugin's probe outputs). `--wav` names a file inside the unpacked model, a local
file (pushed), or `cache:<name>` for an earlier `--out` of a synthesizer in the plugin's cache, so that a voice can be
read back by a recognizer. Install the debug build and test APK first (`py .python/device_speech_run.py install`).
The device behind a remote adb server is reached with ANDROID_ADB_SERVER_ADDRESS / ANDROID_ADB_SERVER_PORT set for
this process only.
"""
import argparse
import base64
import hashlib
import re
import subprocess
import sys
import tarfile
import time
from pathlib import Path

PACKAGE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
RUNNER = PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner"
REMOTE = "/data/local/tmp/macaw-probe"

# Upstream sherpa-onnx assets (size, SHA-256 from the GitHub release API) and the roles the probe needs.
PRESETS = {
    "pengcheng-8": dict(
        asset="asr-models/sherpa-onnx-streaming-zipformer-ar_en_id_ja_ru_th_vi_zh-2025-02-10.tar.bz2", size=258999581,
        sha256="28044b67324f7f831689f0a3761473dd2ade380e93aa53f1dbcd479ef71c40d4", type="online-transducer",
        roles="encoder=encoder-epoch-75-avg-11-chunk-16-left-128.int8.onnx;decoder=decoder-epoch-75-avg-11-chunk-16-left-128.onnx;"
              "joiner=joiner-epoch-75-avg-11-chunk-16-left-128.int8.onnx;tokens=tokens.txt;modelType=zipformer2",
        wav="test_wavs/ja.wav"),
    "omnilingual-300m": dict(
        asset="asr-models/sherpa-onnx-omnilingual-asr-1600-languages-300M-ctc-v2-int8-2026-02-05.tar.bz2", size=292313120,
        sha256="951b32409aade32bd525310bb39e9666773ba3fc611a39e817f620936d76c631", type="omnilingual",
        roles="model=model.int8.onnx;tokens=tokens.txt", wav="test_wavs/es.wav"),
    "canary-180m": dict(
        asset="asr-models/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8.tar.bz2", size=153692328,
        sha256="7a38ed8b13f014ad632b09ff8d22e0c6f1359dd046af9235d281dfae841b9ab9", type="canary",
        roles="encoder=encoder.int8.onnx;decoder=decoder.int8.onnx;tokens=tokens.txt;language=es", wav="test_wavs/en.wav"),
    "nemo-es": dict(
        asset="asr-models/sherpa-onnx-nemo-fast-conformer-transducer-es-1424-int8.tar.bz2", size=107246685,
        sha256="d6d29ffd560c366da1b177b71e79d8733a9a9910dd1ace282d9bfae94e056210", type="nemo-transducer",
        roles="encoder=encoder.int8.onnx;decoder=decoder.int8.onnx;joiner=joiner.int8.onnx;tokens=tokens.txt", wav="test_wavs/es-spanish.wav"),
    "nemo-10": dict(
        asset="asr-models/sherpa-onnx-nemo-fast-conformer-transducer-be-de-en-es-fr-hr-it-pl-ru-uk-20k-int8.tar.bz2", size=106546673,
        sha256="06072bad277f0f4c29cc866d7c62b0e47936da39afafeae453faa925025ccad6", type="nemo-transducer",
        roles="encoder=encoder.int8.onnx;decoder=decoder.int8.onnx;joiner=joiner.int8.onnx;tokens=tokens.txt", wav="test_wavs/es-spanish.wav"),
    "sense-voice-2024": dict(
        asset="asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17.tar.bz2", size=163002883,
        sha256="7d1efa2138a65b0b488df37f8b89e3d91a60676e416f515b952358d83dfd347e", type="sense-voice",
        roles="model=model.int8.onnx;tokens=tokens.txt", wav="test_wavs/ja.wav"),
    "kokoro-v1_0-int8": dict(
        asset="tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2", size=132303094,
        sha256="4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e", type="kokoro",
        roles="model=model.int8.onnx;voices=voices.bin;tokens=tokens.txt;dataDir=espeak-ng-data;"
              "lexicon=lexicon-us-en.txt,lexicon-zh.txt;ruleFsts=date-zh.fst,phone-zh.fst,number-zh.fst"),
    "piper-ar-miro-v2": dict(
        asset="tts-models/vits-piper-ar_JO-SA_miro_V2-high-int8.tar.bz2", size=21875982,
        sha256="20f4db99d450986716ea9149a4df068158f69c91bde3859ab28d537ddfc57ce6", type="vits",
        roles="model=ar_JO-SA_miro_V2-high.onnx;tokens=tokens.txt;dataDir=espeak-ng-data"),
    "piper-ar-dii": dict(
        asset="tts-models/vits-piper-ar_JO-SA_dii-high-int8.tar.bz2", size=21998588,
        sha256="02f6f990b31323b7f36c8c68c3a5dc7f7c9f3db555c632310693cf7d82b9540e", type="vits",
        roles="model=ar_JO-SA_dii-high.onnx;tokens=tokens.txt;dataDir=espeak-ng-data"),
    "piper-ar-kareem": dict(
        asset="tts-models/vits-piper-ar_JO-kareem-medium-int8.tar.bz2", size=20968771,
        sha256="215910431bbe8236b77242b19d869a4eda6073e9cd424bec45b6d4a59a790d82", type="vits",
        roles="model=ar_JO-kareem-medium.onnx;tokens=tokens.txt;dataDir=espeak-ng-data"),
    "mimic3-ko-kss": dict(
        asset="tts-models/vits-mimic3-ko_KO-kss_low.tar.bz2", size=66838474,
        sha256="f015d1d15a52ed00d6fe22757c5ef4a74283c53daf829c838fa5c22616ed789c", type="vits",
        roles="model=ko_KO-kss_low.onnx;tokens=tokens.txt;dataDir=espeak-ng-data"),
}


def adb(serial, *args, check=True, timeout=None):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
    if check and result.returncode != 0:
        raise SystemExit(f"adb {' '.join(args[:3])} failed: {(result.stderr or result.stdout).strip()[:300]}")
    return result.stdout.strip()


def label(serial):
    return f"{adb(serial, 'shell', 'getprop', 'ro.product.model')} ({serial}, API {adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')})"


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def unpack(preset, archives):
    spec = PRESETS[preset]
    archive = archives / spec["asset"]
    if not archive.is_file() or archive.stat().st_size != spec["size"] or sha256(archive) != spec["sha256"]:
        raise SystemExit(f"{archive} is missing or does not match the pinned size and SHA-256")
    target = archives / "probe-unpacked" / preset
    if not target.is_dir():
        staging = target.with_name(preset + ".partial")
        with tarfile.open(archive, "r:bz2") as tar:
            tar.extractall(staging, filter="data")
        roots = [p for p in staging.iterdir() if p.is_dir()]
        (roots[0] if len(roots) == 1 else staging).rename(target)
        if staging.exists():
            staging.rmdir()
    return target


def push(serial, preset, folder):
    remote = f"{REMOTE}/{preset}"
    if adb(serial, "shell", f"test -d {remote} && echo yes", check=False) != "yes":
        adb(serial, "shell", "mkdir", "-p", REMOTE)
        started = time.time()
        adb(serial, "push", str(folder), remote, timeout=3600)
        print(f"{label(serial)}: pushed {preset} in {time.time() - started:.0f} s")
    return remote


def probe(args):
    spec = PRESETS[args.preset]
    archives = Path(args.archives).resolve()
    remote = push(args.serial, args.preset, unpack(args.preset, archives))
    roles = spec["roles"] + (f";lang={args.lang}" if args.lang else "")
    extra = {"macawProbeDir": remote, "macawProbeType": spec["type"], "macawProbeRoles": roles, "macawProbeLabel": args.preset}
    wav = args.wav or spec.get("wav")
    if spec["type"] not in ("kokoro", "vits"):
        if wav.startswith("cache:"):
            extra["macawProbeWav"] = wav
        elif Path(wav).is_file():
            adb(args.serial, "push", wav, f"{REMOTE}/{Path(wav).name}")
            extra["macawProbeWav"] = f"{REMOTE}/{Path(wav).name}"
        else:
            extra["macawProbeWav"] = f"{remote}/{wav}"
    # Texts travel as base64: adb on Windows and the device shell do not agree on non-ASCII arguments.
    for key, value in (("macawProbeRef", args.ref), ("macawProbeText", args.text)):
        if value is not None:
            extra[key + "64"] = base64.b64encode(value.encode("utf-8")).decode("ascii")
    for key, value in (("macawProbeSid", args.sid), ("macawProbeOut", args.out), ("macawProbeHead", args.head)):
        if value is not None:
            extra[key] = str(value)
    since = adb(args.serial, "shell", "date", "'+%m-%d %H:%M:%S.000'")
    command = ["shell", "am", "instrument", "-w", "-r", "-e", "class", f"{PACKAGE}.ModelProbeDeviceTest#probe"]
    for key, value in extra.items():
        command += ["-e", key, "'" + value.replace("'", "'\\''") + "'"]
    output = adb(args.serial, *command, RUNNER, check=False, timeout=3600)
    ok = "OK (" in output and "INSTRUMENTATION_STATUS_CODE: -2" not in output
    print(f"{label(args.serial)}: {args.preset}: {'PASS' if ok else 'FAIL'}")
    if not ok:
        for match in re.finditer(r"INSTRUMENTATION_STATUS: stack=(.+?)(?:\n\S|\Z)", output, re.S):
            print("    " + match.group(1).strip().splitlines()[0][:400])
    log = adb(args.serial, "logcat", "-d", "-v", "raw", "-T", since, "-s", "MacawDeviceRun:I", check=False)
    for line in log.splitlines():
        if line.startswith("probe "):
            print("    " + line.strip()[:400])
    return 0 if ok else 1


def clean(serial):
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    adb(serial, "shell", "run-as", PACKAGE, "rm", "-rf", "cache/probe", "cache/probe-out", "cache/probe-wav", check=False)
    print(f"{label(serial)}: removed {REMOTE} and the probe cache")


def main(argv):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = parser.add_subparsers(dest="action", required=True)
    sub.add_parser("list")
    run = sub.add_parser("probe")
    run.add_argument("serial")
    run.add_argument("preset", choices=sorted(PRESETS))
    run.add_argument("--archives", required=True)
    for name in ("wav", "ref", "text", "sid", "lang", "out", "head"):
        run.add_argument(f"--{name}")
    wipe = sub.add_parser("clean")
    wipe.add_argument("serial")
    args = parser.parse_args(argv)
    if args.action == "list":
        for name, spec in PRESETS.items():
            print(f"{name}: {spec['type']} {spec['asset']}")
        return 0
    if args.action == "clean":
        clean(args.serial)
        return 0
    return probe(args)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
