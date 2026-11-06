"""Device checks of roadmap P11.3 (the actions of the resident wake words and `speech.resident`); one pass / fail line per
item with the key numbers, never a transcript, a token or the recognized text.

    py .python/device_resident_actions.py run <serial>       AutoJs6 6.8.1+ (the run receiver of D220) and a debug build
                                                             of 3-Macaw Speech: `speech.resident.set / get` from a
                                                             script (and its checks), the switch from a visible state,
                                                             then hits of the debug receiver: "Run script" 3 times in the
                                                             background and 3 times with the screen off and locked (the
                                                             target script writes its execArgv), "Send broadcast" to an
                                                             intent task of the host, and a script's `on('hit')`
    py .python/device_resident_actions.py refused <serial>   `speech.resident.enable()` from a script while AutoJs6 and
                                                             the plugin are in the background and the screen is off:
                                                             Android 12+ refuses the microphone foreground service, so
                                                             `{ started: false, restartNeeded: true }`, the switch stays
                                                             on, "tap to resume" is posted, and opening the plugin's
                                                             settings starts the service (D189, D227)
    py .python/device_resident_actions.py refused <serial> --host-foreground off
                                                             the same without AutoJs6's foreground service, for an AVD
                                                             whose cached apps freezer is off: the start must be refused
                                                             (with the service, the default, the system may let it
                                                             through; the microphone must then be in use, not refused)
    py .python/device_resident_actions.py fallback <serial>  an AutoJs6 without the run receiver (6.8.0 and earlier):
                                                             a "Run script" hit in the background opens RunIntentActivity
                                                             where the plugin may (the target script writes its file) or
                                                             posts "Tap to run the script" (D222); rules through the debug
                                                             switch, since such a host has no `speech.resident`

Hits come from the debug build's ResidentDebugHitReceiver (DUMP permission, adb only) while the service runs, so nothing
plays through the speaker. The stored rules and the switch are put back at the end. Nothing is tapped: a dialog of the
system is left to the maintainer. The scripts start through AutoJs6's RunIntentActivity (device_host_scripts.py).
"""
import json
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hs  # noqa: E402

PLUGIN = hs.PLUGIN
HOST = hs.HOST
REMOTE = hs.REMOTE
DEBUG_HIT = f"{PLUGIN}/.ResidentDebugHitReceiver"
DEBUG_SWITCH = f"{PLUGIN}/.ResidentDebugActivity"
SETTINGS = f"{PLUGIN}/.ui.settings.ResidentSettingsAlias"
RUN_RECEIVER = "org.autojs.autojs6.action.SPEECH_RESIDENT_RUN"
HIT_ACTION = "org.autojs.plugin.speech.action.RESIDENT_HIT"
WORD_SCRIPT = "Hey Macaw"
WORD_BROADCAST = "LOVELY CHILD"
WORD_NOTIFY = "LIGHT UP"
RESUME_ID = 0x5255
# The stored rules of a fresh install (ResidentRules.DEFAULT), JSON with ASCII escapes so that adb keeps them intact.
# zh-CN: 新安装时的规则 (ResidentRules.DEFAULT), 用 ASCII 转义的 JSON, 以免经 adb 时被改坏.
DEFAULT_RULES = json.dumps([{"keyword": "小鹦小鹦", "action": "open"}, {"keyword": "Hey Macaw", "action": "open"}],
                           separators=(",", ":"))
RUN_ID = 0x5256

TARGET = """
var argv = engines.myEngine().execArgv;
var out = { keyword: argv.keyword, matchedText: argv.matchedText, arguments: argv.arguments, timeMillis: argv.timeMillis, source: argv.source,
    intentKeyword: null, at: Date.now() };
try { var i = argv.intent; if (i) out.intentKeyword = String(i.getStringExtra("keyword")); } catch (e) {}
files.write("%(dir)s/%(name)s-" + Date.now() + ".json", JSON.stringify(out));
"""

