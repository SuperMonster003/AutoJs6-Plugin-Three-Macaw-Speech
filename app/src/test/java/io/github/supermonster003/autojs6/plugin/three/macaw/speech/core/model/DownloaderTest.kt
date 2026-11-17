package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The resumable downloader against a local HTTP server (roadmap P2.3; on the shared download kit since plugin-center
 * P13.2): full transfers with progress, resumption with `Range` after a dropped connection and after a killed process,
 * servers that ignore `Range`, missing resources, corrupt and oversized bodies, cancellation, the production network
 * policy; then the kit's behaviour through the plugin's transport: `If-Range` with the server's validator, another URL of
 * the same file continuing the bytes on disk, a resumed file failing its SHA-256, automatic retries of server errors,
 * one download for concurrent fetches, the files of the downloader before 1.6.1 (Q51 A) and the leftovers to recycle.
 * zh-CN: 可续传下载器对照本地 HTTP 服务器 (路线图 P2.3; 自 plugin-center P13.2 起基于共用下载库): 带进度的完整传输, 连接中断与进程被杀后以 `Range` 续传,
 * 忽略 Range 的服务器, 缺失的资源, 损坏与超长的响应体, 取消, 正式网络策略; 以及下载库经插件传输的行为: 带服务器验证头的 `If-Range`, 同一文件的另一个 URL
 * 接着磁盘上的字节继续, 续传得到的文件未通过 SHA-256, 服务器错误的自动重试, 并发获取共享一次下载, 1.6.1 之前的下载器留下的文件 (Q51 A), 以及待回收的残留.
 */
class DownloaderTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private val server = FakeGitHub()
    private val content = ByteArray(300_000) { (it * 31 + it / 7).toByte() }
    private val sha256 = MessageDigest.getInstance("SHA-256").digest(content).toHex()
    private val artifact = pinned("https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/test.tar.bz2")
    private val downloader = Downloader(server.transport, retryDelaysMillis = longArrayOf(0, 0))

    /** A downloader of another process that does not retry. zh-CN: 另一个进程中不重试的下载器. */
    private fun withoutRetries() = Downloader(server.transport, retryDelaysMillis = LongArray(0))

    private fun pinned(url: String, size: Long = content.size.toLong(), sha256: String = this.sha256): PinnedArtifact = object : PinnedArtifact {
        override val url = url
        override val size = size
        override val sha256 = sha256
    }

    @After
    fun stop() = server.close()

    @Test
    fun `a full download verifies and reports monotonic progress`() {
        server.serve(artifact.url, content)
        val progress = ArrayList<Long>()
        val file = downloader.fetch(artifact, temporary.root, CancelToken()) { progress += it }
        assertEquals(content.toList(), file.readBytes().toList())
        assertEquals(downloader.completeFile(artifact, temporary.root), file)
        assertEquals(File(downloader.storeDirectory(temporary.root), artifact.sha256), file.parentFile)
        assertEquals(artifact.size, progress.last())
        assertEquals(progress.sorted(), progress)
        assertEquals(0L, downloader.remainingBytes(artifact, temporary.root))
        // A verified copy is never fetched again.
        downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(1, server.requestCount(artifact.url))
    }

    @Test
    fun `a dropped connection resumes with a Range request`() {
        server.serve(artifact.url, content).apply { dropAfter = 100_000; dropTimes = 1 }
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        val ranges = server.requests.map { it.second }
        assertEquals(null, ranges.first())
        assertTrue("resumed from the received prefix: $ranges", ranges.last()!!.startsWith("bytes=") && ranges.last() != "bytes=0-")
    }

    @Test
    fun `a partial download left by a killed process is resumed`() {
        server.serve(artifact.url, content).apply { dropAfter = 123_456; dropTimes = 1 }
        val error = assertThrows(ModelException::class.java) { withoutRetries().fetch(artifact, temporary.root, CancelToken()) }
        assertEquals(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, error.code)
        assertEquals(123_456L, downloader.partialFile(artifact, temporary.root).length())
        assertEquals(artifact.size - 123_456, downloader.remainingBytes(artifact, temporary.root))
        // The next process continues the bytes on disk.
        val file = withoutRetries().fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        assertEquals(listOf(null, "bytes=123456-"), server.requests.map { it.second })
    }

    @Test
    fun `a server that ignores Range restarts the file`() {
        server.serve(artifact.url, content).ignoreRange = true
        downloader.legacyPartialFile(artifact.sha256, temporary.root).writeBytes(content.copyOf(5000))
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        assertEquals(listOf("bytes=5000-"), server.requests.map { it.second })
    }

    @Test
    fun `a missing resource fails at once with its HTTP status`() {
        val error = assertThrows(HttpStatusException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertEquals(404, error.status)
        assertEquals(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, error.code)
        assertEquals(1, server.requestCount(artifact.url))
    }

    @Test
    fun `corrupt and oversized bodies fail the integrity check and leave nothing behind`() {
        server.serve(artifact.url, content.copyOf().also { it[1000] = (it[1000] + 1).toByte() })
        val corrupt = assertThrows(IntegrityException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertEquals(SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, corrupt.code)
        assertFalse(downloader.partialFile(artifact, temporary.root).exists())
        assertFalse(downloader.completeFile(artifact, temporary.root).exists())
        server.serve(artifact.url, content).extraBytes = ByteArray(10)
        assertThrows(IntegrityException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertFalse(downloader.partialFile(artifact, temporary.root).exists())
        assertEquals("integrity failures are not retried", 2, server.requestCount(artifact.url))
    }

    @Test
    fun `cancellation stops a running transfer and keeps the partial file for a later resume`() {
        server.serve(artifact.url, content).bytesPerChunkDelayMillis = 20
        val cancel = CancelToken()
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<Unit> { downloader.fetch(artifact, temporary.root, cancel) }
        assertTrue(server.transferStarted.await(10, TimeUnit.SECONDS))
        Thread.sleep(100)
        cancel.cancel()
        val error = assertThrows(Exception::class.java) { future.get(10, TimeUnit.SECONDS) }
        assertTrue("$error", error.cause is OperationCancelledException)
        executor.shutdown()
        assertTrue(downloader.partialFile(artifact, temporary.root).length() in 1 until artifact.size)
        server.resource(artifact.url).bytesPerChunkDelayMillis = 0
        val kept = downloader.partialFile(artifact, temporary.root).length()
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        val resumed = server.requests.last().second!!
        assertTrue("resumed at or after the kept $kept B: $resumed", resumed.removePrefix("bytes=").removeSuffix("-").toLong() >= kept)
    }

    @Test
    fun `the production policy keeps requests on HTTPS GitHub hosts`() {
        val policy = NetworkPolicy.PRODUCTION
        assertTrue(policy.allowsEntry(URL("https://github.com/k2-fsa/sherpa-onnx/releases/download/a/b")))
        assertTrue(policy.allowsEntry(URL("https://raw.githubusercontent.com/SuperMonster003/AutoJs6-Shared-Assets/main/x")))
        assertFalse(policy.allowsEntry(URL("http://github.com/x")))
        assertFalse(policy.allowsEntry(URL("https://example.com/x")))
        assertFalse(policy.allowsEntry(URL("https://user@github.com/x")))
        assertTrue(policy.allowsRedirect(URL("https://release-assets.githubusercontent.com/x")))
        assertTrue(policy.allowsRedirect(URL("https://objects.githubusercontent.com/x")))
        assertFalse(policy.allowsRedirect(URL("https://githubusercontent.com.example.org/x")))
        assertFalse(policy.allowsRedirect(URL("http://release-assets.githubusercontent.com/x")))
        assertFalse(NetworkPolicy.PRODUCTION.allowsEntry(URL("http://127.0.0.1:8080/x")))
    }

    @Test
    fun `the production policy still guards the downloads of the kit`() {
        val production = Downloader(UrlConnectionTransport(userAgent = "test"), retryDelaysMillis = LongArray(0))
        val error = assertThrows(ModelException::class.java) { production.fetch(pinned("http://127.0.0.1:9/x.bin"), temporary.root, CancelToken()) }
        assertEquals(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, error.code)
        assertTrue(error.message!!, error.message!!.contains("not allowed"))
    }

    // The shared download kit (plugin-center P13.2) ------------------------------------------------------------------------

    @Test
    fun `a resume carries the validator of its server and a changed file starts over`() {
        server.serve(artifact.url, content).apply { dropAfter = 100_000; dropTimes = 1; etag = "\"v1\"" }
        assertEquals(artifact.sha256, Verifier.sha256(downloader.fetch(artifact, temporary.root, CancelToken())))
        assertEquals(listOf(null, "\"v1\""), server.ifRanges.toList())
        assertEquals("bytes=100000-", server.requests.last().second)

        // The server replaced the file (another ETag): it answers the stale If-Range with the whole file, which starts over.
        downloader.discard(artifact.sha256, temporary.root)
        server.requests.clear()
        server.ifRanges.clear()
        server.serve(artifact.url, content).apply { dropAfter = 100_000; dropTimes = 1; etag = "\"v1\"" }
        assertThrows(ModelException::class.java) { withoutRetries().fetch(artifact, temporary.root, CancelToken()) }
        server.resource(artifact.url).etag = "\"v2\""
        val progress = ArrayList<Long>()
        assertEquals(artifact.sha256, Verifier.sha256(downloader.fetch(artifact, temporary.root, CancelToken()) { progress += it }))
        assertEquals(listOf(null, "\"v1\""), server.ifRanges.toList())
        assertEquals("the whole file came again", 0L, progress.first())
    }

    @Test
    fun `another URL of the same file continues the bytes on disk`() {
        val mirror = pinned("https://github.com/SuperMonster003/AutoJs6-Shared-Assets/releases/download/speech-models-v2/test.tar")
        val upstream = pinned("https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/test-copy.tar")
        server.serve(mirror.url, content).apply { dropAfter = 100_000; dropTimes = 1; etag = "\"mirror\"" }
        server.serve(upstream.url, content).etag = "\"upstream\""
        assertThrows(ModelException::class.java) { withoutRetries().fetch(mirror, temporary.root, CancelToken()) }
        assertEquals(artifact.size - 100_000, downloader.remainingBytes(upstream, temporary.root))
        val file = downloader.fetch(upstream, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        val (path, range) = server.requests.last()
        assertTrue(path, path.endsWith("test-copy.tar"))
        assertEquals("bytes=100000-", range)
        assertNull("the validator of the other server is not sent", server.ifRanges.last())
    }

    @Test
    fun `a resumed file that fails its SHA-256 is downloaded whole once more`() {
        server.serve(artifact.url, content)
        downloader.legacyPartialFile(artifact.sha256, temporary.root).writeBytes(ByteArray(50_000) { 7 })
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        assertEquals(listOf("bytes=50000-", null), server.requests.map { it.second })
    }

    @Test
    fun `a file that fails its SHA-256 again is rejected and leaves nothing behind`() {
        server.serve(artifact.url, content.copyOf().also { it[200_000] = (it[200_000] + 1).toByte() })
        downloader.legacyPartialFile(artifact.sha256, temporary.root).writeBytes(content.copyOf(50_000))
        assertThrows(IntegrityException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertEquals("one resume, then the whole file once", listOf("bytes=50000-", null), server.requests.map { it.second })
        assertEquals(0L, downloader.downloadedBytes(artifact.sha256, temporary.root))
    }

    @Test
    fun `server errors are retried automatically and fail as a download failure when they persist`() {
        server.serve(artifact.url, content).apply { errorStatus = 503; errorTimes = 2 }
        assertEquals(artifact.sha256, Verifier.sha256(downloader.fetch(artifact, temporary.root, CancelToken())))
        assertEquals(3, server.requestCount(artifact.url))

        downloader.discard(artifact.sha256, temporary.root)
        server.serve(artifact.url, content).apply { errorStatus = 502; errorTimes = 3 }
        val error = assertThrows(ModelException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertFalse("not a missing mirror: $error", error is HttpStatusException)
        assertEquals(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, error.code)
        assertTrue(error.message!!, error.message!!.contains("502") && error.message!!.contains("2 retries"))
    }

    @Test
    fun `concurrent fetches of one file share one download`() {
        server.serve(artifact.url, content).bytesPerChunkDelayMillis = 5
        val executor = Executors.newFixedThreadPool(2)
        val first = executor.submit<File> { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertTrue(server.transferStarted.await(10, TimeUnit.SECONDS))
        val second = executor.submit<File> { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))
        executor.shutdown()
        assertEquals(1, server.requestCount(artifact.url))
    }

    @Test
    fun `an unfinished download of the previous downloader is imported and resumed`() {
        server.serve(artifact.url, content)
        val legacy = downloader.legacyPartialFile(artifact.sha256, temporary.root).apply { writeBytes(content.copyOf(123_456)) }
        assertEquals(artifact.size - 123_456, downloader.remainingBytes(artifact, temporary.root))
        assertEquals(123_456L, downloader.downloadedBytes(artifact.sha256, temporary.root))
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        assertEquals(listOf("bytes=123456-"), server.requests.map { it.second })
        assertFalse(legacy.exists())
    }

    @Test
    fun `a verified download of the previous downloader is reused without a request`() {
        server.serve(artifact.url, content)
        val legacy = downloader.legacyCompleteFile(artifact.sha256, temporary.root).apply { writeBytes(content) }
        downloader.legacyPartialFile(artifact.sha256, temporary.root).writeBytes(content.copyOf(10))
        assertEquals(0L, downloader.remainingBytes(artifact, temporary.root))
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(content.toList(), file.readBytes().toList())
        assertEquals(downloader.completeFile(artifact, temporary.root), file)
        assertEquals(emptyList<Pair<String, String?>>(), server.requests.toList())
        assertFalse(legacy.exists())
        assertFalse(downloader.legacyPartialFile(artifact.sha256, temporary.root).exists())
    }

    @Test
    fun `unusable files of the previous downloader are dropped`() {
        server.serve(artifact.url, content)
        val longer = downloader.legacyPartialFile(artifact.sha256, temporary.root).apply { writeBytes(content + ByteArray(10)) }
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        assertEquals(listOf<String?>(null), server.requests.map { it.second })
        assertFalse(longer.exists())
        // A damaged verified file restarts the download instead of failing.
        downloader.discard(artifact.sha256, temporary.root)
        downloader.legacyCompleteFile(artifact.sha256, temporary.root).writeBytes(content.copyOf().also { it[5] = 0 })
        assertEquals(artifact.sha256, Verifier.sha256(downloader.fetch(artifact, temporary.root, CancelToken())))
        assertEquals(listOf<String?>(null, null), server.requests.map { it.second })
    }

    @Test
    fun `discarding and recycling cover the store and the files of the previous downloader`() {
        server.serve(artifact.url, content)
        downloader.fetch(artifact, temporary.root, CancelToken())
        val other = "c".repeat(64)
        val stale = File(temporary.root, "${"b".repeat(64)}.part").apply { writeText("stale") }
        val paused = downloader.legacyPartialFile(other, temporary.root).apply { writeText("paused") }
        val foreign = File(downloader.storeDirectory(temporary.root), "foreign.tmp").apply { writeText("x") }
        val entry = downloader.completeFile(artifact, temporary.root).parentFile!!
        assertEquals(setOf(stale, foreign), downloader.leftovers(temporary.root, setOf(artifact.sha256, other)).toSet())
        assertEquals(setOf(stale, foreign, entry), downloader.leftovers(temporary.root, setOf(other)).toSet())
        assertEquals(setOf(stale, foreign, entry, paused), downloader.leftovers(temporary.root, setOf(artifact.sha256, other), modifiedBefore = System.currentTimeMillis() + 60_000).toSet())

        assertEquals(artifact.size, downloader.downloadedBytes(artifact.sha256, temporary.root))
        downloader.discard(artifact.sha256, temporary.root)
        downloader.discard(other, temporary.root)
        assertFalse(entry.exists())
        assertFalse(paused.exists())
        assertEquals(0L, downloader.downloadedBytes(artifact.sha256, temporary.root))
    }
}
