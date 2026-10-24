package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioCapture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioCaptures
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.EarlyAudio
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MicrophoneSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineCache
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.KeywordEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationEvents
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SessionState
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SessionTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechWarning
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceGatedKeywords
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeEvents
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakePower
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
 * the session through [onDead]; [beforeEnd] runs before the `onEnd` of the session itself (not of a wake round) leaves.
 * zh-CN: 投递到 `ISpeechSessionCallback` (oneway) 的 [DictationEvents]. 回调已失效 (宿主进程死亡) 时经 [onDead] 取消会话; [beforeEnd]
 * 在会话本身 (而非唤醒轮次) 的 `onEnd` 发出之前运行.
 */
internal class CallbackDictationEvents(
    private val callback: ISpeechSessionCallback,
    private val beforeEnd: () -> Unit = {},
    private val onDead: () -> Unit,
) : DictationEvents {
    override fun ready(sessionId: String, modelId: String, round: Int?) = send { callback.onReady(SpeechBundles.ready(sessionId, modelId, round)) }
    override fun partial(text: String, segmentIndex: Int) = send { callback.onPartial(SpeechBundles.partial(text, segmentIndex)) }
    override fun final(utterance: Utterance) = send { callback.onFinal(SpeechBundles.utterance(utterance)) }
    override fun endpoint() = send { callback.onEndpoint() }
    override fun voiceActivity(active: Boolean) = send { callback.onVoiceActivity(SpeechBundles.activity(active)) }
    override fun warning(code: String, message: String) = send { callback.onWarning(SpeechBundles.problem(code, message)) }
    override fun error(code: String, message: String) = send { callback.onError(SpeechBundles.problem(code, message)) }
    override fun end(reason: String, text: String, round: Int?) {
        if (round == null) beforeEnd()
        send { callback.onEnd(SpeechBundles.end(reason, text, round)) }
    }

    private inline fun send(block: () -> Unit) {
        try {
            block()
        } catch (_: RemoteException) {
            onDead()
        }
    }
}

/**
 * [WakeEvents] delivered to an `ISpeechWakeCallback`; [beforeEnd] runs before `onEnd` leaves.
 * zh-CN: 投递到 `ISpeechWakeCallback` 的 [WakeEvents]; [beforeEnd] 在 `onEnd` 发出之前运行.
 */
internal class CallbackWakeEvents(
    private val callback: ISpeechWakeCallback,
    private val beforeEnd: () -> Unit = {},
    private val onDead: () -> Unit,
) : WakeEvents {
    override fun ready(sessionId: String, modelId: String) = send { callback.onReady(SpeechBundles.ready(sessionId, modelId, null)) }
    override fun wake(keyword: String, score: Float, timestampMillis: Long, round: Int?) = send { callback.onWake(SpeechBundles.wake(keyword, score, timestampMillis, round)) }
    override fun warning(code: String, message: String) = send { callback.onWarning(SpeechBundles.problem(code, message)) }
    override fun error(code: String, message: String) = send { callback.onError(SpeechBundles.problem(code, message)) }
    override fun end(reason: String) {
        beforeEnd()
        send { callback.onEnd(SpeechBundles.end(reason, null, null)) }
    }

    private inline fun send(block: () -> Unit) {
        try {
            block()
        } catch (_: RemoteException) {
            onDead()
        }
    }
}

/**
 * How a session of the plugin's own entries runs (roadmap D56, D57): [takeover] silences a wake session that holds the
 * microphone instead of failing with `SESSION_BUSY`; [foreground] holds the microphone foreground service (an entry
 * that records only while a screen of the plugin or of its caller is visible does without it); [attribution] is the
 * context the recording is attributed to (the caller of the recognition service, API 31+); [recording] is called on the
 * ASR thread once such an entry without the foreground service records, which it does before the model is ready (the
 * audio waits in [EarlyAudio]). Never reachable over the contract: only in-process callers pass it.
 * zh-CN: 插件自己入口的会话如何运行 (路线图 D56, D57): [takeover] 让占用麦克风的唤醒会话静默, 而不是以 `SESSION_BUSY` 失败; [foreground] 持有麦克风
 * 前台服务 (只在插件或其调用方界面可见时录音的入口不需要); [attribution] 是录音所归属的 Context (识别服务的调用方, API 31+); 不持有前台服务的入口在模型
 * 就绪之前就开始录音 (音频在 [EarlyAudio] 中等待), 开始录音时在 ASR 线程上调用 [recording]. 契约无法传入: 只有进程内调用方会传.
 */
