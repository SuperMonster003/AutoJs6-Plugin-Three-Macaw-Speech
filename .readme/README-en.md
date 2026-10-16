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

The plugin is a separately installed APK. AutoJs6 scripts use it through the global `speech` module; the speech synthesis also registers as an Android system TTS engine, so the AutoJs6 `tts` module and other apps can choose it. Recognition and synthesis models are not bundled with the APK but downloaded on demand from the plugin screen or a script; only the voice activity detection model is built in.

> This is a development preview: AutoJs6 can discover the plugin, which downloads and manages models, recognizes speech, detects voice activity and wake words, and synthesizes speech, also as the system TTS engine, but the plugin screens are not delivered yet, and the AutoJs6 release with the `speech` script module is not published yet. The text below describes the design goals of 1.0.0; see the roadmap for progress.

******

### Features

******

- Speech recognition: streaming dictation from the microphone (partial and final results in real time) and WAV file recognition, with offline models for Chinese, English, mixed Chinese and English, Japanese, Korean and Cantonese.
- Voice activity detection: the built-in silero VAD v5 splits audio into speech segments on its own and also ends dictation sentences automatically.
- Wake words: type the keyword as plain text (for example `小六小六` or `hey macaw`) without training a model; after waking up, dictation can start automatically.
- Text-to-speech: several neural Chinese and English voices, synthesized to WAV files or used as the system TTS engine by the AutoJs6 `tts` module and other apps.
- Model management: downloads from a signed model catalog, enabled only after every file passes its SHA-256 check, plus import of local models; models restricted to non-commercial use show their license before the download.

******

### Usage

******

1. Update AutoJs6 to internal build 5451 (6.8.0) or later.
2. Download the APK that matches the device ABI from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) (choose `universal` if unsure) and install it, then authorize and enable it in the AutoJs6 Plugin Center.
3. Open the plugin screen to grant the microphone permission and download the models you need, then use the `speech` module in scripts.

> The plugin supports devices running Android 7.0 (API 24) or later. There is one package per ABI and a `universal` package for every device.

******

### Models

******

Recognition, wake word and synthesis models take 30 MB to 350 MB each, so they are not bundled with the APK. The plugin downloads them from immutable releases of the AutoJs6 shared assets repository and falls back to the upstream sherpa-onnx address when the mirror is unavailable; on both paths every file must pass its SHA-256 check before the model is enabled.

Every model carries its license. Models restricted to non-commercial use (for example a synthesis model trained on the DataBaker corpus) show a notice before the download, and scripts must pass `acceptLicense: true` explicitly.

******

### Script API

******

Scripts use these capabilities through the global `speech` module (also available as `$speech`; this is the 1.0.0 design draft, the documentation of the released version prevails):

```javascript
let session = speech.listen({ language: "zh", silenceEndMillis: 1500 });
session.on("partial", (e) => console.log("...", e.text));
session.on("final", (e) => files.append("./dictation.txt", e.text + "\n"));
sleep(30000);
session.stop();

let wake = speech.wake(["hey macaw"], { thenListen: true });
wake.on("wake", (e) => e.listen.on("final", (r) => console.log(r.text)));

speech.synthesize("Task completed", "./done.wav");
tts("Task completed", { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" });
```

- `speech.listen(options)` opens a dictation session that reports results through events such as `partial`, `final` and `end`; `speech.recognize(path)` recognizes a WAV file.
- `speech.wake(keywords, options)` opens a wake word session; `thenListen` starts dictation automatically after a wake up.
- `speech.synthesize(text, path)` synthesizes to a WAV file; `speech.speak(text)` and `tts(text, {engine})` speak through the system TTS engine; `speech.models` manages the models.

******

### FAQ

******

#### Do recognition and synthesis need the Internet?

No. Once the models are downloaded, everything runs on the device and no audio is uploaded. The plugin only goes online to download models and refresh the model catalog.

#### Why does the plugin request the microphone permission itself?

Audio is recorded inside the plugin process and never passes through AutoJs6. AutoJs6 hands its foreground capabilities to the plugin when it binds, and the plugin holds a microphone foreground service during a microphone session, so dictation goes on after AutoJs6 moves to the background or the screen turns off. Grant the microphone permission on the plugin screen before the first use.

#### Which package should I install?

Most phones need `arm64-v8a`. If you are unsure about the device ABI, install `universal`: it works on every device but is larger.

#### How do I use 3-Macaw Speech as the text-to-speech engine of the system?

