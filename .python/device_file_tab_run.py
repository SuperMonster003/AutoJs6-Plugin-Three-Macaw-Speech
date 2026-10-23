"""Device run of roadmap P7.8.4 E4 for 3-Macaw Speech: the maintainer recognizes a long audio file through the file tab
of the tryout screen with the system picker; this script prepares, watches and checks without touching the screen.

    py .python/device_file_tab_run.py push <serial> <file>      copies the file to /sdcard/Download/ and asks the media
                                                               scanner to list it, so the system picker shows it
    py .python/device_file_tab_run.py watch <serial> [name]     polls the plugin's notifications every 2 s until the run
                                                               ends (waits up to 6 hours for it to start, then at most
                                                               60 minutes) and prints each change: title,
                                                               progress text, action count, ongoing; fails when a
                                                               notification names the file (default: the name of the
                                                               cached record or the pushed file)
    py .python/device_file_tab_run.py check <serial> [srt]      compares the newest .srt in /sdcard/Download/ (or the
                                                               given device path) with the cached result record of the
                                                               file tab: state, cue count, times cue by cue, text cue by
                                                               cue, the last end against the declared duration

Output keeps counts, times and PASS / FAIL only; recognized text is never printed. The debug build is required (the
cached record is read with `run-as`). Install it first with `py .python/device_speech_run.py install <serial>`.
"""
import json
import re
import subprocess
import sys
import time
from pathlib import Path

PACKAGE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
DOWNLOAD = "/sdcard/Download"
RECORD = "cache/tryout/file-result.json"
RUNNING_ID = 0x4D46
RESULT_ID = 0x4D52
SRT_TIME = re.compile(r"^(\d{2}):(\d{2}):(\d{2}),(\d{3}) --> (\d{2}):(\d{2}):(\d{2}),(\d{3})$")
LINE_BREAKS = re.compile(r"\s*[\r\n]+\s*")


def adb(serial, *args, check=True):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, timeout=120)
    if check and result.returncode != 0:
        raise SystemExit(f"adb {' '.join(args)} failed: {result.stderr.decode('utf-8', 'replace').strip()}")
    return result.stdout.decode("utf-8", "replace")


def record(serial):
    text = adb(serial, "shell", f"run-as {PACKAGE} cat {RECORD}", check=False)
    try:
        return json.loads(text)
    except ValueError:
        return None


def notifications(serial):
    """The plugin's notification records: id, title, text, action count, ongoing and the whole block."""
    dump = adb(serial, "shell", "dumpsys notification --noredact")
    found = []
    for block in re.split(r"\n(?=\s*NotificationRecord\()", dump):
        head = re.search(r"NotificationRecord\([^:]+: pkg=(\S+) .*? id=(-?\d+)", block)
        if not head or head.group(1) != PACKAGE:
            continue
        title = re.search(r"android\.title=\S+ \((.*)\)\s*$", block, re.M)
        text = re.search(r"android\.text=\S+ \((.*)\)\s*$", block, re.M)
        # Hex on older releases, names such as ONGOING_EVENT|FOREGROUND_SERVICE on newer ones.
        # zh-CN: 旧版本是十六进制, 新版本是 ONGOING_EVENT|FOREGROUND_SERVICE 这样的名字.
        flags = re.search(r"^\s*flags=(\S+)", block, re.M)
        actions = re.search(r"actions=\{(.*?)\n\s*\}", block, re.S)
        found.append({
            "id": int(head.group(2)),
            "title": title.group(1) if title else None,
            "text": text.group(1) if text else None,
            "actions": len(re.findall(r"^\s*\[\d+\]", actions.group(1), re.M)) if actions else 0,
            "ongoing": bool(flags and ("ONGOING_EVENT" in flags.group(1) or flags.group(1).startswith("0x") and int(flags.group(1), 16) & 0x2)),
            "block": block,
        })
    return found


def push(serial, local):
    path = Path(local)
    adb(serial, "push", str(path), f"{DOWNLOAD}/{path.name}")
    adb(serial, "shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", f"file://{DOWNLOAD}/{path.name}")
    print(f"push {path.name} bytes={path.stat().st_size}: OK")


