"""The time to `ready` of the first `speech.listen` and its four parts (roadmap P11.1.1, F9, appendix F).

    py .python/f9_startup_timing.py <serial> [hot] [background] [kill] [--runs N] [--idle-minutes M]

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

The host comes to the foreground through its launcher before every run (a frozen host would not start the script).
Output: one line per run and the medians per starting point; no transcript, no microphone audio is needed.
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


def run_script(serial):
    adb(serial, "shell", "rm", "-f", f"{REMOTE}/{NAME}.json", f"{REMOTE}/{NAME}.json.started", check=False)
    script = SCRIPT % {"out": f"{REMOTE}/{NAME}.json", "streaming": STREAMING}
    local = Path(tempfile.gettempdir()) / f"macaw-{NAME}-{serial.replace(':', '_')}.js"
    local.write_text(script, encoding="utf-8")
    adb(serial, "push", str(local), f"{REMOTE}/{NAME}.js")
    local.unlink()
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/{NAME}.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    return hosts.result(serial, NAME, 60)


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


def measure(serial):
    """One run: the script's times and the parts read from the log, or an `error` entry."""
    alive = bool(plugin_pid(serial))
    since = hosts.device_now(serial)
    r = run_script(serial)
    if not r or "ready" not in r:
        return {"error": (r or {}).get("error", "no result"), "alive": alive}
    time.sleep(1)
    t0, ready = r["t0"], r["ready"]
    out = {"alive": alive, "total": ready - t0, "returned": r["returned"] - t0}
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
    if "error" in run and "binding" not in run:
        return f"  run {index}: FAIL {run['error']} (plugin {'alive' if run.get('alive') else 'not running'} before)"
    if "binding" not in run:
        return f"  run {index}: FAIL {run['error']}"
    connect = f", bindService->connected {run['connect']} ms" if run["connect"] is not None else ""
    recording = f", recording at {run['recording']} ms" if run["recording"] is not None else ""
    state = "alive" if run["alive"] else "not running"
    parts = (f" (checks {run['checks']}, runtime {run['runtime']} ms; start {run['start']}; delivery {run['delivery']})"
             if run["checks"] is not None else f" (start {run['start']}; delivery {run['delivery']})")
    return (f"  run {index}: ready {run['total']} ms = binding {run['binding']} + cold {run['cold']} + engines {run['engines']}"
            f" (queue {run['queue']}) + session {run['session']}{parts}{recording}{connect}; plugin {state} before, "
            f"engines loaded {run['loads']}, listen returned after {run['returned']} ms")


def summary(mode, runs):
    good = [run for run in runs if "binding" in run]
    if not good:
        return f"{mode}: no complete run"
    median = {key: int(statistics.median(run[key] for run in good)) for key in ("total", "binding", "cold", "engines", "session")}
    return (f"{mode}: median of {len(good)} / {len(runs)}: ready {median['total']} ms = binding {median['binding']} + cold "
            f"{median['cold']} + engines {median['engines']} + session {median['session']}")


def main(argv):
    if not argv or argv[0].startswith("-"):
        print(__doc__)
        return 2
    serial, rest = argv[0], argv[1:]
    count, idle = 3, 10.0
    modes = []
    index = 0
    while index < len(rest):
        if rest[index] == "--runs":
            count, index = int(rest[index + 1]), index + 2
        elif rest[index] == "--idle-minutes":
            idle, index = float(rest[index + 1]), index + 2
        elif rest[index] in ("hot", "background", "kill"):
            modes.append(rest[index])
            index += 1
        else:
            print(__doc__)
            return 2
    modes = modes or ["hot", "background", "kill"]
    print(f"{hosts.label(serial)}: model {STREAMING}, {count} runs per starting point")
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    lines = []
    for mode in modes:
        print(f"{mode}:", flush=True)
        runs = []
        bring_host(serial)
        warm = measure(serial)
        print(describe(0, warm).replace("run 0", "warm-up"), flush=True)
        for number in range(1, count + 1):
            if mode == "background":
                adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
                time.sleep(idle * 60)
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
    adb(serial, "shell", "rm", "-f", f"{REMOTE}/{NAME}.js", f"{REMOTE}/{NAME}.json", f"{REMOTE}/{NAME}.json.started", check=False)
    for line in lines:
        print(line)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
