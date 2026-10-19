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

- Speech recognition: streaming dictation from the microphone (partial and final results in real time) and WAV file recognition, with offline models for Chinese, English, mixed Chinese and English, Japanese, Korean and Cantonese.
- Voice activity detection: the built-in silero VAD v5 splits audio into speech segments on its own and also ends dictation sentences automatically.
- Wake words: type the keyword as plain text (for example `小六小六` or `hey macaw`) without training a model; after waking up, dictation can start automatically.
- Text-to-speech: several neural Chinese and English voices, synthesized to WAV files or used as the system TTS engine by the AutoJs6 `tts` module and other apps.
- System voice input: microphone buttons in other apps, such as 3-Stove Agent, can use the offline recognition; you say one sentence in a bottom sheet and the recognized text goes back to that app.
- Digital assistant: choose 3-Macaw Speech as the default digital assistant app, then long press the power or Home button, say one sentence, edit the recognized text if needed and hand it to 3-Stove Agent, 3-Stone AI or another app that takes voice commands, or copy it. It also provides a system speech recognition service: on Android 11 and lower, choosing it as the assistant usually makes it the default recognition service too; from Android 12 on, choose it separately in the system's voice input setting (some phones lack this setting), and apps can also name it directly.
- Model management: downloads from a signed model catalog, enabled only after every file passes its SHA-256 check, plus import of local models; models restricted to non-commercial use show their license before the download.
- Standalone screens: the home screen shows the status and manages the models, Try it out tests dictation, wake words and synthesis directly, and the settings change the language, night mode, theme color, launcher icon and the recognition and synthesis defaults; the appearance follows AutoJs6 by default.

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

Recognition, wake word and synthesis models take 8 MB to 390 MB each, so they are not bundled with the APK. The plugin carries a signed model catalog and downloads the models from immutable releases of the AutoJs6 shared assets repository, falling back to the upstream sherpa-onnx address when the mirror is unavailable; on both paths every file must pass its SHA-256 check before the model is enabled. The 1.0.0 catalog contains these models (sizes are the space they take once installed):