internal data class LocalSession(
    val takeover: Boolean = true,
    val foreground: Boolean = true,
    val attribution: Context? = null,
    val recording: (() -> Unit)? = null,
)

/**
 * What every session Binder shares (roadmap D16): the admission slot, the death links of the host callbacks, the
 * engine leases, the microphone and its foreground service, and a cleanup that runs exactly once when the session
 * ended. Everything but the Binder entry points runs on the ASR thread.
 * zh-CN: 每个会话 Binder 共有的部分 (路线图 D16): 准入位置, 宿主回调的死亡监听, 引擎租约, 麦克风及其前台服务, 以及会话结束时恰好运行一次的清理.
 * 除 Binder 入口外, 一切都在 ASR 线程上运行.
 */
internal abstract class SessionBinder(
    protected val runtime: SpeechRuntime,
    protected val slot: SessionTable.Slot,
    private val callbacks: List<IBinder>,
    private val attribution: Context? = null,
) : ISpeechSession.Stub(), IBinder.DeathRecipient, MicrophoneService.Listener {

    protected val leases = ArrayList<EngineCache.Lease<out Closeable>>()
    private val cleaned = AtomicBoolean(false)
    private val freed = AtomicBoolean(false)
    private val pending = AtomicLong(0)
    private val pendingLock = ReentrantLock()
    private val drained = pendingLock.newCondition()
    private var microphone: AudioCapture? = null
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

    /** True while the microphone records for this session. zh-CN: 麦克风正在为本会话录音. */
    protected val recording: Boolean get() = microphone != null

    /** The microphone's read step (roadmap P8.1). zh-CN: 麦克风的读取步长 (路线图 P8.1). */
    protected open val frameSamples: Int get() = MicrophoneSource.FRAME_SAMPLES

    /** Starts recording into [accept] (ASR thread). zh-CN: 开始录音并交给 [accept] (ASR 线程). */
    protected fun startMicrophone() {
        val source = AudioCaptures.open(object : MicrophoneSource.Listener {
            override fun onAudio(samples: FloatArray) = enqueue(samples, microphone = true)

            override fun onSilenced(silenced: Boolean) {
                if (!silenced) return
                onAsr {
                    warn(SpeechErrorCodes.PERMISSION_DENIED, "Android silences the microphone of 3-Macaw Speech: it has no microphone capability while AutoJs6 is in the background without one")
                }
            }

            override fun onFailure(error: SpeechException) = onAsr { fail(error.code, error.message ?: error.code) }
        }, attribution, frameSamples)
        source.start()
        microphone = source
    }

    /** Stops recording; frames already queued still reach [accept] (ASR thread). zh-CN: 停止录音; 已排队的帧仍会交给 [accept] (ASR 线程). */
    protected fun stopMicrophone() {
        microphone?.stop()
        microphone = null
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

    /**
     * Stops recording and frees the microphone foreground service and the admission slot (ASR thread, once). It runs
     * before the terminal `onEnd` leaves, so a client that opens its next session as soon as it sees the end finds the
     * microphone free: a script's `speech.wake` 43 ms after a `speech.listen` ended failed with `SESSION_BUSY` on a Sony
     * XQ-AT72 (roadmap P6.3 smoke run) when this happened only in [cleanup]. The engine leases stay until [cleanup]: the
     * session still closes its recognizer stream after the end.
     * zh-CN: 停止录音并释放麦克风前台服务与准入位置 (ASR 线程, 一次). 它在终结的 `onEnd` 发出之前运行, 客户端一看到结束就打开下一个会话时麦克风已空闲:
     * 只在 [cleanup] 中释放时, Sony XQ-AT72 上脚本在 `speech.listen` 结束 43 ms 后打开的 `speech.wake` 以 `SESSION_BUSY` 失败 (路线图 P6.3
     * 冒烟). 引擎租约留到 [cleanup]: 会话在结束之后仍要关闭识别流.
     */
    protected fun freeMicrophone() {
        if (!freed.compareAndSet(false, true)) return
        microphone?.stop()
        microphone = null
        if (foreground) MicrophoneService.release(this)
        runtime.sessions.release(slot)
    }

    /** Releases everything the session held; runs once, on the ASR thread. zh-CN: 释放会话持有的一切; 只在 ASR 线程上运行一次. */
    protected fun cleanup() {
        if (!cleaned.compareAndSet(false, true)) return
        freeMicrophone()
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
    private val foreground: Boolean,
    attribution: Context?,
    private val recordingStarted: (() -> Unit)? = null,
    makeSession: (DictationEvents) -> DictationSession,
) : SessionBinder(runtime, slot, listOf(callback.asBinder()), attribution) {

    private val session: DictationSession = makeSession(CallbackDictationEvents(callback, beforeEnd = { freeMicrophone() }) { binderDied() })
    private val early = EarlyAudio()

    override val isEnded: Boolean get() = session.isEnded

    /**
     * Loads the model, then starts the dictation. A plugin entry without the foreground service (the sheets, the
     * recognition service, the tryout screen) records at once and keeps the audio until the dictation exists, so a word
     * spoken as the sheet appears is not lost while the model loads; a client's session records after `ready`, as the
     * protocol describes.
     * zh-CN: 加载模型后开始听写. 不持有前台服务的插件入口 (弹层, 识别服务, 试听试说页) 立即录音并保留音频直到听写建立, 因此弹层出现时说出的字不会在模型
     * 加载期间丢失; 客户端的会话按协议在 `ready` 之后录音.
     */
    fun open(model: InstalledModel, warnings: List<SpeechWarning>) {
        link()
        if (!external && foreground) holdForeground()
        val options = session.options
        val recordEarly = !external && !foreground
        // Queued after a yielding wake session stops its recording (D57): both run on the ASR thread.
        if (recordEarly) onAsr { startRecording() }
        loadThen({ runtime.acquireRecognition(model, options.language, options.hotwords.isNotEmpty(), options.punctuation) }) {
            val engine = leases.first().engine
            val punctuator = runtime.punctuator(model, leases)
            session.start(model.modelId) { sink -> runtime.dictation(engine, options, sink, punctuator) }
            warnings.forEach { session.warning(it.code, it.message) }
            if (external || session.isEnded) return@loadThen
            if (recording) early.drain { if (!session.isEnded) session.accept(it) } else startMicrophone()
        }
    }

    private fun startRecording() {
        if (isEnded) return
        try {
            startMicrophone()
        } catch (e: SpeechException) {
            fail(e.code, e.message ?: e.code)
            return
        }
        recordingStarted?.invoke()
    }

    override fun accept(samples: FloatArray) {
        if (session.state == SessionState.PENDING) early.add(samples) else session.accept(samples)
    }

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
 * dictation round per hit on the same audio, on a second recognizer lease. Roadmap D57: while no round runs, a
 * dictation of the plugin's own entries may take the microphone over; the session then stops recording, reports
 * nothing and records again when the takeover is released (the microphone foreground service stays held throughout).
 * The resident wake word (roadmap D76) is the same session as the background holder of the session table, without the
 * microphone foreground service of its own ([foreground] false): every microphone session silences it the same way. Its
 * [power] saving mode (roadmap P8.1, D128) puts the silero VAD before the keyword spotter and reads longer microphone
 * frames; script wake sessions keep [WakePower.OFF].
 * zh-CN: 唤醒会话 (`openWakeSession`, 路线图 P3.4): 在麦克风上检测关键词; 有 `thenListen` 时每次命中在同一音频上进行一轮听写, 使用第二个识别器租约.
 * 路线图 D57: 没有轮次进行时, 插件自己入口的听写可以接管麦克风; 此时会话停止录音, 不报告任何事件, 接管释放后重新录音 (期间一直持有麦克风前台服务).
 * 常驻唤醒 (路线图 D76) 是同一种会话, 作为会话表的后台持有者, 不另持麦克风前台服务 ([foreground] 为 false): 任何麦克风会话都以同样方式让它静默.
 * 它的省电模式 [power] (路线图 P8.1, D128) 在唤醒词检测器之前加 silero VAD, 并以更长的帧读取麦克风; 脚本的唤醒会话保持 [WakePower.OFF].
 */
internal class WakeBinder(
    runtime: SpeechRuntime,
    slot: SessionTable.Slot,
    callback: ISpeechWakeCallback,
    listenCallback: ISpeechSessionCallback?,
    private val foreground: Boolean = true,
    private val power: WakePower = WakePower.OFF,
    makeSession: (WakeEvents, DictationEvents?) -> WakeSession,
) : SessionBinder(runtime, slot, listOfNotNull(callback.asBinder(), listenCallback?.asBinder())), SessionTable.Yielding {

    private val session: WakeSession = makeSession(
        CallbackWakeEvents(callback, beforeEnd = { freeMicrophone() }) { binderDied() },
        listenCallback?.let { listen -> CallbackDictationEvents(listen) { binderDied() } },
    )

    private var gate: VoiceGatedKeywords? = null
    private var gateLogged = 0L

    override val isEnded: Boolean get() = session.isEnded

    override val frameSamples: Int get() = power.frameSamples

    fun open(keywordModel: InstalledModel, buffer: String, listenModel: InstalledModel?) {
        link()
        // The resident wake word runs inside its own microphone foreground service (roadmap D75).
        if (foreground) holdForeground()
        runtime.sessions.yieldable(slot, this)
        val listen = session.options.thenListen
        loadThen({
            val acquired = ArrayList<EngineCache.Lease<out Closeable>>()
            try {
                acquired += runtime.acquireKeywords(keywordModel)
                if (listen != null && listenModel != null) {
                    acquired += runtime.acquireRecognition(listenModel, listen.language, listen.hotwords.isNotEmpty(), listen.punctuation)
                }
            } catch (t: Throwable) {
                acquired.forEach { runCatching { it.close() } }
                throw t
            }
            acquired
        }) {
            val spotter = leases.first().engine as KeywordEngine
            val stream = spotter.stream(buffer, session.options.keywords.size).let { stream ->
                if (!power.voiceGate) return@let stream
                VoiceGatedKeywords(stream, SherpaEngines.vad(runtime.context.assets, SpeechRuntime.SESSION_VAD)).also { gate = it }
            }
            Log.i(TAG, "Wake session ${session.id}: voice gate ${if (power.voiceGate) "on" else "off"}, ${power.frameMillis} ms frames")
            val asr = leases.getOrNull(1)?.engine
            val punctuator = listenModel?.let { runtime.punctuator(it, leases) }
            session.start(
                keywordModel.modelId,
                stream,
                listenModel?.modelId,
                if (listen != null && asr != null) { sink -> runtime.dictation(asr, listen, sink, punctuator) } else null,
            )
            // A takeover that came while the models loaded keeps the microphone until it is released (D57).
            if (!session.isEnded && !session.suspended) startMicrophone()
        }
    }

    /** Roadmap D57, with the session table locked. zh-CN: 路线图 D57, 调用时会话表已加锁. */
    override fun yieldMicrophone(): Boolean {
        if (!session.yieldToTakeover()) return false
        Log.i(TAG, "Wake session ${session.id} yields the microphone to a takeover")
        onAsr { stopMicrophone() }
        return true
    }

    override fun resumeMicrophone() = onAsr {
        if (session.isEnded) return@onAsr
        session.resumeAfterTakeover()
        Log.i(TAG, "Wake session ${session.id} resumes after the takeover")
        if (session.state != SessionState.ACTIVE || recording) return@onAsr
        try {
            startMicrophone()
        } catch (e: SpeechException) {
            fail(e.code, e.message ?: e.code)
        }
    }

    override fun accept(samples: FloatArray) {
        session.accept(samples)
        val current = gate ?: return
        // Every 5 minutes of audio: how much of it the spotter decoded, for the device runs (roadmap P8.1).
        if (current.heard - gateLogged >= GATE_LOG_SAMPLES) {
            gateLogged = current.heard
            Log.i(TAG, "Voice gate of ${session.id}: ${AudioInput.millis(current.passed) / 1000} s of ${AudioInput.millis(current.heard) / 1000} s decoded, ${current.openings} openings")
        }
    }

    override fun stopSession(discard: Boolean) = if (discard) session.cancel() else session.finish()

    override fun failSession(code: String, message: String) = session.fail(code, message)

    override fun warn(code: String, message: String) = session.warning(code, message)

    override fun feed(pcm16: ByteArray?): Unit =
        throw SpeechBundles.thrown(SpeechErrorCodes.INVALID_OPTIONS, "feed() is for sessions opened with source \"external\"")

    private companion object {
        val GATE_LOG_SAMPLES = AudioInput.samples(300_000)
    }

    override fun snapshot(): Bundle = SpeechBundles.snapshot(session.id, session.state.wire, session.modelId, session.text)

    override fun finishRound(discard: Boolean) = onAsr {
        session.finishRound(discard)
        if (session.isEnded) cleanup()
    }
}
