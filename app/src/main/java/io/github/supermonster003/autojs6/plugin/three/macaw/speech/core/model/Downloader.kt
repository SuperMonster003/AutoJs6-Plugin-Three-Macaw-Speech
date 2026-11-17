package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.downloadkit.DownloadFailure
import org.autojs.plugin.downloadkit.DownloadFailureKind
import org.autojs.plugin.downloadkit.DownloadFetcher
import org.autojs.plugin.downloadkit.DownloadKey
import org.autojs.plugin.downloadkit.DownloadListener
import org.autojs.plugin.downloadkit.DownloadOutcome
import org.autojs.plugin.downloadkit.DownloadRequest
import org.autojs.plugin.downloadkit.DownloadStore
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Resumable, verified download of one [PinnedArtifact] (roadmap D23, D24) on the download kit that AutoJs6 shares with
 * its official plugins (`org.autojs.plugin.downloadkit`, plugin-center P13.2, D22, D26). Files live in the kit's store in
 * [STORE_DIRECTORY] of the download directory, one entry per SHA-256 (`<sha256>/file`, `file.part`, `meta.json`):
 *
 * - A verified file is never fetched twice; its SHA-256 is checked again whenever it is reused.
 * - An unfinished file continues with `Range` (with `If-Range` when the server gave a validator) after a dropped
 *   connection, a cancellation, a killed process, and also from another URL of the same file (the final SHA-256
 *   decides); a server that ignores `Range` restarts it, and a resumed file that fails its SHA-256 is downloaded whole
 *   once more.
 * - Connection failures and server errors (5xx, 408, 429) are retried automatically after [retryDelaysMillis], each
 *   retry continuing the bytes on disk. HTTP errors such as a missing mirror and integrity failures fail at once, so the
 *   caller can fall back to the next source.
 * - Concurrent fetches of the same file share one download.
 *
 * Requests go through [transport] ([TransportUrlConnection]), so the network policy and the trust configuration of the
 * plugin keep applying. [fetch] keeps its signature, cancellation and error codes: [OperationCancelledException] for a
 * cancellation, [HttpStatusException] for an HTTP error, [IntegrityException] (`MODEL_CHECKSUM_MISMATCH`) for a size or
 * SHA-256 mismatch, `MODEL_DOWNLOAD_FAILED` when the retries are used up, `IO_FAILED` when the directory cannot be made.
 *
 * Files of the downloader before 1.6.1 (`<sha256>.part` and `<sha256>.bin` directly in the download directory) are
 * imported into the store when their artifact is fetched (plugin-center appendix A Q51 A), so an upgrade continues an
 * unfinished model download instead of starting over.
 *
 * zh-CN: 单个 [PinnedArtifact] 的可续传, 经校验的下载 (路线图 D23, D24), 基于 AutoJs6 与官方插件共用的下载库 (`org.autojs.plugin.downloadkit`, plugin-center
 * P13.2, D22, D26). 文件位于下载目录中 [STORE_DIRECTORY] 下的下载库仓库, 每个 SHA-256 一个条目 (`<sha256>/file`, `file.part`, `meta.json`): 已校验的文件
 * 不会重复下载, 每次复用都重新校验 SHA-256; 未完成的文件在连接中断, 取消, 进程被杀之后以 `Range` 续传 (服务器给过验证头时带 `If-Range`), 也可以从同一文件的
 * 另一个 URL 继续 (由最终 SHA-256 判定); 服务器忽略 `Range` 时从头开始, 续传得到的文件未通过 SHA-256 时整包重下一次. 连接失败与服务器错误 (5xx, 408, 429)
 * 按 [retryDelaysMillis] 自动重试, 每次重试都接着磁盘上的字节继续; HTTP 错误 (例如镜像缺失) 与完整性失败立即失败, 以便调用方回退到下一个来源. 同一文件的并发
 * 获取共享一次下载. 请求经 [transport] 发出 ([TransportUrlConnection]), 因此插件的网络策略与信任配置照常生效. [fetch] 的签名, 取消方式与错误码不变: 取消为
 * [OperationCancelledException], HTTP 错误为 [HttpStatusException], 大小或 SHA-256 不符为 [IntegrityException] (`MODEL_CHECKSUM_MISMATCH`), 重试用尽为
 * `MODEL_DOWNLOAD_FAILED`, 无法创建目录为 `IO_FAILED`. 1.6.1 之前的下载器留下的文件 (下载目录中的 `<sha256>.part` 与 `<sha256>.bin`) 在获取其资源时导入仓库
 * (plugin-center 附录 A Q51 A), 因此升级后未下完的模型接着下载, 不必从头开始.
 */
