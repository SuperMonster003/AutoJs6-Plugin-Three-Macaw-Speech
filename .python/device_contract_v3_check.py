"""Contract version 3 from a script of a 6.8.1 host, against the installed 3-Macaw Speech (roadmap P11.1.4, D193 - D199).

    py .python/device_contract_v3_check.py <serial> v3      a 1.6.0 build that speaks version 3 but declares none of its features
    py .python/device_contract_v3_check.py <serial> v2      a plugin of contract version 2 (1.5.0 and earlier)
    py .python/device_contract_v3_check.py <serial> prepare a 1.6.0 build that declares speechPrepare and speechResident (P11.2, P11.3)

Every version 3 call must fail at once with FEATURE_UNAVAILABLE and a message that names the missing feature (v3) or the
two contract versions (v2): speech.prepare / prepareAsync, the speech.resident methods and `on('hit')`, and listen / wake
with `keepLoaded` or `background`. `speech.release()` answers 0 and `release(id)` false without asking the plugin,
`speech.resident.enabled` answers false, `off('hit')` answers undefined, and the argument checks of D196 still come first
(keepLoaded 1441 is LIMIT_EXCEEDED, keepLoaded inside thenListen INVALID_OPTIONS). An external dictation session without
the new options still opens and ends (the existing API is untouched; device_host_scripts.py covers the rest).

Mode `prepare` (roadmap P11.2, D202 - D204): prepare / prepareAsync answer `{ leaseId, modelId, loadMillis }` (the
streaming model, the default listen model and the default wake model), a second prepare of the same model reuses the
engines (loadMillis 0, a new lease), release(id) answers true once and then false, the 16 leases of D197 / D203 end in
LIMIT_EXCEEDED and release() answers 16, a keepLoaded listen session's ready event carries a lease that release(id)
returns, and `background` still fails with FEATURE_UNAVAILABLE. The resident methods answer since P11.3 (speechResident):
`resident.get()` returns `{ enabled, rules }` and `enabled` a boolean; the switch itself is not touched here
(device_resident_actions.py covers enable / set / on('hit')).

The script starts through AutoJs6's RunIntentActivity (see device_host_scripts.py) and writes a JSON result to shared
storage. Output: one line per item and the host and plugin versions, no transcript. Unlock the device first.
"""
import json
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

SCRIPT_PREPARE = scripts.COMMON + """
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
    var first = null;
    probe("prepare", function () { first = speech.prepare({ model: "%(streaming)s" }); return first; });
    probe("prepare again", function () { return speech.prepare({ model: "%(streaming)s" }); });
    probe("prepare default", function () { return speech.prepare(); });
    probe("prepare kws", function () { return speech.prepare({ kind: "kws" }); });
    probe("release(id)", function () { return speech.release(first && first.leaseId); });
    probe("release(id) again", function () { return speech.release(first && first.leaseId); });
    probe("release(id) unknown", function () { return speech.release("lease-unknown"); });
    probe("lease limit", function () {
        for (var i = 0; i < 20; i++) {
            try { speech.prepare({ model: "%(streaming)s" }); } catch (e) { return { granted: i, code: String(e.code || "none") }; }
        }
        return { granted: 20, code: "none" };
    });
    probe("release()", function () { return speech.release(); });
    probe("release() empty", function () { return speech.release(); });
    probe("resident.get", function () { return speech.resident.get(); });
    probe("resident.enabled", function () { return speech.resident.enabled; });
    probe("resident.off", function () { return speech.resident.off("hit"); });
    probe("wake background", function () { var s = speech.wake(["hello macaw"], { background: true }); s.stop(); return "opened"; });
    probe("wake keepLoaded", function () { var s = speech.wake(["hello macaw"], { keepLoaded: 5 }); s.stop(); return "opened"; });
    probe("keepLoaded 1441", function () { var s = speech.listen({ source: "external", keepLoaded: 1441 }); s.cancel(); return "opened"; });
    probe("thenListen keepLoaded", function () { var s = speech.wake(["hello macaw"], { thenListen: { keepLoaded: true } }); s.stop(); return "opened"; });
    var pending = 2;
    function settle() { if (--pending === 0) { probe("release() at the end", function () { return speech.release(); }); done(); } }
    var asyncAt = Date.now();
    speech.prepareAsync({ model: "%(streaming)s" }).then(function (v) {
        r.codes["prepareAsync"] = "resolved"; r.values["prepareAsync"] = JSON.stringify(v); r.millis["prepareAsync"] = Date.now() - asyncAt; settle();
    }, function (e) {
        r.codes["prepareAsync"] = String(e.code || "none"); r.messages["prepareAsync"] = String(e.message || e).slice(0, 300);
        r.millis["prepareAsync"] = Date.now() - asyncAt; settle();
    });
    var s = speech.listen({ source: "external", model: "%(streaming)s", timeoutMillis: 0, keepLoaded: true });
    s.on("ready", function (e) { r.events.push("ready"); r.keepLease = e && e.leaseId ? String(e.leaseId) : null; s.cancel(); });
    s.on("error", function (e) { r.errors.push(e.code + " " + e.message); });
    s.on("end", function (e) {
        r.events.push("end:" + e.reason);
        probe("release(keepLoaded)", function () { return speech.release(r.keepLease); });
        settle();
    });
} catch (e) { fail(e); }
"""

