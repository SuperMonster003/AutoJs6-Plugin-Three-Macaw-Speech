"""The time to `ready` of the first `speech.listen` and its four parts (roadmap P11.1.1, F9, appendix F), and the same
after prewarming and retention (roadmap P11.2.3, D202 - D205).

    py .python/f9_startup_timing.py <serial> [hot] [background] [kill] [prepare] [keep] [never] [--runs N]
                                    [--idle-minutes M] [--long-runs L]

Each run starts a microphone dictation from an AutoJs6 script (RunIntentActivity, as `device_host_scripts.py` does),
which records when it called `speech.listen` and when `ready` arrived and cancels the session 1.5 s after `ready`. The
run then reads the host's binding log (tag SpeechPluginHost) and the plugin's startup log (tag MacawStartup, written by
a plugin build with `StartupTiming`) from the call on, and splits the time to `ready` into four parts:

    binding   the call to `openSession` arriving in the plugin, less the cold start: bindService, onServiceConnected,
              the capabilities and the session's dedicated binding
    cold      the start of the plugin process to its speech service bound (0 when the process was alive)
    engines   the engine load on the plugin's loader thread, including the time it queued behind another load
    session   the rest: options, model and admission checks, the foreground service, the dictation, `ready` reaching
              the script; split into `checks` (before the session's binder exists, with the creation of the speech
              runtime on the first call of a process), `start` (the dictation after the engines loaded) and `delivery`
              (the plugin's `ready` reaching the script, which includes the return of `openSession` to the host)

`recording` is when the microphone started, from the call. Starting points (all three by default):

    hot          the process alive and the engines loaded: right after a warm-up session
    background   a warm-up session, HOME, then M idle minutes (10 by default) before each run
    kill         HOME, then the plugin process ended with `am kill` (with `run-as kill` when the system keeps it)

The prewarming and retention modes of P11.2.3 (needs a host with contract version 3 and a plugin build that declares
`speechPrepare`; `prepare` takes N runs, `keep` and `never` L runs, 1 by default):

    prepare      as `kill`, then the script calls `speech.prepare({ model })` before the listen; the run also shows the
                 time of the prepare call and its `loadMillis`
    keep         one script per run: a warm-up session with `keepLoaded: true`, HOME, M idle minutes (10 by default)
                 with the script still running, then the measured listen of the same script (the lease returns when the
                 script ends)
    never        the plugin's "release idle models" set to never (`idleMinutes` 0 written with `run-as` while the
                 process is ended, the file restored afterwards), a warm-up session, HOME, 30 idle minutes (or M when
                 given), then the listen; the plugin's total PSS (`dumpsys meminfo`) after the warm-up and before the
                 listen is recorded, without a limit

`run-as` needs a debuggable plugin build. The host comes to the foreground through its launcher before every run (a
frozen host would not start the script). Output: one line per run and the medians per starting point; no transcript,
no microphone audio is needed.
"""
import json
import re
import statistics
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hosts  # noqa: E402

HOST, PLUGIN, REMOTE, STREAMING = hosts.HOST, hosts.PLUGIN, hosts.REMOTE, hosts.STREAMING
adb = hosts.adb
NAME = "f9-startup"

SCRIPT = """
var out = "%(out)s";
var r = {};
files.write(out + ".started", String(Date.now()));
function done() { files.write(out, JSON.stringify(r)); }
try {
    %(before)s
    r.t0 = Date.now();
    var s = speech.listen({ model: "%(streaming)s", timeoutMillis: 0, maxDurationMillis: 20000 });
    r.returned = Date.now();
    s.on("ready", function () {
        r.ready = Date.now();
        setTimeout(function () { s.cancel(); }, 1500);
    });
    s.on("error", function (e) { r.error = e.code + " " + e.message; });
    s.on("end", function (e) { r.reason = e.reason; done(); });
} catch (e) { r.error = String(e.code || "") + " " + String(e.message || e); done(); }
"""

# Prewarming (P11.2.1): the prepare call and its load time, then the listen as above.
PREPARE = """r.p0 = Date.now();
    var lease = speech.prepare({ model: "%(streaming)s" });
    r.prepared = Date.now(); r.loadMillis = lease.loadMillis; r.leased = !!lease.leaseId;"""

