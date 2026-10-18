3-Macaw Speech uses [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) to run speech recognition, voice activity detection, wake word detection and neural text-to-speech offline on the device. Everything is computed locally and no audio is uploaded.

> This is a development preview: AutoJs6 can discover the plugin, which downloads and manages models, recognizes speech, detects voice activity and wake words, and synthesizes speech, also as the system TTS engine; its own screens manage the models, try them out and hold the settings. The AutoJs6 version that provides the `speech` script module is not released yet. The text below describes the 1.0.0 design; see the roadmap for the progress.

### Features

- Speech recognition: streaming dictation from the microphone (partial and final results in real time) and WAV file recognition, with offline models for Chinese, English, mixed Chinese and English, Japanese, Korean and Cantonese.
- Voice activity detection: the built-in silero VAD v5 splits audio into speech segments on its own and also ends dictation sentences automatically.
- Wake words: type the keyword as plain text (for example `小六小六` or `hey macaw`) without training a model; after waking up, dictation can start automatically.
- Text-to-speech: several neural Chinese and English voices, synthesized to WAV files or used as the system TTS engine by the AutoJs6 `tts` module and other apps.
- System voice input: microphone buttons in other apps, such as 3-Stove Agent, can use the offline recognition; you say one sentence in a bottom sheet and the recognized text goes back to that app.
- Digital assistant: choose 3-Macaw Speech as the default digital assistant app, then long press the power or Home button, say one sentence, edit the recognized text if needed and hand it to 3-Stove Agent, 3-Stone AI or another app that takes voice commands, or copy it; as the assistant it is also the speech recognition service other apps use.
- Model management: downloads from a signed model catalog, enabled only after every file passes its SHA-256 check, plus import of local models; models restricted to non-commercial use show their license before the download.

Progress and design are available on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech).
