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

- Speech recognition: streaming dictation from the microphone (partial and final results in real time) and recognition of audio files (WAV, MP3, AAC, Ogg, FLAC and more, or the sound of a video file), with offline models for Chinese, English, mixed Chinese and English, Japanese, Korean and Cantonese.
- Voice activity detection: the built-in silero VAD v5 splits audio into speech segments on its own and also ends dictation sentences automatically.
- Wake words: type the keyword as plain text (for example `小六小六` or `hey macaw`) without training a model; after waking up, dictation can start automatically.
- Text-to-speech: several neural Chinese and English voices, synthesized to WAV files or used as the system TTS engine by the AutoJs6 `tts` module and other apps.
- System voice input: microphone buttons in other apps, such as 3-Stove Agent, can use the offline recognition; you say one sentence in a bottom sheet and the recognized text goes back to that app.
- Digital assistant: choose 3-Macaw Speech as the default digital assistant app, then long press the power or Home button, say one sentence, edit the recognized text if needed and hand it to 3-Stove Agent, 3-Stone AI or another app that takes voice commands, or copy it. It also provides a system speech recognition service: on Android 11 and lower, choosing it as the assistant usually makes it the default recognition service too; from Android 12 on, choose it separately in the system's voice input setting (some phones lack this setting), and apps can also name it directly.
- Trigger entries: open the same dictation sheet without the power button: a Quick Settings tile (also on the lock screen), a home screen widget and an app icon shortcut, a draggable floating microphone (off by default), a key trigger that holds or double presses a volume key (an accessibility service that only filters keys, off by default) and the voice key of a Bluetooth headset. Nothing records until the sheet listens, and only for the few seconds of dictation.
- Resident wake word (off by default): once on, saying a wake word (`小鹦小鹦` and `Hey Macaw` by default, editable) opens the same dictation sheet, turns the screen on when it is off or locked, and reads the recognized text aloud. While it listens, the microphone indicator stays lit, a notification stays and the battery drains faster; a listening time (turn off after 10, 30 or 60 minutes, or always on) and Listen only while charging can be set. When a script, another app or another entry needs the microphone, it steps aside and listens again afterwards.
- Plugin tools (AutoJs6 6.8.0 or later): four tools, `speech_listen` (hear one sentence and return the recognized text), `speech_speak` (read a text aloud), `speech_transcribe` (transcribe an audio file on the phone) and `speech_synthesize` (synthesize text to a WAV or M4A file). The MCP Server plugin uses them so that an AI assistant on a computer can listen and speak through the phone, and scripts can call them with `plugins.tools` too.
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

Recognition, wake word and synthesis models take 9 MB to 410 MB each, so they are not bundled with the APK. The plugin carries a signed model catalog and downloads the models from immutable releases of the AutoJs6 shared assets repository, falling back to the upstream sherpa-onnx address when the mirror is unavailable; on both paths every file must pass its SHA-256 check before the model is enabled. The 1.0.0 catalog contains these models (sizes are the space they take once installed):

