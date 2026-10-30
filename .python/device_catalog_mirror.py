"""Catalog refresh and mirror download of the installed 3-Macaw Speech build on one device (roadmap P7.9.2, D108, P8.5.2).

    py .python/device_catalog_mirror.py <serial> [--model <id>]

1. package: versionCode of the installed plugin and whether it is debuggable (a release build is not).
2. refresh: an AutoJs6 script (RunIntentActivity, like device_host_scripts.py) calls speech.models.refreshCatalog(),
   which downloads the catalog of its wire schema (catalog-v3.json since 1.3.0) and its signature from Shared-Assets `main`
   and verifies the signature over the exact bytes; a refused signature or a rollback ends in an error. The plugin log line
   `Catalog refresh: version N` confirms it, N being `catalog.version` of locks/speech-catalog.lock.
3. mirror: installs one model whose catalog version points at the Release of that catalog (default: the CED mini audio
   tagger of `speech-models-v3`, 10 MB; punctuation models are not open to scripts), removing it first when present, and
   reads the plugin log for `Installed <id> ... from mirror` (a fallback logs `Mirror failed` and `from upstream`). A model
   that was not installed before is removed again.

Output: PASS / FAIL per step and the key numbers. Unlock the device first; nothing here answers a system dialog.
"""
import argparse
import os
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hs  # noqa: E402

PLUGIN = hs.PLUGIN
HOST = hs.HOST
MODEL = "tagger.ced.mini.int8"
LOCK = Path(__file__).resolve().parent.parent / "locks" / "speech-catalog.lock"
LOG_TAG = "MacawModels"

SCRIPT = hs.COMMON + """
r.steps = {};
function attempt(name, fn) {
    try { r.steps[name] = fn(); } catch (e) { r.steps[name] = { error: String(e.code || "") + " " + String(e.message || e) }; }
}
attempt("before", function () { return { catalog: speech.capabilities().catalogVersion }; });
attempt("refresh", function () {
    var t = Date.now();
    var x = speech.models.refreshCatalog();
    return { ms: Date.now() - t, version: x.version, updated: x.updated, catalog: speech.capabilities().catalogVersion };
});
attempt("mirror", function () {
    var present = speech.models.list({ installed: true }).filter(function (m) { return m.id === "%(model)s"; }).length > 0;
    if (present) speech.models.remove("%(model)s");
    var t = Date.now(), done = 0, total = 0;
    var model = speech.models.install("%(model)s", { acceptLicense: true, onProgress: function (d, all) { done = d; total = all; } });
    var result = { ms: Date.now() - t, done: done, total: total, installed: !!(model && model.installed), version: model && model.version,
                   installedBytes: model && model.installedBytes, presentBefore: present };
    if (!present) { speech.models.remove("%(model)s"); result.removedAfter = true; }
    return result;
});
done();
"""


def package_state(serial):
    dump = hs.adb(serial, "shell", "dumpsys", "package", PLUGIN, check=False)
    code = re.search(r"versionCode=(\d+)", dump)
    flags = re.search(r"pkgFlags=\[([^\]]*)\]", dump)
    return (int(code.group(1)) if code else None), (flags is not None and "DEBUGGABLE" in flags.group(1))


def catalog_version():
    """The version of the bundled catalog, which `main` serves until the next release."""
    for line in LOCK.read_text(encoding="utf-8").splitlines():
        if line.startswith("catalog.version="):
            return int(line.split("=", 1)[1])
    raise SystemExit(f"no catalog.version in {LOCK}")


def plugin_log(serial):
    return hs.adb(serial, "logcat", "-d", "-s", f"{LOG_TAG}:I", check=False).splitlines()


def run(serial, model):
    label = hs.label(serial)
    ok = True

    def report(name, passed, text):
        nonlocal ok
        ok = ok and passed
        print(f"    {name}: {'PASS' if passed else 'FAIL'} {text}")

    code, debuggable = package_state(serial)
    print(f"{label}: catalog refresh and mirror download")
    report("package", code is not None and not debuggable, f"versionCode {code}, debuggable {debuggable}")
    hs.adb(serial, "logcat", "-c", check=False)
    hs.adb(serial, "shell", "rm", "-rf", hs.REMOTE, check=False)
    hs.adb(serial, "shell", "mkdir", "-p", hs.REMOTE)
    script = SCRIPT % {"out": f"{hs.REMOTE}/catalog.json", "dir": hs.REMOTE, "model": model}
    local = Path(os.environ.get("TEMP", ".")) / f"macaw-catalog-{serial.replace(':', '_')}.js"
    local.write_text(script, encoding="utf-8")
    hs.adb(serial, "push", str(local), f"{hs.REMOTE}/catalog.js")
    hs.adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    hs.adb(serial, "shell", "am", "start", "-n", hs.launcher(serial))
    time.sleep(2)
    hs.adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
           "-d", f"file://{hs.REMOTE}/catalog.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    r = hs.result(serial, "catalog", 300)
    hs.adb(serial, "shell", "rm", "-rf", hs.REMOTE, check=False)
    local.unlink(missing_ok=True)
    if r is None:
        print("    script: FAIL no result (unlock the device; AutoJs6 needs all-files access)")
        return False
    steps = r.get("steps", {})
    lines = plugin_log(serial)
    refreshed = [line for line in lines if "Catalog refresh: version" in line]
    installed = [line for line in lines if f"Installed {model} " in line]
    fallback = [line for line in lines if f"Mirror failed for {model}" in line]

    f = steps.get("refresh", {})
    expected = catalog_version()
    logged = re.search(r"Catalog refresh: version (\d+), updated (\w+)", refreshed[-1]) if refreshed else None
    report("refresh", f.get("version") == expected and not f.get("error") and logged is not None and logged.group(1) == str(expected),
           f"signature verified, catalog version {f.get('version')} (before {steps.get('before', {}).get('catalog')}, after "
           f"{f.get('catalog')}), updated {f.get('updated')}, log {'version ' + logged.group(1) if logged else 'missing'}, "
           f"{f.get('ms')} ms {f.get('error') or ''}")
    m = steps.get("mirror", {})
    origin = re.search(r"from (\w+), (\d+) bytes downloaded", installed[-1]) if installed else None
    report("mirror", bool(m.get("installed")) and not m.get("error") and origin is not None and origin.group(1) == "mirror" and not fallback,
           f"{model} v{m.get('version')}: from {origin.group(1) if origin else 'unknown'}, {origin.group(2) if origin else '?'} bytes downloaded, "
           f"{m.get('done')} / {m.get('total')} progress bytes in {(m.get('ms') or 0) / 1000:.1f} s, installed {m.get('installedBytes')} bytes, "
           f"present before {m.get('presentBefore')}, removed after {bool(m.get('removedAfter'))}, mirror fallback {len(fallback)} "
           f"{m.get('error') or ''}")
    if r.get("errors"):
        ok = False
        print(f"    errors: {[e[:160] for e in r['errors']]}")
    print(f"{label}: CATALOG {'PASS' if ok else 'FAIL'}")
    return ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--model", default=MODEL, help="a model whose catalog version is mirrored on the Release of the bundled catalog")
    arguments = parser.parse_args(argv)
    return 0 if run(arguments.serial, arguments.model) else 1


if __name__ == "__main__":
    sys.exit(main())
