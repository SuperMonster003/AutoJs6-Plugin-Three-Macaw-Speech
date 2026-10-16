"""Roadmap P5.6 (E4): 3-Stove Agent's microphone button through the system voice input of 3-Macaw Speech.

    py .python/device_stove_voice.py <serial> [out]

Needs 3-Stove Agent with its "Enable voice input" switch on, AutoJs6 (it plays the recording), RECORD_AUDIO granted to
3-Macaw Speech and an installed recognition model. The plugin's debug APK is installed with `adb install -r` (data and
models stay). An AutoJs6 script plays the English test recording through the speaker after a delay while Stove's task
console is in front; this script presses Stove's voice button (an ordinary button: never a permission dialog or a
chooser), waits for the sheet to return and compares the goal field with the reference text (CER). Only pass / fail and
the key numbers are printed, never the screen text; screenshots go to out (default .bak/visual/1.0.0/<serial>/, not in
the repository). When another recognizer also answers RECOGNIZE_SPEECH, Android shows its chooser first: the run stops
and the maintainer picks 3-Macaw Speech on the phone.
"""
import re
import sys
import tempfile
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from device_speech_run import APK_DIR, PACKAGE, ROOT, adb, device_time, label  # noqa: E402

STOVE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
HOST = "org.autojs.autojs6"
REMOTE = "/sdcard/Download/autojs6-macaw-stove"
DUMP = "/sdcard/Download/autojs6-macaw-stove-ui.xml"
AUDIO = ROOT / "app/src/androidTest/assets/test-audio/en.wav"
REFERENCE = "AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS"
DELAY_MILLIS = 12_000
LENGTH_MILLIS = 8_000
MAX_CER = 0.15

SCRIPT = """sleep(%(delay)d);
media.playMusic("%(dir)s/en.wav");
sleep(%(length)d);
media.stopMusic();
files.write("%(dir)s/played", String(Date.now()));
"""


def cer(reference, hypothesis):
    def normalize(text):
        return "".join(ch for ch in text.upper() if ch.isalnum())
    r, h = normalize(reference), normalize(hypothesis)
    previous = list(range(len(h) + 1))
    for i, rc in enumerate(r, 1):
        current = [i] + [0] * len(h)
        for j, hc in enumerate(h, 1):
            current[j] = min(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + (rc != hc))
        previous = current
    return previous[-1] / max(1, len(r))


def handlers(serial):
    """Components answering RECOGNIZE_SPEECH, as `package/class` with the class expanded."""
    out = adb(serial, "shell", "cmd", "package", "query-activities", "--components", "-a", "android.speech.action.RECOGNIZE_SPEECH", check=False)
    found = []
    for line in out.splitlines():
        package, _, name = line.strip().partition("/")
        if name:
            found.append(f"{package}/{package + name if name.startswith('.') else name}")
    return found


def nodes(serial):
    adb(serial, "shell", "uiautomator", "dump", DUMP, check=False)
    raw = adb(serial, "shell", "cat", DUMP, check=False)
    adb(serial, "shell", "rm", "-f", DUMP, check=False)
    try:
        return list(ElementTree.fromstring(raw[raw.index("<?xml"):] if "<?xml" in raw else raw).iter("node"))
    except (ElementTree.ParseError, ValueError):
        return []


def node(serial, view_id):
    return next((n for n in nodes(serial) if n.get("resource-id", "").endswith(f":id/{view_id}")), None)


def center(element):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", element.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def screenshot(serial, target, name):
    target.mkdir(parents=True, exist_ok=True)
    remote = f"{REMOTE}/{name}.png"
    adb(serial, "shell", "mkdir", "-p", REMOTE, check=False)
    adb(serial, "shell", "screencap", "-p", remote, check=False)
    adb(serial, "pull", remote, str(target / f"{name}.png"), check=False)


def resumed(serial):
    dump = adb(serial, "shell", "dumpsys", "activity", "activities", check=False)
    match = re.search(r"(?:mResumedActivity|topResumedActivity)\S*[:=]\s*ActivityRecord\{\S+ \S+ (\S+)", dump)
    return match.group(1) if match else ""


def launcher(serial):
    return adb(serial, "shell", "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", STOVE).splitlines()[-1].strip()


