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
| `speech-api.aar` | `plugin-api/speech-api` | `ISpeechPlugin`, `ISpeechSession`, the session, wake and progress callbacks, `SpeechPluginActions`, `SpeechPluginIds`, `SpeechContract`, `SpeechCapabilityKeys`, `SpeechLimits`, `SpeechOptionKeys`, `SpeechResultKeys`, `SpeechModelKeys`, `SpeechErrorCodes` | `2d45a5e71b1222cebda5b8673cc4d3f5d30dc07258e5ffbbe568db4076e0ccb5` |
| `plugin-tool-api.aar` | `plugin-api/plugin-tool-api` | `IPluginToolProvider`, the call and output tree callbacks, `PluginToolProviderService`, `PluginToolCall`, `PluginToolResult`, `PluginToolContract`, `PluginToolLimits`, `PluginToolNames`, `PluginToolValues`, `PluginToolErrorCodes` and the key objects | `8bce82dd2c9811fa689f6b2c7b3926262cf3a756d78569ef4b19e1af21016005` |

Provenance (2026-10-16): `common-plugin-api.aar` was assembled with `:plugin-api:common-plugin-api:assembleRelease`
on the host snapshot `6c151c900e` (6.8.0, versionCode 5352). The module last changed at host commit `aa8ebaf72e`
and is unchanged at the roadmap baseline `33a1514b74`, so this is the byte-identical artifact that Batch Rename
stages; rebuilt next to `speech-api` it is still byte-identical. The previous `speech-api.aar` (contract version 1, roadmap
P1.1 / D21, introduced at host commit `1d6280bf8`) was assembled with `:plugin-api:speech-api:assembleRelease` on
2026-10-20 from the sources of host commit `6c08e53e2` (6.8.0), which raises
`SpeechPluginIds.REQUIRED_HOST_VERSION_CODE` from the P1.3 temporary value 5451 to 5456, the first host build with
every speech change of the host (up to `11f75efbe`, VERSION_BUILD 3804 + 1652; roadmap D28, P6.4); only
`SpeechPluginIds.class` differs from the artifact of 2026-10-16. It is the minimum host build the plugin declares.

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

## Packaged speech entry (2026-10-20)

The current speech-api.aar is rebuilt from host commit be1b3eee0ab9817fff5ebbdffcada282330e9313 using :plugin-api:speech-api:assembleRelease. SpeechPluginActions adds the separate packaged service action and normal permission; the original signed service, AIDL transaction order, contract version 1 and minimum host build 5456 remain unchanged. This replaces the earlier 0b1d9a0d26dcd68c7f59c403e88c70b4491fd64d7ec38a2edcc14ea9e6a62cb3 artifact.

## Speech contract version 2 (2026-10-26)

The current speech-api.aar (roadmap P8.3.1, D132 - D136) was assembled with :plugin-api:speech-api:assembleRelease from host commit c31ea30eeb4a556c3c56d3c424f7e3933799b118 in a clean clone. ISpeechPlugin appends enrollSpeaker, verifySpeaker, identifySpeaker, listSpeakers, removeSpeaker and the three calls reserved for P8.4 (diarizeFile, detectLanguage, tagAudio) after refreshCatalog, so the 14 transactions of contract version 1 keep their codes; SpeechContract has CONTRACT_VERSION 2 (MIN 1, MAX 2), negotiate(), FEATURES_V1, the kinds speaker / segment / langid / tagger and four features; SpeechCapabilityKeys adds MAX_CONTRACT_VERSION (`speechMaxContractVersion`), SpeechErrorCodes SPEAKER_NOT_FOUND and AUDIO_TOO_SHORT, SpeechLimits the speaker ceilings, SpeechOptionKeys and SpeechResultKeys the speaker keys. The plugin keeps advertising `speechContractVersion` 1 and adds `speechMaxContractVersion` 2 (D136), and SpeechPluginIds.REQUIRED_HOST_VERSION_CODE stays 5456 (D122). Only classes.jar differs from the artifact of host commit be1b3eee0 (18d98c5dd386de30a02c34cd80d44ef822abc5729b6570209ba55119c2990e1f): the ISpeechPlugin classes and the six constant objects above.

## Plugin tool provider (2026-10-22)

plugin-tool-api.aar (roadmap P7.6.1, D99) was assembled with :plugin-api:plugin-tool-api:assembleRelease from host commit 2c55377a9a9d848d5adb149a15104e729f6963ad in a clean clone. The module last changed at 3e811b0ea; host commit 054366be6 bounds results by PluginToolLimits.MAX_RESULT_CHARS (192 Ki UTF-16 units of the JSON and the text together, because the result reaches the host in a oneway Binder callback) and turns a report the host refuses into LIMIT_EXCEEDED instead of a silent drop (roadmap appendix D Q41, MCP roadmap D65). Only classes.jar differs from the previous artifact of host commit b6b1468d4 (c0e31a199d67710a9ea00cb82c5403b4bca1f45b67f6968158c7530309ce2875). The base class PluginToolProviderService exists since b4ae5535e. The AAR keeps org.autojs.plugin.tool.api through its consumer rules and needs the staged common-plugin-api.aar for PluginInfo. It is the same contract the host publishes as io.github.supermonster003:autojs6-plugin-tool-api.
