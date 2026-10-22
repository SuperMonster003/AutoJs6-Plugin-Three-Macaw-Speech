3-Macaw Speech uses [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) to run speech recognition, voice activity detection, wake word detection and neural text-to-speech offline on the device. Everything is computed locally and no audio is uploaded.

> The `speech` module in scripts needs AutoJs6 6.8.0 (internal build 5456) or later. The system TTS engine, system voice input, speech recognition service, digital assistant and the plugin's own screens do not depend on AutoJs6 and work once the plugin is installed and models are downloaded.

### Features

- Speech recognition: streaming dictation from the microphone (partial and final results in real time) and recognition of audio files (WAV, MP3, AAC, Ogg, FLAC and more, or the sound of a video file), with offline models for Chinese, English, mixed Chinese and English, Japanese, Korean and Cantonese.
- Voice activity detection: the built-in silero VAD v5 splits audio into speech segments on its own and also ends dictation sentences automatically.
- Wake words: type the keyword as plain text (for example `小六小六` or `hey macaw`) without training a model; after waking up, dictation can start automatically.
- Text-to-speech: several neural Chinese and English voices, synthesized to WAV files or used as the system TTS engine by the AutoJs6 `tts` module and other apps.
- System voice input: microphone buttons in other apps, such as 3-Stove Agent, can use the offline recognition; you say one sentence in a bottom sheet and the recognized text goes back to that app.
- Digital assistant: choose 3-Macaw Speech as the default digital assistant app, then long press the power or Home button, say one sentence, edit the recognized text if needed and hand it to 3-Stove Agent, 3-Stone AI or another app that takes voice commands, or copy it. It also provides a system speech recognition service: on Android 11 and lower, choosing it as the assistant usually makes it the default recognition service too; from Android 12 on, choose it separately in the system's voice input setting (some phones lack this setting), and apps can also name it directly.
- Trigger entries: open the same dictation sheet without the power button: a Quick Settings tile (also on the lock screen), a home screen widget and an app icon shortcut, a draggable floating microphone (off by default), a key trigger that holds or double presses a volume key (an accessibility service that only filters keys, off by default) and the voice key of a Bluetooth headset. Nothing records until the sheet listens, and only for the few seconds of dictation.
- Resident wake word (off by default): once on, saying a wake word (`小鹦小鹦` and `Hey Macaw` by default, editable) opens the same dictation sheet, turns the screen on when it is off or locked, and reads the recognized text aloud. While it listens, the microphone indicator stays lit, a notification stays and the battery drains faster; a listening time (turn off after 10, 30 or 60 minutes, or always on) and Listen only while charging can be set. When a script, another app or another entry needs the microphone, it steps aside and listens again afterwards.
- Model management: downloads from a signed model catalog, enabled only after every file passes its SHA-256 check, plus import of local models; models restricted to non-commercial use show their license before the download.
- Standalone screens: the home screen shows the status and manages the models, Try it out tests dictation, wake words and synthesis directly, and the settings change the language, night mode, theme color, launcher icon and the recognition and synthesis defaults; the appearance follows AutoJs6 by default.

### Installation and use

1. Download the APK that matches the device ABI from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) and install it: most phones need `arm64-v8a`; if unsure, choose `universal`, which works on every device but is larger.
2. Open 3-Macaw Speech, grant the microphone permission on the home screen and download the models you need in the recognition, wake word and synthesis groups (the models the catalog recommends are a good start, see the model table below). On Android 13 and later the first download also asks to show download progress notifications.
3. Use it in AutoJs6: update AutoJs6 to 6.8.0 (internal build 5456) or later, enable 3-Macaw Speech in the Plugin Center and call the `speech` module in scripts (see Script API below). The AutoJs6 samples include four ready-to-run scripts under Samples > 3-Macaw Speech.
4. Use it in other apps: see System TTS setup below for the system TTS engine and the FAQ for system voice input, the speech recognition service and the digital assistant.

See the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech) for the full instructions, the model list and the FAQ, and the [AutoJs6 documentation](https://docs.autojs6.com/#/speech) for the script API.
