# sherpa-onnx 2.0.x migration evaluation

Roadmap P9.6.1 (D178, Q138, Q9). Started on 2026-11-02 against sherpa-onnx v2.0.1, the newest release; the plugin is on
1.13.8. Experiments ran in a private copy; nothing in the plugin changed for this evaluation. License readings below are
an engineer's reading, not legal advice.

## Result

| Question | Answer |
| --- | --- |
| Prebuilt AAR | Usable: 4 ABIs, 16 KB alignment, API 24 emulator passes, no Maven Central as before |
| API change | One line of plugin code; JVM tests 490 of 492 (the other two pin the version) |
| Speech recognition, keywords, analysis, speakers | Same results on G8441, scores identical. Language detection on 90 s took 17.4 s against 14.1 s |
| APK size | 69 KB smaller on arm64-v8a, 249 KB on universal |
| TTS models (13) | 3 work untouched, 2 (Kokoro) lose words, 8 give no audio until repackaged; 7 of those have no upstream lexicon |
| Text that is not in a lexicon | Skipped silently, numbers included: "Room 204" reads "Room". Kokoro v1.0 keeps 23 - 57 % of its audio in es, fr, pt, it, hi |
| Quality with third-party lexicons | Worse: read-back error de 0.065 -> 0.269, ru 0.151 -> 0.419 |
| Quality with eSpeak-built lexicons | Kept, except numbers; whether such a lexicon is free of the GPL is unsettled |
| Effort | 13 - 19 session days for all languages; 8 - 10 for zh and en only; 10 - 14 for a separate GPL phoneme APK |

Decision: **no-go**, the plugin stays on 1.13.8 (maintainer, 2026-11-02, Q139, D179). 1.4.0 goes on to P9.5 with no catalog
change. Details and reevaluation conditions are in "Conclusion".

## Release form

Compared `sherpa-onnx-1.13.8.aar` (the plugin's) with `sherpa-onnx-2.0.1.aar` (49441341 bytes, SHA-256
`a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7`, tag commit
`af2d8d81dbfa61c4cea4c568da78f3982edca005`), with an ELF reader of our own.

| Item | 1.13.8 | 2.0.1 |
| --- | --- | --- |
| ABIs | arm64-v8a, armeabi-v7a, x86, x86_64 | Same |
| Maven Central | No | No, so the same `libs/` + lock file (D13) |
| `libsherpa-onnx-jni.so` arm64-v8a | 4771760 bytes | 4609320 bytes (-3.4 %) |
| armeabi-v7a | 3428780 bytes | 3381796 bytes |
| x86 | 5358928 bytes | 5261876 bytes |
| x86_64 | 5201192 bytes | 5053592 bytes |
| `libonnxruntime.so` | 1.28.2 | Byte-identical in arm64-v8a |
| JNI `DT_NEEDED` | `libandroid`, `liblog`, `libonnxruntime`, `libm`, `libdl`, `libc` | Same |
| Load segment alignment | 16 KB in all four ABIs, both libraries | Same |
| JNI minimum API note | 21 | 21 |
| `libonnxruntime.so` minimum API note | 27 (a note only; runs on API 24 today) | 27, same binary |
| AAR `minSdkVersion` | 21 | 21 |

JNI static components, from the `cmake/` directory of each tag:

| Component | 1.13.8 | 2.0.1 | License |
| --- | --- | --- | --- |
| eSpeak NG, UCD Tools | Yes | **Removed** | GPL-3.0-or-later |
| piper-phonemize (with uni-algo) | Yes | **Removed** | MIT, Public Domain |
| utf8proc | No | **Added** | MIT-style, to be listed in the notices |
| cargs | In `cmake/` | Removed | MIT, not linked into the JNI in either |
| kaldi-native-fbank, kaldi-decoder, kaldifst, OpenFst, Eigen, simple-sentencepiece, JSON for Modern C++, hclust-cpp | Yes | Yes | Unchanged; pinned versions to be checked when the notices are written |

