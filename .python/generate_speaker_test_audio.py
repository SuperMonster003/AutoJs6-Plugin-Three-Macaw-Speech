"""Generates the speaker device test recordings of roadmap P8.3.6 (D143) and locks them.

    py .python/generate_speaker_test_audio.py --librispeech <LibriSpeech/test-clean> --kokoro <kokoro-int8-multi-lang-v1_0>
                                              [--speaker-model <onnx>]   writes the files, rewrites the lock and, with a
                                                                         speaker model, prints the cosine scores
    py .python/generate_speaker_test_audio.py --check                    fails when a file differs from the lock

The files live in app/src/androidTest/assets/speaker-audio/ (androidTest assets, never in the plugin APK) and are locked
in locks/speaker-test-audio.lock (path, SHA-256, source, license), which app/build.gradle.kts checks. Two kinds:

- libri-a-* / libri-b-*: two female readers of LibriSpeech test-clean (CC BY 4.0, speakers 1284 and 237, the female
  readers with the highest same-speaker scores of the ERes2Net model), the first three utterances of at least 4 s for
  enrollment (cut to 3 s) and the next two for the trials (cut to 2 s), as for the 2 s clips the default threshold was
  measured on (D138). They drive enroll / verify / identify. Measured on the PC (sherpa-onnx 1.13.8): same speaker
  0.77 - 0.80, the other reader below 0.08; the Kokoro wake words score 0.69 (voice a) and 0.35 (voice b) against voice a.
- kokoro-a-* / kokoro-b-*: the Kokoro int8 multi-lang v1.0 model of sherpa-onnx (Apache-2.0), Chinese voices 45
  (zf_xiaobei) and 50 (zm_yunxi): voice a reads the three enrollment sentences of the speaker screen, both voices say the
  wake word, so "only enrolled voices" (D139) lets voice a through and rejects voice b.

Everything is 16 kHz mono PCM16 (Kokoro's 24 kHz resampled by a windowed-sinc 2/3 polyphase filter). The synthesis is
not bit-exact across onnxruntime builds, so --check compares the committed bytes with the lock and never regenerates.
Needs numpy, soundfile and sherpa-onnx 1.13.8 (the plugin's runtime version).
"""
from __future__ import annotations

import argparse
import hashlib
import sys
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "app" / "src" / "androidTest" / "assets" / "speaker-audio"
LOCK = ROOT / "locks" / "speaker-test-audio.lock"
FORMAT = 1
RATE = 16000

LIBRI = {"a": "1284", "b": "237"}
ENROLL_SECONDS = 3.0
TRIAL_SECONDS = 2.0
KOKORO = {"a": 45, "b": 50}
WAKE_WORD = "小鹦小鹦"
# The Chinese enrollment sentences of the speaker screen (values-zh speakers_prompt_1 .. 3).
SENTENCES = [
    "你好, 这是我的声音, 我正在用平常的语速朗读.",
    "早上的火车七点出发, 我习惯在上车前买一杯咖啡.",
    "请记住我的声音, 这样只有我能唤醒你.",
]
PAD_SECONDS = 0.3


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


def resample_24k_to_16k(samples):
    """2/3 polyphase with a Kaiser-windowed sinc low-pass at 7.2 kHz."""
    import numpy as np

    taps = 96 * 2 + 1
    n = np.arange(taps) - (taps - 1) / 2
    cutoff = 7200.0 / 48000.0
    kernel = 2 * cutoff * np.sinc(2 * cutoff * n) * np.kaiser(taps, 8.0)
    kernel *= 2 / kernel.sum()
    up = np.zeros(len(samples) * 2, dtype=np.float64)
    up[::2] = samples
    filtered = np.convolve(up, kernel, mode="same")
    return filtered[::3].astype(np.float32)


def libri_files(root: Path, speaker: str) -> list[Path]:
    import soundfile as sf

    files = sorted((root / speaker).rglob("*.flac"))
    return [f for f in files if sf.info(str(f)).duration >= 4.0][:5]


