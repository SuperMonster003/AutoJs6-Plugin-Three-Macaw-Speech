package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Importer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InsufficientSpaceException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.NoCatalogMatchException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationCancelledException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import org.autojs.plugin.speech.api.SpeechErrorCodes

/** The Android facts and preferences the controller asks for. zh-CN: 控制器需要的 Android 状态与偏好. */
internal interface HomeEnvironment {
    fun networkAvailable(): Boolean
    fun networkMetered(): Boolean
    var allowMeteredDownloads: Boolean

    /** Asks for POST_NOTIFICATIONS once, before the first download (API 33+). zh-CN: 首次下载前询问一次通知权限 (API 33+). */
    fun beforeFirstDownload()
}

/** An import waiting for a license confirmation or the custom model form. zh-CN: 等待许可确认或自定义模型表单的导入. */
internal data class PendingImport(val source: ImportSource, val request: Importer.Request = Importer.Request())

/** The dialogs of the home screen. zh-CN: 首页的对话框. */
internal sealed interface HomeDialog {
    data class Details(val modelId: String) : HomeDialog

    /** Roadmap D11: shown before a download or import of a model that needs `acceptLicense`. zh-CN: 路线图 D11 的许可提示. */
    data class License(val modelId: String, val import: PendingImport? = null) : HomeDialog

    /** Roadmap appendix D Q7 (a): a download on a metered network. zh-CN: 附录 D Q7 (a): 计量网络上的下载. */
    data class Metered(val modelId: String, val acceptLicense: Boolean) : HomeDialog
    data class Delete(val modelId: String) : HomeDialog
    data object ImportChoice : HomeDialog
    data class ImportForm(val import: PendingImport) : HomeDialog
}

/** One-shot messages the screen shows as a snackbar. zh-CN: 界面以 snackbar 显示的一次性消息. */
internal sealed interface HomeMessage {
    data object Offline : HomeMessage
    data class Installed(val name: String) : HomeMessage
    data class Imported(val name: String) : HomeMessage
    data class Removed(val name: String) : HomeMessage
    data class DefaultSet(val name: String) : HomeMessage
    data class Failed(val code: String, val kind: TrackedOperation.Kind, val insufficientSpace: Boolean = false) : HomeMessage
}

/** Status card facts the Activity reads on every resume. zh-CN: Activity 每次恢复时读取的状态卡信息. */
internal data class HomeStatus(
    val runtimeVersion: String = "",
    val microphoneGranted: Boolean = false,
    val ttsEngineSelected: Boolean = false,
)

/**
 * State and actions of the home screen (roadmap P5.1, D11, appendix D Q7): the grouped model list with live operation
 * progress, the download / pause / cancel / delete / set-default actions with their license and metered network
 * confirmations, and the archive or folder import with the custom model form. Blocking backend calls run on [io]; the
 * results and every state change happen on [main].
 * zh-CN: 首页的状态与动作 (路线图 P5.1, D11, 附录 D Q7): 带实时操作进度的分组模型列表, 下载 / 暂停 / 取消 / 删除 / 设为默认及其许可与计量网络确认,
 * 以及带自定义模型表单的归档或文件夹导入. 阻塞的后端调用在 [io] 上运行; 结果与所有状态变化发生在 [main] 上.
 */
