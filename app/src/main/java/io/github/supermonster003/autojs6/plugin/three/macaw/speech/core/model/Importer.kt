package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File
import java.util.Locale

/**
 * Imports a model from a local archive (`tar`, `tar.bz2`, `zip`) or a directory tree such as a SAF folder (roadmap
 * D23). Every regular file is first written to a numbered blob and hashed. When the blobs contain every `files[]` entry
 * of a catalog version (matched by size and SHA-256, whatever their names), the model is installed under its catalog
 * identity, paths and metadata, after the same license rule as downloads (D11). Otherwise the caller must name the
 * kind, model type, display name, languages and an id outside the catalog form `<kind>.<name>`; the files keep their
 * archive paths (one common top directory stripped), each validated like a catalog path. The runtime roles of such
 * models are resolved when they are loaded (roadmap P3 / P4).
 * zh-CN: 从本地归档 (`tar`, `tar.bz2`, `zip`) 或目录树 (例如 SAF 文件夹) 导入模型 (路线图 D23). 每个普通文件先写成编号块文件并计算哈希.
 * 若块文件包含某个目录版本的全部 `files[]` 条目 (按大小与 SHA-256 匹配, 不论名称), 则在执行与下载相同的许可规则 (D11) 后, 以目录身份,
 * 路径与元数据安装. 否则调用方必须给出种类, 模型类型, 显示名, 语言, 以及不采用目录形式 `<kind>.<name>` 的 ID; 文件保留归档内的路径
 * (去掉共同的顶层目录), 每个路径都按目录路径的规则校验. 此类模型的运行时角色在加载时解析 (路线图 P3 / P4).
 */
