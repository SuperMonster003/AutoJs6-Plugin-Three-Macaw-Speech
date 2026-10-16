package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ThreeMacawSpeechContract
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Importer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationCancelledException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeSession
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.speech.api.ISpeechPlugin
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechCapabilityKeys
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Capability service for `org.autojs.plugin.SPEECH` (category `speech`, roadmap D21): answers `ISpeechPlugin`.
 * zh-CN: `org.autojs.plugin.SPEECH` (category `speech`, 路线图 D21) 的能力服务: 应答 `ISpeechPlugin`.
 */
class ThreeMacawSpeechService : Service() {

    private val binder by lazy { SpeechPluginBinder(applicationContext) }

    override fun onBind(intent: Intent?): IBinder = binder

}

/**
 * `ISpeechPlugin` of contract version 1. Model management (roadmap P2.3 / P2.4: `listModels`, `installModel`,
 * `cancelOperation`, `removeModel`, `importModel`, `setDefaultModel`, `refreshCatalog`; long operations run on the
 * model thread and report through `ISpeechProgressCallback`), file recognition and voice activity detection (P3.3, on
 * the calling Binder thread), dictation sessions (P3.2) and wake sessions (P3.4) on [SpeechRuntime], and synthesis to a
 * file (P4.3, [SpeechSynthesis]: an operation on the synthesis thread with progress and cancellation).
 * zh-CN: 契约版本 1 的 `ISpeechPlugin`. 模型管理 (路线图 P2.3 / P2.4: `listModels`, `installModel`, `cancelOperation`, `removeModel`,
 * `importModel`, `setDefaultModel`, `refreshCatalog`; 长操作在模型线程上运行, 经 `ISpeechProgressCallback` 报告), 文件识别与语音活动检测
 * (P3.3, 在调用方 Binder 线程上), 听写会话 (P3.2) 与唤醒会话 (P3.4) 基于 [SpeechRuntime], 以及合成到文件 (P4.3, [SpeechSynthesis]: 在合成线程上
 * 运行的操作, 带进度与取消).
 */
internal class SpeechPluginBinder(private val context: Context) : ISpeechPlugin.Stub() {

    private val manager: ModelManager get() = SpeechModels.get(context)

    private val runtime: SpeechRuntime get() = SpeechRuntime.get(context)

    private val synthesis by lazy { SpeechSynthesis(runtime) }

    override fun getInfo(): PluginInfo = context.pluginRuntimeFields().toPluginInfo()

    override fun getCapabilities(): Bundle = SpeechBundles.answer {
        putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreeMacawSpeechContract.REQUIRED_HOST_VERSION)
        putInt(SpeechCapabilityKeys.CONTRACT_VERSION, ThreeMacawSpeechContract.CONTRACT_VERSION)
        putStringArray(SpeechCapabilityKeys.FEATURES, ThreeMacawSpeechContract.FEATURES.toTypedArray())
        putInt(SpeechCapabilityKeys.CATALOG_VERSION, manager.catalogVersion())
        putString(SpeechCapabilityKeys.ENGINE_NAME, "sherpa-onnx")
        putString(SpeechCapabilityKeys.ENGINE_VERSION, BuildConfig.SHERPA_ONNX_VERSION)
        putBoolean(SpeechCapabilityKeys.MICROPHONE_GRANTED, context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        putString(SpeechCapabilityKeys.TTS_ENGINE_PACKAGE, context.packageName)
    }

    override fun listModels(query: Bundle?): Bundle = answering {
        checkContract(query)
        val kind = query?.getString(SpeechOptionKeys.KIND)
        if (kind != null && kind !in SpeechContract.KINDS) invalid("kind must be one of ${SpeechContract.KINDS}")
        val language = query?.getString(SpeechOptionKeys.LANGUAGE)
        if (language != null && (language.length > SpeechLimits.MAX_TOKEN_CHARS || !LANGUAGE.matches(language))) invalid("Invalid language tag: $language")
        val installed = if (query?.containsKey(SpeechOptionKeys.INSTALLED) == true) query.getBoolean(SpeechOptionKeys.INSTALLED) else null
        val models = manager.list(kind, language, installed).take(SpeechLimits.MAX_MODELS)
        SpeechBundles.answer { putParcelableArrayList(SpeechResultKeys.MODELS, ArrayList(models.map(SpeechBundles::model))) }
    }

    override fun installModel(request: Bundle?, callback: ISpeechProgressCallback?): Int = throwing {
        checkContract(request)
        val id = modelId(request)
        val name = manager.entry(id)?.name ?: id
        manager.install(
            modelId = id,
            acceptLicense = request?.getBoolean(SpeechOptionKeys.ACCEPT_LICENSE, false) == true,
            listener = listener(callback),
            title = context.getString(R.string.notification_model_download_title, name),
        )
    }

    override fun cancelOperation(opId: Int) {
        manager.cancel(opId)
        synthesis.cancel(opId)
    }

