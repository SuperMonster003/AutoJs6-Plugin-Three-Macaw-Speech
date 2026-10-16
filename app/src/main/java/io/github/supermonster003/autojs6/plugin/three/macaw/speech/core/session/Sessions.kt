package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes

/**
 * The events of a dictation channel, one to one with `ISpeechSessionCallback` (`docs/dev/speech-plugin-protocol-v1.md`):
 * [ready] first (or [error] then [end] when the model fails), recognition events in audio order and exactly one
 * [end] last. [round] is set on the listen channel of a wake session.
 * zh-CN: 听写通道的事件, 与 `ISpeechSessionCallback` 一一对应: 先 [ready] (模型失败时为 [error] 后接 [end]), 识别事件按音频顺序, 最后恰好一个
 * [end]. 唤醒会话的听写通道带 [round].
 */
interface DictationEvents {
    fun ready(sessionId: String, modelId: String, round: Int?)
    fun partial(text: String, segmentIndex: Int)
    fun final(utterance: Utterance)
    fun endpoint()
    fun voiceActivity(active: Boolean)
    fun warning(code: String, message: String)
    fun error(code: String, message: String)
    fun end(reason: String, text: String, round: Int?)
}

/** The events of a wake channel, one to one with `ISpeechWakeCallback`. zh-CN: 唤醒通道的事件, 与 `ISpeechWakeCallback` 一一对应. */
interface WakeEvents {
    fun ready(sessionId: String, modelId: String)
    fun wake(keyword: String, score: Float, timestampMillis: Long, round: Int?)
    fun warning(code: String, message: String)
    fun error(code: String, message: String)
    fun end(reason: String)
}

/** `snapshot` states of the contract. zh-CN: 契约的 `snapshot` 状态. */
enum class SessionState(val wire: String) {
    PENDING(SpeechContract.STATE_PENDING),
    ACTIVE(SpeechContract.STATE_ACTIVE),
    ENDED(SpeechContract.STATE_ENDED),
}

/**
 * The life cycle of one dictation session or wake round (roadmap D16, D17): `pending` while the model loads, `active`
 * from [start] (its `ready`) until exactly one `end`. The session ends on [finish] (flush, `stopped`), [cancel]
 * (`cancelled`, no final), [fail] (`error` then `end(error)`), when the audio it consumed reaches `maxDurationMillis`
 * (`maxDuration`) or when `timeoutMillis` of audio passed without speech or new text (`timeout`); a wake round
 * ([endOnFinal]) also ends with `stopped` after its first final. Every call comes from the thread that owns the
 * session; [state] and [text] may be read from any thread. Pure Kotlin: the clocks are the 16 kHz samples consumed,
 * so a fed external session times out by audio, not by wall time.
 * zh-CN: 一个听写会话或唤醒轮次的生命周期 (路线图 D16, D17): 模型加载期间为 `pending`, 自 [start] (其 `ready`) 起为 `active`, 直到恰好一个
 * `end`. 会话在以下情况结束: [finish] (冲刷, `stopped`), [cancel] (`cancelled`, 无 final), [fail] (`error` 后 `end(error)`), 消费的音频达到
 * `maxDurationMillis` (`maxDuration`), 或经过 `timeoutMillis` 的音频仍无语音或新文本 (`timeout`); 唤醒轮次 ([endOnFinal]) 在首个 final 之后
 * 以 `stopped` 结束. 所有调用来自持有会话的线程; [state] 与 [text] 可从任意线程读取. 纯 Kotlin: 时钟为已消费的 16 kHz 采样, 因此外部会话按音频
 * 而非墙钟超时.
 */