SETUP = hs.COMMON + """
function code(fn) { try { fn(); return "returned"; } catch (e) { return String(e.code || e); } }
try {
    r.before = speech.resident.get();
    r.features = speech.capabilities().features.join(",");
    var rules = [
        { keyword: "%(script_word)s", action: "script", script: "%(dir)s/target.js", arguments: "from-p11-3" },
        { keyword: "%(broadcast_word)s", action: "broadcast" },
        { keyword: "%(notify_word)s", action: "notify" },
    ];
    r.eleven = code(function () { var many = []; for (var i = 0; i < 11; i++) many.push("word " + i); speech.resident.set(many); });
    r.noPath = code(function () { speech.resident.set([{ keyword: "x", action: "script" }]); });
    r.badAction = code(function () { speech.resident.set([{ keyword: "x", action: "launch" }]); });
    r.repeat = code(function () { speech.resident.set(["Hey Macaw", "hey macaw"]); });
    r.relative = speech.resident.set([{ keyword: "x y", action: "script", script: "./a.js" }]).rules[0].script;
    r.ten = speech.resident.set(["LIGHT UP", "LOVELY CHILD", "Hey Macaw", "\\u6cd5\\u56fd", "\\u5973\\u513f", "\\u843d\\u5b9e",
        "\\u89c1\\u9762\\u4f1a", "\\u5468\\u671b\\u519b", "\\u848b\\u53cb\\u4f2f", "\\u6587\\u68ee\\u7279\\u5361\\u7d22"]).rules.length;
    r.after = speech.resident.set(rules);
    r.task = tasks.addIntentTask({ path: "%(dir)s/broadcast-target.js", action: "%(hit_action)s" }).id;
    done();
} catch (e) { fail(e); }
"""

LISTEN = hs.COMMON + """
try {
    r.hits = [];
    speech.resident.on("hit", function (hit) {
        r.hits.push(hit);
        speech.resident.off("hit");
        done();
    });
    files.write("%(dir)s/listening", "1");
    setTimeout(function () { r.errors.push("no hit"); speech.resident.off("hit"); done(); }, 30000);
} catch (e) { fail(e); }
"""

RESTORE = hs.COMMON + """
try {
    var before = JSON.parse(files.read("%(dir)s/before.json"));
    r.restored = speech.resident.set(before.rules).rules.length;
    if (!before.enabled) r.disabled = speech.resident.disable();
    %(remove_task)s
    done();
} catch (e) { fail(e); }
"""

ENABLE_LATER = hs.COMMON + """
try {
    r.before = speech.resident.get();
    r.offFirst = speech.resident.disable();
    // AutoJs6 without its foreground service is frozen in the background on Android 14+ (cached apps freezer), so the
    // script would only go on once something brings it back; the switch is put back below.
    // zh-CN: 没有前台服务的 AutoJs6 在 Android 14+ 后台会被冻结, 脚本要等被唤回才继续; 下面恢复原值.
    r.foregroundBefore = $settings.isEnabled("foreground_service");
    $settings.setEnabled("foreground_service", %(foreground)s);
    files.write("%(dir)s/waiting", "1");
    var deadline = Date.now() + 60000;
    while (!files.exists("%(dir)s/go") && Date.now() < deadline) sleep(200);
    r.enabledAt = Date.now();
    r.result = speech.resident.enable();
    r.enabledFlag = speech.resident.enabled;
    $settings.setEnabled("foreground_service", r.foregroundBefore);
    done();
} catch (e) { fail(e); }
"""


def adb(serial, *args, check=False):
    return hs.adb(serial, *args, check=check)


def write_script(serial, name, text):
    local = Path(hs.tempfile.gettempdir()) / f"macaw-p113-{name}-{serial.replace(':', '_')}.js"
    local.write_text(text, encoding="utf-8")
    adb(serial, "push", str(local), f"{REMOTE}/{name}.js", check=True)


def run_host_script(serial, name, text, seconds=60):
    """Pushes [text] as `<name>.js`, starts it through RunIntentActivity and answers its JSON result (or None)."""
    write_script(serial, name, text % {"out": f"{REMOTE}/{name}.json", "dir": REMOTE})
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/{name}.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    return hs.result(serial, name, seconds)


def outputs(serial, prefix):
    listing = adb(serial, "shell", "ls", REMOTE)
    return sorted(name for name in listing.split() if name.startswith(prefix + "-") and name.endswith(".json"))


def read_json(serial, name):
    raw = adb(serial, "shell", "cat", f"{REMOTE}/{name}")
    return json.loads(raw) if raw.startswith("{") else None


def hit(serial, keyword):
    adb(serial, "shell", "am", "broadcast", "-n", DEBUG_HIT, "--es", "keyword", f"'{keyword}'")


