package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.about.LegalDocument
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.about.LegalDocumentActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.about.NoticesActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutActivity
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Screenshots of the screens for the visual checks of roadmap P5.2 / P5.3 (opt-in:
 * `-e macawScreens true`, run by `.python/ui_screenshots.py`): the settings page top, synthesis and about parts plus
 * the theme color and language dialogs, the home and tryout screens, in the appearance given by the arguments
 * `macawLanguage` and `macawNight` (the font scale is the device setting the script changes). The plugin's appearance
 * preferences are restored afterwards. Screenshots go to the app's external files folder `screens/`.
 * zh-CN: 供路线图 P5.2 / P5.3 目视核对的界面截图 (需显式开启: `-e macawScreens true`, 由 `.python/ui_screenshots.py` 运行): 设置页顶部, 合成与关于部分,
 * 主题色与语言对话框, 以及首页与试听试说页; 外观取自参数 `macawLanguage` 与 `macawNight` (字号为脚本修改的设备设置). 结束后恢复插件的外观偏好.
 * 截图保存到应用外部文件目录的 `screens/`.
 */
@RunWith(AndroidJUnit4::class)
class ScreenCaptureDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var saved: AppearancePreferences
    private val prefix: String get() = SpeechDevice.argument("macawPrefix") ?: "screen"
    private val folder: File get() = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }

    @Before
    fun prepare() {
        assumeTrue("opt-in: -e macawScreens true", SpeechDevice.argument("macawScreens") == "true")
        saved = AppearancePreferences.read(context)
        val language = SpeechDevice.argument("macawLanguage") ?: AppearancePreferences.FOLLOW_SYSTEM
        val night = when (SpeechDevice.argument("macawNight")) {
            "true" -> AppearancePreferences.DARK
            "false" -> AppearancePreferences.LIGHT
            else -> AppearancePreferences.FOLLOW_SYSTEM
        }
        check(AppearancePreferences(language = language, darkMode = night, color = saved.color).save(context))
    }

    @After
    fun restore() {
        if (::saved.isInitialized) saved.save(context)
    }

    @Test
    fun settingsScreens() {
        ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)).use {
            settle()
            capture("settings-1-top")
            compose.onNodeWithTag("macaw.settings").performScrollToNode(hasTestTag("macaw.settings.voices"))
            settle()
            capture("settings-2-synthesis")
            compose.onNodeWithTag("macaw.settings").performScrollToNode(hasTestTag("macaw.settings.notices"))
            settle()
            capture("settings-3-about")
            compose.onNodeWithTag("macaw.settings").performScrollToNode(hasTestTag("macaw.settings.color"))
            compose.onNodeWithTag("macaw.settings.color").performClick()
            settle()
            capture("settings-4-theme-dialog")
            compose.onNodeWithTag("macaw.theme.cancel").performClick()
            compose.onNodeWithTag("macaw.settings").performScrollToNode(hasTestTag("macaw.settings.language"))
            compose.onNodeWithTag("macaw.settings.language").performClick()
            settle()
            capture("settings-5-language-dialog")
            compose.onNodeWithTag("macaw.dialog.language.cancel").performClick()
            settle()
        }
    }

    /** Roadmap P5.4: a manual check against the GitHub API (`-e macawNetwork true`). zh-CN: 路线图 P5.4: 对照 GitHub API 的手动检查. */
    @Test
    fun updateCheckScreen() {
        assumeTrue("opt-in: -e macawNetwork true", SpeechDevice.argument("macawNetwork") == "true")
        ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)).use {
            settle()
            compose.onNodeWithTag("macaw.settings").performScrollToNode(hasTestTag("macaw.settings.update"))
            compose.onNodeWithTag("macaw.settings.update").performClick()
            val outcomes = listOf("macaw.update.current", "macaw.update.available", "macaw.update.failed")
            compose.waitUntil(40_000) { outcomes.any { compose.onAllNodes(hasTestTag(it)).fetchSemanticsNodes().isNotEmpty() } }
            val outcome = outcomes.first { compose.onAllNodes(hasTestTag(it)).fetchSemanticsNodes().isNotEmpty() }
            settle()
            capture("update-check")
            SpeechDevice.log("update check outcome=${outcome.substringAfterLast('.')}")
            // "View release" would open the browser: a release dialog closes with "later".
            compose.onNodeWithTag(if (outcome.endsWith("available")) "$outcome.cancel" else "$outcome.confirm").performClick()
        }
    }

    @Test
    fun noticeScreens() {
        ActivityScenario.launch<NoticesActivity>(Intent(context, NoticesActivity::class.java)).use {
            settle()
            capture("notices-1-list")
        }
        ActivityScenario.launch<LegalDocumentActivity>(LegalDocumentActivity.intent(context, LegalDocument.GPL, "GNU General Public License 3.0")).use {
            settle()
            capture("notices-2-gpl")
        }
        ActivityScenario.launch<LegalDocumentActivity>(LegalDocumentActivity.intent(context, LegalDocument.NOTICES, "Third-party notices")).use {
            settle()
            compose.onNodeWithTag("macaw.legal.notices").performScrollToNode(hasText("GPL-3.0-or-later code in the speech runtime"))
            settle()
            capture("notices-3-third-party")
        }
    }

    @Test
    fun homeAndTryoutScreens() {
        ActivityScenario.launch<HomeActivity>(Intent(context, HomeActivity::class.java)).use {
            settle(1500)
            capture("home-1-top")
            compose.onNodeWithTag("macaw.home.list").performScrollToNode(hasTestTag("macaw.home.group.tts"))
            settle()
            capture("home-2-synthesis")
        }
        ActivityScenario.launch<TryoutActivity>(Intent(context, TryoutActivity::class.java)).use {
            settle(1500)
            capture("tryout-1-dictation")
            compose.onNodeWithTag("macaw.tryout.tab.2").performClick()
            settle()
            capture("tryout-3-synthesis")
        }
    }

    private fun settle(millis: Long = 800) {
        compose.waitForIdle()
        Thread.sleep(millis)
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("no screenshot")
        val file = File(folder, "$prefix-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        SpeechDevice.log("screen ${file.name} ${bitmap.width}x${bitmap.height}")
    }
}
