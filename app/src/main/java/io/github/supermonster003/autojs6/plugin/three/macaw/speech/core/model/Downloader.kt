package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Resumable, verified download of one [PinnedArtifact] (roadmap D23, D24). Bytes go to `<sha256>.part` in [directory]
 * and become `<sha256>.bin` only after the size and SHA-256 match, so a killed process resumes from the partial file
 * with an HTTP `Range` request and a verified file is never fetched twice. A server that ignores `Range` restarts the
 * file; connection failures are retried [attempts] times; HTTP errors (a missing mirror) and integrity failures are
 * not, so the caller can fall back to the next source at once.
 * zh-CN: 单个 [PinnedArtifact] 的可续传, 经校验的下载 (路线图 D23, D24). 字节写入 [directory] 中的 `<sha256>.part`, 大小与 SHA-256 均一致后
 * 才改名为 `<sha256>.bin`, 因此进程被杀后以 HTTP `Range` 请求从部分文件续传, 已校验的文件不会重复下载. 服务器忽略 `Range` 时从头开始;
 * 连接失败重试 [attempts] 次; HTTP 错误 (镜像缺失) 与完整性失败不重试, 以便调用方立即回退到下一个来源.
 */
class Downloader(
    private val transport: HttpTransport,
    private val attempts: Int = 3,
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) {

    /** Bytes still to download for [artifact]; 0 when a verified copy or a complete partial file exists. zh-CN: 尚需下载的字节数. */
    fun remainingBytes(artifact: PinnedArtifact, directory: File): Long {
        if (completeFile(artifact, directory).length() == artifact.size) return 0
        val partial = partialFile(artifact, directory).length()
        return if (partial in 0..artifact.size) artifact.size - partial else artifact.size
    }

    fun completeFile(artifact: PinnedArtifact, directory: File): File = File(directory, "${artifact.sha256}.bin")

    fun partialFile(artifact: PinnedArtifact, directory: File): File = File(directory, "${artifact.sha256}.part")

    /**
     * Returns the verified file; [onProgress] receives the bytes present so far (including a resumed prefix).
     * zh-CN: 返回已校验的文件; [onProgress] 收到当前已有的字节数 (含续传的前缀).
     */
    @Throws(ModelException::class)
    fun fetch(artifact: PinnedArtifact, directory: File, cancel: CancelToken, onProgress: (Long) -> Unit = {}): File {
        if (!directory.isDirectory && !directory.mkdirs()) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot create ${directory.name}")
        val complete = completeFile(artifact, directory)
        if (complete.isFile) {
            if (Verifier.matches(complete, artifact, cancel)) {
                onProgress(artifact.size)
                return complete
            }
            complete.delete()
        }
        val partial = partialFile(artifact, directory)
        var failure: Exception? = null
        for (attempt in 0 until attempts) {
            cancel.throwIfCancelled()
            if (attempt > 0) sleep(RETRY_DELAY_MILLIS * attempt)
            try {
                transfer(artifact, partial, cancel, onProgress)
                failure = null
                break
            } catch (e: ModelException) {
                throw e
            } catch (e: IOException) {
                cancel.throwIfCancelled()
                failure = e
            }
        }
        failure?.let { throw ModelException(SpeechErrorCodes.MODEL_DOWNLOAD_FAILED, "Download failed: ${it.message}", it) }
        cancel.throwIfCancelled()
        if (!Verifier.matches(partial, artifact, cancel)) {
            partial.delete()
            throw IntegrityException("Downloaded size or SHA-256 differs from the catalog: ${artifact.sha256.take(8)}")
        }
        if (!partial.renameTo(complete)) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot finish the download file")
        return complete
    }

    private fun transfer(artifact: PinnedArtifact, partial: File, cancel: CancelToken, onProgress: (Long) -> Unit) {
        var offset = if (partial.isFile) partial.length() else 0L
        if (offset > artifact.size) {
            partial.delete()
            offset = 0
        }
        if (offset == artifact.size) return
        transport.get(artifact.url, offset, cancel).use { response ->
            when (response.status) {
                HttpURLConnection.HTTP_OK -> {
                    // The server ignored Range: start over.
                    offset = 0
                    if (response.contentLength >= 0 && response.contentLength != artifact.size) {
                        throw IntegrityException("Content-Length ${response.contentLength} differs from the catalog size ${artifact.size}")
                    }
                }
                HttpURLConnection.HTTP_PARTIAL -> {
                    if (offset == 0L || !rangeMatches(response.contentRange, offset, artifact.size)) {
                        partial.delete()
                        throw IOException("Unexpected Content-Range ${response.contentRange}")
                    }
                    if (response.contentLength >= 0 && response.contentLength != artifact.size - offset) {
                        throw IOException("Unexpected partial Content-Length ${response.contentLength}")
                    }
                }
                416 -> {
                    partial.delete()
                    throw IOException("Range not satisfiable; restarting")
                }
                in 500..599 -> throw IOException("HTTP ${response.status}")
                else -> throw HttpStatusException(response.status, artifact.url)
            }
            val body = response.body ?: throw IOException("Empty response body")
            FileOutputStream(partial, offset > 0).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var total = offset
                onProgress(total)
                while (true) {
                    cancel.throwIfCancelled()
                    val read = body.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    total += read
                    if (total > artifact.size) {
                        output.close()
                        partial.delete()
                        throw IntegrityException("Download exceeds the catalog size ${artifact.size}")
                    }
                    output.write(buffer, 0, read)
                    onProgress(total)
                }
                output.flush()
                output.fd.sync()
                if (total < artifact.size) throw IOException("Connection ended at $total of ${artifact.size} bytes")
            }
        }
    }

    private fun rangeMatches(header: String?, offset: Long, size: Long): Boolean {
        val match = CONTENT_RANGE.matchEntire(header?.trim() ?: return false) ?: return false
        val (start, end, total) = match.destructured
        return start.toLongOrNull() == offset && end.toLongOrNull() == size - 1 && (total == "*" || total.toLongOrNull() == size)
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val RETRY_DELAY_MILLIS = 1000L
        val CONTENT_RANGE = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+|\\*)")
    }
}