# Retention (P11.2.2): a warm-up session with keepLoaded, then the measured listen once the runner writes `.go`.
KEEP_SCRIPT = """
var out = "%(out)s";
var r = {};
files.write(out + ".started", String(Date.now()));
function done() { files.write(out, JSON.stringify(r)); }
function measured() {
    try {
        r.t0 = Date.now();
        var s = speech.listen({ model: "%(streaming)s", timeoutMillis: 0, maxDurationMillis: 20000 });
        r.returned = Date.now();
        s.on("ready", function () {
            r.ready = Date.now();
            setTimeout(function () { s.cancel(); }, 1500);
        });
        s.on("error", function (e) { r.error = e.code + " " + e.message; });
        s.on("end", function (e) { r.reason = e.reason; done(); });
    } catch (e) { r.error = String(e.code || "") + " " + String(e.message || e); done(); }
}
function waitForGo() { if (files.exists(out + ".go")) measured(); else setTimeout(waitForGo, 300); }
try {
    var w = speech.listen({ model: "%(streaming)s", timeoutMillis: 0, maxDurationMillis: 20000, keepLoaded: true });
    w.on("ready", function (e) {
        r.warmReady = Date.now(); r.leased = !!(e && e.leaseId);
        setTimeout(function () { w.cancel(); }, 1500);
    });
    w.on("error", function (e) { r.warmError = e.code + " " + e.message; });
    w.on("end", function () { files.write(out + ".warm", JSON.stringify(r)); waitForGo(); });
} catch (e) { r.error = String(e.code || "") + " " + String(e.message || e); done(); }
"""

PREFERENCES = "shared_prefs/speech-preferences.xml"
IDLE_KEY = "idleMinutes"
PSS = re.compile(r"TOTAL PSS:\s+(\d+)|^\s*TOTAL\s+(\d+)", re.M)

LINE = re.compile(r"^\s*(\d+)\.(\d{3})\s+\d+\s+\d+\s+[VDIWEF]\s+(\S+?)\s*: (.*)$")
CHECKS = re.compile(r"Dictation checked in (\d+) ms \(runtime (created|ready) after (\d+) ms, permission and model in (\d+) ms\)")
SESSION = re.compile(r"Session \S+ ready (\d+) ms after it was opened(?:, recording (\d+) ms)? "
                     r"\(engines (\d+) ms after (\d+) ms in the loader queue\)")


def plugin_pid(serial):
    return adb(serial, "shell", "pidof", PLUGIN, check=False).strip()


def logcat_since(serial, since_millis):
    """The lines of the two tags logged since `since_millis` (device wall clock) as (millis, tag, message)."""
    stamp = f"{since_millis // 1000}.{since_millis % 1000:03d}"
    raw = adb(serial, "logcat", "-d", "-v", "epoch", "-T", stamp, "-s", "SpeechPluginHost:I", "MacawStartup:I", "MacawEngines:I", check=False)
    lines = []
    for line in raw.splitlines():
        match = LINE.match(line)
        if match:
            lines.append((int(match.group(1)) * 1000 + int(match.group(2)), match.group(3), match.group(4)))
    return lines


def push_script(serial, template=SCRIPT, values=None):
    """Pushes a script and starts it through RunIntentActivity, the result files of an earlier run removed first."""
    adb(serial, "shell", "rm", "-f", *(f"{REMOTE}/{NAME}.json{suffix}" for suffix in ("", ".started", ".warm", ".go")), check=False)
    script = template % {"out": f"{REMOTE}/{NAME}.json", "streaming": STREAMING, "before": "", **(values or {})}
    local = Path(tempfile.gettempdir()) / f"macaw-{NAME}-{serial.replace(':', '_')}.js"
    local.write_text(script, encoding="utf-8")
    adb(serial, "push", str(local), f"{REMOTE}/{NAME}.js")
    local.unlink()
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/{NAME}.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")


def run_script(serial, template=SCRIPT, values=None):
    push_script(serial, template, values)
    return hosts.result(serial, NAME, 60)


def wait_json(serial, names, seconds):
    """The first of the JSON files `names` (in REMOTE) that appears within `seconds`, as (name, object)."""
    deadline = time.time() + seconds
    while time.time() < deadline:
        for name in names:
            raw = adb(serial, "shell", "cat", f"{REMOTE}/{name}", check=False)
            if raw.startswith("{"):
                return name, json.loads(raw)
        time.sleep(2)
    return None, None


def pss_kb(serial):
    """The plugin process's total PSS in KB (`dumpsys meminfo`), or None when it is not running."""
    if not plugin_pid(serial):
        return None
    match = PSS.search(adb(serial, "shell", "dumpsys", "meminfo", PLUGIN, check=False))
    return int(match.group(1) or match.group(2)) if match else None


