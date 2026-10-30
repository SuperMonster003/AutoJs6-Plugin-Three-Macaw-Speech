<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Offline speech recognition, wake word detection, voice activity detection and neural text-to-speech, usable as the system TTS engine</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The README.md file is currently available in the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Macaw Speech uses [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) to run speech recognition, voice activity detection, wake word detection and neural text-to-speech offline on the device. Everything is computed locally and no audio is uploaded.

The plugin is a separately installed APK with its own screens for downloading and managing models, trying them out and changing settings. AutoJs6 scripts use it through the global `speech` module; the speech synthesis also registers as an Android system TTS engine, and the recognition is also offered as system voice input, a system speech recognition service and a digital assistant, so other apps can use them too. Recognition and synthesis models are not bundled with the APK but downloaded on demand; only the voice activity detection model is built in.

> The `speech` module in scripts needs AutoJs6 6.8.0 (internal build 5456) or later. The system TTS engine, system voice input, speech recognition service, digital assistant and the plugin's own screens do not depend on AutoJs6 and work once the plugin is installed and models are downloaded.

******

### Features

******

- Speech recognition: streaming dictation from the microphone (partial and final results in real time) and recognition of audio files (WAV, MP3, AAC, Ogg, FLAC and more, or the sound of a video file), with offline models for Chinese, English, mixed Chinese and English, Japanese, Korean and Cantonese. French, Russian and Korean also have streaming models that show the text while you speak. Version 1.3.0 adds an offline Spanish model (with punctuation) and a streaming model for eight languages that detects the language by itself: Arabic, Indonesian, Thai, Vietnamese, Japanese, Russian, Chinese and English.
- Automatic punctuation: once a punctuation model is installed, each sentence that a streaming model recognizes gets commas, periods and question marks (Chinese and English), and English text gets its capital letters back; SenseVoice adds its own punctuation and is left as is. Punctuation models are downloaded from the "Punctuation" group on the home screen.
- Voice activity detection: the built-in silero VAD v5 splits audio into speech segments on its own and also ends dictation sentences automatically.
- Wake words: type the keyword as plain text (for example `小六小六` or `hey macaw`) without training a model; after waking up, dictation can start automatically.
- Text-to-speech: several neural Chinese and English voices, synthesized to WAV files or used as the system TTS engine by the AutoJs6 `tts` module and other apps. French, Spanish, German and Russian Piper voices are available too (non-commercial use only). Version 1.3.0 adds the Kokoro v1.0 multilingual voices (English, Spanish, French, Hindi, Italian, Brazilian Portuguese and Chinese) and Arabic and Korean voices (non-commercial use only).
- System voice input: microphone buttons in other apps, such as 3-Stove Agent, can use the offline recognition; you say one sentence in a bottom sheet and the recognized text goes back to that app.
- Digital assistant: choose 3-Macaw Speech as the default digital assistant app, then long press the power or Home button, say one sentence, edit the recognized text if needed and hand it to 3-Stove Agent, 3-Stone AI or another app that takes voice commands, or copy it. It also provides a system speech recognition service: on Android 11 and lower, choosing it as the assistant usually makes it the default recognition service too; from Android 12 on, choose it separately in the system's voice input setting (some phones lack this setting), and apps can also name it directly.
- Trigger entries: open the same dictation sheet without the power button: a Quick Settings tile (also on the lock screen), a home screen widget and an app icon shortcut, a draggable floating microphone (off by default), a key trigger that holds or double presses a volume key (an accessibility service that only filters keys, off by default) and the voice key of a Bluetooth headset. Nothing records until the sheet listens, and only for the few seconds of dictation.
- Resident wake word (off by default): once on, saying a wake word (`小鹦小鹦` and `Hey Macaw` by default, editable) opens the same dictation sheet, turns the screen on when it is off or locked, and reads the recognized text aloud. While it listens, the microphone indicator stays lit, a notification stays and the battery drains faster (Power saving mode, on by default, does not run the wake word model while it is quiet, which takes much less CPU); a listening time (turn off after 10, 30 or 60 minutes, or always on) and Listen only while charging can be set. When a script, another app or another entry needs the microphone, it steps aside and listens again afterwards.
- Speaker recognition (1.3.0): in Settings > Resident wake word > Speakers, enter a name and read 3 sentences aloud to enroll your voice (several people can enroll, and Try recognition shows the score). With "Only enrolled voices" on, the resident wake word compares the voice first and opens the sheet only when it matches an enrolled person, so other people saying the wake word are ignored; manual entries such as the tile, the widget and the headset key are not affected. Scripts can enroll, verify and identify speakers with `speech.speaker` (AutoJs6 6.8.0 or later). The speaker recognition model is about 26.5 MB; the Speakers page shows how to download it.
- Audio analysis (1.3.0): file recognition on Try it out can also find speakers (who spoke when, with enrolled voices named, or "Speakers and text" to transcribe what each person says), identify the language (the 99 languages of Whisper) and tag sounds (527 kinds from AudioSet, such as speech, music, animals, vehicles and alarms). Everything runs offline on the phone once the models are downloaded (the screen shows which ones). Scripts can use `speech.diarize`, `speech.detectLanguage` and `speech.tag` (AutoJs6 6.8.0 or later).
- Plugin tools (AutoJs6 6.8.0 or later): four tools, `speech_listen` (hear one sentence and return the recognized text), `speech_speak` (read a text aloud), `speech_transcribe` (transcribe an audio file on the phone) and `speech_synthesize` (synthesize text to a WAV or M4A file). The MCP Server plugin uses them so that an AI assistant on a computer can listen and speak through the phone, and scripts can call them with `plugins.tools` too. 1.3.0 adds `speech_speaker_verify` (whether a recording is a given enrolled person) and `speech_speaker_identify` (the most similar enrolled person), which return only names and scores. 1.3.0 also adds three analysis tools: `speech_diarize` (find speakers, files of at most 10 minutes), `speech_detect_language` (identify the language) and `speech_tag` (tag sounds).
- Model management: downloads from a signed model catalog, enabled only after every file passes its SHA-256 check, plus import of local models; models restricted to non-commercial use show their license before the download.
- Standalone screens: the home screen shows the status and manages the models, Try it out tests dictation, wake words and synthesis directly and turns an audio or video file on the phone into text that you can save as plain text or SRT subtitles, and the settings change the language, night mode, theme color, launcher icon and the recognition and synthesis defaults; the appearance follows AutoJs6 by default.

******

### Screenshots

******

The screenshots come from a Sony XQ-DQ72 (Android 13). The dictated text is what the plugin recognized from a sherpa-onnx test recording played through the phone speaker; no personal data is shown.

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/home.png?raw=true" alt="Home: status and the models grouped by purpose" width="360" />
      <br />
      <sub>Home: status and the models grouped by purpose</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/tryout-dictation.png?raw=true" alt="Try it out: live dictation" width="360" />
      <br />
      <sub>Try it out: live dictation</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/voice-input.png?raw=true" alt="System voice input: the bottom sheet another app opened" width="360" />
      <br />
      <sub>System voice input: the bottom sheet another app opened</sub>
    </td>
    <td align="center">
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/docs/images/screenshots/assistant.png?raw=true" alt="Digital assistant: edit the result and choose the app that gets it" width="360" />
      <br />
      <sub>Digital assistant: edit the result and choose the app that gets it</sub>
    </td>
  </tr>
