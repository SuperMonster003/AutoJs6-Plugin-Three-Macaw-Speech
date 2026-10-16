package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Cooperative cancellation of one operation: [cancel] runs the registered hooks (closing the open connection, so a
 * blocked read returns at once) and later checks throw [OperationCancelledException].
 * zh-CN: 单个操作的协作式取消: [cancel] 执行已登记的钩子 (关闭打开的连接, 使阻塞的读取立即返回), 之后的检查抛出 [OperationCancelledException].
 */
class CancelToken {

    @Volatile
    var isCancelled: Boolean = false
        private set

    private val hooks = CopyOnWriteArrayList<() -> Unit>()

    fun cancel() {
        isCancelled = true
        hooks.forEach { runCatching(it) }
    }

    fun throwIfCancelled() {
        if (isCancelled || Thread.currentThread().isInterrupted) throw OperationCancelledException()
    }

    /** Runs [hook] on cancellation until the returned handle is closed. zh-CN: 在返回的句柄关闭前, 取消时执行 [hook]. */
    fun register(hook: () -> Unit): Closeable {
        hooks += hook
        if (isCancelled) runCatching(hook)
        return Closeable { hooks -= hook }
    }
}

/**
 * Where downloads may go (roadmap D9, D24): HTTPS only; the first request goes to a host in [entryHosts] and redirects
 * stay on GitHub's own hosts. Integrity never depends on this policy (every byte is checked against a pinned SHA-256);
 * it only keeps requests away from unexpected servers. Tests allow plain HTTP on the loopback interface.
 * zh-CN: 下载的去向 (路线图 D9, D24): 只用 HTTPS; 首个请求的主机须在 [entryHosts] 中, 重定向只停留在 GitHub 自己的主机上. 完整性从不依赖
 * 该策略 (每个字节都按固定的 SHA-256 校验), 它只让请求不去意外的服务器. 测试允许回环接口上的明文 HTTP.
 */
class NetworkPolicy(
    private val entryHosts: Set<String>,
    private val redirectHostSuffixes: Set<String>,
    private val allowLoopbackHttp: Boolean = false,
) {
    fun allowsEntry(url: URL): Boolean = allowsScheme(url) && (isLoopbackTest(url) || url.host.lowercase() in entryHosts)

    fun allowsRedirect(url: URL): Boolean = allowsScheme(url) && (isLoopbackTest(url) || redirectHostSuffixes.any { suffix ->
        val host = url.host.lowercase()
        host == suffix || host.endsWith(".$suffix")
    })

    private fun allowsScheme(url: URL): Boolean = url.userInfo == null && (url.protocol == "https" || isLoopbackTest(url))

    private fun isLoopbackTest(url: URL): Boolean = allowLoopbackHttp && url.protocol == "http" && url.host == "127.0.0.1"

    companion object {
        val PRODUCTION = NetworkPolicy(
            entryHosts = setOf("github.com", "raw.githubusercontent.com"),
            redirectHostSuffixes = setOf("github.com", "githubusercontent.com"),
        )

        /** JVM tests: the local fake server only. zh-CN: JVM 测试: 只允许本地假服务器. */
        val LOOPBACK_TEST = NetworkPolicy(emptySet(), emptySet(), allowLoopbackHttp = true)
    }
}

/** An open HTTP answer; [body] is null for anything but 200 and 206. zh-CN: 已打开的 HTTP 应答; 仅 200 与 206 时 [body] 非空. */
class HttpResponse(
    val status: Int,
    val contentLength: Long,
    val contentRange: String?,
    val body: InputStream?,
    private val onClose: () -> Unit,
) : Closeable {
    override fun close() = onClose()
}

/** Opens GET requests for the downloader and the catalog refresh. zh-CN: 为下载器与目录刷新发起 GET 请求. */
fun interface HttpTransport {
    @Throws(IOException::class)
    fun get(url: String, rangeStart: Long, cancel: CancelToken): HttpResponse
}