def wait_new(serial, prefix, known, seconds=20):
    deadline = time.time() + seconds
    while time.time() < deadline:
        new = [name for name in outputs(serial, prefix) if name not in known]
        if new:
            return new[0]
        time.sleep(1)
    return None


def service_running(serial):
    dump = adb(serial, "shell", "dumpsys", "activity", "services", f"{PLUGIN}/.resident.ResidentWakeService")
    return "ServiceRecord{" in dump and "isForeground=true" in dump


def notification_posted(serial, notification_id):
    dump = adb(serial, "shell", "dumpsys", "notification", "--noredact")
    return any(f"pkg={PLUGIN}" in line and f"id={notification_id} " in line for line in dump.splitlines())


def has_run_receiver(serial):
    dump = adb(serial, "shell", "pm", "query-receivers", "-a", RUN_RECEIVER, HOST)
    return "SpeechResidentRunReceiver" in dump


def versions(serial):
    def version(package):
        dump = adb(serial, "shell", "dumpsys", "package", package)
        code = next((line.split("versionCode=")[1].split()[0] for line in dump.splitlines() if "versionCode=" in line), "?")
        name = next((line.split("versionName=")[1].strip() for line in dump.splitlines() if "versionName=" in line), "?")
        return f"{name} ({code})"
    return f"host {version(HOST)}, plugin {version(PLUGIN)}"


def item(results, name, ok, detail=""):
    results.append(ok)
    print(f"    {'PASS' if ok else 'FAIL'} {name}" + (f": {detail}" if detail else ""))


def lock(serial):
    adb(serial, "shell", "input", "keyevent", "KEYCODE_SLEEP")
    time.sleep(4)


def unlock(serial):
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "wm", "dismiss-keyguard")
    time.sleep(1)


def prepare(serial):
    adb(serial, "shell", "rm", "-rf", REMOTE)
    adb(serial, "shell", "mkdir", "-p", REMOTE, check=True)
    write_script(serial, "target", TARGET % {"dir": REMOTE, "name": "run"})
    write_script(serial, "broadcast-target", TARGET % {"dir": REMOTE, "name": "broadcast"})