def generate(librispeech: Path, kokoro: Path) -> list[tuple[str, str, str]]:
    import numpy as np
    import sherpa_onnx
    import soundfile as sf

    made: list[tuple[str, str, str]] = []
    for voice, speaker in LIBRI.items():
        files = libri_files(librispeech, speaker)
        if len(files) < 5:
            raise SystemExit(f"LibriSpeech speaker {speaker} has fewer than five utterances of 4 s")
        for index, file in enumerate(files):
            enroll = index < 3
            seconds = ENROLL_SECONDS if enroll else TRIAL_SECONDS
            data, rate = sf.read(str(file), dtype="float32")
            assert rate == RATE, (file, rate)
            name = f"libri-{voice}-{'enroll' if enroll else 'trial'}-{index + 1 if enroll else index - 2}"
            write_wav(OUTPUT / f"{name}.wav", data[: int(seconds * RATE)])
            made.append((name, f"LibriSpeech test-clean {file.stem}.flac, first {seconds:g} s", "CC-BY-4.0"))

    config = sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
                model=str(kokoro / "model.int8.onnx"),
                voices=str(kokoro / "voices.bin"),
                tokens=str(kokoro / "tokens.txt"),
                data_dir=str(kokoro / "espeak-ng-data"),
                lexicon=f"{kokoro / 'lexicon-us-en.txt'},{kokoro / 'lexicon-zh.txt'}",
            ),
            num_threads=4,
        ),
        rule_fsts=",".join(str(kokoro / name) for name in ("phone-zh.fst", "date-zh.fst", "number-zh.fst")),
    )
    tts = sherpa_onnx.OfflineTts(config)
    pad = np.zeros(int(PAD_SECONDS * RATE), dtype=np.float32)

    def speak(voice: str, text: str, name: str, what: str) -> None:
        audio = tts.generate(text, sid=KOKORO[voice], speed=1.0)
        assert audio.sample_rate == 24000, audio.sample_rate
        samples = resample_24k_to_16k(np.array(audio.samples, dtype=np.float32))
        write_wav(OUTPUT / f"{name}.wav", np.concatenate([pad, samples, pad]))
        made.append((name, f"Kokoro int8 multi-lang v1.0 (sherpa-onnx tts-models), speaker {KOKORO[voice]}, {what}", "Apache-2.0"))

    for index, sentence in enumerate(SENTENCES):
        speak("a", sentence, f"kokoro-a-enroll-{index + 1}", f"enrollment sentence {index + 1} of the speaker screen")
    for voice in KOKORO:
        speak(voice, WAKE_WORD, f"kokoro-{voice}-wake", "the default Chinese wake word")
    return made


def write_lock(made: list[tuple[str, str, str]]) -> None:
    text = [
        "# Speaker device test recordings of roadmap P8.3.6 (D143; androidTest assets, never in the plugin APK), checked by",
        "#   app/build.gradle.kts. Made by .python/generate_speaker_test_audio.py: libri-* are cuts of LibriSpeech test-clean",
        "#   (CC BY 4.0, https://www.openslr.org/12), kokoro-* are synthesized by the Kokoro int8 multi-lang v1.0 model of",
        "#   sherpa-onnx (Apache-2.0, https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models).",
        f"format={FORMAT}",
    ]
    for name, source, license in sorted(made):
        path = OUTPUT / f"{name}.wav"
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


def scores(model: Path) -> None:
    """Cosine scores of the trials against the enrollment means (the plugin's rule: mean of normalized embeddings)."""
    import numpy as np
    import sherpa_onnx
    import soundfile as sf

    extractor = sherpa_onnx.SpeakerEmbeddingExtractor(sherpa_onnx.SpeakerEmbeddingExtractorConfig(model=str(model), num_threads=2))

    def embed(name: str):
        data, rate = sf.read(str(OUTPUT / f"{name}.wav"), dtype="float32")
        stream = extractor.create_stream()
        stream.accept_waveform(rate, data)
        stream.input_finished()
        vector = np.array(extractor.compute(stream), dtype=np.float64)
        return vector / np.linalg.norm(vector)

    def mean(names: list[str]):
        vector = np.mean([embed(n) for n in names], axis=0)
        return vector / np.linalg.norm(vector)

    prints = {
        "libri-a": mean([f"libri-a-enroll-{i}" for i in (1, 2, 3)]),
        "libri-b": mean([f"libri-b-enroll-{i}" for i in (1, 2, 3)]),
        "kokoro-a": mean([f"kokoro-a-enroll-{i}" for i in (1, 2, 3)]),
    }
    trials = [f"libri-{v}-trial-{i}" for v in "ab" for i in (1, 2)] + ["kokoro-a-wake", "kokoro-b-wake"]
    for trial in trials:
        vector = embed(trial)
        print(trial, " ".join(f"{name}={float(np.dot(vector, p)):.3f}" for name, p in prints.items()))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--librispeech", type=Path, help="the LibriSpeech/test-clean folder")
    parser.add_argument("--kokoro", type=Path, help="the extracted kokoro-int8-multi-lang-v1_0 folder")
    parser.add_argument("--speaker-model", type=Path, help="a speaker embedding model, to print the cosine scores")
    parser.add_argument("--check", action="store_true", help="compare the committed files with the lock")
    args = parser.parse_args()
    if args.check:
        errors = check()
        print("speaker test audio: " + ("ok" if not errors else "; ".join(errors)))
        return 1 if errors else 0
    if args.librispeech is None or args.kokoro is None:
        parser.error("--librispeech and --kokoro are required unless --check")
    for old in OUTPUT.glob("*.wav"):
        old.unlink()
    write_lock(generate(args.librispeech, args.kokoro))
    if args.speaker_model:
        scores(args.speaker_model)
    print(f"wrote {len(list(OUTPUT.glob('*.wav')))} files, {sum(p.stat().st_size for p in OUTPUT.glob('*.wav'))} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
