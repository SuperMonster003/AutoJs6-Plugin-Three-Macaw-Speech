package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Handler
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ArchiveExtractor
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Downloader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Importer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.LicensePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogSignatureVerifier
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelCatalogRepository
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelInstaller
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.UrlConnectionTransport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.MacawTheme
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.HostAppearance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.MacawPalette
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeController
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeEnvironment
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeScreen
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.ImportSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.ManagerBackend
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.ModelBackend
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.OperationTracker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Roadmap P5.1 (E3): the home list with and without network and with an empty catalog, the license dialog of D11
 * (cancel installs nothing, confirm installs with `acceptLicense`), and the default model after the default one is
 * deleted, on a private [ModelManager] over the JVM fixtures (no network, the plugin's own models stay untouched).
 * zh-CN: 路线图 P5.1 (E3): 有 / 无网络与空目录时的首页列表, D11 的许可对话框 (取消不安装, 确认以 `acceptLicense` 安装), 以及删除默认模型后的默认模型;
 * 使用基于 JVM 夹具的私有 [ModelManager] (不联网, 插件自身的模型不受影响).
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenDeviceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComposeTestHostActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val root = File(context.cacheDir, "home-screen-test-${UUID.randomUUID()}")

    @After
    fun cleanUp() {
        io.shutdownNow()
        root.deleteRecursively()
    }

    private class FakeEnvironment(var online: Boolean = true) : HomeEnvironment {
        override fun networkAvailable() = online
        override fun networkMetered() = false
        override var allowMeteredDownloads = false
        override fun beforeFirstDownload() = Unit
    }

    /** Records installs instead of downloading; everything else answers from [entries]. zh-CN: 记录安装而不下载. */
    private class FakeBackend(var entries: List<ModelEntry>, private val licensed: Set<String> = emptySet()) : ModelBackend {
        val installs = ArrayList<Pair<String, Boolean>>()
        override val builtinIds = setOf("vad.silero.v5")
        override fun entries() = entries
        override fun catalogVersion() = 1
        override fun downloadedBytes(modelId: String) = 0L
        override fun needsLicenseAcceptance(modelId: String) = modelId in licensed
        override fun install(modelId: String, acceptLicense: Boolean, listener: OperationListener): Int {
            synchronized(installs) { installs += modelId to acceptLicense }
            return installs.size
        }
        override fun cancel(opId: Int, discardPartial: Boolean) = Unit
        override fun discardDownload(modelId: String, onDone: () -> Unit) = onDone()
        override fun remove(modelId: String) = false
        override fun setDefault(modelId: String, kind: String) = Unit
        override fun import(source: ImportSource, request: Importer.Request, listener: OperationListener) = 0
        override fun refreshCatalog(listener: OperationListener) = 0
        override fun recycle(listener: OperationListener) = 0
    }

    private fun entry(id: String, kind: String, commercialUse: CommercialUse = CommercialUse.ALLOWED, installed: Boolean = false) = ModelEntry(
        id = id, kind = kind, modelType = "vits", name = id, languages = listOf("zh", "en"), sizeBytes = 64L shl 20, installedBytes = if (installed) 70L shl 20 else null,
        installed = installed, isDefault = installed, recommended = false, version = 1, licenseSpdx = "Apache-2.0", commercialUse = commercialUse,
        noticeKey = "speech_model_license_non_commercial", noticeUrl = null, speakers = 1, sampleRate = 22050, source = ModelStore.SOURCE_CATALOG,
    )

    private fun show(backend: ModelBackend, environment: FakeEnvironment): HomeController {
        val controller = HomeController(backend, OperationTracker { main.post(it) }, environment, { io.execute(it) }, { main.post(it) })
        controller.networkAvailable = environment.online
        compose.setContent {
            MacawTheme(MacawPalette.resolve(HostAppearance("en", dark = false, primary = -0x2153, accent = -0x2153))) {
                HomeScreen(controller, HomeActions())
            }
        }
        controller.refresh()
        compose.waitUntil(10_000) { controller.loaded }
        compose.waitForIdle()
        return controller
    }

    /** A node inside a model row, whose clickable row merges its children. zh-CN: 模型行内的节点, 可点击的行合并了其子节点. */
    private fun child(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)

    private fun awaitNode(tag: String) {
        try {
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            val roots = compose.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes()
            roots.forEach { root -> log("missing $tag; root ${root.id}: " + root.children.joinToString { it.config.toString().take(200) }) }
            compose.onAllNodes(isRoot(), useUnmergedTree = true).printToLog("MacawDeviceRun", maxDepth = Int.MAX_VALUE)
            throw e
        }
        compose.waitForIdle()
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag("macaw.home.list").performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun theListRendersTheKindGroupsOnline() {
        val backend = FakeBackend(listOf(entry("asr.a", "asr"), entry("punct.a", "punct"), entry("kws.a", "kws"), entry("tts.a", "tts", installed = true), entry("vad.silero.v5", "vad", installed = true)))
        show(backend, FakeEnvironment(online = true))
        compose.onNodeWithTag("macaw.home.status").assertIsDisplayed()
        compose.onNodeWithTag("macaw.home.offline").assertDoesNotExist()
        listOf("asr", "punct", "kws", "tts", "vad").forEach { kind -> scrollTo("macaw.home.group.$kind") }
        scrollTo("macaw.home.about.punct")
        scrollTo("macaw.model.asr.a")
        compose.onNodeWithTag("macaw.model.asr.a.download").assertIsEnabled()
        scrollTo("macaw.model.tts.a")
        child("macaw.model.tts.a.installed").assertIsDisplayed()
        child("macaw.model.tts.a.default").assertIsDisplayed()
        log("home online groups=5 rows=5")
    }

    @Test
    fun withoutNetworkTheDownloadsWaitAndTheNoticeShows() {
        val backend = FakeBackend(listOf(entry("asr.a", "asr")))
        show(backend, FakeEnvironment(online = false))
        compose.onNodeWithTag("macaw.home.offline").assertIsDisplayed()
        scrollTo("macaw.model.asr.a")
        compose.onNodeWithTag("macaw.model.asr.a.download").assertIsNotEnabled()
        scrollTo("macaw.home.import")
        compose.onNodeWithTag("macaw.home.import").assertIsEnabled()
        log("home offline notice=shown download=disabled import=enabled")
    }

    @Test
    fun anEmptyCatalogShowsTheEmptyState() {
        show(FakeBackend(emptyList()), FakeEnvironment())
        compose.onNodeWithTag("macaw.emptyState").assertIsDisplayed()
        log("home empty catalog emptyState=shown")
    }

    @Test
    fun theLicenseDialogInstallsOnlyAfterConfirming() {
        val backend = FakeBackend(listOf(entry("asr.nc", "asr", CommercialUse.NOT_ALLOWED)), licensed = setOf("asr.nc"))
        show(backend, FakeEnvironment())
        scrollTo("macaw.model.asr.nc")
        child("macaw.model.asr.nc.license").assertIsDisplayed()
        compose.onNodeWithTag("macaw.model.asr.nc.download").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("macaw.license")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("macaw.license.cancel").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("macaw.license").assertDoesNotExist()
        assertEquals("cancel installs nothing", emptyList<Pair<String, Boolean>>(), backend.installs.toList())

        compose.onNodeWithTag("macaw.model.asr.nc.download").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("macaw.license")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("macaw.license.confirm").performClick()
        compose.waitUntil(5_000) { backend.installs.isNotEmpty() }
        assertEquals("confirm installs with acceptLicense", listOf("asr.nc" to true), backend.installs.toList())
        log("home license cancel=0 installs confirm=${backend.installs}")
    }

    @Test
    fun deletingTheDefaultModelMakesTheOtherOneTheDefault() {
        val manager = privateManager()
        installFixture(manager)
        manager.setDefault("tts.test.tiny", "tts")
        val backend = ManagerBackend(context, provider = { manager }, onRemoved = {}, builtinIds = emptySet())
        val controller = show(backend, FakeEnvironment())
        scrollTo("macaw.model.tts.test.tiny")
        child("macaw.model.tts.test.tiny.default").assertIsDisplayed()
        child("macaw.model.my-voice.default").assertDoesNotExist()

        compose.onNodeWithTag("macaw.model.tts.test.tiny").performClick()
        awaitNode("macaw.details.delete")
        // The details may be taller than the dialog: bring the button into view first.
        compose.onNodeWithTag("macaw.details.delete").performScrollTo().performClick()
        // The details dialog gives way to the confirmation in a new dialog window.
        awaitNode("macaw.delete.confirm")
        compose.onNodeWithTag("macaw.delete.confirm").performClick()
        compose.waitUntil(10_000) { controller.row("tts.test.tiny")?.isDefault == false && controller.row("my-voice")?.isDefault == true }
        compose.waitForIdle()
        scrollTo("macaw.model.my-voice")
        child("macaw.model.my-voice.default").assertIsDisplayed()
        assertTrue("the catalog model is gone from the store", manager.store.installed("tts.test.tiny") == null)
        assertEquals("my-voice", manager.defaultModel("tts")?.modelId)
        log("home delete default tiny -> default=${manager.defaultModel("tts")?.modelId}")
    }

    /** A manager over the fixture catalog of the JVM tests, in a private folder. zh-CN: 基于 JVM 测试夹具目录, 位于私有文件夹的管理器. */
    private fun privateManager(): ModelManager {
        val assets = instrumentation.context.assets
        val catalog = assets.open("model-fixtures/catalog-v2.json").use { it.readBytes() }
        val publicKey = assets.open("model-fixtures/catalog-signing-public.pem").use { it.readBytes() }
        val transport = UrlConnectionTransport(userAgent = "macaw-home-screen-test")
        val store = ModelStore(File(root, "models"))
        val extractor = ArchiveExtractor()
        val license = LicensePolicy(store)
        val downloads = File(root, "downloads").apply { mkdirs() }
        return ModelManager(
            store = store,
            // The fixture key is not the pinned production key, so the verifier is built from it directly. The fixture is
            // the schema 2 catalog, read under its own file name since the APK bundles schema 3 (roadmap P8.5.2).
            catalogs = ModelCatalogRepository(File(root, "catalog"), catalog, null, fixtureVerifier(publicKey), transport, bundledFile = "catalog-v2.json"),
            installer = ModelInstaller(store, Downloader(transport), extractor, downloads, freeSpace = { Long.MAX_VALUE }),
            importer = Importer(store, extractor, license, freeSpace = { Long.MAX_VALUE }),
            licensePolicy = license,
            downloadDirectory = downloads,
        )
    }

    /** The catalog model `tts.test.tiny` from its mirror archive and a custom `my-voice`, both by import. zh-CN: 以导入方式安装两个 TTS 模型. */
    private fun installFixture(manager: ModelManager) {
        val tiny = File(root, "tiny.tar").also { file ->
            instrumentation.context.assets.open("model-fixtures/autojs6-speech-tts.test.tiny-v1-ec288c30.tar").use { input -> file.outputStream().use { input.copyTo(it) } }
        }
        await(manager) { listener -> manager.importArchive(tiny, Importer.Request(), "tiny", listener) }
        val custom = File(root, "custom.zip").also { file ->
            ZipOutputStream(file.outputStream()).use { zip ->
                listOf("voice/model.onnx" to "not a real model", "voice/tokens.txt" to "a 0\nb 1\n").forEach { (name, text) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray())
                    zip.closeEntry()
                }
            }
        }
        await(manager) { listener ->
            manager.importArchive(custom, Importer.Request(modelId = "my-voice", kind = "tts", modelType = "vits", name = "my-voice", languages = listOf("zh")), "custom", listener)
        }
    }

    private fun await(manager: ModelManager, start: (OperationListener) -> Int) {
        val done = CountDownLatch(1)
        var failure: ModelException? = null
        start(object : OperationListener {
            override fun onProgress(opId: Int, done: Long, total: Long) = Unit
            override fun onCompleted(opId: Int, result: OperationResult) = done.countDown()
            override fun onFailed(opId: Int, error: ModelException) {
                failure = error
                done.countDown()
            }
        })
        assertTrue(done.await(30, TimeUnit.SECONDS))
        failure?.let { throw it }
        assertTrue(manager.list(installed = true).isNotEmpty())
    }

    private fun fixtureVerifier(pem: ByteArray): CatalogSignatureVerifier {
        val der = CatalogSignatureVerifier.derFromPem(pem)
        return CatalogSignatureVerifier(der, CatalogSignatureVerifier.keyIdForDer(der))
    }

    private fun log(message: String) = SpeechDevice.log(message)
}
