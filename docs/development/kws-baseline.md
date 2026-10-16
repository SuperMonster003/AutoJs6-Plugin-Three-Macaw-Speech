# Keyword spotting baseline

Roadmap P3.4 / P3.5. Measured on 2026-10-16 with the debug build of the P3 commit, sherpa-onnx 1.13.8 and the catalog
model `kws.zipformer.zh-en.2025-12-20` (version 1, chunk-16-left-64 int8, installed from the `speech-models-v1`
mirror). Instrumentation `KeywordSpottingDeviceTest`, run with `py .python/device_speech_run.py test <serial>
KeywordSpottingDeviceTest`; the results are the `MacawDeviceRun` log lines of the run.

## Method

- Keywords: the ten keywords of the model's `test_wavs/keywords_raw.txt` (eight Chinese, `LIGHT UP`, `LOVELY CHILD`),
  spelled by the plugin (`KeywordTokenizer`: customary pinyin split into initial and tonal final, English phones from
  the model's `en.phone`). The plugin's spelling equals the upstream `keywords.txt` token for token
  (`KeywordTokenizerTest`), and the spotter accepts it with an empty built-in keyword file (appendix D Q4).
- Recordings: `test-audio/kws-zh-3` .. `kws-zh-6` and `en` of `locks/test-audio.lock` (upstream `test_wavs`, 16 kHz);
  `kws-zh-0` .. `kws-zh-2` hold none of the ten keywords and serve as background speech.
- Hit rate: every recording five times in one stream with 1.5 s of silence in between, at the original level and at a
  quarter of it (about -12 dB); a hit counts when the keyword the single recording triggers fires again.
- False wakes: ten minutes of other speech (the bilingual recognizer's test recordings and `kws-zh-0` .. `kws-zh-2`,
  separated by one second of low noise) against the custom keywords `小六小六` and `hey macaw`.
- Round: a wake session with `thenListen` on the real engines, fed `kws-zh-6` (keyword `朱丽楠`), 0.3 s of silence and
  the mixed zh-en command; it must report the hit with round 0, a final of the round and `end(stopped)` of the round.

## Results

| Device | API | Keywords per recording | Hit rate, original level | Hit rate, quarter level | False wakes in 10 min | Real time factor | Round |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Sony G8441 (BH900ASK9E) | 28 | as below | 37/40 | 33/40 | 0 | 0.094 | pass |
| Sony XQ-AT72 (QV710AF65F) | 31 | as below | 37/40 | 33/40 | 0 | 0.035 | pass |
| Sony XQ-DQ72 (QV770340J7) | 33 | as below | 37/40 | 33/40 | 0 | 0.027 | pass |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | as below | 37/40 | 33/40 | 0 | 0.104 | pass |
| Xiaomi 23046RP50C (968e9f18) | 35 | as below | 37/40 | 33/40 | 0 | 0.022 | pass |
| OnePlus OPD2413 (63751f63) | 35 | as below | 37/40 | 33/40 | 0 | 0.018 | pass |

The real time factor is that of the ten minute false wake run on the final build. Keywords each single recording
triggers (identical on all six devices): `kws-zh-3`: 文森特卡索, 法国; `kws-zh-4`: 蒋友伯,
女儿; `kws-zh-5`: 周望军, 落实; `kws-zh-6`: 朱丽楠; `en`: LIGHT UP. `见面会` and `LOVELY CHILD` are not in these
recordings. Misses at the original level: one of five for 文森特卡索, 女儿 and LIGHT UP; at the quarter level two of
five for 文森特卡索, one for 女儿 and 落实, three for LIGHT UP. The five repetitions of `kws-zh-6` also fired another
keyword of the list twice at the original level (once at the quarter level): a keyword list with similar syllables
should raise the `threshold` of the keyword that misfires.

The detector reports no score: sherpa-onnx 1.13.8 exposes the keyword, its tokens and timestamps only, so the `score` of
a `wake` event is the trigger threshold of that keyword (0.25 unless the script sets `threshold`).

## Through the host and the microphone

`py .python/device_host_scripts.py wake <serial>` runs a script in AutoJs6 (in the foreground) that opens
`speech.wake` with the keyword `朱丽楠` and `thenListen`, then plays `kws-zh-6` twice and the mixed command through the
device speaker into its own microphone. It passes when the script sees the `wake` event, a `final` of the round and the
`end(stopped)` of the round: Sony G8441 (API 28), XQ-AT72 (API 31, at 70 % music volume; 40 % was too quiet for its
microphone), XQ-DQ72 (API 33) and OnePlus OPD2413 (API 35) pass. On the Xiaomi 23046RP50C (API 35) Android refused the
microphone foreground service (`warning` `PERMISSION_DENIED`, see D22) and the session heard nothing.