Install at least one synthesis model first, then choose 3-Macaw Speech as the preferred engine in the text-to-speech settings of the system (usually under Accessibility or Languages). Every speaker of a model is a voice named `<model id>/<speaker number>`; bilingual models also offer `<model id>/<speaker number>/en-US` for English. AutoJs6 scripts can pick the engine without changing the system setting: `tts(text, {engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech"})` or `speech.speak(text)`.

******

### Permissions and security

******

The plugin keeps its permission surface as small as possible:

- Microphone: audio is recorded only for dictation, wake words and the tryout screen, and the microphone is released as soon as the session ends or the script exits.
- Network: used only to download models and refresh the signed model catalog; recognition and synthesis stay offline.
- Storage: models live in the private directory of the plugin and local models are imported through the system file picker, so no storage permission is requested.
- Protected entries: the capability service and the activation entry require the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly; the system TTS engine and its voice check, sample text and settings entries are open to every app, as Android requires, and only synthesize speech.

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
minimum host build: 5451
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### Roadmap

******

Plans, design decisions and progress are tracked as a checklist in ROADMAP.md. Unchecked items are plans, not features of the current version.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### Release history

******

#### v1.0.0

_2026/10/16_

- `Hint` Development preview: AutoJs6 can discover the plugin, which downloads and manages models, recognizes speech in WAV files, fed PCM and from the microphone, detects voice activity and wake words, and synthesizes speech, also as the system TTS engine; the plugin screens and the speech script module are not delivered yet
- `Hint` Requires AutoJs6 6.8.0 (5451) or later (the exact minimum build will be filled in once the host changes land)
- `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and speech capability service
- `Feature` Built-in sherpa-onnx 1.13.8 speech runtime and silero VAD v5 voice activity detection model, with one package per ABI for 4 ABIs
- `Feature` Model management: downloads the offline speech models of the signed model catalog from the AutoJs6 shared assets releases, falling back to the official sherpa-onnx releases; every file is checked by SHA-256 before use, with resumable downloads, local import (tar, tar.bz2, zip), removal and default models; models limited to non-commercial use or without a declared license need their license accepted first
- `Feature` Speech recognition of WAV files (8, 16, 24 or 32-bit integer or float, any sample rate and channel count, resampled to 16 kHz mono in the plugin) with timed segments, plus language, emotion and event tags for SenseVoice models; streaming models accept hotwords
- `Feature` Dictation sessions on the microphone or on fed 16 kHz PCM report ready, partial and final results, endpoints, voice activity and the end; the silence that ends a sentence, the no-speech timeout and the longest duration are adjustable; streaming models update partial results word by word, offline models segment by segment
- `Feature` Voice activity detection lists the speech segments of a WAV file with the bundled silero VAD
- `Feature` Wake words: open-vocabulary keyword spotting; Chinese keywords are spelled in the pinyin of their customary reading, English words in the phones of the model's lexicon, and model tokens may be written directly; a hit can start a dictation round on the same recording
- `Feature` During a microphone session the plugin keeps recording with a microphone foreground service and its notification, so a session started while AutoJs6 is in the foreground goes on after AutoJs6 moves to the background or the screen turns off; one microphone session runs at a time; without the microphone permission sessions fail with PERMISSION_DENIED and name the permission screen
- `Feature` Speech synthesis: the installed synthesis models (VITS, Matcha and Kokoro; the catalog has 5 Chinese and English models) turn up to 8000 characters of text into a WAV file sentence by sentence, at the model's sample rate; a speaker and a speed (0.5 to 2 times) can be chosen, progress counts the characters done, and the synthesis can be cancelled at any time; a model with a single speaker ignores a chosen speaker and says so
- `Feature` System TTS engine: once a synthesis model is installed, 3-Macaw Speech can be chosen in the text-to-speech settings of the system, and AutoJs6's tts module and other apps read aloud with it; every speaker is one voice, bilingual models add an English voice for every speaker, and the speech rate follows the system or app setting; without an installed model the engine reports missing voice data
- `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
- `Dependency` Attach sherpa-onnx 1.13.8
- `Dependency` Attach Jetpack Compose BOM 2026.09.00
- `Dependency` Attach AndroidX Activity Compose 1.12.2
- `Dependency` Attach AndroidX Core KTX 1.15.0
- `Dependency` Attach Kotlin Coroutines 1.11.0

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
```

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). The sherpa-onnx speech runtime uses the Apache-2.0 license, onnxruntime and silero VAD use the MIT license; the license of every model is listed in the model catalog.

- [Third-party notices (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### Related links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- sherpa-onnx project: https://github.com/k2-fsa/sherpa-onnx
