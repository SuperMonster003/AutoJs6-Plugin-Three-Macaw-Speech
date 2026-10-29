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
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.SpeechOperationOwners
import org.autojs.plugin.speech.api.*

/**
 * A separate, explicitly permissioned entry for applications signed by their own authors.
 * The existing host entry retains org.autojs.permission.PLUGIN and its signature protection.
 */
class ThreeMacawSpeechPackagedService : Service() {
    private val binder by lazy { PackagedSpeechBinder(applicationContext) }
    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { binder.close(); super.onDestroy() }
}

internal class PackagedSpeechBinder(context: Context) : ISpeechPlugin.Stub(), AutoCloseable {
    private val delegate = SpeechPluginBinder(context) {
        if (context.checkPermission(Manifest.permission.RECORD_AUDIO, Binder.getCallingPid(), Binder.getCallingUid()) != PackageManager.PERMISSION_GRANTED) {
            throw SpeechException(SpeechErrorCodes.PERMISSION_DENIED,
                "The calling packaged application also needs android.permission.RECORD_AUDIO; the plugin's microphone grant does not grant it to other applications")
        }
    }
    private val owners = SpeechOperationOwners(delegate::cancelOperation)

    override fun getInfo() = delegate.getInfo()
    override fun getCapabilities() = delegate.getCapabilities()
    override fun listModels(query: Bundle?) = delegate.listModels(query)
    override fun removeModel(request: Bundle?) = delegate.removeModel(request)
    override fun setDefaultModel(request: Bundle?) = delegate.setDefaultModel(request)
    override fun recognizeFile(audio: ParcelFileDescriptor?, options: Bundle?) = delegate.recognizeFile(audio, options)
    override fun detectVoice(audio: ParcelFileDescriptor?, options: Bundle?) = delegate.detectVoice(audio, options)
    override fun openSession(options: Bundle?, callback: ISpeechSessionCallback?) = delegate.openSession(options, callback)
    override fun openWakeSession(options: Bundle?, callback: ISpeechWakeCallback?, listenCallback: ISpeechSessionCallback?) =
        delegate.openWakeSession(options, callback, listenCallback)

    override fun installModel(request: Bundle?, callback: ISpeechProgressCallback?) =
        operation(callback) { delegate.installModel(request, it) }
    override fun importModel(archive: ParcelFileDescriptor?, request: Bundle?, callback: ISpeechProgressCallback?) =
        operation(callback) { delegate.importModel(archive, request, it) }
    override fun refreshCatalog(callback: ISpeechProgressCallback?) = operation(callback, delegate::refreshCatalog)
    override fun synthesizeToFile(request: Bundle?, output: ParcelFileDescriptor?, callback: ISpeechProgressCallback?) =
        operation(callback) { delegate.synthesizeToFile(request, output, it) }
    override fun enrollSpeaker(audio: Array<out ParcelFileDescriptor?>?, options: Bundle?) = delegate.enrollSpeaker(audio, options)
    override fun verifySpeaker(audio: ParcelFileDescriptor?, options: Bundle?) = delegate.verifySpeaker(audio, options)
    override fun identifySpeaker(audio: ParcelFileDescriptor?, options: Bundle?) = delegate.identifySpeaker(audio, options)
    override fun listSpeakers(query: Bundle?) = delegate.listSpeakers(query)
    override fun removeSpeaker(request: Bundle?) = delegate.removeSpeaker(request)
    override fun diarizeFile(audio: ParcelFileDescriptor?, options: Bundle?, callback: ISpeechProgressCallback?) =
        operation(callback) { delegate.diarizeFile(audio, options, it) }
    override fun detectLanguage(audio: ParcelFileDescriptor?, options: Bundle?, callback: ISpeechProgressCallback?) =
        operation(callback) { delegate.detectLanguage(audio, options, it) }
    override fun tagAudio(audio: ParcelFileDescriptor?, options: Bundle?, callback: ISpeechProgressCallback?) =
        operation(callback) { delegate.tagAudio(audio, options, it) }
    override fun cancelOperation(opId: Int) = owners.cancel(Binder.getCallingUid(), opId)
    override fun close() = owners.close()

    private fun operation(callback: ISpeechProgressCallback?, start: (ISpeechProgressCallback) -> Int): Int {
        val owner = owners.begin(Binder.getCallingUid())
        val peer = callback?.asBinder()
        val death = IBinder.DeathRecipient { owner.abort() }
        fun finish(): Boolean = owner.finish().also { if (it) runCatching { peer?.unlinkToDeath(death, 0) } }
        val relay = object : ISpeechProgressCallback.Stub() {
            override fun onProgress(opId: Int, done: Long, total: Long) { runCatching { callback?.onProgress(opId, done, total) } }
            override fun onCompleted(opId: Int, result: Bundle?) { if (finish()) runCatching { callback?.onCompleted(opId, result) } }
            override fun onFailed(opId: Int, error: Bundle?) { if (finish()) runCatching { callback?.onFailed(opId, error) } }
        }
        try {
            peer?.linkToDeath(death, 0)
            return start(relay).also(owner::attach)
        } catch (error: Throwable) {
            owner.abort()
            finish()
            throw error
        }
    }
}
