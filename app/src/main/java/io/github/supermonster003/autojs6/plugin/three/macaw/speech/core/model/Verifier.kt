package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * The integrity checks of roadmap D24, in one place: an archive must match its pinned size and SHA-256 before it is
 * unpacked, and every installed file must match its `files[]` record afterwards. [ArchiveExtractor] applies the
 * file-level check while writing; [verifyInstalled] repeats it on demand for a model already on disk.
 * zh-CN: 路线图 D24 的完整性校验集中于此: 归档在解包前必须符合固定的大小与 SHA-256, 解包后每个安装文件都必须符合其 `files[]` 记录.
 * [ArchiveExtractor] 在写入时执行文件级校验; [verifyInstalled] 按需对已在磁盘上的模型重复该校验.
 */
object Verifier {

    private const val BUFFER_SIZE = 64 * 1024

    /** SHA-256 of a file, checking [cancel] between blocks. zh-CN: 文件的 SHA-256, 块之间检查 [cancel]. */
    fun sha256(file: File, cancel: CancelToken? = null): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                cancel?.throwIfCancelled()
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    /** Whether [file] has exactly the pinned size and SHA-256 of [artifact]. zh-CN: [file] 是否恰好符合 [artifact] 的固定大小与 SHA-256. */
    fun matches(file: File, artifact: PinnedArtifact, cancel: CancelToken? = null): Boolean =
        file.isFile && file.length() == artifact.size && sha256(file, cancel) == artifact.sha256

    /** Installed paths below [directory] whose size or SHA-256 differ from [files]. zh-CN: [directory] 下大小或 SHA-256 与 [files] 不符的路径. */
    fun verifyInstalled(directory: File, files: List<CatalogFile>, cancel: CancelToken? = null): List<String> = files.filter { file ->
        val path = File(directory, file.path)
        !path.isFile || path.length() != file.size || sha256(path, cancel) != file.sha256
    }.map { it.path }

    /** Cheap presence and size check of every installed file. zh-CN: 对每个安装文件做低成本的存在性与大小检查. */
    fun sizesMatch(directory: File, files: List<CatalogFile>): Boolean = files.all { file ->
        val path = File(directory, file.path)
        path.isFile && path.length() == file.size
    }
}
