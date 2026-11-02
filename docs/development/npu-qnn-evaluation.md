# NPU acceleration (QNN) evaluation

Roadmap P9.4.1 (D167, Q132). Evaluated on 2026-11-02 against sherpa-onnx v2.0.1, the newest release; the plugin itself is
on sherpa-onnx 1.13.8. Nothing in the plugin changed for this evaluation. The Qualcomm license reading below is an
engineer's reading, not legal advice.

## Result

| Question | Answer |
| --- | --- |
| Prebuilt AAR or jniLibs? | None with QNN in any upstream release. The only prebuilt QNN JNI is inside a demo APK, and it has no TTS, so it cannot replace the plugin's JNI. A full-featured QNN JNI means building sherpa-onnx ourselves (a non-goal) |
| Redistribution of `libQnnHtp*`? | Allowed only in object code and incorporated in our application, never standalone; revocable by QTI without cause. Conflicts with today's APK, conveyed as GPL-3.0-or-later as a whole because of eSpeak NG in 1.13.8. sherpa-onnx 2.0.0 removed eSpeak NG, so the conflict ends with a migration, which is its own project (Q9) |
| Model binding? | `libmodel.so`: any SoC, any QNN SDK, needs `libQnnHtpPrepare.so` and a slow first run. `model.bin`: bound to one SoC and one QNN SDK version |
| Latency on SM8475 | Fixed at about 240 ms with the 20 s model. Slower than the CPU below about 8 s of speech (4 times slower at 1 s), 2.4 times faster at 17.6 s |
| Mixed recordings | 1.25 times faster than the CPU in wall time on the four test recordings, 9.2 times less process CPU time |
| Text | The 20 s model is worse on code-switched speech on this device (`mixed-zh-en`, see below); same text on `en` and `zh-8k` |
| Power | Not measured. Both estimates are blind to the NPU; see "Power" |
| Size | APK +20 to +51 MB (15.2 MB today), plus 162 MB model download that takes about 490 MB on the device (239 MB for the CPU model) |
| Who benefits | Only SM8350 and newer flagships, which already run at 20 to 30 times real time on the CPU. The devices that struggle (G8441 RTF 0.330, Redmi 0.250) cannot use QNN |

Decision: no-go for 1.4.0, kept as a P7.2 candidate (the maintainer on 2026-11-02, Q137, D177). See "Conclusion".

## Upstream support

- Path: `SHERPA_ONNX_ENABLE_QNN=ON`, provider `qnn`, `QnnConfig{backendLib, contextBinary, systemLib}`. It talks to QNN HTP
  directly; it is not the onnxruntime QNN execution provider. `ADSP_LIBRARY_PATH` is `;` separated; the Kotlin API has
  `OfflineRecognizer.prependAdspLibraryPath(nativeLibraryDir)`.
