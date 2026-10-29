package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The fixtures of `.python/generate_model_fixtures.py`, written by the Shared-Assets reference tools.
 * zh-CN: `.python/generate_model_fixtures.py` 生成的夹具, 由 Shared-Assets 参考工具写出.
 */
internal object Fixtures {

    fun file(name: String): File = RepositoryFiles.path("app/src/test/resources/model-fixtures/$name").toFile()

    fun bytes(name: String): ByteArray = Files.readAllBytes(file(name).toPath())

    /** The schema 3 catalog the plugin fetches (`catalog-v3.json`, release 3). zh-CN: 插件刷新的 schema 3 目录 (发布 3). */
    val catalogBytes: ByteArray get() = bytes(CatalogRules.CATALOG_FILE)

    /**
     * The schema 2 catalog of release 2 (`catalog-v2.json`), as bundled until roadmap P8.5.2 and cached by 1.2.0.
     * zh-CN: 发布 2 的 schema 2 目录, 即路线图 P8.5.2 之前的内置目录与 1.2.0 的缓存.
     */
    val catalogV2Bytes: ByteArray get() = bytes("catalog-v2.json")

    val catalog: ModelCatalog get() = ModelCatalogParser().parse(catalogBytes)

    /**
     * The schema 1 predecessor (catalogVersion 1, the three models without punctuation), as cached by 1.0.0 / 1.1.0.
     * zh-CN: schema 1 的前一版 (catalogVersion 1, 不含标点的三个模型), 即 1.0.0 / 1.1.0 缓存的目录.
     */
    val catalogV1Bytes: ByteArray get() = bytes("catalog-v1.json")

    /** The catalog JSON as text, for mutations. zh-CN: 目录 JSON 文本, 供变异使用. */
    val catalogText: String get() = catalogBytes.toString(Charsets.UTF_8)

    val tiny: CatalogModel get() = catalog.model("tts.test.tiny")!!
}

/** In-test P-256 keys and the crypto.py envelope. zh-CN: 测试内生成的 P-256 密钥与 crypto.py 信封. */
internal class TestSigner(val keys: KeyPair = generate()) {

    val publicDer: ByteArray get() = keys.public.encoded

    val keyId: String get() = CatalogSignatureVerifier.keyIdForDer(publicDer)

    fun verifier(): CatalogSignatureVerifier = CatalogSignatureVerifier(publicDer, keyId)

    fun derSignature(bytes: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
        initSign(keys.private)
        update(bytes)
        sign()
    }

    /** `crypto.sign_catalog` output: canonical JSON (sorted keys, two-space indent, trailing newline). */
    fun envelope(bytes: ByteArray, keyId: String = this.keyId, signature: String = Base64Strict.encode(derSignature(bytes)), catalog: String = CatalogRules.CATALOG_FILE): ByteArray =
        ("{\n  \"algorithm\": \"ECDSA_P256_SHA256\",\n  \"catalog\": \"$catalog\",\n  \"keyId\": \"$keyId\",\n" +
            "  \"signature\": \"$signature\"\n}\n").toByteArray(Charsets.UTF_8)

    companion object {
        fun generate(curve: String = "secp256r1"): KeyPair = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec(curve))
            generateKeyPair()
        }
    }
}

/**
 * A local HTTP server standing in for GitHub: `https://github.com/` and `https://raw.githubusercontent.com/` are
 * rewritten to it by [transport], and every resource honours `Range` unless told otherwise. Behaviours per path let
 * tests produce missing mirrors, corrupt bytes, dropped connections, ignored ranges and slow transfers.
 * zh-CN: 代替 GitHub 的本地 HTTP 服务器: [transport] 把 `https://github.com/` 与 `https://raw.githubusercontent.com/` 改写到它,
 * 每个资源都支持 `Range`, 除非另行指定. 按路径设置的行为让测试可以制造镜像缺失, 字节损坏, 连接中断, 忽略 Range 与慢速传输.
 */
internal class FakeGitHub : AutoCloseable {

    class Resource(
        var bytes: ByteArray,
        var status: Int = 200,
        var ignoreRange: Boolean = false,
        /** Close the connection after this many body bytes, [dropTimes] times. */
        var dropAfter: Int = -1,
        var dropTimes: Int = 0,
        var bytesPerChunkDelayMillis: Long = 0,
        var extraBytes: ByteArray = ByteArray(0),
    )

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val resources = Collections.synchronizedMap(HashMap<String, Resource>())
    val requests: MutableList<Pair<String, String?>> = Collections.synchronizedList(ArrayList())
    private val active = AtomicInteger()
    val maxConcurrent = AtomicInteger()
    var transferStarted = CountDownLatch(1)

    init {
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/") { exchange -> try { handle(exchange) } finally { exchange.close() } }
        server.start()
    }

    val base: String get() = "http://127.0.0.1:${server.address.port}"

    /** Serves [bytes] at the public [url] (`https://github.com/...` or `https://raw.githubusercontent.com/...`). */
    fun serve(url: String, bytes: ByteArray): Resource = Resource(bytes).also { resources[local(url)] = it }

    fun resource(url: String): Resource = resources.getValue(local(url))

    fun remove(url: String) {
        resources.remove(local(url))
    }

    fun requestCount(url: String): Int = requests.count { it.first == local(url) }