| Model ID | Purpose | Languages | Size | License | Commercial use |
| --- | --- | --- | --- | --- | --- |
| `asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8` (recommended) | Recognition (offline, with punctuation) | zh, en, ja, ko, yue | 239.5 MB | FunASR Model License 1.1 | Allowed |
| `asr.zipformer-streaming.zh-en.2023-02-20.int8` (recommended) | Recognition (streaming) | zh, en | 199.1 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.en.2023-06-26.int8` (recommended) | Recognition (streaming) | en | 70.3 MB | Apache-2.0 | Allowed |
| `asr.zipformer-streaming.zh.2025-06-30.int8` | Recognition (streaming) | zh | 167.4 MB | None declared | Non-commercial only |
| `kws.zipformer.zh-en.2025-12-20` (recommended) | Wake word | zh, en | 8.8 MB | Apache-2.0 | Allowed |
| `tts.vits.melo.zh-en` (recommended) | Synthesis (speakers: 1, 44.1 kHz) | zh, en | 177.4 MB | MIT | Allowed |
| `tts.vits.icefall.zh-aishell3` (recommended) | Synthesis (speakers: 174, 8 kHz) | zh | 32.8 MB | None declared | Unknown |
| `tts.kokoro.multi-lang.v1_1` | Synthesis (speakers: 103, 24 kHz) | zh, en | 405.7 MB | Apache-2.0, GPL-3.0-or-later (espeak-ng-data) | Allowed |
| `tts.matcha.icefall.zh-baker` | Synthesis (speakers: 1, 22.05 kHz) | zh | 81.0 MB | None declared, MIT (vocoder) | Non-commercial only |
| `tts.vits.piper.en-us-lessac-medium` | Synthesis (speakers: 1, 22.05 kHz) | en | 81.1 MB | MIT, GPL-3.0-or-later (espeak-ng-data) | Non-commercial only |

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

- `speech.listen(options)` opens a dictation session that reports through the events `ready`, `partial`, `final`, `endpoint`, `vad`, `warning`, `error` and `end`; `stop()` ends it early. `speech.recognize(path)` recognizes an audio file (formats other than WAV need AutoJs6 6.8.0 or later; 16 kHz PCM is accepted too) and `speech.detectVoice(path)` lists its speech segments.
- `speech.wake(keywords, options)` opens a wake word session with the keywords written as plain text (at most 50); with `thenListen` every wake up turns into one round of dictation on the same recording, whose session is the `listen` property of the `wake` event.
- `speech.synthesize(text, path, options)` synthesizes to a WAV file (at most 8000 characters, with an optional model, speaker and speed); `speech.speak(text, options)` and `tts(text, { engine })` speak through the system TTS engine.
- `speech.models` lists, downloads (`install`, with progress), imports and removes models and sets the default models; `speech.capabilities()` answers the plugin version, its features and whether it holds the microphone permission.
- The whole plugin has one microphone session at a time (dictation or wake word); without the microphone permission the calls fail with `PERMISSION_DENIED`, which the plugin's home screen grants. Recognized text is never written to the AutoJs6 log.
- With AutoJs6 6.8.0 or later, scripts can also call these four plugin tools through `plugins.tools.call`, for example `plugins.tools.call('speech_speak', { text: 'Hello' })`; the arguments are the same ones MCP Server sees. For everyday scripts the `speech` module is more flexible.
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
- Digital assistant: the system must let you choose a third-party digital assistant app. How to open it depends on the system: a long press of the Home button (three-button navigation), a swipe up from a bottom corner, or the power button set to open the digital assistant in the system settings. The ColorOS and HyperOS tablets tested (Android 15) keep the power button and the gestures for their own assistants; use the trigger entries there.
- Trigger entries: tested on Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15), Xiaomi Pad 23046RP50C (HyperOS, Android 15) and API 33 and 35 emulators. On ColorOS and HyperOS a tile tapped on the lock screen asks for the unlock first and then opens the sheet; some launchers accept a widget request without showing a confirmation, so add the widget by hand with the steps in the settings. With the screen off, phones pass no keys to accessibility services, so the key trigger works only while the screen is on. Bluetooth headset voice key: with a Sony WF-1000XM4 and the XQ-DQ72 (Android 13) every long press opened the sheet and the dictation used the headset's microphone; when another app (such as the Google app) answers too, the system shows a chooser first. On the HyperOS tablet (Xiaomi Pad 23046RP50C, Android 15) the chooser did nothing after 3-Macaw Speech was picked.
- Resident wake word: tested on Sony G8441 (Android 9), XQ-AT72 (Android 12), XQ-DQ72 (Android 13), OnePlus OPD2413 (ColorOS, Android 15) and an API 35 emulator (16 KB pages). Battery use with the screen off for 30 minutes (Sony XQ-DQ72, Android 13; Android's estimate from the device's power profile, the phone connected by USB with the unplugged state simulated): on, 17.9 mAh and 454 seconds of the plugin's CPU time (about a quarter of one CPU core on average), recording all the time; off, 0.006 mAh, the plugin not running. Actual use varies by device.
- Plugin tools: tested on Sony G8441 (Android 9), XQ-DQ72 (Android 13), and API 24 and 35 emulators (on API 24 only the parts that need no model); calls through MCP Server tested on Sony G8441 (Android 9).

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

Listening for a wake word all the time means recording all the time: from Android 12 on, the status bar shows the microphone indicator (the green dot) for as long as an app records, and no app can hide it; the low-power hotword channels that light no indicator are open only to preinstalled system assistants, not to third-party apps, even when one is the default digital assistant. So the resident wake word is off by default, and while it is on the green dot and a notification stay (the notification can turn it off). On a Sony XQ-DQ72 with the screen off for 30 minutes, Android estimated the plugin's use at about 18 mAh with it on (about 0.4% of the battery) and almost nothing with it off. Turn it on only when you need it: set the listening time to 10, 30 or 60 minutes, or turn on Listen only while charging (it pauses when unplugged and continues when plugged in again); the trigger entries such as the tile, the widget and the floating microphone record nothing in between.

#### Why does only a "Tap to speak" notification appear after the wake word?

From Android 10 on, an app in the background may open a screen directly only when it is the default digital assistant or may display over other apps. When 3-Macaw Speech is not the default digital assistant, turning the resident wake word on asks once for that permission; without it, a heard wake word shows a "Tap to speak" notification that opens the sheet and goes away after a minute. To let the wake word open the sheet directly, make 3-Macaw Speech the default digital assistant, or allow it to display over other apps in the system settings.

#### Why am I asked to turn 3-Stove Agent off before turning the resident wake word on?

The resident listeners of 3-Macaw Speech, 3-Stone AI and 3-Stove Agent exclude each other: only one app at a time may listen on a phone, or they would compete for the microphone. While one is on, the other's switch says which app listens and opens its settings so that you can turn it off there. After a restart or an update the resident wake word does not start recording by itself: while the switch is on, a "Tap to resume the resident wake word" notification appears, and a tap, or opening the home screen or the settings of 3-Macaw Speech, resumes it; if the listening time ran out meanwhile, the switch turns off instead.

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
- Storage: models live in the private directory of the plugin and local models are imported through the system file picker, so no storage permission is requested.
- Protected entries: the capability service and the activation entry require the AutoJs6 plugin permission (`org.autojs.permission.PLUGIN`), so other apps cannot call them directly; the system TTS engine and its voice check, sample text and settings entries are open to every app, as Android requires, and only synthesize speech.
- System voice input: like the system TTS engine it is open to every app; it records only while its sheet is on the screen, returns nothing but the recognized text and can be turned off in the settings.
- Digital assistant: it never reads the screen and does not listen in the background while the resident wake word is off; it records only while its sheet is on the screen, and the recognized text leaves the sheet only when you tap an app button or copy it (on the lock screen, after you unlock). The speech recognition service recognizes only for apps that hold the microphone permission themselves.
- Trigger entries: the tile, the widget, the shortcut, the floating microphone, the key trigger and the headset voice key record nothing themselves; they only open the dictation sheet. The key trigger is an accessibility service that only filters keys: it never reads the screen content, performs no gestures and takes only the chosen volume key (a short press still changes the volume, and keys are left alone while the screen is off or during a call). The floating microphone needs the permission to display over other apps and stays on screen with a quiet notification; starting at boot only brings a floating microphone that is on back after a restart. On Android 12 and later the headset voice key needs Nearby devices, asked only in a sheet the headset opened: it only lets the Bluetooth service hand the voice recognition to the sheet and connect the headset's microphone; no scanning, no location.
- Resident wake word: off by default and turned on only in the plugin's screens (after a restart or an update a tap on its notification resumes it; nothing records by itself). The wake word is detected on the device, no audio is kept or uploaded and the recognized text is never logged; while it is on it records all the time, the microphone indicator stays lit and its notification can turn it off. It excludes the resident listeners of 3-Stone AI and 3-Stove Agent: the system allows one at a time.
- Plugin tools: the tool service is protected by the AutoJs6 plugin permission as well, so only AutoJs6 can call it directly. `speech_listen` records one sentence per call, with a notification while it records (and the microphone indicator from Android 12 on); `speech_transcribe` reads only the one file AutoJs6 hands over, and `speech_synthesize` hands the audio back to AutoJs6, which writes it to the requested place. Neither recognized nor spoken text is written to logs.

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
tool service: org.autojs.plugin.TOOL_PROVIDER (short name speech: listen, speak, transcribe, synthesize; AutoJs6 6.8.0+)
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

#### v1.2.0

_2026/10/21_

- `Hint` New resident wake word, off by default: while on, it keeps listening for the wake word on the device, the microphone indicator stays lit, a notification stays and the battery drains faster (see the README for a 30 minute screen-off measurement). Using it with "Listening time" or "Listen only while charging" is recommended
- `Feature` Resident wake word (Settings > Resident wake word): saying a wake word ("小鹦小鹦" and "Hey Macaw" by default; editable, with high, medium and low sensitivity) opens the same dictation sheet as the digital assistant; with the screen off or locked it turns the screen on, and you unlock only before sending or copying
- `Feature` A sheet opened by the wake word reads the recognized text aloud, with a 3-Macaw Speech voice of that language if there is one, else with the system voice; this can be turned off in the settings
- `Feature` Listening time (turn off after 10, 30 or 60 minutes, or always on) and Listen only while charging (pauses when unplugged and continues when plugged in again), the same options as in 3-Stove Agent
- `Feature` When 3-Macaw Speech is not the default digital assistant, turning the resident wake word on asks once for the permission to display over other apps; without it, a heard wake word shows a "Tap to speak" notification that opens the sheet
- `Feature` When a script, another app or another entry of this plugin uses the microphone, the resident wake word steps aside and listens again once they finish
- `Feature` Only one resident listener can run at a time among 3-Stone AI, 3-Stove Agent and 3-Macaw Speech; while another one is on, you are asked to turn it off first and can open its settings directly
- `Feature` No recording starts by itself after a restart or an update: while the switch is on, a "Tap to resume the resident wake word" notification appears, and opening the home screen or the settings resumes it as well
- `Feature` Recognizing audio files (speech.recognize, speech.detectVoice) accepts common formats besides WAV, such as MP3, AAC (including M4A), Ogg (Vorbis, Opus), FLAC and AMR, as well as the sound of video files (MP4, WebM and others); the limit stays at 200 MB or 30 minutes (needs AutoJs6 6.8.0 or later; earlier versions accept WAV only)
- `Feature` Four plugin tools (AutoJs6 6.8.0 or later): speech_listen hears one sentence and returns the text, speech_speak reads aloud, speech_transcribe transcribes an audio file on the phone, speech_synthesize synthesizes text to a WAV or M4A file. The MCP Server plugin uses them so that an AI assistant on a computer can listen and speak through the phone, and scripts can call them through plugins.tools; when the plugin cannot record right away, listening first shows a "Tap to speak" notification on the phone and starts once it is tapped

#### v1.1.0

_2026/10/21_

- `Hint` New trigger entries: nothing records in between; a tap on the Quick Settings tile, the home screen widget, the shortcut or the floating microphone, or a press of a volume key or of a Bluetooth headset's voice key, opens the same dictation sheet as the digital assistant
- `Hint` New permissions: display over other apps (only for the floating microphone, off by default), start at boot (only to bring back a floating microphone that is on after a restart), a special use foreground service (only keeps the floating microphone on screen, never records) and nearby devices (Android 12 and later, asked only when the headset voice key opens the sheet, to take the headset's voice recognition and microphone over). The key trigger is turned on in the system accessibility settings and only filters keys
- `Feature` Quick Settings tile (Voice command): add it in the Triggers group of the settings (Android 13 and later confirm through the system; older versions show the manual steps) and tap it in Quick Settings to speak; on the lock screen you can dictate right away and unlock only before sending or copying (ColorOS and HyperOS ask for the unlock first); a long press of the tile opens the Triggers settings
- `Feature` Floating microphone: off by default and asks for the permission to display over other apps when turned on; a round button you drag along the screen edge and tap to speak; the button records nothing itself, stays on screen with a quiet notification (which can turn it off) and comes back after a restart while it is on; it hides while the dictation sheet shows and can show together with the floating ball of 3-Stove Agent
- `Feature` Home screen widget (Tap to speak) and the shortcut of a long press on the app icon (Voice command) both open the dictation sheet; the settings can put the widget on the home screen and show the manual steps where the launcher cannot
- `Feature` Key trigger: an accessibility service that only filters keys and never reads the screen content, turned on in the system accessibility settings; by default a hold of volume down for about 0.8 seconds opens the dictation sheet, and it can be a hold of volume up or a double press of volume down instead; a short press still changes the volume, and keys are left alone while the screen is off or during a call
- `Feature` Bluetooth headset voice key: a long press on the headset's voice key or touch area opens the dictation sheet (3-Macaw Speech answers the voice command request VOICE_COMMAND). With Nearby devices allowed it listens through the headset's microphone and every press works. When another app answers too, the system first asks which app to use. Turn it off with Headset voice key in the Triggers group of the settings
- `Feature` Every entry opens the same sheet as a long press of the power button: through the assistant session while 3-Macaw Speech is the default digital assistant, otherwise the same sheet directly; when the system does not show the assistant session (for example after a tile tapped on the lock screen asks for the unlock), the sheet opens directly as well
- `Feature` Packaged applications can use speech recognition and model management through a separate entry; microphone access follows each application's own permission
- `Fix` The dictation sheet records as soon as it opens: while the model still loads (the first time, or after 5 idle minutes) the first words are no longer lost, and a short phrase such as hello no longer goes unanswered

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
