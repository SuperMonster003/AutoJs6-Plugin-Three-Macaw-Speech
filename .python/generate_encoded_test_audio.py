"""Generates the encoded device test recordings of roadmap P7.5 (D81 - D86) from the WAV test recordings.

    py .python/generate_encoded_test_audio.py [--ffmpeg <ffmpeg.exe>]   encodes every file and rewrites the lock
    py .python/generate_encoded_test_audio.py --check                   fails when a file differs from the lock

The files live in app/src/androidTest/assets/encoded-audio/ (androidTest assets, never in the plugin APK) and are locked
in locks/encoded-test-audio.lock (path, SHA-256, source recording, ffmpeg arguments), which app/build.gradle.kts checks.
They exercise the system decoders of MediaAudioReader and the plugin's own Ogg Opus parser: MP3 (44.1 kHz stereo, so
the reader resamples and mixes), AAC in ADTS and in MP4, Ogg Vorbis, Ogg Opus, FLAC, AMR-NB, video files with an audio
track (MP4 with H.264 + AAC, WebM with VP8 + Opus) and an MP4 without one. The encoders are not bit-exact across
ffmpeg versions, so --check compares the committed bytes with the lock and never re-encodes; the recorded files were
made with ffmpeg n5.0 (the MeGUI build of the maintainer's PC).
"""
from __future__ import annotations

import argparse
import hashlib
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCES = ROOT / "app" / "src" / "androidTest" / "assets" / "test-audio"
OUTPUT = ROOT / "app" / "src" / "androidTest" / "assets" / "encoded-audio"
LOCK = ROOT / "locks" / "encoded-test-audio.lock"
DEFAULT_FFMPEG = Path(r"D:\Program Files\MeGUI_2418_x86\tools\ffmpeg\ffmpeg.exe")
FORMAT = 1
CLEAN = ["-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact"]
GRAY = ["-f", "lavfi", "-i", "color=c=gray:s=160x120:r=10"]
H264 = ["-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", "-flags:v", "+bitexact"]

# name -> (source recording or None, input arguments before the source, output arguments)
FILES: dict[str, tuple[str | None, list[str], list[str]]] = {
    "en.mp3": ("en", [], ["-ar", "44100", "-ac", "2", "-c:a", "libmp3lame", "-b:a", "96k"]),
    "en.aac": ("en", [], ["-c:a", "aac", "-b:a", "48k", "-f", "adts"]),
    "en.m4a": ("en", [], ["-ar", "22050", "-c:a", "aac", "-b:a", "48k", "-movflags", "+faststart", "-f", "mp4"]),
    "en.ogg": ("en", [], ["-c:a", "libvorbis", "-q:a", "4"]),
    "en.opus": ("en", [], ["-c:a", "libopus", "-b:a", "24k", "-f", "opus"]),
    "en.flac": ("en", [], ["-c:a", "flac"]),
    "en.amr": ("en", [], ["-ar", "8000", "-c:a", "libopencore_amrnb", "-b:a", "12.2k", "-f", "amr"]),
    "en-video.mp4": ("en", GRAY, ["-map", "0:v", "-map", "1:a", *H264, "-c:a", "aac", "-b:a", "48k", "-shortest", "-movflags", "+faststart", "-f", "mp4"]),
    "en-video.webm": ("en", GRAY, ["-map", "0:v", "-map", "1:a", "-c:v", "libvpx", "-b:v", "50k", "-c:a", "libopus", "-b:a", "24k", "-shortest", "-f", "webm"]),
    "video-only.mp4": (None, GRAY, ["-t", "2", *H264, "-movflags", "+faststart", "-f", "mp4"]),
    "zh.mp3": ("zh", [], ["-c:a", "libmp3lame", "-b:a", "64k"]),
    "zh.aac": ("zh", [], ["-c:a", "aac", "-b:a", "48k", "-f", "adts"]),
    "zh.m4a": ("zh", [], ["-c:a", "aac", "-b:a", "48k", "-movflags", "+faststart", "-f", "mp4"]),
    "zh.ogg": ("zh", [], ["-c:a", "libvorbis", "-q:a", "4"]),
    "zh.opus": ("zh", [], ["-ac", "2", "-c:a", "libopus", "-b:a", "32k", "-f", "opus"]),
    "zh.flac": ("zh", [], ["-c:a", "flac"]),
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def encode(ffmpeg: Path) -> dict[str, str]:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    lines = {}
    for name, (source, before, after) in FILES.items():
        command = [str(ffmpeg), "-hide_banner", "-loglevel", "error", "-y", *before]
        if source is not None:
            command += ["-i", str(SOURCES / f"{source}.wav")]
        command += [*CLEAN, *after]
        # A sibling temporary name with the same extension (not tempfile.mkdtemp, whose restricted DACL a moved file keeps on Windows).
        target = OUTPUT / f".tmp-{name}"
        try:
            subprocess.run([*command, str(target)], check=True)
            os.replace(target, OUTPUT / name)
        finally:
            target.unlink(missing_ok=True)
        lines[name] = " ".join(before + ([f"-i test-audio/{source}.wav"] if source else []) + after)
    return lines


def write_lock(arguments: dict[str, str]) -> None:
    text = [
        "# Encoded device test recordings of roadmap P7.5 (androidTest assets, never in the plugin APK), checked by app/build.gradle.kts.",
        "# Made by .python/generate_encoded_test_audio.py with ffmpeg n5.0 from the WAV recordings of locks/test-audio.lock",
        "#   (same licenses: en is LibriSpeech CC-BY-4.0, zh Apache-2.0); video tracks are a generated gray frame.",
        f"format={FORMAT}",
    ]
    for name in sorted(FILES):
        key = name.replace(".", "_")
        text += [
            f"{key}.path=app/src/androidTest/assets/encoded-audio/{name}",
            f"{key}.sha256={sha256(OUTPUT / name)}",
            f"{key}.ffmpeg={arguments[name]}",
        ]
    LOCK.write_text("\n".join(text) + "\n", encoding="utf-8", newline="\n")


def check() -> list[str]:
    errors = []
    entries = {}
    for line in LOCK.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            key, _, value = line.partition("=")
            entries[key] = value
    listed = {Path(value).name for key, value in entries.items() if key.endswith(".path")}
    present = {path.name for path in OUTPUT.iterdir() if path.is_file()} if OUTPUT.is_dir() else set()
    if listed != set(FILES):
        errors.append(f"lock lists {sorted(listed)}, the generator {sorted(FILES)}")
    if present != listed:
        errors.append(f"encoded-audio holds {sorted(present)}, the lock lists {sorted(listed)}")
    for name in sorted(listed & present):
        key = name.replace(".", "_")
        if entries.get(f"{key}.sha256") != sha256(OUTPUT / name):
            errors.append(f"{name}: SHA-256 differs from the lock")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="compare the committed files with the lock")
    parser.add_argument("--ffmpeg", type=Path, default=Path(os.environ.get("MACAW_FFMPEG", DEFAULT_FFMPEG)))
    args = parser.parse_args()
    if args.check:
        errors = check()
        for error in errors:
            print(f"ENCODED_AUDIO_ERROR {error}")
        if errors:
            return 1
        print(f"ENCODED_AUDIO_OK files={len(FILES)}")
        return 0
    write_lock(encode(args.ffmpeg))
    errors = check()
    print("\n".join(errors) if errors else f"ENCODED_AUDIO_WRITTEN files={len(FILES)}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
