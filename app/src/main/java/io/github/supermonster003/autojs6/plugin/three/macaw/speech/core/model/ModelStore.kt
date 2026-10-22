package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * One installed model as recorded by its manifest. Catalog models keep the catalog metadata of their version (runtime
 * roles, license, languages), so they stay loadable when a later catalog deprecates or revokes the version.
 * zh-CN: 由清单记录的单个已安装模型. 目录模型保存其版本的目录元数据 (运行时角色, 许可, 语言), 因此后续目录弃用或撤回该版本时仍可加载.
 */
data class InstalledModel(
    val modelId: String,
    val versionCode: Int,
    /** `catalog` or `imported` (`SpeechContract.MODEL_SOURCES`). zh-CN: `catalog` 或 `imported`. */
    val source: String,
    /** How the bytes arrived: `mirror`, `upstream` or `import`. zh-CN: 字节的来源: `mirror`, `upstream` 或 `import`. */
    val origin: String,
    val kind: String,
    val modelType: String,
    val names: Map<String, String>,
    val languages: List<String>,
    val sampleRate: Int?,
    val speakers: Int?,
    val license: ModelLicense?,
    val minSherpaOnnx: String?,
    val runtimeConfig: Map<String, Any>,
    val catalogVersion: Int?,
    val installedAtMillis: Long,
    val files: List<CatalogFile>,
    val directory: File,
) {
    val installedBytes: Long get() = files.sumOf { it.size }

    /** Absolute path of an installed file for the runtime (roadmap P3 / P4). zh-CN: 供运行时使用的安装文件绝对路径. */
    fun resolve(path: String): File = ArchiveExtractor.installTarget(directory, path)
}

/**
 * Private model storage `filesDir/models/<modelId>/<versionCode>/` (roadmap D23). A version directory exists only
 * after its staged copy, written and verified in `.staging/`, was renamed into place together with its `.manifest.json`
 * (installed paths never start with a dot, so the manifest cannot collide with a model file); the rename is the commit.
 * Replacing a version moves the old directory to `.trash/` first and [reconcile] restores it if the process died in
 * between. `.state.json` holds the default model of each kind and the remembered license acceptances (roadmap D11),
 * written atomically. Pure JVM, no Android types.
 * zh-CN: 私有模型存储 `filesDir/models/<modelId>/<versionCode>/` (路线图 D23). 版本目录只有在 `.staging/` 中写好并校验的副本连同其
 * `.manifest.json` 一起改名到位后才存在 (安装路径从不以点开头, 清单不会与模型文件冲突); 改名即提交. 替换版本时先把旧目录移到 `.trash/`,
 * 若进程在中途死亡, [reconcile] 会将其恢复. `.state.json` 保存每种模型的默认模型与已记住的许可接受记录 (路线图 D11), 以原子方式写入.
 * 纯 JVM, 不含 Android 类型.
 */
class ModelStore(root: File, private val clock: () -> Long = System::currentTimeMillis) {

    val root: File = root.absoluteFile
    private val staging = File(this.root, ".staging")
    private val trash = File(this.root, ".trash")
    private val stateFile = File(this.root, ".state.json")
    private val lock = Any()
    private var cache: List<InstalledModel>? = null

    /** A remembered license acceptance. zh-CN: 已记住的许可接受记录. */
    data class LicenseAcceptance(val modelId: String, val versionCode: Int, val spdx: String, val acceptedAtMillis: Long)

    private data class State(val defaults: Map<String, String>, val licenses: List<LicenseAcceptance>)

    init {
        reconcile()
    }

