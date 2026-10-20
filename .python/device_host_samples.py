"""Runs the four AutoJs6 samples of 3-Macaw Speech on one device (roadmap P6.3, host sample/3-Macaw Speech/).

    py .python/device_host_samples.py <serial> [dictation|wake|tts|agent ...] [--host <AutoJs6 checkout>]

The sample files are read from the host checkout (default D:/idea-projects/AutoJs6, app/src/main/assets-app/sample/
3-Macaw Speech/) and pushed with the test recordings; a small wrapper script starts each sample through
engines.execScriptFile with test arguments (`output` for a JSON summary, `keyword` 朱丽楠 for the wake samples,
`rounds` 1, `file` in the run directory) and plays the recording through the speaker:

    dictation  听写到文件: en.wav while the dictation runs; at least one sentence written, CER of the file
    wake       唤醒词与听写: the keyword twice and the mixed command (wake.wav of device_host_scripts.py); one round
    tts        TTS 播报: synthesis to a file, media playback, speech.speak and tts with the plugin engine
    agent      语音命令交给 Agent: the same recording; the command goes to ai.agent.run (observe tools only) and the
               summary is spoken; the Agent's status or error code is reported as it is

The music volume is raised to MACAW_VOLUME_PERCENT (70 by default) and restored. Output: PASS / FAIL per sample with
the key numbers; recognized text and Agent summaries are not printed. Unlock the device first; nothing answers a dialog.
"""
import argparse
import json
import os
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import device_host_scripts as hs  # noqa: E402
from device_stove_voice import REFERENCE, cer  # noqa: E402

SAMPLES = {
    "dictation": ("听写到文件 [v6.8.0+].js", "en.wav", 4000, 120),
    "wake": ("唤醒词与听写 [v6.8.0+].js", "wake.wav", 4000, 120),
    "tts": ("TTS 播报 [v6.8.0+].js", "", 0, 180),
    "agent": ("语音命令交给 Agent [v6.8.0+].js", "wake.wav", 4000, 300),
}
REMOTE_SAMPLES = hs.REMOTE + "/samples"

WRAPPER = """
var out = "%(out)s";
files.write(out + ".started", String(Date.now()));
engines.execScriptFile("%(sample)s", { arguments: { output: "%(result)s", keyword: "朱丽楠", rounds: 1, file: "%(dir)s/dictation.txt" } });
if ("%(audio)s") {
    sleep(%(delay)d);
    media.playMusic("%(dir)s/%(audio)s", 1, false);
    sleep(16000);
    media.stopMusic();
}
files.write(out, "{}");
"""


def push_samples(serial, host):
    source = Path(host) / "app/src/main/assets-app/sample/3-Macaw Speech"
    hs.adb(serial, "shell", "mkdir", "-p", REMOTE_SAMPLES)
    for name, *_ in SAMPLES.values():
        hs.adb(serial, "push", str(source / name), f"{REMOTE_SAMPLES}/{name}")


def run_sample(serial, key):
    name, audio, delay, seconds = SAMPLES[key]
    result = f"{hs.REMOTE}/{key}-result.json"
    values = {"out": f"{hs.REMOTE}/{key}.json", "sample": f"{REMOTE_SAMPLES}/{name}", "result": result, "dir": hs.REMOTE,
              "audio": audio, "delay": delay}
    local = Path(os.environ.get("TEMP", ".")) / f"macaw-sample-{key}-{serial.replace(':', '_')}.js"
    local.write_text(WRAPPER % values, encoding="utf-8")
    hs.adb(serial, "push", str(local), f"{hs.REMOTE}/{key}.js")
    local.unlink(missing_ok=True)
    hs.adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    hs.adb(serial, "shell", "am", "start", "-n", hs.launcher(serial))
    time.sleep(2)
    started = time.time()
    hs.adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
           "-d", f"file://{hs.REMOTE}/{key}.js", "-n", f"{hs.HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    deadline = time.time() + seconds
    data = None
    while time.time() < deadline:
        raw = hs.adb(serial, "shell", "cat", result, check=False)
        if raw.startswith("{"):
            data = json.loads(raw)
            break
        time.sleep(2)
    elapsed = time.time() - started
    if data is None:
        print(f"    {key} ({name}): FAIL no summary within {seconds} s")
        return False
    if key == "dictation":
        text = hs.adb(serial, "shell", "cat", f"{hs.REMOTE}/dictation.txt", check=False)
        score = cer(REFERENCE, text)
        ok = data.get("sentences", 0) >= 1 and score <= 0.2
        print(f"    {key} ({name}): {'PASS' if ok else 'FAIL'} {data.get('sentences')} sentence(s) in the file, CER {score:.3f}, "
              f"end {data.get('reason')}, {elapsed:.0f} s")
    elif key == "wake":
        rounds = data.get("rounds", [])
        ok = len(rounds) == 1 and rounds[0].get("keyword") == "朱丽楠" and rounds[0].get("chars", 0) > 0 and data.get("reason") == "stopped"
        print(f"    {key} ({name}): {'PASS' if ok else 'FAIL'} rounds {len(rounds)}, "
              f"{[(r.get('chars'), r.get('reason')) for r in rounds]} chars / round end, wake session end {data.get('reason')}, {elapsed:.0f} s")
    elif key == "tts":
        ok = (data.get("durationMillis") or 0) > 0 and bool(data.get("speak")) and bool(data.get("tts")) and not data.get("error")
        print(f"    {key} ({name}): {'PASS' if ok else 'FAIL'} {data.get('model')} {data.get('durationMillis')} ms audio at "
              f"{data.get('sampleRate')} Hz, {data.get('sizeBytes')} bytes, warnings {data.get('warnings')}; speech.speak {data.get('speak')}, "
              f"tts {data.get('tts')} {data.get('error') or ''}, {elapsed:.0f} s")
    else:
        commands = data.get("commands", [])
        first = commands[0] if commands else {}
        ok = len(commands) >= 1 and first.get("chars", 0) > 0 and bool(first.get("spoken")) and not str(first.get("spoken")).startswith("error")
        print(f"    {key} ({name}): {'PASS' if ok else 'FAIL'} command {first.get('chars')} chars, Agent status {first.get('status')} "
              f"{first.get('code') or ''}, summary spoken by {first.get('spoken')}, {elapsed:.0f} s")
    return ok


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("samples", nargs="*", default=list(SAMPLES))
    parser.add_argument("--host", default="D:/idea-projects/AutoJs6")
    arguments = parser.parse_args(argv)
    os.environ.setdefault("MACAW_VOLUME_PERCENT", "70")
    serial = arguments.serial
    hs.prepare(serial)
    push_samples(serial, arguments.host)
    print(f"{hs.label(serial)}: samples {', '.join(arguments.samples)}")
    ok = True
    with hs.Volume(serial) as volume:
        print(f"    media {volume.describe()}")
        for key in arguments.samples:
            ok = run_sample(serial, key) and ok
            time.sleep(3)
    hs.adb(serial, "shell", "rm", "-rf", hs.REMOTE, check=False)
    print(f"{hs.label(serial)}: SAMPLES {'PASS' if ok else 'FAIL'}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
