package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.android
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.androidOrNull
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.child
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles.children
import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.speech.api.SpeechPluginActions
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
    fun `the permissions are exactly the plugin permission, the roadmap D29 set and the floating microphone's`() {
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
                // Roadmap D62 (P7.1): the floating microphone's overlay, its foreground service and its return after a reboot.
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
                "android.permission.RECEIVE_BOOT_COMPLETED",
            ),
            manifest.children("uses-permission").map { it.android("name") },
        )
        val permissions = manifest.children("uses-permission").map { it.android("name") }
        assertTrue("no storage permission (roadmap D23 / D29)", permissions.none { it.contains("STORAGE") || it.contains("MEDIA") })
        assertEquals("only the VOICE_COMMAND query of roadmap D50", 1, manifest.children("queries").size)
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
                VOICE_INPUT, ".ui.assistant.AssistantActivity", TRIGGER, ".service.MicrophoneStartActivity", ".service.MicrophonePermissionActivity",
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
        val aliases = application.children("activity-alias").filter { it.android("name").startsWith(".launcher.") }
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
            // Roadmap D63: every alias carries the launcher shortcut "Voice command".
            assertEquals(mapOf("android.app.shortcuts" to "@xml/shortcuts"), alias.children("meta-data").associate { it.android("name") to it.android("resource") })
        }
        val names = activities.map { it.android("name") }
        assertTrue("the real activity is declared before the aliases", names.indexOf(HOME) >= 0)
        val receiver = application.children("receiver").single { it.android("name") == ".ui.appearance.LauncherIconUpdateReceiver" }
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
    fun `the voice input sheet answers RECOGNIZE_SPEECH of any app in a translucent window`() {
        // Roadmap P5.6 (D41 - D43): exported without a permission, enabled by default, in the caller's task.
        val activity = application.children("activity").single { it.android("name") == VOICE_INPUT }
        assertEquals("true", activity.android("exported"))
        assertNull("any app may ask for voice input", activity.androidOrNull("permission"))
        assertNull("enabled by default; the settings switch changes the component state", activity.androidOrNull("enabled"))
        assertNull("the result goes back to the caller's task", activity.androidOrNull("launchMode"))
        assertNull(activity.androidOrNull("taskAffinity"))
        assertEquals("true", activity.android("excludeFromRecents"))
        assertEquals("@style/Theme.ThreeMacawSpeech.VoiceInput", activity.android("theme"))
        assertFilter(activity, action = "android.speech.action.RECOGNIZE_SPEECH", category = DEFAULT_CATEGORY)
        val theme = RepositoryFiles.xml("app/src/main/res/values/themes.xml").children("style").single { it.getAttribute("name") == "Theme.ThreeMacawSpeech.VoiceInput" }
        val items = theme.children("item").associate { it.getAttribute("name") to it.textContent.trim() }
        assertEquals("true", items["android:windowIsTranslucent"])
        assertEquals("@android:color/transparent", items["android:windowBackground"])
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
                ".service.ThreeMacawSpeechInfoService", ".service.ThreeMacawSpeechService", PACKAGED, ".service.ModelOperationService", ".service.MicrophoneService",
                ".ttsengine.ThreeMacawSpeechTtsEngineService", ASSISTANT, ASSISTANT_SESSION, RECOGNITION, FLOATING, KEY, TILE,
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
    fun `packaged speech has its own normal permission without changing the signed entry`() {
        val packaged = service(PACKAGED)
        assertEquals("true", packaged.android("exported"))
        assertEquals(SpeechPluginActions.PACKAGED_PERMISSION, packaged.android("permission"))
        assertFilter(packaged, SpeechPluginActions.PACKAGED_SPEECH, SpeechPluginActions.SERVICE_CATEGORY)
        assertRequiresHostVersion(packaged)
        val permission = manifest.children("permission").single { it.android("name") == SpeechPluginActions.PACKAGED_PERMISSION }
        assertEquals("normal", permission.android("protectionLevel"))
        assertEquals(PLUGIN_PERMISSION, service(".service.ThreeMacawSpeechService").android("permission"))
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
    fun `the digital assistant follows roadmap D48, D54 and D55`() {
        // Roadmap P5.7: bound by the system only, through BIND_VOICE_INTERACTION.
        val service = service(ASSISTANT)
        assertEquals("true", service.android("exported"))
        assertEquals(BIND_VOICE_INTERACTION, service.android("permission"))
        assertEquals("@string/app_name", service.android("label"))
        assertFilter(service, action = "android.service.voice.VoiceInteractionService", category = null)
        assertEquals(mapOf("android.voice_interaction" to "@xml/voice_interaction_service"), service.children("meta-data").associate { it.android("name") to it.android("resource") })
        val session = service(ASSISTANT_SESSION)
        assertEquals("true", session.android("exported"))
        assertEquals(BIND_VOICE_INTERACTION, session.android("permission"))
        assertTrue(session.children("intent-filter").isEmpty())

        val description = RepositoryFiles.xml("app/src/main/res/xml/voice_interaction_service.xml")
        assertEquals("voice-interaction-service", description.tagName)
        assertEquals("$PACKAGE$ASSISTANT_SESSION", description.android("sessionService"))
        assertEquals("both services are required (VoiceInteractionServiceInfo)", "$PACKAGE$RECOGNITION", description.android("recognitionService"))
        assertEquals("$PACKAGE$RECOGNITION_SETTINGS", description.android("settingsActivity"))
        // The system makes only supportsAssist services the assistant; D54 is kept by disabling the assist data in code.
        assertEquals("true", description.android("supportsAssist"))
        assertEquals("D55: the assistant starts on the lock screen", "true", description.android("supportsLaunchVoiceAssistFromKeyguard"))
        val source = RepositoryFiles.root.resolve("app/src/main/java/io/github/supermonster003/autojs6/plugin/three/macaw/speech/assistant/MacawVoiceInteractionService.kt").toFile().readText()
        assertTrue("D54: the assist data and the screenshot are disabled", "SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT" in source)
        assertEquals("disabledShowContext is set for the service and every session", 2, Regex("disabledShowContext = ").findAll(source).count())

        val sheet = application.children("activity").single { it.android("name") == ".ui.assistant.AssistantActivity" }
        assertEquals("started by the assistant session and the trigger entries only", "false", sheet.android("exported"))
        assertEquals("true", sheet.android("excludeFromRecents"))
        assertEquals("@style/Theme.ThreeMacawSpeech.VoiceInput", sheet.android("theme"))
        assertEquals("D55 for every start, not only the session's", "true", sheet.android("showWhenLocked"))
        assertEquals("D60: a task of its own, never over the plugin's other screens", "", sheet.android("taskAffinity"))
        assertTrue(sheet.children("intent-filter").isEmpty())

        // D50: the targets are visible on API 30+ through one query of their action.
        val queries = manifest.child("queries").children("intent")
        assertEquals(listOf("org.autojs.plugin.action.VOICE_COMMAND"), queries.flatMap { intent -> intent.children("action").map { it.android("name") } })
        val answered = application.children("activity").flatMap { activity -> activity.children("intent-filter").flatMap { filter -> filter.children("action").map { it.android("name") } } }
        assertTrue("the plugin never answers VOICE_COMMAND itself", "org.autojs.plugin.action.VOICE_COMMAND" !in answered)
    }

    @Test
    fun `the trigger activity is internal, windowless and shows over the lock screen`() {
        // Roadmap D60, D61, D63: the widget, the shortcut and the tile start it; it opens the sheet and finishes.
        val activity = application.children("activity").single { it.android("name") == TRIGGER }
        assertEquals("false", activity.android("exported"))
        assertNull(activity.androidOrNull("permission"))
        assertEquals("@android:style/Theme.NoDisplay", activity.android("theme"))
        assertEquals("true", activity.android("excludeFromRecents"))
        assertEquals("true", activity.android("noHistory"))
        assertEquals("", activity.android("taskAffinity"))
        assertEquals("D61: a tile tapped on the lock screen needs no unlock", "true", activity.android("showWhenLocked"))
        assertTrue(activity.children("intent-filter").isEmpty())
    }

    @Test
    fun `the floating microphone is an internal special use service resumed after a reboot`() {
        // Roadmap D62: it only keeps the overlay; the type is specialUse with its declared subtype, never microphone.
        val service = service(FLOATING)
        assertEquals("false", service.android("exported"))
        assertEquals("specialUse", service.android("foregroundServiceType"))
        assertNull(service.androidOrNull("permission"))
        assertTrue(service.children("intent-filter").isEmpty())
        val property = service.children("property").single()
        assertEquals("android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE", property.android("name"))
        assertTrue("the subtype says it never records", "never records" in property.android("value"))
        val receiver = application.children("receiver").single { it.android("name") == ".trigger.TriggerResumeReceiver" }
        assertEquals("false", receiver.android("exported"))
        assertEquals(
            listOf("android.intent.action.BOOT_COMPLETED", "android.intent.action.MY_PACKAGE_REPLACED"),
            receiver.child("intent-filter").children("action").map { it.android("name") },
        )
    }

    @Test
    fun `the key trigger is an accessibility service that filters keys and reads no screen content`() {
        // Roadmap D64 (and D54): bound by the system only, key events only, never the window content.
        val service = service(KEY)
        assertEquals("true", service.android("exported"))
        assertEquals("android.permission.BIND_ACCESSIBILITY_SERVICE", service.android("permission"))
        assertEquals("@string/key_service_label", service.android("label"))
        assertFilter(service, action = "android.accessibilityservice.AccessibilityService", category = null)
        assertEquals(mapOf("android.accessibilityservice" to "@xml/key_trigger_service"), service.children("meta-data").associate { it.android("name") to it.android("resource") })
        val config = RepositoryFiles.xml("app/src/main/res/xml/key_trigger_service.xml")
        assertEquals("accessibility-service", config.tagName)
        assertEquals("flagRequestFilterKeyEvents", config.android("accessibilityFlags"))
        assertEquals("true", config.android("canRequestFilterKeyEvents"))
        assertEquals("false", config.android("canRetrieveWindowContent"))
        assertNull("no accessibility events at all", config.androidOrNull("accessibilityEventTypes"))
        listOf("canPerformGestures", "canTakeScreenshot", "canRequestTouchExplorationMode", "canControlMagnification", "packageNames").forEach { attribute ->
            assertNull("$attribute is not requested", config.androidOrNull(attribute))
        }
        assertEquals("$PACKAGE$TRIGGER_SETTINGS", config.android("settingsActivity"))
    }

    @Test
    fun `the widget and the launcher shortcut open the sheet through the trigger activity`() {
        // Roadmap D63: the widget provider answers the system's APPWIDGET_UPDATE only and never updates on a schedule.
        val widget = application.children("receiver").single { it.android("name") == WIDGET }
        assertEquals("true", widget.android("exported"))
        assertNull(widget.androidOrNull("permission"))
        assertEquals("@string/widget_voice_label", widget.android("label"))
        assertFilter(widget, action = "android.appwidget.action.APPWIDGET_UPDATE", category = null)
        assertEquals(mapOf("android.appwidget.provider" to "@xml/voice_widget_info"), widget.children("meta-data").associate { it.android("name") to it.android("resource") })
        val info = RepositoryFiles.xml("app/src/main/res/xml/voice_widget_info.xml")
        assertEquals("appwidget-provider", info.tagName)
        assertEquals("0", info.android("updatePeriodMillis"))
        assertEquals("@layout/widget_voice", info.android("initialLayout"))
        assertNull("no configuration activity", info.androidOrNull("configure"))
        // The static shortcut names the internal trigger activity of this package and its entry name.
        val shortcuts = RepositoryFiles.xml("app/src/main/res/xml/shortcuts.xml").children("shortcut")
        assertEquals(listOf("voice_command"), shortcuts.map { it.android("shortcutId") })
        val intent = shortcuts.single().child("intent")
        assertEquals(PACKAGE, intent.android("targetPackage"))
        assertEquals("$PACKAGE$TRIGGER", intent.android("targetClass"))
        assertEquals(
            mapOf("$PACKAGE.extra.TRIGGER_SOURCE" to "shortcut"),
            intent.children("extra").associate { it.android("name") to it.android("value") },
        )
    }

    @Test
    fun `the quick settings tile is bound by the system UI only and its settings open at the triggers`() {
        // Roadmap D61: a tile service of the system UI; a long press opens the settings screen at the triggers group.
        val tile = service(TILE)
        assertEquals("true", tile.android("exported"))
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", tile.android("permission"))
        assertEquals("@string/tile_voice_label", tile.android("label"))
        assertEquals("@drawable/ic_mic_24", tile.android("icon"))
        assertNull("the tile records nothing (D58)", tile.androidOrNull("foregroundServiceType"))
        assertFilter(tile, action = "android.service.quicksettings.action.QS_TILE", category = null)
        val alias = application.children("activity-alias").single { it.android("name") == TRIGGER_SETTINGS }
        assertEquals("true", alias.android("exported"))
        assertNull(alias.androidOrNull("permission"))
        assertEquals(SETTINGS, alias.android("targetActivity"))
        assertFilter(alias, action = "android.service.quicksettings.action.QS_TILE_PREFERENCES", category = DEFAULT_CATEGORY)
    }

    @Test
    fun `the recognition service is the system's and opens the settings at recognition`() {
        // Roadmap D56: exported without a permission like the TTS engine; the service checks the caller's microphone.
        val service = service(RECOGNITION)
        assertEquals("true", service.android("exported"))
        assertNull(service.androidOrNull("permission"))
        assertNull("no foreground service type: it records for a visible caller", service.androidOrNull("foregroundServiceType"))
        assertFilter(service, action = "android.speech.RecognitionService", category = DEFAULT_CATEGORY)
        assertEquals(mapOf("android.speech" to "@xml/recognition_service"), service.children("meta-data").associate { it.android("name") to it.android("resource") })
        val description = RepositoryFiles.xml("app/src/main/res/xml/recognition_service.xml")
        assertEquals("recognition-service", description.tagName)
        assertEquals("$PACKAGE$RECOGNITION_SETTINGS", description.android("settingsActivity"))
        val alias = application.children("activity-alias").single { it.android("name") == RECOGNITION_SETTINGS }
        assertEquals("true", alias.android("exported"))
        assertEquals(SETTINGS, alias.android("targetActivity"))
        assertTrue(alias.children("intent-filter").isEmpty())
    }

    @Test
    fun `every exported component requires the plugin permission`() {
        val components = listOf("activity", "activity-alias", "service", "receiver", "provider").flatMap { application.children(it) }
        val unprotected = components.filter { it.androidOrNull("exported") == "true" && it.androidOrNull("permission") != PLUGIN_PERMISSION }
        assertEquals(
            "exported without the plugin permission: the system TTS engine with its activities (roadmap P4.2), the screen entries (P5), the voice " +
                "input (P5.6), the digital assistant bound by the system only and the recognition service (P5.7), the tile bound by the system UI " +
                "only, the key trigger bound by the accessibility system only, the widget provider (P7.1), and the separate permissioned packaged speech entry",
            SYSTEM_TTS_COMPONENTS + SCREEN_ENTRIES + VOICE_INPUT + ASSISTANT + ASSISTANT_SESSION + RECOGNITION + TILE + KEY + WIDGET + PACKAGED,
            unprotected.map { it.android("name") }.toSet(),
        )
        assertEquals(
            "the assistant services are bound by the system only",
            setOf(ASSISTANT, ASSISTANT_SESSION),
            unprotected.filter { it.androidOrNull("permission") == BIND_VOICE_INTERACTION }.map { it.android("name") }.toSet(),
        )
        assertTrue("no provider is declared", application.children("provider").isEmpty())
        assertEquals("only the widget provider is an exported receiver", listOf(WIDGET), application.children("receiver").filter { it.android("exported") == "true" }.map { it.android("name") })
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
        const val PACKAGED = ".service.ThreeMacawSpeechPackagedService"
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val DEFAULT_CATEGORY = "android.intent.category.DEFAULT"

        /** Reached by Android's TextToSpeech and the system settings, which hold no plugin permission. zh-CN: 由不持有插件权限的系统访问. */
        val SYSTEM_TTS_COMPONENTS = setOf(
            ".ttsengine.ThreeMacawSpeechTtsEngineService", ".ttsengine.CheckVoiceDataActivity", ".ttsengine.GetSampleTextActivity",
        )

        const val HOME = ".ui.home.HomeActivity"
        const val SETTINGS = ".ui.settings.SettingsActivity"

        /** Roadmap P5.6: the system voice input, reached by any app. zh-CN: 系统语音输入, 任何应用均可启动. */
        const val VOICE_INPUT = ".ui.voice.VoiceInputActivity"

        /** Roadmap P5.7: the digital assistant and the recognition service. zh-CN: 数字助理与识别服务. */
        const val PACKAGE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
        const val ASSISTANT = ".assistant.MacawVoiceInteractionService"
        const val ASSISTANT_SESSION = ".assistant.MacawVoiceInteractionSessionService"
        const val RECOGNITION = ".recognition.MacawRecognitionService"
        const val RECOGNITION_SETTINGS = ".ui.settings.RecognitionSettingsAlias"

        /** Roadmap P7.1: the launch target of the trigger entries. zh-CN: 触发式入口的启动目标. */
        const val TRIGGER = ".trigger.TriggerActivity"
        const val TILE = ".trigger.VoiceTileService"
        const val FLOATING = ".trigger.FloatingMicService"
        const val KEY = ".trigger.KeyTriggerService"
        const val WIDGET = ".trigger.VoiceWidgetProvider"
        const val TRIGGER_SETTINGS = ".ui.settings.TriggerSettingsAlias"
        const val BIND_VOICE_INTERACTION = "android.permission.BIND_VOICE_INTERACTION"

        /**
         * The screens the launcher and the system TTS settings open (roadmap P5): they show the plugin's own screens only.
         * zh-CN: 启动器与系统 TTS 设置打开的界面 (路线图 P5): 只显示插件自己的界面.
         */
        val SCREEN_ENTRIES = setOf(
            HOME, SETTINGS, ".launcher.AdaptiveLightIconAlias", ".launcher.AdaptiveDarkIconAlias", ".launcher.AdaptiveAutoIconAlias",
            ".launcher.TransparentIconAlias", RECOGNITION_SETTINGS, TRIGGER_SETTINGS,
        )
    }

}
