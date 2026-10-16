package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipFile

/** A file written by the extractor, with its measured size and SHA-256. zh-CN: 解包器写出的文件, 带实测大小与 SHA-256. */
data class ExtractedFile(val name: String, val size: Long, val sha256: String, val file: File)

/**
 * Unpacks model archives into a staging directory (roadmap D23):
 * - mirror archives: the canonical uncompressed USTAR of Shared-Assets, each member checked against `files[]`;
 * - upstream `tar.bz2` parts: only the catalog's `members` are written, under their mapped installed paths;
 * - imports (`tar`, `tar.bz2`, `zip`, or a directory tree): every regular file is written to a numbered blob first and
 *   only renamed to a validated path later, so no archive name ever touches the file system unchecked.
 * Every write is bounded by the expected or the remaining size before any byte lands (oversized members, zip bombs),
 * hashed while written, and synced; truncated data is an error.
 * zh-CN: 把模型归档解包到暂存目录 (路线图 D23): 镜像归档为 Shared-Assets 的规范未压缩 USTAR, 每个成员都对照 `files[]`; 上游 `tar.bz2`
 * 只写出目录的 `members`, 落在映射后的安装路径; 导入 (`tar`, `tar.bz2`, `zip` 或目录树) 先把每个普通文件写成编号的块文件, 之后才改名为
 * 已校验的路径, 因此归档中的名称从不未经检查就接触文件系统. 每次写入在落盘前都受预期大小或剩余额度约束 (超大成员, zip 炸弹), 边写边计算
 * 哈希并同步到磁盘; 数据被截断即报错.
 */
class ArchiveExtractor {

    /** Installs every member of a mirror archive whose own SHA-256 was already verified. zh-CN: 安装已校验归档中的每个成员. */
    fun extractMirror(archive: File, files: List<CatalogFile>, target: File, cancel: CancelToken, onProgress: (Long) -> Unit = {}) {
        val expected = files.associateBy { it.path }
        val seen = HashSet<String>()
        var written = 0L
        BufferedInputStream(FileInputStream(archive), BUFFER_SIZE).use { input ->
            val reader = TarReader(input, canonical = true)
            while (true) {
                cancel.throwIfCancelled()
                val entry = reader.next() ?: break
                val file = expected[entry.name] ?: throw ArchiveFormatException("Unexpected archive member ${entry.name}")
                if (entry.size != file.size) throw IntegrityException("Archive member ${file.path} has ${entry.size} bytes instead of ${file.size}")
                seen += file.path
                try {
                    writeChecked(reader.entryStream(), installTarget(target, file.path), file.size, file.sha256, cancel) { written += it; onProgress(written) }
                } catch (e: ModelException) {
                    throw e
                } catch (e: IOException) {
                    throw ArchiveFormatException("Archive is truncated: ${e.message}", e)
                }
            }
        }
        val missing = expected.keys - seen
        if (missing.isNotEmpty()) throw IntegrityException("Archive lacks ${missing.size} catalog files")
    }

    /**
     * Installs the mapped [members] of an upstream `tar.bz2` (`preparation._stage_members`): selected members must be
     * regular files of exactly the catalog size and SHA-256; everything else is skipped without touching the disk.
     * zh-CN: 安装上游 `tar.bz2` 中映射的 [members] (`preparation._stage_members`): 选中的成员必须是大小与 SHA-256 均与目录一致的普通文件;
     * 其他成员被跳过, 不写磁盘.
     */
    fun extractUpstream(archive: File, members: Map<String, String>, files: Map<String, CatalogFile>, target: File, cancel: CancelToken, onProgress: (Long) -> Unit = {}) {
        val pending = HashMap(members)
        var written = 0L
        Bzip2InputStream(BufferedInputStream(FileInputStream(archive), BUFFER_SIZE)).use { input ->
            val reader = TarReader(input, canonical = false)
            while (pending.isNotEmpty()) {
                cancel.throwIfCancelled()
                val entry = try {
                    reader.next()
                } catch (e: IOException) {
                    throw ArchiveFormatException("Upstream archive is damaged: ${e.message}", e)
                } ?: break
                val path = pending.remove(entry.name)
                if (path == null) {
                    if (entry.name in members) throw ArchiveFormatException("Upstream archive contains a selected member twice: ${entry.name}")
                    continue
                }
                if (!entry.isRegularFile) throw ArchiveFormatException("Selected upstream members must be regular files: ${entry.name}")
                val file = files[path] ?: throw ArchiveFormatException("Upstream member maps outside the files array: $path")
                if (entry.size != file.size) throw IntegrityException("Upstream member ${entry.name} has ${entry.size} bytes instead of ${file.size}")
                try {
                    writeChecked(reader.entryStream(), installTarget(target, path), file.size, file.sha256, cancel) { written += it; onProgress(written) }
                } catch (e: ModelException) {
                    throw e
                } catch (e: IOException) {
                    throw ArchiveFormatException("Upstream archive is damaged: ${e.message}", e)
                }
            }
        }
        if (pending.isNotEmpty()) throw IntegrityException("Upstream archive lacks selected members: ${pending.keys.sorted().joinToString()}")
    }

