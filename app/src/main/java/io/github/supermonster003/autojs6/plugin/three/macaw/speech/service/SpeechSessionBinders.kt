package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MicrophoneSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineCache
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.KeywordEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationEvents
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SessionTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechWarning
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeEvents
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeSession
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [DictationEvents] delivered to an `ISpeechSessionCallback` (oneway). A dead callback (the host process died) cancels
 * the session through [onDead].
 * zh-CN: 投递到 `ISpeechSessionCallback` (oneway) 的 [DictationEvents]. 回调已失效 (宿主进程死亡) 时经 [onDead] 取消会话.
 */
internal class CallbackDictationEvents(private val callback: ISpeechSessionCallback, private val onDead: () -> Unit) : DictationEvents {
    override fun ready(sessionId: String, modelId: String, round: Int?) = send { callback.onReady(SpeechBundles.ready(sessionId, modelId, round)) }
    override fun partial(text: String, segmentIndex: Int) = send { callback.onPartial(SpeechBundles.partial(text, segmentIndex)) }
    override fun final(utterance: Utterance) = send { callback.onFinal(SpeechBundles.utterance(utterance)) }
    override fun endpoint() = send { callback.onEndpoint() }
    override fun voiceActivity(active: Boolean) = send { callback.onVoiceActivity(SpeechBundles.activity(active)) }
    override fun warning(code: String, message: String) = send { callback.onWarning(SpeechBundles.problem(code, message)) }
    override fun error(code: String, message: String) = send { callback.onError(SpeechBundles.problem(code, message)) }
    override fun end(reason: String, text: String, round: Int?) = send { callback.onEnd(SpeechBundles.end(reason, text, round)) }

    private inline fun send(block: () -> Unit) {
        try {
            block()
        } catch (_: RemoteException) {
            onDead()
        }
    }
}

/** [WakeEvents] delivered to an `ISpeechWakeCallback`. zh-CN: 投递到 `ISpeechWakeCallback` 的 [WakeEvents]. */
internal class CallbackWakeEvents(private val callback: ISpeechWakeCallback, private val onDead: () -> Unit) : WakeEvents {
    override fun ready(sessionId: String, modelId: String) = send { callback.onReady(SpeechBundles.ready(sessionId, modelId, null)) }
    override fun wake(keyword: String, score: Float, timestampMillis: Long, round: Int?) = send { callback.onWake(SpeechBundles.wake(keyword, score, timestampMillis, round)) }
    override fun warning(code: String, message: String) = send { callback.onWarning(SpeechBundles.problem(code, message)) }
    override fun error(code: String, message: String) = send { callback.onError(SpeechBundles.problem(code, message)) }
    override fun end(reason: String) = send { callback.onEnd(SpeechBundles.end(reason, null, null)) }

    private inline fun send(block: () -> Unit) {
        try {
            block()
        } catch (_: RemoteException) {
            onDead()
        }
    }
}

/**
 * What every session Binder shares (roadmap D16): the admission slot, the death links of the host callbacks, the
 * engine leases, the microphone and its foreground service, and a cleanup that runs exactly once when the session
 * ended. Everything but the Binder entry points runs on the ASR thread.
 * zh-CN: 每个会话 Binder 共有的部分 (路线图 D16): 准入位置, 宿主回调的死亡监听, 引擎租约, 麦克风及其前台服务, 以及会话结束时恰好运行一次的清理.
 * 除 Binder 入口外, 一切都在 ASR 线程上运行.
 */
