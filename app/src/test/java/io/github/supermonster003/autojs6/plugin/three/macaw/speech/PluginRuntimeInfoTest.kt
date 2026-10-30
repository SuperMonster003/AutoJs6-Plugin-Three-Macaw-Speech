package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.parseSupportedAbis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.pluginRuntimeFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure PluginInfo field mapping and the identity published by the build, the Manifest and the README sources
 * (roadmap 5.5, D14, D28). The Android-backed `toPluginInfo()` mapping is exercised by the device contract test.
 * zh-CN: 纯数据的 PluginInfo 字段映射, 以及构建, Manifest 与 README 源发布的身份 (路线图 5.5, D14, D28). 依赖 Android 的
 * `toPluginInfo()` 映射由设备契约测试覆盖.
 */
class PluginRuntimeInfoTest {

    @Test
    fun `runtime fields keep the identity, the package version, the four ABIs and the speech capabilities`() {
        val fields = pluginRuntimeFields(
            name = "3-Macaw Speech",
            description = "Offline speech",
            author = "SuperMonster003",
            id = "three-macaw-speech",
            engine = "speech",
            variant = "default",
            versionName = "1.0.0",
            versionCode = 7L,
            versionDate = "Oct 16, 2026",
            supportedAbis = SUPPORTED_ABIS.joinToString(","),
        )
        assertEquals("3-Macaw Speech", fields.name)
        assertEquals("Offline speech", fields.description)
        assertEquals("@raw/plugin_instruction", fields.instruction)
        assertEquals(ThreeMacawSpeechContract.INSTRUCTION_REFERENCE, fields.instruction)
        assertEquals("SuperMonster003", fields.author)
        assertEquals("three-macaw-speech", fields.id)
        assertEquals("speech", fields.engine)
        assertEquals("default", fields.variant)
        assertEquals("1.0.0", fields.versionName)
        assertEquals(7L, fields.versionCode)
        assertEquals("Oct 16, 2026", fields.versionDate)
        // Roadmap D14: sherpa-onnx ships native code for exactly four ABIs, reported in the split order.
        assertEquals(SUPPORTED_ABIS, fields.supportedAbis)
        // Roadmap D28: the P6.4 floor is the first host build with every speech change of the host (11f75efbe).
        assertEquals(5456L, fields.requiredHostVersion)
        assertEquals(ThreeMacawSpeechContract.REQUIRED_HOST_VERSION, fields.requiredHostVersion)
        assertEquals(1, fields.contractVersion)
        // Roadmap P2.3 / P2.4: model management, then recognition (P3), synthesis and the system TTS engine (P4), speakers (P8.3)
        // and the offline analyses (P8.4).
        assertEquals(
            listOf("models", "asr", "asrStreaming", "vad", "kws", "tts", "ttsSystemEngine", "audioDecode", "speaker", "diarization", "languageId", "audioTagging"),
            fields.features,
        )
        assertEquals("the version of assets/catalog/catalog-v3.json (BundledCatalogTest)", 3, fields.catalogVersion)
    }

    @Test
    fun `the supported ABI list rejects blanks and duplicates`() {
        assertEquals(listOf("arm64-v8a", "x86"), parseSupportedAbis(" arm64-v8a , x86 "))
        listOf("", ",", "arm64-v8a,", "arm64-v8a,,x86", "x86,x86").forEach { value ->
            assertThrows("\"$value\" must be rejected", IllegalArgumentException::class.java) { parseSupportedAbis(value) }
        }
    }

