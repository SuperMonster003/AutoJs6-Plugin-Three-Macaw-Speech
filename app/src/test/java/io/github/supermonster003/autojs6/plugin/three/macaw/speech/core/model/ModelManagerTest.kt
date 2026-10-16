package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Installing, importing and managing models end to end against the local server (roadmap P2.3): the mirror path, the
 * fallback to upstream when the mirror is missing or corrupt, upstream-only versions, free space, cancellation and
 * resumption, one download at a time, license acceptance (D11), defaults, removal, damaged installs, atomic
 * replacement with recovery, and imports by catalog content or with an explicit identity.
 * zh-CN: 对照本地服务器端到端地安装, 导入与管理模型 (路线图 P2.3): 镜像路径, 镜像缺失或损坏时回退上游, 仅上游版本, 可用空间, 取消与续传,
 * 同时只跑一个下载, 许可接受 (D11), 默认模型, 删除, 损坏的安装, 原子替换与恢复, 以及按目录内容或显式身份导入.
 */
class ModelManagerTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private val server = FakeGitHub()
    private var freeSpace = Long.MAX_VALUE
    private lateinit var store: ModelStore
    private lateinit var downloads: File
    private lateinit var manager: ModelManager
    private val logs = ArrayList<String>()

    @Before
    fun start() {
        server.serveCatalogAssets(Fixtures.catalog)
        store = ModelStore(temporary.newFolder("models"))
        downloads = temporary.newFolder("downloads")
        manager = manager()
    }

    @After
    fun stop() = server.close()

    private fun manager(catalogBytes: ByteArray = Fixtures.catalogBytes): ModelManager {
        val signer = TestSigner()
        val catalogs = ModelCatalogRepository(temporary.newFolder(), catalogBytes, null, signer.verifier(), server.transport)
        val extractor = ArchiveExtractor()
        val license = LicensePolicy(store)
        return ModelManager(
            store = store,
            catalogs = catalogs,
            installer = ModelInstaller(store, Downloader(server.transport, sleep = {}), extractor, downloads, freeSpace = { freeSpace }, log = { synchronized(logs) { logs += it } }),
            importer = Importer(store, extractor, license, freeSpace = { freeSpace }),
            licensePolicy = license,
            downloadDirectory = downloads,
            locale = { Locale.US },
        )
    }

    private fun install(id: String, acceptLicense: Boolean = false): RecordingListener =
        RecordingListener().also { manager.install(id, acceptLicense, it) }.await()

    @Test
    fun `the mirror installs a verified model with its manifest`() {
        val listener = install("tts.test.tiny")
        val result = listener.result as OperationResult.Model
        assertEquals("mirror", result.origin)
        assertNull(result.mirrorFailure)
        assertTrue(result.entry.installed)
        assertTrue("the first installed tts model is the default", result.entry.isDefault)
        val installed = store.installed("tts.test.tiny")!!
        assertEquals(emptyList<String>(), store.verify(installed))
        assertTrue(File(installed.directory, ModelStore.MANIFEST).isFile)
        assertEquals(File(store.root, "tts.test.tiny/1").canonicalPath, installed.directory.canonicalPath)
        assertEquals("matcha", installed.modelType)
        assertTrue(installed.resolve("model-steps-3.onnx").isFile)
        val (done, total) = listener.progress.last()
        assertEquals(total, done)
        assertEquals(Fixtures.tiny.latestVersion.mirror!!.size + Fixtures.tiny.latestVersion.installBytes, total)
        assertEquals("no archive is kept after the install", emptyList<String>(), downloads.list()!!.toList())
        assertEquals(0, server.requests.count { it.first.contains("sherpa-onnx") })
    }

    @Test
    fun `a missing mirror falls back to the upstream assets`() {
        server.remove(Fixtures.tiny.latestVersion.mirror!!.url)
        val result = install("tts.test.tiny").result as OperationResult.Model
        assertEquals("upstream", result.origin)
        assertTrue(result.mirrorFailure!!.contains("404"))
        assertEquals(emptyList<String>(), store.verify(store.installed("tts.test.tiny")!!))
        assertEquals("upstream", store.installed("tts.test.tiny")!!.origin)
        assertTrue(logs.any { it.contains("falling back") })
    }

    @Test
    fun `a corrupt mirror falls back to the upstream assets`() {
        val mirror = Fixtures.tiny.latestVersion.mirror!!
        server.serve(mirror.url, Fixtures.bytes(mirror.fileName).also { it[600] = (it[600] + 1).toByte() })
        val result = install("tts.test.tiny").result as OperationResult.Model
        assertEquals("upstream", result.origin)
        assertFalse("the corrupt download is deleted", File(downloads, "${mirror.sha256}.part").exists())
    }

    @Test
    fun `an upstream-only version installs from upstream without asking the mirror`() {
        val text = Fixtures.catalogText
        val mirrorJson = Regex("\"mirror\": \\{[^}]*\"url\": \"${Regex.escape(Fixtures.tiny.latestVersion.mirror!!.url)}\"\\s*}").find(text)!!.value
        manager = manager(text.replace(mirrorJson, "\"mirror\": null").toByteArray())
        val result = install("tts.test.tiny").result as OperationResult.Model
        assertEquals("upstream", result.origin)
        assertNull(result.mirrorFailure)
        assertEquals(0, server.requestCount(Fixtures.tiny.latestVersion.mirror!!.url))
    }

    @Test
    fun `when every source fails the error says so and nothing is installed`() {
        val version = Fixtures.tiny.latestVersion
        server.remove(version.mirror!!.url)
        server.remove(version.upstream.first().url)
        val error = install("tts.test.tiny").error!!
        assertEquals(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, error.code)
        assertNull(store.installed("tts.test.tiny"))
        val corrupt = version.upstream.first()
        server.serve(corrupt.url, Fixtures.bytes("tiny-model.tar.bz2").also { it[50] = (it[50] + 1).toByte() })
        assertEquals(SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, install("tts.test.tiny").error!!.code)
    }

    @Test
    fun `missing free space is reported before any download`() {
        freeSpace = 1024
        val error = install("tts.test.tiny").error!!
        assertTrue(error is InsufficientSpaceException)
        assertEquals(SpeechErrorCodes.IO_FAILED, error.code)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun `licenses that are not commercial need an acceptance that is remembered`() {
        val refused = install("asr.test.noncommercial").error!!
        assertEquals(SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED, refused.code)
        assertTrue(server.requests.isEmpty())
        assertEquals(SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED, install("kws.test.unknown").error!!.code)
        assertNotNull(install("asr.test.noncommercial", acceptLicense = true).result)
        assertTrue(manager.remove("asr.test.noncommercial"))
        assertNotNull("the remembered acceptance covers a reinstall", install("asr.test.noncommercial").result)
    }

    @Test
    fun `cancellation fails the operation and a later install resumes the partial download`() {
        val mirror = Fixtures.tiny.latestVersion.mirror!!
        server.resource(mirror.url).bytesPerChunkDelayMillis = 300
        val listener = RecordingListener()
        val opId = manager.install("tts.test.tiny", false, listener)
        assertTrue(server.transferStarted.await(10, TimeUnit.SECONDS))
        Thread.sleep(150)
        manager.cancel(opId)
        listener.await()
        assertEquals(SpeechErrorCodes.SESSION_CANCELLED, listener.error!!.code)
        assertNull(store.installed("tts.test.tiny"))
        // The bytes received before the cancellation stay for a resume (the local server may deliver this small archive
        // in one piece, so the partial file can already be complete).
        val partial = File(downloads, "${mirror.sha256}.part")
        assertTrue("partial ${partial.length()} of ${mirror.size}", partial.length() in 1..mirror.size)
        server.resource(mirror.url).bytesPerChunkDelayMillis = 0
        val before = server.requests.size
        assertNotNull(install("tts.test.tiny").result)
        val again = server.requests.drop(before).filter { it.first.endsWith(mirror.fileName) }
        assertTrue("never restarted from the first byte: $again", again.all { it.second?.startsWith("bytes=") == true })
    }

    @Test
    fun `operations run one at a time in order and unstarted ones can be cancelled`() {
        Fixtures.catalog.models.forEach { model -> model.versions.forEach { version -> server.resource(version.mirror!!.url).bytesPerChunkDelayMillis = 20 } }
        val first = RecordingListener()
        val second = RecordingListener()
        val third = RecordingListener()
        manager.install("tts.test.tiny", false, first)
        manager.install("asr.test.noncommercial", true, second)
        val thirdId = manager.install("kws.test.unknown", true, third)
        manager.cancel(thirdId)
        assertEquals(SpeechErrorCodes.SESSION_CANCELLED, third.await().error!!.code)
        first.await()
        second.await()
        assertEquals(1, server.maxConcurrent.get())
        assertNull(store.installed("kws.test.unknown"))
        assertNotNull(store.installed("asr.test.noncommercial"))
    }

    @Test
    fun `list, defaults and removal follow the store`() {
        assertEquals(3, manager.list().size)
        assertEquals(listOf("tts.test.tiny"), manager.list(kind = "tts").map { it.id })
        assertEquals(emptyList<String>(), manager.list(installed = true).map { it.id })
        install("tts.test.tiny")
        install("kws.test.unknown", acceptLicense = true)
        val entry = manager.entry("tts.test.tiny")!!
        assertTrue(entry.installed && entry.isDefault)
        assertEquals(Fixtures.tiny.latestVersion.mirror!!.size, entry.sizeBytes)
        assertEquals(Fixtures.tiny.latestVersion.installBytes, entry.installedBytes)
        assertEquals("Test tts.test.tiny", entry.name)
        assertEquals(setOf("tts.test.tiny", "kws.test.unknown"), manager.list(installed = true).map { it.id }.toSet())
        assertThrows(ModelException::class.java) { manager.setDefault("kws.test.unknown", "tts") }
        assertEquals("kws.test.unknown", manager.setDefault("kws.test.unknown", "kws").id)
        assertEquals("kws.test.unknown", manager.defaultModel("kws")!!.modelId)
        assertEquals(SpeechErrorCodes.MODEL_MISSING, assertThrows(ModelException::class.java) { manager.setDefault("asr.test.noncommercial", null) }.code)
        assertTrue(manager.remove("tts.test.tiny"))
        assertFalse(manager.remove("tts.test.tiny"))
        assertNull(manager.defaultModel("tts"))
        assertEquals(SpeechErrorCodes.MODEL_MISSING, assertThrows(ModelException::class.java) { manager.remove("tts.unknown.model") }.code)
        assertFalse(File(store.root, "tts.test.tiny").exists())
    }

    @Test
    fun `a damaged install is reported as not installed and a reinstall repairs it`() {
        install("tts.test.tiny")
        val installed = store.installed("tts.test.tiny")!!
        installed.resolve("tokens.txt").delete()
        assertFalse(manager.entry("tts.test.tiny")!!.installed)
        val before = server.requests.size
        install("tts.test.tiny")
        assertTrue(server.requests.size > before)
        assertEquals(emptyList<String>(), store.verify(store.installed("tts.test.tiny")!!))
    }

    @Test
    fun `an interrupted replacement is rolled back to the previous copy`() {
        install("tts.test.tiny")
        store.installed("tts.test.tiny")!!.resolve("tokens.txt").appendText("damage")
        store.beforeCommitRename = { throw IllegalStateException("process killed between the renames") }
        assertEquals(SpeechErrorCodes.INTERNAL, install("tts.test.tiny").error!!.code)
        store.beforeCommitRename = {}
        // The next start reconciles: the displaced copy returns, staging leftovers disappear.
        val reopened = ModelStore(store.root)
        val restored = reopened.installed("tts.test.tiny")!!
        assertEquals(listOf("tokens.txt"), reopened.verify(restored))
        assertEquals(emptyList<String>(), File(store.root, ".staging").list()?.toList().orEmpty())
        assertEquals(emptyList<String>(), File(store.root, ".trash").list()?.toList().orEmpty())
    }

    @Test
    fun `imports are recognized by content as catalog models`() {
        val version = Fixtures.tiny.latestVersion
        val mirrorCopy = temporary.newFile("mirror.tar").also { Fixtures.file(version.mirror!!.fileName).copyTo(it, overwrite = true) }
        val listener = RecordingListener()
        manager.importArchive(mirrorCopy, Importer.Request(), "import", listener)
        val entry = (listener.await().result as OperationResult.Model).entry
        assertEquals("tts.test.tiny", entry.id)
        assertEquals("import", store.installed("tts.test.tiny")!!.origin)
        assertFalse("the private copy is deleted", mirrorCopy.exists())
        assertTrue(manager.remove("tts.test.tiny"))
        // An upstream tar.bz2 with extra members still matches its catalog model by content, after the license rule.
        val upstreamCopy = temporary.newFile("up.tar.bz2").also { Fixtures.file("noncommercial.tar.bz2").copyTo(it, overwrite = true) }
        val license = RecordingListener()
        manager.importArchive(upstreamCopy, Importer.Request(), "import", license)
        assertEquals(SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED, license.await().error!!.code)
        val accepted = RecordingListener()
        val again = temporary.newFile("up2.tar.bz2").also { Fixtures.file("noncommercial.tar.bz2").copyTo(it, overwrite = true) }
        manager.importArchive(again, Importer.Request(acceptLicense = true), "import", accepted)
        assertEquals("asr.test.noncommercial", (accepted.await().result as OperationResult.Model).entry.id)
        assertEquals(emptyList<String>(), store.verify(store.installed("asr.test.noncommercial")!!))
    }

    @Test
    fun `imports outside the catalog need an identity and keep safe paths`() {
        val zip = temporary.newFile("voice.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            listOf("my-voice/model.onnx" to "weights", "my-voice/tokens.txt" to "a 1\n", "my-voice/espeak-ng-data/phontab" to "x").forEach { (name, text) ->
                out.putNextEntry(ZipEntry(name))
                out.write(text.toByteArray())
                out.closeEntry()
            }
        }
        val anonymous = RecordingListener()
        manager.importArchive(zip.copyTo(temporary.newFile(), overwrite = true), Importer.Request(), "import", anonymous)
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, anonymous.await().error!!.code)
        assertThrows(ModelException::class.java) {
            Importer(store, ArchiveExtractor(), LicensePolicy(store), freeSpace = { freeSpace }).importArchive(zip, Importer.Request(modelId = "tts.looks-like-catalog", kind = "tts", modelType = "vits", name = "x", languages = listOf("zh")), Fixtures.catalog, CancelToken())
        }
        val listener = RecordingListener()
        val request = Importer.Request(modelId = "my-voice", kind = "tts", modelType = "vits", name = "My voice", languages = listOf("zh"))
        manager.importArchive(zip.copyTo(temporary.newFile(), overwrite = true), request, "import", listener)
        val entry = (listener.await().result as OperationResult.Model).entry
        assertEquals("imported", entry.source)
        assertEquals("My voice", entry.name)
        val installed = store.installed("my-voice")!!
        assertEquals(listOf("espeak-ng-data/phontab", "model.onnx", "tokens.txt"), installed.files.map { it.path })
        assertEquals(Importer.IMPORTED_VERSION, installed.versionCode)
        assertTrue(entry.isDefault)

        val hostile = temporary.newFile("hostile.zip")
        ZipOutputStream(hostile.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("../../escape.txt"))
            out.write(1)
            out.closeEntry()
        }
        val refused = RecordingListener()
        manager.importArchive(hostile, request.copy(modelId = "other-voice"), "import", refused)
        assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, refused.await().error!!.code)
        assertFalse(File(temporary.root, "escape.txt").exists())
        assertNull(store.installed("other-voice"))
    }

    @Test
    fun `a directory tree import matches the catalog like an archive`() {
        val version = Fixtures.tiny.latestVersion
        val tree = temporary.newFolder("tree")
        ArchiveExtractor().extractMirror(Fixtures.file(version.mirror!!.fileName), version.files, tree, CancelToken())
        val listener = RecordingListener()
        manager.importTree(FileImportTree(tree), Importer.Request(), "import", listener)
        assertEquals("tts.test.tiny", (listener.await().result as OperationResult.Model).entry.id)
    }

    @Test
    fun `stale downloads are recycled when the manager starts`() {
        File(downloads, "${"a".repeat(64)}.part").writeText("stale")
        val kept = File(downloads, "${Fixtures.tiny.latestVersion.mirror!!.sha256}.part").also { it.writeText("resume me") }
        val executor = Executors.newSingleThreadExecutor()
        ModelManager(store, ModelCatalogRepository(temporary.newFolder(), Fixtures.catalogBytes, null, TestSigner().verifier(), server.transport),
            ModelInstaller(store, Downloader(server.transport), ArchiveExtractor(), downloads, freeSpace = { freeSpace }), Importer(store, ArchiveExtractor(), LicensePolicy(store), freeSpace = { freeSpace }),
            LicensePolicy(store), downloads, executor = executor)
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        assertEquals(listOf(kept.name), downloads.list()!!.toList())
    }
}
