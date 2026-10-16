package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * One model as `listModels` reports it (`SpeechModelKeys`, the script's `SpeechModelInfo`): the catalog entry merged
 * with the installed state, or an imported or built-in model.
 * zh-CN: `listModels` 报告的单个模型 (`SpeechModelKeys`, 脚本的 `SpeechModelInfo`): 合并安装状态后的目录条目, 或导入模型与内置模型.
 */
data class ModelEntry(
    val id: String,
    val kind: String,
    val modelType: String,
    val name: String,
    val languages: List<String>,
    val sizeBytes: Long,
    val installedBytes: Long?,
    val installed: Boolean,
    val isDefault: Boolean,
    val recommended: Boolean,
    val version: Int,
    val licenseSpdx: String,
    val commercialUse: CommercialUse,
    val noticeKey: String?,
    val noticeUrl: String?,
    val speakers: Int?,
    val sampleRate: Int?,
    val source: String,
)

/** Results of the operations that run on the model thread. zh-CN: 在模型线程上运行的操作的结果. */
sealed class OperationResult {
    /** [downloadedBytes]: bytes transferred by this operation (a resumed download counts only the rest). */
    data class Model(val entry: ModelEntry, val origin: String, val mirrorFailure: String?, val downloadedBytes: Long = 0) : OperationResult()
    data class Catalog(val catalogVersion: Int, val updated: Boolean) : OperationResult()

    /** [ModelManager.recycle]: bytes and files freed. zh-CN: [ModelManager.recycle]: 释放的字节数与文件数. */
    data class Recycled(val freedBytes: Long, val files: Int) : OperationResult()
}

/** Progress and completion of one operation id. zh-CN: 单个操作 ID 的进度与完成回调. */
interface OperationListener {
    fun onProgress(opId: Int, done: Long, total: Long)
    fun onCompleted(opId: Int, result: OperationResult)
    fun onFailed(opId: Int, error: ModelException)
}

/**
 * Model management behind `ISpeechPlugin` (roadmap P2.3 / P2.4): catalog queries, defaults, removal, and the long
 * operations (install, import, catalog refresh) on one dedicated thread, so only one download runs at a time
 * (roadmap D30) and the others wait in order. Each operation gets an id for progress, cancellation and its single
 * completion. [activity] observes running work so the Android layer can hold the data sync foreground service.
 * zh-CN: `ISpeechPlugin` 背后的模型管理 (路线图 P2.3 / P2.4): 目录查询, 默认模型, 删除, 以及在一条专用线程上执行的长操作 (安装, 导入,
 * 目录刷新), 因此同时只运行一个下载 (路线图 D30), 其他操作按顺序等待. 每个操作有一个 ID, 用于进度, 取消与唯一的完成回调. [activity]
 * 观察进行中的工作, 以便 Android 层持有数据同步前台服务.
 */
class ModelManager(
    val store: ModelStore,
    val catalogs: ModelCatalogRepository,
    private val installer: ModelInstaller,
    private val importer: Importer,
    private val licensePolicy: LicensePolicy,
    private val downloadDirectory: File,
    private val builtins: List<ModelEntry> = emptyList(),
    private val locale: () -> Locale = Locale::getDefault,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "MacawModelOperations").apply { isDaemon = true } },
    private val log: (String) -> Unit = {},
    /** Scratch directories [recycle] empties (the import copies). zh-CN: [recycle] 清空的临时目录 (导入副本). */
    private val scratchDirectories: List<File> = emptyList(),
) {

    /** What runs now, for the foreground notification. zh-CN: 当前运行的内容, 供前台通知使用. */
    data class Activity(val pending: Int, val title: String?, val done: Long, val total: Long)

    fun interface ActivityListener {
        fun onActivity(activity: Activity)
    }

    @Volatile
    var activity: ActivityListener? = null

    private class Operation(val id: Int, val title: String, val listener: OperationListener, val cleanup: () -> Unit, val modelId: String?) {
        val cancel = CancelToken()
        @Volatile var started = false

        /** Set by [cancel] when the partial download goes too (the screens' "cancel", not "pause"). */
        @Volatile var discard = false
        @Volatile var done = 0L
        @Volatile var total = 0L
    }

    private val ids = AtomicInteger(0)
    private val operations = ConcurrentHashMap<Int, Operation>()
    private val busyModels = ConcurrentHashMap.newKeySet<String>()

    /** Hook for roadmap P3 / P4: a model loaded by a session cannot be removed. zh-CN: 路线图 P3 / P4 的钩子: 会话加载中的模型不能删除. */
    @Volatile
    var isModelInUse: (String) -> Boolean = { false }

    init {
        executor.execute { runCatching { recycleDownloads() }.onFailure { log("Download recycling failed: $it") } }
    }

    // Queries -----------------------------------------------------------------------------------------------------------

    fun catalogVersion(): Int = catalogs.current().catalog.catalogVersion

    /** `listModels`: catalog models, imported models and built-ins, filtered. zh-CN: `listModels`: 筛选后的目录模型, 导入模型与内置模型. */
    fun list(kind: String? = null, language: String? = null, installed: Boolean? = null): List<ModelEntry> {
        val catalog = catalogs.current().catalog
        val present = store.installed().associateBy { it.modelId }
        val defaults = effectiveDefaults(catalog, present)
        val fromCatalog = catalog.models.map { entry(it, present[it.modelId], defaults) }
        val imported = present.values.filter { catalog.model(it.modelId) == null }.map { entry(it, defaults) }
        return (builtins + fromCatalog + imported).filter { entry ->
            (kind == null || entry.kind == kind) &&
                (language == null || entry.languages.any { ModelCatalog.languageMatches(it, language) }) &&
                (installed == null || entry.installed == installed)
        }
    }

    fun entry(modelId: String): ModelEntry? = list().firstOrNull { it.id == modelId }

    /** The default installed model of [kind]; see [effectiveDefaults]. zh-CN: [kind] 的默认已安装模型. */
    fun defaultModel(kind: String): InstalledModel? {
        val present = store.installed().associateBy { it.modelId }
        return effectiveDefaults(catalogs.current().catalog, present)[kind]?.let { present[it] }
    }

    /**
     * Explicit defaults when still installed and intact; otherwise the installed model the catalog recommends, then
     * the first installed one in catalog order, then the first imported one.
     * zh-CN: 显式默认模型仍已安装且完好时使用它; 否则依次取目录推荐的已安装模型, 目录顺序中第一个已安装模型, 第一个导入模型.
     */
    private fun effectiveDefaults(catalog: ModelCatalog, present: Map<String, InstalledModel>): Map<String, String> {
        val usable = present.filterValues { store.isIntact(it) }
        val explicit = store.defaults().filter { (kind, id) -> usable[id]?.kind == kind }
        return CatalogRules.KINDS.mapNotNull { kind ->
            val chosen = explicit[kind]
                ?: catalog.models.firstOrNull { it.kind == kind && it.recommended && it.modelId in usable }?.modelId
                ?: catalog.models.firstOrNull { it.kind == kind && it.modelId in usable }?.modelId
                ?: usable.values.filter { it.kind == kind }.minByOrNull { it.modelId }?.modelId
            chosen?.let { kind to it }
        }.toMap()
    }

    private fun entry(model: CatalogModel, installed: InstalledModel?, defaults: Map<String, String>): ModelEntry {
        val intact = installed != null && store.isIntact(installed)
        val version = if (intact) model.version(installed.versionCode) ?: model.latestVersion else model.installableVersion ?: model.latestVersion
        return ModelEntry(
            id = model.modelId,
            kind = model.kind,
            modelType = version.modelType,
            name = model.localizedName(locale()),
            languages = version.languages,
            sizeBytes = version.downloadBytes,
            installedBytes = if (intact) installed.installedBytes else null,
            installed = intact,
            isDefault = defaults[model.kind] == model.modelId,
            recommended = model.recommended,
            version = version.versionCode,
            licenseSpdx = version.license.spdx,
            commercialUse = version.license.commercialUse,
            noticeKey = version.license.noticeKey,
            noticeUrl = version.sourceUrl,
            speakers = version.speakers,
            sampleRate = version.sampleRate,
            source = ModelStore.SOURCE_CATALOG,
        )
    }

    private fun entry(model: InstalledModel, defaults: Map<String, String>): ModelEntry = ModelEntry(
        id = model.modelId,
        kind = model.kind,
        modelType = model.modelType,
        name = localized(model.names, locale()),
        languages = model.languages,
        sizeBytes = 0,
        installedBytes = model.installedBytes,
        installed = store.isIntact(model),
        isDefault = defaults[model.kind] == model.modelId,
        recommended = false,
        version = model.versionCode,
        licenseSpdx = model.license?.spdx ?: "LicenseRef-UserImported",
        commercialUse = model.license?.commercialUse ?: CommercialUse.UNKNOWN,
        noticeKey = model.license?.noticeKey,
        noticeUrl = null,
        speakers = model.speakers,
        sampleRate = model.sampleRate,
        source = model.source,
    )

    // Immediate mutations -----------------------------------------------------------------------------------------------

    /** `removeModel`: false when the known model was not installed. zh-CN: `removeModel`: 已知模型未安装时返回 false. */
    @Throws(ModelException::class)
    fun remove(modelId: String): Boolean {
        if (builtins.any { it.id == modelId }) throw ModelException(SpeechErrorCodes.INVALID_OPTIONS, "$modelId is part of the plugin and cannot be removed")
        if (catalogs.current().catalog.model(modelId) == null && store.installed(modelId) == null) {
            throw ModelException(SpeechErrorCodes.MODEL_MISSING, "Unknown model $modelId")
        }
        if (modelId in busyModels || isModelInUse(modelId)) throw ModelException(SpeechErrorCodes.SESSION_BUSY, "$modelId is in use")
        return store.remove(modelId)
    }

    /** `setDefaultModel`: the model must be installed and of [kind] when given. zh-CN: `setDefaultModel`: 模型须已安装且种类相符. */
    @Throws(ModelException::class)
    fun setDefault(modelId: String, kind: String?): ModelEntry {
        val installed = store.installed(modelId)?.takeIf { store.isIntact(it) }
            ?: throw ModelException(SpeechErrorCodes.MODEL_MISSING, "$modelId is not installed")
        if (kind != null && kind != installed.kind) throw ModelException(SpeechErrorCodes.INVALID_OPTIONS, "$modelId is a ${installed.kind} model, not $kind")
        store.setDefault(installed.kind, modelId)
        return entry(modelId)!!
    }

    // Operations ------------------------------------------------------------------------------------------------------

    /**
     * `installModel`: unknown ids and models without an installable version fail at once; everything else, including
     * the license check (roadmap D11), completes through [listener].
     * zh-CN: `installModel`: 未知 ID 与没有可安装版本的模型立即失败; 其余 (包括许可检查, 路线图 D11) 经 [listener] 完成.
     */
    @Throws(ModelException::class)
    fun install(modelId: String, acceptLicense: Boolean, listener: OperationListener, title: String? = null): Int {
        val model = catalogs.current().catalog.model(modelId)
            ?: throw ModelException(SpeechErrorCodes.MODEL_MISSING, "Unknown catalog model $modelId")
        model.installableVersion ?: throw ModelException(SpeechErrorCodes.MODEL_MISSING, "$modelId has no installable version")
        return enqueue(title ?: model.localizedName(locale()), listener, modelId = modelId) { operation ->
            // Re-read the catalog on the worker: a refresh may have finished meanwhile.
            val snapshot = catalogs.current().catalog
            val current = snapshot.model(modelId) ?: throw ModelException(SpeechErrorCodes.MODEL_MISSING, "Unknown catalog model $modelId")
            val version = current.installableVersion ?: throw ModelException(SpeechErrorCodes.MODEL_MISSING, "$modelId has no installable version")
            licensePolicy.require(modelId, version, acceptLicense)
            busyModels += modelId
            try {
                val result = installer.install(current, version, snapshot.catalogVersion, operation.cancel) { done, total -> progress(operation, done, total) }
                log("Installed $modelId v${version.versionCode} from ${result.origin}, ${result.downloadedBytes} bytes downloaded" +
                    (result.mirrorFailure?.let { " (mirror failed: $it)" } ?: ""))
                OperationResult.Model(entry(modelId)!!, result.origin, result.mirrorFailure, result.downloadedBytes)
            } finally {
                busyModels -= modelId
            }
        }
    }

    /**
     * `importModel`: [archive] is a private copy (this operation deletes it), filled by [prepare] on the model thread
     * when given, whose progress is measured against [totalBytes]; [cleanup] runs once in every outcome, also when the
     * operation is cancelled before it starts.
     * zh-CN: `importModel`: [archive] 是私有副本 (由本操作删除), 若给出 [prepare] 则在模型线程上由其写入, 进度以 [totalBytes] 计算;
     * [cleanup] 在任何结局下都恰好执行一次, 包括操作开始前即被取消.
     */
    fun importArchive(
        archive: File,
        request: Importer.Request,
        title: String,
        listener: OperationListener,
        prepare: ((CancelToken, (Long) -> Unit) -> Unit)? = null,
        totalBytes: Long = 0,
        cleanup: () -> Unit = {},
    ): Int =
        enqueue(title, listener, cleanup) { operation ->
            try {
                prepare?.invoke(operation.cancel) { progress(operation, it, totalBytes) }
                val model = importer.importArchive(archive, request, catalogs.current().catalog, operation.cancel) { progress(operation, it, 0) }
                OperationResult.Model(entry(model.modelId)!!, model.origin, null)
            } finally {
                archive.delete()
            }
        }

    /** Directory imports for the screens of roadmap P5 (SAF trees). zh-CN: 供路线图 P5 界面使用的目录导入 (SAF 目录树). */
    fun importTree(tree: ImportTree, request: Importer.Request, title: String, listener: OperationListener): Int =
        enqueue(title, listener) { operation ->
            val model = importer.importTree(tree, request, catalogs.current().catalog, operation.cancel) { progress(operation, it, 0) }
            OperationResult.Model(entry(model.modelId)!!, model.origin, null)
        }

    /** `refreshCatalog` (roadmap D24, manual only per D29). zh-CN: `refreshCatalog` (路线图 D24, 按 D29 仅手动触发). */
    fun refreshCatalog(title: String, listener: OperationListener): Int = enqueue(title, listener) { operation ->
        val result = catalogs.refresh(operation.cancel)
        log("Catalog refresh: version ${result.catalogVersion}, updated ${result.updated}")
        OperationResult.Catalog(result.catalogVersion, result.updated)
    }

    /**
     * An operation id for work outside the model thread (syntheses, roadmap P4.3): ids stay unique across every operation
     * of `ISpeechPlugin`, so `cancelOperation` reaches the right one.
     * zh-CN: 模型线程以外工作 (合成, 路线图 P4.3) 的操作 ID: ID 在 `ISpeechPlugin` 的全部操作中保持唯一, `cancelOperation` 因此能找到正确的操作.
     */
    fun nextOperationId(): Int = ids.incrementAndGet()

    /**
     * Cancels a queued or running operation; unknown ids are ignored. A cancelled download keeps its partial file, so
     * installing the model again resumes it (the screens' "pause"); [discardPartial] deletes the partial files as well,
     * once the operation has stopped writing (the screens' "cancel").
     * zh-CN: 取消排队或运行中的操作; 忽略未知 ID. 被取消的下载保留部分文件, 再次安装即续传 (界面的 "暂停"); [discardPartial] 在操作停止写入后
     * 同时删除部分文件 (界面的 "取消").
     */
    fun cancel(opId: Int, discardPartial: Boolean = false) {
        val operation = operations[opId] ?: return
        if (discardPartial) operation.discard = true
        operation.cancel.cancel()
        val unstarted = synchronized(operation) { !operation.started && operations.remove(opId) != null }
        if (unstarted) {
            runCatching(operation.cleanup)
            if (discardPartial) operation.modelId?.let(::discardDownload)
            runCatching { operation.listener.onFailed(opId, OperationCancelledException()) }
            publishActivity()
        }
    }

    /**
     * Bytes already downloaded for [modelId] (partial and complete download files of its catalog versions): non-zero
     * after a paused or interrupted download.
     * zh-CN: [modelId] 已下载的字节数 (其目录版本的部分与完整下载文件): 下载暂停或中断后不为零.
     */
    fun downloadedBytes(modelId: String): Long {
        val model = catalogs.current().catalog.model(modelId) ?: return 0
        return artifacts(model).sumOf { sha ->
            listOf(File(downloadDirectory, "$sha.part"), File(downloadDirectory, "$sha.bin")).sumOf { if (it.isFile) it.length() else 0L }
        }
    }

    /**
     * Deletes the download files of [modelId] on the model thread, after the operations queued before; [onDone] runs there.
     * zh-CN: 在模型线程上 (排在此前的操作之后) 删除 [modelId] 的下载文件; [onDone] 在该线程上运行.
     */
    fun discardDownload(modelId: String, onDone: () -> Unit = {}) {
        executor.execute {
            runCatching { discardDownloads(modelId) }.onFailure { log("Discarding the download of $modelId failed: $it") }
            runCatching(onDone)
        }
    }

    /**
     * True when installing [modelId] needs the license confirmation of roadmap D11 (not accepted before).
     * zh-CN: 安装 [modelId] 需要路线图 D11 的许可确认 (此前未接受) 时为真.
     */
    fun needsLicenseAcceptance(modelId: String): Boolean {
        val version = catalogs.current().catalog.model(modelId)?.installableVersion ?: return false
        return !licensePolicy.isAccepted(modelId, version)
    }

    /**
     * Frees what no model refers to (the settings' "recycle unreferenced files", roadmap D23): download files that no
     * artifact of the current catalog names (paused downloads stay), the import copies, and the store's interrupted
     * staging, trash and broken version directories.
     * zh-CN: 释放不被任何模型引用的内容 (设置页的 "回收未引用文件", 路线图 D23): 当前目录任何资源都不引用的下载文件 (暂停的下载保留), 导入副本,
     * 以及存储中被中断的暂存, 回收站与损坏的版本目录.
     */
    fun recycle(title: String, listener: OperationListener): Int = enqueue(title, listener) {
        val referenced = catalogs.current().catalog.models.flatMap(::artifacts).toSet()
        var bytes = 0L
        var files = 0
        fun drop(file: File) {
            file.walkBottomUp().filter { it.isFile }.forEach { bytes += it.length(); files++ }
            file.deleteRecursively()
        }
        downloadDirectory.listFiles().orEmpty().filter { it.name.substringBefore('.') !in referenced }.forEach(::drop)
        scratchDirectories.forEach { directory -> directory.listFiles().orEmpty().forEach(::drop) }
        val before = usage(store.root)
        store.reconcile()
        val after = usage(store.root)
        bytes += maxOf(0L, before.first - after.first)
        files += maxOf(0, before.second - after.second)
        log("Recycled $files files, $bytes bytes")
        OperationResult.Recycled(bytes, files)
    }

    private fun usage(root: File): Pair<Long, Int> {
        var bytes = 0L
        var files = 0
        root.walkBottomUp().filter { it.isFile }.forEach { bytes += it.length(); files++ }
        return bytes to files
    }

    private fun artifacts(model: CatalogModel): List<String> =
        model.versions.flatMap { version -> listOfNotNull(version.mirror?.sha256) + version.upstream.map { it.sha256 } }

    private fun discardDownloads(modelId: String) {
        val model = catalogs.current().catalog.model(modelId) ?: return
        artifacts(model).forEach { sha ->
            File(downloadDirectory, "$sha.part").delete()
            File(downloadDirectory, "$sha.bin").delete()
        }
    }

    private fun enqueue(title: String, listener: OperationListener, cleanup: () -> Unit = {}, modelId: String? = null, body: (Operation) -> OperationResult): Int {
        val operation = Operation(ids.incrementAndGet(), title, listener, cleanup, modelId)
        operations[operation.id] = operation
        publishActivity()
        executor.execute {
            val runs = synchronized(operation) {
                if (operations[operation.id] !== operation) false else {
                    operation.started = true
                    true
                }
            }
            if (!runs) return@execute
            publishActivity()
            val outcome = try {
                operation.cancel.throwIfCancelled()
                Result.success(body(operation))
            } catch (e: ModelException) {
                Result.failure(e)
            } catch (e: Exception) {
                log("Operation ${operation.id} failed: $e")
                Result.failure(ModelException(SpeechErrorCodes.INTERNAL, e.message ?: e.javaClass.simpleName, e))
            } finally {
                runCatching(operation.cleanup)
                if (operation.discard && operation.cancel.isCancelled) operation.modelId?.let { id -> runCatching { discardDownloads(id) } }
            }
            operations.remove(operation.id)
            publishActivity()
            outcome.fold(
                onSuccess = { runCatching { listener.onCompleted(operation.id, it) } },
                onFailure = { error ->
                    val failure = if (operation.cancel.isCancelled) OperationCancelledException() else error as ModelException
                    runCatching { listener.onFailed(operation.id, failure) }
                },
            )
        }
        return operation.id
    }

    private fun progress(operation: Operation, done: Long, total: Long) {
        if (operation.cancel.isCancelled) return
        // About 200 reports per phase (1 MiB steps when the total is unknown), plus the last one and every change of
        // total (the upstream fallback has another size).
        val step = if (total > 0) maxOf(total / 200, 1) else 1L shl 20
        val last = total in 1..done
        if (!last && total == operation.total && done >= operation.done && done - operation.done < step) return
        operation.done = done
        operation.total = total
        runCatching { operation.listener.onProgress(operation.id, done, total) }
        publishActivity()
    }

    private fun publishActivity() {
        val running = operations.values.firstOrNull { it.started }
        runCatching { activity?.onActivity(Activity(operations.size, running?.title, running?.done ?: 0, running?.total ?: 0)) }
    }

    /**
     * Drops download leftovers no current catalog artifact refers to, and imports or archives older than a week.
     * zh-CN: 删除当前目录资源不再引用的下载残留, 以及超过一周的导入副本或归档.
     */
    private fun recycleDownloads() {
        val referenced = catalogs.current().catalog.models.flatMap(::artifacts).toSet()
        val limit = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        downloadDirectory.listFiles().orEmpty().forEach { file ->
            val sha = file.name.substringBefore('.')
            if (sha !in referenced || file.lastModified() < limit) file.deleteRecursively()
        }
    }
}
