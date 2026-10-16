package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ThreeMacawSpeechContract
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Importer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationCancelledException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
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
 * `ISpeechPlugin` of contract version 1. Model management is implemented (roadmap P2.3 / P2.4: `listModels`,
 * `installModel`, `cancelOperation`, `removeModel`, `importModel`, `setDefaultModel`, `refreshCatalog`; long
 * operations run on the model thread and report through `ISpeechProgressCallback`). Recognition, voice activity,
 * sessions and synthesis answer `ENGINE_FAILED` until roadmap P3 / P4; the plugin does not advertise those features,
 * so the host does not call them.
 * zh-CN: 契约版本 1 的 `ISpeechPlugin`. 已实现模型管理 (路线图 P2.3 / P2.4: `listModels`, `installModel`, `cancelOperation`,
 * `removeModel`, `importModel`, `setDefaultModel`, `refreshCatalog`; 长操作在模型线程上运行, 经 `ISpeechProgressCallback` 报告).
 * 识别, 语音活动检测, 会话与合成在路线图 P3 / P4 之前应答 `ENGINE_FAILED`; 插件不声明这些功能, 宿主因此不会调用它们.
 */
internal class SpeechPluginBinder(private val context: Context) : ISpeechPlugin.Stub() {

    private val manager: ModelManager get() = SpeechModels.get(context)

    override fun getInfo(): PluginInfo = context.pluginRuntimeFields().toPluginInfo()

    override fun getCapabilities(): Bundle = SpeechBundles.answer {
        putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreeMacawSpeechContract.REQUIRED_HOST_VERSION)
        putInt(SpeechCapabilityKeys.CONTRACT_VERSION, ThreeMacawSpeechContract.CONTRACT_VERSION)
        putStringArray(SpeechCapabilityKeys.FEATURES, ThreeMacawSpeechContract.FEATURES.toTypedArray())
        putInt(SpeechCapabilityKeys.CATALOG_VERSION, manager.catalogVersion())
        putString(SpeechCapabilityKeys.ENGINE_NAME, "sherpa-onnx")
        putString(SpeechCapabilityKeys.ENGINE_VERSION, BuildConfig.SHERPA_ONNX_VERSION)
        putBoolean(SpeechCapabilityKeys.MICROPHONE_GRANTED, context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
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

    override fun cancelOperation(opId: Int) = manager.cancel(opId)

    override fun removeModel(request: Bundle?): Bundle = answering {
        checkContract(request)
        val removed = manager.remove(modelId(request))
        SpeechBundles.answer { putBoolean(SpeechResultKeys.REMOVED, removed) }
    }

    override fun recognizeFile(audio: ParcelFileDescriptor?, options: Bundle?): Bundle =
        SpeechBundles.failure(SpeechErrorCodes.ENGINE_FAILED, "File recognition arrives with roadmap P3; the plugin does not advertise the asr feature yet")

    override fun detectVoice(audio: ParcelFileDescriptor?, options: Bundle?): Bundle =
        SpeechBundles.failure(SpeechErrorCodes.ENGINE_FAILED, "Voice activity detection arrives with roadmap P3; the plugin does not advertise the vad feature yet")

    override fun openSession(options: Bundle?, callback: ISpeechSessionCallback?): ISpeechSession =
        throw SpeechBundles.thrown(SpeechErrorCodes.ENGINE_FAILED, "Dictation sessions arrive with roadmap P3")

    override fun openWakeSession(options: Bundle?, callback: ISpeechWakeCallback?, listenCallback: ISpeechSessionCallback?): ISpeechSession =
        throw SpeechBundles.thrown(SpeechErrorCodes.ENGINE_FAILED, "Wake sessions arrive with roadmap P3")

    override fun synthesizeToFile(request: Bundle?, output: ParcelFileDescriptor?, callback: ISpeechProgressCallback?): Int =
        throw SpeechBundles.thrown(SpeechErrorCodes.ENGINE_FAILED, "Synthesis arrives with roadmap P4")

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
    } catch (e: ModelException) {
        SpeechBundles.failure(e)
    } catch (e: RuntimeException) {
        SpeechBundles.failure(SpeechErrorCodes.INTERNAL, e.message ?: e.javaClass.simpleName)
    }

    private inline fun throwing(block: () -> Int): Int = try {
        block()
    } catch (e: ModelException) {
        throw SpeechBundles.thrown(e)
    }

    private companion object {
        val LANGUAGE = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")
    }
}
