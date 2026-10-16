******

### Release history

******

# v1.0.0

###### 2026/10/16

* `Hint` Development preview: AutoJs6 can discover the plugin, which downloads and manages models, recognizes speech in WAV files, fed PCM and from the microphone, detects voice activity and wake words, and synthesizes speech, also as the system TTS engine; the plugin screens and the speech script module are not delivered yet
* `Hint` Requires AutoJs6 6.8.0 (5451) or later (the exact minimum build will be filled in once the host changes land)
* `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and speech capability service
* `Feature` Built-in sherpa-onnx 1.13.8 speech runtime and silero VAD v5 voice activity detection model, with one package per ABI for 4 ABIs
* `Feature` Model management: downloads the offline speech models of the signed model catalog from the AutoJs6 shared assets releases, falling back to the official sherpa-onnx releases; every file is checked by SHA-256 before use, with resumable downloads, local import (tar, tar.bz2, zip), removal and default models; models limited to non-commercial use or without a declared license need their license accepted first
* `Feature` Speech recognition of WAV files (8, 16, 24 or 32-bit integer or float, any sample rate and channel count, resampled to 16 kHz mono in the plugin) with timed segments, plus language, emotion and event tags for SenseVoice models; streaming models accept hotwords
* `Feature` Dictation sessions on the microphone or on fed 16 kHz PCM report ready, partial and final results, endpoints, voice activity and the end; the silence that ends a sentence, the no-speech timeout and the longest duration are adjustable; streaming models update partial results word by word, offline models segment by segment
* `Feature` Voice activity detection lists the speech segments of a WAV file with the bundled silero VAD
* `Feature` Wake words: open-vocabulary keyword spotting; Chinese keywords are spelled in the pinyin of their customary reading, English words in the phones of the model's lexicon, and model tokens may be written directly; a hit can start a dictation round on the same recording
* `Feature` During a microphone session the plugin keeps recording with a microphone foreground service and its notification, so a session started while AutoJs6 is in the foreground goes on after AutoJs6 moves to the background or the screen turns off; one microphone session runs at a time; without the microphone permission sessions fail with PERMISSION_DENIED and name the permission screen
* `Feature` Speech synthesis: the installed synthesis models (VITS, Matcha and Kokoro; the catalog has 5 Chinese and English models) turn up to 8000 characters of text into a WAV file sentence by sentence, at the model's sample rate; a speaker and a speed (0.5 to 2 times) can be chosen, progress counts the characters done, and the synthesis can be cancelled at any time; a model with a single speaker ignores a chosen speaker and says so
* `Feature` System TTS engine: once a synthesis model is installed, 3-Macaw Speech can be chosen in the text-to-speech settings of the system, and AutoJs6's tts module and other apps read aloud with it; every speaker is one voice, bilingual models add an English voice for every speaker, and the speech rate follows the system or app setting; without an installed model the engine reports missing voice data
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach sherpa-onnx 1.13.8
* `Dependency` Attach Jetpack Compose BOM 2026.09.00
* `Dependency` Attach AndroidX Activity Compose 1.12.2
* `Dependency` Attach AndroidX Core KTX 1.15.0
* `Dependency` Attach Kotlin Coroutines 1.11.0