    override fun removeModel(request: Bundle?): Bundle = answering {
        checkContract(request)
        val id = modelId(request)
        val removed = manager.remove(id)
        if (removed) runtime.engines.evict(id)
        SpeechBundles.answer { putBoolean(SpeechResultKeys.REMOVED, removed) }
    }

    /** Blocks the calling Binder thread until the file is recognized (the host calls it from its IO dispatcher). zh-CN: 阻塞调用方 Binder 线程直到识别完成. */
    override fun recognizeFile(audio: ParcelFileDescriptor?, options: Bundle?): Bundle = answering {
        val request = SpeechOptions.recognize(options?.let(::BundleOptionSource))
        val source = audio ?: throw SpeechException(SpeechErrorCodes.IO_FAILED, "An audio descriptor is required")
        SpeechBundles.transcript(runtime.recognizeFile(source, request))
    }

    override fun detectVoice(audio: ParcelFileDescriptor?, options: Bundle?): Bundle = answering {
        val request = SpeechOptions.vad(options?.let(::BundleOptionSource))
        val source = audio ?: throw SpeechException(SpeechErrorCodes.IO_FAILED, "An audio descriptor is required")
        SpeechBundles.voiceActivity(runtime.detectVoice(source, request))
    }

    /**
     * Opens a dictation session at once (roadmap D30): options, permission, model and admission are checked here and
     * thrown as `CODE: detail`; the model loads on the plugin's loader thread and `ready` follows.
     * zh-CN: 立即打开听写会话 (路线图 D30): 选项, 权限, 模型与准入在此检查并以 `CODE: detail` 抛出; 模型在插件的加载线程上加载, 随后发出 `ready`.
     */
    override fun openSession(options: Bundle?, callback: ISpeechSessionCallback?): ISpeechSession = throwing {
        val listen = SpeechOptions.listen(options?.let(::BundleOptionSource))
        val listener = callback ?: invalid("A session callback is required")
        if (!listen.isExternal) runtime.requireMicrophone()
        val model = runtime.resolve(SpeechContract.KIND_ASR, listen.model)
        val warnings = runtime.warnings(model, listen.language, listen.hotwords)
        val slot = runtime.sessions.reserve(Binder.getCallingUid(), microphone = !listen.isExternal)
        try {
            val id = runtime.nextId(if (listen.isExternal) "external" else "listen")
            DictationBinder(runtime, slot, listener, listen.isExternal) { events -> DictationSession(id, listen, events) }.also { it.open(model, warnings) }
        } catch (t: Throwable) {
            runtime.sessions.release(slot)
            throw t
        }
    }

    /**
     * Opens a wake session at once (roadmap D18): the keywords are spelled in the model's tokens here, so a keyword the
     * model cannot spell fails the call with `INVALID_OPTIONS`.
     * zh-CN: 立即打开唤醒会话 (路线图 D18): 关键词在此按模型 token 拼写, 模型无法拼写的关键词使调用以 `INVALID_OPTIONS` 失败.
     */
    override fun openWakeSession(options: Bundle?, callback: ISpeechWakeCallback?, listenCallback: ISpeechSessionCallback?): ISpeechSession = throwing {
        val wake = SpeechOptions.wake(options?.let(::BundleOptionSource))
        val listener = callback ?: invalid("A wake callback is required")
        val listen = if (wake.thenListen != null) listenCallback ?: invalid("thenListen needs the listen callback") else null
        runtime.requireMicrophone()
        val keywordModel = runtime.resolve(SpeechContract.KIND_KWS, wake.model)
        val listenModel = wake.thenListen?.let { runtime.resolve(SpeechContract.KIND_ASR, it.model) }
        val buffer = runtime.keywordBuffer(keywordModel, wake.keywords)
        val slot = runtime.sessions.reserve(Binder.getCallingUid(), microphone = true)
        try {
            val id = runtime.nextId("wake")
            WakeBinder(runtime, slot, listener, listen) { events, listenEvents ->
                WakeSession(id, wake, events, listenEvents, SherpaEngines.DEFAULT_KEYWORD_THRESHOLD)
            }.also { it.open(keywordModel, buffer, listenModel) }
        } catch (t: Throwable) {
            runtime.sessions.release(slot)
            throw t
        }
    }

    /**
     * Starts a synthesis into the host's staged [output] and answers its operation id at once (roadmap D20, P4.3); the
     * request is checked here and thrown as `CODE: detail`.
     * zh-CN: 开始合成到宿主暂存的 [output] 并立即返回操作 ID (路线图 D20, P4.3); 请求在此检查, 失败以 `CODE: detail` 抛出.
     */
    override fun synthesizeToFile(request: Bundle?, output: ParcelFileDescriptor?, callback: ISpeechProgressCallback?): Int = throwing {
        synthesis.start(request, output, callback)
    }

