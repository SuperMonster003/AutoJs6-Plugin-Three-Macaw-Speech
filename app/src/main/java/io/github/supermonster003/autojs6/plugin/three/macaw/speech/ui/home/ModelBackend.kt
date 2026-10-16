package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ImportTree
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Importer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File
import java.io.InputStream
import java.util.UUID

/** Where an import reads from: one archive document or a document tree. zh-CN: 导入的来源: 单个归档文档或文档树. */
internal data class ImportSource(val uri: Uri, val tree: Boolean, val displayName: String?)

/**
 * The model operations the home screen needs, behind an interface so the instrumentation can drive the screen with a
 * fake (roadmap P5.1 tests); [ManagerBackend] is the real one on the process-wide [ModelManager].
 * zh-CN: 首页所需的模型操作, 以接口隔离, 供 instrumentation 以替身驱动界面 (路线图 P5.1 测试); [ManagerBackend] 基于进程级 [ModelManager].
 */
internal interface ModelBackend {
    val builtinIds: Set<String>
    fun entries(): List<ModelEntry>
    fun catalogVersion(): Int
    fun downloadedBytes(modelId: String): Long
    fun needsLicenseAcceptance(modelId: String): Boolean

    @Throws(ModelException::class)
    fun install(modelId: String, acceptLicense: Boolean, listener: OperationListener): Int
    fun cancel(opId: Int, discardPartial: Boolean)
    fun discardDownload(modelId: String, onDone: () -> Unit)

    @Throws(ModelException::class)
    fun remove(modelId: String): Boolean

    @Throws(ModelException::class)
    fun setDefault(modelId: String, kind: String)

    @Throws(ModelException::class)
    fun import(source: ImportSource, request: Importer.Request, listener: OperationListener): Int
    fun refreshCatalog(listener: OperationListener): Int
    fun recycle(listener: OperationListener): Int
}

/**
 * [ModelBackend] on the process-wide [ModelManager]; the instrumentation passes a manager of its own and an
 * [onRemoved] without the shared engine cache.
 * zh-CN: 基于进程级 [ModelManager] 的 [ModelBackend]; instrumentation 传入自己的管理器与不涉及共享引擎缓存的 [onRemoved].
 */
internal class ManagerBackend(
    context: Context,
    private val provider: () -> ModelManager = { SpeechModels.get(context.applicationContext) },
    // As the capability service does: drop the idle engines of a removed model. zh-CN: 与能力服务相同, 丢弃已删除模型的空闲引擎.
    private val onRemoved: (String) -> Unit = { id -> SpeechRuntime.get(context.applicationContext).engines.evict(id) },
    override val builtinIds: Set<String> = setOf(SpeechModels.BUNDLED_VAD_ID),
) : ModelBackend {
    private val app = context.applicationContext
    private val manager: ModelManager get() = provider()

    override fun entries(): List<ModelEntry> = manager.list()
    override fun catalogVersion(): Int = manager.catalogVersion()
    override fun downloadedBytes(modelId: String): Long = manager.downloadedBytes(modelId)
    override fun needsLicenseAcceptance(modelId: String): Boolean = manager.needsLicenseAcceptance(modelId)

    override fun install(modelId: String, acceptLicense: Boolean, listener: OperationListener): Int {
        val name = manager.entry(modelId)?.name ?: modelId
        return manager.install(modelId, acceptLicense, listener, app.getString(R.string.notification_model_download_title, name))
    }

    override fun cancel(opId: Int, discardPartial: Boolean) = manager.cancel(opId, discardPartial)
    override fun discardDownload(modelId: String, onDone: () -> Unit) = manager.discardDownload(modelId, onDone)

    override fun remove(modelId: String): Boolean {
        val removed = manager.remove(modelId)
        if (removed) onRemoved(modelId)
        return removed
    }

    override fun setDefault(modelId: String, kind: String) {
        manager.setDefault(modelId, kind)
    }

    override fun import(source: ImportSource, request: Importer.Request, listener: OperationListener): Int {
        val title = app.getString(R.string.notification_model_import_title)
        if (source.tree) return manager.importTree(DocumentTree(app.contentResolver, source.uri), request, title, listener)
        val copy = File(SpeechModels.importDirectory(app), "${UUID.randomUUID()}.archive")
        val size = runCatching { app.contentResolver.openAssetFileDescriptor(source.uri, "r")?.use { it.length } }.getOrNull() ?: 0L
        return manager.importArchive(
            archive = copy,
            request = request,
            title = title,
            listener = listener,
            prepare = { cancel, progress -> copyDocument(app.contentResolver, source.uri, copy, cancel, progress) },
            totalBytes = size.coerceAtLeast(0),
        )
    }

    override fun refreshCatalog(listener: OperationListener): Int = manager.refreshCatalog(app.getString(R.string.notification_catalog_refresh_title), listener)

    override fun recycle(listener: OperationListener): Int = manager.recycle(app.getString(R.string.notification_recycle_title), listener)

    private fun copyDocument(resolver: ContentResolver, uri: Uri, target: File, cancel: CancelToken, progress: (Long) -> Unit) {
        target.parentFile?.mkdirs()
        val input = resolver.openInputStream(uri) ?: throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot open the chosen file")
        input.use { stream ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    cancel.throwIfCancelled()
                    val read = stream.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    total += read
                    if (total > CatalogRules.MAX_INSTALL_BYTES) throw ModelException(SpeechErrorCodes.LIMIT_EXCEEDED, "The archive exceeds ${CatalogRules.MAX_INSTALL_BYTES} bytes")
                    progress(total)
                }
                output.fd.sync()
            }
        }
    }
}