</table>

******

### Installation and use

******

1. Download the APK that matches the device ABI from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) and install it: most phones need `arm64-v8a`; if unsure, choose `universal`, which works on every device but is larger.
2. Open 3-Macaw Speech, grant the microphone permission on the home screen and download the models you need in the recognition, wake word and synthesis groups (the models the catalog recommends are a good start, see the model table below). On Android 13 and later the first download also asks to show download progress notifications.
3. Use it in AutoJs6: update AutoJs6 to 6.8.0 (internal build 5456) or later, enable 3-Macaw Speech in the Plugin Center and call the `speech` module in scripts (see Script API below). The AutoJs6 samples include four ready-to-run scripts under Samples > 3-Macaw Speech.
4. Use it in other apps: see System TTS setup below for the system TTS engine and the FAQ for system voice input, the speech recognition service and the digital assistant.

> There is one package per ABI and a `universal` package for every device. The packages contain no recognition or synthesis models; models are downloaded on demand from the plugin screens or a script, or imported from local files.

******

### Models

******

Recognition, punctuation, wake word, synthesis, speaker and audio analysis models take 6 MB to 410 MB each, so they are not bundled with the APK. The plugin carries a signed model catalog and downloads the models from immutable releases of the AutoJs6 shared assets repository, falling back to the upstream sherpa-onnx address when the mirror is unavailable; on both paths every file must pass its SHA-256 check before the model is enabled. The 1.3.0 catalog contains these models (sizes are the space they take once installed):

