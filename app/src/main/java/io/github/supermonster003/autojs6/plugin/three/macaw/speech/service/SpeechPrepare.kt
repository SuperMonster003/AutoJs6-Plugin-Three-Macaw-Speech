package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineCache
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineLeases
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PrepareOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechOptions
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * `prepare` of contract version 3 (roadmap P11.2.1, D184, D202, D203): the request and the model are checked on the
 * calling Binder thread (thrown as `CODE: detail`), a place among the caller's leases is taken, and the engines load on
 * the loader thread, in line with the sessions' loads; the completion answers the lease id, the model and how long the
 * engines took (0 when they were loaded already). The engines are those of a listen (`asr`, default options: no
 * hotwords, punctuation on) or wake session (`kws`) of the model, so that session finds them loaded. The lease ends with
 * `release`, when the callback's process (the host) dies, or at once when the completion cannot be delivered;
 * `cancelOperation` before the completion answers `SESSION_CANCELLED` and frees what was loaded for it.
 * zh-CN: 契约版本 3 的 `prepare` (路线图 P11.2.1, D184, D202, D203): 请求与模型在调用方 Binder 线程上检查 (以 `CODE: detail` 抛出), 占用调用方
 * 租约中的一个位置, 引擎在加载线程上与会话的加载依次进行; 完成回调返回租约 ID, 模型以及引擎加载耗时 (已加载时为 0). 加载的引擎就是该模型的听写会话
 * (`asr`, 默认选项: 无热词, 开启标点) 或唤醒会话 (`kws`) 所用的那些, 因此会话直接拿到已加载的引擎. 租约在 `release`, 回调所在进程 (宿主) 死亡,
 * 或完成回调无法送达时立即结束; 完成之前的 `cancelOperation` 返回 `SESSION_CANCELLED` 并释放为其加载的引擎.
 */
internal class SpeechPrepare(private val runtime: SpeechRuntime) {

    private val operations = ConcurrentHashMap<Int, Operation>()

    private class Operation(val id: Int, private val callback: ISpeechProgressCallback) {
        private val settled = AtomicBoolean(false)

        @Volatile
        var cancelled = false

        /** The lease once granted. zh-CN: 发放后的租约. */
        val lease = AtomicReference<String?>(null)

        fun progress() {
            if (!settled.get()) runCatching { callback.onProgress(id, 0, 1) }
        }

        /** False when the operation was settled already or the host did not take the answer. zh-CN: 操作已结束或宿主未收下应答时为 false. */
        fun complete(result: Bundle): Boolean = settled.compareAndSet(false, true) && runCatching { callback.onCompleted(id, result) }.isSuccess

        fun fail(error: Bundle) {
            if (settled.compareAndSet(false, true)) runCatching { callback.onFailed(id, error) }
        }
    }

    /**
     * Starts a prepare of [holder] (the calling uid) and answers its operation id. zh-CN: 开始 [holder] (调用方 uid) 的预热并返回操作 ID.
     */
    fun start(request: Bundle?, callback: ISpeechProgressCallback?, holder: Int): Int {
        val options = SpeechOptions.prepare(request?.let(::BundleOptionSource))
        val listener = callback ?: throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "A progress callback is required")
        val model = runtime.resolve(options.kind, options.model)
        val ticket = runtime.leases.reserve(holder)
        val operation = Operation(runtime.models.nextOperationId(), listener)
        val binder = listener.asBinder()
        val death = IBinder.DeathRecipient {
            operation.cancelled = true
            operation.lease.get()?.let { runtime.leases.drop(it) }
        }
        try {
            binder.linkToDeath(death, 0)
        } catch (e: android.os.RemoteException) {
            ticket.cancel()
            throw SpeechException(SpeechErrorCodes.INTERNAL, "The caller died before the prepare started", e)
        }
        operations[operation.id] = operation
        operation.progress()
        runtime.load {
            try {
                load(operation, ticket, model, options) { runCatching { binder.unlinkToDeath(death, 0) } }
            } finally {
                operations.remove(operation.id)
            }
        }
        return operation.id
    }

    /**
     * Stops a prepare before its completion with `SESSION_CANCELLED`; completed or unknown ids are ignored (a lease ends
     * with `release`). zh-CN: 在完成之前以 `SESSION_CANCELLED` 停止预热; 已完成或未知的 ID 被忽略 (租约以 `release` 结束).
     */
    fun cancel(opId: Int) {
        val operation = operations[opId] ?: return
        operation.cancelled = true
        operation.fail(SpeechBundles.failure(SpeechErrorCodes.SESSION_CANCELLED, "The prepare was cancelled", SpeechContract.CALL_VERSION_3))
    }

    private fun load(operation: Operation, ticket: EngineLeases.Ticket, model: InstalledModel, options: PrepareOptions, unlink: () -> Unit) {
        if (operation.cancelled) {
            ticket.cancel()
            unlink()
            return
        }
        operation.progress()
        val began = SystemClock.elapsedRealtime()
        val uses: List<EngineCache.Lease<out Closeable>> = try {
            when (options.kind) {
                SpeechContract.KIND_KWS -> listOf(runtime.acquireKeywords(model))
                else -> runtime.acquireRecognition(model, options.language, hotwords = false, punctuation = true)
            }
        } catch (e: SpeechException) {
            ticket.cancel()
            unlink()
            operation.fail(SpeechBundles.failure(e, SpeechContract.CALL_VERSION_3))
            return
        } catch (e: OutOfMemoryError) {
            ticket.cancel()
            unlink()
            Log.w(TAG, "Preparing ${model.modelId} ran out of memory; ${SpeechRuntime.memory()}")
            operation.fail(SpeechBundles.failure(SpeechErrorCodes.LIMIT_EXCEEDED, "The engines of ${model.modelId} do not fit this device's memory", SpeechContract.CALL_VERSION_3))
            return
        } catch (e: RuntimeException) {
            ticket.cancel()
            unlink()
            Log.w(TAG, "Preparing ${model.modelId} failed", e)
            operation.fail(SpeechBundles.failure(SpeechErrorCodes.ENGINE_FAILED, e.message ?: e.javaClass.simpleName, SpeechContract.CALL_VERSION_3))
            return
        }
        val loadMillis = if (uses.any { it.loadedNow }) SystemClock.elapsedRealtime() - began else 0L
        if (operation.cancelled) {
            uses.forEach { runCatching { it.close() } }
            ticket.cancel()
            unlink()
            return
        }
        val leaseId = ticket.grant(model.modelId, uses, unlink)
        operation.lease.set(leaseId)
        StartupTiming.log("Prepare of ${model.modelId} (${options.kind}) loaded in $loadMillis ms")
        val answer = SpeechBundles.answer(SpeechContract.CALL_VERSION_3) {
            putString(SpeechResultKeys.LEASE_ID, leaseId)
            putString(SpeechResultKeys.MODEL_ID, model.modelId)
            putLong(SpeechResultKeys.LOAD_MILLIS, loadMillis)
        }
        // Cancelled or dead in the meantime, or the answer did not arrive: nobody knows the lease. zh-CN: 期间被取消或死亡, 或应答未送达: 无人知道该租约.
        if (operation.cancelled || !operation.complete(answer)) runtime.leases.drop(leaseId)
    }

    private companion object {
        const val TAG = "MacawEngines"
    }
}
