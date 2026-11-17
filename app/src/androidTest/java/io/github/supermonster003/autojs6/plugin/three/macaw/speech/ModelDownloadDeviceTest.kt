package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Downloader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.HttpResponse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.HttpTransport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.IntegrityException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationCancelledException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.PinnedArtifact
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.UrlConnectionTransport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Verifier
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.util.Collections
import java.util.TreeMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * The model downloader on the shared download kit on a device (plugin-center P13.2), driven by
 * `py .python/device_download_run.py local|github <serial>`; results in logcat tag `MacawDeviceRun`, one `P13.2 <case>:`
 * line per case.
 *
 * - local (`-e macawDownloadServer http://127.0.0.1:<port>`): the PC server of the script, reached through `adb reverse`.
 *   It serves a 3,000,000 byte blob (byte i = i * 31 + i / 7, the content of DownloaderTest) with a strong ETag, honours
 *   `Range` and `If-Range`, and behaves per path: `drop` cuts the first answer after 1,000,000 bytes, `slow` sends 64 KiB
 *   every 50 ms, `corrupt` flips byte 2,000,000. The plugin forbids cleartext HTTP (network_security_config), so these
 *   cases reach it through [SocketTransport] instead of the production transport; everything else (the kit, its
 *   coroutines, the store on the device's file system, [Downloader] and its import of the files of 1.6.0) is the
 *   production code. Cases: a dropped connection continues on its own, a paused download continues in the next process,
 *   a SHA-256 mismatch is refused (also after a resume, then after one whole download), the unfinished and the verified
 *   download of 1.6.0 are imported.
 * - github (`-e macawDownloadGitHub true`): the production transport (HTTPS only, GitHub hosts, redirects checked, the
 *   pinned ISRG Root X1 of API 24 - 25) with the smallest mirror archive of the bundled catalog (about 6 MB), paused after
 *   2 MB and continued.
 *
 * zh-CN: 共用下载库上的模型下载器在设备上的运行 (plugin-center P13.2), 由 `py .python/device_download_run.py local|github <serial>` 驱动; 结果在 logcat 标签
 * `MacawDeviceRun` 中, 每个用例一行 `P13.2 <用例>:`. local: 脚本在 PC 上的服务器经 `adb reverse` 访问, 提供 3,000,000 字节的数据 (与 DownloaderTest 的
 * 内容相同) 与强 ETag, 支持 `Range` 与 `If-Range`, 按路径表现: `drop` 首次应答在 1,000,000 字节后断开, `slow` 每 50 ms 发 64 KiB, `corrupt` 改动第
 * 2,000,000 字节. 插件禁止明文 HTTP, 因此这些用例经 [SocketTransport] 访问它, 其余 (下载库, 协程, 设备文件系统上的仓库, [Downloader] 与对 1.6.0 文件的
 * 导入) 都是正式代码. github: 正式传输 (只用 HTTPS, GitHub 主机, 检查重定向, API 24 - 25 锁定的 ISRG Root X1) 下载内置目录中最小的镜像归档 (约 6 MB),
 * 2 MB 后暂停再继续.
 */
@RunWith(AndroidJUnit4::class)
class ModelDownloadDeviceTest {

    private val context = SpeechDevice.context
    private val directory = File(context.cacheDir, "download-device-test")
    private val logs: MutableList<String> = Collections.synchronizedList(ArrayList())
    private val content = ByteArray(SIZE) { (it * 31 + it / 7).toByte() }
    private val sha256 = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }

    private fun downloader(transport: HttpTransport) = Downloader(transport, log = { message ->
        logs += message
        SpeechDevice.log("kit: $message")
    })

    private fun pinned(url: String, size: Long, sha256: String): PinnedArtifact = object : PinnedArtifact {
        override val url = url
        override val size = size
        override val sha256 = sha256
    }

    private fun local(path: String): PinnedArtifact {
        val server = SpeechDevice.argument("macawDownloadServer")
        assumeTrue("opt-in: -e macawDownloadServer http://127.0.0.1:<port> (py .python/device_download_run.py local)", server != null)
        return pinned("$server/blob/$path", SIZE.toLong(), sha256)
    }

    private fun device() = "${Build.MODEL} API ${Build.VERSION.SDK_INT}"

    private fun report(case: String, message: String) = SpeechDevice.log("P13.2 $case: $message (${device()})")

    @Before
    fun clean() {
        directory.deleteRecursively()
    }

    @After
    fun tidy() {
        directory.deleteRecursively()
    }

    @Test
    fun droppedConnectionContinuesOnItsOwn() {
        val artifact = local("drop")
        val started = SystemClock.elapsedRealtime()
        val file = downloader(SocketTransport()).fetch(artifact, directory, CancelToken())
        assertEquals(sha256, Verifier.sha256(file))
        assertTrue(logs.joinToString("\n"), logs.any { it.contains("download retry 1/2") })
        assertTrue(logs.joinToString("\n"), logs.any { it.contains("downloaded: $SIZE B (resumed)") })
        report("interrupted", "PASS, cut at 1000000 B, retried and resumed, SHA-256 verified in ${SystemClock.elapsedRealtime() - started} ms")
    }

    @Test
    fun pausedDownloadContinuesInTheNextProcess() {
        val artifact = local("slow")
        val first = downloader(SocketTransport())
        val cancel = CancelToken()
        val progress = AtomicLong()
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<File> { first.fetch(artifact, directory, cancel) { progress.set(it) } }
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (progress.get() < 1_000_000 && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(20)
        cancel.cancel()
        val error = assertThrows(Exception::class.java) { future.get(10, TimeUnit.SECONDS) }
        executor.shutdown()
        assertTrue("$error", error.cause is OperationCancelledException)
        SystemClock.sleep(300)
        val kept = first.downloadedBytes(sha256, directory)
        assertTrue("kept $kept", kept in 1 until SIZE)
        // Another Downloader stands for the next process: it continues the bytes on disk.
        val file = downloader(SocketTransport()).fetch(artifact, directory, CancelToken())
        assertEquals(sha256, Verifier.sha256(file))
        assertTrue(logs.joinToString("\n"), logs.any { it.contains("downloaded: $SIZE B (resumed)") })
        report("paused", "PASS, paused at $kept B, the next downloader resumed and verified the SHA-256")
    }

    @Test
    fun sha256MismatchIsRefused() {
        val artifact = local("corrupt")
        val downloader = downloader(SocketTransport())
        val error = assertThrows(IntegrityException::class.java) { downloader.fetch(artifact, directory, CancelToken()) }
        assertEquals(SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, error.code)
        assertEquals("nothing is kept", 0L, downloader.downloadedBytes(sha256, directory))
        assertFalse(downloader.completeFile(artifact, directory).exists())

        // A resumed file that fails is downloaded whole once more, then refused for good.
        // zh-CN: 续传得到的文件未通过时整包重下一次, 仍不符则最终拒绝.
        downloader.legacyPartialFile(sha256, directory).apply { parentFile!!.mkdirs(); writeBytes(content.copyOf(1_000_000)) }
        val resumed = assertThrows(IntegrityException::class.java) { downloader.fetch(artifact, directory, CancelToken()) }
        assertEquals(SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, resumed.code)
        assertTrue(logs.joinToString("\n"), logs.any { it.contains("failed its check (CHECKSUM_MISMATCH), downloading it whole once more") })
        assertEquals(0L, downloader.downloadedBytes(sha256, directory))
        report("hash-mismatch", "PASS, refused with ${error.code}, also after a resume and one whole download; nothing kept")
    }

    @Test
    fun unfinishedDownloadOfTheFormerDownloaderIsImported() {
        val artifact = local("legacy")
        val downloader = downloader(SocketTransport())
        val legacy = downloader.legacyPartialFile(sha256, directory).apply { parentFile!!.mkdirs(); writeBytes(content.copyOf(1_500_000)) }
        assertEquals(SIZE - 1_500_000L, downloader.remainingBytes(artifact, directory))
        val file = downloader.fetch(artifact, directory, CancelToken())
        assertEquals(sha256, Verifier.sha256(file))
        assertFalse(legacy.exists())
        assertTrue(logs.joinToString("\n"), logs.any { it.contains("continuing the 1500000 B of part left by 1.6.0 or earlier") })
        assertTrue(logs.joinToString("\n"), logs.any { it.contains("downloaded: $SIZE B (resumed)") })

        // A verified file of 1.6.0 is reused without a request.
        // zh-CN: 1.6.0 已校验的文件不发请求直接复用.
        downloader.discard(sha256, directory)
        val verified = downloader.legacyCompleteFile(sha256, directory).apply { writeBytes(content) }
        val reused = downloader(SocketTransport()).fetch(local("legacy-verified"), directory, CancelToken())
        assertEquals(sha256, Verifier.sha256(reused))
        assertFalse(verified.exists())
        report("legacy-import", "PASS, the 1500000 B .part of 1.6.0 resumed with Range, its verified .bin reused without a request")
    }

    @Test
    fun realGitHubDownloadPausesAndContinues() {
        assumeTrue("opt-in: -e macawDownloadGitHub true (about 6 MB from GitHub)", SpeechDevice.argument("macawDownloadGitHub") == "true")
        val catalog = SpeechModels.get(context).catalogs.current().catalog
        val mirror = catalog.models.flatMap { it.versions }.mapNotNull { it.mirror }.minBy { it.size }
        val transport = UrlConnectionTransport(userAgent = "Three-Macaw-Speech device test")
        val host = transport.get(mirror.url, HttpTransport.rangeHeaders(mirror.size - 1), CancelToken()).use { "${it.url?.let { final -> URL(final).host }} HTTP ${it.status}" }
        val downloader = downloader(transport)
        val cancel = CancelToken()
        val progress = AtomicLong()
        val executor = Executors.newSingleThreadExecutor()
        val started = SystemClock.elapsedRealtime()
        val future = executor.submit<File> { downloader.fetch(mirror, directory, cancel) { progress.set(it) } }
        val deadline = SystemClock.elapsedRealtime() + 120_000
        while (progress.get() < 2_000_000 && !future.isDone && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(20)
        cancel.cancel()
        runCatching { future.get(30, TimeUnit.SECONDS) }
        executor.shutdown()
        SystemClock.sleep(300)
        val kept = downloader.downloadedBytes(mirror.sha256, directory)
        val file = downloader.fetch(mirror, directory, CancelToken())
        assertEquals(mirror.sha256, Verifier.sha256(file))
        val resumed = logs.any { it.contains("downloaded: ${mirror.size} B (resumed)") }
        assertTrue("paused at $kept of ${mirror.size}: ${logs.joinToString("\n")}", kept in 1 until mirror.size && resumed)
        report("github", "PASS, ${mirror.fileName} (${mirror.size} B) via $host, paused at $kept B and resumed, SHA-256 verified in ${SystemClock.elapsedRealtime() - started} ms")
    }

    /**
     * A plain HTTP/1.1 client on a socket for the loopback server of the script (the plugin forbids cleartext through
     * `HttpURLConnection`); it sends the kit's headers and answers like [UrlConnectionTransport] does.
     * zh-CN: 套接字上的简单 HTTP/1.1 客户端, 用于脚本的回环服务器 (插件禁止经 `HttpURLConnection` 的明文流量); 发出下载库的请求头, 应答方式与
     * [UrlConnectionTransport] 相同.
     */
    private class SocketTransport : HttpTransport {

        override fun get(url: String, rangeStart: Long, cancel: CancelToken): HttpResponse = get(url, HttpTransport.rangeHeaders(rangeStart), cancel)

        override fun get(url: String, headers: Map<String, String>, cancel: CancelToken): HttpResponse {
            val target = URL(url)
            if (target.protocol != "http" || target.host != "127.0.0.1") throw IOException("Only the loopback test server: $url")
            cancel.throwIfCancelled()
            val socket = Socket()
            val registration = cancel.register { runCatching { socket.close() } }
            try {
                socket.connect(InetSocketAddress(target.host, target.port), 15_000)
                socket.soTimeout = 30_000
                val request = StringBuilder("GET ${target.file} HTTP/1.1\r\nHost: 127.0.0.1:${target.port}\r\nConnection: close\r\nAccept-Encoding: identity\r\n")
                headers.forEach { (name, value) -> request.append(name).append(": ").append(value).append("\r\n") }
                socket.getOutputStream().apply { write(request.append("\r\n").toString().toByteArray(Charsets.ISO_8859_1)); flush() }
                val input = BufferedInputStream(socket.getInputStream())
                val status = line(input).split(' ').getOrNull(1)?.toIntOrNull() ?: throw IOException("No status line")
                val fields = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                while (true) {
                    val field = line(input)
                    if (field.isEmpty()) break
                    fields[field.substringBefore(':').trim()] = field.substringAfter(':').trim()
                }
                val length = fields["Content-Length"]?.toLongOrNull() ?: -1L
                val body = if (status == 200 || status == 206) Bounded(input, length) else null
                return HttpResponse(status, length, fields["Content-Range"], body, url, { fields[it] }) {
                    registration.close()
                    runCatching { socket.close() }
                }
            } catch (e: IOException) {
                registration.close()
                runCatching { socket.close() }
                throw e
            }
        }

        private fun line(input: InputStream): String {
            val out = ByteArrayOutputStream()
            while (true) {
                val b = input.read()
                if (b < 0) throw IOException("Connection closed in the header")
                if (b == '\n'.code) break
                if (b != '\r'.code) out.write(b)
            }
            return out.toString(Charsets.ISO_8859_1.name())
        }

        /** The body: at most [length] bytes (until the end without one). zh-CN: 正文: 最多 [length] 字节 (没有长度时读到结束). */
        private class Bounded(private val input: InputStream, private var left: Long) : InputStream() {
            override fun read(): Int {
                val one = ByteArray(1)
                return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (left == 0L) return -1
                val wanted = if (left < 0) len else minOf(len.toLong(), left).toInt()
                val read = input.read(b, off, wanted)
                if (read > 0 && left > 0) left -= read
                return read
            }

            override fun close() = input.close()
        }
    }

    private companion object {
        const val SIZE = 3_000_000
    }
}
