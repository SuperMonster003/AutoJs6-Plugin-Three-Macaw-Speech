package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.content.Intent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A real download from the home screen (roadmap P5.1, opt-in `-e macawDownload <modelId>`): the row's download button
 * starts it, the row shows the progress, and the installed mark replaces it. Commercial-use models on an unmetered
 * network only, so no confirmation dialog appears.
 * zh-CN: 从首页真实下载 (路线图 P5.1, 需显式开启 `-e macawDownload <modelId>`): 由行内下载按钮开始, 行内显示进度, 完成后显示已安装标记. 只用于可商用模型且
 * 在非计量网络上, 因此不会出现确认对话框.
 */
@RunWith(AndroidJUnit4::class)
class HomeDownloadDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val modelId: String? get() = SpeechDevice.argument("macawDownload")

    @Before
    fun optIn() {
        assumeTrue("opt-in: -e macawDownload <modelId>", modelId != null)
    }

    @Test
    fun downloadsAModelFromItsRow() {
        val id = modelId!!
        val manager = SpeechModels.get(context)
        assumeTrue("$id is not installed yet", manager.store.installed(id) == null)
        ActivityScenario.launch<HomeActivity>(Intent(context, HomeActivity::class.java)).use {
            // The list composes only the rows in view: wait for the groups, then scroll the row in.
            compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("macaw.home.group.asr")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("macaw.home.list").performScrollToNode(hasTestTag("macaw.model.$id"))
            val started = System.currentTimeMillis()
            compose.onNodeWithTag("macaw.model.$id.download", useUnmergedTree = true).performClick()
            compose.waitUntil(30_000) { compose.onAllNodes(hasTestTag("macaw.model.$id.progress"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            SpeechDevice.log("home download $id progress shown after ${System.currentTimeMillis() - started} ms")
            // Installed rows sort first in their group (ModelRows) while the list keeps its first visible row, so on a small
            // screen the finished row can move above the viewport and is no longer composed (Redmi 720 x 1650): wait for the
            // store, then scroll the row back in before looking for its installed mark.
            compose.waitUntil(900_000) { manager.store.installed(id) != null }
            compose.onNodeWithTag("macaw.home.list").performScrollToNode(hasTestTag("macaw.model.$id"))
            compose.waitUntil(30_000) { compose.onAllNodes(hasTestTag("macaw.model.$id.installed"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            val seconds = (System.currentTimeMillis() - started) / 1000.0
            val entry = manager.entry(id)!!
            SpeechDevice.log("home download $id installed in ${"%.1f".format(seconds)} s, ${entry.installedBytes} bytes, default=${entry.isDefault}")
            assertTrue(entry.installed)
        }
    }
}