| Model ID | Purpose | Languages | Size | License | Commercial use |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (recommended) | Recognition (offline, with punctuation) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | Allowed |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (recommended) | Recognition (streaming) | zh, en | 199.1 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.en.2023-06-26.int8` (recommended) | Recognition (streaming) | en | 70.3 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | Recognition (streaming) | zh | 167.4 MB | None declared | Non-commercial only |
| `asr.zipformer-streaming.fr.2023-04-14.int8` (recommended) | Recognition (streaming) | fr | 129.0 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.ru.2025-08-16.int8` (recommended) | Recognition (streaming) | ru | 28.6 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.ko.2024-06-16.int8` | Recognition (streaming) | ko | 140.9 MB | Apache-2.0 | Non-commercial only |
| `asr.nemo-transducer.es.2026-05-03.int8` (recommended) | Recognition (offline, with punctuation) | es | 136.5 MB | CC-BY-4.0 | Allowed |
| `asr.zipformer-streaming.ar-en-id-ja-ru-th-vi-zh.2025-02-10.int8` | Recognition (streaming) | ar, en, id, ja, ru, th, vi, zh | 338.9 MB | Apache-2.0 | Unknown |
| `punct.ct-transformer.zh-en.2024-04-12.int8` (recommended) | Punctuation | zh, en | 75.5 MB | Apache-2.0 | Allowed |
| `punct.cnn-bilstm.en.2024-08-06.int8` (recommended) | Punctuation | en | 7.6 MB | Apache-2.0 | Allowed |
| `kws.zipformer.zh-en.2025-12-20` (recommended) | Wake word | zh, en | 8.8 MB | Apache-2.0 | Allowed |
| `tts.vits.melo.zh-en` (recommended) | Synthesis (speakers: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | Allowed |
| `tts.vits.icefall.zh-aishell3` (recommended) | Synthesis (speakers: 174, 8 kHz) | zh | 32.8 MB | None declared | Unknown |
| `tts.kokoro.multi-lang.v1_1` | Synthesis (speakers: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Allowed |
| `tts.matcha.icefall.zh-baker` | Synthesis (speakers: 1, 22.05 kHz) | zh | 81.0 MB | None declared, MIT (vocoder) | Non-commercial only |
| `tts.vits.piper.en-us-lessac-medium` | Synthesis (speakers: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `tts.vits.piper.fr-fr-siwis-medium.int8` | Synthesis (speakers: 1, 22.05 kHz) | fr | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `tts.vits.piper.es-es-davefx-medium.int8` | Synthesis (speakers: 1, 22.05 kHz) | es | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `tts.vits.piper.de-de-thorsten-medium.int8` | Synthesis (speakers: 1, 22.05 kHz) | de | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `tts.vits.piper.ru-ru-denis-medium.int8` | Synthesis (speakers: 1, 22.05 kHz) | ru | 36.6 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `tts.kokoro.multi-lang.v1_0.int8` | Synthesis (speakers: 49, 24 kHz) | en, es, fr, hi, it, pt, zh | 168.9 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Allowed |
| `tts.vits.piper.ar-jo-sa-dii-high.int8` | Synthesis (speakers: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-SA-4.0, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `tts.vits.piper.ar-jo-sa-miro-v2-high.int8` | Synthesis (speakers: 1, 22.05 kHz) | ar | 36.6 MB | CC-BY-NC-ND-4.0, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `tts.vits.mimic3.ko-ko-kss-low` | Synthesis (speakers: 1, 22.05 kHz) | ko | 80.8 MB | CC0-1.0, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |
| `speaker.eres2net.3dspeaker.en-voxceleb` (recommended) | Speaker recognition | en, zh | 26.5 MB | Apache-2.0 | Allowed |
| `segment.pyannote.3-0` (recommended) | Speaker segmentation | Any | 6.0 MB | MIT | Non-commercial only |
| `langid.whisper.tiny.int8` (recommended) | Language identification | 99 languages | 102.8 MB | MIT | Unknown |
| `langid.whisper.base.int8` | Language identification | 99 languages | 159.8 MB | MIT | Unknown |
| `tagger.ced.mini.int8` (recommended) | Audio tagging | Any | 10.5 MB | CC-BY-4.0, CC-BY-SA-4.0 (AudioSet labels) | Allowed |

Models restricted to non-commercial use or with an unknown license show their license before the download, and scripts must pass `acceptLicense: true` explicitly. The weights of the 2025 Chinese streaming model declare no license and its training data includes corpora for non-commercial use only, so it is downloaded only from the upstream sherpa-onnx address and not mirrored in the shared assets repository. The Kokoro and Piper synthesis models include the pronunciation data of eSpeak NG (espeak-ng-data, GPL-3.0-or-later). The training data of the Korean streaming model (KsponSpeech) is open to applications from Korean nationals only and needs a separate agreement for use outside Korea, so the model is marked as non-commercial and is not mirrored either: it is downloaded only from the upstream address (about 418 MB to download, 141 MB once installed). The French, Spanish, German and Russian Piper voices are all fine-tuned from the English lessac voice and, like it, are for non-commercial use only. The weights of the eight-language streaming model (PengChengStarling) are Apache-2.0, but its publisher does not name the training data, so its commercial use is marked as unknown. The Arabic dii and miro voices were trained on synthetic speech under CC BY-NC-ND 4.0; dii itself is CC BY-NC-SA 4.0 and miro CC BY-NC-ND 4.0. Because miro allows no adaptations, it is downloaded only from the upstream address and not mirrored in the shared assets repository. The weights of the Korean KSS voice are CC0, but its training data (KSS) is CC BY-NC-SA 4.0, so it is marked as non-commercial. The weights of the speaker segmentation model (pyannote segmentation 3.0) are MIT, but its training data includes MSDWild, which is for research only, so it is marked as non-commercial. The weights of the two Whisper language identification models are MIT, but OpenAI has not published the training data, so their commercial use is marked as unknown. The weights of the CED-mini audio tagging model are CC BY 4.0, and the AudioSet class table that comes with it is CC BY-SA 4.0.

Import a local model on the home screen takes a tar, tar.bz2 or zip archive or a folder: files that match a catalog model are registered as that model, other files become a custom model once you enter its kind, model type, name and languages. Import goes through the system file picker and needs no storage permission.

******

### System TTS setup

******

1. Download at least one synthesis model in the synthesis group of the plugin's home screen. To try it out, the smallest `tts.vits.icefall.zh-aishell3` (Chinese, 174 speakers) and the mixed Chinese and English `tts.vits.melo.zh-en` are both recommended.
2. Open the text-to-speech settings of the system and choose 3-Macaw Speech as the preferred engine. The System text-to-speech engine row on the status card of the plugin's home screen opens that page directly; otherwise it is usually under Settings > Accessibility > Text-to-speech output or Settings > System > Languages and input > Text-to-speech output (names vary by manufacturer).
3. The settings button next to the engine opens the settings of 3-Macaw Speech, where the synthesis group sets the default voice of each language and the speaker. The speech rate comes from the system setting or the app that asks for speech; pitch is not supported.
4. Back in the system settings, tap Listen to an example to hear the default voice of that language.

Every speaker of every model is a voice named `<model id>/<speaker number>`; the speakers of bilingual models also have `<model id>/<speaker number>/en-US` for English. AutoJs6 scripts can use the engine without changing the system setting: `tts(text, { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" })`, or `speech.speak(text, { model, speaker })` to name the model and speaker directly.

******

### Script API

******

From AutoJs6 6.8.0 (internal build 5456) on, scripts use these capabilities through the global `speech` module (also available as `$speech`). The three examples below can run on their own: dictation for 30 seconds that appends every sentence to a file, one sentence after a wake word, and synthesis to a file followed by speech:

```javascript
// 1. Dictation: partial text while you speak, one final per sentence, 30 seconds in all
let session = speech.listen({ silenceEndMillis: 1500, timeoutMillis: 0 });
session.on("partial", (e) => console.log("...", e.text));
session.on("final", (e) => files.append("./dictation.txt", e.text + "\n"));
setTimeout(() => session.stop(), 30000);

// 2. A wake word, then one round of dictation on the same recording
let wake = speech.wake(["hey macaw", "小六小六"], { thenListen: true });
wake.on("wake", (e) => e.listen.on("final", (r) => {
    console.log(e.keyword, r.text);
    wake.stop();
}));

// 3. Synthesis to a WAV file, and speech through the system TTS engine
let result = speech.synthesize("Task completed", "./done.wav", { model: "tts.vits.melo.zh-en" });
console.log(result.durationMillis, result.sampleRate);
speech.speak("任务完成");
tts("Task completed", { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" });
```

- `speech.listen(options)` opens a dictation session that reports through the events `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` and `end`; `stop()` ends it early. `speech.recognize(path)` recognizes an audio file (formats other than WAV need AutoJs6 6.8.0 or later; 16 kHz PCM is accepted too) and `speech.detectVoice(path)` lists its speech segments.
- The `punctuation` option of `speech.listen` and `speech.recognize` is true by default: the punctuation of the model itself (SenseVoice) is kept, the results of streaming models get punctuation from an installed punctuation model (Chinese and English, with English casing restored) and stay as they are when no punctuation model covers their language; false removes all punctuation. Punctuation models are managed only on the plugin home screen and are not listed by `speech.models`.
- `speech.wake(keywords, options)` opens a wake word session with the keywords written as plain text (at most 50); with `thenListen` every wake up turns into one round of dictation on the same recording, whose session is the `listen` property of the `wake` event.
- `speech.synthesize(text, path, options)` synthesizes to a WAV file (at most 8000 characters, with an optional model, speaker and speed); `speech.speak(text, options)` and `tts(text, { engine })` speak through the system TTS engine.
- `speech.models` lists, downloads (`install`, with progress), imports and removes models and sets the default models; `speech.capabilities()` answers the plugin version, its features and whether it holds the microphone permission.
- `speech.speaker.enroll(name, files, { replace })` enrolls a speaker (the recordings are averaged; the same name adds to the voiceprint by default, `replace: true` starts over), `verify(name, file)` returns the score and whether it matches, `identify(file, { topK })` finds the most similar enrolled person and lists the candidates, and `list()` and `remove(name)` manage enrolled people; `enroll`, `verify` and `identify` each have an `...Async` version that returns a Promise. Results hold only names, scores, whether they match and the threshold (0.50 by default, `threshold` changes it), never voiceprint vectors. Needs the AutoJs6 6.8.0 release and the speaker recognition model.
- `speech.diarize(file, options)` finds the speakers and returns their turns in time order (`segments`, each with `startMillis`, `endMillis` and `speaker` numbered from 0) and their number (`speakers`); the `speakers` option sets how many there are (0 counts them, at most 20), and `identify: true` names the speakers whose voice matches an enrolled voiceprint (`names`). `speech.detectLanguage(file, { windows })` judges the first windows of speech (30 seconds each, 3 by default) sentence by sentence: `language` is the language spoken longest, `candidates` gives each language's share of the speaking time and `segments` the language of each sentence. `speech.tag(file, { topK, timeline })` scores windows of 10 seconds and returns the best labels (the 527 English labels of AudioSet, 5 by default); `timeline: true` adds the labels of each window. All three report progress through `onProgress(done, total)` and have an `...Async` version that returns a Promise; they need the AutoJs6 6.8.0 release and the matching models.
- The whole plugin has one microphone session at a time (dictation or wake word); without the microphone permission the calls fail with `PERMISSION_DENIED`, which the plugin's home screen grants. Recognized text is never written to the AutoJs6 log.
- With AutoJs6 6.8.0 or later, scripts can also call these plugin tools through `plugins.tools.call`, for example `plugins.tools.call('speech_speak', { text: 'Hello' })`; the arguments are the same ones MCP Server sees. For everyday scripts the `speech` module is more flexible.
- The [AutoJs6 documentation](https://docs.autojs6.com/#/speech) describes every method, option, event and error code; the AutoJs6 Samples > 3-Macaw Speech hold eight examples: dictation to a file, wake word and dictation, TTS announcements, voice commands handed to the Agent, speaker recognition, speaker diarization, language identification and audio tagging.

******

### Compatibility

******

- Android 7.0 (API 24) or later, target API 37. Packages for arm64-v8a, armeabi-v7a, x86_64 and x86 plus a universal package; the native libraries are aligned to 16 KB memory pages and were verified on 16 KB page emulators.
- AutoJs6: the `speech` module of scripts needs 6.8.0 (internal build 5456) or later; the plugin screens, system TTS engine, system voice input, speech recognition service and digital assistant do not depend on AutoJs6. Recognizing audio files other than WAV (MP3, M4A and so on) and the plugin tools need the AutoJs6 6.8.0 release or later; with an earlier AutoJs6, scripts recognize WAV only. `speech.speaker`, `speech.diarize`, `speech.detectLanguage` and `speech.tag` in scripts also need the AutoJs6 6.8.0 release or later; the Speakers page, "Only enrolled voices" and the file analyses of Try it out in the plugin do not depend on AutoJs6.
- Background listening: a dictation or wake word session started while AutoJs6 is in the foreground keeps listening after AutoJs6 moves to the background or the screen turns off (the plugin keeps recording with a microphone foreground service and its notification). A session opened while AutoJs6 is already in the background may be silenced by the system: it then gets a `warning` event with the code `PERMISSION_DENIED` but does not end, and the audio comes back once AutoJs6 returns to the foreground; scripts that need to listen in the background should open the session while AutoJs6 is visible.
- Tested devices: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), and emulators with API 24, 35 and 37 (16 KB pages).
- Performance: on the low-end Sony G8441 a streaming model loads in about 1.7 seconds and SenseVoice in about 2.8 seconds, and dictation is processed about 3 times faster than real time; MeloTTS and Kokoro synthesize slower than real time on such devices, AISHELL-3 is the fastest. Newer devices such as the Sony XQ-DQ72 load in about 0.5 to 1.2 seconds and synthesize faster than real time.
- Memory: a loaded recognition model takes about 210 to 300 MB of native memory. The plugin keeps the loaded models within a quarter of the device memory (384 MB to 1.5 GB) and releases them after 5 idle minutes (1 to 30 minutes in the settings).
- Digital assistant: the system must let you choose a third-party digital assistant app. How to open it depends on the system: a long press of the Home button (three-button navigation), a swipe up from a bottom corner, or the power button set to open the digital assistant in the system settings. The ColorOS and HyperOS tablets tested (Android 15) keep the power button and the gestures for their own assistants; use the trigger entries there.
- Trigger entries: tested on Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15) and API 33 and 35 emulators. On ColorOS and HyperOS a tile tapped on the lock screen asks for the unlock first and then opens the sheet; some launchers accept a widget request without showing a confirmation, so add the widget by hand with the steps in the settings. With the screen off, phones pass no keys to accessibility services, so the key trigger works only while the screen is on. Bluetooth headset voice key: with a Sony WF-1000XM4 and the XQ-DQ72 (Android 13) every long press opened the sheet and the dictation used the headset's microphone; when another app (such as the Google app) answers too, the system shows a chooser first. On the HyperOS tablet (Xiaomi Pad 23046RP50C, Android 15) the chooser did nothing after 3-Macaw Speech was picked.
- Resident wake word: tested on Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15) and an API 35 emulator (16 KB pages). Battery use with the screen off for 30 minutes (Sony XQ-DQ72, Android 13; Android's estimate from the device's power profile, the phone connected by USB with the unplugged state simulated): on with Power saving mode (the default), 17.1 mAh and 235 seconds of the plugin's CPU time; on without it, 17.9 mAh and 454 seconds (about a quarter of one CPU core on average); recording all the time in both, and about 14 mAh of the estimate goes to the recording itself. Sounds went on around the phone during that test, so Power saving mode mainly saved CPU time; in a quiet room (Sony G8441, Android 9, screen off for 10 minutes) the plugin's CPU time fell from 265 to 26 seconds. Off, 0.006 mAh, the plugin not running. Actual use varies by device and by the sounds around it.
- Plugin tools: tested on Sony G8441 (Android 9), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15) and API 24 and 35 emulators (on API 24 only the parts that need no model); calls through MCP Server tested on Sony G8441 (Android 9).
- Punctuation and the models added in 1.2.0: tested on Sony XQ-AT72 (Android 12). With punctuation on, the Chinese, English and mixed recordings got punctuation and kept the same words as with it off; sentences synthesized by the Russian and French Piper voices were recognized back by the streaming model of the same language with character error rates of 7.9% and 0%; the Spanish and German voices synthesize normally, and the Korean model was only checked to load (there is no Korean test recording).
- Models added in 1.3.0: tested on Sony G8441 (Android 9), XQ-DQ72 (Android 13) and an API 35 emulator (install, use and removal). There are no Spanish, Arabic or Korean test recordings, so synthesized sentences were recognized back: the Spanish Kokoro v1.0 voice by the NeMo model with character error rates of 0% to 6.5%, both Arabic voices by the eight-language model with 3.0% (33% for miro on the emulator), and the Korean voice by SenseVoice with 0%; the eight-language model recognized the English recording with 0% to 1.1%. Once loaded, the eight-language model peaks at about 600 MiB (G8441) to 880 MiB (XQ-DQ72) of memory; Kokoro v1.0 synthesizes slower than real time on the G8441 (about 4 to 6 times the audio length) and about in real time on the XQ-DQ72.
- Recognizing files on Try it out: tested on Sony G8441 (Android 9), XQ-AT72 (Android 12), OnePlus OPD2413 (ColorOS, Android 15) and API 24 and 35 emulators. On the OnePlus OPD2413 a 10 minute 03 second MP3 was recognized in about 21 seconds (SenseVoice, automatic punctuation on); recognition goes on after you leave the screen, with the progress in a notification.
- Speaker recognition: tested on Sony XQ-AT72 (Android 12), XQ-DQ72 (Android 13) and API 24 and 35 (16 KB pages) emulators (enroll, verify, identify, list and remove, and "Only enrolled voices" letting a voice through or rejecting it; with two LibriSpeech readers and synthesized wake words). 2 second clips of the same person score 0.75 to 0.79 and those of the other person below 0.08, against the default threshold of 0.50; on a computer, with 40 English and 12 Chinese speakers, 2 second clips were falsely rejected about 2.5% (English) and 0% (Chinese) of the time and falsely accepted about 0.05% and 0.9%. The "Only enrolled voices" check takes 0.15 to 0.25 seconds on the Sony phones.
- Audio analysis: tested on Sony G8441 (Android 9), OnePlus OPD2413 (ColorOS, Android 15) and API 24 and 35 (16 KB pages) emulators (diarization with names, language identification, tagging, cancelling, the file screen, the three tools and AutoJs6 scripts). A 42.5 second recording in which two LibriSpeech readers take turns gave 2 speakers and 7 turns everywhere, with 93% of the speech time given to the right reader; the enrolled reader was named (score 0.85) and the other was not (0.01). Chinese, Russian and English recordings got the right language, and music, white noise and beeps were tagged in the right 10 second windows. A 10 minute recording took about 70 seconds to diarize, 2.5 seconds to identify the language and 3.8 seconds to tag on the OnePlus OPD2413, and 222 seconds (peak memory about 470 MiB), 12 seconds and 15 seconds on the Sony G8441.

