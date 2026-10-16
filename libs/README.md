# Staged binary artifacts

This directory holds the exact, hash-locked binary artifacts the plugin consumes. Gradle never resolves
them from sibling repositories or from `mavenLocal()`: `app/build.gradle.kts` reads the lock files in
`../locks/` and fails configuration when a staged file is missing, extra, listed with an invalid digest
or different from its locked SHA-256.

## Host contract AARs (lock `../locks/host-api-aars.lock`)

Release artifacts of the AutoJs6 host, consumed with `implementation` because the plugin runs in its own
process and needs the AIDL stubs and constants at run time.

| File | Host module | Content | SHA-256 |
| --- | --- | --- | --- |
| `common-plugin-api.aar` | `plugin-api/common-plugin-api` | `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`, `AutoJs6HostSettingsContract` | `ed7a71942ffb67255ebad8d8a2c912b01c4a3cdd8aad497baba0687b4072032a` |
| `speech-api.aar` | `plugin-api/speech-api` | `ISpeechPlugin`, `ISpeechSession`, the session, wake and progress callbacks, `SpeechPluginActions`, `SpeechPluginIds`, `SpeechContract`, `SpeechCapabilityKeys`, `SpeechLimits`, `SpeechOptionKeys`, `SpeechResultKeys`, `SpeechModelKeys`, `SpeechErrorCodes` | `74f29a089c7da86c07920a956455b18e8039def5f006c7fddf78af1db33cd1f2` |

Provenance (2026-10-16): `common-plugin-api.aar` was assembled with `:plugin-api:common-plugin-api:assembleRelease`
on the host snapshot `6c151c900e` (6.8.0, versionCode 5352). The module last changed at host commit `aa8ebaf72e`
and is unchanged at the roadmap baseline `33a1514b74`, so this is the byte-identical artifact that Batch Rename
stages; rebuilt next to `speech-api` it is still byte-identical. `speech-api.aar` (contract version 1, roadmap
P1.1 / D21) was assembled with `:plugin-api:speech-api:assembleRelease` from the sources of host commit
`1d6280bf8` (6.8.0, versionCode 5451), which introduces `plugin-api/speech-api`; the module is unchanged up to
`51b6bd3b7`. Its `SpeechPluginIds.REQUIRED_HOST_VERSION_CODE` (5451) is the minimum host build the plugin declares
(roadmap D28, temporary until P6.4).

## sherpa-onnx runtime (lock `../locks/sherpa-onnx.lock`, roadmap D13 / P0.2)

`sherpa-onnx-1.13.8.aar` is the official GitHub Release asset of sherpa-onnx `v1.13.8` (tag commit
`11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf`, published 2026-09-10), 50129134 bytes, SHA-256
`633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96`, Apache-2.0 with the bundled
onnxruntime 1.28.2 (MIT). It carries the Kotlin API (`com.k2fsa.sherpa.onnx`, no consumer rules) and
four native libraries for each of arm64-v8a, armeabi-v7a, x86_64 and x86.

Why a staged file (roadmap appendix D Q2, decided (b) on 2026-10-16): Maven Central has no k2-fsa
coordinate (`https://repo1.maven.org/maven2/com/k2fsa/` answers 404 and a search for
`g:com.k2fsa.sherpa.onnx` finds nothing). JitPack publishes `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.8`,
which its build log shows to be a download of this very release asset (same SHA-256), but JitPack
rebuilds artifacts on demand and is not an immutable repository, so the file is staged and pinned here.

Packaging (roadmap D14 / D31): only `libsherpa-onnx-jni.so` and `libonnxruntime.so` enter the APK; the C
and C++ API libraries are excluded because the JNI library does not need them. Every library is 16 KB
aligned; `py .python/verify_native_alignment.py` regenerates `../docs/development/native-abi-report.md`
from the AAR and `--check` keeps it in sync.

To update: download the new release asset, record its tag commit, size and SHA-256 in the lock, update
`THIRD_PARTY_NOTICES.md`, run `py .python/verify_native_alignment.py` and the device tests on every ABI.