def run(serial):
    print(f"{hs.label(serial)}: {versions(serial)}")
    results = []
    if not has_run_receiver(serial):
        print("    FAIL the host has no SpeechResidentRunReceiver (AutoJs6 6.8.1+ needed)")
        return False
    prepare(serial)
    unlock(serial)
    script = SETUP.replace("%(script_word)s", WORD_SCRIPT).replace("%(broadcast_word)s", WORD_BROADCAST)         .replace("%(notify_word)s", WORD_NOTIFY).replace("%(hit_action)s", HIT_ACTION)
    setup = run_host_script(serial, "setup", script)
    if setup is None or setup.get("errors"):
        print(f"    FAIL setup: {None if setup is None else setup['errors']}")
        return False
    before_local = Path(hs.tempfile.gettempdir()) / f"macaw-p113-before-{serial.replace(':', '_')}.json"
    before_local.write_text(json.dumps(setup["before"], ensure_ascii=False), encoding="utf-8")
    adb(serial, "push", str(before_local), f"{REMOTE}/before.json", check=True)
    task = setup.get("task")
    try:
        item(results, "features", "speechResident" in setup["features"], setup["features"])
        item(results, "set: 11 rules", setup["eleven"] == "LIMIT_EXCEEDED", setup["eleven"])
        item(results, "set: script without path", setup["noPath"] == "INVALID_OPTIONS", setup["noPath"])
        item(results, "set: unknown action", setup["badAction"] == "INVALID_OPTIONS", setup["badAction"])
        item(results, "set: repeated word", setup["repeat"] == "INVALID_OPTIONS", setup["repeat"])
        item(results, "set: relative path resolved", str(setup["relative"]).startswith("/") and setup["relative"].endswith("/a.js"), setup["relative"])
        item(results, "set: 10 rules", setup["ten"] == 10, str(setup["ten"]))
        actions = [rule["action"] for rule in setup["after"]["rules"]]
        item(results, "get after set", actions == ["script", "broadcast", "notify"], ",".join(actions))

        adb(serial, "shell", "am", "start", "-n", DEBUG_SWITCH, "--es", "resident", "on")
        time.sleep(6)
        item(results, "switch on (visible start)", service_running(serial))

        # "Run script" in the background, then with the screen off and locked (D220).
        for state in ("background", "locked"):
            if state == "background":
                adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
            else:
                lock(serial)
            for i in range(3):
                time.sleep(2)
                known = outputs(serial, "run")
                started = time.time()
                hit(serial, WORD_SCRIPT)
                name = wait_new(serial, "run", known)
                value = read_json(serial, name) if name else None
                ok = value is not None and value.get("keyword") == WORD_SCRIPT and value.get("source") == "resident-wake" \
                    and value.get("arguments") == "from-p11-3" and value.get("intentKeyword") == WORD_SCRIPT
                item(results, f"run script, {state} #{i + 1}", ok,
                     f"{round(time.time() - started, 1)} s" if value else "no output in 20 s")
            if state == "locked":
                unlock(serial)

        # "Send broadcast" to the intent task of the host (D194, D225).
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(2)
        known = outputs(serial, "broadcast")
        hit(serial, WORD_BROADCAST)
        name = wait_new(serial, "broadcast", known, 30)
        value = read_json(serial, name) if name else None
        item(results, "broadcast to an intent task", value is not None and value.get("intentKeyword") == WORD_BROADCAST,
             "received" if value else "no task run in 30 s")

        # on('hit') of a script, whatever the action (D194, D226).
        adb(serial, "shell", "rm", "-f", f"{REMOTE}/listening")
        write_script(serial, "listen", LISTEN % {"out": f"{REMOTE}/listen.json", "dir": REMOTE})
        adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
            "-d", f"file://{REMOTE}/listen.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
        for _ in range(15):
            if adb(serial, "shell", "ls", f"{REMOTE}/listening").endswith("listening"):
                break
            time.sleep(1)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(2)
        hit(serial, WORD_NOTIFY)
        listened = hs.result(serial, "listen", 40)
        got = (listened or {}).get("hits") or []
        ok = len(got) == 1 and got[0].get("keyword") == WORD_NOTIFY and got[0].get("action") == "notify" and got[0].get("timeMillis", 0) > 0
        item(results, "on('hit')", ok, json.dumps({k: v for k, v in (got[0] if got else {}).items() if k != "matchedText"}))
        item(results, "notify action posts tap to speak", notification_posted(serial, 0x5253))
    finally:
        unlock(serial)
        remove = f'r.removed = tasks.removeIntentTask({task});' if task is not None else ""
        restored = run_host_script(serial, "restore", RESTORE.replace("%(remove_task)s", remove))
        print(f"    restore: {restored and {k: restored.get(k) for k in ('restored', 'disabled', 'removed', 'errors')}}")
        adb(serial, "shell", "rm", "-rf", REMOTE)
    ok = all(results)
    print(f"{hs.label(serial)}: run {'PASS' if ok else 'FAIL'} ({results.count(True)} / {len(results)})")
    return ok


def microphone_access(serial):
    """Seconds since the plugin last used and last was refused the microphone (appops), or None when never."""
    line = adb(serial, "shell", "appops", "get", PLUGIN, "RECORD_AUDIO")

    def ago(name):
        match = re.search(name + r"=\+(?:(\d+)d)?(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?(?:(\d+)ms)? ago", line)
        if not match:
            return None
        d, h, m, sec, ms = (int(v or 0) for v in match.groups())
        return d * 86400 + h * 3600 + m * 60 + sec + ms / 1000
    return ago("time"), ago("rejectTime")