internal abstract class SessionBinder(
    protected val runtime: SpeechRuntime,
    private val slot: SessionTable.Slot,
    private val callbacks: List<IBinder>,
) : ISpeechSession.Stub(), IBinder.DeathRecipient, MicrophoneService.Listener {

    protected val leases = ArrayList<EngineCache.Lease<out Closeable>>()
    private val cleaned = AtomicBoolean(false)
    private val pending = AtomicLong(0)
    private val pendingLock = ReentrantLock()
    private val drained = pendingLock.newCondition()
    private var microphone: MicrophoneSource? = null
    private var foreground = false
    private var behindWarned = false

    protected abstract val isEnded: Boolean
    protected abstract fun accept(samples: FloatArray)
    protected abstract fun stopSession(discard: Boolean)
    protected abstract fun failSession(code: String, message: String)
    protected abstract fun warn(code: String, message: String)

    fun link() {
        callbacks.forEach { binder ->
            try {
                binder.linkToDeath(this, 0)
            } catch (_: RemoteException) {
                binderDied()
            }
        }
    }

    /** Starts the microphone foreground service while the plugin still has the host's capability (Q5 (a)). zh-CN: 趁插件仍有宿主能力时启动麦克风前台服务. */
    fun holdForeground() {
        foreground = true
        MicrophoneService.acquire(runtime.context, this)
    }

    override fun onRefused(reason: String) {
        onAsr {
            warn(SpeechErrorCodes.PERMISSION_DENIED, "Android refused the microphone foreground service of 3-Macaw Speech ($reason); recording may be silenced while AutoJs6 is in the background")
        }
    }

    override fun binderDied() {
        onAsr {
            stopSession(discard = true)
            cleanup()
        }
    }

    /** Loads on the loader thread, then continues on the ASR thread; a failure ends the session. zh-CN: 在加载线程上加载, 再在 ASR 线程上继续; 失败即结束会话. */
    protected fun loadThen(load: () -> List<EngineCache.Lease<out Closeable>>, then: () -> Unit) {
        runtime.load {
            val acquired = try {
                load()
            } catch (e: SpeechException) {
                onAsr { fail(e.code, e.message ?: e.code) }
                return@load
            } catch (e: Throwable) {
                onAsr { fail(SpeechErrorCodes.ENGINE_FAILED, "Loading the model failed: ${e.message ?: e.javaClass.simpleName}") }
                return@load
            }
            onAsr {
                leases += acquired
                if (isEnded || cleaned.get()) {
                    cleanup()
                    return@onAsr
                }
                try {
                    then()
                } catch (e: SpeechException) {
                    fail(e.code, e.message ?: e.code)
                } catch (e: RuntimeException) {
                    fail(SpeechErrorCodes.ENGINE_FAILED, "Starting the session failed: ${e.message ?: e.javaClass.simpleName}")
                }
                if (isEnded) cleanup()
            }
        }
    }

    /** Starts recording into [accept] (ASR thread). zh-CN: 开始录音并交给 [accept] (ASR 线程). */
    protected fun startMicrophone() {
        val source = MicrophoneSource(object : MicrophoneSource.Listener {
            override fun onAudio(samples: FloatArray) = enqueue(samples, microphone = true)

            override fun onSilenced(silenced: Boolean) {
                if (!silenced) return
                onAsr {
                    warn(SpeechErrorCodes.PERMISSION_DENIED, "Android silences the microphone of 3-Macaw Speech: it has no microphone capability while AutoJs6 is in the background without one")
                }
            }

            override fun onFailure(error: SpeechException) = onAsr { fail(error.code, error.message ?: error.code) }
        })
        source.start()
        microphone = source
    }

    /**
     * Queues 16 kHz audio for the ASR thread. External feeds wait while more than [MAX_PENDING_SAMPLES] are queued
     * (back pressure for a host that feeds faster than the device decodes); microphone frames are dropped instead,
     * with one warning.
     * zh-CN: 把 16 kHz 音频排入 ASR 线程. 排队超过 [MAX_PENDING_SAMPLES] 时外部输入等待 (宿主送入快于设备解码时的背压); 麦克风帧则被丢弃, 并给出一次警告.
     */
    protected fun enqueue(samples: FloatArray, microphone: Boolean) {
        if (pending.get() > MAX_PENDING_SAMPLES) {
            if (microphone) {
                if (!behindWarned) {
                    behindWarned = true
                    onAsr { warn(SpeechErrorCodes.ENGINE_FAILED, "Recognition falls behind the microphone; audio was dropped") }
                }
                return
            }
            var left = TimeUnit.MILLISECONDS.toNanos(FEED_WAIT_MILLIS)
            pendingLock.withLock {
                while (pending.get() > MAX_PENDING_SAMPLES && !isEnded && left > 0) left = drained.awaitNanos(left)
            }
        }
        pending.addAndGet(samples.size.toLong())
        onAsr {
            try {
                if (!isEnded) accept(samples)
                if (isEnded) cleanup()
            } finally {
                pending.addAndGet(-samples.size.toLong())
                pendingLock.withLock { drained.signalAll() }
            }
        }
    }

    protected fun fail(code: String, message: String) {
        failSession(code, message)
        cleanup()
    }

    protected fun onAsr(block: () -> Unit) {
        try {
            runtime.asr.execute {
                try {
                    block()
                } catch (t: Throwable) {
                    Log.e(TAG, "Session task failed", t)
                    runCatching { failSession(SpeechErrorCodes.INTERNAL, t.message ?: t.javaClass.simpleName) }
                    cleanup()
                }
            }
        } catch (e: java.util.concurrent.RejectedExecutionException) {
            Log.w(TAG, "ASR thread is gone: $e")
        }
    }

    /** Releases everything the session held; runs once, on the ASR thread. zh-CN: 释放会话持有的一切; 只在 ASR 线程上运行一次. */
    protected fun cleanup() {
        if (!cleaned.compareAndSet(false, true)) return
        microphone?.stop()
        microphone = null
        if (foreground) MicrophoneService.release(this)
        runtime.sessions.release(slot)
        callbacks.forEach { runCatching { it.unlinkToDeath(this, 0) } }
        leases.forEach { runCatching { it.close() } }
        leases.clear()
    }

    // ISpeechSession: the entry points post to the ASR thread.

    override fun finish() = onAsr {
        stopSession(discard = false)
        cleanup()
    }

    override fun cancel() = onAsr {
        stopSession(discard = true)
        cleanup()
    }

    override fun isActive(): Boolean = !isEnded

    protected fun closed(): Nothing = throw SpeechBundles.thrown(SpeechErrorCodes.SESSION_CLOSED, "The session has ended")

    companion object {
        const val TAG = "MacawSessions"

        /** About 16 s of 16 kHz audio queued for the ASR thread. zh-CN: 排给 ASR 线程的约 16 秒 16 kHz 音频. */
        const val MAX_PENDING_SAMPLES = 256_000L
        private const val FEED_WAIT_MILLIS = 25_000L
    }
}

