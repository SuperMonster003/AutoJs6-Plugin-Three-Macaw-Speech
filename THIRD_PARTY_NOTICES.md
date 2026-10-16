# Third-party notices

3-Macaw Speech is licensed under the Mozilla Public License 2.0 (see `LICENSE`). The APK bundles the
components below; their licenses apply to those components only. Speech recognition, wake word and
text-to-speech models are not part of this repository or the APK: each model is downloaded on demand
and its own license is listed in the model catalog (roadmap D11 / D24).

## Speech runtime

| Artifact | Version | Source | License |
| --- | --- | --- | --- |
| sherpa-onnx Android AAR (`libs/sherpa-onnx-1.13.8.aar`: Kotlin API and `libsherpa-onnx-jni.so`) | 1.13.8 | https://github.com/k2-fsa/sherpa-onnx (release `v1.13.8`, commit `11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf`) | Apache-2.0 |
| ONNX Runtime (`libonnxruntime.so`, bundled in the sherpa-onnx AAR) | 1.28.2 | https://github.com/microsoft/onnxruntime | MIT |
| silero VAD v5 model (`app/src/main/assets/vad/silero_vad_v5.onnx`) | v5.0 | https://github.com/snakers4/silero-vad (`files/silero_vad.onnx` at tag `v5.0`, redistributed by sherpa-onnx as `silero_vad_v5.onnx`) | MIT |

The C and C++ API libraries of the AAR (`libsherpa-onnx-c-api.so`, `libsherpa-onnx-cxx-api.so`) are
not packaged. Provenance and SHA-256 digests are recorded in `locks/sherpa-onnx.lock`,
`locks/vad-assets.lock`, `libs/README.md` and `docs/development/native-abi-report.md`.

The device tests (not the APK) use `app/src/androidTest/assets/test_silero_vad.wav`, the
`tests/data/test.wav` recording of silero-vad (MIT), redistributed by sherpa-onnx as `test_silero_vad.wav`.

## AutoJs6 host contract libraries

| Artifact | Source | License |
| --- | --- | --- |
| `libs/common-plugin-api.aar` | AutoJs6 host module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6) | MPL-2.0 |

Provenance and SHA-256 digests are recorded in `locks/host-api-aars.lock` and `libs/README.md`.

## AndroidX and Kotlin libraries (Maven)

| Library | Version | License |
| --- | --- | --- |
| Jetpack Compose (BOM `androidx.compose:compose-bom`: runtime, ui, foundation, material3, ui-tooling-preview) | 2026.09.00 | Apache-2.0 |
| AndroidX Activity Compose (`androidx.activity:activity-compose`) | 1.12.2 | Apache-2.0 |
| AndroidX Core KTX (`androidx.core:core-ktx`) | 1.15.0 | Apache-2.0 |
| Kotlin Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-android`) | 1.11.0 | Apache-2.0 |
| Kotlin standard library (`org.jetbrains.kotlin:kotlin-stdlib`, added by the Android Gradle Plugin) | follows the build's Kotlin version | Apache-2.0 |

Jetpack Compose `ui-graphics` depends on `androidx.graphics:graphics-path`, whose
`libandroidx.graphics.path.so` (Apache-2.0) is packaged for arm64-v8a, armeabi-v7a, x86_64 and x86.

The Apache License 2.0 text is available at https://www.apache.org/licenses/LICENSE-2.0 and the MIT
license texts in the upstream repositories linked above.