`phontab`, the eSpeak NG data table name, occurs 6 to 8 times in each 1.13.8 JNI and 0 times in 2.0.1. What is left of
`espeak` and `piper` in the 2.0.1 strings is deprecation messages and function names such as `PiperPhonemesToIdsKitten`.

## API differences

Upstream's Kotlin sources changed in five files; only three matter here.

| Upstream file | Change | Effect on the plugin |
| --- | --- | --- |
| `Vad.kt` | `SpeechSegment.start` is `Long` (was `Int`), `#4079`, fixes a SIGSEGV on long streams | One compile error: `core/runtime/SherpaRuntime.kt:97`; `SherpaEngines.kt:181` and `AnalysisResults.kt:152` compile unchanged |
| `OfflineRecognizer.kt` | `OfflineRecognizerResult.words`, `OfflineCtcFstDecoderConfig` (new, defaulted) | None |
| `Tts.kt` | `dataDir` deprecated and ignored; `GenerationConfig.phonemeCodepoints`, `tokens`; `OfflineTts.lang()`; `OfflineTtsKittenModelConfig.lexicon` | Compiles unchanged; behavior changes, see "TTS models and catalog" |
| `OfflineSpeechDenoiser.kt` | VoxLock | Not used |
| `OfflineDiacritization.kt` | New | Not used |

JNI changes worth having: recognizer and punctuation constructors now raise Java exceptions instead of aborting the
process (`#3988`); text with characters outside the BMP no longer comes back garbled or drops the end (`#4062`, `#4064`).

| Plugin file | Change |
| --- | --- |
| `core/runtime/SherpaRuntime.kt` | 1 line (`segment.start.toInt()`) |
| `locks/sherpa-onnx.lock`, `libs/`, `libs/README.md`, `app/build.gradle.kts` comments | AAR swap |
| `NativeRuntimeLockTest` | 2 tests pin the version; they are the only JVM failures |
| `SherpaRuntimeDeviceTest`, `SherpaReleaseProbeInstrumentation` | Version strings |

In the private copy: JVM 492 tests, 490 passed, the 2 failures are those lock tests. The debug APKs and the device test
APK build.

## TTS models and catalog

### What 2.0 does with text

`#4055` left two front ends: a lexicon inside sherpa-onnx, or phonemes computed outside and passed in
`GenerationConfig.phonemeCodepoints` / `tokens`. `dataDir` and Kokoro's `lang` are ignored with a warning. Upstream's own
Android engine (`android/SherpaOnnxTtsEngine`) either sets a lexicon or depends on the GPL-3.0 `piper-phonemize` AAR.

Two consequences, read in `offline-tts-vits-impl.h` and `offline-tts-kokoro-impl.h` and confirmed by runs:

