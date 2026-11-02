package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ArchiveExtractor
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Downloader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelInstaller
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.UrlConnectionTransport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.EspeakHome
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.EspeakLink
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechBundles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechModelKeys
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Model updates on devices (roadmap P9.3, D166, D174, D175). [updatesAModelFromItsRow] (opt-in `-e macawUpdate <modelId>`,
 * real downloads) starts from version 1, installed from the mirror it stayed on when needed, speaks with it, holds an
 * engine of it and updates from the home row with a pause in between: the running engine keeps speaking, the old files go
 * when it ends, and every synthesis of the process afterwards has its full length, which is what a deleted espeak-ng data
 * directory breaks. [removalLeavesEspeakForTheOthers] (opt-in `-e macawEspeakRemoval true`, about 36 MB) does the same for
 * the removal of the model espeak-ng started with. [theLinkMovesOnThisFileSystem] needs no model.
 * zh-CN: 设备上的模型更新 (路线图 P9.3, D166, D174, D175). [updatesAModelFromItsRow] (需显式开启 `-e macawUpdate <modelId>`, 真实下载) 从版本 1 开始, 需要时从其
 * 所在镜像安装, 用它合成, 持有一个它的引擎, 从首页行内更新并在中途暂停一次: 运行中的引擎继续合成, 旧文件在引擎结束后删除, 此后进程内的每次合成都保持完整长度,
 * 而 espeak-ng 数据目录被删除时破坏的正是这一点. [removalLeavesEspeakForTheOthers] (需显式开启 `-e macawEspeakRemoval true`, 约 36 MB) 针对删除 espeak-ng 启动时所用模型做同样的检查.
 * [theLinkMovesOnThisFileSystem] 不需要模型.
 */