def megabytes(kb):
    return f"{kb / 1024:.0f} MB" if kb else "-"


def read_preferences(serial):
    """The plugin's speech preference file (needs a debuggable build), or None when it does not exist."""
    text = adb(serial, "shell", "run-as", PLUGIN, "cat", PREFERENCES, check=False)
    return text + "\n" if "<map" in text else None


def write_preferences(serial, text):
    """Writes (or removes, for None) the speech preference file with run-as; the plugin process must be ended."""
    if text is None:
        adb(serial, "shell", "run-as", PLUGIN, "rm", "-f", PREFERENCES, check=False)
        return
    local = Path(tempfile.gettempdir()) / f"macaw-{NAME}-preferences-{serial.replace(':', '_')}.xml"
    local.write_text(text, encoding="utf-8", newline="\n")
    staged = f"/data/local/tmp/macaw-{NAME}-preferences.xml"
    adb(serial, "push", str(local), staged)
    local.unlink()
    adb(serial, "shell", f"run-as {PLUGIN} sh -c 'mkdir -p shared_prefs && cat {staged} > {PREFERENCES}'", check=False)
    adb(serial, "shell", "rm", "-f", staged, check=False)


def idle_of(text):
    match = re.search(rf'<int name="{IDLE_KEY}" value="(-?\d+)"', text or "")
    return match.group(1) if match else "default"


def with_idle(text, minutes):
    entry = f'<int name="{IDLE_KEY}" value="{minutes}" />'
    if text is None or "<map />" in text:
        return "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    " + entry + "\n</map>\n"
    pattern = rf'<int name="{IDLE_KEY}" value="-?\d+" ?/>'
    if re.search(pattern, text):
        return re.sub(pattern, entry, text)
    return text.replace("</map>", f"    {entry}\n</map>")


def bring_host(serial):
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", hosts.launcher(serial))
    time.sleep(2)


def end_plugin(serial):
    """Ends the plugin process in the background; answers how ("am kill", "run-as kill" or "alive")."""
    pid = plugin_pid(serial)
    if not pid:
        return "not running"
    adb(serial, "shell", "am", "kill", PLUGIN, check=False)
    time.sleep(1)
    if not plugin_pid(serial):
        return "am kill"
    adb(serial, "shell", "run-as", PLUGIN, "kill", "-9", pid, check=False)
    time.sleep(1)
    return "run-as kill" if not plugin_pid(serial) else "alive"


def measure(serial, template=SCRIPT, values=None):
    """One run: the script's times and the parts read from the log, or an `error` entry."""
    alive = bool(plugin_pid(serial))
    since = hosts.device_now(serial)
    return analyse(serial, run_script(serial, template, values), since, alive)


def analyse(serial, r, since, alive):
    """The parts of the measured listen of the result `r` from the log since `since`."""
    if not r or "ready" not in r:
        return {"error": (r or {}).get("error", "no result"), "alive": alive}
    time.sleep(1)
    t0, ready = r["t0"], r["ready"]
    out = {"alive": alive, "total": ready - t0, "returned": r["returned"] - t0}
    if "prepared" in r:
        out.update({"prepare": r["prepared"] - r["p0"], "loadMillis": r.get("loadMillis"), "leased": r.get("leased")})
    bind = connected = dedicated = received = None
    bound = created = 0
    session = checks = None
    loads = 0
    component = f"{{{PLUGIN}/"
    for millis, tag, message in logcat_since(serial, since):
        if millis < t0 - 5:
            continue
        if tag == "SpeechPluginHost" and component in message:
            if message.startswith("bindService:") and bind is None:
                bind = millis
            elif message.startswith("onServiceConnected:") and connected is None:
                connected = millis
            elif message.startswith("dedicated bindService:") and dedicated is None:
                dedicated = millis
        elif tag == "MacawStartup":
            if message.startswith("Speech service created") and not created:
                created = int(message.split()[3])
            elif message.startswith("Speech service bound") and not bound:
                bound = int(message.split()[3])
            elif message == "openSession received" and received is None:
                received = millis
            elif checks is None and CHECKS.match(message):
                checks = CHECKS.match(message)
            elif session is None and SESSION.match(message):
                session = (millis, SESSION.match(message))
        elif tag == "MacawEngines" and message.startswith("Loaded "):
            loads += 1
    if received is None or session is None:
        out["error"] = "startup log missing (a plugin build without StartupTiming, or logd dropped the lines)"
        return out
    _, match = session
    recording = match.group(2)
    engines = int(match.group(3)) + int(match.group(4))
    cold = bound if (created or bound) and not alive else 0
    # The binder opens after the checks; `ready` and the recording are logged from there.
    opened = received + (int(checks.group(1)) if checks else 0)
    plugin_ready = opened + int(match.group(1))
    out.update({
        "binding": received - t0 - cold,
        "cold": cold,
        "engines": engines,
        "queue": int(match.group(4)),
        "session": ready - received - engines,
        "checks": int(checks.group(1)) if checks else None,
        "runtime": f"{checks.group(2)} {checks.group(3)}" if checks else None,
        "start": int(match.group(1)) - engines,
        "delivery": ready - plugin_ready,
        "recording": (opened + int(recording)) - t0 if recording else None,
        "connect": connected - bind if bind is not None and connected is not None else None,
        "loads": loads,
        "processStarted": bool(created or bound),
    })
    return out


