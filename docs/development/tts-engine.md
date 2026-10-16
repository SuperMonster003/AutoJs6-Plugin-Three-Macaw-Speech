# Speech synthesis and the system TTS engine

Roadmap P4 (D3, D19, D20, appendix D Q6). 3-Macaw Speech synthesizes with sherpa-onnx 1.13.8 `OfflineTts` on two paths
that share the loaded models: `ISpeechPlugin.synthesizeToFile` (the script's `speech.synthesize`) and the Android system
TTS engine `ThreeMacawSpeechTtsEngineService` (AutoJs6's `tts` module with the `engine` option, `speech.speak` and any
other app). Measurements are in `tts-baseline.md`.

## Models and configurations

`core/tts/TtsEngineCore` builds the `OfflineTtsConfig` from the model's `runtime.config` (catalog models) or from the
roles `RuntimeRoles` infers for imported ones:

| Model type | Roles | Catalog models |
| --- | --- | --- |
| `vits` | `model`, `tokens`, `lexicon[]`, `dataDir`, `ruleFsts[]`, `ruleFars[]` | `tts.vits.melo.zh-en`, `tts.vits.icefall.zh-aishell3`, `tts.vits.piper.en-us-lessac-medium` |
| `matcha` | `acousticModel`, `vocoder`, `tokens`, `lexicon[]`, `dataDir`, `ruleFsts[]` | `tts.matcha.icefall.zh-baker` |
| `kokoro` | `model`, `voices`, `tokens`, `lexicon[]`, `dataDir`, `ruleFsts[]` | `tts.kokoro.multi-lang.v1_1` |

Lexicons and rule FSTs / FARs are joined with commas; `maxNumSentences` is 1 and 4 onnxruntime threads (at most the
processor count) run per model, twice the recognizers' 2 (`tts-baseline.md` has the comparison).
An imported TTS model without `runtime.config` gets `model*.onnx` (int8 preferred, otherwise the first top-level
`.onnx`), `tokens.txt`, `lexicon*.txt`, top-level `*.fst` / `*.far`, `voices.bin`, `espeak-ng-data/`, a
`model-steps-*.onnx` acoustic model and a `hifigan*` / `vocos*` vocoder. Speaker names come from a `speakers.txt` next to
the model (one name per line, line `n` is speaker `n`; AISHELL-3 has one).

Loaded models live in the engine cache shared with recognition (roadmap D30): released after 5 minutes without use and
when a model is removed, and weighed by their installed bytes against one memory budget (a quarter of the device memory,
within 384 MiB and 1.5 GiB). Before a model loads, the idle instances released longest ago are closed until the new one
fits; instances in use are never closed. The system engine also drops idle instances on `onTrimMemory`
(`TRIM_MEMORY_RUNNING_LOW` and above).

## The synthesis loop

`TtsSynthesis` splits the text with `SentenceSplitter` and synthesizes chunk by chunk:

- Sentences end at the Chinese terminators (ideographic full stop, full-width exclamation and question marks and
  semicolon, ellipsis), at `.!?;`, at line breaks and after a run of them, closing quotes and brackets included. An ASCII
  period ends a sentence only before white space, the end of the text or a CJK character, and never inside a number
  (`3.14`), after a single Latin letter (`J. K.`), after a token with inner periods (`U.S.`, `e.g.`) or after a known
  abbreviation (`Mr.`, `Dr.`, `etc.`, months, weekdays).
- A sentence longer than 120 characters is cut at the last comma, colon or dash before the limit (not one between digits
  such as `1,000` or `12:30`), then at the last white space, then before the word or number that would cross the limit.
- Chunks without a letter or digit are skipped; 120 ms of silence separates two spoken chunks.
- Progress reports the characters done after every chunk; a cancel is checked between chunks and in the generation
  callback, so it takes effect within one sentence. Nothing leaves the native callback as an exception: a failure of the
  output stops the generation and is thrown once it returned.

## `synthesizeToFile`

Request keys (protocol `docs/dev/speech-plugin-protocol-v1.md` of the host): `text` (1 to 8000 UTF-16 units, not blank),
`model` (default TTS model when absent), `speaker` (a number or a `speakers.txt` name), `speed` (0.5 to 2.0, the sherpa-onnx
speed: the length scale is divided by it) and `format` (`wav`). The call checks the request, the model kind and the
speaker and answers the operation id at once; the model loads on the plugin's synthesis thread `MacawTts`, where the
operations run one after another. The output is a mono PCM16 WAV at the model's sample rate written into a duplicate of
the host's descriptor, header sizes patched at the end; a file that would pass 100 MiB fails with `LIMIT_EXCEEDED`. The
completion carries `sampleRate`, `durationMillis`, `modelId`, `speakerId`, `sizeBytes` and `warnings`. A model with a
single speaker ignores a requested speaker other than `0` and reports it in `warnings` (`INVALID_OPTIONS`); an unknown
speaker of a multi-speaker model, a speed out of range or a non-TTS model fail the call with `INVALID_OPTIONS`, a text
over 8000 characters with `LIMIT_EXCEEDED`, an unknown or missing model with `MODEL_MISSING`. `cancelOperation` (or the
death of the host's callback Binder) fails the operation with `SESSION_CANCELLED`; the host discards the partial file.

## System TTS engine

Manifest: the service with the `android.intent.action.TTS_SERVICE` filter and the `android.speech.tts` meta-data
(`res/xml/tts_engine.xml`, whose `settingsActivity` is the plugin's `ui.settings.SettingsActivity`, opened at the synthesis
group since roadmap P5.2), plus `CheckVoiceDataActivity` (`CHECK_TTS_DATA`), `GetSampleTextActivity` (`GET_SAMPLE_TEXT`)
and the home screen `ui.home.HomeActivity` for `INSTALL_TTS_DATA` (scrolled to the synthesis models). The engine and the
two data activities are exported without the plugin permission, like the settings and home screens: Android's
`TextToSpeech` binds the engine from the client app's own process and the system settings start the activities. They
only synthesize or answer what the engine can speak. The settings' per-language default voices and the speaker of
each model feed the engine's voice table; speed and pitch come from the request.

### Voice names (appendix D Q6)

Every installed TTS model contributes one voice per speaker and language:

- The voice in the speaker's first language is `<modelId>/<speakerId>`, for example `tts.vits.icefall.zh-aishell3/10`.
  This is the name `speech.speak({model, speaker})` asks for (roadmap D19).
- The same speaker in the model's other languages is `<modelId>/<speakerId>/<languageTag>`, for example
  `tts.vits.melo.zh-en/0/en-US`. Voice names stay unique (Android requires it), and an app that asks for English by
  locale finds an English voice of a bilingual model.
- Locales come from the catalog languages: `zh` -> `zh-CN`, `en` -> `en-US` (also `ja-JP`, `ko-KR`, `yue-HK` for future
  models).
- A speaker's first language is the model's first catalog language, except where the catalog's per-model languages are
  not true for every speaker. `TtsSpeakerHints` records that case: Kokoro multi-lang v1.1 has 3 English speakers (0 to 2)
  and 100 Chinese ones (3 to 102). The English speakers read mixed Chinese and English text with English phonetics
  (SenseVoice CER 1.0 on the device test sentence, against 0.13 to 0.28 for Chinese speakers), so `tts.kokoro.multi-lang.v1_1/0`
  is an `en-US` voice (with `/0/zh-CN`), `.../3` a `zh-CN` voice (with `/3/en-US`), and the model's default speaker is 3.
- Quality: 22.05 kHz and more `QUALITY_HIGH`, 16 kHz `QUALITY_NORMAL`, less (AISHELL-3, 8 kHz) `QUALITY_LOW`; latency
  `LATENCY_NORMAL`; no voice needs the network. The list follows the installed models at every call.

A client that sends a speaker by name (`<modelId>/<speakerName>`, as `speech.speak({speaker: "SSB0005"})` builds it) is
accepted by `onIsValidVoiceName` / `onLoadVoice` and synthesis, but `getVoices` lists numbered names only.

### Languages and defaults

- `onIsLanguageAvailable`: `LANG_COUNTRY_AVAILABLE` when an installed voice has the language and the country,
  `LANG_AVAILABLE` for the language in another region (the models read Chinese or English whatever the region),
  `LANG_MISSING_DATA` when only catalog models that are not installed speak it, otherwise `LANG_NOT_SUPPORTED`. ISO 639-2
  and 639-1 codes are both accepted, `cmn` reads as Chinese.
- `onGetDefaultVoiceNameFor` and a request without a voice: the default TTS model first (`speech.models.setDefault`),
  then a voice in its first language, then the matching country, then the installed order, then the model's default
  speaker.
- `CHECK_TTS_DATA` lists `<iso3 language>-<iso3 country>` of every installed language plus the common regions of Chinese
  (CN, TW, HK, SG, MO) and English (US, GB, AU, CA, IN, IE, NZ, SG, ZA), because the system settings enable the sample
  only for a device locale in that list; the catalog languages without an installed model are listed as unavailable.
- `GET_SAMPLE_TEXT` answers a Chinese or an English sentence; other languages get `LANG_NOT_SUPPORTED` and the settings
  use their own sample.

### Synthesis

`onSynthesizeText` resolves the voice (request voice name, else the default voice of the request language), loads the
model through the shared cache (the first request after an idle release waits for the load), starts the callback with
the model's sample rate, PCM16 mono, and streams every generated piece in buffers of `getMaxBufferSize()`. The Android
speech rate (100 = normal) maps to the sherpa-onnx speed `rate / 100` within 0.25 and 4; pitch has no equivalent and is
ignored (logged once). `onStop` stops the running synthesis within one sentence. Without an installed model a synthesis
fails with `ERROR_NOT_INSTALLED_YET`; a model that fails to load or synthesize gives `ERROR_SYNTHESIS`. Every request
logs its character count, voice, rate and audio length under `MacawTtsEngine`, never the text.