******

### FAQ

******

#### Do recognition and synthesis need the Internet?

No. Once the models are downloaded, everything runs on the device and no audio is uploaded. The plugin only goes online to download models and refresh the model catalog.

#### Why does the plugin request the microphone permission itself?

Audio is recorded inside the plugin process and never passes through AutoJs6. AutoJs6 hands its foreground capabilities to the plugin when it binds, and the plugin holds a microphone foreground service during a microphone session, so dictation goes on after AutoJs6 moves to the background or the screen turns off. Grant the microphone permission on the plugin screen before the first use.

#### Which package should I install?

Most phones need `arm64-v8a`. If you are unsure about the device ABI, install `universal`: it works on every device but is larger.

#### Why does the example in the system settings stay silent, or the engine report missing voice data?

The engine reports only the languages of the installed synthesis models. Without a synthesis model, or when the installed models do not cover the device language, the system shows missing voice data and the example and the speech rate may be unavailable. Download a synthesis model for the device language on the plugin's home screen and open the system settings again; the example uses the default voice of that language, which the synthesis group of the plugin settings changes.

#### Can other apps use 3-Macaw Speech for voice input?

Yes. With a recognition model installed and the microphone permission granted, apps that start the system voice input (for example the microphone button of 3-Stove Agent) open the 3-Macaw Speech sheet. If another voice input app such as Google is installed, Android asks which one to use and can remember the choice. To hide the entry from other apps, turn off 'Use as the system voice input' in the recognition group of the settings.