class DictationSession(
    val id: String,
    val options: ListenOptions,
    private val events: DictationEvents,
    val round: Int? = null,
    private val endOnFinal: Boolean = false,
) {

    @Volatile
    var state: SessionState = SessionState.PENDING
        private set

    @Volatile
    var modelId: String? = null
        private set

    @Volatile
    var text: String = ""
        private set

    @Volatile
    var reason: String? = null
        private set

    private var dictation: Dictation? = null
    private var muted = false
    private var endRequested = false

    val isEnded: Boolean get() = state == SessionState.ENDED

    private val sink = object : DictationSink {
        override fun partial(text: String, segmentIndex: Int) {
            if (!muted) events.partial(text, segmentIndex)
        }

        override fun final(utterance: Utterance) {
            if (muted) return
            events.final(utterance)
            text = SpeechText.join(dictation?.finals.orEmpty() + utterance.text)
            if (endOnFinal) endRequested = true
        }

        override fun endpoint() {
            if (muted) return
            events.endpoint()
            if (endRequested) muted = true
        }

        override fun voiceActivity(active: Boolean) {
            if (!muted) events.voiceActivity(active)
        }
    }

    /**
     * The model is ready: [create] builds the dictation on the sink of this session, `ready` follows. A session that
     * ended while its model loaded closes the dictation at once.
     * zh-CN: 模型已就绪: [create] 以本会话的 sink 构建听写, 随后发出 `ready`. 模型加载期间已结束的会话立即关闭该听写.
     */
    fun start(modelId: String, create: (DictationSink) -> Dictation) {
        if (state != SessionState.PENDING) return
        val built = create(sink)
        if (state != SessionState.PENDING) {
            built.close()
            return
        }
        this.modelId = modelId
        dictation = built
        state = SessionState.ACTIVE
        events.ready(id, modelId, round)
    }

    /** 16 kHz mono audio. zh-CN: 16 kHz 单声道音频. */
    fun accept(samples: FloatArray) {
        if (state != SessionState.ACTIVE) return
        val current = dictation ?: return
        guarded {
            current.accept(samples)
            when {
                endRequested -> stop(SpeechContract.REASON_STOPPED, flush = false)
                AudioInput.millis(current.position) >= options.maxDurationMillis -> stop(SpeechContract.REASON_MAX_DURATION, flush = true)
                options.timeoutMillis > 0 && AudioInput.millis(current.position - current.lastActivity) >= options.timeoutMillis ->
                    stop(SpeechContract.REASON_TIMEOUT, flush = true)
            }
        }
    }

    fun finish() = guarded { stop(SpeechContract.REASON_STOPPED, flush = true) }

    fun cancel() = guarded { stop(SpeechContract.REASON_CANCELLED, flush = false) }

    fun warning(code: String, message: String) {
        if (state != SessionState.ENDED) events.warning(code, message)
    }

    fun fail(code: String, message: String) {
        if (state == SessionState.ENDED) return
        state = SessionState.ENDED
        reason = SpeechContract.REASON_ERROR
        events.error(code, message)
        events.end(SpeechContract.REASON_ERROR, text, round)
        closeDictation()
    }

    private fun stop(reason: String, flush: Boolean) {
        if (state == SessionState.ENDED) return
        val current = dictation
        if (state == SessionState.ACTIVE && current != null) {
            if (!flush || endRequested) muted = true
            current.flush(emitFinal = flush && !muted)
        }
        state = SessionState.ENDED
        this.reason = reason
        events.end(reason, text, round)
        closeDictation()
    }

    private fun closeDictation() {
        val current = dictation ?: return
        dictation = null
        runCatching { current.close() }
    }

    /** A runtime failure while consuming audio ends the session with `ENGINE_FAILED`. zh-CN: 消费音频时的运行时失败以 `ENGINE_FAILED` 结束会话. */
    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: SpeechException) {
            fail(e.code, e.message ?: e.code)
        } catch (e: RuntimeException) {
            fail(SpeechErrorCodes.ENGINE_FAILED, "Recognition failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

/**
 * A wake session (roadmap D18): keyword spotting on the microphone, one `wake` per hit (hits closer than
 * `minIntervalMillis` of audio to the previous one are dropped), and with `thenListen` one dictation round per hit on
 * the same audio stream (round index 0, 1, ...), after which spotting resumes from scratch. [finishRound] ends the
 * current round (`stopped`, or `cancelled` with discard), [finish] / [cancel] end the round and then the session.
 * The keyword stream reports the keyword index; the event carries the keyword as the script wrote it and [score] is
 * its trigger threshold, because sherpa-onnx 1.13.8 reports no per-hit score.
 * zh-CN: 唤醒会话 (路线图 D18): 在麦克风上检测关键词, 每次命中一个 `wake` (与上次命中相距不足 `minIntervalMillis` 音频的命中被丢弃); 有
 * `thenListen` 时每次命中在同一音频流上进行一轮听写 (轮次序号 0, 1, ...), 之后从头恢复检测. [finishRound] 结束当前轮次 (`stopped`, 丢弃时为
 * `cancelled`), [finish] / [cancel] 先结束轮次再结束会话. 关键词流报告关键词序号; 事件携带脚本所写的关键词, 分数为其触发阈值, 因为
 * sherpa-onnx 1.13.8 不报告每次命中的分数.
 */
class WakeSession(
    val id: String,
    val options: WakeOptions,
    private val events: WakeEvents,
    private val listenEvents: DictationEvents?,
    private val defaultThreshold: Float,
    private val now: () -> Long = System::currentTimeMillis,
) {

    @Volatile
    var state: SessionState = SessionState.PENDING
        private set

    @Volatile
    var modelId: String? = null
        private set

    @Volatile
    var text: String = ""
        private set

    private var stream: KeywordStream? = null
    private var listenModelId: String? = null
    private var newRound: ((DictationSink) -> Dictation)? = null
    private var round: DictationSession? = null
    private var rounds = 0
    private var position = 0L
    private var lastHit: Long? = null
    private val texts = ArrayList<String>()

    val isEnded: Boolean get() = state == SessionState.ENDED

    /** The round in progress, if any. zh-CN: 进行中的轮次 (如有). */
    val currentRound: DictationSession? get() = round

    /**
     * Models are ready: [stream] spots the keywords; [roundFactory] builds the dictation of a `thenListen` round on
     * [listenModel].
     * zh-CN: 模型已就绪: [stream] 检测关键词; [roundFactory] 在 [listenModel] 上构建 `thenListen` 轮次的听写.
     */
    fun start(modelId: String, stream: KeywordStream, listenModel: String? = null, roundFactory: ((DictationSink) -> Dictation)? = null) {
        if (state != SessionState.PENDING) {
            runCatching { stream.close() }
            return
        }
        require(options.thenListen == null || (listenModel != null && roundFactory != null && listenEvents != null))
        this.modelId = modelId
        this.stream = stream
        listenModelId = listenModel
        newRound = roundFactory
        state = SessionState.ACTIVE
        events.ready(id, modelId)
    }

    fun accept(samples: FloatArray) {
        if (state != SessionState.ACTIVE) return
        position += samples.size
        val current = round
        if (current != null) {
            current.accept(samples)
            if (current.isEnded) closeRound(current)
            return
        }
        val spotter = stream ?: return
        try {
            spotter.accept(samples)
            val hit = spotter.poll() ?: return
            val at = AudioInput.millis(position)
            val previous = lastHit
            if (previous != null && at - previous < options.minIntervalMillis) return
            lastHit = at
            onHit(options.keywords[hit])
        } catch (e: RuntimeException) {
            fail(SpeechErrorCodes.ENGINE_FAILED, "Keyword spotting failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    fun finishRound(discard: Boolean) {
        val current = round ?: return
        if (discard) current.cancel() else current.finish()
        closeRound(current)
    }

    fun finish() = stop(SpeechContract.REASON_STOPPED, discard = false)

    fun cancel() = stop(SpeechContract.REASON_CANCELLED, discard = true)

    fun warning(code: String, message: String) {
        if (state == SessionState.ENDED) return
        round?.warning(code, message)
        events.warning(code, message)
    }

    fun fail(code: String, message: String) {
        if (state == SessionState.ENDED) return
        round?.fail(code, message)
        round = null
        state = SessionState.ENDED
        events.error(code, message)
        events.end(SpeechContract.REASON_ERROR)
        closeStream()
    }

    private fun onHit(keyword: Keyword) {
        val listen = options.thenListen
        val index = if (listen != null) rounds++ else null
        events.wake(keyword.text, keyword.threshold ?: defaultThreshold, now(), index)
        if (listen == null || index == null) return
        val session = DictationSession("$id/$index", listen, listenEvents!!, round = index, endOnFinal = true)
        session.start(listenModelId!!, newRound!!)
        round = session
    }

    private fun closeRound(current: DictationSession) {
        if (round !== current) return
        round = null
        if (current.text.isNotEmpty()) {
            texts += current.text
            text = SpeechText.join(texts)
        }
        stream?.restart()
    }

    private fun stop(reason: String, discard: Boolean) {
        if (state == SessionState.ENDED) return
        round?.let { current ->
            if (discard) current.cancel() else current.finish()
            closeRound(current)
        }
        state = SessionState.ENDED
        events.end(reason)
        closeStream()
    }

    private fun closeStream() {
        val current = stream ?: return
        stream = null
        runCatching { current.close() }
    }
}