- A Piper voice with neither gives no audio: 0 samples and only a log line ("phoneme_codepoints is empty, no lexicon, and no
  frontend available"), not an exception.
- A word that is not in the lexicon is **skipped**, with a log line. 1.13.8 fell back to eSpeak NG for it. Digits are such
  words: nothing expands them unless a rule FST does. Upstream's answer for Kokoro English is an open pull request, `#4144`.

Issue #3731 promised on 2026-07-15 that "a lexicon.txt file will be added to every model directory". On 2026-11-02 none
of the Hugging Face Piper repositories checked (en_US-lessac, de_DE-thorsten, ru_RU-denis) has one and the upstream archives
are unchanged since 2025. Upstream publishes exactly two general lexicons: `lexicon-en-us.txt` (168654 entries, 6.3 MB)
and `lexicon-zh-g2pw.txt`. Its header credits "CMUDict + en_list + compounds"; the generating script is not in the
repository. `en_list` is the name of an eSpeak NG data file, so its license is not clear (see "Licenses and documents").

### Catalog v3 entries

13 TTS models. "espeak data" is 355 files, 17991651 bytes, in each of the 10 models that carry it.

| Model | Languages | 1.13.8 front end | 2.0.1 |
| --- | --- | --- | --- |
| `tts.vits.icefall.zh-aishell3` | zh | Lexicon, FST | Loads as is |
| `tts.vits.melo.zh-en` | zh, en | Lexicon, FST | Loads as is |
| `tts.matcha.icefall.zh-baker` | zh | Lexicon, FST | Loads as is |
| `tts.kokoro.multi-lang.v1_1` | zh, en | Lexicon, eSpeak NG for out-of-vocabulary words | Loads; those words are skipped |
| `tts.kokoro.multi-lang.v1_0.int8` | en, zh, es, fr, hi, it, pt | Lexicon for en and zh, eSpeak NG for the rest | Loads; es, fr, hi, it, pt lose almost everything |
| `tts.vits.piper.en-us-lessac-medium` | en | eSpeak NG | No audio without a new file; upstream lexicon usable with gaps |
| `tts.vits.piper.de-de-thorsten-medium.int8` | de | eSpeak NG | No audio; no upstream lexicon |
| `tts.vits.piper.es-es-davefx-medium.int8` | es | eSpeak NG | Same |
| `tts.vits.piper.fr-fr-siwis-medium.int8` | fr | eSpeak NG | Same |
| `tts.vits.piper.ru-ru-denis-medium.int8` | ru | eSpeak NG | Same |
| `tts.vits.piper.ar-jo-sa-dii-high.int8` | ar | eSpeak NG | Same |
| `tts.vits.piper.ar-jo-sa-miro-v2-high.int8` | ar | eSpeak NG | Same |
| `tts.vits.mimic3.ko-ko-kss-low` | ko | eSpeak NG | Same; no lexicon anywhere |

So 3 of 13 keep working untouched, 2 lose words, 8 stop speaking until they are repackaged, and for 7 of those there is
no upstream replacement.

### Lexicon sources tried

PC, sherpa-onnx Python wheels 1.13.8 and 2.0.1, same voices. 14 everyday sentences per language (12 for English), written
for this evaluation: deliveries, weather, directions, 7 to 9 numbers. Coverage counts running words. "Same as eSpeak"
compares phonemes without stress marks; the reference is eSpeak NG through the `piper-tts` 1.8.0 wheel, the phonemizer the
voices were trained on (their `.onnx.json` says `"phoneme_type": "espeak"`).

| Language | Lexicon | Entries | Size | Coverage | Same as eSpeak |
| --- | --- | --- | --- | --- | --- |
| en | Upstream `lexicon-en-us.txt` | 168654 | 6.3 MB | 98.5 % | 97.9 % |
| de | voicegarden `deu` | 333945 | 13.7 MB | 98.0 % | 24.3 % |
| es | voicegarden `spa-ES` | 627587 | 20.6 MB | 100 % | 60.4 % |
| fr | voicegarden `fra` | 182176 | 4.7 MB | 100 % | 80.7 % |
| ru | voicegarden `ru` | 545315 | 22.8 MB | 99.2 % | 0 % |
| ar | voicegarden `ara` | 64813 | 1.9 MB | 70.9 % | 21.7 % |
| ko | None | | | | |

[voicegarden-lexicons](https://github.com/AACTools/voicegarden-lexicons) v0.3.1 is a third-party MIT / BSD archive, the only
source found for these languages. It is built from gruut and similar data, not eSpeak NG. The ASCII `g` and tie bars had to
be folded before its phones existed in the voices' `tokens.txt`; it has no stress marks, and `ru` writes `baːbuʂkʲi` where
the voice learned `bˈɑbuʃkʲɪ`.

### Read-back

Each sentence synthesized, then transcribed by Whisper base int8 in the voice's language; character error rate against
the input, ignoring case, spaces and punctuation. Whisper is imperfect, Arabic especially, so compare columns, not rows.
"eSpeak built" is a best case: a lexicon of exactly the corpus words made with eSpeak NG on the PC, so only the 2.0.1 text
path differs. Audio seconds show what was dropped.

| Language | 1.13.8 eSpeak NG | 2.0.1 lexicon above | 2.0.1 eSpeak built |
| --- | --- | --- | --- |
| en | CER 0.036, 42.0 s | 0.047, 38.8 s | 0.049, 40.1 s |
| de | 0.065, 54.0 s | 0.269, 48.5 s | 0.086, 50.0 s |
| es | 0.070, 48.2 s | 0.134, 41.9 s | 0.083, 48.3 s |
| fr | 0.135, 49.0 s | 0.144, 43.7 s | 0.118, 49.6 s |
| ru | 0.151, 52.8 s | 0.419, 45.8 s | 0.178, 51.0 s |
| ar | 0.447, 54.4 s | 0.580, 33.1 s | 0.365, 47.4 s |

Reading it: the third-party lexicons are plainly worse for de, es, ru and ar. eSpeak-built lexicons keep the quality, which
shows the voices need their own phonemes, and what is left is the loss of numbers: "moved to room 204 on the second floor"
comes back as "moved to room on the second floor" in both 2.0.1 columns.

### Gaps in common words

332 frequent English words were looked up.

| Lexicon | Missing |
| --- | --- |
| Upstream `lexicon-en-us.txt` | `her`, `or` |
| Kokoro `lexicon-us-en.txt` (v1.0 and v1.1, identical) | `apps`, `degrees`, `downloads`, `emails`, `euros`, `kilometers`, `minutes`, `opened`, `opens`, `videos`, `wifi` |

"Tea or coffee?" reads "Tea coffee?" with Piper on 2.0.1 (confirmed from the debug log). Kokoro's lexicon lacks inflected
forms that its Python front end derives by rule. On the plugin's own English README, 8.1 % and 11.2 % of the running words
are missing respectively, and 449 number runs per 9668 words would be silent.

### Kokoro

`tts.kokoro.multi-lang.v1_0.int8` on the PC, audio seconds per sentence.

| Language | Sentence | 1.13.8 | 2.0.1 |
| --- | --- | --- | --- |
| en | "... moved to Room 204 on the second floor." | 4.93 | 3.87 |
| en | "Your battery is at 15 percent, it is 23 degrees outside." | 5.45 | 4.37 |
| en | "Siobhan and Nguyen flew from Xiaomi Campus to Guangzhou on a Boeing 737." | 5.76 | 3.80 |
| zh | Date, temperature, telephone number | 6.83 | 7.16 |
| zh + en | The baseline's mixed sentence | 2.80 | 4.01 |
| es | One sentence | 4.92 | 2.22 |
| fr | One sentence | 5.63 | 2.15 |
| pt | One sentence | 4.72 | 2.61 |
| it | One sentence | 5.04 | 2.87 |
| hi | One sentence | 5.30 | 1.23 |

Chinese keeps its numbers through the rule FSTs. English does not. The five other languages are gone: their words are
skipped, and what is left is whatever happens to be spelled like an English word.

### Keeping eSpeak NG out of process, or in

| Route | Result |
| --- | --- |
| `piper-phonemize` AAR v1.5.9 (JitPack, GPL-3.0) in the APK | Keeps every voice with no catalog change, adds about 3 MB per ABI plus 18 MB of data; **the APK stays GPL**, so nothing is gained for Q9. armeabi-v7a and x86 are 4 KB aligned, which the plugin's alignment gate refuses |
| The same library in a separate APK, phonemes over Binder | The main APK leaves the GPL; a second APK to sign, list and attach source for, and every non-Chinese voice needs it installed |
| floravox-capi (Rust, third-party) | Same alphabet as voicegarden, so the results above; no Android build published |

### Chinese: polyphones and numbers

PC, 6 sentences with `行`, `重`, `长`, `乐`, `还`, a date, a price and a telephone number, read back by Whisper in Chinese.

- Matcha zh-baker: audio length equal within 0.02 s per sentence, transcripts equal apart from script variant and one
  character. No change between the versions. Polyphones behave the same, including the misreading they share.
- Kokoro v1.0 int8: 0.06 to 0.26 s longer per sentence on 2.0.1. Whisper's text is too poor on this speaker in both
  versions to tell whether polyphones changed (not conclusive). On the phone, Kokoro v1.1's mixed test sentence improved.

## Device results

Sony G8441 (API 28), debug builds of the same commit with the project's debug key, 1.13.8 first, models kept. The 2.0.1
synthesis test ran without Piper, which otherwise stops it.

| Test | 1.13.8 | 2.0.1 |
| --- | --- | --- |
| `SherpaRuntimeDeviceTest` (native set, versions, 15 VAD reference segments) | 4 / 4 | 4 / 4 |
| `SpeechRecognitionDeviceTest` (zh-en streaming, SenseVoice) | 6 / 6 | 6 / 6 |
| SenseVoice, 10 minutes | RTF 0.311, 818 MiB | RTF 0.330, 817 MiB |
| `KeywordSpottingDeviceTest` | 7 / 7; hits 37 / 40 and 33 / 40; 0 false wakes in 10 minutes | Identical |
| `AnalysisDeviceTest` | 7 passed, 1 skipped | Same |
| Language, 90 s of speech | 14.1 s | 17.4 s |
| Diarization, 42.5 s of audio | 19.6 s, accuracy 0.931 | 19.0 s, accuracy 0.931 |
| Tagger, 3 windows | 1024 ms | 828 ms, same labels and scores |
| `SpeakerDeviceTest` | 2 / 2 | 2 / 2, identical scores |
| `PunctuationDeviceTest` | Skipped (needs models this phone lacks) | Skipped |
| AISHELL-3: load, RTF | 1900 ms, 0.166 | 1870 - 2139 ms, 0.164 - 0.166 |
| MeloTTS | 6278 ms, 2.401, CER 0.125, 3 / 3 English words | 5491 - 6206 ms, 2.413 - 2.563, CER 0.063 - 0.219, 2 - 3 / 3 (the voice samples noise; earlier spread 0 - 0.188) |
| Matcha zh-baker | 4830 ms, 0.436 | 4670 - 4879 ms, 0.526 - 0.585 (slower in both 2.0.1 runs; cause not looked into) |
| Kokoro v1.1, mixed sentence | 3290 ms, 3.438, CER 0.125 | 4155 - 4323 ms, 3.432 - 3.438, CER 0.000 |
| Piper en-US, installed package | 2918 ms, 0.392 | **0 samples**, test fails: "0 ms for 61 characters" |
| Piper en-US + `lexicon-en-us.txt` (168654 entries) | | Load 3124 ms, RTF 0.273 on another sentence, peak 374 MiB |
| Piper ru + 545315 entries | | Load 8222 ms, first audio 2.5 s, RTF 0.721, peak 472 MiB |
| 8000 characters | RTF 0.193 | RTF 0.198 |
| Cancel | 227 ms | 258 ms |
| System engine | Passes, 384 voices | Passes, 384 voices |

Everything that is not text-to-speech gives the same results, with scores identical to the last digit. Two speed changes
were seen and not looked into: language detection (14.1 -> 17.4 s) and Matcha (RTF 0.436 -> 0.526 - 0.585). The cost of this
migration is in TTS.

Emulators, debug builds of 2.0.1, native code only (no models): API 35 16 KB x86_64 (`-memory 4096`) and API 24 x86:
`SherpaRuntimeDeviceTest` 4 / 4, `ThreeMacawSpeechContractTest` 8 / 8 on both, so minSdk 24 and 16 KB hold.
`verify_native_alignment.py` passes on the 2.0.1 AAR, and `:app:assembleRelease` builds.

| Release APK | 1.13.8 | 2.0.1 |
| --- | --- | --- |
| arm64-v8a | 15295293 | 15225781 (-69 KB) |
| armeabi-v7a | 14471335 | 14411971 (-59 KB) |
| x86 | 16721931 | 16662743 (-59 KB) |
| x86_64 | 16493136 | 16439708 (-53 KB) |
| universal | 46888666 | 46639406 (-249 KB) |

## Catalog v4 and installed models

- A new wire schema is unavoidable. Plugins up to 1.3.0 keep reading `catalog-v3.json` and run 1.13.8, where a Piper entry
  must have `dataDir`; a 2.0 plugin needs the opposite. So schema 4, `catalog-v4.json`, with `CatalogRules.SCHEMA_VERSION`,
  `BUNDLED_SCHEMA`, `ThreeMacawSpeechContract.CATALOG_VERSION`, the Shared-Assets `distribution.py` / `crypto.py` port,
  `generate_model_fixtures.py`, `locks/speech-catalog.lock`, `BundledCatalogTest`, and the `CATALOG_SCHEMAS` companion
  rule of D127. `minSherpaOnnx` is stored but enforced nowhere, so it cannot do this job.
- Entries that change: the 10 models with eSpeak data lose 17991651 bytes each and gain a lexicon of 2 to 25 MB, as
  `versionCode` 2 (3 for Kokoro v1.1). Licenses lose `AND GPL-3.0-or-later`. Kokoro v1.0 loses 5 of 7 languages unless
  lexicons appear for them. `runtime.minSherpaOnnx` becomes 2.0.1. Mirror archives in a new immutable Release.
- Pending files go to Shared-Assets `speech-models/pending/v4` (local only). Signing and the Release `speech-models-v4`
  stay with the maintainer (D108, D127).
- Download size barely moves: Piper en 81.1 -> 69.5 MB, int8 voices 36.6 -> 20 to 41 MB, Kokoro 405.6 -> 387.7 MB.

Installed models after an update of the plugin:

| State | Behavior on 2.0 unless the plugin intervenes |
| --- | --- |
| Piper / mimic3 installed from catalog v3 | Loads, then 0 samples and no exception: silence from `speech.synthesize`, the tool and the system TTS engine. Today's `TtsSynthesis` would report success |
| Kokoro installed | Keeps speaking, drops words; no error at all |
| Matcha, AISHELL-3, MeloTTS | Unchanged |
| Imported model with `espeak-ng-data/` | Same as Piper: silence. Must be refused or warned about, and `RuntimeRoles` must stop inferring `dataDir` |

The P9.3 update flow helps: a bundled catalog v4 makes these rows show "update available" at once. But D29 forbids
downloading without the user's action, so between plugin update and model update the voice needs an explicit error, and the
system engine must report `ERROR_NOT_INSTALLED_YET` or similar. A script that names a Piper voice fails until someone
opens the plugin. Rolling the model back is not possible once updated: an older plugin cannot use the new package. Android
does not allow an APK downgrade without losing the data anyway, so the only way back after release is a new version.

Effect on earlier decisions:

| Decision | Effect |
| --- | --- |
| D175 | `EspeakHome`, `EspeakLink`, `ModelStore.beforeDelete`, the `noBackupFilesDir/espeak` link and `EspeakHomeTest` go away, with the two `ModelUpdateDeviceTest` cases; the fix becomes unnecessary. Old links are left to clean up once |
| Q68, D107 | None: they are about recognition punctuation |
| D19, Q6 (voice names) | Kept when model ids stay; Kokoro v1.0's speakers for es, fr, hi, it, pt must leave `TtsVoiceTable`. `TtsSpeakerHints.engineLanguage` has nothing to do any more (`lang` is ignored) |
| D124, D127 (P8.2) | The TTS languages 1.3.0 added (ar, ko, plus de, es, fr, ru from 1.2.0) are what is at stake |
| D158, D166, D173, D174 | Reused for the update |
| D31 (compressed native libraries), D14, D13 | Unchanged |
| D32, D40, P5.2, P6.4 | See "Licenses and documents" |
| D177 | Not reopened: no QNN AAR, see below |

## Licenses and documents

Only a list; nothing was changed.

| Where | Change on a migration |
| --- | --- |
| `THIRD_PARTY_NOTICES.md` | Opening paragraph; rows of eSpeak NG, UCD Tools, piper-phonemize; sections "GPL-3.0-or-later code in the speech runtime" and "Unicode data"; sherpa-onnx row to v2.0.1; kaldi-native-fbank `v1.22.3` -> `v1.22.4`; new row utf8proc `v2.12.0` (MIT-style); closing paragraph |
| About page (P5.2, D40) | `ui/about/NoticesActivity`, strings `notices_gpl`, `notices_gpl_summary`, `notices_statement`, `settings_notices_summary` in 11 value folders; `assets/legal/` shrinks to `LICENSE` and the notices |
| `third_party/licenses/GPL-3.0.txt`, `COPYING.UCD`, `locks/legal-texts.lock`, `generateLegalAssets` | Removed or reduced |
| Release source archive (P6.4) | `locks/corresponding-source.lock`, `.python/corresponding_source.py`, its tests, `release_sums.py`, R4 of the release checklist. Stops at the first release without eSpeak NG. Existing Releases keep their archives: they were offered with the APKs, so the archives must stay as long as those APKs are offered |
| D32, Q9 | New decision supersedes the GPL part; Q9 closes |
| `.readme/lang_*.json` (10), `common.json`, generated READMEs | `p_license`, the FAQ "why is the APK GPL", component lists |
| `.changelog/lang_*.json` (10) | Entry for the license change, and one for each lost language |
| Catalog | `license` of 10 entries (see above) |
| Shared-Assets `RIGHTS_AND_TAKEDOWN.md`, audits | Checked by the maintainer |

The license gain has a catch. The only lexicons that keep quality are built with eSpeak NG. Whether its output is covered by the
GPL is unsettled for dictionary data: the GPL FAQ treats program output as free of the program's license unless the program
copies parts of itself into it, and a pronunciation dictionary's output is largely what its own dictionary tables say.
Upstream's `lexicon-en-us.txt` names `en_list`, and states no license. So a migration that keeps voice quality may
move the GPL question from the APK to the model packages rather than end it. That is for the maintainer to judge, and it
is the reason a lexicon pipeline should not be started before that decision.

What the GPL costs today, for comparison: one archive of 54 MB per Release, a tool that builds it, one screen, one notice
section. All done and running since 1.0.0. The only thing it blocks is shipping closed libraries in the same APK, and QNN is
no-go for other reasons (D177).

## Effort

Session days, at the pace of 1.3.0 (five roadmap items in six days, one card each). Not counted: the maintainer's signing.

| Item | All languages kept | Only zh, en kept |
| --- | --- | --- |
| Runtime swap, native report, version tests | 0.5 | 0.5 |
| Lexicon pipeline: clean word lists, phonemes, gaps such as `or`, Kokoro inflections | 3 - 4 (ar and ko have no usable list) | 1 |
| Text normalizer: numbers (`android.icu` spellout, API 24), dates, times, units, fallback for unknown words | 3 - 5, 7 languages | 1 - 2, en only (zh has FSTs) |
| Catalog v4, Shared-Assets tooling, fixtures, dev snapshot | 2 | 1.5 |
| Migration: silent voices, explicit errors, system engine, imports, removing D175 | 2 - 3 | 2 |
| Notices, About page, README, changelog (10 languages), d.ts, documentation site, Ace, Offline Docs | 1 - 2 | 1 - 2 |
| Regression: every voice, 3 phones, API 24 and 35 16 KB AVDs | 1 - 2 | 1 |
| Total | 13 - 19 days | 8 - 10 days |

A separate GPL companion APK that holds `piper-phonemize` and answers over Binder: about 10 to 14 days (a new repository,
signing, official index entry, source archive, a Binder contract, fallback when it is missing, 16 KB alignment of its 32-bit
libraries). Every voice stays identical, and users of non-Chinese voices must install a second APK.

## Risks

- Quality regressions that do not show as errors: words and numbers vanish silently, on the path scripts rely on.
- Breaking change for every script that names a Piper voice, and for system TTS users, right after 1.3.0 added those voices.
- A catalog signing round with the maintainer (one is not otherwise needed in 1.4.0; D177 said so).
- Memory and load time of large lexicons on low-end phones: see "Device results".
- Upstream is moving: the promised per-model lexicons, `#4144` and whatever replaces the Kokoro front end may land soon. Work
  done now may be thrown away. Conversely 1.13.8 will not get fixes.
- Ten-language copy and documentation sync, all local until the maintainer pushes.
- Rollback: before release, drop the branch of changes; nothing has been published. After release, a new version only.

## Official QNN AAR

Recorded for D177, not acted on. v2.0.1 has no QNN AAR or jniLibs among its 288 assets, and the only official AARs are
`sherpa-onnx-2.0.1.aar`, `-rknn` and `-static-link-onnxruntime`. On `master`, `#4126` (2026-10-29) exposes `QnnConfig` in
the Java and Kotlin bindings, after the release. D177's condition is not met.

## Not verified

- By ear. Quality here is read-back by Whisper base and phoneme comparison; nobody listened.
- `tts.vits.mimic3.ko-ko-kss-low` on 2.0.1 beyond the source reading: no lexicon exists to try.
- The eSpeak-built lexicon beyond the corpus words: real coverage depends on a word list nobody has chosen.
- OnePlus OPD2413 and XQ-AT72: not installed (ColorOS install dialog; the 1.3.0 package stays on the XQ-AT72). G8441 only.
- Models on the emulators: native code and contract only.
- The release build on a device. R8 keeps `com.k2fsa.sherpa.onnx.**` whole, as today.
- `lexicon-zh-g2pw.txt`, `lexicon-gb-en.txt`: not tried; neither helps the voices at stake.
- Kokoro v1.1 on the PC: the tables used v1.0 int8; its lexicons are the same for English.

## Conclusion

No-go for 1.4.0 (D179). The maintainer chose the recommended option on 2026-11-02.

Why:

- The gain is small. The APK barely shrinks, and the GPL duties it would remove are already met by tooling that has run since
  1.0.0: one 54 MB source archive per Release, one screen, one notice section.
- The cost is user-visible. 8 of 13 voices stop speaking until repackaged, and most lack a lexicon that matches what they
  were trained on. Numbers and unknown words vanish without an error on the path scripts rely on.
- The only lexicons that keep quality are built with eSpeak NG, so the license gain itself is uncertain.
- Upstream is moving: the per-model lexicons promised in `#3731` have not appeared, and `#4144` is open. Work done now may
  be replaced within months.

Options not taken: all languages (13 - 19 days), only zh and en (8 - 10 days, drops the de, es, fr, ru, ar, ko voices 1.2.0 and
1.3.0 added), a separate GPL phoneme APK (10 - 14 days, a second APK for every non-Chinese voice).

Reevaluate when any of these holds:

- Upstream ships multi-language lexicons that match the voices' phonemes (the promise in `#3731`), or a built-in fallback for
  unknown words with number normalization (such as `#4144`, extended past English).
- 1.13.8 shows a defect that can only be fixed by upgrading.
- Upstream ships an official QNN AAR with TTS and the maintainer decides to adopt it (D177).

The part that is not TTS costs one line, so when a condition holds the work is in catalog v4 and the migration of installed
models. Rerun the read-back tables first: they compare the same sentences and voices.

## Sources

- Upstream: <https://github.com/k2-fsa/sherpa-onnx/issues/3731>, <https://github.com/k2-fsa/sherpa-onnx/pull/4055>,
  <https://github.com/k2-fsa/sherpa-onnx/pull/4144>, `CHANGELOG.md`, `cmake/`, `sherpa-onnx/kotlin-api/`, `sherpa-onnx/jni/` and
  `sherpa-onnx/csrc/offline-tts-*-impl.h` of tags v1.13.8 and v2.0.1, `android/SherpaOnnxTtsEngine` of v2.0.1, release
  `tts-models`.
- piper-phonemize: <https://github.com/csukuangfj/piper-phonemize>, JitPack `com.github.csukuangfj.piper-phonemize:piper-phonemize:v1.5.9`.
- voicegarden-lexicons v0.3.1: <https://github.com/AACTools/voicegarden-lexicons>.
- Plugin: `THIRD_PARTY_NOTICES.md`, `docs/development/tts-engine.md`, `docs/development/tts-baseline.md`,
  `docs/development/npu-qnn-evaluation.md`, `docs/development/catalog-v3-release.md`.