def music_volume(serial, value=None):
    if value is not None:
        adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "3", "--set", str(value), check=False)
        return None
    match = re.search(r"volume is (\d+) in range \[(\d+)\.\.(\d+)\]", adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "3", "--get", check=False))
    return (int(match.group(1)), int(match.group(3))) if match else None


def run(serial, out):
    name = label(serial)
    if not adb(serial, "shell", "pm", "path", STOVE, check=False):
        print(f"{name}: FAIL: 3-Stove Agent is not installed")
        return False
    abi = adb(serial, "shell", "getprop", "ro.product.cpu.abi")
    apk = next((APK_DIR / "debug").glob(f"*-{abi}.apk"), None) or next((APK_DIR / "debug").glob("*-universal.apk"))
    adb(serial, "install", "-r", str(apk))
    found = handlers(serial)
    ours = f"{PACKAGE}/{PACKAGE}.ui.voice.VoiceInputActivity"
    print(f"{name}: RECOGNIZE_SPEECH handlers {len(found)}, 3-Macaw Speech {'present' if ours in found else 'missing'}")
    if ours not in found:
        print(f"{name}: FAIL: the voice input is not offered (switched off in the settings?)")
        return False
    if len(found) > 1:
        print(f"{name}: STOP: another recognizer answers too; Android shows its chooser, which the maintainer handles on the phone")
        return False
    target = Path(out) if out else ROOT / ".bak" / "visual" / "1.0.0" / serial.replace(":", "_")
    volume = music_volume(serial)
    since = device_time(serial)
    adb(serial, "shell", "rm", "-rf", REMOTE, check=False)
    adb(serial, "shell", "mkdir", "-p", REMOTE)
    try:
        if volume:
            music_volume(serial, max(volume[0], volume[1] * 70 // 100))
        adb(serial, "push", str(AUDIO), f"{REMOTE}/en.wav")
        with tempfile.TemporaryDirectory() as folder:
            local = Path(folder) / "play.js"
            local.write_text(SCRIPT % {"delay": DELAY_MILLIS, "length": LENGTH_MILLIS, "dir": REMOTE}, encoding="utf-8")
            adb(serial, "push", str(local), f"{REMOTE}/play.js")
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
        started = time.time()
        adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
            "-d", f"file://{REMOTE}/play.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
        time.sleep(2)
        adb(serial, "shell", "am", "start", "-n", launcher(serial))
        time.sleep(2)
        button = node(serial, "workbench_voice")
        if button is None:
            print(f"{name}: FAIL: Stove shows no voice button (turn on its 'Enable voice input' setting)")
            return False
        adb(serial, "shell", "input", "tap", *map(str, center(button)))
        time.sleep(3)
        sheet = resumed(serial)
        screenshot(serial, target, "stove-voice-sheet")
        print(f"{name}: voice button pressed, top activity {'3-Macaw Speech sheet' if 'VoiceInputActivity' in sheet else sheet or 'unknown'}")
        deadline = started + (DELAY_MILLIS + LENGTH_MILLIS) / 1000 + 30
        while time.time() < deadline and not adb(serial, "shell", "ls", f"{REMOTE}/played", check=False).endswith("played"):
            time.sleep(1)
        time.sleep(4)
        screenshot(serial, target, "stove-voice-result")
        goal = node(serial, "workbench_goal")
        text = goal.get("text", "") if goal is not None else ""
        log = adb(serial, "logcat", "-d", "-v", "raw", "-T", since, "-s", "MacawVoiceInput:I", check=False)
        codes = re.findall(r"Voice input result (-?\d+) \((\d+) chars\)", log)
        score = cer(REFERENCE, text)
        back = STOVE in resumed(serial)
        print(f"{name}: sheet result {codes[-1][0] if codes else 'none'} ({codes[-1][1] if codes else 0} chars), back in Stove {back}, goal chars {len(text)}, CER {score:.3f}")
        ok = bool(codes) and codes[-1][0] == "-1" and back and score <= MAX_CER
        print(f"{name}: Stove voice input {'PASS' if ok else 'FAIL'}; screenshots in {target}")
        return ok
    finally:
        if volume:
            music_volume(serial, volume[0])
        adb(serial, "shell", "rm", "-rf", REMOTE, check=False)


def main(argv):
    if not argv:
        print(__doc__)
        return 2
    return 0 if run(argv[0], argv[1] if len(argv) > 1 else None) else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