@RunWith(AndroidJUnit4::class)
class ModelUpdateDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context get() = SpeechDevice.context
    private val manager: ModelManager get() = SpeechModels.get(context)
    private val runtime get() = SpeechRuntime.get(context)

    private fun forgetEspeakLink() {
        File(context.noBackupFilesDir, "espeak/espeak-ng-data").delete()
    }

    private fun samples(engine: TtsEngine): Long {
        var count = 0L
        engine.generate(SENTENCE, 0, 1f) { chunk ->
            count += chunk.size
            true
        }
        return count
    }

    private fun speak(model: InstalledModel, label: String): Long = runtime.acquireTts(model).use { lease ->
        samples(lease.engine).also { SpeechDevice.log("$label: ${model.modelId} v${model.versionCode}, $it samples") }
    }

    private fun listed(id: String): Bundle {
        @Suppress("DEPRECATION")
        val models = SpeechDevice.plugin.listModels(Bundle().apply { putString(SpeechOptionKeys.KIND, SpeechContract.KIND_TTS) }).getParcelableArrayList<Bundle>(SpeechResultKeys.MODELS)!!
        return models.single { it.getString(SpeechModelKeys.ID) == id }
    }

    private fun assertListed(id: String, version: Int, latest: Int, update: Boolean) {
        val model = listed(id)
        assertEquals(version, model.getInt(SpeechModelKeys.VERSION))
        assertEquals(latest, model.getInt(SpeechBundles.MODEL_LATEST_VERSION_CODE))
        assertEquals(update, model.getBoolean(SpeechBundles.MODEL_UPDATE_AVAILABLE))
    }

    /** Version 1 can no longer be installed through the catalog (roadmap D158). zh-CN: 版本 1 已不能经目录安装 (路线图 D158). */
    private fun installVersionOne(id: String) {
        if (manager.store.installed(id)?.versionCode == 1) return
        val snapshot = manager.catalogs.current().catalog
        val model = snapshot.model(id)!!
        val transport = UrlConnectionTransport(userAgent = "Three-Macaw-Speech device test")
        val installer = ModelInstaller(manager.store, Downloader(transport), ArchiveExtractor(), File(context.cacheDir, "downloads"), freeSpace = { it.usableSpace })
        val started = SystemClock.elapsedRealtime()
        installer.install(model, model.version(1)!!, snapshot.catalogVersion, CancelToken()) { _, _, _ -> }
        SpeechDevice.log("installed v1 of $id in ${SystemClock.elapsedRealtime() - started} ms")
    }

    private fun has(tag: String): Boolean = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun scrollToRow(id: String) {
        compose.onNodeWithTag("macaw.home.list").performScrollToNode(hasTestTag("macaw.model.$id"))
        compose.waitForIdle()
    }

    private fun click(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()

    /**
     * The questions before a download, in order: the license of the new version, then a metered network, which only an
     * emulator answers (a phone would spend the maintainer's mobile data).
     * zh-CN: 下载前的询问, 依次为新版许可与计量网络; 计量询问只在模拟器上作答 (手机上会花掉维护者的移动流量).
     */
    private fun confirmQuestions(timeoutMillis: Long = 5_000, started: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline && !started()) {
            if (has("macaw.license")) {
                click("macaw.license.confirm")
                compose.waitForIdle()
                SpeechDevice.log("confirmed the license of the new version")
            }
            if (has("macaw.metered")) {
                check(Build.HARDWARE in EMULATOR_HARDWARE) { "metered network: run this on Wi-Fi" }
                click("macaw.metered.confirm")
                compose.waitForIdle()
                SpeechDevice.log("confirmed the metered network")
            }
            SystemClock.sleep(100)
        }
    }

    @Test
    fun updatesAModelFromItsRow() {
        val id = SpeechDevice.argument("macawUpdate")
        assumeTrue("opt-in: -e macawUpdate <modelId>", id != null)
        id!!
        installVersionOne(id)
        // The partial file of a failed run would show the row as a paused update.
        File(context.cacheDir, "downloads").listFiles()?.forEach { it.delete() }
        val old = manager.store.installed(id)!!
        assertListed(id, version = 1, latest = 2, update = true)

        // espeak-ng starts on the old version when it uses espeak-ng at all.
        forgetEspeakLink()
        val espeakModel = runCatching { manager.store.installed(PIPER) }.getOrNull()
        val reference = speak(old, "before")
        val otherReference = espeakModel?.let { speak(it, "before") }
        val lease = runtime.acquireTts(old)
        val running = samples(lease.engine)

        ActivityScenario.launch<HomeActivity>(Intent(context, HomeActivity::class.java)).use {
            compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("macaw.home.group.asr")).fetchSemanticsNodes().isNotEmpty() }
            scrollToRow(id)
            compose.waitUntil(10_000) { has("macaw.model.$id.update") }
            assertTrue(has("macaw.model.$id.updateBadge"))

            click("macaw.model.$id.update")
            confirmQuestions { has("macaw.model.$id.progress") }
            compose.waitUntil(60_000) { has("macaw.model.$id.progress") }
            SpeechDevice.log("the row shows the progress of the update")
            compose.waitUntil(120_000) { manager.downloadedBytes(id) > PAUSE_AFTER_BYTES }
            click("macaw.model.$id.pause")
            compose.waitUntil(30_000) { has("macaw.model.$id.resume") }
            assertEquals("a paused update keeps the old version", 1, manager.store.installed(id)!!.versionCode)
            assertTrue(manager.store.isIntact(old))
            SpeechDevice.log("paused at ${manager.downloadedBytes(id)} bytes, v1 stays")

            click("macaw.model.$id.resume")
            val started = SystemClock.elapsedRealtime()
            // The resume asks about a metered network again unless the user chose to remember the answer.
            confirmQuestions { has("macaw.model.$id.pause") }
            compose.waitUntil(1_800_000) { manager.store.installed(id)?.versionCode == 2 }
            SpeechDevice.log("updated in ${SystemClock.elapsedRealtime() - started} ms after the resume")
            scrollToRow(id)
            compose.waitUntil(30_000) { has("macaw.model.$id.installed") }
            assertFalse(has("macaw.model.$id.updateBadge"))
        }

        val updated = manager.store.installed(id)!!
        assertEquals(2, updated.versionCode)
        assertEquals("every file matches the catalog", emptyList<String>(), manager.store.verify(updated))
        assertListed(id, version = 2, latest = 2, update = false)

        // The engine that was running keeps working on the old version, next to the new one.
        assertTrue("the old files wait for the running engine", old.directory.isDirectory)
        assertTrue("the running engine still speaks: ${samples(lease.engine)} of $running", samples(lease.engine) >= running * 8 / 10)
        val after = speak(updated, "after, old engine running")
        lease.close()
        val deadline = SystemClock.elapsedRealtime() + 60_000
        while (old.directory.exists() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        assertFalse("the old files go with the last engine", old.directory.exists())
        SpeechDevice.log("old version deleted after its engine ended")

        // A deleted espeak-ng directory shows here: about a tenth of the samples, without an error (roadmap D175).
        assertTrue("$after of $reference samples", after >= reference * 8 / 10)
        runtime.engines.trim()
        val fresh = speak(updated, "after, new engine")
        assertTrue("$fresh of $reference samples", fresh >= reference * 8 / 10)
        if (espeakModel != null) {
            val other = speak(espeakModel, "after")
            assertTrue("$other of $otherReference samples", other >= otherReference!! * 8 / 10)
        }
    }

    @Test
    fun removalLeavesEspeakForTheOthers() {
        assumeTrue("opt-in: -e macawEspeakRemoval true", SpeechDevice.argument("macawEspeakRemoval") == "true")
        val other = manager.store.installed(PIPER)
        assumeTrue("$PIPER is installed", other != null)
        val listener = RecordingOperation()
        manager.install(REMOVED, true, listener)
        val first = listener.await()
        forgetEspeakLink()
        speak(first, "before removal")
        val reference = speak(other!!, "before removal")
        runtime.engines.trim()
        assertTrue(manager.remove(REMOVED))
        assertNull(manager.store.installed(REMOVED))
        runtime.engines.trim()
        val after = speak(other, "after removal")
        assertTrue("$after of $reference samples", after >= reference * 8 / 10)
    }

    @Test
    fun theLinkMovesOnThisFileSystem() {
        val root = File(context.cacheDir, "device-test/espeak").apply { deleteRecursively() }
        fun data(model: String, text: String) = File(root, "$model/1/espeak-ng-data").also {
            it.mkdirs()
            File(it, "phontab").writeText(text)
        }
        val first = data("first", "one")
        val second = data("second", "two")
        val link = EspeakLink(File(root, "home/espeak-ng-data"))
        val home = EspeakHome(link)
        assertNull(link.target())
        val path = home.dataDir(first)
        assertEquals(link.path, path)
        assertEquals(first, link.target())
        assertEquals("one", File(path, "phontab").readText())
        assertEquals(path, home.dataDir(second))
        assertEquals("one", File(path, "phontab").readText())

        home.leave(first.parentFile!!) { listOf(second) }
        assertEquals("two", File(path, "phontab").readText())
        first.parentFile!!.deleteRecursively()
        assertEquals("two", File(path, "phontab").readText())

        home.leave(second.parentFile!!) { emptyList() }
        second.parentFile!!.deleteRecursively()
        assertFalse("the link leads nowhere", File(path, "phontab").exists())
        assertEquals(path, home.dataDir(data("third", "three")))
        assertEquals("three", File(path, "phontab").readText())
        assertEquals("no temporary links stay", listOf("espeak-ng-data"), File(root, "home").list()!!.toList())
        root.deleteRecursively()
    }

    /** Waits for an install on the model thread. zh-CN: 等待模型线程上的安装. */
    private inner class RecordingOperation : OperationListener {
        private val done = CountDownLatch(1)
        @Volatile private var failure: Exception? = null

        override fun onProgress(opId: Int, done: Long, total: Long) = Unit

        override fun onCompleted(opId: Int, result: OperationResult) = done.countDown()

        override fun onFailed(opId: Int, error: ModelException) {
            failure = error
            done.countDown()
        }

        fun await(): InstalledModel {
            assertTrue(done.await(20, TimeUnit.MINUTES))
            failure?.let { throw it }
            return manager.store.installed(REMOVED)!!
        }
    }

    private companion object {
        const val PIPER = "tts.vits.piper.en-us-lessac-medium"
        const val REMOVED = "tts.vits.piper.de-de-thorsten-medium.int8"
        const val SENTENCE = "Hello world, this is a test of the xylophone in the quizzical bazaar."
        const val PAUSE_AFTER_BYTES = 1L shl 20

        /** `Build.HARDWARE` of the Android emulator. zh-CN: Android 模拟器的 `Build.HARDWARE`. */
        val EMULATOR_HARDWARE = setOf("ranchu", "goldfish")
    }
}