    /**
     * Recovers from an interrupted operation: restores a replaced version whose successor never arrived, removes
     * staging and trash leftovers, version directories without a readable manifest, and defaults of missing models.
     * zh-CN: 从中断的操作中恢复: 恢复后继版本未到位的被替换版本, 删除暂存与回收站残留, 没有可读清单的版本目录, 以及指向缺失模型的默认设置.
     */
    fun reconcile() = synchronized(lock) {
        root.mkdirs()
        trash.listFiles().orEmpty().forEach { entry ->
            val parts = entry.name.split('@')
            val target = if (parts.size == 3) File(File(root, parts[0]), parts[1]) else null
            if (target != null && isModelId(parts[0]) && parts[1].toIntOrNull() != null && !target.exists() && readManifest(entry) != null) {
                target.parentFile?.mkdirs()
                if (!entry.renameTo(target)) entry.deleteRecursively()
            } else {
                entry.deleteRecursively()
            }
        }
        staging.deleteRecursively()
        root.listFiles().orEmpty().filter { it.isDirectory && !it.name.startsWith(".") }.forEach { modelDirectory ->
            if (!isModelId(modelDirectory.name)) {
                modelDirectory.deleteRecursively()
                return@forEach
            }
            modelDirectory.listFiles().orEmpty().forEach { versionDirectory ->
                if (versionDirectory.name.toIntOrNull() == null || readManifest(versionDirectory)?.modelId != modelDirectory.name) {
                    versionDirectory.deleteRecursively()
                }
            }
            if (modelDirectory.listFiles().isNullOrEmpty()) modelDirectory.delete()
        }
        cache = null
        val state = readState()
        val installed = scan().map { it.modelId }.toSet()
        val defaults = state.defaults.filterValues { it in installed }
        if (defaults != state.defaults) writeState(state.copy(defaults = defaults))
    }

    fun installed(): List<InstalledModel> = synchronized(lock) { cache ?: scan().also { cache = it } }

    /** The installed version of [modelId] (one per model). zh-CN: [modelId] 的已安装版本 (每个模型一个). */
    fun installed(modelId: String): InstalledModel? = installed().firstOrNull { it.modelId == modelId }