- Versions: QNN SDK 2.40.0.251030; onnxruntime 1.28.2 in both the plugin's AAR and the demo.
- Published models: releases `asr-models-qnn`, `-2`, `-3` and `asr-models-qnn-binary`, `-2`, `-3`, 2718 assets. They
  include SenseVoice (2024-07-17 and 2025-09-09), Paraformer, Zipformer CTC, Moonshine, Parakeet and Whisper for offline
  use; streaming Zipformer transducer (x-asr 2026-06-05 at 160 / 480 / 960 / 1920 ms chunks, and 2023-03-20) and Nemotron.
  There is no QNN model for keyword spotting, TTS or VAD, so the resident wake word cannot use the NPU
  ([#3417](https://github.com/k2-fsa/sherpa-onnx/issues/3417)).
- SenseVoice comes in 5 / 8 / 10 / 13 / 15 / 18 / 20 / 23 / 25 / 28 / 30 s versions of about 162 MB each.

### No dynamic input shapes

QNN has no dynamic shapes, so upstream fixes a maximum duration per model: shorter input is padded, longer input is
truncated.

- Offline: the inference time no longer depends on the speech, only on the model's duration. The plugin cuts segments at
  20 s (`Dictation.MAX_SEGMENT_MILLIS`, `SpeechRuntime.FILE_MAX_SPEECH_SECONDS`), so only the 20 s model (or longer) keeps
  every word. That is what makes short speech slow: a 1 s utterance pays for 20 s. Upstream's answer to long segments is a
  longer model ([#2992](https://github.com/k2-fsa/sherpa-onnx/issues/2992)). Choosing a shorter model per segment would
  need several contexts in memory and about 490 MB of disk each.
- Streaming: a fixed chunk fits a fixed shape, so shapes are no obstacle, but each chunk size is a separate model. The
  catalog's streaming model (`zipformer` 2023-02-20) is not among them, so QNN would change the model, not only where it
  runs. Not measured; the CPU already runs it in real time on every test device (`asr-baseline.md`).

## Prebuilt binaries

- Upstream releases v1.13.8, v2.0.0 and v2.0.1 have no QNN AAR or jniLibs.
- The plugin's AAR 1.13.8 was not built with QNN: it has the string "Please rebuild sherpa-onnx with
  -DSHERPA_ONNX_ENABLE_QNN=ON" and no `QnnInterface_getProviders`.
- The demo APK (Hugging Face `csukuangfj2/sherpa-onnx-apk`, 2.0.1, arm64-v8a only) has a QNN JNI of 4.19 MB, built with
  `SHERPA_ONNX_ENABLE_TTS=OFF` (no `OfflineTts` symbols), so it cannot replace the plugin's JNI.

To have recognition on the NPU and TTS in one process, sherpa-onnx must be built with QNN enabled. That is the stated
non-goal, and it would be the maintainer's decision. Nothing was compiled for this evaluation.

## Redistribution license

From `LICENSE.pdf` of QAIRT 2.40.0.251030 (AI Stack License, 3 pages). Upstream's `qnn-libs` asset and the Hugging Face
copies do not extend these rights to the plugin; the clean source is the SDK downloaded from Qualcomm by the maintainer,
who then accepts this agreement personally.

| Section | Content | Effect here |
| --- | --- | --- |
| 1 (iv) | Distribution only in object code and incorporated in our application; nothing grants standalone distribution | An on-demand download of the runtime libraries alone is at risk. Inside an APK is fine |
| 1, 8 | Revocable; QTI may terminate without cause on notice; on termination all copies must be deleted | A published plugin version could be left unlawful to distribute; every release needs a build without QNN ready |
| 2 (a) | No reverse engineering of the object code | Debugging stops at the library boundary |
| 2 (c) | We comply with all third party licenses and answer for our use | See the GPL conflict below |
| 2 (d) | Prohibited use cases, among them emotion recognition in workplace and education | The script API exposes SenseVoice's `emotion`; a script can do exactly that. We cannot police scripts |
| 2 (e) | Biometric identification is listed as high risk and advised against | Speaker identification (P8.4) would have to stay on the CPU |
| 9 (a) | We defend and indemnify QTI, including for posting applications on download sites | Personal liability for the maintainer; no cap |
| 10 (c) | California law, San Diego County courts | |
| 10 (f) | US export control: no export to embargoed countries, end-use limits; a party in China certifies it is no military end-user | A public GitHub release cannot enforce this; condition of the license |

### GPL conflict in today's package

The JNI of sherpa-onnx 1.13.8 links eSpeak NG statically, so the APK as a whole is conveyed under the GPL-3.0-or-later
(`THIRD_PARTY_NOTICES.md`, D32). The QNN libraries are closed, revocable and must not be reverse engineered. A GPL work
cannot carry them in the same APK and process under GPL 3 sections 5 (c) and 10: the further restrictions cannot be
passed on, and they are no system library, since we would ship them ourselves.

Ways out:

- Move to sherpa-onnx 2.0.x. Upstream removed eSpeak NG and piper-phonemize there
  ([#3731](https://github.com/k2-fsa/sherpa-onnx/issues/3731), closed 2026-10-20; change `#4055` in its changelog; v2.0.0
  published 2026-10-23), and the APK could go back to plain MPL-2.0 plus Apache-2.0. That is the re-evaluation Q9 asked for,
  and a project of its own: Piper and Kokoro voices would need a lexicon, so the catalog changes. It solves the license
  only; the JNI question above stays.
- A separate companion APK with an ASR-only JNI without eSpeak NG (the demo's would do), reached over Binder. Two programs
  at arm's length.
- A separate process in the same APK: still one package, whether that is an "aggregate" is arguable. Not advised.

## Model binding

| File | Bound to | First load | Notes |
| --- | --- | --- | --- |
| `libmodel.so` | Nothing (generic) | 9 s (5 s model) to 106 s (20 s model) on SM8475; upstream says about 20 s for its 5 s log | Needs `libQnnHtpPrepare.so` (74 MB, 31 MB compressed) and 1.9 GB of peak memory; writes `model.bin` |
| `model.bin` | One SoC and one QNN SDK version | 0.6 to 0.7 s | 161 to 168 MB download per SoC, no `Prepare` library; catalog entries would be split per SoC family |

With `libmodel.so`, "downloading a model" means downloading native code and loading it from `filesDir`. The demo targets
SDK 34, the plugin 37; loading a downloaded `.so` from there was not tried at 37 and it changes what the signed catalog
vouches for.

HTP architectures (`.github/scripts/export-qnn/device_info.py`): SM8350 v68; SM8450, SM8475 v69; SM8550 v73; SM8650 v75;
SM8750 v79; SM8850 v81. Each architecture also needs its own `libQnnHtpV*Skel.so` and `Stub`.

## Applicable SoCs

Upstream publishes models for SM8350 (106 assets, fewer than the others), SM8450, SM8475, SM8550, SM8650, SM8750, SM8850,
SM8845, and the automotive / IoT parts SA8255, SA8295 and QCS9100. Nothing for Snapdragon 7 or 6 series (`SM7xxx`,
`SM6xxx`), MediaTek, Exynos or Tensor. arm64-v8a only.

| Device | SoC | HTP | QNN | CPU RTF, 10 min SenseVoice (`asr-baseline.md`) |
| --- | --- | --- | --- | --- |
| Xiaomi 23046RP50C (968e9f18) | SM8475 | v69 | Yes, measured | 0.034 |
| OnePlus OPD2413 (63751f63) | SM8750P | v79 | Yes, not measured: busy with api-coverage P6.2 on the day | 0.043 |
| Sony XQ-DQ72 (QV770340J7) | Not read: its remote adb has been unreachable since 2026-10-26. Public specifications of the model say SM8550 | v73 if so | Probably, not verified | 0.056 |
| Sony XQ-AT72 (QV710AF65F) | SM8250 | - | No | 0.054 |
| Sony G8441 (BH900ASK9E) | MSM8998 | - | No | 0.330 |
| Redmi 22120RN86C (bek749scrwv4wo8h) | MT6769V | - | No | 0.250 |

## Measurements

### Method

- Device: Xiaomi 23046RP50C, SM8475, Android 15, 2026-11-02. No plugin code: a small harness under `app_process` as the
  shell user (not an app), so `untrusted_app` was not covered. JNI and QNN libraries from the demo APK (sherpa-onnx 2.0.1,
  QNN 2.40); the same JNI ran both providers, so the comparison is only the provider.
- CPU: the catalog's `model.int8.onnx` of `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8`, 2 threads, as in the plugin.
- NPU: upstream's `libmodel.so` of the 5 s and 20 s `sense-voice-zh-en-ja-ko-yue-2024-07-17-int8`, same weights as the
  CPU model in origin. The first load generates `model.bin`.
- Recordings of `locks/test-audio.lock` (`zh` cut to 1 and 3 s as well), 6 decodes each, median. Process CPU time from
  the harness. Peak memory is `VmHWM`.
- The harness is not part of the plugin.

### Latency

Median ms per decode, and real time factor. Phone awake, nothing else running.

| Speech | CPU | NPU, 20 s model | NPU, 5 s model |
| --- | --- | --- | --- |
| 1.00 s | 53 (0.053) | 218 (0.218) | 62 (0.062) |
| 3.00 s | 102 (0.034) | 237 (0.079) | 66 (0.022) |
| 5.10 s | 157 (0.031) | 225 (0.044) | 76 (0.015) |
| 6.63 s | 204 (0.031) | 243 (0.037) | 79, truncated |
| 10.05 s | 305 (0.030) | 242 (0.024) | 79, truncated |
| 17.64 s | 583 (0.033) | 243 (0.014) | 100, truncated |

| | CPU | NPU, 20 s model | NPU, 5 s model |
| --- | --- | --- | --- |
| First load ever | 1.4 s | 105.7 s | 9.0 s |
| Later loads | 1.4 s | 0.7 s | 0.6 s |
| Peak memory, first load | 520 MB | 1.89 GB | 1.70 GB |
| Peak memory, later loads | 520 MB | 667 MB | 663 MB |
| Process CPU per decode | 107 - 1143 ms | 33 - 63 ms | 28 - 60 ms |

Upstream's log (documentation, device not named): 5.592 s of audio, 0.095 s (RTF 0.017) on a first run and 0.297 s
(RTF 0.053) on a second, which matches the scale here; SM8850 with a binary, 5 s model: 0.052 s (RTF 0.009).

### Text

Against the CPU text of the same recording. There is a reference only for `en`.

| Recording | 20 s model | 5 s model |
| --- | --- | --- |
| `en`, 6.63 s | Identical | Cut off after "the squalid" |
| `zh-8k`, 17.64 s | Identical | Cut off |
| `zh`, 5.10 s | 7 of 20 differ: "OSOS" becomes "Al always" (no reference; cannot say which is right) | Close to the CPU, with a leading comma |
| `zh` cut to 3 s | Identical | Leading comma |
| `zh` cut to 1 s | The CPU gets one character, the NPU none | Same |
| `mixed-zh-en`, 10.05 s | 6 of 42 differ, plainly worse: "昨天是Mdaytoday is Libathe day after tomorrow是星期三" for "昨天是monday, today is礼拜2, the day after tomorrow是星期三" | Cut off after "today is" |

Why the code-switched recording suffers is not known, and four recordings cannot show whether it generalizes.

### Power

The method of P8.1 (`resident-power-baseline.md`): `batterystats --reset`, `dumpsys battery unplug` on USB, screen off,
30 minutes each. The four recordings in turn, each decoded once per its own length, as if someone were speaking
continuously. Same harness, so the recognizer's work only.

| 30 min, 1813 s of audio, 184 decodes | CPU | NPU, 20 s model |
| --- | --- | --- |
| Busy wall time | 60.8 s (330 ms per decode) | 48.6 s (264 ms) |
| Real time factor | 0.034 | 0.027 |
| Process CPU time | 117.9 s (641 ms per decode) | 12.8 s (69 ms) |
| Peak memory | 514 MB | 670 MB |
| Charge counter | Unchanged | Unchanged |
| Batterystats estimate of UID 2000 | No row | No row |

The real energy is not measured, and these numbers cannot give it:

- The phone stayed at 100 % on USB, so the battery did not move (actual drain 0 in both runs), and `dumpsys battery unplug`
  does not stop the charger from carrying the load.
- The estimate comes from the power profile, has no NPU term, and on this phone it listed no CPU row for the shell at all.
- `/sys/class/power_supply/*` is not readable by the shell (permission denied), so the charger's input cannot be sampled.

What is known: 9.2 times less process CPU time. What is not: what the HTP draws meanwhile. Answering that takes an
unplugged run from a part charge, reading the charge counter before and after, which needs the maintainer's hands. The
conclusion below does not depend on it: even continuous speech keeps the recognizer busy for 3 % of the time, and the
resident wake word, the one load that runs for hours, cannot use the NPU.

## Size

| Item | Size |
| --- | --- |
| Plugin arm64-v8a release package 1.3.0 | 15.2 MB |
| QNN JNI of the demo, ASR only, compressed | 1.70 MB (4.19 MB raw) |
| In the APK, compressed: `libQnnHtpPrepare.so` | 31.04 MB |
| `libQnnHtp.so`, `libQnnSystem.so` | 0.92 MB each |
| Skel and Stub of six architectures | About 18.6 MB |
| Total: everything / no `Prepare` (needs per-SoC `model.bin`) / one architecture with `Prepare` | About 51 / 20 / 36 MB |
| Uncompressed on the device | About 140 MB |
| Model download | 162 MB (generic), 161 - 168 MB (per SoC) |
| Model on the device | `libmodel.so` 238 MB + `model.bin` 250 - 254 MB, about 490 MB (CPU model 239 MB) |

## Risks

- `#2808` ([Galaxy S22, Android 15, crash on first start](https://github.com/k2-fsa/sherpa-onnx/issues/2808)) is open.
  Upstream's stability on OEM builds is what kept QNN out of 1.3.0 (P7.2).
- The demo declares `<uses-native-library android:name="libcdsprpc.so">`; the plugin does not. ColorOS, HyperOS and One UI
  gate FastRPC differently; only HyperOS was run, and only as shell.
- An HTP performance mode ([#4117](https://github.com/k2-fsa/sherpa-onnx/issues/4117), 2026-10-28) landed after v2.0.1,
  so no prebuilt has it. Whether it lowers the 240 ms, and at what power, is not measured.
- First run: 105.7 s and 1.9 GB of peak memory on SM8475.
- A QNN SDK update invalidates every `model.bin`; `model.bin` catalog entries multiply by SoC and SDK version.

## Not verified

- Real power on any device (see "Power").
- SM8750P or any other SoC than SM8475. XQ-DQ72's SoC.
- `untrusted_app`: everything ran as shell. Upstream's demo is an ordinary app, so this should work at its target SDK 34;
  the plugin's target SDK 37 was not tried.
- Streaming on QNN.
- Text quality beyond four recordings.

## Conclusion

No-go for 1.4.0 (D177). Reasons, strongest first:

1. **It cannot ship the simple way.** Recognition on the NPU and TTS in one JNI means building sherpa-onnx ourselves, and
   today's package conflicts with the GPL-3.0-or-later. Without a build of our own, it takes a separate companion APK;
   without a companion APK, it takes the migration to 2.0.x (Q9) first and a build of our own.
2. **The gain is small where it can be had.** On SM8475 the 20 s model is slower than the CPU for anything under 8 s, and
   1.25 times faster on the four recordings together. The CPU already has a margin of about 30 times real time.
3. **It does not reach the devices that need it.** No test device that struggles with SenseVoice has a supported HTP, and the
   resident wake word has no QNN model.
4. **The costs are real**: +20 to +51 MB on a 15 MB APK, 490 MB per model, a first load of 106 s, a revocable license
   with an indemnity that states no cap, and worse text on the mixed recording.

The one real gain is 9.2 times less process CPU time, whose battery value is unknown.

### Worth another look when

- The plugin has moved to sherpa-onnx 2.0.x (Q9) and upstream publishes an official QNN AAR with TTS.
- A model with real length handling appears, or tiers can share a context, so that short speech no longer pays for 20 s.
- QNN models for keyword spotting or VAD appear; that is where the battery goes.

### If the decision is go anyway

A separate companion APK, ASR only, after 1.4.0: about 3 to 5 weeks. It would hold the demo's prebuilt JNI, the QNN
libraries of the architectures chosen and per-SoC `model.bin` downloads; Macaw would reach it over Binder with a CPU
fallback, and it needs its own signing, listing and license notices. It needs the maintainer to accept the Qualcomm
license and take a position on sections 8, 9 and 10 (f).

## Sources

- Upstream documentation: <https://k2-fsa.github.io/sherpa/onnx/qnn/>, sources in `k2-fsa/sherpa`,
  `docs/source/onnx/qnn/*.rst`; `run-executables-on-your-phone.rst` and `run-executables-on-your-phone-binary.rst` for the logs.
- Releases: <https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models-qnn> and
  <https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models-qnn-binary>, each with `-2` and `-3`.
- Demo APK: <https://huggingface.co/csukuangfj2/sherpa-onnx-apk>.
- Qualcomm AI Stack License: `LICENSE.pdf` in `qairt/2.40.0.251030` of the QAIRT SDK.
- Removal of eSpeak NG: `CHANGELOG.md` of tag v2.0.1 (`#4055`); `cmake/` of that tag has no espeak or piper module.
- Plugin: `THIRD_PARTY_NOTICES.md`, `docs/development/asr-baseline.md`, `docs/development/resident-power-baseline.md`.