# Mode `prepare`: a build that declares speechPrepare (P11.2) but not yet speechResident / speechBackgroundWake.
PREPARE_UNAVAILABLE = {"wake background": "speechBackgroundWake"}
PREPARE_RETURNS = {"release(id)": "true", "release(id) again": "false", "release(id) unknown": "false", "release() empty": "0",
                   "resident.enabled": ("false", "true"), "resident.off": "undefined", "wake keepLoaded": '"opened"',
                   "release(keepLoaded)": "true", "release() at the end": ("1", "2")}
# At the end the prepareAsync lease is held, and the lease of "wake keepLoaded" (5 minutes) too when that session
# reached ready before its stop arrived (D204: the lease is given with the ready event), so 1 or 2.
# zh-CN: 结束时持有 prepareAsync 的租约; "wake keepLoaded" 的会话若在 stop 到达前已 ready, 还持有它的 5 分钟租约 (D204: 租约随 ready
# 事件发放), 所以是 1 或 2.

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


def lease(value):
    """The `{ leaseId, modelId, loadMillis }` of a prepare as a dict, or None when it is not one."""
    try:
        answer = json.loads(value or "")
    except ValueError:
        return None
    ok = isinstance(answer, dict) and str(answer.get("leaseId", "")).startswith("lease-") and answer.get("modelId") \
        and isinstance(answer.get("loadMillis"), (int, float)) and answer["loadMillis"] >= 0
    return answer if ok else None


def check_prepare(r):
    """The checks of mode `prepare`; prints one line per item and answers the number of failures."""
    codes, messages, values, millis = r["codes"], r["messages"], r["values"], r["millis"]
    failures = 0

    def report(ok, name, detail):
        nonlocal failures
        failures += not ok
        print(f"    {'PASS' if ok else 'FAIL'} {name}: {detail}")

    features = (r.get("features") or "").split(",")
    report(r.get("contractVersion") == 3 and "speechPrepare" in features and "speechResident" in features
           and "speechBackgroundWake" not in features, "features", f"contractVersion {r.get('contractVersion')}, {r.get('features')}")
    answers = {name: lease(values.get(name)) for name in ("prepare", "prepare again", "prepare default", "prepare kws", "prepareAsync")}
    for name, answer in answers.items():
        detail = (f"{answer['modelId']} loadMillis {answer['loadMillis']} in {millis.get(name)} ms" if answer
                  else f"{codes.get(name)} {messages.get(name, values.get(name))}")
        report(answer is not None and (name != "prepare" or answer["modelId"] == scripts.STREAMING), name, detail)
    again, first = answers["prepare again"], answers["prepare"]
    report(bool(again and first) and again["loadMillis"] == 0 and again["leaseId"] != first["leaseId"],
           "prepare again reuses the engines", f"loadMillis {again and again['loadMillis']}, a new lease id {bool(again and first and again['leaseId'] != first['leaseId'])}")
    limit = json.loads(values.get("lease limit") or "{}")
    # After three leases (prepare again, default, kws) the 16 of D197 / D203 leave 13 places.
    report(limit.get("granted") == 13 and limit.get("code") == "LIMIT_EXCEEDED", "lease limit 16",
           f"{limit.get('granted')} more granted, then {limit.get('code')}")
    report(codes.get("release()") == "returned" and values.get("release()") == "16", "release() of 16 leases", values.get("release()", codes.get("release()")))
    for name, feature in PREPARE_UNAVAILABLE.items():
        report(codes.get(name) == "FEATURE_UNAVAILABLE" and f'"{feature}"' in messages.get(name, ""), name, f"{codes.get(name)} in {millis.get(name)} ms")
    resident = values.get("resident.get") or ""
    report(codes.get("resident.get") == "returned" and '"rules"' in resident and '"enabled"' in resident, "resident.get",
           resident[:120] if codes.get("resident.get") == "returned" else codes.get("resident.get"))
    for name, value in PREPARE_RETURNS.items():
        report(codes.get(name) == "returned" and values.get(name) in (value if isinstance(value, tuple) else (value,)), name, values.get(name) if codes.get(name) == "returned" else codes.get(name))
    for name, code in ARGUMENTS.items():
        report(codes.get(name) == code, name, codes.get(name))
    events = r.get("events", [])
    report(events[:1] == ["ready"] and events[-1:] == ["end:cancelled"] and str(r.get("keepLease") or "").startswith("lease-")
           and not r.get("errors"), "keepLoaded session: ready carries a lease", f"events={','.join(events)} lease={'yes' if r.get('keepLease') else 'no'} errors={r.get('errors')}")
    return failures


def main(argv):
    if len(argv) != 2 or argv[1] not in ("v2", "v3", "prepare"):
        print(__doc__)
        return 2
    serial, mode = argv
    head = scripts.label(serial)
    host, plugin = versions(serial)
    print(f"{head}: host {host}, plugin {plugin}, expecting a contract {mode} plugin")
    scripts.SCRIPTS[NAME] = SCRIPT_PREPARE if mode == "prepare" else SCRIPT
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
    if mode == "prepare":
        failures = check_prepare(r)
        print(f"{head}: {'PASS' if failures == 0 else 'FAIL'} ({failures} failed)")
        return 0 if failures == 0 else 1
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