/**
 * [ImportTree] over a SAF document tree (roadmap P5.1): walks the children of the chosen folder through
 * `DocumentsContract`, sorted by name; files are opened on demand. No storage permission (D23).
 * zh-CN: 基于 SAF 文档树的 [ImportTree] (路线图 P5.1): 经 `DocumentsContract` 按名称顺序遍历所选文件夹的子项; 文件按需打开. 无需存储权限 (D23).
 */
internal class DocumentTree(private val resolver: ContentResolver, private val treeUri: Uri) : ImportTree {
    override fun forEachFile(visitor: (path: String, open: () -> InputStream) -> Unit) {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        walk(rootId, "", 0, visitor)
    }

    private fun walk(parentId: String, prefix: String, depth: Int, visitor: (String, () -> InputStream) -> Unit) {
        if (depth > MAX_DEPTH) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        val entries = ArrayList<Triple<String, String, String?>>()
        resolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) entries += Triple(cursor.getString(0), cursor.getString(1) ?: continue, cursor.getString(2))
        }
        entries.sortedBy { it.second }.forEach { (id, name, mime) ->
            val path = if (prefix.isEmpty()) name else "$prefix/$name"
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                walk(id, path, depth + 1, visitor)
            } else {
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                visitor(path) { resolver.openInputStream(uri) ?: throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot open $path") }
            }
        }
    }

    private companion object {
        const val MAX_DEPTH = 8
    }
}

/**
 * Process-wide record of the operations the screens started, as Compose state (roadmap P5.1): downloads keep running in
 * [ModelManager] when the home screen closes, so a new screen must still see their progress. Listener callbacks are
 * posted to [post] (the main thread in the app); [revision] grows whenever a model operation ends.
 * zh-CN: 界面发起的操作的进程级记录, 以 Compose 状态保存 (路线图 P5.1): 首页关闭后下载仍在 [ModelManager] 中运行, 新打开的界面必须仍能看到其进度.
 * 监听回调经 [post] 投递 (应用中为主线程); 每当模型操作结束, [revision] 增加.
 */
internal class OperationTracker(private val post: (() -> Unit) -> Unit) {

    /** The end of an operation for the screens' messages. zh-CN: 操作的结局, 供界面提示. */
    sealed class Outcome {
        abstract val operation: TrackedOperation

        data class Completed(override val operation: TrackedOperation, val result: OperationResult) : Outcome()
        data class Failed(override val operation: TrackedOperation, val error: ModelException) : Outcome()
    }

    val operations = mutableStateMapOf<Int, TrackedOperation>()
    var revision by mutableIntStateOf(0)
        private set

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(Outcome) -> Unit>()

    fun addListener(listener: (Outcome) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (Outcome) -> Unit) {
        listeners -= listener
    }

    /** Starts [start] with a listener that tracks it as [kind] for [modelId]. zh-CN: 以跟踪 [kind] / [modelId] 的监听器启动 [start]. */
    @Throws(ModelException::class)
    fun track(kind: TrackedOperation.Kind, modelId: String?, start: (OperationListener) -> Int): Int {
        val listener = object : OperationListener {
            override fun onProgress(opId: Int, done: Long, total: Long) = post {
                operations[opId]?.let { operations[opId] = it.copy(started = true, done = done, total = total) }
            }

            override fun onCompleted(opId: Int, result: OperationResult) = post { finish(opId, kind, modelId) { Outcome.Completed(it, result) } }

            override fun onFailed(opId: Int, error: ModelException) = post { finish(opId, kind, modelId) { Outcome.Failed(it, error) } }
        }
        // Called on the main thread: the callbacks posted meanwhile run after this record, except with a synchronous [post].
        val opId = start(listener)
        if (!finished.remove(opId)) operations[opId] = operations[opId] ?: TrackedOperation(opId, modelId, kind)
        return opId
    }

    fun forModel(modelId: String): TrackedOperation? = operations.values.firstOrNull { it.modelId == modelId && it.kind == TrackedOperation.Kind.DOWNLOAD }

    fun running(kind: TrackedOperation.Kind): TrackedOperation? = operations.values.firstOrNull { it.kind == kind }

    /** Ids that ended before [track] recorded them (an operation that fails at once). zh-CN: 在 [track] 记录之前就结束的操作 ID. */
    private val finished = HashSet<Int>()

    private fun finish(opId: Int, kind: TrackedOperation.Kind, modelId: String?, outcome: (TrackedOperation) -> Outcome) {
        val operation = operations.remove(opId) ?: TrackedOperation(opId, modelId, kind).also { finished += opId }
        revision++
        val result = outcome(operation)
        listeners.forEach { runCatching { it(result) } }
    }

    companion object {
        private val main = Handler(Looper.getMainLooper())

        /** The tracker of the plugin process. zh-CN: 插件进程的跟踪器. */
        val shared: OperationTracker by lazy { OperationTracker { task -> main.post(task) } }
    }
}