#### How do I make 3-Macaw Speech the default digital assistant?

Open the settings of 3-Macaw Speech and tap Digital assistant in the recognition group: the system settings open, where you choose 3-Macaw Speech as the digital assistant app (the page may be named a little differently on some systems). Then long press the power or Home button, or use the assist gesture, to dictate. If the power button is set to another function on your phone, use the Home button or the assist gesture.

#### Why does the resident wake word keep the microphone indicator lit, and how much battery does it use?

Listening for a wake word all the time means recording all the time: from Android 12 on, the status bar shows the microphone indicator (the green dot) for as long as an app records, and no app can hide it; the low-power hotword channels that light no indicator are open only to preinstalled system assistants, not to third-party apps, even when one is the default digital assistant. So the resident wake word is off by default, and while it is on the green dot and a notification stay (the notification can turn it off). On a Sony XQ-DQ72 with the screen off for 30 minutes, Android estimated the plugin's use at about 17 mAh with it on (about 0.4% of the battery) and almost nothing with it off. Most of the estimate goes to the recording itself: Power saving mode, on by default, runs the wake word model only while the phone hears speech or another sound, which cuts the plugin's CPU time by half (with sounds all the time) to nine tenths (in quiet), but the estimate by only about 1 mAh. A very soft wake word may be missed in Power saving mode; then it can be turned off in the settings. Turn it on only when you need it: set the listening time to 10, 30 or 60 minutes, or turn on Listen only while charging (it pauses when unplugged and continues when plugged in again); the trigger entries such as the tile, the widget and the floating microphone record nothing in between.

#### Why does only a "Tap to speak" notification appear after the wake word?

From Android 10 on, an app in the background may open a screen directly only when it is the default digital assistant or may display over other apps. When 3-Macaw Speech is not the default digital assistant, turning the resident wake word on asks once for that permission; without it, a heard wake word shows a "Tap to speak" notification that opens the sheet and goes away after a minute. To let the wake word open the sheet directly, make 3-Macaw Speech the default digital assistant, or allow it to display over other apps in the system settings.

#### Why am I asked to turn 3-Stove Agent off before turning the resident wake word on?

The resident listeners of 3-Macaw Speech, 3-Stone AI and 3-Stove Agent exclude each other: only one app at a time may listen on a phone, or they would compete for the microphone. While one is on, the other's switch says which app listens and opens its settings so that you can turn it off there. After a restart or an update the resident wake word does not start recording by itself: while the switch is on, a "Tap to resume the resident wake word" notification appears, and a tap, or opening the home screen or the settings of 3-Macaw Speech, resumes it; if the listening time ran out meanwhile, the switch turns off instead.

#### How do I make the resident wake word respond only to my voice?

Open the settings of 3-Macaw Speech, tap "Speakers" in the Resident wake word group, download the speaker recognition model (about 26.5 MB) as prompted, then tap "Add a speaker", enter a name and read the 3 sentences shown, about 4 seconds each; family members can enroll too. Then turn on "Only enrolled voices": when it hears the wake word, the plugin compares the about 2 seconds of audio before it with the enrolled voiceprints and opens the sheet if anyone matches (a score of 0.50 or more); otherwise it acts as if it heard nothing (no sheet, no notification). "Try recognition" on the Speakers page shows your score. A quiet voice, a noisy room or another microphone (for example a headset) can be rejected; then add recordings in the same conditions. This keeps people nearby from waking it by accident; it is not a security check, and a recording of an enrolled person may also pass.

#### How does an AI assistant listen and speak through 3-Macaw Speech?

