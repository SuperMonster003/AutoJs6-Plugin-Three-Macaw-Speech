******

### Release history

******

# v1.0.0

###### 2026/10/16

* `Hint` Development preview: AutoJs6 can discover the plugin, which loads the speech runtime and downloads and manages models; speech recognition, wake words, text-to-speech, the plugin screens and the speech script module are not delivered yet
* `Hint` Requires AutoJs6 6.8.0 (5451) or later (the exact minimum build will be filled in once the host changes land)
* `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and speech capability service
* `Feature` Built-in sherpa-onnx 1.13.8 speech runtime and silero VAD v5 voice activity detection model, with one package per ABI for 4 ABIs
* `Feature` Model management: downloads the offline speech models of the signed model catalog from the AutoJs6 shared assets releases, falling back to the official sherpa-onnx releases; every file is checked by SHA-256 before use, with resumable downloads, local import (tar, tar.bz2, zip), removal and default models; models limited to non-commercial use or without a declared license need their license accepted first
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach sherpa-onnx 1.13.8
* `Dependency` Attach Jetpack Compose BOM 2026.09.00
* `Dependency` Attach AndroidX Activity Compose 1.12.2
* `Dependency` Attach AndroidX Core KTX 1.15.0
* `Dependency` Attach Kotlin Coroutines 1.11.0
