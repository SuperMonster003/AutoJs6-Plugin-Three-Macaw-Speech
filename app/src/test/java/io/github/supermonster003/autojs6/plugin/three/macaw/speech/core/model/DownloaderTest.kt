package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The resumable downloader against a local HTTP server (roadmap P2.3): full transfers with progress, resumption with
 * `Range` after a dropped connection and after a killed process, servers that ignore `Range`, missing resources,
 * corrupt and oversized bodies, cancellation, and the production network policy.
 * zh-CN: 可续传下载器对照本地 HTTP 服务器 (路线图 P2.3): 带进度的完整传输, 连接中断与进程被杀后以 `Range` 续传, 忽略 `Range` 的服务器,
 * 缺失的资源, 损坏与超长的响应体, 取消, 以及正式网络策略.
 */
class DownloaderTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private val server = FakeGitHub()
    private val content = ByteArray(300_000) { (it * 31 + it / 7).toByte() }
    private val artifact = object : PinnedArtifact {
        override val url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/test.tar.bz2"
        override val size = content.size.toLong()
        override val sha256 = MessageDigest.getInstance("SHA-256").digest(content).toHex()
    }
    private val downloader = Downloader(server.transport, attempts = 3, sleep = {})

    @After
    fun stop() = server.close()

    @Test
    fun `a full download verifies and reports monotonic progress`() {
        server.serve(artifact.url, content)
        val progress = ArrayList<Long>()
        val file = downloader.fetch(artifact, temporary.root, CancelToken()) { progress += it }
        assertEquals(content.toList(), file.readBytes().toList())
        assertEquals("${artifact.sha256}.bin", file.name)
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
    fun `a partial file left by a killed process is resumed`() {
        server.serve(artifact.url, content)
        downloader.partialFile(artifact, temporary.root).writeBytes(content.copyOf(123_456))
        assertEquals(artifact.size - 123_456, downloader.remainingBytes(artifact, temporary.root))
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
        assertEquals(listOf("bytes=123456-"), server.requests.map { it.second })
    }

    @Test
    fun `a server that ignores Range restarts the file`() {
        server.serve(artifact.url, content).ignoreRange = true
        downloader.partialFile(artifact, temporary.root).writeBytes(content.copyOf(5000))
        val file = downloader.fetch(artifact, temporary.root, CancelToken())
        assertEquals(artifact.sha256, Verifier.sha256(file))
    }

    @Test
    fun `a missing resource fails at once with its HTTP status`() {
        val error = assertThrows(HttpStatusException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertEquals(404, error.status)
        assertEquals(1, server.requestCount(artifact.url))
    }

    @Test
    fun `corrupt and oversized bodies fail the integrity check and leave nothing behind`() {
        server.serve(artifact.url, content.copyOf().also { it[1000] = (it[1000] + 1).toByte() })
        assertThrows(IntegrityException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertFalse(downloader.partialFile(artifact, temporary.root).exists())
        assertFalse(downloader.completeFile(artifact, temporary.root).exists())
        server.serve(artifact.url, content).extraBytes = ByteArray(10)
        assertThrows(IntegrityException::class.java) { downloader.fetch(artifact, temporary.root, CancelToken()) }
        assertFalse(downloader.partialFile(artifact, temporary.root).exists())
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
}