def refused(serial, host_foreground=True):
    """`host_foreground` False needs a device without the cached apps freezer (an AVD with it turned off), since a
    script of AutoJs6 in the background without its foreground service is frozen otherwise; then the start must be
    refused. With it (the default), the system may let the start through, as the plugin is bound by a process with a
    foreground service; the item then checks that the microphone is used and not refused (while-in-use, D227).
    zh-CN: host_foreground 为 False 时需要关掉缓存应用冻结器的设备 (AVD), 否则没有前台服务的 AutoJs6 脚本在后台会被冻结;
    此时启动必须被拒. 为 True (默认) 时系统可能放行 (插件被持有前台服务的进程绑定), 此时检查麦克风确实在用且未被拒."""
    print(f"{hs.label(serial)}: {versions(serial)}; AutoJs6 foreground service {'on' if host_foreground else 'off'}")
    results = []
    prepare(serial)
    unlock(serial)
    write_script(serial, "later", ENABLE_LATER % {"out": f"{REMOTE}/later.json", "dir": REMOTE,
                                                  "foreground": "true" if host_foreground else "false"})
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/later.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    for _ in range(20):
        if adb(serial, "shell", "ls", f"{REMOTE}/waiting").endswith("waiting"):
            break
        time.sleep(1)
    adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
    lock(serial)
    time.sleep(12)
    adb(serial, "shell", "touch", f"{REMOTE}/go")
    later = hs.result(serial, "later", 40)
    try:
        if later is None or later.get("errors"):
            item(results, "enable() from the background", False, str(None if later is None else later["errors"]))
            return False
        answer = later.get("result") or {}
        if answer.get("started") is True and host_foreground:
            time.sleep(3)
            used, rejected = microphone_access(serial)
            item(results, "start allowed (bound by a foreground service)", service_running(serial) and answer.get("restartNeeded") is False,
                 json.dumps(answer))
            item(results, "microphone used, not refused", used is not None and used < 10 and (rejected is None or rejected > used),
                 f"last use {used} s ago, last refusal {rejected} s ago")
            return finish_refused(serial, results)
        item(results, "enable() answers started false", answer.get("started") is False, json.dumps(answer))
        item(results, "restartNeeded true", answer.get("restartNeeded") is True)
        item(results, "switch remembered", later.get("enabledFlag") is True or "true" in adb(serial, "shell", "pm", "dump", PLUGIN).lower())
        item(results, "tap to resume posted", notification_posted(serial, RESUME_ID))
        unlock(serial)
        adb(serial, "shell", "am", "start", "-n", SETTINGS)
        time.sleep(8)
        item(results, "opening the settings starts it", service_running(serial))
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
    finally:
        before = (later or {}).get("before") or {}
        if not before.get("enabled", False):
            adb(serial, "shell", "am", "start", "-n", DEBUG_SWITCH, "--es", "resident", "off")
            time.sleep(2)
            adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        adb(serial, "shell", "rm", "-rf", REMOTE)
    return finish_refused(serial, results)


def finish_refused(serial, results):
    ok = all(results)
    print(f"{hs.label(serial)}: refused {'PASS' if ok else 'FAIL'} ({results.count(True)} / {len(results)})")
    return ok


def fallback(serial):
    print(f"{hs.label(serial)}: {versions(serial)}")
    results = []
    if has_run_receiver(serial):
        print("    FAIL the host has the run receiver; fallback needs AutoJs6 6.8.0 or earlier")
        return False
    prepare(serial)
    unlock(serial)
    rules = json.dumps([{"keyword": WORD_SCRIPT, "action": "script", "script": f"{REMOTE}/target.js", "arguments": "from-p11-3"}], separators=(",", ":"))
    overlay = adb(serial, "shell", "appops", "get", PLUGIN, "SYSTEM_ALERT_WINDOW")
    try:
        adb(serial, "shell", "am", "start", "-n", DEBUG_SWITCH, "--es", "resident", "on", "--es", "rules", "'" + rules + "'")
        time.sleep(6)
        item(results, "switch on", service_running(serial))
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(3)
        known = outputs(serial, "run")
        hit(serial, WORD_SCRIPT)
        name = wait_new(serial, "run", known, 15)
        value = read_json(serial, name) if name else None
        if value is not None:
            item(results, "RunIntentActivity ran the script", value.get("intentKeyword") == WORD_SCRIPT, f"overlay: {overlay.strip()}")
        else:
            item(results, "tap to run posted", notification_posted(serial, RUN_ID), f"overlay: {overlay.strip()}")
    finally:
        adb(serial, "shell", "am", "start", "-n", DEBUG_SWITCH, "--es", "resident", "off", "--es", "rules", "'" + DEFAULT_RULES + "'")
        time.sleep(2)
        adb(serial, "shell", "input", "keyevent", "KEYCODE_HOME")
        adb(serial, "shell", "rm", "-rf", REMOTE)
    ok = all(results)
    print(f"{hs.label(serial)}: fallback {'PASS' if ok else 'FAIL'} ({results.count(True)} / {len(results)})")
    return ok


def main(argv):
    if argv[:1] == ["refused"] and argv[2:] == ["--host-foreground", "off"]:
        return 0 if refused(argv[1], host_foreground=False) else 1
    if len(argv) != 2 or argv[0] not in ("run", "refused", "fallback"):
        print(__doc__)
        return 2
    return 0 if {"run": run, "refused": refused, "fallback": fallback}[argv[0]](argv[1]) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
