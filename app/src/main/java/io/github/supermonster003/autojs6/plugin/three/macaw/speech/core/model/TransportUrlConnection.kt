package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * The `HttpURLConnection` that the shared download kit opens for [Downloader] (plugin-center P13.2): the request goes
 * through the plugin's own [HttpTransport], so the network policy of roadmap D9 / D24 (HTTPS only, GitHub hosts, every
 * redirect hop checked by hand) and the platform trust configuration (the pinned ISRG Root X1 of API 24 - 25) still
 * apply to every model download. The headers the kit sets (`Range`, `If-Range`, `Accept-Encoding`) go with the request
 * on every hop; the answer gives the kit its status, lengths, `Content-Range`, validators and the final URL. The kit's
 * timeouts and redirect switch are ignored: the transport has its own. [abort] closes the open connection, so a read
 * blocked on a stalled server returns at once.
 * zh-CN: 共用下载库为 [Downloader] 打开的 `HttpURLConnection` (plugin-center P13.2): 请求经插件自己的 [HttpTransport] 发出, 因此路线图 D9 / D24 的网络策略
 * (只用 HTTPS, GitHub 主机, 手动跟随并逐跳检查重定向) 与平台的信任配置 (API 24 - 25 上锁定的 ISRG Root X1) 仍作用于每次模型下载. 下载库设置的请求头
 * (`Range`, `If-Range`, `Accept-Encoding`) 在每一跳随请求发出; 应答向下载库提供状态码, 长度, `Content-Range`, 验证头与最终 URL. 下载库的超时与重定向开关被忽略:
 * 传输有自己的设置. [abort] 关闭已打开的连接, 使阻塞在停滞服务器上的读取立即返回.
 */
internal class TransportUrlConnection(
    url: URL,
    private val transport: HttpTransport,
    private val onConnect: (TransportUrlConnection) -> Unit = {},
    private val onDisconnect: (TransportUrlConnection) -> Unit = {},
) : HttpURLConnection(url) {

    /** The URL the kit asked for, before redirects. zh-CN: 下载库请求的 URL (重定向之前). */
    val requestUrl: String = url.toString()

    private val headers = LinkedHashMap<String, String>()
    private val cancel = CancelToken()

    @Volatile
    private var response: HttpResponse? = null
    private var failure: IOException? = null

    override fun setRequestProperty(key: String, value: String?) {
        check(!connected) { "Already connected" }
        headers.keys.firstOrNull { it.equals(key, ignoreCase = true) }?.let(headers::remove)
        if (value != null) headers[key] = value
    }

    override fun addRequestProperty(key: String, value: String?) = setRequestProperty(key, value)

    override fun getRequestProperty(key: String?): String? = headers.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value

    override fun getRequestProperties(): Map<String, List<String>> = headers.mapValues { listOf(it.value) }

    @Throws(IOException::class)
    override fun connect() {
        if (connected) return
        failure?.let { throw it }
        onConnect(this)
        try {
            response = transport.get(requestUrl, headers.toMap(), cancel)
            connected = true
        } catch (e: IOException) {
            failure = e
            onDisconnect(this)
            throw e
        } catch (e: ModelException) {
            // [abort] before or during the request: the kit treats it like a dropped connection.
            // zh-CN: 请求之前或期间的 [abort]: 下载库将其视同连接中断.
            val io = IOException(e.message, e)
            failure = io
            onDisconnect(this)
            throw io
        }
    }

    @Throws(IOException::class)
    private fun answer(): HttpResponse {
        connect()
        return response ?: throw IOException("No response")
    }

    @Throws(IOException::class)
    override fun getResponseCode(): Int = answer().status

    @Throws(IOException::class)
    override fun getResponseMessage(): String? {
        answer()
        return null
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream = answer().let { it.body ?: throw IOException("HTTP ${it.status} has no body") }

    override fun getContentLengthLong(): Long = runCatching { answer().contentLength }.getOrDefault(-1L)

    override fun getContentLength(): Int = contentLengthLong.let { if (it > Int.MAX_VALUE) -1 else it.toInt() }

    override fun getHeaderField(name: String?): String? {
        if (name == null) return null
        val answer = runCatching { answer() }.getOrNull() ?: return null
        return when {
            name.equals("Content-Length", ignoreCase = true) -> answer.contentLength.takeIf { it >= 0 }?.toString()
            name.equals("Content-Range", ignoreCase = true) -> answer.contentRange
            else -> answer.header(name)
        }
    }

    /** The final URL after the transport's redirects (the kit reports its host). zh-CN: 传输跟随重定向之后的最终 URL (下载库报告其主机). */
    override fun getURL(): URL = response?.url?.let { runCatching { URL(it) }.getOrNull() } ?: url

    override fun disconnect() {
        response?.close()
        onDisconnect(this)
    }

    override fun usingProxy(): Boolean = false

    /** Closes the connection from another thread. zh-CN: 从其他线程关闭连接. */
    fun abort() = cancel.cancel()
}
