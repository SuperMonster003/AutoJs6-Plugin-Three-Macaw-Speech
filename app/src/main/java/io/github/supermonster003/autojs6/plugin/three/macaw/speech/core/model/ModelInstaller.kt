package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.io.File

/**
 * Installs one catalog model version (roadmap D9, D23, D24): the Shared-Assets mirror first, then, when the mirror
 * fails for any reason other than cancellation or lack of space (HTTP error such as the 404 of a Release that does not
 * exist yet, timeout, size or SHA-256 mismatch, damaged archive), the sherpa-onnx upstream assets. Both paths check
 * the archive SHA-256 before unpacking and every file SHA-256 while unpacking into a staging directory, which the store
 * then activates atomically. Free space is checked before each path. Progress counts downloaded plus written bytes.
 * zh-CN: 安装单个目录模型版本 (路线图 D9, D23, D24): 先用 Shared-Assets 镜像; 镜像因取消与空间不足以外的任何原因失败时 (HTTP 错误,
 * 例如 Release 尚不存在时的 404, 超时, 大小或 SHA-256 不符, 归档损坏), 改用 sherpa-onnx 上游资源. 两条路径都在解包前校验归档 SHA-256,
 * 并在解包到暂存目录时校验每个文件的 SHA-256, 之后由存储原子激活. 每条路径开始前都检查可用空间. 进度为已下载字节加已写出字节.
 */
class ModelInstaller(
    private val store: ModelStore,
    private val downloader: Downloader,
    private val extractor: ArchiveExtractor,
    private val downloadDirectory: File,
    private val freeSpace: (File) -> Long,
    private val clock: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = {},
) {

    /** The installed model and how it arrived; [mirrorFailure] explains a fallback. zh-CN: 已安装的模型及来源; [mirrorFailure] 说明回退原因. */
    data class Result(val model: InstalledModel, val origin: String, val mirrorFailure: String?, val downloadedBytes: Long)

    @Throws(ModelException::class)
    fun install(model: CatalogModel, version: CatalogModelVersion, catalogVersion: Int, cancel: CancelToken, onProgress: (done: Long, total: Long) -> Unit): Result {
        store.installed(model.modelId)?.let { existing ->
            if (existing.versionCode == version.versionCode && existing.source == ModelStore.SOURCE_CATALOG &&
                existing.files == version.files && store.isIntact(existing)
            ) {
                return Result(existing, existing.origin, null, 0)
            }
        }
        val mirror = version.mirror ?: return installFromUpstream(model, version, catalogVersion, null, cancel, onProgress)
        val mirrorFailure = try {
            return installFromMirror(model, mirror, version, catalogVersion, cancel, onProgress)
        } catch (e: OperationCancelledException) {
            throw e
        } catch (e: InsufficientSpaceException) {
            throw e
        } catch (e: ModelException) {
            e.message ?: e.code
        }
        log("Mirror failed for ${model.modelId}: $mirrorFailure; falling back to the upstream release")
        return installFromUpstream(model, version, catalogVersion, mirrorFailure, cancel, onProgress)
    }

    private fun installFromMirror(model: CatalogModel, mirror: MirrorArchive, version: CatalogModelVersion, catalogVersion: Int, cancel: CancelToken, onProgress: (Long, Long) -> Unit): Result {
        requireSpace(downloader.remainingBytes(mirror, downloadDirectory) + version.installBytes)
        val total = mirror.size + version.installBytes
        val before = downloader.remainingBytes(mirror, downloadDirectory)
        val archive = downloader.fetch(mirror, downloadDirectory, cancel) { onProgress(it, total) }
        val staging = store.newStagingDirectory()
        try {
            try {
                extractor.extractMirror(archive, version.files, staging, cancel) { onProgress(mirror.size + it, total) }
            } catch (e: ModelException) {
                // A verified archive that does not unpack to the catalog files is useless: drop it before the fallback.
                if (e !is OperationCancelledException) archive.delete()
                throw e
            }
            val installed = commit(staging, model, version, catalogVersion, ModelStore.ORIGIN_MIRROR)
            archive.delete()
            return Result(installed, ModelStore.ORIGIN_MIRROR, null, before)
        } finally {
            staging.deleteRecursively()
        }
    }

    /** [mirrorFailure] is null for upstream-only versions. zh-CN: 仅上游版本的 [mirrorFailure] 为 null. */
    private fun installFromUpstream(model: CatalogModel, version: CatalogModelVersion, catalogVersion: Int, mirrorFailure: String?, cancel: CancelToken, onProgress: (Long, Long) -> Unit): Result {
        val remaining = version.upstream.sumOf { downloader.remainingBytes(it, downloadDirectory) }
        requireSpace(remaining + version.installBytes)
        val total = version.upstreamBytes + version.installBytes
        val files = version.files.associateBy { it.path }
        var done = 0L
        val downloaded = version.upstream.map { part ->
            val base = done
            downloader.fetch(part, downloadDirectory, cancel) { onProgress(base + it, total) }.also { done += part.size }
        }
        val staging = store.newStagingDirectory()
        try {
            version.upstream.zip(downloaded).forEach { (part, file) ->
                cancel.throwIfCancelled()
                val base = done
                when (part) {
                    is UpstreamPart.TarBz2 -> extractor.extractUpstream(file, part.members, files, staging, cancel) { onProgress(base + it, total) }
                    is UpstreamPart.RawFile -> extractor.installRawFile(file, part, files.getValue(part.path), staging)
                }
                done += when (part) {
                    is UpstreamPart.TarBz2 -> part.members.values.sumOf { files.getValue(it).size }
                    is UpstreamPart.RawFile -> part.size
                }
                onProgress(done, total)
            }
            val missing = version.files.filter { !File(staging, it.path).isFile }
            if (missing.isNotEmpty()) throw IntegrityException("Upstream assets lack ${missing.size} catalog files")
            val installed = commit(staging, model, version, catalogVersion, ModelStore.ORIGIN_UPSTREAM)
            downloaded.forEach { it.delete() }
            return Result(installed, ModelStore.ORIGIN_UPSTREAM, mirrorFailure, remaining)
        } catch (e: ModelException) {
            if (e !is OperationCancelledException && e !is InsufficientSpaceException) downloaded.forEach { it.delete() }
            throw e
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun commit(staging: File, model: CatalogModel, version: CatalogModelVersion, catalogVersion: Int, origin: String): InstalledModel =
        store.commit(
            staging,
            InstalledModel(
                modelId = model.modelId,
                versionCode = version.versionCode,
                source = ModelStore.SOURCE_CATALOG,
                origin = origin,
                kind = model.kind,
                modelType = version.modelType,
                names = model.displayName,
                languages = version.languages,
                sampleRate = version.sampleRate,
                speakers = version.speakers,
                license = version.license,
                minSherpaOnnx = version.minSherpaOnnx,
                runtimeConfig = version.runtimeConfig,
                catalogVersion = catalogVersion,
                installedAtMillis = clock(),
                files = version.files,
                directory = staging,
            ),
        )

    /** Download bytes plus installed bytes plus a margin must fit. zh-CN: 下载字节, 安装字节与余量之和必须放得下. */
    private fun requireSpace(bytes: Long) {
        downloadDirectory.mkdirs()
        val available = minOf(freeSpace(downloadDirectory), freeSpace(store.root))
        val required = bytes + SPACE_MARGIN_BYTES
        if (available < required) throw InsufficientSpaceException(required, available)
    }

    companion object {
        /** Head room kept free on top of every install. zh-CN: 每次安装之外预留的空间. */
        const val SPACE_MARGIN_BYTES = 32L * 1024 * 1024
    }
}