    /** Serves every mirror and upstream asset of [catalog] from the fixtures. zh-CN: 从夹具提供 [catalog] 的全部镜像与上游资源. */
    fun serveCatalogAssets(catalog: ModelCatalog) {
        catalog.models.flatMap { it.versions }.forEach { version ->
            version.mirror?.let { serve(it.url, Fixtures.bytes(it.fileName)) }
            version.upstream.forEach { serve(it.url, Fixtures.bytes(it.url.substringAfterLast('/'))) }
        }
    }

    val transport: HttpTransport = HttpTransport { url, start, cancel ->
        val rewritten = when {
            url.startsWith("https://github.com/") -> "$base/github/" + url.removePrefix("https://github.com/")
            url.startsWith("https://raw.githubusercontent.com/") -> "$base/raw/" + url.removePrefix("https://raw.githubusercontent.com/")
            else -> url
        }
        UrlConnectionTransport(NetworkPolicy.LOOPBACK_TEST, userAgent = "test").get(rewritten, start, cancel)
    }

    private fun local(url: String): String = when {
        url.startsWith("https://github.com/") -> "/github/" + url.removePrefix("https://github.com/")
        url.startsWith("https://raw.githubusercontent.com/") -> "/raw/" + url.removePrefix("https://raw.githubusercontent.com/")
        else -> error("Unexpected URL $url")
    }

    private fun handle(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        val range = exchange.requestHeaders.getFirst("Range")
        requests += path to range
        val resource = resources[path]
        if (resource == null) {
            exchange.sendResponseHeaders(404, -1)
            return
        }
        if (resource.status != 200) {
            exchange.sendResponseHeaders(resource.status, -1)
            return
        }
        val concurrent = active.incrementAndGet()
        maxConcurrent.accumulateAndGet(concurrent) { a, b -> maxOf(a, b) }
        try {
            val body = resource.bytes + resource.extraBytes
            var start = 0
            if (range != null && !resource.ignoreRange) {
                start = Regex("bytes=([0-9]+)-").matchEntire(range)!!.groupValues[1].toInt()
                if (start >= body.size) {
                    exchange.sendResponseHeaders(416, -1)
                    return
                }
                exchange.responseHeaders.add("Content-Range", "bytes $start-${body.size - 1}/${body.size}")
                exchange.sendResponseHeaders(206, (body.size - start).toLong())
            } else {
                exchange.sendResponseHeaders(200, body.size.toLong())
            }
            transferStarted.countDown()
            val output = exchange.responseBody
            var sent = 0
            var at = start
            while (at < body.size) {
                val chunk = minOf(4096, body.size - at)
                if (resource.dropTimes > 0 && resource.dropAfter in 0 until sent + chunk) {
                    resource.dropTimes--
                    output.write(body, at, resource.dropAfter - sent)
                    output.flush()
                    // Abort mid-body: the client sees the connection end before Content-Length.
                    exchange.close()
                    return
                }
                output.write(body, at, chunk)
                output.flush()
                at += chunk
                sent += chunk
                if (resource.bytesPerChunkDelayMillis > 0) Thread.sleep(resource.bytesPerChunkDelayMillis)
            }
        } catch (_: Exception) {
            // The client went away (cancellation tests).
        } finally {
            active.decrementAndGet()
        }
    }

    override fun close() = server.stop(0)
}

/** Collects the events of one operation. zh-CN: 收集单个操作的事件. */
internal class RecordingListener : OperationListener {
    val progress: MutableList<Pair<Long, Long>> = Collections.synchronizedList(ArrayList())
    @Volatile var result: OperationResult? = null
    @Volatile var error: ModelException? = null
    private val done = CountDownLatch(1)

    override fun onProgress(opId: Int, done: Long, total: Long) {
        progress += done to total
    }

    override fun onCompleted(opId: Int, result: OperationResult) {
        this.result = result
        done.countDown()
    }

    override fun onFailed(opId: Int, error: ModelException) {
        this.error = error
        done.countDown()
    }

    fun await(seconds: Long = 30): RecordingListener = apply { check(done.await(seconds, TimeUnit.SECONDS)) { "operation did not finish" } }
}

/** A small tar writer for malformed and hostile archives the reference tools would never write. */
internal object TarBuilder {

    fun entry(name: String, content: ByteArray, type: Char = '0', declaredSize: Long = content.size.toLong(), mode: Int = 420): ByteArray {
        val header = ByteArray(512)
        fun put(offset: Int, text: String) = text.toByteArray(Charsets.ISO_8859_1).copyInto(header, offset)
        put(0, name.take(100))
        put(100, Integer.toOctalString(mode).padStart(7, '0'))
        put(108, "0000000")
        put(116, "0000000")
        put(124, java.lang.Long.toOctalString(declaredSize).padStart(11, '0'))
        put(136, "00000000000")
        put(148, "        ")
        header[156] = type.code.toByte()
        put(257, "ustar")
        put(263, "00")
        val sum = header.sumOf { it.toInt() and 0xFF }
        put(148, Integer.toOctalString(sum).padStart(6, '0'))
        header[154] = 0
        header[155] = ' '.code.toByte()
        val out = ByteArrayOutputStream()
        out.write(header)
        out.write(content)
        out.write(ByteArray(((content.size + 511) / 512 * 512) - content.size))
        return out.toByteArray()
    }

    fun archive(vararg entries: ByteArray, end: Boolean = true): ByteArray {
        val out = ByteArrayOutputStream()
        entries.forEach { out.write(it) }
        if (end) out.write(ByteArray(1024))
        return out.toByteArray()
    }
}