/**
 * [HttpTransport] on `HttpURLConnection` (no extra dependency): manual redirects checked against [policy] at every hop
 * (at most [maxRedirects]), `Accept-Encoding: identity` so lengths are the raw bytes, a `Range` header on every hop
 * when resuming, and the connection closed on cancellation.
 * zh-CN: 基于 `HttpURLConnection` 的 [HttpTransport] (无额外依赖): 手动跟随重定向并在每一跳按 [policy] 检查 (最多 [maxRedirects] 次),
 * `Accept-Encoding: identity` 使长度即原始字节数, 续传时每一跳都带 `Range` 头, 取消时关闭连接.
 */
class UrlConnectionTransport(
    private val policy: NetworkPolicy = NetworkPolicy.PRODUCTION,
    private val userAgent: String,
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 30_000,
    private val maxRedirects: Int = 5,
) : HttpTransport {

    override fun get(url: String, rangeStart: Long, cancel: CancelToken): HttpResponse {
        var current = URL(url)
        if (!policy.allowsEntry(current)) throw IOException("Download URL is not allowed: ${redactQuery(url)}")
        repeat(maxRedirects + 1) {
            cancel.throwIfCancelled()
            val connection = current.openConnection() as HttpURLConnection
            val registration = cancel.register { connection.disconnect() }
            var keep = false
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = connectTimeoutMillis
                connection.readTimeout = readTimeoutMillis
                connection.useCaches = false
                connection.setRequestProperty("User-Agent", userAgent)
                connection.setRequestProperty("Accept", "application/octet-stream, application/json;q=0.9, */*;q=0.1")
                connection.setRequestProperty("Accept-Encoding", "identity")
                if (rangeStart > 0) connection.setRequestProperty("Range", "bytes=$rangeStart-")
                val status = connection.responseCode
                cancel.throwIfCancelled()
                if (status in REDIRECTS) {
                    val location = connection.getHeaderField("Location") ?: throw IOException("Redirect without a location")
                    val next = URI(current.toString()).resolve(location.trim()).toURL()
                    if (!policy.allowsRedirect(next)) throw IOException("Redirect target is not allowed: ${redactQuery(next.toString())}")
                    current = next
                    return@repeat
                }
                val body = if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_PARTIAL) connection.inputStream else null
                keep = true
                return HttpResponse(
                    status = status,
                    contentLength = connection.getHeaderField("Content-Length")?.trim()?.toLongOrNull() ?: -1L,
                    contentRange = connection.getHeaderField("Content-Range"),
                    body = body,
                ) {
                    registration.close()
                    runCatching { body?.close() }
                    connection.disconnect()
                }
            } finally {
                if (!keep) {
                    registration.close()
                    connection.disconnect()
                }
            }
        }
        throw IOException("Too many redirects for ${redactQuery(url)}")
    }

    private companion object {
        val REDIRECTS = setOf(301, 302, 303, 307, 308)
    }
}

/** Reads a small resource (catalog, signature) completely, refusing more than [maxBytes]. zh-CN: 完整读取小资源, 超过 [maxBytes] 即拒绝. */
@Throws(IOException::class)
fun HttpTransport.fetchBytes(url: String, maxBytes: Long, cancel: CancelToken = CancelToken()): ByteArray = get(url, 0, cancel).use { response ->
    if (response.status != HttpURLConnection.HTTP_OK) throw HttpStatusException(response.status, url)
    if (response.contentLength > maxBytes) throw IOException("Response exceeds $maxBytes bytes: ${redactQuery(url)}")
    val body = response.body ?: throw IOException("Empty response body")
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
        val read = body.read(buffer)
        if (read < 0) break
        if (out.size() + read > maxBytes) throw IOException("Response exceeds $maxBytes bytes: ${redactQuery(url)}")
        out.write(buffer, 0, read)
    }
    if (response.contentLength >= 0 && out.size().toLong() != response.contentLength) throw IOException("Response ended early: ${redactQuery(url)}")
    out.toByteArray()
}
