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
                // Roadmap P5.1: the metered network question and the offline notice of the home screen.
                "android.permission.ACCESS_NETWORK_STATE",
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
    fun `the wake activity follows the activation contract`() {
        assertEquals(
            listOf(
                ".WakeActivity", ".ui.home.HomeActivity", ".ui.settings.SettingsActivity", ".ui.tryout.TryoutActivity",
                ".ui.release.ReleaseHistoryActivity", ".ui.about.NoticesActivity", ".ui.about.LegalDocumentActivity",
                ".service.MicrophoneStartActivity", ".service.MicrophonePermissionActivity",
                ".ttsengine.CheckVoiceDataActivity", ".ttsengine.GetSampleTextActivity",
            ),
            application.children("activity").map { it.android("name") },
        )
        val wake = application.children("activity").first()
        assertEquals(".WakeActivity", wake.android("name"))
        assertEquals("true", wake.android("exported"))
        assertEquals("true", wake.android("excludeFromRecents"))
        assertEquals("true", wake.android("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.android("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.android("theme"))
        assertFilter(wake, action = "org.autojs.plugin.action.WAKE", category = DEFAULT_CATEGORY)
    }

    @Test
    fun `the microphone permission activity is protected, translucent and has no filter`() {
        val activity = application.children("activity").single { it.android("name") == ".service.MicrophonePermissionActivity" }
        assertEquals("true", activity.android("exported"))
        assertEquals(PLUGIN_PERMISSION, activity.android("permission"))
        assertEquals("true", activity.android("excludeFromRecents"))
        assertEquals("@android:style/Theme.Translucent.NoTitleBar", activity.android("theme"))
        assertTrue("opened by component name only (roadmap P3.1)", activity.children("intent-filter").isEmpty())
    }

    @Test
    fun `the microphone start activity is internal and transparent`() {
        val activity = application.children("activity").single { it.android("name") == ".service.MicrophoneStartActivity" }
        assertEquals("false", activity.android("exported"))
        assertEquals("true", activity.android("excludeFromRecents"))
        assertEquals("true", activity.android("noHistory"))
        assertEquals("", activity.android("taskAffinity"))
        assertEquals("@android:style/Theme.Translucent.NoTitleBar", activity.android("theme"))
        assertTrue(activity.children("intent-filter").isEmpty())
    }

    @Test
    fun `the microphone service is internal and of type microphone`() {
        val service = service(".service.MicrophoneService")
        assertEquals("false", service.android("exported"))
        assertEquals("microphone", service.android("foregroundServiceType"))
        assertNull(service.androidOrNull("permission"))
        assertTrue("started by the plugin only", service.children("intent-filter").isEmpty())
    }

    @Test
    fun `the launcher entry is one of four aliases of the home screen`() {
        val activities = application.children("activity")
        val home = activities.single { it.android("name") == HOME }
        assertTrue("the real activity has no launcher filter", home.children("intent-filter").none { filter ->
            filter.children("category").any { it.android("name") == "android.intent.category.LAUNCHER" }
        })
        // Icon specification section 4: four fixed aliases after the real activity, only the automatic one enabled.
        val aliases = application.children("activity-alias")
        assertEquals(
            listOf(
                ".launcher.AdaptiveLightIconAlias" to "@mipmap/ic_launcher_system_light",
                ".launcher.AdaptiveDarkIconAlias" to "@mipmap/ic_launcher_system",
                ".launcher.AdaptiveAutoIconAlias" to "@mipmap/ic_launcher_system_auto",
                ".launcher.TransparentIconAlias" to "@mipmap/ic_launcher",
            ),
            aliases.map { it.android("name") to it.android("icon") },
        )
        aliases.forEach { alias ->
            assertEquals(HOME, alias.android("targetActivity"))
            assertEquals(alias.android("icon"), alias.android("roundIcon"))
            assertEquals("true", alias.android("exported"))
            assertEquals(if (alias.android("name") == ".launcher.AdaptiveAutoIconAlias") "true" else "false", alias.android("enabled"))
            assertFilter(alias, action = "android.intent.action.MAIN", category = "android.intent.category.LAUNCHER")
        }
        val names = activities.map { it.android("name") }
        assertTrue("the real activity is declared before the aliases", names.indexOf(HOME) >= 0)
        val receiver = application.children("receiver").single()
        assertEquals(".ui.appearance.LauncherIconUpdateReceiver", receiver.android("name"))
        assertEquals("false", receiver.android("exported"))
        assertFilter(receiver, action = "android.intent.action.MY_PACKAGE_REPLACED", category = null)
    }

    @Test
    fun `the plugin screens other than the two system entries are internal`() {
        listOf(".ui.tryout.TryoutActivity", ".ui.release.ReleaseHistoryActivity", ".ui.about.NoticesActivity", ".ui.about.LegalDocumentActivity").forEach { name ->
            val activity = application.children("activity").single { it.android("name") == name }
            assertEquals("$name is internal", "false", activity.android("exported"))
            assertTrue(activity.children("intent-filter").isEmpty())
        }
        assertEquals("@style/Theme.ThreeMacawSpeech", application.android("theme"))
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
            listOf(
                ".service.ThreeMacawSpeechInfoService", ".service.ThreeMacawSpeechService", ".service.ModelOperationService", ".service.MicrophoneService",
                ".ttsengine.ThreeMacawSpeechTtsEngineService",
            ),
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
    fun `the system TTS engine follows appendix C2`() {
        val engine = service(".ttsengine.ThreeMacawSpeechTtsEngineService")
        assertEquals("true", engine.android("exported"))
        assertNull("Android's TextToSpeech binds it from the client app", engine.androidOrNull("permission"))
        assertEquals("@string/app_name", engine.android("label"))
        assertNull(engine.androidOrNull("process"))
        assertFilter(engine, action = "android.intent.action.TTS_SERVICE", category = DEFAULT_CATEGORY)
        assertEquals(mapOf("android.speech.tts" to "@xml/tts_engine"), engine.children("meta-data").associate { it.android("name") to it.android("resource") })
        val description = RepositoryFiles.xml("app/src/main/res/xml/tts_engine.xml")
        assertEquals("tts-engine", description.tagName)
        assertEquals(
            "the settings entry is the settings screen of roadmap P5.2, named in full",
            "io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.settings.SettingsActivity",
            description.android("settingsActivity"),
        )
        mapOf(
            ".ttsengine.CheckVoiceDataActivity" to "android.speech.tts.engine.CHECK_TTS_DATA",
            ".ttsengine.GetSampleTextActivity" to "android.speech.tts.engine.GET_SAMPLE_TEXT",
        ).forEach { (name, action) ->
            val activity = application.children("activity").single { it.android("name") == name }
            assertEquals("true", activity.android("exported"))
            assertNull("the system settings start $name", activity.androidOrNull("permission"))
            assertEquals("true", activity.android("excludeFromRecents"))
            assertFilter(activity, action = action, category = DEFAULT_CATEGORY)
        }
        // INSTALL_TTS_DATA opens the home screen at the synthesis models; the settings screen has no filter.
        val home = application.children("activity").single { it.android("name") == HOME }
        assertEquals("true", home.android("exported"))
        assertNull(home.androidOrNull("permission"))
        assertFilter(home, action = "android.speech.tts.engine.INSTALL_TTS_DATA", category = DEFAULT_CATEGORY)
        val settings = application.children("activity").single { it.android("name") == SETTINGS }
        assertEquals("true", settings.android("exported"))
        assertNull(settings.androidOrNull("permission"))
        assertTrue(settings.children("intent-filter").isEmpty())
    }

    @Test
    fun `every exported component requires the plugin permission`() {
        val components = listOf("activity", "activity-alias", "service", "receiver", "provider").flatMap { application.children(it) }
        val unprotected = components.filter { it.androidOrNull("exported") == "true" && it.androidOrNull("permission") != PLUGIN_PERMISSION }
        assertEquals(
            "exported without the plugin permission: the system TTS engine with its activities (roadmap P4.2) and the screen entries (P5)",
            SYSTEM_TTS_COMPONENTS + SCREEN_ENTRIES,
            unprotected.map { it.android("name") }.toSet(),
        )
        assertTrue("no provider is declared", application.children("provider").isEmpty())
        assertTrue("the receiver is internal", application.children("receiver").all { it.android("exported") == "false" })
    }

    private fun service(name: String): Element = application.children("service").single { it.android("name") == name }

    private fun assertFilter(component: Element, action: String, category: String?) {
        val filter = component.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.android("name") })
        assertEquals(listOfNotNull(category), filter.children("category").map { it.android("name") })
        assertTrue(filter.children("data").isEmpty())
    }

    private fun assertRequiresHostVersion(service: Element) {
        val metaData = service.children("meta-data").associate { it.android("name") to it.android("value") }
        assertEquals(mapOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION to "@string/plugin_requires_host_version"), metaData)
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val DEFAULT_CATEGORY = "android.intent.category.DEFAULT"

        /** Reached by Android's TextToSpeech and the system settings, which hold no plugin permission. zh-CN: 由不持有插件权限的系统访问. */
        val SYSTEM_TTS_COMPONENTS = setOf(
            ".ttsengine.ThreeMacawSpeechTtsEngineService", ".ttsengine.CheckVoiceDataActivity", ".ttsengine.GetSampleTextActivity",
        )

        const val HOME = ".ui.home.HomeActivity"
        const val SETTINGS = ".ui.settings.SettingsActivity"

        /**
         * The screens the launcher and the system TTS settings open (roadmap P5): they show the plugin's own screens only.
         * zh-CN: 启动器与系统 TTS 设置打开的界面 (路线图 P5): 只显示插件自己的界面.
         */
        val SCREEN_ENTRIES = setOf(
            HOME, SETTINGS, ".launcher.AdaptiveLightIconAlias", ".launcher.AdaptiveDarkIconAlias", ".launcher.AdaptiveAutoIconAlias",
            ".launcher.TransparentIconAlias",
        )
    }

}