/**
 * A dictation session (`openSession`, roadmap P3.2): microphone or external PCM, streaming or offline model.
 * zh-CN: 听写会话 (`openSession`, 路线图 P3.2): 麦克风或外部 PCM, 流式或离线模型.
 */
internal class DictationBinder(
    runtime: SpeechRuntime,
    slot: SessionTable.Slot,
    callback: ISpeechSessionCallback,
    private val external: Boolean,
    makeSession: (DictationEvents) -> DictationSession,
) : SessionBinder(runtime, slot, listOf(callback.asBinder())) {

    private val session: DictationSession = makeSession(CallbackDictationEvents(callback) { binderDied() })

    override val isEnded: Boolean get() = session.isEnded

    fun open(model: InstalledModel, warnings: List<SpeechWarning>) {
        link()
        if (!external) holdForeground()
        val options = session.options
        loadThen({ listOf(runtime.acquireAsr(model, options.language, options.hotwords.isNotEmpty())) }) {
            val engine = leases.first().engine
            session.start(model.modelId) { sink -> runtime.dictation(engine, options, sink) }
            warnings.forEach { session.warning(it.code, it.message) }
            if (!external && !session.isEnded) startMicrophone()
        }
    }

    override fun accept(samples: FloatArray) = session.accept(samples)

    override fun stopSession(discard: Boolean) = if (discard) session.cancel() else session.finish()

    override fun failSession(code: String, message: String) = session.fail(code, message)

    override fun warn(code: String, message: String) = session.warning(code, message)

    override fun feed(pcm16: ByteArray?) {
        if (!external) throw SpeechBundles.thrown(SpeechErrorCodes.INVALID_OPTIONS, "feed() is for sessions opened with source \"external\"")
        if (session.isEnded) closed()
        val bytes = pcm16 ?: return
        if (bytes.size > SpeechLimits.MAX_FEED_BYTES) throw SpeechBundles.thrown(SpeechErrorCodes.LIMIT_EXCEEDED, "One feed() takes at most ${SpeechLimits.MAX_FEED_BYTES} bytes, not ${bytes.size}")
        if (bytes.size % 2 != 0) throw SpeechBundles.thrown(SpeechErrorCodes.INVALID_OPTIONS, "PCM16 audio has an even byte count, not ${bytes.size}")
        if (bytes.isEmpty()) return
        enqueue(AudioInput.pcm16(bytes), microphone = false)
    }

    override fun snapshot(): Bundle = SpeechBundles.snapshot(session.id, session.state.wire, session.modelId, session.text)

    override fun finishRound(discard: Boolean) = Unit
}

