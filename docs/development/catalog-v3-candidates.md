# Catalog v3 language candidates

Roadmap P8.2.1 (decisions D124, D131; appendix D Q79 - Q85). Candidate sherpa-onnx models for the languages the
maintainer put first for 1.3.0 (Spanish and Arabic recognition, Arabic synthesis, Japanese streaming recognition,
Japanese and Korean synthesis), measured before the maintainer chose the final list on 2026-10-25.

## Method

- Sources: the GitHub release API listings of `k2-fsa/sherpa-onnx` `asr-models` (499 assets) and `tts-models` (647
  assets) with size and SHA-256, the license cards of the Hugging Face repositories the assets come from, and the
  archives themselves (member lists, model cards, ONNX JSON configurations). Every archive was downloaded into a new
  directory outside the repository and checked against the release digest before use.
- Gate (D124): installed size at most 350 MiB; the licenses of the weights and of the training data can be looked up
  (non-commercial models are taken and marked, D11); streaming first; models refused in 1.2.0 for their license stay
  refused unless the license changed.
- Device numbers come from the androidTest `ModelProbeDeviceTest`, driven by
  `py -I .python/device_model_probe.py probe <serial> <preset> --archives <dir>`. It loads the unpacked model straight
  through the sherpa-onnx 1.13.8 API with the plugin's thread counts (2 for recognizers, 4 for synthesizers) in a fresh
  instrumentation process of the debug build. Peak RSS is `VmHWM` after writing 5 to `/proc/self/clear_refs`, so it
  covers loading and the first request only; PSS is taken after the request. First result: a streaming recognizer is
  fed in 100 ms chunks at real-time pace and the latency is the first non-empty partial after the first audible sample;
  an offline recognizer decodes the whole recording (one sentence, what a VAD segment gives it); a synthesizer reports
  the time to its first samples. Read-back character error rates (CER; case, spaces and punctuation ignored) come from
  synthesizing a sentence and recognizing the WAV with another candidate.
- Recordings: the upstream test recordings `ar.wav`, `es.wav`, `ja.wav` and `ko.wav` of the `asr-models` release (the
  same sentence in each language) and the `test_wavs/ja.wav` of the PengChengStarling and SenseVoice archives. They
  were not added to the repository; the device evidence of P8.2.4 uses committed or synthesized audio instead.

## Sony G8441 (BH900ASK9E, API 28, 4 GB), 2026-10-25

Recognition. Load in ms; first result in ms; real-time factor (RTF) of the offline decode; peak RSS / PSS after in MiB.

| Candidate | Installed | Recording | Load | First result | RTF | Peak RSS | PSS after | Result |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | --- |
| NeMo FastConformer es int8 (`offline-transducer`) | 130.1 MiB | es-spanish (2.45 s) | 2307 | 378 | 0.154 | 314 | 266 | one word missed |
| | | es (5.33 s) | 2230 | 776 | 0.146 | 324 | 285 | correct, with punctuation |
| NeMo FastConformer 10 languages int8 | 132.0 MiB | es (5.33 s) | 2246 | 816 | 0.153 | 340 | 289 | two words wrong |
| Canary 180M flash int8 (en / es / de / fr) | 197.6 MiB | es (5.33 s) | 2630 | 3484 | 0.654 | 426 | 386 | correct, with punctuation |
| Omnilingual 300M CTC v2 int8 | 349.0 MiB | es (5.33 s) | 1992 | 7400 | 1.389 | 580 | 541 | correct, no punctuation |
| | | ar (6.55 s) | 1981 | 9202 | 1.406 | 610 | 557 | several words wrong |
| | | ja (8.16 s) | 1916 | 11764 | 1.441 | 628 | 577 | correct |
| | | ko (6.92 s) | 1920 | 9778 | 1.414 | 608 | 562 | several syllables wrong |
| PengChengStarling 8 languages (streaming) | 323.8 MiB | ar (6.55 s) | 5464 | 771 (partial) | - | 590 | 499 | several words wrong |
| | | ja (8.16 s) | 5251 | 661 (partial) | - | 592 | 504 | first words read as English |
| | | PengChengStarling `ja` (13.96 s) | 5340 | 1852 (partial) | - | 596 | 526 | one character wrong |
| | | SenseVoice `ja` (7.20 s) | 5132 | 720 (partial) | - | 590 | 505 | correct |
| SenseVoice int8 (catalog v1, reference) | 228.5 MiB | ja (8.16 s) | 3022 | 2414 | 0.296 | 418 | 377 | one character wrong |
| | | PengChengStarling `ja` (13.96 s) | 2885 | 4226 | 0.303 | 444 | 405 | correct |
| | | SenseVoice `ja` (7.20 s) | 2809 | 2071 | 0.288 | 419 | 381 | one character wrong |

Synthesis. Load and first audio in ms; RTF of the whole sentence; peak RSS / PSS after in MiB; read-back CER.