class Importer(
    private val store: ModelStore,
    private val extractor: ArchiveExtractor,
    private val licensePolicy: LicensePolicy,
    private val freeSpace: (File) -> Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** What the caller says about the import (`SpeechOptionKeys`). zh-CN: 调用方对导入的说明 (`SpeechOptionKeys`). */
    data class Request(
        val modelId: String? = null,
        val kind: String? = null,
        val modelType: String? = null,
        val name: String? = null,
        val languages: List<String> = emptyList(),
        val acceptLicense: Boolean = false,
    )

    @Throws(ModelException::class)
    fun importArchive(archive: File, request: Request, catalog: ModelCatalog, cancel: CancelToken, onProgress: (Long) -> Unit = {}): InstalledModel =
        run(request, catalog) { blobs, limits -> extractor.extractForImport(archive, blobs, limits, cancel, onProgress) }

    @Throws(ModelException::class)
    fun importTree(tree: ImportTree, request: Request, catalog: ModelCatalog, cancel: CancelToken, onProgress: (Long) -> Unit = {}): InstalledModel =
        run(request, catalog) { blobs, limits -> extractor.extractTree(tree, blobs, limits, cancel, onProgress) }

    private fun run(request: Request, catalog: ModelCatalog, extract: (File, ArchiveExtractor.ImportLimits) -> List<ExtractedFile>): InstalledModel {
        val requested = request.modelId?.let { catalog.model(it) }
        if (request.modelId != null && requested == null) validateCustomRequest(request)
        val staging = store.newStagingDirectory()
        try {
            val blobs = File(staging, ".blobs")
            val available = freeSpace(store.root) - ModelInstaller.SPACE_MARGIN_BYTES
            val limits = ArchiveExtractor.ImportLimits(
                maxTotalBytes = minOf(CatalogRules.MAX_INSTALL_BYTES, maxOf(available, 0)),
                overflow = {
                    if (available < CatalogRules.MAX_INSTALL_BYTES) InsufficientSpaceException(available + 1, available)
                    else ArchiveFormatException("Imported files exceed ${CatalogRules.MAX_INSTALL_BYTES} bytes")
                },
            )
            val extracted = extract(blobs, limits)
            if (extracted.isEmpty()) throw ArchiveFormatException("The import contains no files")
            val candidates = if (requested != null) listOf(requested) else catalog.models
            val match = candidates.firstNotNullOfOrNull { model ->
                model.versions.asReversed().firstOrNull { !it.revoked && contains(extracted, it.files) }?.let { model to it }
            }
            val model = if (match != null) {
                val (catalogModel, version) = match
                licensePolicy.require(catalogModel.modelId, version, request.acceptLicense)
                placeCatalogFiles(extracted, version.files, staging)
                InstalledModel(
                    modelId = catalogModel.modelId,
                    versionCode = version.versionCode,
                    source = ModelStore.SOURCE_CATALOG,
                    origin = ModelStore.ORIGIN_IMPORT,
                    kind = catalogModel.kind,
                    modelType = version.modelType,
                    names = catalogModel.displayName,
                    languages = version.languages,
                    sampleRate = version.sampleRate,
                    speakers = version.speakers,
                    license = version.license,
                    minSherpaOnnx = version.minSherpaOnnx,
                    runtimeConfig = version.runtimeConfig,
                    catalogVersion = catalog.catalogVersion,
                    installedAtMillis = clock(),
                    files = version.files,
                    directory = staging,
                )
            } else {
                if (requested != null) {
                    throw IntegrityException("The import does not contain the files of ${requested.modelId}")
                }
                if (request.modelId == null) throw NoCatalogMatchException()
                val files = placeCustomFiles(extracted, staging)
                InstalledModel(
                    modelId = request.modelId,
                    versionCode = IMPORTED_VERSION,
                    source = ModelStore.SOURCE_IMPORTED,
                    origin = ModelStore.ORIGIN_IMPORT,
                    kind = request.kind!!,
                    modelType = request.modelType!!,
                    names = mapOf("en" to request.name!!),
                    languages = request.languages,
                    sampleRate = null,
                    speakers = null,
                    license = null,
                    minSherpaOnnx = null,
                    runtimeConfig = emptyMap(),
                    catalogVersion = null,
                    installedAtMillis = clock(),
                    files = files,
                    directory = staging,
                )
            }
            File(staging, ".blobs").deleteRecursively()
            return store.commit(staging, model)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun validateCustomRequest(request: Request) {
        fun invalid(message: String): Nothing = throw ModelException(SpeechErrorCodes.INVALID_OPTIONS, message)
        val id = request.modelId!!
        if (!ModelStore.IMPORT_ID.matches(id) || CatalogRules.MODEL_ID.matches(id)) {
            invalid("Imported model ids use 2 to 64 of a-z, 0-9, '.', '_' and '-', and not the catalog form <kind>.<name>: $id")
        }
        if (request.kind !in CatalogRules.KINDS) invalid("kind must be one of ${CatalogRules.KINDS}")
        if (request.modelType.isNullOrBlank() || !MODEL_TYPE.matches(request.modelType)) invalid("modelType is required (for example vits or zipformer)")
        if (request.name.isNullOrBlank() || request.name.length > 128) invalid("name is required (at most 128 characters)")
        if (request.languages.isEmpty() || request.languages.size > 32 || request.languages.any { !CatalogRules.LANGUAGE.matches(it) }) {
            invalid("languages must list 1 to 32 language tags such as zh or en")
        }
    }

    /** Every catalog file present among the blobs (by size and SHA-256). zh-CN: 每个目录文件都出现在块文件中 (按大小与 SHA-256). */
    private fun contains(extracted: List<ExtractedFile>, files: List<CatalogFile>): Boolean {
        val available = extracted.groupingBy { it.size to it.sha256 }.eachCount().toMutableMap()
        return files.all { file -> available.containsKey(file.size to file.sha256) }
    }

    private fun placeCatalogFiles(extracted: List<ExtractedFile>, files: List<CatalogFile>, staging: File) {
        val byContent = extracted.groupBy { it.size to it.sha256 }
        files.forEach { file ->
            val source = byContent.getValue(file.size to file.sha256).first().file
            val target = ArchiveExtractor.installTarget(staging, file.path)
            target.parentFile?.mkdirs()
            // Identical content may serve two paths: move once, copy afterwards.
            if (source.exists()) {
                if (!source.renameTo(target)) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot place ${file.path}")
            } else {
                val first = files.first { it.size == file.size && it.sha256 == file.sha256 }
                ArchiveExtractor.installTarget(staging, first.path).copyTo(target)
            }
        }
        val damaged = Verifier.verifyInstalled(staging, files)
        if (damaged.isNotEmpty()) throw IntegrityException("Imported files differ from the catalog: ${damaged.joinToString()}")
    }

    private fun placeCustomFiles(extracted: List<ExtractedFile>, staging: File): List<CatalogFile> {
        val names = extracted.map { normalize(it.name) }
        val firstSegments = names.map { it.substringBefore('/', "") }.toSet()
        val strip = firstSegments.size == 1 && CatalogRules.PATH_SEGMENT.matches(firstSegments.first()) && names.all { '/' in it }
        val paths = names.map { if (strip) it.substringAfter('/') else it }
        val seen = HashSet<String>()
        paths.forEach { path ->
            if (!CatalogRules.isInstallPath(path)) throw ArchiveFormatException("Unsupported or unsafe path in the import: $path")
            if (!seen.add(path.lowercase(Locale.ROOT))) throw ArchiveFormatException("Duplicate path in the import: $path")
        }
        val folders = ModelCatalogParser.directoriesOf(paths).map { it.lowercase(Locale.ROOT) }.toSet()
        if (seen.any { it in folders }) throw ArchiveFormatException("A path of the import is both a file and a directory")
        val files = extracted.zip(paths).map { (blob, path) ->
            val target = ArchiveExtractor.installTarget(staging, path)
            target.parentFile?.mkdirs()
            if (!blob.file.renameTo(target)) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot place $path")
            CatalogFile(path, blob.size, blob.sha256)
        }.sortedBy { it.path }
        if (files.sumOf { it.size } > CatalogRules.MAX_INSTALL_BYTES) throw ArchiveFormatException("Imported files exceed the size limit")
        return files
    }

    /** Archive names use `/`; a leading `./` is dropped, nothing else is rewritten. zh-CN: 归档名称使用 `/`; 只去掉开头的 `./`. */
    private fun normalize(name: String): String {
        var value = name.replace('\\', '/')
        while (value.startsWith("./")) value = value.substring(2)
        return value
    }

    companion object {
        /** Imported models outside the catalog have no version of their own. zh-CN: 目录外的导入模型没有自己的版本号. */
        const val IMPORTED_VERSION = 0
        private val MODEL_TYPE = Regex("[a-z0-9][a-z0-9-]{0,63}")
    }
}