    /** Moves a verified raw upstream download to its installed path. zh-CN: 把已校验的上游单文件移动到安装路径. */
    fun installRawFile(downloaded: File, part: UpstreamPart.RawFile, file: CatalogFile, target: File) {
        if (part.size != file.size || part.sha256 != file.sha256) throw IntegrityException("Upstream file ${file.path} differs from its catalog record")
        val destination = installTarget(target, file.path)
        destination.parentFile?.mkdirs()
        if (destination.exists() || !downloaded.renameTo(destination)) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot place ${file.path}")
    }

    /**
     * Bounds of one import: total bytes and file count; [overflow] builds the error when the bytes run out (for
     * example "not enough free space").
     * zh-CN: 单次导入的上限: 总字节数与文件数; 字节用尽时由 [overflow] 构造错误 (例如 "可用空间不足").
     */
    class ImportLimits(
        val maxTotalBytes: Long,
        val maxEntries: Int = CatalogRules.MAX_FILES,
        val overflow: () -> ModelException = { ArchiveFormatException("Archive content exceeds $maxTotalBytes bytes") },
    )

    /**
     * Writes every regular file of an import archive (`tar`, `tar.bz2` or `zip`, detected by content) to numbered blobs
     * in [blobs], within [limits].
     * zh-CN: 把导入归档 (`tar`, `tar.bz2` 或 `zip`, 按内容识别) 中的每个普通文件写成 [blobs] 中的编号块文件, 不超过 [limits].
     */
    fun extractForImport(archive: File, blobs: File, limits: ImportLimits, cancel: CancelToken, onProgress: (Long) -> Unit = {}): List<ExtractedFile> {
        val sink = BlobSink(blobs, limits, cancel, onProgress)
        val format = detectFormat(archive)
        try {
            when (format) {
                ImportFormat.ZIP -> ZipFile(archive).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        if (entry.size > CatalogRules.MAX_FILE_BYTES) throw ArchiveFormatException("Archive member is too large: ${entry.name}")
                        zip.getInputStream(entry).use { sink.accept(entry.name, it) }
                    }
                }
                ImportFormat.TAR, ImportFormat.TAR_BZ2 -> {
                    val raw = BufferedInputStream(FileInputStream(archive), BUFFER_SIZE)
                    val input: InputStream = if (format == ImportFormat.TAR_BZ2) Bzip2InputStream(raw) else raw
                    input.use {
                        val reader = TarReader(it, canonical = false)
                        while (true) {
                            cancel.throwIfCancelled()
                            val entry = reader.next() ?: break
                            if (entry.isRegularFile) sink.accept(entry.name, reader.entryStream())
                        }
                    }
                }
            }
        } catch (e: ModelException) {
            throw e
        } catch (e: IOException) {
            throw ArchiveFormatException("Archive is damaged: ${e.message}", e)
        }
        return sink.files
    }

    /** The same blob extraction for a directory tree (SAF or a plain directory). zh-CN: 对目录树 (SAF 或普通目录) 做同样的块文件解包. */
    fun extractTree(tree: ImportTree, blobs: File, limits: ImportLimits, cancel: CancelToken, onProgress: (Long) -> Unit = {}): List<ExtractedFile> {
        val sink = BlobSink(blobs, limits, cancel, onProgress)
        try {
            tree.forEachFile { path, open -> open().use { sink.accept(path, it) } }
        } catch (e: ModelException) {
            throw e
        } catch (e: IOException) {
            throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot read the import directory: ${e.message}", e)
        }
        return sink.files
    }

    enum class ImportFormat { TAR, TAR_BZ2, ZIP }

    /** Detects the import format by magic bytes. zh-CN: 按魔数识别导入格式. */
    fun detectFormat(archive: File): ImportFormat {
        val head = ByteArray(CatalogRules.BLOCK)
        val count = FileInputStream(archive).use { input ->
            var offset = 0
            while (offset < head.size) {
                val read = input.read(head, offset, head.size - offset)
                if (read < 0) break
                offset += read
            }
            offset
        }
        fun at(offset: Int, text: String) = count >= offset + text.length && (text.indices).all { head[offset + it].toInt() == text[it].code }
        return when {
            at(0, "BZh") -> ImportFormat.TAR_BZ2
            at(0, "PK") && count >= 4 && head[2].toInt() in listOf(3, 5) && head[3].toInt() in listOf(4, 6) -> ImportFormat.ZIP
            at(257, "ustar") -> ImportFormat.TAR
            else -> throw ArchiveFormatException("Unsupported archive format: expected tar, tar.bz2 or zip")
        }
    }

    private inner class BlobSink(
        private val directory: File,
        private val limits: ImportLimits,
        private val cancel: CancelToken,
        private val onProgress: (Long) -> Unit,
    ) {
        val files = ArrayList<ExtractedFile>()
        private var total = 0L

        fun accept(name: String, input: InputStream) {
            cancel.throwIfCancelled()
            if (files.size >= limits.maxEntries) throw ArchiveFormatException("Archive contains more than ${limits.maxEntries} files")
            if (!directory.isDirectory && !directory.mkdirs()) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot create the import directory")
            val blob = File(directory, files.size.toString())
            val limit = minOf(limits.maxTotalBytes - total, CatalogRules.MAX_FILE_BYTES)
            val (size, sha256) = try {
                copyHashed(input, blob, limit, cancel) { total += it; onProgress(total) }
            } catch (e: IntegrityException) {
                throw if (limit < CatalogRules.MAX_FILE_BYTES) limits.overflow() else ArchiveFormatException("Archive member is too large: $name")
            }
            files += ExtractedFile(name, size, sha256, blob)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024

        /**
         * Resolves an installed path below [root], refusing anything that escapes it even after validation.
         * zh-CN: 在 [root] 下解析安装路径; 即使已经校验, 仍拒绝任何越出 [root] 的结果.
         */
        fun installTarget(root: File, path: String): File {
            if (!CatalogRules.isInstallPath(path)) throw ArchiveFormatException("Unsafe installed path: $path")
            val base = root.canonicalFile
            val file = File(base, path)
            val canonical = file.canonicalFile
            if (!canonical.path.startsWith(base.path + File.separator)) throw ArchiveFormatException("Path escapes the model directory: $path")
            return file
        }

        /**
         * Copies exactly [size] bytes into a new file and checks them against [sha256]; a longer or shorter source is
         * an error.
         * zh-CN: 向新文件写入恰好 [size] 个字节并对照 [sha256]; 源数据过长或过短都会报错.
         */
        fun writeChecked(input: InputStream, destination: File, size: Long, sha256: String, cancel: CancelToken, onBytes: (Long) -> Unit) {
            destination.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot create ${it.name}") }
            if (destination.exists()) throw ArchiveFormatException("Duplicate installed path ${destination.name}")
            val (written, digest) = copyHashed(input, destination, size, cancel, onBytes)
            if (written != size) throw IntegrityException("${destination.name} has $written bytes instead of $size")
            if (digest != sha256) throw IntegrityException("SHA-256 of ${destination.name} differs from the catalog")
        }

        /** Copies at most [limit] bytes (more is an error), syncs the file, returns size and SHA-256. */
        private fun copyHashed(input: InputStream, destination: File, limit: Long, cancel: CancelToken, onBytes: (Long) -> Unit): Pair<Long, String> {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    cancel.throwIfCancelled()
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    if (total + read > limit) throw IntegrityException("${destination.name} exceeds its expected size")
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    total += read
                    onBytes(read.toLong())
                }
                output.flush()
                output.fd.sync()
            }
            return total to digest.digest().toHex()
        }
    }
}

/**
 * A directory tree to import, as relative paths with `/` separators (a plain directory in tests, a SAF tree on the
 * device).
 * zh-CN: 待导入的目录树, 以 `/` 分隔的相对路径表示 (测试中为普通目录, 设备上为 SAF 目录树).
 */
fun interface ImportTree {
    fun forEachFile(visitor: (path: String, open: () -> InputStream) -> Unit)
}

/** [ImportTree] over a local directory; symbolic links are not followed. zh-CN: 本地目录的 [ImportTree]; 不跟随符号链接. */
class FileImportTree(private val root: File) : ImportTree {
    override fun forEachFile(visitor: (path: String, open: () -> InputStream) -> Unit) {
        val base = root.canonicalFile
        fun walk(directory: File, prefix: String) {
            directory.listFiles().orEmpty().sortedBy { it.name }.forEach { child ->
                val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                val canonical = child.canonicalFile
                if (!canonical.path.startsWith(base.path + File.separator)) return@forEach
                when {
                    canonical.isDirectory -> walk(child, path)
                    canonical.isFile -> visitor(path) { FileInputStream(child) }
                }
            }
        }
        walk(root, "")
    }
}