def describe(index, run):
    note = f"; {run['note']}" if run.get("note") else ""
    if "error" in run and "binding" not in run:
        return f"  run {index}: FAIL {run['error']} (plugin {'alive' if run.get('alive') else 'not running'} before){note}"
    if "binding" not in run:
        return f"  run {index}: FAIL {run['error']}{note}"
    connect = f", bindService->connected {run['connect']} ms" if run["connect"] is not None else ""
    recording = f", recording at {run['recording']} ms" if run["recording"] is not None else ""
    state = "alive" if run["alive"] else "not running"
    parts = (f" (checks {run['checks']}, runtime {run['runtime']} ms; start {run['start']}; delivery {run['delivery']})"
             if run["checks"] is not None else f" (start {run['start']}; delivery {run['delivery']})")
    prepare = (f"; prepare {run['prepare']} ms (loadMillis {run['loadMillis']}, lease {'given' if run['leased'] else 'missing'})"
               if "prepare" in run else "")
    return (f"  run {index}: ready {run['total']} ms = binding {run['binding']} + cold {run['cold']} + engines {run['engines']}"
            f" (queue {run['queue']}) + session {run['session']}{parts}{recording}{connect}; plugin {state} before, "
            f"engines loaded {run['loads']}, listen returned after {run['returned']} ms{prepare}{note}")


def summary(mode, runs, limit=None):
    good = [run for run in runs if "binding" in run]
    if not good:
        return f"{mode}: no complete run"
    median = {key: int(statistics.median(run[key] for run in good)) for key in ("total", "binding", "cold", "engines", "session")}
    line = (f"{mode}: median of {len(good)} / {len(runs)}: ready {median['total']} ms = binding {median['binding']} + cold "
            f"{median['cold']} + engines {median['engines']} + session {median['session']}")
    prepared = [run["prepare"] for run in good if "prepare" in run]
    if prepared:
        line += f"; prepare median {int(statistics.median(prepared))} ms"
    pss = [run["pssBefore"] for run in good if run.get("pssBefore")]
    if pss:
        line += f"; PSS before the listen {', '.join(megabytes(value) for value in pss)}"
    if limit is not None:
        line += f"; {'PASS' if median['total'] <= limit else 'FAIL'} against {limit} ms"
    return line


def warm_up(serial, label):
    bring_host(serial)
    print(describe(0, measure(serial)).replace("run 0", label), flush=True)


def prepare_runs(serial, count):
    """`prepare`: from an ended plugin process, the script prepares the model, then listens."""
    runs = []
    for number in range(1, count + 1):
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(3)
        print(f"  run {number}: plugin process {end_plugin(serial)}", flush=True)
        bring_host(serial)
        run = measure(serial, SCRIPT, {"before": PREPARE % {"streaming": STREAMING}})
        runs.append(run)
        print(describe(number, run), flush=True)
    return runs


def keep_runs(serial, count, idle):
    """`keep`: a warm-up session with keepLoaded true, M idle minutes in the background, the listen of the same script."""
    runs = []
    for number in range(1, count + 1):
        bring_host(serial)
        push_script(serial, KEEP_SCRIPT)
        name, warm = wait_json(serial, (f"{NAME}.json.warm", f"{NAME}.json"), 90)
        if not warm or "warmReady" not in warm:
            reason = (warm or {}).get("warmError") or (warm or {}).get("error") or "no result"
            run = {"error": f"warm-up failed: {reason}", "alive": bool(plugin_pid(serial))}
            runs.append(run)
            print(describe(number, run), flush=True)
            continue
        lease = "given" if warm.get("leased") else "missing"
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(idle * 60)
        alive = bool(plugin_pid(serial))
        state = hosts.process_state(serial, PLUGIN, PLUGIN)
        pss = pss_kb(serial)
        bring_host(serial)
        since = hosts.device_now(serial)
        adb(serial, "shell", "touch", f"{REMOTE}/{NAME}.json.go")
        run = analyse(serial, hosts.result(serial, NAME, 60), since, alive)
        run["note"] = f"warm-up lease {lease}; plugin {state} after {idle:g} min in the background, PSS {megabytes(pss)}"
        run["pssBefore"] = pss
        runs.append(run)
        print(describe(number, run), flush=True)
    return runs