It needs AutoJs6 6.8.0 or later and the MCP Server plugin. 3-Macaw Speech is an official plugin, so `speech_listen`, `speech_speak` and `speech_transcribe` appear in the MCP Server tool list by default (they can be switched off under "Plugin tools" in the MCP Server settings); `speech_synthesize` writes files, so it also needs the second switch of the group, and without a given place its files go to plugin-outputs/speech in the AutoJs6 working directory. `speech_listen` hears one sentence at a time (at most 60 seconds): when it can record right away (for example with the resident wake word on, or when 3-Macaw Speech is the default digital assistant or may display over other apps) it starts at once, otherwise a "Tap to speak" notification appears on the phone and listening starts when it is tapped within 1 minute. While it listens, the resident listening of other apps steps aside and resumes afterwards; it returns BUSY while another dictation is running, and NOT_READY without the microphone permission or a recognition model (the plugin's home screen fixes both). `speech_speak` reads with a 3-Macaw Speech voice for the language when there is one, otherwise with the system voice.

#### How else can I open the dictation sheet without the power button?

In the settings of 3-Macaw Speech, the Triggers group sets them up: Quick Settings tile, tap to add it (Android 13 and later confirm through the system; older versions show the manual steps), then tap Voice command in Quick Settings; Home screen widget and shortcut, put the Tap to speak widget on the home screen, or long press the app icon and choose Voice command; Floating microphone, turn it on and grant the permission to display over other apps, and a draggable microphone button appears at the screen edge; Key trigger, turn on 3-Macaw Speech key trigger in the system accessibility settings; by default it is a hold of volume down for about 0.8 seconds, and it can be a hold of volume up or a double press of volume down instead. Every entry opens the same sheet as the digital assistant, where you choose the app that gets the text; while 3-Macaw Speech is the default digital assistant it opens through the assistant session, otherwise the same sheet opens directly.

#### Why does the key trigger need an accessibility service, and is it safe?

Android lets only accessibility services see hardware keys before the system handles them, so the key trigger is an accessibility service. It requests key filtering only and subscribes to no interface events, so it can neither read the screen content nor perform gestures; it takes only the chosen volume key, a short press still changes the volume, and keys are left alone while the screen is off or during a call. If its switch is grayed out (apps installed from a package file on Android 13 and later), first open the app info of 3-Macaw Speech and choose Allow restricted settings in its menu.

#### Can the voice button of a Bluetooth headset open the dictation sheet?

Yes. 3-Macaw Speech answers the voice command request (VOICE_COMMAND) of the Bluetooth service: a long press on the headset's voice key or touch area opens the dictation sheet. On Android 12 and later the sheet asks once for Nearby devices. Allowed, 3-Macaw Speech takes the headset's voice recognition over: it listens through the headset's microphone and every press works. Not allowed, it listens through the phone's microphone and some headsets (such as the Sony WF-1000XM4) open the sheet only on every other press. When another app answers voice commands too (the Google app, a vendor assistant), the system first asks which one to use; on HyperOS the chooser did nothing after 3-Macaw Speech was picked, a fault of the system's chooser. To stop it, turn off Headset voice key in the Triggers group of the settings.

#### What if a model download fails?

The plugin downloads from the GitHub releases of the AutoJs6 shared assets repository first and switches to the upstream sherpa-onnx address by itself when that fails; interrupted downloads resume. Without access to GitHub, download the model archive on another device (the mirror or upstream address of the model catalog) and use Import a local model on the home screen; files that match the catalog are registered as the catalog model.

#### How much space do the models take, and how do I free it?

The model table above lists the installed size of every model. The home screen deletes models you no longer need, and Recycle unreferenced files in the settings deletes download files that no model needs, import copies and interrupted installs, keeping installed models and paused downloads. Uninstalling the plugin deletes all models.

#### Why is there no punctuation in the result?

The streaming Zipformer models output words without punctuation. The SenseVoice model recognizes speech segment by segment, adds punctuation and reports the language, which suits complete sentences: name it with the `model` option in scripts or make it the default recognition model in the settings.

#### How do I write a wake word, and why does it not wake up?

Write the keyword as plain text: Chinese is spelled in pinyin by its common reading, English words by the model's dictionary (words missing from it are refused), and the model's pinyin tokens can be written directly to choose the reading of a character with several readings. Three to four syllables and an uncommon phrase wake up by mistake less often. Every keyword takes a `threshold` (lower wakes up more easily) and a `boost`. Try it in the wake word trial of the Try it out screen before writing it into a script.

#### Why is the APK conveyed under the GPL-3.0, and where is the source?

The plugin's own code is under MPL-2.0. The official sherpa-onnx Android library statically links eSpeak NG (GPL-3.0-or-later), so the APK as a whole is conveyed under the GPL-3.0-or-later. Every release attaches the corresponding source archive (the sherpa-onnx source and build scripts with all their dependencies), whose file name contains `corresponding-source` and whose SHA-256 is listed in `SHA256SUMS`; the plugin's source is this repository at the release tag.

******

### Permissions and security

******

The plugin keeps its permission surface as small as possible:

- Microphone: audio is recorded only for dictation, wake words, the tryout screen and while the resident wake word is on, and the microphone is released as soon as the session ends or the script exits.
- Network: used only to download models and refresh the signed model catalog; recognition and synthesis stay offline.
- Storage: models live in the private directory of the plugin; local models to import and files to recognize on Try it out are chosen through the system file picker, and you choose where a transcript is saved, so no storage permission is requested. The last transcript stays in the cache of the plugin until you clear it on the screen.
- Protected entries: the capability service and the activation entry require the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly; the system TTS engine and its voice check, sample text and settings entries are open to every app, as Android requires, and only synthesize speech.
- System voice input: like the system TTS engine it is open to every app; it records only while its sheet is on the screen, returns nothing but the recognized text and can be turned off in the settings.
- Digital assistant: it never reads the screen and does not listen in the background while the resident wake word is off; it records only while its sheet is on the screen, and the recognized text leaves the sheet only when you tap an app button or copy it (on the lock screen, after you unlock). The speech recognition service recognizes only for apps that hold the microphone permission themselves.
- Trigger entries: the tile, the widget, the shortcut, the floating microphone, the key trigger and the headset voice key record nothing themselves; they only open the dictation sheet. The key trigger is an accessibility service that only filters keys: it never reads the screen content, performs no gestures and takes only the chosen volume key (a short press still changes the volume, and keys are left alone while the screen is off or during a call). The floating microphone needs the permission to display over other apps and stays on screen with a quiet notification; starting at boot only brings a floating microphone that is on back after a restart. On Android 12 and later the headset voice key needs Nearby devices, asked only in a sheet the headset opened: it only lets the Bluetooth service hand the voice recognition to the sheet and connect the headset's microphone; no scanning, no location.
- Resident wake word: off by default and turned on only in the plugin's screens (after a restart or an update a tap on its notification resumes it; nothing records by itself). The wake word is detected on the device, no audio is kept or uploaded and the recognized text is never logged; while it is on it records all the time, the microphone indicator stays lit and its notification can turn it off. It excludes the resident listeners of 3-Stone AI and 3-Stove Agent: the system allows one at a time.
- Voiceprints: the plugin keeps no enrollment recording, only the voiceprint computed from it; voiceprints stay in the plugin's private folder, are never uploaded and cannot be exported or imported, and scripts and plugin tools get only names, scores and whether they match. The Speakers page deletes them one by one; clearing the data of 3-Macaw Speech or uninstalling it deletes all of them. The about 2 seconds before a wake word that "Only enrolled voices" compares are handled in memory only and never kept.
- Plugin tools: the tool service is protected by the AutoJs6 plugin permission as well, so only AutoJs6 can call it directly. `speech_listen` records one sentence per call, with a notification while it records (and the microphone indicator from Android 12 on); `speech_transcribe` reads only the one file AutoJs6 hands over, and `speech_synthesize` hands the audio back to AutoJs6, which writes it to the requested place. Neither recognized nor spoken text is written to logs. The three analysis tools also read only the one file AutoJs6 hands over, and `speech_diarize` with `identify` returns only names and scores, never voiceprints.

******

### Plugin interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate capabilities:

```text
application id: io.github.supermonster003.autojs6.plugin.three.macaw.speech
plugin id: three-macaw-speech
engine: speech
variant: default
info service: org.autojs.plugin.INFO (category three-macaw-speech)
capability service: org.autojs.plugin.SPEECH (category speech)
wake action: org.autojs.plugin.action.WAKE
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize, speaker_verify, speaker_identify, diarize, detect_language, tag; AutoJs6 6.8.0+)
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### Acknowledgements

******

3-Macaw Speech builds on the following open source projects, models and data. Thanks to their authors and maintainers:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) and the [k2-fsa (next-gen Kaldi)](https://github.com/k2-fsa) team: the runtime of recognition, wake words, voice activity detection and synthesis, the ONNX exports of the models and the Android library (Apache-2.0).
- Models trained with [icefall](https://github.com/k2-fsa/icefall): the Zipformer streaming recognizers (bilingual by pfluo, English by Zengwei, Chinese 2025 by yuekai, French by shaojieli, Korean by johnBamma), the open vocabulary keyword spotter (pkufool), and the AISHELL-3 VITS and Baker Matcha-TTS synthesis models of k2-fsa.
- [Vosk](https://huggingface.co/alphacep/vosk-model-small-streaming-ru) (Alpha Cephei): the small Russian streaming recognizer (Apache-2.0).
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, FunASR model license 1.1): the multilingual offline recognizer.
- Punctuation models: the CT-Transformer Chinese and English punctuation model of [FunASR](https://github.com/modelscope/FunASR) (Alibaba DAMO Academy) and the English punctuation and casing model of [Edge-Punct-Casing](https://github.com/frankyoujian/Edge-Punct-Casing) (frankyoujian), both Apache-2.0.
- The synthesis models [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, Chinese recordings granted by LongMaoData) and [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT), and the [HiFi-GAN](https://github.com/jik876/hifi-gan) vocoder (MIT). The French siwis, Spanish davefx, German thorsten and Russian denis voices come from [piper-voices](https://huggingface.co/rhasspy/piper-voices) (MIT), all fine-tuned from the English lessac voice.
- Training data: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), the [Data-Baker Chinese Standard Mandarin Speech Corpus](https://www.data-baker.com/open_source.html) (non-commercial use only), the Blizzard 2013 data of Lessac Technologies (research only) and [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (public domain). The French, Korean and Piper models added in 1.2.0 also use [Common Voice](https://commonvoice.mozilla.org/) 12 French (CC0), [KsponSpeech](https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=123) (AI Hub, applications from Korean nationals only), the [SIWIS French speech synthesis database](https://datashare.is.ed.ac.uk/handle/10283/2353) (CC BY 4.0, SIWIS database, University of Edinburgh and partners), [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) (CC0) and the davefx and denis recordings of [NabuCasa voice-datasets](https://github.com/NabuCasa/voice-datasets) (CC0).
- Models added in 1.3.0: the Spanish FastConformer of [NVIDIA NeMo](https://huggingface.co/nvidia/stt_es_fastconformer_hybrid_large_pc) (CC-BY-4.0; trained on Common Voice 12 (CC0), Multilingual LibriSpeech (CC BY 4.0), VoxPopuli (CC0) and Fisher Spanish (LDC)), the eight-language streaming model [PengChengStarling](https://github.com/yangb05/PengChengStarling) (Apache-2.0), [Kokoro v1.0](https://huggingface.co/hexgrad/Kokoro-82M) (hexgrad, Apache-2.0), the Arabic [dii](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_dii_espeak) and [miro](https://huggingface.co/OpenVoiceOS/phoonnx_ar-SA_miro_espeak_V2) voices of OpenVoiceOS (trained with [phoonnx](https://github.com/TigreGotico/phoonnx) on synthetic speech data of TigreGotico) and the Korean KSS voice of Mycroft [Mimic 3](https://github.com/MycroftAI/mimic3-voices) (CC0; trained on the [KSS dataset](https://www.kaggle.com/datasets/bryanpark/korean-single-speaker-speech-dataset), CC BY-NC-SA 4.0).
- The [ONNX Runtime](https://github.com/microsoft/onnxruntime) inference engine (MIT), the [silero VAD](https://github.com/snakers4/silero-vad) voice activity model (MIT), [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) and [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) for phonemes, and [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) for the readings of Chinese wake words.

******

### Roadmap

******

Plans, design decisions and progress are tracked as a checklist in ROADMAP.md. Unchecked items are plans, not features of the current version.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Release history

******

#### v1.4.0

_2026/10/30_

- `Improvement` Language identification now goes sentence by sentence: when a recording mixes languages, you can see which language each sentence is in (very short sentences join a neighbor). Try it out lists every sentence, and the segments of speech.detectLanguage in scripts and of the speech_detect_language tool are sentences too; the overall language is the one spoken longest and the candidates' scores are shares of the speaking time. The windows option stays as it was. Each sentence takes its own pass, so this is somewhat slower than in 1.3.0

#### v1.3.0

_2026/10/30_

- `Hint` speech.speaker, speech.diarize, speech.detectLanguage and speech.tag in scripts and the five speaker and analysis tools need the AutoJs6 6.8.0 release or later; the Speakers page, "Only enrolled voices" and the file analyses of Try it out are used in the plugin itself and do not depend on the AutoJs6 version. With an older AutoJs6, speech.models.list() in scripts also lists the speaker recognition, speaker segmentation, language identification and audio tagging models (kinds speaker, segment, langid and tagger), which can be installed or deleted as usual
- `Hint` Analyzing long audio takes a lot of memory: finding the speakers of a 10 minute recording peaked at about 470 MB to 1.3 GB in tests, so the plugin asks Android for a larger memory limit; on phones with little memory, try shorter files first
- `Feature` Power saving mode for the resident wake word (Settings > Resident wake word, on by default): the wake word model runs only while the phone hears speech or another sound, not in a quiet room, so the plugin takes half to nine tenths less CPU time (the quieter, the less). The recording itself still costs the same, so Android's battery estimate drops only a little (see the README for the measurement). A very soft wake word may be missed; then turn power saving off
- `Feature` More models to download: offline Spanish recognition (NeMo, with punctuation); streaming recognition of eight languages, Arabic, Indonesian, Thai, Vietnamese, Japanese, Russian, Chinese and English, that detects the language by itself (PengChengStarling, about 339 MB once installed); the Kokoro v1.0 multilingual voices (49 speakers of English, Spanish, French, Hindi, Italian, Brazilian Portuguese and Chinese, each speaking its own language); two Arabic voices and one Korean voice. The Arabic and Korean voices are for non-commercial use only, and the training data of the eight-language model is not disclosed, so its commercial use is unknown; the license is shown before each download. The new voices also appear by language in the system TTS engine, and the system voice input and the plugin tools pick the eight-language model automatically only for languages that no other installed model covers
- `Feature` Speaker recognition and "Only enrolled voices" (Settings > Resident wake word): on the Speakers page, enter a name and read 3 sentences aloud to enroll your voice (several people can enroll, and Try recognition shows the score); with "Only enrolled voices" on, the resident wake word compares the voice first and opens the sheet only when it matches an enrolled person, so other people saying the wake word are ignored. Manual entries such as the tile, the widget and the headset key are not affected. The speaker recognition model (about 26.5 MB) is downloaded first, and the page shows how; voiceprints stay on the device and are never uploaded or exported
- `Feature` Scripts can enroll, verify and identify speakers with speech.speaker, and list or remove enrolled people; two new plugin tools, speech_speaker_verify (whether a recording is a given enrolled person) and speech_speaker_identify (the most similar enrolled person), return only names and scores, never voiceprints
- `Feature` File recognition on Try it out gains three analyses: Find speakers (who spoke when, with enrolled voices named; "Speakers and text" also transcribes what each person says), Identify language (the 99 languages of Whisper) and Tag sounds (which of 527 kinds of sounds, such as speech, music or a barking dog, the file holds). They run offline on the phone, keep going after you leave the screen and show their progress in a notification. The models are downloaded first, and the screen shows how: the speaker segmentation model (about 6 MB, plus the speaker recognition model), the language identification model (about 103 MB) and the audio tagging model (about 10.5 MB). The training data of the speaker segmentation model includes research-only material, so it is marked as non-commercial; the training data of the language identification models is not published, so their commercial use is unknown
- `Feature` Scripts can run the same analyses with speech.diarize, speech.detectLanguage and speech.tag (and their ...Async versions that return a Promise), with progress reports; three new plugin tools, speech_diarize, speech_detect_language and speech_tag, do so too. A file can be up to 30 minutes long (10 minutes for speech_diarize); one analysis runs at a time and the others wait their turn
- `Fix` Model downloads work again on Android 7.0: the model catalog and the models are fetched from GitHub, whose certificates are now issued under a root certificate (ISRG Root X1 of Let's Encrypt) that Android 7.0 does not have, so every download and every catalog refresh failed with a certificate error. The plugin now carries that root certificate itself and uses it next to the ones of the system; Android 7.1 and later already have it and were not affected
- `Fix` On some phones, model and file sizes showed a wide gap between the number and the unit (when the screen language was one without a region, such as English); they now show normally

#### v1.2.0

_2026/10/23_

- `Hint` New resident wake word, off by default: while on, it keeps listening for the wake word on the device, the microphone indicator stays lit, a notification stays and the battery drains faster (see the README for a 30 minute screen-off measurement). Using it with "Listening time" or "Listen only while charging" is recommended
- `Hint` Recognizing non-WAV audio in scripts and the four plugin tools need the AutoJs6 6.8.0 release or later; with an older AutoJs6, scripts still recognize WAV only. The resident wake word, the new models, automatic punctuation and the "File" tab are used in the plugin itself and do not depend on the AutoJs6 version
- `Hint` New permissions: wake lock (only while Try it out recognizes a file, for at most 60 minutes, so that recognition goes on with the screen off) and a media processing foreground service (Android 15 and later, while a file is recognized, showing the progress in a notification)
- `Feature` Resident wake word (Settings > Resident wake word): saying a wake word ("小鹦小鹦" and "Hey Macaw" by default; editable, with high, medium and low sensitivity) opens the same dictation sheet as the digital assistant; with the screen off or locked it turns the screen on, and you unlock only before sending or copying
- `Feature` A sheet opened by the wake word reads the recognized text aloud, with a 3-Macaw Speech voice of that language if there is one, else with the system voice; this can be turned off in the settings
- `Feature` Listening time (turn off after 10, 30 or 60 minutes, or always on) and Listen only while charging (pauses when unplugged and continues when plugged in again), the same options as in 3-Stove Agent
- `Feature` When 3-Macaw Speech is not the default digital assistant, turning the resident wake word on asks once for the permission to display over other apps; without it, a heard wake word shows a "Tap to speak" notification that opens the sheet
- `Feature` When a script, another app or another entry of this plugin uses the microphone, the resident wake word steps aside and listens again once they finish
- `Feature` Only one resident listener can run at a time among 3-Stone AI, 3-Stove Agent and 3-Macaw Speech; while another one is on, you are asked to turn it off first and can open its settings directly
- `Feature` No recording starts by itself after a restart or an update: while the switch is on, a "Tap to resume the resident wake word" notification appears, and opening the home screen or the settings resumes it as well
- `Feature` Recognizing audio files (speech.recognize, speech.detectVoice) accepts common formats besides WAV, such as MP3, AAC (including M4A), Ogg (Vorbis, Opus), FLAC and AMR, as well as the sound of video files (MP4, WebM and others); the limit stays at 200 MB or 30 minutes (needs AutoJs6 6.8.0 or later; earlier versions accept WAV only)
- `Feature` Four plugin tools (AutoJs6 6.8.0 or later): speech_listen hears one sentence and returns the text, speech_speak reads aloud, speech_transcribe transcribes an audio file on the phone, speech_synthesize synthesizes text to a WAV or M4A file. The MCP Server plugin uses them so that an AI assistant on a computer can listen and speak through the phone, 3-Stove Agent calls them directly on the phone, and scripts can call them through plugins.tools; when the plugin cannot record right away, listening first shows a "Tap to speak" notification on the phone and starts once it is tapped
- `Feature` More models to download: streaming recognition for French, Russian and Korean, and Piper voices for French, Spanish, German and Russian; the Piper voices and the Korean model are for non-commercial use only and show their license before the download, and the Korean model is downloaded only from the upstream address
- `Feature` Automatic punctuation: after you download a punctuation model from the new "Punctuation" group on the home screen, Chinese and English text recognized by streaming models gets commas, periods and question marks, and English gets its capital letters back; SenseVoice adds its own punctuation and is not changed; scripts can still pass punctuation: false to remove all punctuation
- `Feature` A new "File" tab on Try it out: choose an audio or video file (MP3, M4A, WAV and more, up to 200 MB and 30 minutes) and get what is said in it as text without writing a script, in timed segments that you can copy or save as plain text or SRT subtitles; the model, the language and automatic punctuation are chosen on the tab; recognition goes on when you leave the screen or turn it off, and a notification shows the progress with a cancel button; the last result is still there the next time you open the tab

##### For more release history, refer to

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build

******

This section targets developers who want to build the plugin from source.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
py -I .python\corresponding_source.py build --downloads <directory outside the repository>
```

******

### License

******

The plugin's source code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). The official sherpa-onnx Android library (Apache-2.0) statically links eSpeak NG and UCD Tools, which are licensed under the GNU GPL version 3 or later, so the released APK as a whole is conveyed under the GPL-3.0-or-later (MPL-2.0 section 3.3 allows this combination), without any warranty. Every release attaches the corresponding source `autojs6-plugin-three-macaw-speech-corresponding-source-sherpa-onnx-v1.13.8-ac68c80b.tar`: the source and build scripts of sherpa-onnx 1.13.8 and every dependency archive that its build pins by URL and SHA-256; its SHA-256 is listed in the `SHA256SUMS` of the release, and `.python/corresponding_source.py` rebuilds and checks it. ONNX Runtime and silero VAD are under the MIT license; the license of each model is in the model table above. The About page of the plugin shows the full license texts and the third-party notices.

- [Third-party notices (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### Related links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: the speech module: https://docs.autojs6.com/#/speech
- sherpa-onnx project: https://github.com/k2-fsa/sherpa-onnx
