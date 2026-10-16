# Speech recognition baseline

Roadmap P3.2 / P3.3 / P3.5. Measured on 2026-10-16 with the debug build of the P3 commit (sherpa-onnx 1.13.8,
onnxruntime 1.28.2, 2 intra-op threads per recognizer) and the catalog models
`asr.zipformer-streaming.zh-en.2023-02-20.int8` (streaming transducer) and
`asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (offline, VAD segments), both version 1 and installed from the
`speech-models-v1` mirror by `SpeechModelSetupDeviceTest`. Instrumentation `SpeechRecognitionDeviceTest`, run with
`py .python/device_speech_run.py test <serial> SpeechRecognitionDeviceTest`; the numbers are its `MacawDeviceRun` lines.

## Method

- Load: the first `EngineCache.acquire` of a model (from a cold cache), with the growth of the native heap; PSS of the
  process right after.
- Files: `ISpeechPlugin.recognizeFile` (in-process Binder) on the recordings of `locks/test-audio.lock`: `en` (LibriSpeech,
  6.6 s, reference transcript), `zh` (5.1 s), `mixed-zh-en` (10.1 s) and `zh-8k` (17.6 s at 8 kHz, resampled by the
  plugin). Real time factor = wall time / audio time. Character error rate (letters and digits, case, white space and
  punctuation ignored) only for `en`, the one recording with a reference transcript; the Chinese and mixed recordings have
  none, so their texts are checked for presence only.
- First partial: an external session (`source: "external"`) fed `mixed-zh-en` in 20 ms chunks at real-time pace; the
  latency is the time from the first audible sample of the recording to the first `partial` event. SenseVoice reports
  partials per VAD segment, so its latency includes the first segment and the 1.2 s of silence before it closes.
- Long files: 30 s and 10 min of the four recordings in turn with 0.5 s gaps, written as one 16 kHz WAV; real time factor
  and the peak PSS of the process sampled every 200 ms while both models are loaded.
- The VAD gate opens the recognizer's stream 1 s before the VAD fires. With the first value, 480 ms, the streaming
  transducer lost the first five words of `en` (CER 0.368): a stream that starts inside speech drops up to two seconds of
  words (raw stream started at 224 ms into `en`: CER 0.000; at 448 ms: 0.368). `test0StreamingPipelineKeepsWhatTheRawStreamHears`
  guards the gated and ungated pipelines against the raw stream.

## Results

Load in ms (native heap growth in MiB); file real time factors as the range over the four recordings (the first call,
`en`, is the slowest: it pays for the first inference of the freshly loaded model); first partial in ms after the first
audible sample; 10 min real time factors and the peak PSS of the 10 min runs.

| Device | API | Load streaming | Load SenseVoice | Load KWS | CER `en` streaming / SenseVoice | File RTF streaming | File RTF SenseVoice | First partial streaming / SenseVoice | 10 min RTF streaming / SenseVoice | Peak PSS |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Sony G8441 (BH900ASK9E) | 28 | 1731 (228) | 2841 (303) | 1091 | 0.046 / 0.000 | 0.302 - 0.582 | 0.354 - 0.758 | 587 / 2509 | 0.324 / 0.330 | 714 MiB |
| Sony XQ-AT72 (QV710AF65F) | 31 | 965 (218) | 1848 (270) | 637 | 0.034 / 0.000 | 0.115 - 0.281 | 0.066 - 0.360 | 390 / 2010 | 0.105 / 0.054 | 746 MiB |
| Sony XQ-DQ72 (QV770340J7) | 33 | 536 (218) | 1188 (270) | 371 | 0.034 / 0.000 | 0.074 - 0.176 | 0.041 - 0.231 | 352 / 1942 | 0.089 / 0.056 | 905 MiB |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | 2193 (224) | 3839 (302) | 1355 | 0.034 / 0.000 | 0.392 - 0.725 | 0.233 - 0.873 | 586 / 2325 | 0.412 / 0.250 | 759 MiB |
| Xiaomi 23046RP50C (968e9f18) | 35 | 616 (210) | 1415 (261) | 445 | 0.034 / 0.000 | 0.090 - 0.207 | 0.044 - 0.267 | 361 / 1979 | 0.080 / 0.034 | 830 MiB |
| OnePlus OPD2413 (63751f63) | 35 | 380 (222) | 727 (303) | 280 | 0.034 / 0.000 | 0.053 - 0.122 | 0.035 - 0.155 | 379 / 1943 | 0.062 / 0.043 | 719 MiB |

- Every device recognizes all four recordings with both models; SenseVoice reports `en` for `en` and `zh` for the
  Chinese and mixed recordings. The texts of the streaming model are identical on all devices (the gated, ungated and
  raw pipelines all give CER 0.034 on `en`), so the BH900 value of 0.046 comes from the slower run of the same file.
- The 10 min peak PSS is that of the instrumentation process with both recognizers loaded (the test runs inside the
  plugin process); it stays below 1 GiB on every device, the Redmi with 3.7 GiB of RAM included. The slowest device,
  the Redmi, runs the streaming model at 0.41 of real time over 10 minutes, so a microphone session keeps up with the
  recorder everywhere.
- Battery drain was not measured (the devices were on USB power); the real time factor is the CPU proxy.
- A device with 16 KB pages was not available (the AVDs were offline and no such phone was connected), so the 16 KB row
  of P3.5 stays open; the native libraries passed the 16 KB alignment check of P0.2.