    /** A fresh staging directory on the same file system as the models. zh-CN: 与模型位于同一文件系统的新暂存目录. */
    fun newStagingDirectory(): File = synchronized(lock) {
        File(staging, UUID.randomUUID().toString()).also { if (!it.mkdirs()) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot create a staging directory") }
    }

    /**
     * Commits a fully written and verified [stagingDirectory] as [model]: writes the manifest, renames the directory
     * into place, replaces the previous copy of the same version and removes the other versions of the model.
     * zh-CN: 把已写完并校验的 [stagingDirectory] 提交为 [model]: 写入清单, 改名到位, 替换同版本的旧副本并删除该模型的其他版本.
     */
    fun commit(stagingDirectory: File, model: InstalledModel): InstalledModel = synchronized(lock) {
        val manifest = File(stagingDirectory, MANIFEST)
        writeAtomically(manifest, StrictJson.write(manifestJson(model)))
        val modelDirectory = File(root, model.modelId)
        modelDirectory.mkdirs()
        val target = File(modelDirectory, model.versionCode.toString())
        var displaced: File? = null
        if (target.exists()) {
            trash.mkdirs()
            displaced = File(trash, "${model.modelId}@${model.versionCode}@${UUID.randomUUID()}")
            if (!target.renameTo(displaced)) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot replace ${model.modelId}")
        }
        beforeCommitRename()
        if (!stagingDirectory.renameTo(target)) {
            displaced?.renameTo(target)
            throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot activate ${model.modelId}")
        }
        displaced?.deleteRecursively()
        modelDirectory.listFiles().orEmpty().filter { it.name != target.name }.forEach { discard(it, model.modelId) }
        cache = null
        model.copy(directory = target)
    }

    /** Deletes every installed version of [modelId]; false when none was installed. zh-CN: 删除 [modelId] 的全部已安装版本. */
    fun remove(modelId: String): Boolean = synchronized(lock) {
        val modelDirectory = File(root, modelId)
        if (!isModelId(modelId) || !modelDirectory.isDirectory) return@synchronized false
        val existed = installed(modelId) != null
        modelDirectory.listFiles().orEmpty().forEach { discard(it, modelId) }
        modelDirectory.delete()
        val state = readState()
        if (modelId in state.defaults.values) writeState(state.copy(defaults = state.defaults.filterValues { it != modelId }))
        cache = null
        existed
    }

    /** Explicit default model id per kind. zh-CN: 每种模型显式设置的默认模型 ID. */
    fun defaults(): Map<String, String> = synchronized(lock) { readState().defaults }

    fun setDefault(kind: String, modelId: String?) = synchronized(lock) {
        val state = readState()
        val defaults = LinkedHashMap(state.defaults)
        if (modelId == null) defaults.remove(kind) else defaults[kind] = modelId
        writeState(state.copy(defaults = defaults))
    }

    fun hasAcceptedLicense(modelId: String, versionCode: Int, spdx: String): Boolean = synchronized(lock) {
        readState().licenses.any { it.modelId == modelId && it.versionCode == versionCode && it.spdx == spdx }
    }

    fun rememberLicense(modelId: String, versionCode: Int, spdx: String) = synchronized(lock) {
        val state = readState()
        if (state.licenses.any { it.modelId == modelId && it.versionCode == versionCode && it.spdx == spdx }) return@synchronized
        writeState(state.copy(licenses = state.licenses + LicenseAcceptance(modelId, versionCode, spdx, clock())))
    }

    /**
     * Cheap check before use: every file is present with its recorded size. A failing model is damaged and reported as
     * not installed until it is installed again.
     * zh-CN: 使用前的低成本检查: 每个文件都存在且大小与记录一致. 未通过的模型视为已损坏, 在重新安装前报告为未安装.
     */
    fun isIntact(model: InstalledModel): Boolean = Verifier.sizesMatch(model.directory, model.files)

    /** Full SHA-256 check of every installed file; returns the paths that differ. zh-CN: 对每个安装文件做完整 SHA-256 校验, 返回不一致的路径. */
    fun verify(model: InstalledModel, cancel: CancelToken? = null): List<String> = Verifier.verifyInstalled(model.directory, model.files, cancel)

    /** Test hook between the two renames of a replacement. zh-CN: 替换过程两次改名之间的测试钩子. */
    internal var beforeCommitRename: () -> Unit = {}

    /** Removes a version for good; the trash name has no `@`, so [reconcile] never restores it. zh-CN: 永久删除一个版本. */
    private fun discard(directory: File, modelId: String) {
        trash.mkdirs()
        val moved = File(trash, "discarded-$modelId-${UUID.randomUUID()}")
        if (directory.renameTo(moved)) moved.deleteRecursively() else directory.deleteRecursively()
    }

    private fun scan(): List<InstalledModel> = root.listFiles().orEmpty()
        .filter { it.isDirectory && !it.name.startsWith(".") && isModelId(it.name) }
        .sortedBy { it.name }
        .mapNotNull { modelDirectory ->
            modelDirectory.listFiles().orEmpty()
                .mapNotNull { version -> readManifest(version)?.takeIf { it.modelId == modelDirectory.name && it.versionCode.toString() == version.name } }
                .maxByOrNull { it.versionCode }
        }

    private fun readManifest(directory: File): InstalledModel? {
        val file = File(directory, MANIFEST)
        if (!file.isFile || file.length() > MAX_MANIFEST_BYTES) return null
        return runCatching { parseManifest(StrictJson.parseObject(file.readBytes()), directory) }.getOrNull()
    }

    private fun readState(): State {
        if (!stateFile.isFile) return State(emptyMap(), emptyList())
        return runCatching {
            val json = StrictJson.parseObject(stateFile.readBytes())
            @Suppress("UNCHECKED_CAST")
            val defaults = (json["defaults"] as Map<String, Any?>).mapValues { it.value as String }.filter { it.key in KINDS && isModelId(it.value) }
            val licenses = (json["licenses"] as List<*>).map { raw ->
                val item = raw as Map<*, *>
                LicenseAcceptance(
                    modelId = item["modelId"] as String,
                    versionCode = (item["versionCode"] as JsonNumber).longOrNull()!!.toInt(),
                    spdx = item["spdx"] as String,
                    acceptedAtMillis = (item["acceptedAt"] as JsonNumber).longOrNull()!!,
                )
            }
            State(defaults, licenses)
        }.getOrElse { State(emptyMap(), emptyList()) }
    }

    private fun writeState(state: State) {
        val json = linkedMapOf(
            "format" to 1,
            "defaults" to state.defaults.toSortedMap(),
            "licenses" to state.licenses.map {
                linkedMapOf("modelId" to it.modelId, "versionCode" to it.versionCode, "spdx" to it.spdx, "acceptedAt" to it.acceptedAtMillis)
            },
        )
        writeAtomically(stateFile, StrictJson.write(json))
    }

    private fun writeAtomically(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temporary).use {
                it.write(bytes)
                it.flush()
                it.fd.sync()
            }
            if (!temporary.renameTo(file)) {
                // Windows (JVM tests) does not replace on rename.
                file.delete()
                if (!temporary.renameTo(file)) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot write ${file.name}")
            }
        } finally {
            temporary.delete()
        }
    }

    companion object {
        const val MANIFEST = ".manifest.json"
        const val SOURCE_CATALOG = "catalog"
        const val SOURCE_IMPORTED = "imported"
        const val ORIGIN_MIRROR = "mirror"
        const val ORIGIN_UPSTREAM = "upstream"
        const val ORIGIN_IMPORT = "import"
        private const val MAX_MANIFEST_BYTES = 4L * 1024 * 1024
        private val KINDS = setOf("asr", "kws", "punct", "tts", "vad")

        /** Catalog ids and the ids of imported models. zh-CN: 目录模型与导入模型的 ID. */
        val IMPORT_ID = Regex("[a-z0-9][a-z0-9._-]{1,63}")

        fun isModelId(value: String): Boolean = CatalogRules.MODEL_ID.matches(value) || IMPORT_ID.matches(value)

        private fun manifestJson(model: InstalledModel): Map<String, Any?> = linkedMapOf(
            "format" to 1,
            "modelId" to model.modelId,
            "versionCode" to model.versionCode,
            "source" to model.source,
            "origin" to model.origin,
            "kind" to model.kind,
            "modelType" to model.modelType,
            "names" to model.names.toSortedMap(),
            "languages" to model.languages,
            "sampleRate" to model.sampleRate,
            "speakers" to model.speakers,
            "license" to model.license?.let { linkedMapOf("spdx" to it.spdx, "commercialUse" to it.commercialUse.json, "noticeKey" to it.noticeKey) },
            "minSherpaOnnx" to model.minSherpaOnnx,
            "runtime" to model.runtimeConfig,
            "catalogVersion" to model.catalogVersion,
            "installedAt" to model.installedAtMillis,
            "files" to model.files.map { linkedMapOf("path" to it.path, "size" to it.size, "sha256" to it.sha256) },
        )

        private fun parseManifest(json: Map<String, Any?>, directory: File): InstalledModel {
            fun int(key: String): Int? = (json[key] as JsonNumber?)?.longOrNull()?.toInt()
            @Suppress("UNCHECKED_CAST")
            val license = (json["license"] as Map<String, Any?>?)?.let {
                ModelLicense(
                    spdx = it["spdx"] as String,
                    commercialUse = CommercialUse.entries.first { use -> use.json == it["commercialUse"] },
                    noticeKey = it["noticeKey"] as String,
                )
            }
            @Suppress("UNCHECKED_CAST")
            val runtime = (json["runtime"] as Map<String, Any?>).mapValues { (_, value) ->
                when (value) {
                    is String -> value
                    is List<*> -> value.map { it as String }
                    else -> error("Invalid runtime role")
                }
            }
            val files = (json["files"] as List<*>).map { raw ->
                val item = raw as Map<*, *>
                val path = item["path"] as String
                require(CatalogRules.isInstallPath(path)) { "Invalid installed path" }
                CatalogFile(path, (item["size"] as JsonNumber).longOrNull()!!, item["sha256"] as String)
            }
            require(json["format"] == JsonNumber("1") && files.isNotEmpty())
            @Suppress("UNCHECKED_CAST")
            return InstalledModel(
                modelId = json["modelId"] as String,
                versionCode = int("versionCode")!!,
                source = json["source"] as String,
                origin = json["origin"] as String,
                kind = json["kind"] as String,
                modelType = json["modelType"] as String,
                names = (json["names"] as Map<String, Any?>).mapValues { it.value as String },
                languages = (json["languages"] as List<*>).map { it as String },
                sampleRate = int("sampleRate"),
                speakers = int("speakers"),
                license = license,
                minSherpaOnnx = json["minSherpaOnnx"] as String?,
                runtimeConfig = runtime,
                catalogVersion = int("catalogVersion"),
                installedAtMillis = (json["installedAt"] as JsonNumber).longOrNull()!!,
                files = files,
                directory = directory,
            )
        }
    }
}