/**
 * A wake session (`openWakeSession`, roadmap P3.4): keyword spotting on the microphone and, with `thenListen`, one
 * dictation round per hit on the same audio, on a second recognizer lease.
 * zh-CN: 唤醒会话 (`openWakeSession`, 路线图 P3.4): 在麦克风上检测关键词; 有 `thenListen` 时每次命中在同一音频上进行一轮听写, 使用第二个识别器租约.
 */
internal class WakeBinder(
    runtime: SpeechRuntime,
    slot: SessionTable.Slot,
    callback: ISpeechWakeCallback,
    listenCallback: ISpeechSessionCallback?,
    makeSession: (WakeEvents, DictationEvents?) -> WakeSession,
) : SessionBinder(runtime, slot, listOfNotNull(callback.asBinder(), listenCallback?.asBinder())) {

    private val session: WakeSession = makeSession(
        CallbackWakeEvents(callback) { binderDied() },
        listenCallback?.let { listen -> CallbackDictationEvents(listen) { binderDied() } },
    )

    override val isEnded: Boolean get() = session.isEnded

    fun open(keywordModel: InstalledModel, buffer: String, listenModel: InstalledModel?) {
        link()
        holdForeground()
        val listen = session.options.thenListen
        loadThen({
            val acquired = ArrayList<EngineCache.Lease<out Closeable>>()
            try {
                acquired += runtime.acquireKeywords(keywordModel)
                if (listen != null && listenModel != null) acquired += runtime.acquireAsr(listenModel, listen.language, listen.hotwords.isNotEmpty())
            } catch (t: Throwable) {
                acquired.forEach { runCatching { it.close() } }
                throw t
            }
            acquired
        }) {
            val spotter = leases.first().engine as KeywordEngine
            val stream = spotter.stream(buffer, session.options.keywords.size)
            val asr = leases.getOrNull(1)?.engine
            session.start(
                keywordModel.modelId,
                stream,
                listenModel?.modelId,
                if (listen != null && asr != null) { sink -> runtime.dictation(asr, listen, sink) } else null,
            )
            if (!session.isEnded) startMicrophone()
        }
    }

    override fun accept(samples: FloatArray) = session.accept(samples)

    override fun stopSession(discard: Boolean) = if (discard) session.cancel() else session.finish()

    override fun failSession(code: String, message: String) = session.fail(code, message)

    override fun warn(code: String, message: String) = session.warning(code, message)

    override fun feed(pcm16: ByteArray?): Unit =
        throw SpeechBundles.thrown(SpeechErrorCodes.INVALID_OPTIONS, "feed() is for sessions opened with source \"external\"")

    override fun snapshot(): Bundle = SpeechBundles.snapshot(session.id, session.state.wire, session.modelId, session.text)

    override fun finishRound(discard: Boolean) = onAsr {
        session.finishRound(discard)
        if (session.isEnded) cleanup()
    }
}