    /**
     * The host owns [archive] and closes it after the call; the operation reads a duplicate into a private copy first.
     * zh-CN: [archive] 归宿主所有并在调用后关闭; 操作先读取其副本描述符并复制到私有文件.
     */
    override fun importModel(archive: ParcelFileDescriptor?, request: Bundle?, callback: ISpeechProgressCallback?): Int = throwing {
        checkContract(request)
        val source = archive?.dup() ?: invalid("An archive descriptor is required")
        val importRequest = Importer.Request(
            modelId = request?.getString(SpeechOptionKeys.MODEL)?.also { if (it.length > SpeechLimits.MAX_TOKEN_CHARS) invalid("model id is too long") },
            kind = request?.getString(SpeechOptionKeys.KIND),
            modelType = request?.getString(SpeechOptionKeys.MODEL_TYPE),
            name = request?.getString(SpeechOptionKeys.NAME),
            languages = request?.getStringArray(SpeechOptionKeys.LANGUAGES)?.toList().orEmpty(),
            acceptLicense = request?.getBoolean(SpeechOptionKeys.ACCEPT_LICENSE, false) == true,
        )
        val copy = File(SpeechModels.importDirectory(context), "${UUID.randomUUID()}.archive")
        try {
            manager.importArchive(
                archive = copy,
                request = importRequest,
                title = context.getString(R.string.notification_model_import_title),
                listener = listener(callback),
                prepare = { cancel, progress -> copyDescriptor(source, copy, cancel, progress) },
                totalBytes = source.statSize,
                cleanup = { runCatching { source.close() } },
            )
        } catch (e: RuntimeException) {
            runCatching { source.close() }
            throw e
        }
    }

    override fun setDefaultModel(request: Bundle?): Bundle = answering {
        checkContract(request)
        val kind = request?.getString(SpeechOptionKeys.KIND)
        if (kind != null && kind !in SpeechContract.KINDS) invalid("kind must be one of ${SpeechContract.KINDS}")
        val entry = manager.setDefault(modelId(request), kind)
        SpeechBundles.answer { putBundle(SpeechResultKeys.MODEL, SpeechBundles.model(entry)) }
    }

    override fun refreshCatalog(callback: ISpeechProgressCallback?): Int = throwing {
        manager.refreshCatalog(context.getString(R.string.notification_catalog_refresh_title), listener(callback))
    }

    private fun copyDescriptor(source: ParcelFileDescriptor, target: File, cancel: CancelToken, progress: (Long) -> Unit) {
        target.parentFile?.mkdirs()
        source.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        cancel.throwIfCancelled()
                        val read = input.read(buffer)
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

    private fun listener(callback: ISpeechProgressCallback?): OperationListener = object : OperationListener {
        override fun onProgress(opId: Int, done: Long, total: Long) {
            runCatching { callback?.onProgress(opId, done, total) }
        }

        override fun onCompleted(opId: Int, result: OperationResult) {
            val bundle = when (result) {
                is OperationResult.Model -> SpeechBundles.answer { putBundle(SpeechResultKeys.MODEL, SpeechBundles.model(result.entry)) }
                is OperationResult.Catalog -> SpeechBundles.answer {
                    putInt(SpeechResultKeys.CATALOG_VERSION, result.catalogVersion)
                    putBoolean(SpeechResultKeys.UPDATED, result.updated)
                }
                // Only the settings screen recycles; the contract has no such call. zh-CN: 只有设置页会回收; 契约没有此调用.
                is OperationResult.Recycled -> SpeechBundles.answer {}
            }
            runCatching { callback?.onCompleted(opId, bundle) }
        }

        override fun onFailed(opId: Int, error: ModelException) {
            val failure = if (error is OperationCancelledException) SpeechBundles.failure(SpeechErrorCodes.SESSION_CANCELLED, error.message) else SpeechBundles.failure(error)
            runCatching { callback?.onFailed(opId, failure) }
        }
    }

    private fun modelId(request: Bundle?): String {
        val id = request?.getString(SpeechOptionKeys.MODEL) ?: invalid("model is required")
        if (id.isEmpty() || id.length > SpeechLimits.MAX_TOKEN_CHARS) invalid("Invalid model id")
        return id
    }

    /** Requests of another contract version are refused (`SpeechContract` rules). zh-CN: 拒绝其他契约版本的请求. */
    private fun checkContract(request: Bundle?) {
        if (request == null || !request.containsKey(SpeechContract.KEY_CONTRACT_VERSION)) return
        val version = request.getInt(SpeechContract.KEY_CONTRACT_VERSION)
        if (!SpeechContract.supportsContractVersion(version)) throw ModelException(SpeechErrorCodes.PLUGIN_INCOMPATIBLE, "Unsupported speech contract version $version")
    }

    private fun invalid(message: String): Nothing = throw ModelException(SpeechErrorCodes.INVALID_OPTIONS, message)

    private inline fun answering(block: () -> Bundle): Bundle = try {
        block()
    } catch (e: SpeechException) {
        SpeechBundles.failure(e)
    } catch (e: RuntimeException) {
        SpeechBundles.failure(SpeechErrorCodes.INTERNAL, e.message ?: e.javaClass.simpleName)
    }

    private inline fun <T> throwing(block: () -> T): T = try {
        block()
    } catch (e: SpeechException) {
        throw SpeechBundles.thrown(e)
    }

    private companion object {
        val LANGUAGE = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")
    }
}
