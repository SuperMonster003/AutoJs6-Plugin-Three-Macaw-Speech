******

### Release history

******

# v1.0.0

###### 2026/10/16

* `Hint` Development preview: AutoJs6 can discover the plugin, which downloads and manages models, recognizes speech in WAV files, fed PCM and from the microphone, detects voice activity and wake words, and synthesizes speech, also as the system TTS engine; models can be managed and tried out in the plugin's own screens; the AutoJs6 version with the speech script module is not released yet
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
* `Feature` Home screen: shows the speech runtime version, the number of installed models, the microphone permission (with a button to grant it) and whether 3-Macaw Speech is the preferred system TTS engine
* `Feature` Models on the home screen, grouped into recognition, wake words, synthesis and voice activity detection, with size, languages and license: download, pause, resume, cancel, delete and set as default; download progress also shows in a notification; models limited to non-commercial use or without a declared license ask you to accept their license first, and a download on mobile data or another metered network asks before it starts
* `Feature` Import a local model from a tar, tar.bz2 or zip archive or from a folder: files that match a catalog model are registered as that model, other files become your own model after you fill in its kind, model type, name, languages and ID
* `Feature` Try it out: live dictation (partial results in gray) with a choice of model and language, a wake word trial that lists the hits with their keyword and score, and speech synthesis of your text with a choice of model, speaker and speed, played back or saved as a WAV file; leaving the screen releases the microphone
* `Feature` System voice input: microphone buttons in other apps, such as 3-Stove Agent, can use the offline recognition of 3-Macaw Speech: say one sentence in a bottom sheet and the recognized text goes straight back to that app; it ends after a pause, or stop or cancel it earlier; it follows the endpoint sensitivity and voice activity detection of the settings and can be turned off in their recognition group
* `Feature` Settings: language, night mode, theme color and launcher icon (following AutoJs6 by default); the default recognition and synthesis models, endpoint sensitivity, voice activity detection, speaker, speed and a default voice per language for the tryout screen and the system TTS engine (scripts keep the documented defaults); when idle models are released, catalog refresh and cleanup of leftover files; the settings button of 3-Macaw Speech in the system text-to-speech settings opens this screen
* `Feature` About: version, the release history included with the plugin, update checks (automatically at most every 12 hours, a release can be ignored) and the licenses and notices: the app as a whole is conveyed under the GNU GPL 3.0 or later without any warranty, with the third-party notices, the full GPL text, the Unicode Terms of Use and where the corresponding source is
* `Feature` Launcher icon: adaptive icon (light), adaptive icon (dark), adaptive icon (automatic, the default) or transparent background icon
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach sherpa-onnx 1.13.8
* `Dependency` Attach Jetpack Compose BOM 2026.09.00
* `Dependency` Attach AndroidX Activity Compose 1.12.2
* `Dependency` Attach AndroidX Core KTX 1.15.0
* `Dependency` Attach Kotlin Coroutines 1.11.0
* `Dependency` Attach Material Color Utilities (MaterialKolor) 4.1.1
