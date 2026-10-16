# Speech synthesis baseline

Roadmap P4.1 - P4.4. Measured on 2026-10-16 with the debug build of the P4 commit (sherpa-onnx 1.13.8, onnxruntime
1.28.2, 4 intra-op threads per synthesizer) and the five TTS models of the catalog (appendix A), version 1, installed
from the `speech-models-v1` mirror by `SpeechSynthesisDeviceTest#test0InstallsTheSynthesisModels`
(`py .python/device_speech_run.py tts-models <serial>`, about 740 MB; 2 to 5 minutes per device). Instrumentation
`SpeechSynthesisDeviceTest` (`py .python/device_speech_run.py tts <serial>`); the numbers are its `MacawDeviceRun` lines.
How the engine works is in `tts-engine.md`.

## Method

- One sentence per model through `ISpeechPlugin.synthesizeToFile` (in-process Binder), default speaker: Chinese
  (`今天天气很好, 我们一起去公园散步吧.`, 19 characters) for AISHELL-3 and Matcha, English (61 characters) for Piper, mixed
  Chinese and English with three English words (`这个 project 的 deadline 是明天, 请按时提交 report.`, 40 characters) for
  the bilingual MeloTTS and Kokoro.
- The first call loads the model from a cold cache, the second is warm: load = cold minus warm wall time, real-time factor
  (RTF) = warm wall time / audio time. Native heap and PSS of the process right after the warm call.
- The output must be a mono PCM16 WAV at the catalog sample rate whose length matches the completion; between 25 and 700
  ms of audio per character; progress ends at the text length.
- Dropped words: SenseVoice (`asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8`) recognizes the bilingual models' output;
  the character error rate against the input (letters and digits, case, white space and punctuation ignored) must stay
  at most 0.35, and the three English words are counted.
- Long text: 8000 characters of numbered Chinese sentences with AISHELL-3 to the end (header and completion checked,
  more than 100 progress updates), then the same text cancelled after the first progress update.
- System engine: Android's `TextToSpeech` bound to the plugin package in the test process: engine listed, voices equal to
  the plugin's voice table, language availability, `synthesizeToFile` of a Chinese sentence with
  `tts.vits.icefall.zh-aishell3/10` at speech rate 1.0 and 2.0, the English voice `tts.vits.melo.zh-en/0/en-US`, and
  `stop()` during a 3000 character synthesis.

## Results: one sentence per model

Load in ms, RTF of the warm call, PSS in MiB after it; CER and English words for the bilingual models.

| Device | API | AISHELL-3 (8 kHz) | MeloTTS (44.1 kHz) | Piper (22.05 kHz) | Matcha (22.05 kHz) | Kokoro (24 kHz) |
| --- | --- | --- | --- | --- | --- | --- |
| Sony G8441 (BH900ASK9E) | 28 | 1934 ms, 0.125, 212 | 4998 ms, 2.262, 444; CER 0.031, 3/3 | 2450 ms, 0.346, 550 | 3791 ms, 0.440, 506 | 2632 ms, 3.434, 943; CER 0.125, 3/3 |
| Sony XQ-DQ72 (QV770340J7) | 33 | 625 ms, 0.035, 262 | 2339 ms, 0.504, 499; CER 0.125, 3/3 | 641 ms, 0.076, 786 | 1250 ms, 0.122, 606 | 1750 ms, 0.679, 985; CER 0.125, 3/3 |
| OnePlus OPD2413 (63751f63) | 35 | 481 ms, 0.042, 254 | 1875 ms, 0.564, 495; CER 0.063, 3/3 | 531 ms, 0.072, 618 | 1004 ms, 0.111, 518 | 1137 ms, 0.663, 986; CER 0.125, 3/3 |

Audio lengths: AISHELL-3 4.9 - 5.5 s, MeloTTS 4.0 - 4.1 s, Piper 2.8 - 2.9 s, Matcha 3.4 s, Kokoro 4.3 s. The VITS models
sample noise, so their length and the CER vary a little from run to run (MeloTTS CER 0.000 - 0.188 over seven runs).

The G8441 (Snapdragon 835) is the low end: MeloTTS and Kokoro synthesize slower than real time there (three runs with 4
threads: MeloTTS 1.67 - 2.26, Kokoro 2.36 - 3.43, the slower runs after the 8000 character test had warmed the phone),
so the system engine pauses before each sentence with them; AISHELL-3, Piper and Matcha stay well below real time on
every device. The thread count: with 2 threads the same G8441 measured MeloTTS 2.99, Kokoro 3.9, AISHELL-3 0.197,
Piper 0.378, Matcha 0.541, the XQ-DQ72 MeloTTS 0.83 and Kokoro 1.04; 4 threads are used since.

Kokoro speakers: the first device run used speaker 0 by default and SenseVoice heard pinyin-like English for the
Chinese words (CER 1.00). A probe on the XQ-DQ72 compared speakers 0, 2, 3, 18 and 50 on the mixed, the Chinese and the
English sentence: speakers 0 and 2 read pure Chinese and pure English correctly (CER 0.00) but the mixed sentence with
English phonetics (CER 1.00, 0.97); speakers 3, 18 and 50 read all three (mixed CER 0.13, 0.28, 0.25). Speakers 0 to 2
are the English speakers of Kokoro multi-lang v1.1, so their first language is English and speaker 3 is the model's
default (`TtsSpeakerHints`).

## Results: long text, cancel and the speaker rules