def never_runs(serial, count, idle):
    """`never`: idleMinutes 0, a warm-up session, M idle minutes in the background, then the listen; PSS recorded."""
    original = read_preferences(serial)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(3)
    how = end_plugin(serial)
    if plugin_pid(serial):
        print(f"  FAIL the plugin process still runs ({how}); preferences not changed", flush=True)
        return []
    write_preferences(serial, with_idle(original, 0))
    print(f"  idleMinutes {idle_of(original)} -> {idle_of(read_preferences(serial))} (plugin process {how})", flush=True)
    runs = []
    try:
        for number in range(1, count + 1):
            bring_host(serial)
            warm = measure(serial)
            print(describe(0, warm).replace("run 0", f"run {number} warm-up"), flush=True)
            pss_warm = pss_kb(serial)
            adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
            time.sleep(idle * 60)
            state = hosts.process_state(serial, PLUGIN, PLUGIN)
            pss = pss_kb(serial)
            bring_host(serial)
            run = measure(serial)
            run["note"] = (f"PSS {megabytes(pss_warm)} after the warm-up, {megabytes(pss)} before the listen "
                           f"(plugin {state} after {idle:g} min in the background)")
            run["pssWarm"], run["pssBefore"] = pss_warm, pss
            runs.append(run)
            print(describe(number, run), flush=True)
    finally:
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(3)
        how = end_plugin(serial)
        write_preferences(serial, original)
        print(f"  idleMinutes restored to {idle_of(read_preferences(serial))} (plugin process {how})", flush=True)
    return runs


def main(argv):
    if not argv or argv[0].startswith("-"):
        print(__doc__)
        return 2
    serial, rest = argv[0], argv[1:]
    count, long_count, idle, limit = 3, 1, None, None
    modes = []
    index = 0
    while index < len(rest):
        if rest[index] == "--runs":
            count, index = int(rest[index + 1]), index + 2
        elif rest[index] == "--long-runs":
            long_count, index = int(rest[index + 1]), index + 2
        elif rest[index] == "--idle-minutes":
            idle, index = float(rest[index + 1]), index + 2
        elif rest[index] == "--limit":
            limit, index = int(rest[index + 1]), index + 2
        elif rest[index] in ("hot", "background", "kill", "prepare", "keep", "never"):
            modes.append(rest[index])
            index += 1
        else:
            print(__doc__)
            return 2
    modes = modes or ["hot", "background", "kill"]
    print(f"{hosts.label(serial)}: model {STREAMING}, {count} runs per starting point ({long_count} for keep / never)")
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    lines = []
    for mode in modes:
        print(f"{mode}:", flush=True)
        if mode == "prepare":
            warm_up(serial, "warm-up")
            lines.append(summary(mode, prepare_runs(serial, count), limit))
            continue
        if mode == "keep":
            lines.append(summary(mode, keep_runs(serial, long_count, 10.0 if idle is None else idle)))
            continue
        if mode == "never":
            lines.append(summary(mode, never_runs(serial, long_count, 30.0 if idle is None else idle)))
            continue
        runs = []
        warm_up(serial, "warm-up")
        for number in range(1, count + 1):
            if mode == "background":
                adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
                time.sleep((10.0 if idle is None else idle) * 60)
                bring_host(serial)
            elif mode == "kill":
                adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
                time.sleep(3)
                how = end_plugin(serial)
                print(f"  run {number}: plugin process {how}", flush=True)
                bring_host(serial)
            else:
                time.sleep(3)
                bring_host(serial)
            run = measure(serial)
            runs.append(run)
            print(describe(number, run), flush=True)
        lines.append(summary(mode, runs))
    adb(serial, "shell", "rm", "-f", f"{REMOTE}/{NAME}.js",
        *(f"{REMOTE}/{NAME}.json{suffix}" for suffix in ("", ".started", ".warm", ".go")), check=False)
    for line in lines:
        print(line)
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