internal class HomeController(
    private val backend: ModelBackend,
    private val tracker: OperationTracker,
    /** Replaced by every new Activity, so the controller never keeps a destroyed one. zh-CN: 每个新 Activity 都会替换, 控制器因此不会持有已销毁的 Activity. */
    var environment: HomeEnvironment,
    private val io: (() -> Unit) -> Unit,
    private val main: (() -> Unit) -> Unit,
) : ViewModel() {

    private var entries by mutableStateOf<List<ModelEntry>>(emptyList())
    private var downloaded by mutableStateOf<Map<String, Long>>(emptyMap())

    var loaded by mutableStateOf(false)
        private set
    var catalogVersion by mutableIntStateOf(0)
        private set
    var status by mutableStateOf(HomeStatus())
    var networkAvailable by mutableStateOf(true)
    var dialog by mutableStateOf<HomeDialog?>(null)
    var message by mutableStateOf<Pair<Int, HomeMessage>?>(null)
        private set

    val groups by derivedStateOf {
        val cached = downloaded
        ModelRows.build(entries, tracker.operations.values, backend.builtinIds) { cached[it] ?: 0L }
    }

    val installedCount by derivedStateOf { ModelRows.installedCount(groups) }

    val importRunning by derivedStateOf { tracker.running(TrackedOperation.Kind.IMPORT) != null }

    private var messages = 0

    private val outcomes: (OperationTracker.Outcome) -> Unit = { outcome -> onOutcome(outcome) }

    init {
        tracker.addListener(outcomes)
    }

    override fun onCleared() {
        tracker.removeListener(outcomes)
    }

    fun row(modelId: String): ModelRow? = groups.firstNotNullOfOrNull { group -> group.rows.firstOrNull { it.id == modelId } }

    /** Reloads the entries and the partial download sizes off the main thread. zh-CN: 在主线程之外重新加载条目与部分下载大小. */
    fun refresh() {
        io {
            val list = runCatching { backend.entries() }.getOrDefault(emptyList())
            val version = runCatching { backend.catalogVersion() }.getOrDefault(0)
            val partial = list.filter { !it.installed && it.id !in backend.builtinIds }
                .associate { it.id to runCatching { backend.downloadedBytes(it.id) }.getOrDefault(0L) }
            main {
                entries = list
                downloaded = partial
                catalogVersion = version
                loaded = true
            }
        }
    }

    // Downloads ------------------------------------------------------------------------------------------------------

    /** The download button, also "resume" for a paused download. zh-CN: 下载按钮, 对暂停的下载也表示 "继续". */
    fun requestDownload(modelId: String) {
        if (!environment.networkAvailable()) {
            post(HomeMessage.Offline)
            return
        }
        io {
            val needsLicense = runCatching { backend.needsLicenseAcceptance(modelId) }.getOrDefault(false)
            main {
                if (needsLicense) dialog = HomeDialog.License(modelId) else continueDownload(modelId, acceptLicense = false)
            }
        }
    }

    /** Confirm of the license dialog. zh-CN: 许可对话框的确认. */
    fun acceptLicense(confirmed: HomeDialog.License) {
        dialog = null
        val pending = confirmed.import
        if (pending != null) {
            startImport(pending.copy(request = pending.request.copy(acceptLicense = true)))
        } else {
            continueDownload(confirmed.modelId, acceptLicense = true)
        }
    }

    /** Confirm of the metered network dialog; [remember] stops asking. zh-CN: 计量网络对话框的确认; [remember] 后不再询问. */
    fun acceptMetered(confirmed: HomeDialog.Metered, remember: Boolean) {
        dialog = null
        if (remember) environment.allowMeteredDownloads = true
        startDownload(confirmed.modelId, confirmed.acceptLicense)
    }

    private fun continueDownload(modelId: String, acceptLicense: Boolean) {
        if (environment.networkMetered() && !environment.allowMeteredDownloads) {
            dialog = HomeDialog.Metered(modelId, acceptLicense)
        } else {
            startDownload(modelId, acceptLicense)
        }
    }

    private fun startDownload(modelId: String, acceptLicense: Boolean) {
        if (tracker.forModel(modelId) != null) return
        environment.beforeFirstDownload()
        try {
            tracker.track(TrackedOperation.Kind.DOWNLOAD, modelId) { listener -> backend.install(modelId, acceptLicense, listener) }
        } catch (e: ModelException) {
            post(HomeMessage.Failed(e.code, TrackedOperation.Kind.DOWNLOAD))
        }
    }

    /** Stops the download and keeps the partial file. zh-CN: 停止下载并保留部分文件. */
    fun pause(modelId: String) {
        tracker.forModel(modelId)?.let { backend.cancel(it.opId, discardPartial = false) }
    }

    /** Stops a running, queued or paused download and deletes its partial file. zh-CN: 停止运行中, 排队或暂停的下载并删除部分文件. */
    fun cancelDownload(modelId: String) {
        val operation = tracker.forModel(modelId)
        if (operation != null) {
            backend.cancel(operation.opId, discardPartial = true)
        } else {
            backend.discardDownload(modelId) { main { refresh() } }
        }
    }

    // Installed models ------------------------------------------------------------------------------------------------

    fun requestDelete(modelId: String) {
        dialog = HomeDialog.Delete(modelId)
    }

    /** Deletes the model; the default of its kind falls back as `ModelManager` defines. zh-CN: 删除模型; 其种类的默认模型按 `ModelManager` 的规则回退. */
    fun confirmDelete(modelId: String) {
        dialog = null
        val name = row(modelId)?.name ?: modelId
        io {
            val failure = try {
                backend.remove(modelId)
                null
            } catch (e: ModelException) {
                e.code
            }
            main {
                post(if (failure == null) HomeMessage.Removed(name) else HomeMessage.Failed(failure, TrackedOperation.Kind.DOWNLOAD))
                refresh()
            }
        }
    }

    fun setDefault(modelId: String) {
        val row = row(modelId) ?: return
        dialog = null
        io {
            val failure = try {
                backend.setDefault(modelId, row.kind)
                null
            } catch (e: ModelException) {
                e.code
            }
            main {
                post(if (failure == null) HomeMessage.DefaultSet(row.name) else HomeMessage.Failed(failure, TrackedOperation.Kind.DOWNLOAD))
                refresh()
            }
        }
    }

    // Imports -------------------------------------------------------------------------------------------------------

    fun importChosen(source: ImportSource) = startImport(PendingImport(source))

    /** Submits the custom model form. zh-CN: 提交自定义模型表单. */
    fun submitImportForm(pending: PendingImport, request: Importer.Request) {
        dialog = null
        startImport(pending.copy(request = request.copy(acceptLicense = pending.request.acceptLicense)))
    }

    private var lastImport: PendingImport? = null

    private fun startImport(pending: PendingImport) {
        lastImport = pending
        try {
            tracker.track(TrackedOperation.Kind.IMPORT, null) { listener -> backend.import(pending.source, pending.request, listener) }
        } catch (e: ModelException) {
            post(HomeMessage.Failed(e.code, TrackedOperation.Kind.IMPORT))
        }
    }

    // Outcomes ------------------------------------------------------------------------------------------------------

    private fun onOutcome(outcome: OperationTracker.Outcome) {
        val operation = outcome.operation
        when (outcome) {
            is OperationTracker.Outcome.Completed -> {
                val result = outcome.result
                if (result is OperationResult.Model) {
                    post(if (operation.kind == TrackedOperation.Kind.IMPORT) HomeMessage.Imported(result.entry.name) else HomeMessage.Installed(result.entry.name))
                }
            }
            is OperationTracker.Outcome.Failed -> {
                val error = outcome.error
                when {
                    error is OperationCancelledException -> Unit
                    operation.kind == TrackedOperation.Kind.IMPORT && error is NoCatalogMatchException ->
                        lastImport?.let { dialog = HomeDialog.ImportForm(it) }
                    operation.kind == TrackedOperation.Kind.IMPORT && error.code == SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED ->
                        lastImport?.let { pending -> dialog = HomeDialog.License(licensedModel(error) ?: "", pending) }
                    operation.kind == TrackedOperation.Kind.DOWNLOAD || operation.kind == TrackedOperation.Kind.IMPORT ->
                        post(HomeMessage.Failed(error.code, operation.kind, error is InsufficientSpaceException))
                }
            }
        }
        if (operation.kind == TrackedOperation.Kind.DOWNLOAD || operation.kind == TrackedOperation.Kind.IMPORT ||
            operation.kind == TrackedOperation.Kind.REFRESH || operation.kind == TrackedOperation.Kind.RECYCLE
        ) {
            refresh()
        }
    }

    /** The catalog id named by a license failure (`<id> is limited to ...`). zh-CN: 许可失败信息中的目录 ID. */
    private fun licensedModel(error: ModelException): String? = error.message?.substringBefore(' ')?.takeIf { id -> entries.any { it.id == id } }

    private fun post(value: HomeMessage) {
        message = ++messages to value
    }
}