| Device | API | 8000 characters (AISHELL-3) | Cancel |
| --- | --- | --- | --- |
| Sony G8441 | 28 | 37.8 min of audio in 405 s, RTF 0.179, 36.3 MB, 374 progress updates, PSS 238 MiB | after 21 characters, answered in 257 ms |
| Sony XQ-DQ72 | 33 | 37.8 min in 114 s, RTF 0.050, 36.3 MB, 374 updates, PSS 833 MiB | after 21 characters, 81 ms |
| OnePlus OPD2413 | 35 | 37.8 min in 98 s, RTF 0.043, 36.3 MB, 374 updates, PSS 809 MiB | after 21 characters, 60 ms |

With 2 threads the G8441 took 499 s (RTF 0.22). The host gives `synthesizeToFile` 10 minutes, so on the G8441 an 8000
character text fits with AISHELL-3, while MeloTTS or Kokoro would need hours (and MeloTTS at 44.1 kHz passes the 100 MiB
output limit after about 20 minutes of audio). On all three devices: a single speaker model (MeloTTS) given speaker `3`
completes with speaker 0 and one `INVALID_OPTIONS` warning; AISHELL-3 resolves `SSB0009` to speaker 1 and accepts 173;
speaker 174, speed 2.5 and a recognition model are `INVALID_OPTIONS`, 8001 characters `LIMIT_EXCEEDED`, an unknown model
`MODEL_MISSING`, each thrown by the call itself.

## Results: system engine

| Device | API | Voices | Languages (zh / en / ja) | AISHELL-3 voice 10 at rate 1.0 / 2.0 | `stop()` |
| --- | --- | --- | --- | --- | --- |
| Sony G8441 | 28 | 384 | 1 / 1 / -2 | 3399 / 1922 ms (0.57) | answered at once, `onStop(interrupted = true)` |
| Sony XQ-DQ72 | 33 | 384 | 1 / 1 / -2 | 3361 / 2072 ms (0.62) | at once |
| OnePlus OPD2413 | 35 | 384 | 1 / 1 / -2 | 3794 / 2176 ms (0.57) | at once |

384 voices = AISHELL-3 174 + MeloTTS 2 + Piper 1 + Matcha 1 + Kokoro 206 (103 speakers in two languages); language
codes `LANG_COUNTRY_AVAILABLE` (1) for zh-CN and en-US, `LANG_NOT_SUPPORTED` (-2) for Japanese (no catalog model). The
English voice of MeloTTS synthesized the English sample (4.6 s at 44.1 kHz) on every device.

System settings (E4, the maintainer on the device, 2026-10-16): on the XQ-DQ72 and the G8441 the text-to-speech settings
select 3-Macaw Speech as the preferred engine, the sample plays and the speech rate slider changes it (the settings
called `CHECK_TTS_DATA` and `GET_SAMPLE_TEXT`, logged in the event log; the rate stayed at 191 and 164). On the OnePlus
the engine was first selected before any TTS model was installed: the sample button and the rate slider stayed disabled
(no voice data, `CHECK_TTS_DATA` failed), as designed. After the five models were installed the sample played with
`tts.kokoro.multi-lang.v1_1/0` (device locale en-US) at rates 100, 75 and 200: 3416, 4536 and 1872 ms of audio
(`MacawTtsEngine`). TalkBack and Select to Speak were not tried; AutoJs6 itself (below) is the third-party client.

## Results: AutoJs6 host

`py .python/device_tts_run.py host <serial>` with the AutoJs6 debug builds on the devices (5455 on the Sony phones, 5456
on the OnePlus), passed on all three:

- `speech.capabilities()`: features `models,asr,asrStreaming,vad,kws,tts,ttsSystemEngine`, `ttsEnginePackage` the plugin.
- `speech.synthesize(text, path, {model: AISHELL-3, speaker: 10})`: 8 kHz, 2.5 - 2.8 s, speaker 10 (476 ms on the OnePlus,
  825 ms on the XQ-DQ72, 2301 ms on the G8441, model load included); `media.playMusic` plays it (duration equals the
  synthesized length). `speech.synthesizeAsync` (OnePlus) resolves the same way and leaves no `.part` file.
- `tts.engines()` lists the plugin next to the default engine, `tts.ready(<package>)` answers ready, `tts.voices({engine})`
  384 voices (174 of AISHELL-3, all offline), `tts.languageAvailability("zh-CN", <package>)` `language_and_country`.
- `tts("你好", {engine})` speaks with the engine's default voice for the device locale (`tts.kokoro.multi-lang.v1_1/0`,
  en-US), `tts("你好", {engine, voice: "tts.vits.icefall.zh-aishell3/20"})` with that voice, `tts.synthesize(text, path,
  {engine})` writes 148 KB, `speech.speak(text, {model: AISHELL-3, speaker: 3})` speaks with `.../3`.
- Fallback (`py .python/device_tts_run.py fallback QV770340J7`): with the plugin uninstalled (`pm uninstall -k`, models
  kept) `speech.isAvailable()` is false, `tts.engines()` lists the Google engine only, `tts.synthesize` without an engine
  works, with the plugin's engine it fails with `INITIALIZATION` (`TTS engine is not installed or enabled`);
  `speech.synthesize` fails with `PLUGIN_UNAVAILABLE` and the localized missing-plugin message, `speech.speak` with the
  `tts` module's `INITIALIZATION` error. Reinstalling restored the plugin with its models.