| Model ID | Purpose | Languages | Size | License | Commercial use |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (recommended) | Recognition (offline, with punctuation) | zh, en, ja, ko, yue | 228.5 MB | FunASR Model License 1.1 | Allowed |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (recommended) | Recognition (streaming) | zh, en | 189.8 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.en.2023-06-26.int8` (recommended) | Recognition (streaming) | en | 67.1 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | Recognition (streaming) | zh | 159.6 MB | None declared | Non-commercial only |
| `kws.zipformer.zh-en.2025-12-20` (recommended) | Wake word | zh, en | 8.4 MB | Apache-2.0 | Allowed |
| `tts.vits.melo.zh-en` (recommended) | Synthesis (speakers: 1, 44.1 kHz) | zh, en | 169.2 MB | MIT | Allowed |
| `tts.vits.icefall.zh-aishell3` (recommended) | Synthesis (speakers: 174, 8 kHz) | zh | 31.2 MB | None declared | Unknown |
| `tts.kokoro.multi-lang.v1_1` | Synthesis (speakers: 103, 24 kHz) | zh, en | 386.9 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Allowed |
| `tts.matcha.icefall.zh-baker` | Synthesis (speakers: 1, 22.05 kHz) | zh | 77.2 MB | None declared, MIT (vocoder) | Non-commercial only |
| `tts.vits.piper.en-us-lessac-medium` | Synthesis (speakers: 1, 22.05 kHz) | en | 77.4 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |

Models restricted to non-commercial use or with an unknown license show their license before the download, and scripts must pass `acceptLicense: true` explicitly. The weights of the 2025 Chinese streaming model declare no license and its training data includes corpora for non-commercial use only, so it is downloaded only from the upstream sherpa-onnx address and not mirrored in the shared assets repository. The Kokoro and Piper synthesis models include the pronunciation data of eSpeak NG (espeak-ng-data, GPL-3.0-or-later).

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

- `speech.listen(options)` opens a dictation session that reports through the events `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` and `end`; `stop()` ends it early. `speech.recognize(path)` recognizes a WAV file (16 kHz PCM is accepted too) and `speech.detectVoice(path)` lists its speech segments.
- `speech.wake(keywords, options)` opens a wake word session with the keywords written as plain text (at most 50); with `thenListen` every wake up turns into one round of dictation on the same recording, whose session is the `listen` property of the `wake` event.
- `speech.synthesize(text, path, options)` synthesizes to a WAV file (at most 8000 characters, with an optional model, speaker and speed); `speech.speak(text, options)` and `tts(text, { engine })` speak through the system TTS engine.
- `speech.models` lists, downloads (`install`, with progress), imports and removes models and sets the default models; `speech.capabilities()` answers the plugin version, its features and whether it holds the microphone permission.
- The whole plugin has one microphone session at a time (dictation or wake word); without the microphone permission the calls fail with `PERMISSION_DENIED`, which the plugin's home screen grants. Recognized text is never written to the AutoJs6 log.
- The [AutoJs6 documentation](https://docs.autojs6.com/#/speech) describes every method, option, event and error code; the AutoJs6 Samples > 3-Macaw Speech hold four examples: dictation to a file, wake word and dictation, TTS announcements and voice commands handed to the Agent.

******

### Compatibility

******

- Android 7.0 (API 24) or later, target API 37. Packages for arm64-v8a, armeabi-v7a, x86_64 and x86 plus a universal package; the native libraries are aligned to 16 KB memory pages and were verified on 16 KB page emulators.
- AutoJs6: the `speech` module of scripts needs 6.8.0 (internal build 5456) or later; the plugin screens, system TTS engine, system voice input, speech recognition service and digital assistant do not depend on AutoJs6.
- Background listening: a dictation or wake word session started while AutoJs6 is in the foreground keeps listening after AutoJs6 moves to the background or the screen turns off (the plugin keeps recording with a microphone foreground service and its notification). A session opened while AutoJs6 is already in the background may be silenced by the system: it then gets a `warning` event with the code `PERMISSION_DENIED` but does not end, and the audio comes back once AutoJs6 returns to the foreground; scripts that need to listen in the background should open the session while AutoJs6 is visible.
- Tested devices: Sony G8441 (Android 9), Sony XQ-AT72 (Android 12), Sony XQ-DQ72 (Android 13), Redmi 22120RN86C (Android 13), Xiaomi Pad 23046RP50C (Android 15), OnePlus OPD2413 (ColorOS, Android 15), and emulators with API 24, 35 and 37 (16 KB pages).
- Performance: on the low-end Sony G8441 a streaming model loads in about 1.7 seconds and SenseVoice in about 2.8 seconds, and dictation is processed about 3 times faster than real time; MeloTTS and Kokoro synthesize slower than real time on such devices, AISHELL-3 is the fastest. Newer devices such as the Sony XQ-DQ72 load in about 0.5 to 1.2 seconds and synthesize faster than real time.
- Memory: a loaded recognition model takes about 210 to 300 MB of native memory. The plugin keeps the loaded models within a quarter of the device memory (384 MB to 1.5 GB) and releases them after 5 idle minutes (1 to 30 minutes in the settings).
- Digital assistant: the system must let you choose a third-party digital assistant app. Some systems reserve the long press of the power button for their own assistant; use the Home button or the assist gesture there. ColorOS and HyperOS have not been verified yet.

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

#### Why does the assistant not listen for a wake word all the time?

Listening all the time keeps the microphone, an ongoing notification and the microphone indicator on and uses more battery, so the digital assistant listens only when you press the assist button. Apps such as 3-Stone AI and 3-Stove Agent can offer their own always-on listening switch (off by default); while one listens, the digital assistant, voice input and speech recognition still work, and wake word detection pauses and resumes by itself afterwards. A built-in wake word of 3-Macaw Speech itself is planned for a later version.

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

- Microphone: audio is recorded only for dictation, wake words and the tryout screen, and the microphone is released as soon as the session ends or the script exits.
- Network: used only to download models and refresh the signed model catalog; recognition and synthesis stay offline.
- Storage: models live in the private directory of the plugin and local models are imported through the system file picker, so no storage permission is requested.
- Protected entries: the capability service and the activation entry require the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly; the system TTS engine and its voice check, sample text and settings entries are open to every app, as Android requires, and only synthesize speech.
- System voice input: like the system TTS engine it is open to every app; it records only while its sheet is on the screen, returns nothing but the recognized text and can be turned off in the settings.
- Digital assistant: it never reads the screen and does not listen in the background; it records only while its sheet is on the screen, and the recognized text leaves the sheet only when you tap an app button or copy it (on the lock screen, after you unlock). The speech recognition service recognizes only for apps that hold the microphone permission themselves.

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
minimum host build: 5456
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### Acknowledgements

******

3-Macaw Speech builds on the following open source projects, models and data. Thanks to their authors and maintainers:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) and the [k2-fsa (next-gen Kaldi)](https://github.com/k2-fsa) team: the runtime of recognition, wake words, voice activity detection and synthesis, the ONNX exports of the models and the Android library (Apache-2.0).
- Models trained with [icefall](https://github.com/k2-fsa/icefall): the Zipformer streaming recognizers (bilingual by pfluo, English by Zengwei, Chinese 2025 by yuekai), the open vocabulary keyword spotter (pkufool), and the AISHELL-3 VITS and Baker Matcha-TTS synthesis models of k2-fsa.
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice) (FunAudioLLM / Alibaba, FunASR model license 1.1): the multilingual offline recognizer.
- The synthesis models [MeloTTS](https://github.com/myshell-ai/MeloTTS) (MyShell.ai, MIT), [Kokoro v1.1](https://huggingface.co/hexgrad/Kokoro-82M-v1.1-zh) (hexgrad, Apache-2.0, Chinese recordings granted by LongMaoData) and [Piper](https://github.com/rhasspy/piper) (Michael Hansen / rhasspy, MIT), and the [HiFi-GAN](https://github.com/jik876/hifi-gan) vocoder (MIT).
- Training data: [LibriSpeech](https://www.openslr.org/12/) (CC BY 4.0), [AISHELL-3](https://www.openslr.org/93/) (Apache-2.0), the [Data-Baker Chinese Standard Mandarin Speech Corpus](https://www.data-baker.com/open_source.html) (non-commercial use only), the Blizzard 2013 data of Lessac Technologies (research only) and [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) (public domain).
- The [ONNX Runtime](https://github.com/microsoft/onnxruntime) inference engine (MIT), the [silero VAD](https://github.com/snakers4/silero-vad) voice activity model (MIT), [eSpeak NG](https://github.com/espeak-ng/espeak-ng) (GPL-3.0-or-later) and [piper-phonemize](https://github.com/rhasspy/piper-phonemize) (MIT) for phonemes, and [pinyin-data](https://github.com/mozillazg/pinyin-data) (mozillazg, MIT) for the readings of Chinese wake words.

******

### Roadmap

******

Plans, design decisions and progress are tracked as a checklist in ROADMAP.md. Unchecked items are plans, not features of the current version.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Release history

******

#### v1.0.0

_2026/10/20_

- `Hint` First release. The speech module in scripts needs AutoJs6 6.8.0 (5456) or later; the plugin screens, system TTS engine, system voice input, speech recognition service and digital assistant do not depend on AutoJs6
- `Hint` Recognition and synthesis models are not part of the package and are downloaded from the plugin screens or a script; the app as a whole is conveyed under the GNU GPL 3.0 or later (the speech runtime statically links eSpeak NG), and every release comes with the corresponding source
- `Feature` Speech plugin of AutoJs6: once enabled in the Plugin Center, scripts use speech recognition, wake words, voice activity detection, speech synthesis and model management through the speech module
- `Feature` Built-in sherpa-onnx 1.13.8 speech runtime and silero VAD v5 voice activity detection model, with one package per ABI for 4 ABIs
- `Feature` Model management: downloads the offline speech models of the signed model catalog from the AutoJs6 shared assets releases, falling back to the official sherpa-onnx releases; every file is checked by SHA-256 before use, with resumable downloads, local import (tar, tar.bz2, zip), removal and default models; models limited to non-commercial use or without a declared license need their license accepted first
- `Feature` Speech recognition of WAV files (8, 16, 24 or 32-bit integer or float, any sample rate and channel count, resampled to 16 kHz mono in the plugin) with timed segments, plus language, emotion and event tags for SenseVoice models; streaming models accept hotwords
- `Feature` Dictation sessions on the microphone or on fed 16 kHz PCM report ready, partial and final results, endpoints, voice activity and the end; the silence that ends a sentence, the no-speech timeout and the longest duration are adjustable; streaming models update partial results word by word, offline models segment by segment
- `Feature` Voice activity detection lists the speech segments of a WAV file with the bundled silero VAD
- `Feature` Wake words: open-vocabulary keyword spotting; Chinese keywords are spelled in the pinyin of their customary reading, English words in the phones of the model's lexicon, and model tokens may be written directly; a hit can start a dictation round on the same recording
- `Feature` During a microphone session the plugin keeps recording with a microphone foreground service and its notification, so a session started while AutoJs6 is in the foreground goes on after AutoJs6 moves to the background or the screen turns off; one microphone session runs at a time; without the microphone permission sessions fail with PERMISSION_DENIED and name the permission screen
- `Feature` Speech synthesis: the installed synthesis models (VITS, Matcha and Kokoro; the catalog has 5 Chinese and English models) turn up to 8000 characters of text into a WAV file sentence by sentence, at the model's sample rate; a speaker and a speed (0.5 to 2 times) can be chosen, progress counts the characters done, and the synthesis can be cancelled at any time; a model with a single speaker ignores a chosen speaker and says so
- `Feature` System TTS engine: once a synthesis model is installed, 3-Macaw Speech can be chosen in the text-to-speech settings of the system, and AutoJs6's tts module and other apps read aloud with it; every speaker is one voice, bilingual models add an English voice for every speaker, and the speech rate follows the system or app setting; without an installed model the engine reports missing voice data
- `Feature` Home screen: shows the speech runtime version, the number of installed models, the microphone permission (with a button to grant it) and whether 3-Macaw Speech is the preferred system TTS engine
- `Feature` Models on the home screen, grouped into recognition, wake words, synthesis and voice activity detection, with size, languages and license: download, pause, resume, cancel, delete and set as default; download progress also shows in a notification; models limited to non-commercial use or without a declared license ask you to accept their license first, and a download on mobile data or another metered network asks before it starts
- `Feature` Import a local model from a tar, tar.bz2 or zip archive or from a folder: files that match a catalog model are registered as that model, other files become your own model after you fill in its kind, model type, name, languages and ID
- `Feature` Try it out: live dictation (partial results in gray) with a choice of model and language, a wake word trial that lists the hits with their keyword and score, and speech synthesis of your text with a choice of model, speaker and speed, played back or saved as a WAV file; leaving the screen releases the microphone
- `Feature` System voice input: microphone buttons in other apps, such as 3-Stove Agent, can use the offline recognition of 3-Macaw Speech: say one sentence in a bottom sheet and the recognized text goes straight back to that app; it ends after a pause, or stop or cancel it earlier; it follows the endpoint sensitivity and voice activity detection of the settings and can be turned off in their recognition group
- `Feature` Digital assistant: after choosing 3-Macaw Speech as the default digital assistant app in the system settings, long press the power or Home button (or use the assist gesture) and say one sentence in a bottom sheet; the recognized text can be edited, then a button hands it to an app that takes voice commands, such as 3-Stove Agent or 3-Stone AI, or you copy it or speak again; nothing is sent before you tap a button. It also works on the lock screen and unlocks before sending or copying. It never reads the screen and does not listen in the background. The recognition group of the settings shows whether it is the default assistant and opens the system settings to choose it
- `Feature` System speech recognition service: apps that use the Android speech recognizer (SpeechRecognizer) can name 3-Macaw Speech directly and get its offline recognition, with partial results; once it is the default recognition service, apps that name none use it too. On Android 11 and lower, choosing it as the digital assistant usually makes it the default as well; from Android 12 on, choose it separately in the system's voice input setting (some phones lack this setting). When another app keeps a wake word listening through 3-Macaw Speech (3-Stone AI, for one), the digital assistant, the system voice input, the speech recognition service and the tryout screen still get the microphone: wake word detection pauses meanwhile and resumes by itself afterwards
- `Feature` Settings: language, night mode, theme color and launcher icon (following AutoJs6 by default); the default recognition and synthesis models, endpoint sensitivity, voice activity detection, speaker, speed and a default voice per language for the tryout screen and the system TTS engine (scripts keep the documented defaults); when idle models are released, catalog refresh and cleanup of leftover files; the settings button of 3-Macaw Speech in the system text-to-speech settings opens this screen
- `Feature` About: version, the release history included with the plugin, update checks (automatically at most every 12 hours, a release can be ignored) and the licenses and notices: the app as a whole is conveyed under the GNU GPL 3.0 or later without any warranty, with the third-party notices, the full GPL text, the Unicode Terms of Use and where the corresponding source is
- `Feature` Launcher icon: adaptive icon (light), adaptive icon (dark), adaptive icon (automatic, the default) or transparent background icon
- `Feature` Screens, README, Plugin Center instructions and changelog in 10 languages: Simplified Chinese, Traditional Chinese (Hong Kong, Taiwan), English, French, Spanish, Japanese, Korean, Russian and Arabic
- `Dependency` Attach sherpa-onnx 1.13.8
- `Dependency` Attach Jetpack Compose BOM 2026.09.00
- `Dependency` Attach AndroidX Activity Compose 1.12.2
- `Dependency` Attach AndroidX Core KTX 1.15.0
- `Dependency` Attach Kotlin Coroutines 1.11.0
- `Dependency` Attach Material Color Utilities (MaterialKolor) 4.1.1

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
