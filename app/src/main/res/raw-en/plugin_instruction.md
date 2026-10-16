3-Macaw Speech uses [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) to run speech recognition, voice activity detection, wake word detection and neural text-to-speech offline on the device. Everything is computed locally and no audio is uploaded.

> This is a development preview: AutoJs6 can discover the plugin and it loads the speech runtime, but recognition, wake words, synthesis, model downloads, the plugin screens and the `speech` script module are not delivered yet. The text below describes the design goals of 1.0.0; see the roadmap for progress.

### Features

- Speech recognition: streaming dictation from the microphone (partial and final results in real time) and WAV file recognition, with offline models for Chinese, English, mixed Chinese and English, Japanese, Korean and Cantonese.
- Voice activity detection: the built-in silero VAD v5 splits audio into speech segments on its own and also ends dictation sentences automatically.
- Wake words: type the keyword as plain text (for example `小六小六` or `hey macaw`) without training a model; after waking up, dictation can start automatically.
- Text-to-speech: several neural Chinese and English voices, synthesized to WAV files or used as the system TTS engine by the AutoJs6 `tts` module and other apps.
- Model management: downloads from a signed model catalog, enabled only after every file passes its SHA-256 check, plus import of local models; models restricted to non-commercial use show their license before the download.

Progress and design are available on the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech).