class Downloader(
    private val transport: HttpTransport,
    private val retryDelaysMillis: LongArray = DownloadFetcher.DEFAULT_RETRY_DELAYS_MILLIS,
    private val log: (String) -> Unit = {},
) {

    private class Kit(val store: DownloadStore, val fetcher: DownloadFetcher)

    /**
     * One store and one fetcher per store root, as the kit requires (fetches share a download only within one fetcher).
     * zh-CN: 每个仓库根目录一个仓库与一个取包器, 这是下载库的要求 (只有同一个取包器内的获取才共享下载).
     */
    private val kits = HashMap<String, Kit>()

    /** Connections the kit has open. zh-CN: 下载库已打开的连接. */
    private val connections: MutableSet<TransportUrlConnection> = ConcurrentHashMap.newKeySet()

    /** Callers waiting in [fetch], per store key. zh-CN: 在 [fetch] 中等待的调用方, 按仓库键计数. */
    private val callers = HashMap<String, Int>()

    private fun kit(directory: File): Kit {
        val root = storeDirectory(directory)
        return synchronized(kits) {
            kits.getOrPut(root.absoluteFile.normalize().path) {
                val store = DownloadStore(root, log = log, logName = "Model download store")
                Kit(store, DownloadFetcher(store, retryDelaysMillis, open = ::open, log = log, logName = "Model download"))
            }
        }
    }

    private fun open(url: URL): HttpURLConnection =
        TransportUrlConnection(url, transport, onConnect = { connections += it }, onDisconnect = { connections -= it })

    private fun request(artifact: PinnedArtifact) = DownloadRequest(artifact.url, artifact.sha256, artifact.size)

    /** The kit's store inside [directory]. zh-CN: [directory] 中的下载库仓库. */
    fun storeDirectory(directory: File): File = File(directory, STORE_DIRECTORY)

    /** Bytes still to download for [artifact]; 0 when a verified copy or a complete partial file exists. zh-CN: 尚需下载的字节数. */
    fun remainingBytes(artifact: PinnedArtifact, directory: File): Long {
        val store = kit(directory).store
        val key = request(artifact).key
        if (store.file(key).length() == artifact.size || legacyCompleteFile(artifact.sha256, directory).length() == artifact.size) return 0
        val partial = listOf(store.partFile(key), legacyPartialFile(artifact.sha256, directory)).map { it.length() }.filter { it <= artifact.size }.maxOrNull() ?: 0L
        return artifact.size - partial
    }

    /** Where the verified file of [artifact] lies in the store. zh-CN: [artifact] 已校验的文件在仓库中的位置. */
    fun completeFile(artifact: PinnedArtifact, directory: File): File = kit(directory).store.file(request(artifact).key)

    /** Where the unfinished download of [artifact] lies in the store. zh-CN: [artifact] 未完成的下载在仓库中的位置. */
    fun partialFile(artifact: PinnedArtifact, directory: File): File = kit(directory).store.partFile(request(artifact).key)

    /** The unfinished download the downloader before 1.6.1 left. zh-CN: 1.6.1 之前的下载器留下的未完成下载. */
    fun legacyPartialFile(sha256: String, directory: File): File = File(directory, "$sha256$LEGACY_PART_SUFFIX")

    /** The verified download the downloader before 1.6.1 left. zh-CN: 1.6.1 之前的下载器留下的已校验下载. */
    fun legacyCompleteFile(sha256: String, directory: File): File = File(directory, "$sha256$LEGACY_COMPLETE_SUFFIX")

    /**
     * Returns the verified file in the store; [onProgress] receives the bytes present so far (including a resumed prefix).
     * zh-CN: 返回仓库中已校验的文件; [onProgress] 收到当前已有的字节数 (含续传的前缀).
     */
    @Throws(ModelException::class)
    fun fetch(artifact: PinnedArtifact, directory: File, cancel: CancelToken, onProgress: (Long) -> Unit = {}): File {
        val kit = kit(directory)
        if (!kit.store.root.isDirectory && !kit.store.root.mkdirs()) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot create ${directory.name}")
        cancel.throwIfCancelled()
        val request = request(artifact)
        val key = request.key
        importLegacy(kit.store, request, artifact.sha256, directory)
        val cancelled = AtomicBoolean(false)
        synchronized(callers) { callers[key] = (callers[key] ?: 0) + 1 }
        val registration = cancel.register {
            cancelled.set(true)
            // The last caller of the file stops the transfer at once instead of at its next read, as before 1.6.1.
            // zh-CN: 该文件的最后一个调用方取消时立即停止传输 (与 1.6.1 之前相同), 而不是等到下一次读取.
            if (synchronized(callers) { callers[key] == 1 }) abort(request.urls)
        }
        val outcome = try {
            kit.fetcher.fetchBlocking(request, cancelled, DownloadListener.ofProgress { done, _ -> onProgress(done) })
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            cancelled.set(true)
            DownloadOutcome.Cancelled
        } finally {
            registration.close()
            synchronized(callers) {
                val count = callers[key] ?: 0
                if (count <= 1) callers.remove(key) else callers[key] = count - 1
            }
        }
        // A cancellation wins over the outcome, as before: a file finished meanwhile stays verified in the store.
        // zh-CN: 取消优先于结果, 与此前相同: 期间完成的文件已校验并留在仓库中.
        if (cancelled.get() || cancel.isCancelled) throw OperationCancelledException()
        return when (outcome) {
            is DownloadOutcome.Ready -> {
                onProgress(artifact.size)
                outcome.file
            }
            DownloadOutcome.Cancelled -> throw OperationCancelledException()
            is DownloadOutcome.Failed -> throw failureOf(outcome.failure, artifact)
        }
    }

    /**
     * Bytes on disk for the artifact [sha256] (store entry and files of the downloader before 1.6.1): non-zero after a
     * paused or interrupted download.
     * zh-CN: 资源 [sha256] 在磁盘上的字节数 (仓库条目与 1.6.1 之前的下载器的文件): 下载暂停或中断后不为零.
     */
    fun downloadedBytes(sha256: String, directory: File): Long {
        val legacy = listOf(legacyPartialFile(sha256, directory), legacyCompleteFile(sha256, directory)).sumOf(::sizeOf)
        val key = DownloadKey.normalizeSha256(sha256) ?: return legacy
        val store = kit(directory).store
        return legacy + sizeOf(store.file(key)) + sizeOf(store.partFile(key))
    }

    /** Deletes every download file of the artifact [sha256]. zh-CN: 删除资源 [sha256] 的全部下载文件. */
    fun discard(sha256: String, directory: File) {
        legacyPartialFile(sha256, directory).delete()
        legacyCompleteFile(sha256, directory).delete()
        DownloadKey.normalizeSha256(sha256)?.let { kit(directory).store.remove(it) }
    }

    /**
     * Download files in [directory] to free: those of artifacts not in [referenced] (SHA-256 values) or, with
     * [modifiedBefore], last written before it, and files in the store that belong to no entry. Entries in use stay.
     * zh-CN: [directory] 中应释放的下载文件: 不在 [referenced] (SHA-256 值) 中的资源的文件, 或 (给出 [modifiedBefore] 时) 最后写入早于它的文件, 以及仓库中
     * 不属于任何条目的文件. 正在使用的条目保留.
     */
    fun leftovers(directory: File, referenced: Set<String>, modifiedBefore: Long = Long.MIN_VALUE): List<File> {
        val result = ArrayList<File>()
        directory.listFiles().orEmpty().forEach { file ->
            if (file.name == STORE_DIRECTORY && file.isDirectory) return@forEach
            if (file.name.substringBefore('.') !in referenced || file.lastModified() < modifiedBefore) result += file
        }
        val store = kit(directory).store
        result += store.foreignFiles()
        store.entries().forEach { entry ->
            val entryDir = store.entryDir(entry.key)
            val written = entryDir.listFiles().orEmpty().maxOfOrNull { it.lastModified() } ?: entryDir.lastModified()
            if (!store.isInUse(entry.key) && (entry.key !in referenced || written < modifiedBefore)) result += entryDir
        }
        return result
    }

    /**
     * Q51 A: the files the downloader before 1.6.1 left for this artifact become the `.part` of its store entry. A
     * verified `.bin` is imported first (its full length is then checked without a request); a shorter `.part` next to it,
     * one longer than the catalog size and an empty one are dropped. A file whose entry is downloading stays for later.
     * zh-CN: Q51 A: 1.6.1 之前的下载器为该资源留下的文件成为其仓库条目的 `.part`. 先导入已校验的 `.bin` (之后不发请求直接按完整长度校验); 与之并存的更短的
     * `.part`, 超过目录大小的与空的文件被删除. 条目正在下载时文件留待以后导入.
     */
    private fun importLegacy(store: DownloadStore, request: DownloadRequest, sha256: String, directory: File) {
        for (legacy in listOf(legacyCompleteFile(sha256, directory), legacyPartialFile(sha256, directory))) {
            if (!legacy.isFile) continue
            val length = legacy.length()
            when (store.importPartial(request, legacy)) {
                DownloadStore.PartialImport.IMPORTED -> log("Model download ${request.key.take(12)}: continuing the $length B of ${legacy.name.substringAfterLast('.')} left by 1.6.0 or earlier.")
                DownloadStore.PartialImport.KEPT_EXISTING, DownloadStore.PartialImport.REJECTED -> {
                    legacy.delete()
                    log("Model download ${request.key.take(12)}: dropped the $length B of ${legacy.name.substringAfterLast('.')} left by 1.6.0 or earlier.")
                }
                DownloadStore.PartialImport.BUSY -> Unit
            }
        }
    }

    private fun abort(urls: List<String>) {
        val targets = urls.map { runCatching { URL(it).toString() }.getOrDefault(it) }.toSet()
        connections.filter { it.requestUrl in targets }.forEach { it.abort() }
    }

    private fun failureOf(failure: DownloadFailure, artifact: PinnedArtifact): ModelException {
        val status = failure.httpStatus
        return when (failure.kind) {
            // A missing mirror or another refusal: the caller falls back at once. Server errors were retried already.
            // zh-CN: 镜像缺失或其他拒绝: 调用方立即回退. 服务器错误已经重试过.
            DownloadFailureKind.HTTP_STATUS -> if (status != null && !failure.retryable && status != 416) HttpStatusException(status, artifact.url) else downloadFailed(failure)
            DownloadFailureKind.CHECKSUM_MISMATCH -> IntegrityException("Downloaded SHA-256 differs from the catalog: ${artifact.sha256.take(8)}")
            // A shorter file is a cut connection whose retries ran out; a longer one is another file.
            // zh-CN: 偏短的文件是重试用尽的中断连接; 偏长的是另一个文件.
            DownloadFailureKind.SIZE_MISMATCH -> if (failure.retryable) downloadFailed(failure) else {
                IntegrityException("Downloaded size ${failure.actualSize ?: failure.downloadedBytes} differs from the catalog size ${artifact.size}")
            }
            DownloadFailureKind.NETWORK, DownloadFailureKind.SECURITY, DownloadFailureKind.OTHER -> downloadFailed(failure)
        }
    }

    private fun downloadFailed(failure: DownloadFailure) = ModelException(
        SpeechErrorCodes.MODEL_DOWNLOAD_FAILED,
        buildString {
            append("Download failed: ").append(failure.message)
            failure.host?.let { host ->
                append(" (").append(host)
                if (failure.retries > 0) append(", ").append(failure.retries).append(if (failure.retries == 1) " retry" else " retries")
                append(')')
            }
        },
    )

    private fun sizeOf(file: File): Long = if (file.isFile) file.length() else 0L

    companion object {
        /** The kit's store inside the download directory. zh-CN: 下载目录中的下载库仓库. */
        const val STORE_DIRECTORY = "store"

        /** Suffixes of the downloader before 1.6.1. zh-CN: 1.6.1 之前的下载器的后缀. */
        const val LEGACY_PART_SUFFIX = ".part"
        const val LEGACY_COMPLETE_SUFFIX = ".bin"
    }
}