    @Test
    fun `identity constants follow roadmap 5_5 and appendix C_1`() {
        assertEquals("three-macaw-speech", ThreeMacawSpeechContract.PLUGIN_ID)
        assertEquals("speech", ThreeMacawSpeechContract.ENGINE)
        assertEquals("default", ThreeMacawSpeechContract.VARIANT)
        assertEquals("org.autojs.plugin.INFO", ThreeMacawSpeechContract.INFO_ACTION)
        assertEquals(ThreeMacawSpeechContract.PLUGIN_ID, ThreeMacawSpeechContract.INFO_CATEGORY)
        assertEquals("org.autojs.plugin.SPEECH", ThreeMacawSpeechContract.SERVICE_ACTION)
        assertEquals("speech", ThreeMacawSpeechContract.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.speech.api.ISpeechPlugin", ThreeMacawSpeechContract.CAPABILITY_DESCRIPTOR)
        assertEquals("speechContractVersion", ThreeMacawSpeechContract.CAPABILITY_CONTRACT_VERSION)
        assertEquals("speechFeatures", ThreeMacawSpeechContract.CAPABILITY_FEATURES)
        assertEquals("speechCatalogVersion", ThreeMacawSpeechContract.CAPABILITY_CATALOG_VERSION)
    }

    @Test
    fun `the build publishes the same identity, ABI splits and native packaging as the constants`() {
        val build = RepositoryFiles.text("app/build.gradle.kts")
        assertTrue(build.contains("val globalApplicationId = \"$PACKAGE_NAME\""))
        assertTrue(build.contains("namespace = globalApplicationId"))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("resValue(\"string\", \"app_name\", \"3-Macaw Speech\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_author\", \"SuperMonster003\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_id\", \"${ThreeMacawSpeechContract.PLUGIN_ID}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_engine\", \"${ThreeMacawSpeechContract.ENGINE}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_variant\", \"${ThreeMacawSpeechContract.VARIANT}\")"))
        assertTrue(build.contains("resValue(\"string\", \"plugin_requires_host_version\", \"\$requiredHostVersion\")"))
        // Roadmap D28: the minimum host build is changed in the build script, the constant and the README sources together.
        assertTrue(build.contains("val requiredHostVersion = ${ThreeMacawSpeechContract.REQUIRED_HOST_VERSION}\n"))
        // Roadmap D14: one list drives the ABI splits, BuildConfig.SUPPORTED_ABIS and the release verifier.
        assertTrue(build.contains("val pluginAbis = listOf(${SUPPORTED_ABIS.joinToString(", ") { "\"$it\"" }})"))
        assertTrue(build.contains("include(*pluginAbis.toTypedArray())"))
        assertTrue(build.contains("isUniversalApk = true"))
        assertTrue(build.contains("buildConfigField(\"String\", \"SUPPORTED_ABIS\", \"\\\"\${pluginAbis.joinToString(\",\")}\\\"\")"))
        // Roadmap D31 (P0.2): compressed native libraries without the unused C / C++ API libraries.
        assertTrue(build.contains("useLegacyPackaging = true"))
        assertTrue(build.contains("val excludedSherpaLibraries = listOf(\"libsherpa-onnx-c-api.so\", \"libsherpa-onnx-cxx-api.so\")"))
        assertTrue(build.contains("strictAbis.set(pluginAbis.toSet())"))

        val settings = RepositoryFiles.text("settings.gradle.kts")
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-three-macaw-speech\""))
    }

    @Test
    fun `the README sources publish the same identity as the constants`() {
        val common = RepositoryFiles.text(".readme/common.json")
        mapOf(
            "plugin_application_id" to PACKAGE_NAME,
            "plugin_id" to ThreeMacawSpeechContract.PLUGIN_ID,
            "plugin_engine" to ThreeMacawSpeechContract.ENGINE,
            "plugin_variant" to ThreeMacawSpeechContract.VARIANT,
            "info_action" to ThreeMacawSpeechContract.INFO_ACTION,
            "info_category" to ThreeMacawSpeechContract.INFO_CATEGORY,
            "discovery_action" to ThreeMacawSpeechContract.SERVICE_ACTION,
            "discovery_category" to ThreeMacawSpeechContract.SERVICE_CATEGORY,
            "required_host_version_code" to ThreeMacawSpeechContract.REQUIRED_HOST_VERSION.toString(),
            "app_name" to "3-Macaw Speech",
        ).forEach { (key, value) ->
            assertTrue(".readme/common.json must publish $key = $value", common.contains("\"$key\": \"$value\""))
        }
    }

    @Test
    fun `the package of the Kotlin sources is the application id`() {
        assertEquals(PACKAGE_NAME, ThreeMacawSpeechContract::class.java.packageName)
    }

    private companion object {
        const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
        val SUPPORTED_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
    }

}
