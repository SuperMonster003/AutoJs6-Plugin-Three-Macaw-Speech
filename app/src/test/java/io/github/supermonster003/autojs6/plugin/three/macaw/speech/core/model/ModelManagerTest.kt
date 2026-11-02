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
import java.util.concurrent.CountDownLatch
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
        val catalogs = ModelCatalogRepository(temporary.newFolder(), catalogBytes, null, signer.verifier(), server.transport, bundledFile = CatalogRules.CATALOG_FILE)
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
        assertEquals("the screens can tell the unpacking from the download", setOf(Fixtures.tiny.latestVersion.installBytes), listener.unpackBytes)
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
        assertEquals(9, manager.list().size)
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
        ModelManager(store, ModelCatalogRepository(temporary.newFolder(), Fixtures.catalogBytes, null, TestSigner().verifier(), server.transport, bundledFile = CatalogRules.CATALOG_FILE),
            ModelInstaller(store, Downloader(server.transport), ArchiveExtractor(), downloads, freeSpace = { freeSpace }), Importer(store, ArchiveExtractor(), LicensePolicy(store), freeSpace = { freeSpace }),
            LicensePolicy(store), downloads, executor = executor)
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        assertEquals(listOf(kept.name), downloads.list()!!.toList())
    }

    @Test
    fun `pausing keeps the partial download and cancelling with discard deletes it`() {
        val mirror = Fixtures.tiny.latestVersion.mirror!!
        server.resource(mirror.url).bytesPerChunkDelayMillis = 300
        val paused = RecordingListener()
        val pausedId = manager.install("tts.test.tiny", false, paused)
        assertTrue(server.transferStarted.await(10, TimeUnit.SECONDS))
        Thread.sleep(150)
        manager.cancel(pausedId)
        paused.await()
        assertEquals(SpeechErrorCodes.SESSION_CANCELLED, paused.error!!.code)
        assertTrue("a paused download keeps its bytes", manager.downloadedBytes("tts.test.tiny") in 1..mirror.size)

        // A queued download cancelled with discard drops its partial file once the running operation is done.
        val noncommercial = Fixtures.catalog.model("asr.test.noncommercial")!!.latestVersion.mirror!!
        server.resource(noncommercial.url).bytesPerChunkDelayMillis = 100
        val running = RecordingListener()
        manager.install("asr.test.noncommercial", true, running)
        val queued = RecordingListener()
        val queuedId = manager.install("tts.test.tiny", false, queued)
        manager.cancel(queuedId, discardPartial = true)
        assertEquals(SpeechErrorCodes.SESSION_CANCELLED, queued.await().error!!.code)
        assertNotNull(running.await().result)
        val done = java.util.concurrent.CountDownLatch(1)
        manager.discardDownload("asr.test.noncommercial") { done.countDown() }
        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertEquals("a discarded download leaves nothing", 0L, manager.downloadedBytes("tts.test.tiny"))
        assertFalse(File(downloads, "${mirror.sha256}.part").exists())
        assertNull(store.installed("tts.test.tiny"))
    }

    @Test
    fun `discarding a paused download deletes its partial files on the model thread`() {
        val mirror = Fixtures.tiny.latestVersion.mirror!!
        File(downloads, "${mirror.sha256}.part").writeText("partial")
        assertEquals(7L, manager.downloadedBytes("tts.test.tiny"))
        val done = java.util.concurrent.CountDownLatch(1)
        manager.discardDownload("tts.test.tiny") { done.countDown() }
        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertEquals(0L, manager.downloadedBytes("tts.test.tiny"))
        assertEquals("unknown ids have no download", 0L, manager.downloadedBytes("tts.unknown.model"))
    }

    @Test
    fun `the license confirmation is needed until a license is accepted`() {
        assertFalse("commercial models need no confirmation", manager.needsLicenseAcceptance("tts.test.tiny"))
        assertTrue(manager.needsLicenseAcceptance("asr.test.noncommercial"))
        assertTrue(manager.needsLicenseAcceptance("kws.test.unknown"))
        assertNotNull(install("asr.test.noncommercial", acceptLicense = true).result)
        assertFalse("the acceptance is remembered", manager.needsLicenseAcceptance("asr.test.noncommercial"))
        assertFalse("unknown ids need no confirmation", manager.needsLicenseAcceptance("asr.unknown.model"))
    }

    @Test
    fun `recycling frees unreferenced files and keeps installed models and paused downloads`() {
        assertNotNull(install("tts.test.tiny").result)
        val installedBytes = store.installed("tts.test.tiny")!!.installedBytes
        val stale = File(downloads, "${"b".repeat(64)}.part").also { it.writeText("stale bytes") }
        val paused = File(downloads, "${Fixtures.catalog.model("asr.test.noncommercial")!!.latestVersion.mirror!!.sha256}.part").also { it.writeText("resume") }
        val imports = temporary.newFolder("imports")
        File(imports, "copy.archive").writeText("import copy")
        File(store.root, ".staging/left/file.bin").apply { parentFile!!.mkdirs(); writeText("staged") }
        val recycler = ModelManager(
            store = store,
            catalogs = ModelCatalogRepository(temporary.newFolder(), Fixtures.catalogBytes, null, TestSigner().verifier(), server.transport, bundledFile = CatalogRules.CATALOG_FILE),
            installer = ModelInstaller(store, Downloader(server.transport), ArchiveExtractor(), downloads, freeSpace = { freeSpace }),
            importer = Importer(store, ArchiveExtractor(), LicensePolicy(store), freeSpace = { freeSpace }),
            licensePolicy = LicensePolicy(store),
            downloadDirectory = downloads,
            scratchDirectories = listOf(imports),
        )
        val listener = RecordingListener().also { recycler.recycle("recycle", it) }.await()
        val result = listener.result as OperationResult.Recycled
        assertFalse(stale.exists())
        assertTrue("a paused download stays", paused.exists())
        assertTrue(imports.list()!!.isEmpty())
        assertFalse(File(store.root, ".staging").exists())
        assertEquals("the installed model stays", installedBytes, store.installed("tts.test.tiny")!!.installedBytes)
        // The manager's start already recycles stale downloads, so the operation counts the import copy and the staged file.
        assertTrue("import copy and staged bytes: ${result.freedBytes}", result.freedBytes >= ("import copy".length + "staged".length).toLong())
        assertTrue(result.files >= 2)
    }

    // Updates (roadmap D166, D173, D174) ----------------------------------------------------------------------------------

    /** The fixture catalog with a version 2 of the tiny model: the same files, from a mirror URL of its own. */
    private fun catalogWithUpdate(): ByteArray {
        val json = StrictJson.parseObject(Fixtures.catalogBytes)
        @Suppress("UNCHECKED_CAST")
        val model = (json["models"] as List<MutableMap<String, Any?>>).first { it["modelId"] == TINY }
        @Suppress("UNCHECKED_CAST")
        val versions = model["versions"] as MutableList<Map<String, Any?>>
        val first = versions.single()
        @Suppress("UNCHECKED_CAST")
        val mirror = first["mirror"] as Map<String, Any?>
        versions += first + mapOf(
            "versionCode" to 2,
            "sourceUrl" to (first["sourceUrl"] as String).removeSuffix("/1") + "/2",
            "mirror" to mirror + mapOf("fileName" to (mirror["fileName"] as String).replace("-v1-", "-v2-"), "url" to (mirror["url"] as String).replace("-v1-", "-v2-")),
        )
        model["recommendedVersionCode"] = 2
        return StrictJson.write(json)
    }

    /** Version 1 is installed, then a catalog with version 2 arrives. zh-CN: 已安装版本 1, 随后到来包含版本 2 的目录. */
    private fun updateArrives(): CatalogModelVersion {
        assertNotNull(install(TINY).result)
        val bytes = catalogWithUpdate()
        val version = ModelCatalogParser().parse(bytes).model(TINY)!!.version(2)!!
        server.serve(version.mirror!!.url, Fixtures.bytes(Fixtures.tiny.latestVersion.mirror!!.fileName))
        manager = manager(bytes)
        return version
    }

    private fun versionDirectory(code: Int) = File(store.root, "$TINY/$code")

    private fun assertVersionOneStays() {
        val kept = store.installed(TINY)!!
        assertEquals(1, kept.versionCode)
        assertEquals(emptyList<String>(), store.verify(kept))
        assertFalse(versionDirectory(2).exists())
        val entry = manager.entry(TINY)!!
        assertTrue(entry.installed)
        assertEquals("the update is still offered", 2, entry.update?.version)
    }

    @Test
    fun `a newer catalog version is offered until it is installed`() {
        assertNull(manager.entry(TINY)!!.update)
        val version = updateArrives()
        val offered = manager.entry(TINY)!!
        assertEquals(1, offered.version)
        assertEquals(2, offered.latestVersion)
        assertEquals(ModelUpdate(2, version.mirror!!.size, version.license.spdx, version.license.noticeKey, version.sourceUrl), offered.update)
        assertEquals(listOf(TINY), manager.list().filter { it.update != null }.map { it.id })
        assertEquals("what is reported stays the installed version", Fixtures.tiny.version(1)!!.downloadBytes, offered.sizeBytes)

        val result = install(TINY).result as OperationResult.Model
        assertEquals(1, result.replacedVersion)
        assertEquals(2, result.entry.version)
        assertEquals(2, result.entry.latestVersion)
        assertNull(result.entry.update)
    }

    @Test
    fun `models that are not installed or are damaged offer no update`() {
        manager = manager(catalogWithUpdate())
        val available = manager.entry(TINY)!!
        assertEquals("a new install takes the latest version", 2, available.version)
        assertEquals(2, available.latestVersion)
        assertNull(available.update)
        assertEquals("the other models have one version", available.latestVersion - 1, manager.entry("kws.test.unknown")!!.latestVersion)

        manager = manager()
        updateArrives()
        store.installed(TINY)!!.resolve("tokens.txt").delete()
        val damaged = manager.entry(TINY)!!
        assertFalse(damaged.installed)
        assertNull("installing repairs it, with the latest version", damaged.update)
        assertEquals(2, (install(TINY).result as OperationResult.Model).entry.version)
    }

    @Test
    fun `a catalog that went back to an older version offers no update and installing follows it`() {
        updateArrives()
        assertNotNull(install(TINY).result)
        assertEquals(2, store.installed(TINY)!!.versionCode)
        manager = manager()
        val entry = manager.entry(TINY)!!
        assertTrue(entry.installed)
        assertEquals(1, entry.latestVersion)
        assertNull(entry.update)
        // A newer version never stays behind an older one, in use or not: the store reports the highest.
        store.isVersionInUse = { _, _ -> true }
        assertEquals(1, (install(TINY).result as OperationResult.Model).entry.version)
        assertEquals(1, store.installed(TINY)!!.versionCode)
        assertFalse(versionDirectory(2).exists())
    }

    @Test
    fun `an update replaces the old version and keeps the default`() {
        updateArrives()
        manager.setDefault(TINY, "tts")
        val replaced = ArrayList<String>()
        manager.onModelReplaced = { replaced += it }
        val deleted = ArrayList<String>()
        store.beforeDelete = { directory ->
            assertTrue("the hook runs while the directory exists", directory.isDirectory)
            deleted += directory.name
        }
        assertNotNull(install(TINY).result)
        val installed = store.installed(TINY)!!
        assertEquals(2, installed.versionCode)
        assertEquals(versionDirectory(2).canonicalPath, installed.directory.canonicalPath)
        assertEquals(emptyList<String>(), store.verify(installed))
        assertFalse("the old version is deleted", versionDirectory(1).exists())
        assertEquals(listOf("1"), deleted)
        assertEquals(listOf(TINY), replaced)
        assertEquals(TINY, manager.defaultModel("tts")!!.modelId)
        assertEquals("no archive is kept", emptyList<String>(), downloads.list()!!.toList())
        assertEquals(emptyList<String>(), File(store.root, ".staging").list()?.toList().orEmpty())

        // The same version again has nothing to replace.
        replaced.clear()
        assertNull((install(TINY).result as OperationResult.Model).replacedVersion)
        assertEquals(emptyList<String>(), replaced)
    }

    @Test
    fun `an update whose downloads fail keeps the old version`() {
        val version = updateArrives()
        server.remove(version.mirror!!.url)
        version.upstream.forEach { server.remove(it.url) }
        assertEquals(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, install(TINY).error!!.code)
        assertVersionOneStays()
    }

    @Test
    fun `an update whose bytes differ keeps the old version`() {
        val version = updateArrives()
        val mirror = version.mirror!!
        server.serve(mirror.url, Fixtures.bytes(Fixtures.tiny.latestVersion.mirror!!.fileName).also { it[600] = (it[600] + 1).toByte() })
        val upstream = version.upstream.first()
        server.serve(upstream.url, Fixtures.bytes(upstream.url.substringAfterLast('/')).also { it[50] = (it[50] + 1).toByte() })
        assertEquals(SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, install(TINY).error!!.code)
        assertVersionOneStays()
    }

    @Test
    fun `an update that does not fit keeps the old version`() {
        updateArrives()
        freeSpace = 1024
        assertTrue(install(TINY).error is InsufficientSpaceException)
        assertVersionOneStays()
    }

    @Test
    fun `an update that dies before the switch keeps the old version`() {
        updateArrives()
        store.beforeCommitRename = { throw IllegalStateException("process killed before the rename") }
        assertEquals(SpeechErrorCodes.INTERNAL, install(TINY).error!!.code)
        store.beforeCommitRename = {}
        assertVersionOneStays()
        assertEquals("the next start finds the same", 1, ModelStore(store.root).installed(TINY)!!.versionCode)
    }

    @Test
    fun `a paused update keeps the old version and resumes`() {
        val version = updateArrives()
        val mirror = version.mirror!!
        server.resource(mirror.url).bytesPerChunkDelayMillis = 300
        server.transferStarted = CountDownLatch(1)
        val paused = RecordingListener()
        val opId = manager.install(TINY, false, paused)
        assertTrue(server.transferStarted.await(10, TimeUnit.SECONDS))
        Thread.sleep(150)
        manager.cancel(opId)
        assertEquals(SpeechErrorCodes.SESSION_CANCELLED, paused.await().error!!.code)
        assertVersionOneStays()
        assertTrue("its bytes stay for a resume", manager.downloadedBytes(TINY) in 1..mirror.size)
        server.resource(mirror.url).bytesPerChunkDelayMillis = 0
        assertEquals(2, (install(TINY).result as OperationResult.Model).entry.version)
        assertEquals(0L, manager.downloadedBytes(TINY))
    }

    @Test
    fun `a version an engine runs on stays until the engine is done`() {
        updateArrives()
        var running = true
        store.isVersionInUse = { id, code -> running && id == TINY && code == 1 }
        val old = store.installed(TINY)!!
        val result = install(TINY).result as OperationResult.Model
        assertEquals("new work gets the new version at once", 2, result.entry.version)
        assertEquals(2, store.installed(TINY)!!.versionCode)
        assertEquals("the running engine keeps its files", emptyList<String>(), store.verify(old))
        store.sweep(TINY)
        assertTrue("still running", versionDirectory(1).isDirectory)
        running = false
        store.sweep(TINY)
        assertFalse(versionDirectory(1).exists())
        assertTrue(versionDirectory(2).isDirectory)
        store.sweep("tts.unknown.model")
    }

    @Test
    fun `an old version left behind is gone at the next start`() {
        updateArrives()
        store.isVersionInUse = { _, _ -> true }
        assertNotNull(install(TINY).result)
        assertTrue(versionDirectory(1).isDirectory)
        val reopened = ModelStore(store.root)
        assertFalse(versionDirectory(1).exists())
        assertEquals(2, reopened.installed(TINY)!!.versionCode)
    }

    @Test
    fun `recycling keeps a version an engine runs on`() {
        updateArrives()
        store.isVersionInUse = { _, _ -> true }
        assertNotNull(install(TINY).result)
        store.reconcile()
        assertTrue(versionDirectory(1).isDirectory)
    }

    private companion object {
        const val TINY = "tts.test.tiny"
    }
}
