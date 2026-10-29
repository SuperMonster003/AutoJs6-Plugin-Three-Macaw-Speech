"""Generates the analysis device test recordings of roadmap P8.4.5 (D146 - D149) and locks them.

    py .python/generate_analysis_test_audio.py --librispeech <LibriSpeech/test-clean> --kokoro <kokoro-int8-multi-lang-v1_0>
                                               --piper-ru <vits-piper-ru_RU-denis-medium-int8>
                                               [--segment <onnx> --speaker-model <onnx> --whisper <dir> --ced <dir>]
                                                writes the files, rewrites the lock and, with the models, prints what
                                                sherpa-onnx finds in them
    py .python/generate_analysis_test_audio.py --check   fails when a file differs from the lock

The files live in app/src/androidTest/assets/analysis-audio/ (androidTest assets, never in the plugin APK) and are locked
in locks/analysis-test-audio.lock (path, SHA-256, source, license), which app/build.gradle.kts checks:

- two-speakers.wav / two-speakers.json: three utterances each of two LibriSpeech test-clean readers (CC BY 4.0), 1284
  (voice a of the speaker recordings, so identify can name it) and the male reader 1089, alternating with 0.7 s of
  silence; the JSON lists the true turns in milliseconds. Speaker 1284 uses utterances after the five of
  speaker-audio, so no clip is enrolled and diarized at once.
- language-zh.wav / language-ru.wav: two sentences each, synthesized by the Kokoro int8 multi-lang v1.0 model (speaker
  45) and the Piper ru_RU denis medium model of sherpa-onnx (Apache-2.0 and MIT); English uses test-audio/en.wav.
- sounds.wav: 30 s made by this script (no third-party audio): a melody of decaying harmonic tones, white noise and
  1 kHz beeps, 10 s each, so each fills one tagging window (D149).

Everything is 16 kHz mono PCM16 (Kokoro's 24 kHz and Piper's 22.05 kHz resampled with scipy's polyphase filter). The
synthesis is not bit-exact across onnxruntime builds, so --check compares the committed bytes with the lock and never
regenerates. Needs numpy, scipy, soundfile and sherpa-onnx 1.13.8 (the plugin's runtime version).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "app" / "src" / "androidTest" / "assets" / "analysis-audio"
LOCK = ROOT / "locks" / "analysis-test-audio.lock"
FORMAT = 1
RATE = 16000

READERS = {"a": ("1284", 5), "b": ("1089", 0)}
TURN_SECONDS = 6.5
GAP_SECONDS = 0.7
SENTENCES = {
    "zh": ["今天天气很好, 我们一起去公园散步吧.", "火车晚点了二十分钟, 大家都在站台上等."],
    "ru": ["Сегодня прекрасная погода, давай погуляем в парке.", "Поезд опоздал на двадцать минут, и все ждали на платформе."],
}
KOKORO_SPEAKER = 45
SOUND_SECONDS = 10.0


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_wav(path: Path, samples) -> None:
    import numpy as np

    pcm = (np.clip(samples, -1.0, 1.0) * 32767).astype("<i2")
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(pcm.tobytes())


def to16k(samples, rate: int):
    import numpy as np
    from scipy.signal import resample_poly

    if rate == RATE:
        return np.asarray(samples, dtype=np.float32)
    g = np.gcd(rate, RATE)
    return resample_poly(np.asarray(samples, dtype=np.float64), RATE // g, rate // g).astype(np.float32)


def two_speakers(librispeech: Path) -> list[tuple[str, str, str]]:
    import numpy as np
    import soundfile as sf

    clips = {}
    for voice, (speaker, skip) in READERS.items():
        files = [f for f in sorted((librispeech / speaker).rglob("*.flac")) if sf.info(str(f)).duration >= TURN_SECONDS][skip:skip + 3]
        if len(files) < 3:
            raise SystemExit(f"LibriSpeech speaker {speaker} has fewer than three utterances of {TURN_SECONDS} s")
        clips[voice] = [(f, sf.read(str(f), dtype="float32")[0][: int(TURN_SECONDS * RATE)]) for f in files]
    gap = np.zeros(int(GAP_SECONDS * RATE), dtype=np.float32)
    parts, turns, sources = [], [], []
    position = 0
    for index in range(3):
        for voice in ("a", "b"):
            file, samples = clips[voice][index]
            if parts:
                parts.append(gap)
                position += len(gap)
            parts.append(samples)
            turns.append({"startMillis": position * 1000 // RATE, "endMillis": (position + len(samples)) * 1000 // RATE, "speaker": voice})
            position += len(samples)
            sources.append(file.stem)
    write_wav(OUTPUT / "two-speakers.wav", np.concatenate(parts))
    (OUTPUT / "two-speakers.json").write_text(json.dumps({"readers": {v: s for v, (s, _) in READERS.items()}, "turns": turns}, indent=2) + "\n", encoding="utf-8", newline="\n")
    source = "LibriSpeech test-clean " + ", ".join(f"{s}.flac" for s in sources) + f", first {TURN_SECONDS:g} s each, alternating with {GAP_SECONDS:g} s of silence"
    return [("two-speakers", "wav", source, "CC-BY-4.0"), ("two-speakers", "json", "the true turns of two-speakers.wav, written by this script", "CC-BY-4.0")]


def languages(kokoro: Path, piper: Path) -> list[tuple[str, str, str, str]]:
    import numpy as np
    import sherpa_onnx

    pad = np.zeros(int(0.4 * RATE), dtype=np.float32)
    zh = sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
                model=str(kokoro / "model.int8.onnx"), voices=str(kokoro / "voices.bin"), tokens=str(kokoro / "tokens.txt"),
                data_dir=str(kokoro / "espeak-ng-data"), lexicon=f"{kokoro / 'lexicon-us-en.txt'},{kokoro / 'lexicon-zh.txt'}",
            ),
            num_threads=4,
        ),
        rule_fsts=",".join(str(kokoro / name) for name in ("phone-zh.fst", "date-zh.fst", "number-zh.fst")),
    ))
    onnx = next(piper.glob("*.onnx"))
    ru = sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(
        vits=sherpa_onnx.OfflineTtsVitsModelConfig(model=str(onnx), tokens=str(piper / "tokens.txt"), data_dir=str(piper / "espeak-ng-data")),
        num_threads=4,
    )))
    made = []
    for language, tts, sid, source, license in (
        ("zh", zh, KOKORO_SPEAKER, f"Kokoro int8 multi-lang v1.0 (sherpa-onnx tts-models), speaker {KOKORO_SPEAKER}", "Apache-2.0"),
        ("ru", ru, 0, "Piper ru_RU denis medium int8 (sherpa-onnx tts-models)", "MIT"),
    ):
        parts = [pad]
        for sentence in SENTENCES[language]:
            audio = tts.generate(sentence, sid=sid, speed=1.0)
            parts += [to16k(audio.samples, audio.sample_rate), pad]
        write_wav(OUTPUT / f"language-{language}.wav", np.concatenate(parts))
        made.append((f"language-{language}", "wav", f"{source}, two sentences", license))
    return made


def sounds() -> list[tuple[str, str, str, str]]:
    import numpy as np

    length = int(SOUND_SECONDS * RATE)
    notes = [261.63, 329.63, 392.0, 523.25, 392.0, 329.63, 293.66, 349.23, 440.0, 349.23, 293.66, 261.63]
    melody = np.zeros(length)
    step = length // len(notes)
    t = np.arange(int(1.2 * RATE)) / RATE
    for i, frequency in enumerate(notes):
        tone = sum((0.6 ** k) * np.sin(2 * np.pi * frequency * (k + 1) * t) for k in range(6)) * np.exp(-3.0 * t)
        end = min(length, i * step + len(tone))
        melody[i * step:end] += tone[: end - i * step]
    melody = 0.3 * melody / np.max(np.abs(melody))
    noise = 0.15 * np.random.default_rng(7).standard_normal(length)
    beeps = np.zeros(length)
    beep = 0.3 * np.sin(2 * np.pi * 1000 * np.arange(int(0.25 * RATE)) / RATE)
    for k in range(int(SOUND_SECONDS * 2)):
        beeps[int(k * 0.5 * RATE):int(k * 0.5 * RATE) + len(beep)] = beep
    write_wav(OUTPUT / "sounds.wav", np.concatenate([melody, noise, beeps]).astype(np.float32))
    return [("sounds", "wav", "made by this script: 10 s melody of harmonic tones, 10 s white noise (seed 7), 10 s of 1 kHz beeps", "CC0-1.0")]


def write_lock(made: list[tuple[str, str, str, str]]) -> None:
    text = [
        "# Analysis device test recordings of roadmap P8.4.5 (androidTest assets, never in the plugin APK), checked by",
        "#   app/build.gradle.kts. Made by .python/generate_analysis_test_audio.py: two-speakers is cut from LibriSpeech",
        "#   test-clean (CC BY 4.0, https://www.openslr.org/12), language-* are synthesized by sherpa-onnx TTS models",
        "#   (https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models), sounds is made by the script itself.",
        f"format={FORMAT}",
    ]
    for stem, extension, source, license in sorted(made):
        name = stem if extension == "wav" else f"{stem}-{extension}"
        path = OUTPUT / f"{stem}.{extension}"
        text += [
            f"{name}.path={path.relative_to(ROOT).as_posix()}",
            f"{name}.sha256={sha256(path)}",
            f"{name}.source={source}",
            f"{name}.license={license}",
        ]
    LOCK.write_text("\n".join(text) + "\n", encoding="utf-8", newline="\n")


def check() -> list[str]:
    errors = []
    entries = {}
    for line in LOCK.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            key, _, value = line.partition("=")
            entries[key] = value
    names = sorted(key[: -len(".path")] for key in entries if key.endswith(".path"))
    listed = set()
    for name in names:
        path = ROOT / entries[f"{name}.path"]
        listed.add(path.name)
        if not path.is_file():
            errors.append(f"{name}: missing")
        elif sha256(path) != entries.get(f"{name}.sha256"):
            errors.append(f"{name}: differs from the lock")
    extra = sorted({p.name for p in OUTPUT.iterdir() if p.is_file()} - listed) if OUTPUT.is_dir() else []
    errors += [f"{name}: not in the lock" for name in extra]
    return errors


def verify(segment: Path, speaker: Path, whisper: Path, ced: Path) -> None:
    """What sherpa-onnx finds in the files, with the plugin's defaults (threshold 0.85, D154)."""
    import numpy as np
    import sherpa_onnx
    import soundfile as sf

    def read(name: str):
        return sf.read(str(OUTPUT / name), dtype="float32")[0]

    diarization = sherpa_onnx.OfflineSpeakerDiarization(sherpa_onnx.OfflineSpeakerDiarizationConfig(
        segmentation=sherpa_onnx.OfflineSpeakerSegmentationModelConfig(pyannote=sherpa_onnx.OfflineSpeakerSegmentationPyannoteModelConfig(model=str(segment))),
        embedding=sherpa_onnx.SpeakerEmbeddingExtractorConfig(model=str(speaker)),
        clustering=sherpa_onnx.FastClusteringConfig(num_clusters=-1, threshold=0.85),
        min_duration_on=0.3, min_duration_off=0.5,
    ))
    truth = json.loads((OUTPUT / "two-speakers.json").read_text(encoding="utf-8"))["turns"]
    found = diarization.process(read("two-speakers.wav")).sort_by_start_time()
    print("two-speakers truth:", [(t["startMillis"], t["endMillis"], t["speaker"]) for t in truth])
    print("two-speakers found:", [(int(s.start * 1000), int(s.end * 1000), s.speaker) for s in found])

    def language(name: str) -> str:
        files = {"encoder": next(whisper.glob("*tiny-encoder.int8.onnx")), "decoder": next(whisper.glob("*tiny-decoder.int8.onnx"))}
        lid = sherpa_onnx.SpokenLanguageIdentification(sherpa_onnx.SpokenLanguageIdentificationConfig(
            whisper=sherpa_onnx.SpokenLanguageIdentificationWhisperConfig(encoder=str(files["encoder"]), decoder=str(files["decoder"])),
        ))
        stream = lid.create_stream()
        stream.accept_waveform(RATE, read(name) if (OUTPUT / name).exists() else sf.read(str(ROOT / "app/src/androidTest/assets/test-audio" / name), dtype="float32")[0])
        return lid.compute(stream)

    for name in ("language-zh.wav", "language-ru.wav", "en.wav"):
        print(name, language(name))
    tagger = sherpa_onnx.AudioTagging(sherpa_onnx.AudioTaggingConfig(
        model=sherpa_onnx.AudioTaggingModelConfig(ced=str(ced / "model.int8.onnx")), labels=str(ced / "class_labels_indices.csv"), top_k=3,
    ))
    samples = read("sounds.wav")
    for window in range(3):
        stream = tagger.create_stream()
        stream.accept_waveform(RATE, samples[window * 10 * RATE:(window + 1) * 10 * RATE])
        print(f"sounds {window * 10}-{window * 10 + 10} s:", [(e.name, round(e.prob, 3)) for e in tagger.compute(stream)])
    _ = np


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--librispeech", type=Path, help="the LibriSpeech/test-clean folder")
    parser.add_argument("--kokoro", type=Path, help="the extracted kokoro-int8-multi-lang-v1_0 folder")
    parser.add_argument("--piper-ru", type=Path, help="the extracted vits-piper-ru_RU-denis-medium-int8 folder")
    parser.add_argument("--segment", type=Path, help="the pyannote segmentation model, to print what is found")
    parser.add_argument("--speaker-model", type=Path, help="the speaker embedding model")
    parser.add_argument("--whisper", type=Path, help="the extracted sherpa-onnx-whisper-tiny folder")
    parser.add_argument("--ced", type=Path, help="the extracted CED audio tagging folder")
    parser.add_argument("--check", action="store_true", help="compare the committed files with the lock")
    args = parser.parse_args()
    if args.check:
        errors = check()
        print("analysis test audio: " + ("ok" if not errors else "; ".join(errors)))
        return 1 if errors else 0
    if args.librispeech is None or args.kokoro is None or args.piper_ru is None:
        parser.error("--librispeech, --kokoro and --piper-ru are required unless --check")
    for old in OUTPUT.glob("*"):
        old.unlink()
    write_lock(two_speakers(args.librispeech) + languages(args.kokoro, args.piper_ru) + sounds())
    if args.segment and args.speaker_model and args.whisper and args.ced:
        verify(args.segment, args.speaker_model, args.whisper, args.ced)
    files = [p for p in OUTPUT.iterdir() if p.is_file()]
    print(f"wrote {len(files)} files, {sum(p.stat().st_size for p in files)} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
