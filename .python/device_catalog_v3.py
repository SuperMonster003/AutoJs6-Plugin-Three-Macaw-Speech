"""Device evidence of the catalog v3 models (roadmap P8.2.4, P8.3, D127, D131) with an unsigned development catalog.

    py .python/device_catalog_v3.py push <serial> <catalog-v3.json>   copy the pending catalog into the debug build
    py .python/device_catalog_v3.py run <serial> [--install] [--remove] [--models <ids>]
    py .python/device_catalog_v3.py analysis <serial> [--long]        the offline analyses of P8.4 (AnalysisDeviceTest, the tools)
    py .python/device_catalog_v3.py clean <serial>                    remove the development catalog and the temp files

`push` copies Shared-Assets `speech-models/catalogs/pending/v3/catalog-v3.json` (schema 3, catalogVersion 3, unsigned;
the language models of P8.2, the speaker model of P8.3 and the analysis models of P8.4) through `/data/local/tmp` and
`run-as` to `files/dev-catalog/catalog-v3.json` of the installed debug build, which uses it instead of the bundled
snapshot (`SpeechModels.DEV_CATALOG`; release builds ignore it). `run` runs `CatalogV3ModelsDeviceTest`: `--install`
installs the six language models first (about 610 MB; from the sherpa-onnx assets until the `speech-models-v3` mirror
was published, from the mirror since; roadmap P8.5.2 bundles the signed release 3, so `push` is optional), `--models` the given ones instead (`ANALYSIS_MODELS` for P8.4), `--remove` deletes them at the end;
the output is PASS / FAIL with the `MacawDeviceRun` lines (memory, character error rates, sample rates). `analysis`
runs AnalysisDeviceTest and the analysis case of SpeechToolServiceDeviceTest (roadmap P8.4.5), `--long` adds the
10 minute timing. `clean` deletes the development catalog, so the bundled one is used again. Install the debug and test
APKs first (`py .python/device_speech_run.py install <serial>`). Nothing here answers a dialog on the device.
"""
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_speech_run as sr  # noqa: E402

REMOTE = "/data/local/tmp/macaw-v3"
DEV_CATALOG = "files/dev-catalog/catalog-v3.json"
TEST = "CatalogV3ModelsDeviceTest"
ANALYSIS_MODELS = "segment.pyannote.3-0,speaker.eres2net.3dspeaker.en-voxceleb,langid.whisper.tiny.int8,tagger.ced.mini.int8"
ANALYSIS_TESTS = ("AnalysisDeviceTest", "SpeechToolServiceDeviceTest#test6AnalysisTools")


def push(serial, catalog):
    data = Path(catalog).read_bytes()
    sr.adb(serial, "shell", "mkdir", "-p", REMOTE)
    sr.adb(serial, "push", str(catalog), f"{REMOTE}/catalog-v3.json")
    sr.adb(serial, "shell", "chmod", "644", f"{REMOTE}/catalog-v3.json")
    sr.adb(serial, "shell", "run-as", sr.PACKAGE, "sh", "-c", f"'mkdir -p files/dev-catalog && cp {REMOTE}/catalog-v3.json {DEV_CATALOG}'")
    size = sr.adb(serial, "shell", "run-as", sr.PACKAGE, "stat", "-c", "%s", DEV_CATALOG, check=False)
    sr.adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    ok = size.strip() == str(len(data))
    print(f"{sr.label(serial)}: development catalog: {'PASS' if ok else 'FAIL'} ({size.strip()} of {len(data)} bytes)")
    return ok


def clean(serial):
    sr.adb(serial, "shell", "run-as", sr.PACKAGE, "rm", "-rf", "files/dev-catalog", "cache/device-test", check=False)
    sr.adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    left = sr.adb(serial, "shell", "run-as", sr.PACKAGE, "ls", "files/dev-catalog", check=False)
    ok = "No such file" in left or not left.strip()
    print(f"{sr.label(serial)}: clean: {'PASS' if ok else 'FAIL'}")
    return ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("push")
    p.add_argument("serial")
    p.add_argument("catalog")
    r = sub.add_parser("run")
    r.add_argument("serial")
    r.add_argument("--install", action="store_true")
    r.add_argument("--remove", action="store_true")
    r.add_argument("--models", help="comma separated model ids to install instead of all six")
    a = sub.add_parser("analysis")
    a.add_argument("serial")
    a.add_argument("--long", action="store_true")
    c = sub.add_parser("clean")
    c.add_argument("serial")
    args = parser.parse_args(argv)
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if args.command == "push":
        return 0 if push(args.serial, args.catalog) else 1
    if args.command == "clean":
        return 0 if clean(args.serial) else 1
    if args.command == "analysis":
        results = [sr.run_test(args.serial, test, ["macawLong=true"] if args.long and "#" not in test else []) for test in ANALYSIS_TESTS]
        return 0 if all(results) else 1
    arguments = []
    if args.install or args.models:
        arguments.append(f"macawV3Models={args.models or 'true'}")
    if args.remove:
        arguments.append("macawV3Remove=true")
    return 0 if sr.run_test(args.serial, TEST, arguments) else 1


if __name__ == "__main__":
    sys.exit(main())