def watch(serial, name):
    names = {name, Path(name).stem} if name else set()
    started = time.monotonic()
    first_running = None
    last = None
    seen_running = False
    leaked = False
    # Up to 6 hours for the maintainer to start, then up to 60 minutes for the run.
    # zh-CN: 最多等 6 小时让维护者开始, 之后识别最多 60 分钟.
    while time.monotonic() - started < 6 * 3600 and (first_running is None or time.monotonic() - first_running < 3600):
        mine = [n for n in notifications(serial) if n["id"] in (RUNNING_ID, RESULT_ID)]
        for n in mine:
            if any(part and part in n["block"] for part in names):
                leaked = True
        state = tuple((n["id"], n["title"], n["text"], n["actions"], n["ongoing"]) for n in mine)
        if state != last:
            last = state
            elapsed = int(time.monotonic() - started)
            if not mine:
                print(f"  +{elapsed}s no notification")
            for n in mine:
                kind = "running" if n["id"] == RUNNING_ID else "result"
                print(f"  +{elapsed}s {kind} title={n['title']!r} text={n['text']!r} actions={n['actions']} ongoing={n['ongoing']}")
        running = any(n["id"] == RUNNING_ID for n in mine)
        if running and first_running is None:
            first_running = time.monotonic()
        seen_running = seen_running or running
        if seen_running and not running:
            break
        time.sleep(2)
    time.sleep(2)
    result = [n for n in notifications(serial) if n["id"] == RESULT_ID]
    for n in result:
        leaked = leaked or any(part and part in n["block"] for part in names)
        print(f"  end result title={n['title']!r} text={n['text']!r} actions={n['actions']} ongoing={n['ongoing']}")
    if not result:
        print("  end no result notification (a visible tryout screen shows the result itself)")
    print(f"watch running={'seen' if seen_running else 'never'} nameInNotification={leaked}: {'FAIL' if leaked or not seen_running else 'PASS'}")
    return 1 if leaked or not seen_running else 0


def srt_cues(text):
    cues = []
    for block in re.split(r"\r\n\r\n", text.strip("\r\n")):
        lines = block.split("\r\n")
        match = SRT_TIME.match(lines[1]) if len(lines) >= 3 else None
        if not match:
            raise SystemExit(f"malformed cue {len(cues) + 1}")
        h1, m1, s1, ms1, h2, m2, s2, ms2 = map(int, match.groups())
        cues.append((int(lines[0]), ((h1 * 60 + m1) * 60 + s1) * 1000 + ms1, ((h2 * 60 + m2) * 60 + s2) * 1000 + ms2, "\r\n".join(lines[2:])))
    return cues


def check(serial, srt):
    if not srt:
        listing = adb(serial, "shell", f"ls -t {DOWNLOAD}/*.srt", check=False).split()
        if not listing or "No such file" in listing[0]:
            raise SystemExit(f"no .srt in {DOWNLOAD}")
        srt = listing[0]
    raw = subprocess.run(["adb", "-s", serial, "exec-out", f"cat '{srt}'"], capture_output=True, timeout=120).stdout
    data = record(serial)
    if data is None:
        raise SystemExit("no cached record (run-as needs the debug build)")
    segments = [s for s in data["segments"] if s["text"].strip()]
    cues = srt_cues(raw.decode("utf-8"))
    problems = []
    if data["state"] != "COMPLETED":
        problems.append(f"state {data['state']}")
    if raw.startswith(b"\xef\xbb\xbf"):
        problems.append("BOM")
    if len(cues) != len(segments):
        problems.append(f"cues {len(cues)} != segments {len(segments)}")
    for index, (cue, segment) in enumerate(zip(cues, segments), 1):
        number, start, end, text = cue
        if number != index:
            problems.append(f"cue {index} numbered {number}")
        if start != max(segment["startMillis"], 0) or end != max(segment["endMillis"], segment["startMillis"], 0):
            problems.append(f"cue {index} times")
        if text != LINE_BREAKS.sub(" ", segment["text"].strip()):
            problems.append(f"cue {index} text")
        if index > 1 and start < cues[index - 2][1]:
            problems.append(f"cue {index} starts before cue {index - 1}")
    declared = data.get("declaredMillis")
    last_end = cues[-1][2] if cues else 0
    print(f"check srt={Path(srt).name} bytes={len(raw)} cues={len(cues)} segments={len(segments)} declared={declared} "
          f"recognized={data['recognizedMillis']} lastEnd={last_end} model={data['modelId']} punctuation={data['punctuation']} "
          f"warnings={len(data['warnings'])}: {'FAIL ' + '; '.join(problems[:5]) if problems else 'PASS'}")
    return 1 if problems else 0


def main(argv):
    if len(argv) < 2 or argv[0] not in ("push", "watch", "check"):
        print(__doc__)
        return 2
    action, serial = argv[0], argv[1]
    if action == "push":
        push(serial, argv[2])
        return 0
    if action == "watch":
        return watch(serial, argv[2] if len(argv) > 2 else (record(serial) or {}).get("displayName"))
    return check(serial, argv[2] if len(argv) > 2 else None)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