| Candidate | Installed | Text | Load | First audio | RTF | Peak RSS | PSS after | Read back by | CER |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |
| Kokoro v1.0 int8, speaker 47 (zh) | 181.1 MiB | zh, 3.50 s | 4237 | 7344 | 4.539 | 413 | 375 | - | - |
| Kokoro v1.0 int8, speaker 3 (en) | | en, 2.66 s | 4151 | 10476 | 3.938 | 424 | 386 | - | - |
| Kokoro v1.0 int8, speaker 28, `lang` es | | es, 2.33 s | 4169 | 7509 | 3.218 | 409 | 371 | NeMo es | 0.000 |
| Piper ar_JO SA miro V2 high int8 | about 35 MiB | ar (no diacritics), 3.33 s | 3469 | 1453 | 0.708 | 212 | 174 | PengChengStarling | 0.364 |
| Piper ar_JO SA dii high int8 | about 35 MiB | ar, 3.11 s | 3569 | 1331 | 0.722 | 210 | 172 | PengChengStarling | 0.182 |
| Piper ar_JO kareem medium int8 (refused, D124) | about 35 MiB | ar, 4.29 s | 3280 | 1848 | 0.655 | 219 | 181 | PengChengStarling | 0.030 |
| mimic3 ko_KO kss low | 77.0 MiB | ko, 2.58 s | 1591 | 777 | 0.301 | 290 | 251 | Omnilingual 300M | 0.200 |

- The Arabic voices read undiacritized text through eSpeak NG; kareem reads it best, but its dataset still has no
  license, so it stays refused. The read-back rates include the recognizer's own errors.
- Kokoro v1.0 int8 is slower than real time on this device (the fp32 Kokoro v1.1 of catalog v1 measured RTF 2.8 here
  with four threads, `tts-baseline.md`); the plugin splits text into sentences, so the first audio arrives after the
  first sentence.
- Omnilingual 300M is the one candidate for Arabic recognition that meets the gate, but it decodes slower than real time
  here and has no punctuation or language choice; the maintainer took the PengChengStarling streaming model as an
  exception instead (Q80).

## Sony XQ-DQ72 (QV770340J7, API 33), 2026-10-25

The two chosen candidates above 150 MiB (Canary and Omnilingual were not chosen). This phone is reached through a remote
adb server, and pushing the large model files over it broke off ("Broken pipe") twice, so the plugin installed both
models itself from the catalog v3 development snapshot (`.python/device_catalog_v3.py`, upstream downloads checked
file by file) and `ModelProbeDeviceTest` read the installed directories in place: the catalog file set, so Kokoro
without the jieba `dict/` and `lexicon-gb-en.txt` (the G8441 rows above loaded the full archive with the same roles).
Same method otherwise: a fresh instrumentation process, peak RSS after the `clear_refs` reset.

| Candidate | Input | Load | First result | RTF | Peak RSS | PSS after |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| PengChengStarling 8 languages (streaming) | ar (6.55 s) | 2905 | 671 (partial) | - | 868 | 237 |
| | ja (8.16 s) | 2478 | 569 (partial) | - | 875 | 240 |
| | PengChengStarling `ja` (13.96 s) | 1897 | 2747 (partial) | - | 880 | 241 |
| Kokoro v1.0 int8, speaker 47 (zh) | zh, 3.10 s | 1675 | 1524 (audio) | 0.987 | 559 | 299 |
| Kokoro v1.0 int8, speaker 28, `lang` es | es, 2.33 s | 1477 | 1515 (audio) | 0.649 | 557 | 298 |

- A first run before the reinstall measured PengChengStarling on `ar` at 1731 ms load, 681 ms first partial, 874 MiB
  peak. The run of speaker 3 (en) passed, but its log line was lost: this phone keeps only 256 KiB of logcat.
- Peak RSS is higher than on the G8441 (590 - 602 and 409 - 424 MiB) while PSS after the request is lower (about 240
  and 300 MiB against 500 and 375 MiB). A likely reason, not verified: the peak counts the pages of the model files
  read while loading, which the 8 GB XQ-DQ72 keeps resident longer than the 4 GB G8441.
- Kokoro v1.0 int8 is about real time here (RTF 0.65 - 0.99), against 3.2 - 4.5 on the G8441. The PengChengStarling
  `ja` recording begins with a longer silence, hence the later first partial.

## Final list (D131)

| Model ID | Kind / type | Installed | Sample rate | License expression | commercialUse | Mirror |
| --- | --- | ---: | ---: | --- | --- | --- |
| `asr.nemo-transducer.es.2026-05-03.int8` | asr / `offline-transducer` | 130.2 MiB | 16000 | `CC-BY-4.0` | true | yes |
| `asr.zipformer-streaming.ar-en-id-ja-ru-th-vi-zh.2025-02-10.int8` | asr / `online-transducer` | 323.2 MiB | 16000 | `Apache-2.0` | unknown | yes |
| `tts.kokoro.multi-lang.v1_0.int8` | tts / `kokoro` | 161.1 MiB | 24000 | `Apache-2.0 AND GPL-3.0-or-later` | true | yes |
| `tts.vits.piper.ar-jo-sa-dii-high.int8` | tts / `vits` | 34.9 MiB | 22050 | `CC-BY-NC-SA-4.0 AND GPL-3.0-or-later` | false | yes |
| `tts.vits.piper.ar-jo-sa-miro-v2-high.int8` | tts / `vits` | 34.9 MiB | 22050 | `CC-BY-NC-ND-4.0 AND GPL-3.0-or-later` | false | no (Q85) |
| `tts.vits.mimic3.ko-ko-kss-low` | tts / `vits` | 77.0 MiB | 22050 | `CC0-1.0 AND GPL-3.0-or-later` | false | yes |

Installed sizes as `prepare_sources.py` recorded them in Shared-Assets `8ca543f` (P8.2.2): the catalog installs fewer files than
the probes above unpacked (Kokoro v1.0 without the jieba `dict/` and `lexicon-gb-en.txt`, 181.1 MiB unpacked;
PengChengStarling without `bpe.model` and the test recordings).
