"""Contract version 3 from a script of a 6.8.1 host, against the installed 3-Macaw Speech (roadmap P11.1.4, D193 - D199).

    py .python/device_contract_v3_check.py <serial> v3      a 1.6.0 build that speaks version 3 but declares none of its features
    py .python/device_contract_v3_check.py <serial> v2      a plugin of contract version 2 (1.5.0 and earlier)

Every version 3 call must fail at once with FEATURE_UNAVAILABLE and a message that names the missing feature (v3) or the
two contract versions (v2): speech.prepare / prepareAsync, the speech.resident methods and `on('hit')`, and listen / wake
with `keepLoaded` or `background`. `speech.release()` answers 0 and `release(id)` false without asking the plugin,
`speech.resident.enabled` answers false, `off('hit')` answers undefined, and the argument checks of D196 still come first
(keepLoaded 1441 is LIMIT_EXCEEDED, keepLoaded inside thenListen INVALID_OPTIONS). An external dictation session without
the new options still opens and ends (the existing API is untouched; device_host_scripts.py covers the rest).

The script starts through AutoJs6's RunIntentActivity (see device_host_scripts.py) and writes a JSON result to shared
storage. Output: one line per item and the host and plugin versions, no transcript. Unlock the device first.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as scripts  # noqa: E402

NAME = "contract3"

SCRIPT = scripts.COMMON + """
r.codes = {}; r.messages = {}; r.values = {}; r.millis = {};
function probe(name, fn) {
    var t = Date.now();
    try {
        var value = fn();
        r.codes[name] = "returned";
        r.values[name] = value === undefined ? "undefined" : JSON.stringify(value);
    } catch (e) {
        r.codes[name] = String(e.code || "none");
        r.messages[name] = String(e.message || e).slice(0, 300);
    }
    r.millis[name] = Date.now() - t;
}
try {
    r.available = speech.isAvailable();
    var caps = speech.capabilities();
    r.contractVersion = caps.contractVersion;
    r.features = caps.features.join(",");
    probe("prepare", function () { return speech.prepare(); });
    probe("release()", function () { return speech.release(); });
    probe("release(id)", function () { return speech.release("lease-unknown"); });
    probe("resident.get", function () { return speech.resident.get(); });
    probe("resident.set", function () { return speech.resident.set(["hello macaw"]); });
    probe("resident.enable", function () { return speech.resident.enable(); });
    probe("resident.disable", function () { return speech.resident.disable(); });
    probe("resident.enabled", function () { return speech.resident.enabled; });
    probe("resident.on", function () { return speech.resident.on("hit", function () {}); });
    probe("resident.off", function () { return speech.resident.off("hit"); });
    probe("listen keepLoaded", function () { var s = speech.listen({ source: "external", keepLoaded: true }); s.cancel(); return "opened"; });
    probe("wake keepLoaded", function () { var s = speech.wake(["hello macaw"], { keepLoaded: 5 }); s.stop(); return "opened"; });
    probe("wake background", function () { var s = speech.wake(["hello macaw"], { background: true }); s.stop(); return "opened"; });
    probe("keepLoaded 1441", function () { var s = speech.listen({ source: "external", keepLoaded: 1441 }); s.cancel(); return "opened"; });
    probe("thenListen keepLoaded", function () { var s = speech.wake(["hello macaw"], { thenListen: { keepLoaded: true } }); s.stop(); return "opened"; });
    var pending = 2;
    function settle() { if (--pending === 0) done(); }
    var asyncAt = Date.now();
    speech.prepareAsync().then(function (v) {
        r.codes["prepareAsync"] = "resolved"; r.millis["prepareAsync"] = Date.now() - asyncAt; settle();
    }, function (e) {
        r.codes["prepareAsync"] = String(e.code || "none"); r.messages["prepareAsync"] = String(e.message || e).slice(0, 300);
        r.millis["prepareAsync"] = Date.now() - asyncAt; settle();
    });
    var s = speech.listen({ source: "external", model: "%(streaming)s", timeoutMillis: 0 });
    s.on("ready", function () { r.events.push("ready"); s.cancel(); });
    s.on("error", function (e) { r.errors.push(e.code + " " + e.message); });
    s.on("end", function (e) { r.events.push("end:" + e.reason); settle(); });
} catch (e) { fail(e); }
"""

UNAVAILABLE = ("prepare", "prepareAsync", "resident.get", "resident.set", "resident.enable", "resident.disable", "resident.on",
               "listen keepLoaded", "wake keepLoaded", "wake background")
RETURNS = {"release()": "0", "release(id)": "false", "resident.enabled": "false", "resident.off": "undefined"}
ARGUMENTS = {"keepLoaded 1441": "LIMIT_EXCEEDED", "thenListen keepLoaded": "INVALID_OPTIONS"}
FEATURES = {"prepare": "speechPrepare", "prepareAsync": "speechPrepare", "listen keepLoaded": "speechPrepare", "wake keepLoaded": "speechPrepare",
            "wake background": "speechBackgroundWake"}
# The host answers an operation it refuses within this time: no binding, no wait for the plugin (D193).
MAX_REFUSAL_MILLIS = 1500


def versions(serial):
    def field(package, key):
        for line in scripts.adb(serial, "shell", "dumpsys", "package", package, check=False).splitlines():
            line = line.strip()
            if line.startswith(key + "="):
                value = line.split("=", 1)[1]
                return value if key == "lastUpdateTime" else value.split(" ")[0]
        return "?"
    host = f"{field(scripts.HOST, 'versionName')} ({field(scripts.HOST, 'versionCode')}, updated {field(scripts.HOST, 'lastUpdateTime')})"
    plugin = f"{field(scripts.PLUGIN, 'versionName')} ({field(scripts.PLUGIN, 'versionCode')})"
    return host, plugin


def expected_message(name, mode, message):
    if mode == "v2":
        return "version 2" in message and "version 3" in message
    feature = FEATURES.get(name, "speechResident")
    return f'"{feature}"' in message


def main(argv):
    if len(argv) != 2 or argv[1] not in ("v2", "v3"):
        print(__doc__)
        return 2
    serial, mode = argv
    head = scripts.label(serial)
    host, plugin = versions(serial)
    print(f"{head}: host {host}, plugin {plugin}, expecting a contract {mode} plugin")
    scripts.SCRIPTS[NAME] = SCRIPT
    scripts.adb(serial, "shell", "rm", "-rf", scripts.REMOTE, check=False)
    scripts.adb(serial, "shell", "mkdir", "-p", scripts.REMOTE)
    try:
        if not scripts.start(serial, NAME, {}):
            print(f"{head}: BLOCKED the script did not start (unlock the device; AutoJs6 needs all-files access)")
            return 1
        r = scripts.result(serial, NAME, 120)
    finally:
        scripts.adb(serial, "shell", "rm", "-rf", scripts.REMOTE, check=False)
    if r is None:
        print(f"{head}: FAIL no result")
        return 1
    if r.get("errors") and "codes" not in r:
        print(f"{head}: FAIL script error {r['errors']}")
        return 1
    failures = 0
    print(f"    available={r.get('available')} contractVersion={r.get('contractVersion')} features={r.get('features')}")
    codes, messages, values, millis = r["codes"], r["messages"], r["values"], r["millis"]
    for name in UNAVAILABLE:
        code, message, spent = codes.get(name), messages.get(name, ""), millis.get(name)
        ok = code == "FEATURE_UNAVAILABLE" and expected_message(name, mode, message) and spent is not None and spent <= MAX_REFUSAL_MILLIS
        failures += not ok
        detail = "" if ok else f" message={message!r}"
        print(f"    {'PASS' if ok else 'FAIL'} {name}: {code} in {spent} ms{detail}")
    for name, value in RETURNS.items():
        ok = codes.get(name) == "returned" and values.get(name) == value
        failures += not ok
        print(f"    {'PASS' if ok else 'FAIL'} {name}: {values.get(name) if codes.get(name) == 'returned' else codes.get(name)} in {millis.get(name)} ms")
    for name, code in ARGUMENTS.items():
        ok = codes.get(name) == code
        failures += not ok
        print(f"    {'PASS' if ok else 'FAIL'} {name}: {codes.get(name)}")
    events = r.get("events", [])
    ok = events[:1] == ["ready"] and events[-1:] == ["end:cancelled"] and not r.get("errors")
    failures += not ok
    print(f"    {'PASS' if ok else 'FAIL'} external session without new options: events={','.join(events)} errors={r.get('errors')}")
    print(f"{head}: {'PASS' if failures == 0 else 'FAIL'} ({failures} failed)")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
