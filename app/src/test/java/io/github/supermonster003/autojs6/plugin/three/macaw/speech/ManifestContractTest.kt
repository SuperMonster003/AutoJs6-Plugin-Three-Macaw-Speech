package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.android
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.androidOrNull
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.child
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.children
import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Keeps `AndroidManifest.xml` and [ThreeMacawSpeechContract] from drifting apart: the host discovers the plugin through
 * the Manifest (Wake activity, INFO and SPEECH services), while the services and the tests use the Kotlin constants
 * (roadmap 5.5, D21, D29).
 * zh-CN: 防止 `AndroidManifest.xml` 与 [ThreeMacawSpeechContract] 漂移: 宿主经 Manifest 发现插件 (Wake Activity, INFO 与 SPEECH
 * 服务), 服务与测试则使用 Kotlin 常量 (路线图 5.5, D21, D29).
 */
class ManifestContractTest {

    private val manifest: Element by lazy { RepositoryFiles.xml("app/src/main/AndroidManifest.xml") }

    private val application: Element get() = manifest.child("application")

    @Test
    fun `the permissions are exactly the plugin permission and the roadmap D29 set`() {
        assertEquals(
            listOf(
                PLUGIN_PERMISSION,
                "android.permission.RECORD_AUDIO",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MICROPHONE",
                // Roadmap P2.3: the model download service.
                "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                "android.permission.INTERNET",
                "android.permission.POST_NOTIFICATIONS",
            ),
            manifest.children("uses-permission").map { it.android("name") },
        )
        val permissions = manifest.children("uses-permission").map { it.android("name") }
        assertTrue("no storage permission (roadmap D23 / D29)", permissions.none { it.contains("STORAGE") || it.contains("MEDIA") })
        assertTrue("no package queries yet", manifest.children("queries").isEmpty())
        assertTrue("no uses-sdk override: SDK levels come from version.properties", manifest.children("uses-sdk").isEmpty())
        assertTrue("no feature requirements", manifest.children("uses-feature").isEmpty())
    }

    @Test
    fun `the application disables backups and uses the generated title and system icon`() {
        assertEquals("false", application.android("allowBackup"))
        assertEquals("false", application.android("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.android("dataExtractionRules"))
        assertEquals("@string/app_name", application.android("label"))
        assertEquals("true", application.android("supportsRtl"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("icon"))
        assertEquals("@mipmap/ic_icon_studio_application", application.android("roundIcon"))
        assertNull("no Application subclass in the skeleton", application.androidOrNull("name"))
        assertNull("native libraries are extracted per useLegacyPackaging, not by a Manifest override", application.androidOrNull("extractNativeLibs"))
    }

    @Test
    fun `application meta-data names the wake activity, the author and the page alignment`() {
        val metaData = application.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(
            mapOf(
                "org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT" to "16384",
                "org.autojs.plugin.WAKE_ACTIVITY" to ".WakeActivity",
                "org.autojs.plugin.info.AUTHOR" to "@string/plugin_author",
            ),
            metaData,
        )
        assertEquals(PluginCapabilityKeys.NATIVE_PAGE_ALIGNMENT, "org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT")
    }

    @Test
    fun `the wake activity follows the activation contract and is the only activity`() {
        val wake = application.children("activity").single()
        assertEquals(".WakeActivity", wake.android("name"))
        assertEquals("true", wake.android("exported"))
        assertEquals("true", wake.android("excludeFromRecents"))
        assertEquals("true", wake.android("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.android("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.android("theme"))
        assertFilter(wake, action = "org.autojs.plugin.action.WAKE", category = DEFAULT_CATEGORY)
    }

    @Test
    fun `no launcher entry before roadmap P5`() {
        val categories = listOf("activity", "activity-alias")
            .flatMap { application.children(it) }
            .flatMap { it.children("intent-filter") }
            .flatMap { it.children("category") }
            .map { it.android("name") }
        assertTrue("the launcher aliases arrive with the screens (roadmap P5)", "android.intent.category.LAUNCHER" !in categories)
        assertTrue(application.children("activity-alias").isEmpty())
    }

    @Test
    fun `the model operation service is internal and of type data sync`() {
        val service = service(".service.ModelOperationService")
        assertEquals("false", service.android("exported"))
        assertEquals("dataSync", service.android("foregroundServiceType"))
        assertNull(service.androidOrNull("permission"))
        assertTrue("started by the plugin only", service.children("intent-filter").isEmpty())
    }

    @Test
    fun `the INFO and SPEECH services match the discovery constants`() {
        assertEquals(
            listOf(".service.ThreeMacawSpeechInfoService", ".service.ThreeMacawSpeechService", ".service.ModelOperationService"),
            application.children("service").map { it.android("name") },
        )
        val info = service(".service.ThreeMacawSpeechInfoService")
        assertFilter(info, action = PluginActions.INFO, category = ThreeMacawSpeechContract.INFO_CATEGORY)
        assertEquals(ThreeMacawSpeechContract.INFO_ACTION, PluginActions.INFO)
        assertRequiresHostVersion(info)

        val capability = service(".service.ThreeMacawSpeechService")
        assertFilter(capability, action = ThreeMacawSpeechContract.SERVICE_ACTION, category = ThreeMacawSpeechContract.SERVICE_CATEGORY)
        assertRequiresHostVersion(capability)

        listOf(info, capability).forEach { service ->
            assertEquals("true", service.android("exported"))
            assertEquals(PLUGIN_PERMISSION, service.android("permission"))
            assertNull("services run in the plugin's default process", service.androidOrNull("process"))
            assertNull("no foreground service type in the skeleton", service.androidOrNull("foregroundServiceType"))
        }
    }

    @Test
    fun `every exported component requires the plugin permission`() {
        val components = listOf("activity", "activity-alias", "service", "receiver", "provider").flatMap { application.children(it) }
        val unprotected = components.filter { it.androidOrNull("exported") == "true" && it.androidOrNull("permission") != PLUGIN_PERMISSION }
        assertTrue("exported without the plugin permission: ${unprotected.map { it.android("name") }}", unprotected.isEmpty())
        assertTrue("receivers and providers are not declared", application.children("receiver").isEmpty() && application.children("provider").isEmpty())
    }

    private fun service(name: String): Element = application.children("service").single { it.android("name") == name }

    private fun assertFilter(component: Element, action: String, category: String) {
        val filter = component.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.android("name") })
        assertEquals(listOf(category), filter.children("category").map { it.android("name") })
        assertTrue(filter.children("data").isEmpty())
    }

    private fun assertRequiresHostVersion(service: Element) {
        val metaData = service.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(mapOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION to "@string/plugin_requires_host_version"), metaData)
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val DEFAULT_CATEGORY = "android.intent.category.DEFAULT"
    }

}
