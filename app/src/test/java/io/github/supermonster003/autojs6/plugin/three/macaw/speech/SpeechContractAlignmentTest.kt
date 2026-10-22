package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.speech.api.ISpeechPlugin
import org.autojs.plugin.speech.api.SpeechCapabilityKeys
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechPluginActions
import org.autojs.plugin.speech.api.SpeechPluginIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plugin's own constants against the staged host contract `speech-api.aar` (roadmap P1.1, D21, D28): action,
 * category, package, identity, Binder descriptor, contract version, capability keys and the minimum host build agree,
 * so the plugin and the host cannot drift apart before the service implements `ISpeechPlugin` (roadmap P3 / P4).
 * zh-CN: 插件自身常量与暂存的宿主契约 `speech-api.aar` 对照 (路线图 P1.1, D21, D28): action, category, 包名, 身份, Binder 描述符,
 * 契约版本, 能力键与最低宿主构建号一致, 使插件在服务实现 `ISpeechPlugin` (路线图 P3 / P4) 之前也不会与宿主漂移.
 */
class SpeechContractAlignmentTest {

    @Test
    fun `discovery and identity match the host contract`() {
        assertEquals(SpeechPluginActions.SPEECH, ThreeMacawSpeechContract.SERVICE_ACTION)
        assertEquals(SpeechPluginActions.SERVICE_CATEGORY, ThreeMacawSpeechContract.SERVICE_CATEGORY)
        assertEquals(SpeechPluginActions.INFO, ThreeMacawSpeechContract.INFO_ACTION)
        assertEquals(PluginActions.INFO, SpeechPluginActions.INFO)
        assertEquals(SpeechPluginIds.OFFICIAL_PLUGIN_ID, ThreeMacawSpeechContract.PLUGIN_ID)
        assertEquals(SpeechPluginIds.OFFICIAL_PLUGIN_ID, ThreeMacawSpeechContract.INFO_CATEGORY)
        assertEquals(SpeechPluginIds.ENGINE, ThreeMacawSpeechContract.ENGINE)
        assertEquals(SpeechPluginIds.VARIANT_DEFAULT, ThreeMacawSpeechContract.VARIANT)
        assertEquals(SpeechPluginIds.OFFICIAL_PACKAGE_NAME, ThreeMacawSpeechContract::class.java.packageName)
        assertEquals(ISpeechPlugin.DESCRIPTOR, ThreeMacawSpeechContract.CAPABILITY_DESCRIPTOR)
    }

    @Test
    fun `contract version and capability keys match the host contract`() {
        assertEquals(SpeechContract.CONTRACT_VERSION, ThreeMacawSpeechContract.CONTRACT_VERSION)
        assertTrue(SpeechContract.supportsContractVersion(ThreeMacawSpeechContract.CONTRACT_VERSION))
        assertEquals(SpeechCapabilityKeys.CONTRACT_VERSION, ThreeMacawSpeechContract.CAPABILITY_CONTRACT_VERSION)
        assertEquals(SpeechCapabilityKeys.FEATURES, ThreeMacawSpeechContract.CAPABILITY_FEATURES)
        assertEquals(SpeechCapabilityKeys.CATALOG_VERSION, ThreeMacawSpeechContract.CAPABILITY_CATALOG_VERSION)
        assertEquals(PluginCapabilityKeys.REQUIRES_HOST_VERSION, SpeechCapabilityKeys.REQUIRES_HOST_VERSION)
        // Roadmap D84: "audioDecode" was appended to the host contract after the staged speech-api AAR.
        assertTrue("advertised features are contract features", (SpeechContract.FEATURES + "audioDecode").containsAll(ThreeMacawSpeechContract.FEATURES))
    }

    @Test
    fun `the minimum host build is the one the host contract accepts`() {
        // Roadmap D28: the host refuses plugins that declare less than the build which ships the speech module.
        assertEquals(SpeechPluginIds.REQUIRED_HOST_VERSION_CODE, ThreeMacawSpeechContract.REQUIRED_HOST_VERSION)
    }

    @Test
    fun `the manifest declares the contract service`() {
        val manifest = RepositoryFiles.text("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("<action android:name=\"${SpeechPluginActions.SPEECH}\" />"))
        assertTrue(manifest.contains("<category android:name=\"${SpeechPluginActions.SERVICE_CATEGORY}\" />"))
        assertTrue(manifest.contains("android:permission=\"${SpeechPluginActions.PLUGIN_PERMISSION}\""))
    }
}
