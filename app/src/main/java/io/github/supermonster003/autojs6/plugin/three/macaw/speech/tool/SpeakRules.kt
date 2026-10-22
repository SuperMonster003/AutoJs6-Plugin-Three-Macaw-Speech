package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * What `speech_speak` reads with: the language tag passed to the engine, the voice of 3-Macaw Speech's own engine (the
 * named one, else the default voice for the language; null when no installed model speaks it, so only the system
 * engine is asked) and the warnings of the choice.
 * zh-CN: `speech_speak` 的朗读方式: 交给引擎的语言标签, 3-Macaw Speech 自己引擎的语音 (指定的语音, 否则该语言的默认语音; 没有已装模型说该语言时为
 * null, 此时只请系统引擎朗读), 以及选择产生的 warning.
 */
internal data class SpeakPlan(
    val language: String,
    val voice: TtsVoice?,
    /** True when the request named [voice]: the engine is asked for it by name. zh-CN: 请求指定了 [voice]: 按名字向引擎请求. */
    val named: Boolean = false,
    val warnings: List<ToolWarning> = emptyList(),
)

/** What one `speech_speak` call read. zh-CN: 一次 `speech_speak` 调用的朗读情况. */
internal data class SpeakReport(
    val enginePackage: String,
    val language: String,
    val voice: String?,
    val completed: Boolean,
    val durationMillis: Long?,
    val warnings: List<ToolWarning>,
)

/**
 * Roadmap P7.6.3 (D96, D100): the pure rules of `speech_speak`: the language guessed from the text, the engine and voice
 * plan, the messages of the engine problems and errors, and the shape of the result. Pure Kotlin.
 * zh-CN: 路线图 P7.6.3 (D96, D100): `speech_speak` 的纯规则: 从文字推断的语言, 引擎与语音的选择, 引擎问题与错误的信息, 以及结果的形态. 纯 Kotlin.
 */
internal object SpeakRules {
    /** The longest wait for an engine to start. zh-CN: 等待引擎启动的最长时间. */
    const val OPEN_TIMEOUT_MILLIS = 5000L

    /**
     * A reading that the engine no longer reports as speaking this long after it was queued, for this long, is over (an
     * engine that died sends no callback).
     * zh-CN: 排队后经过此时长, 引擎持续此时长不再报告正在朗读的朗读视为已结束 (死掉的引擎不发回调).
     */
    const val GRACE_MILLIS = 3000L

    /** How long a stopped reading may take to report its end. zh-CN: 停止的朗读报告结束可用的时长. */
    const val END_WAIT_MILLIS = 2000L

    const val OWN_ENGINE = "3-Macaw Speech's engine"
    const val SYSTEM_ENGINE = "the system default engine"

    // Values of android.speech.tts.TextToSpeech, repeated so these rules stay pure Kotlin.
    const val ERROR = -1
    const val ERROR_SYNTHESIS = -3
    const val ERROR_SERVICE = -4
    const val ERROR_OUTPUT = -5
    const val ERROR_NETWORK = -6
    const val ERROR_NETWORK_TIMEOUT = -7
    const val ERROR_INVALID_REQUEST = -8
    const val ERROR_NOT_INSTALLED_YET = -9

    /**
     * The language of [text] when the request names none: Japanese with kana, Korean with Hangul, Chinese with Han
     * characters, Russian with Cyrillic, Arabic with Arabic letters, else English.
     * zh-CN: 请求未指定语言时 [text] 的语言: 含假名为日文, 含谚文为韩文, 含汉字为中文, 含西里尔字母为俄文, 含阿拉伯字母为阿拉伯文, 否则为英文.
     */
    fun guessLanguage(text: String): String {
        val scripts = HashSet<Character.UnicodeScript>()
        text.codePoints().forEach { scripts += Character.UnicodeScript.of(it) }
        return when {
            Character.UnicodeScript.HIRAGANA in scripts || Character.UnicodeScript.KATAKANA in scripts -> "ja"
            Character.UnicodeScript.HANGUL in scripts -> "ko"
            Character.UnicodeScript.HAN in scripts -> "zh"
            Character.UnicodeScript.CYRILLIC in scripts -> "ru"
            Character.UnicodeScript.ARABIC in scripts -> "ar"
            else -> "en"
        }
    }

    /**
     * The plan of [request] on the installed voices: a named voice must exist (`INVALID_ARGUMENTS`) and reads in its own
     * language, or in the requested one when the same speaker speaks it; otherwise it is ignored with a warning and the
     * language decides, as without a voice: 3-Macaw Speech's engine when one of its voices speaks the language, else the
     * system engine alone.
     * zh-CN: 按已装语音为 [request] 制定朗读方式: 指定的语音必须存在 (`INVALID_ARGUMENTS`), 以其自身语言朗读, 或在同一说话人说请求的语言时以该语言
     * 朗读; 否则忽略该语音并给出 warning, 与未指定语音一样由语言决定: 3-Macaw Speech 有语音说该语言时用它自己的引擎, 否则只用系统引擎.
     */
    fun plan(request: SpeakRequest, table: TtsVoiceTable): SpeakPlan {
        val warnings = ArrayList<ToolWarning>()
        val named = request.voice?.let { choice ->
            table.voice(choice.toString()) ?: throw PluginToolException(
                PluginToolErrorCodes.INVALID_ARGUMENTS,
                "3-Macaw Speech has no installed voice $choice; a voice is <modelId>/<speaker> of an installed text-to-speech model",
            )
        }
        if (named != null) {
            val language = request.language ?: return SpeakPlan(named.languageTag, named, named = true)
            val variant = if (sameLanguage(named.languageTag, language)) named else table.voices.firstOrNull {
                it.modelId == named.modelId && it.speakerId == named.speakerId && sameLanguage(it.languageTag, language)
            }
            if (variant != null) return SpeakPlan(language, variant, named = true)
            warnings += ToolWarning(SpeechErrorCodes.INVALID_OPTIONS, "The voice ${named.name} does not speak '$language' and was ignored")
        }
        val language = request.language ?: guessLanguage(request.text)
        val locale = Locale.forLanguageTag(language)
        return SpeakPlan(language, table.defaultVoice(locale.language, locale.country.ifEmpty { null }), warnings = warnings)
    }

    /** Why an engine cannot read the language, from its `setLanguage` answer. zh-CN: 引擎不能朗读该语言的原因 (按 `setLanguage` 的应答). */
    fun languageProblem(answer: Int): String = when (answer) {
        TtsVoiceTable.LANG_MISSING_DATA -> "its voice data for the language is not installed"
        else -> "it does not speak the language"
    }

    fun engineLabel(own: Boolean, enginePackage: String?): String =
        if (own) OWN_ENGINE else SYSTEM_ENGINE + enginePackage?.takeIf { it.isNotEmpty() }?.let { " $it" }.orEmpty()

    /** The `NOT_READY` message when no engine could read: every engine asked and its problem. zh-CN: 没有引擎能朗读时的 `NOT_READY` 信息. */
    fun unavailable(language: String, problems: List<Pair<String, String>>): String =
        "No text-to-speech engine can read '$language' (${problems.joinToString("; ") { "${it.first}: ${it.second}" }}). To fix this, " +
            "download a text-to-speech model of 3-Macaw Speech that speaks the language, or install the voice data of the language for the system engine"

    /** The warning when the system engine read because 3-Macaw Speech's engine could not. zh-CN: 自己的引擎不能朗读而由系统引擎朗读时的 warning. */
    fun fallbackWarning(problem: String, named: Boolean): ToolWarning = ToolWarning(
        SpeechErrorCodes.ENGINE_FAILED,
        "$OWN_ENGINE could not read ($problem); the system default engine read instead" + if (named) " and the voice was ignored" else "",
    )

    /** The error of an engine's `onError` code. zh-CN: 引擎 `onError` 错误码对应的错误. */
    fun engineError(code: Int, enginePackage: String): PluginToolException {
        val engine = "The text-to-speech engine ${enginePackage.ifEmpty { "of the system" }}"
        return when (code) {
            ERROR_NOT_INSTALLED_YET -> PluginToolException(PluginToolErrorCodes.NOT_READY, "$engine has not installed the voice data yet (ERROR_NOT_INSTALLED_YET)")
            ERROR_NETWORK, ERROR_NETWORK_TIMEOUT -> PluginToolException(PluginToolErrorCodes.FAILED, "$engine needs the network and could not reach it (${errorName(code)})")
            ERROR_INVALID_REQUEST -> PluginToolException(PluginToolErrorCodes.FAILED, "$engine refused the request (ERROR_INVALID_REQUEST)")
            ERROR_OUTPUT -> PluginToolException(PluginToolErrorCodes.FAILED, "$engine could not play the audio (ERROR_OUTPUT)")
            ERROR_SYNTHESIS -> PluginToolException(PluginToolErrorCodes.FAILED, "$engine could not synthesize the text (ERROR_SYNTHESIS)")
            else -> PluginToolException(PluginToolErrorCodes.FAILED, "$engine failed (${errorName(code)})")
        }
    }

    fun busy(): PluginToolException = PluginToolException(PluginToolErrorCodes.BUSY, "Another speech_speak call is still reading aloud; try again when it ends")

    fun cancelled(): PluginToolException = PluginToolException(PluginToolErrorCodes.CANCELLED, "The reading of speech_speak was cancelled")

    fun expired(): PluginToolException = SpeechToolErrors.of(SpeechErrorCodes.TIMEOUT, "speech_speak ran out of time before reading ended; reading stopped")

    fun silent(enginePackage: String): PluginToolException =
        PluginToolException(PluginToolErrorCodes.FAILED, "The text-to-speech engine ${enginePackage.ifEmpty { "of the system" }} stopped reading without reporting the end")

    /** The tool result of the catalog's output schema. zh-CN: 符合目录输出 schema 的工具结果. */
    fun result(report: SpeakReport, ownPackage: String): Map<String, Any?> = linkedMapOf<String, Any?>().apply {
        put("engine", if (report.enginePackage == ownPackage) SpeechToolCatalog.ENGINE_PLUGIN else SpeechToolCatalog.ENGINE_SYSTEM)
        put("enginePackage", report.enginePackage)
        put("language", report.language)
        report.voice?.let { put("voice", it) }
        put("completed", report.completed)
        report.durationMillis?.let { put("durationMillis", it) }
        put("warnings", report.warnings.map { it.toJson() })
    }

    private fun sameLanguage(a: String, b: String): Boolean = primary(a) == primary(b)

    private fun primary(tag: String): String = tag.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)

    private fun errorName(code: Int): String = when (code) {
        ERROR -> "ERROR"
        ERROR_SERVICE -> "ERROR_SERVICE"
        ERROR_NETWORK -> "ERROR_NETWORK"
        ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        else -> "error $code"
    }
}

/** The progress of one utterance, as `UtteranceProgressListener` reports it. zh-CN: 一段朗读的进度 (同 `UtteranceProgressListener`). */
internal interface SpeakListener {
    fun started()
    fun done()
    fun failed(code: Int)
    fun stopped()
}

/** One connection to a text-to-speech engine (a `TextToSpeech`, or a fake one in the tests). zh-CN: 与一个 TTS 引擎的连接. */
internal interface SpeechOutput {
    /** The engine that answered. zh-CN: 应答的引擎. */
    val enginePackage: String

    /** `setLanguage` of [tag]: a `TextToSpeech.LANG_*` value. zh-CN: 对 [tag] 调用 `setLanguage` 的结果. */
    fun language(tag: String): Int

    /** Selects the engine's voice [name]; false when it has none of that name. zh-CN: 选用引擎中名为 [name] 的语音. */
    fun voice(name: String): Boolean

    fun rate(speed: Float)

    /** Reads [text] in place of anything this connection still reads; false when the engine refused it. zh-CN: 朗读 [text]. */
    fun speak(text: String, listener: SpeakListener): Boolean

    fun isSpeaking(): Boolean

    fun stop()

    fun shutdown()
}

/** Connects to text-to-speech engines. zh-CN: 连接 TTS 引擎. */
internal interface SpeechOutputs {
    /** The package of 3-Macaw Speech's own engine. zh-CN: 3-Macaw Speech 自己引擎的包名. */
    val ownPackage: String

    /** Connects to [enginePackage] (null: the system default engine) within [timeoutMillis]. zh-CN: 在 [timeoutMillis] 内连接引擎. */
    fun open(enginePackage: String?, timeoutMillis: Long): Opened

    sealed interface Opened {
        class Ready(val output: SpeechOutput) : Opened
        class Refused(val reason: String) : Opened
    }
}

/** The cancellation of the tool call. zh-CN: 工具调用的取消. */
internal interface ToolCancellation {
    val cancelled: Boolean

    fun onCancel(hook: Runnable)
}

/**
 * Roadmap P7.6.3 (D96): reads for `speech_speak`, one reading at a time per process. It asks 3-Macaw Speech's engine
 * first when the plan has a voice of it, then the system default engine (never 3-Macaw Speech's again), and answers
 * `NOT_READY` with every engine's problem when none can read. A reading returns when it ends, or as soon as it starts
 * without `wait`; it then holds the speaker until it ends, so another call meanwhile is `BUSY` (a reading the engine no
 * longer reports as speaking does not hold it). Cancelling the call or its time limit stops the reading at once. Pure
 * Kotlin; [clock] is the elapsed real time.
 * zh-CN: 路线图 P7.6.3 (D96): 为 `speech_speak` 朗读, 每个进程同一时刻只有一个朗读. 朗读方式中有 3-Macaw Speech 的语音时先请它自己的引擎, 再请系统
 * 默认引擎 (不再请 3-Macaw Speech 的引擎), 都不能朗读时以 `NOT_READY` 返回每个引擎的问题. 朗读结束时返回, 不 `wait` 时一开始朗读就返回; 此后朗读
 * 持有 speaker 直到结束, 其间的其他调用为 `BUSY` (引擎已不再报告正在朗读的朗读不再持有). 取消调用或工具时限到达时立即停止朗读. 纯 Kotlin;
 * [clock] 为开机以来的实际时间.
 */
internal class Speaker(private val clock: () -> Long) {
    private val lock = ReentrantLock()
    private var current: Reading? = null

    fun speak(outputs: SpeechOutputs, request: SpeakRequest, plan: SpeakPlan, deadline: Long, cancel: ToolCancellation): SpeakReport {
        val reading = Reading(request.wait, cancel)
        lock.withLock {
            current?.let { previous ->
                if (previous.holds()) throw SpeakRules.busy()
                release(previous)
            }
            current = reading
        }
        try {
            val problems = ArrayList<Pair<String, String>>()
            var ownProblem: String? = null
            if (plan.voice == null) problems += SpeakRules.OWN_ENGINE to "no installed voice speaks the language"
            for (wanted in if (plan.voice != null) listOf(outputs.ownPackage, null) else listOf(null)) {
                if (cancel.cancelled) throw SpeakRules.cancelled()
                val remaining = deadline - clock()
                if (remaining <= 0) throw SpeakRules.expired()
                val output = when (val opened = outputs.open(wanted, minOf(SpeakRules.OPEN_TIMEOUT_MILLIS, remaining))) {
                    is SpeechOutputs.Opened.Ready -> opened.output
                    is SpeechOutputs.Opened.Refused -> {
                        problems += SpeakRules.engineLabel(wanted != null, null) to opened.reason
                        if (wanted != null) ownProblem = opened.reason
                        continue
                    }
                }
                val own = output.enginePackage == outputs.ownPackage
                val problem = when {
                    own && wanted == null && plan.voice != null -> "it is ${SpeakRules.OWN_ENGINE}"
                    own && plan.named -> if (output.voice(plan.voice!!.name)) null else "it has no voice ${plan.voice.name}"
                    else -> output.language(plan.language).takeIf { it < TtsVoiceTable.LANG_AVAILABLE }?.let(SpeakRules::languageProblem)
                }
                if (problem != null) {
                    problems += SpeakRules.engineLabel(wanted != null, output.enginePackage) to problem
                    if (wanted != null) ownProblem = problem
                    output.shutdown()
                    continue
                }
                output.rate(request.speed)
                if (!reading.start(output, request.text)) {
                    problems += SpeakRules.engineLabel(wanted != null, output.enginePackage) to "it refused the text"
                    if (wanted != null) ownProblem = "it refused the text"
                    continue
                }
                val (completed, durationMillis) = reading.await(deadline)
                val warnings = plan.warnings + listOfNotNull(ownProblem?.let { SpeakRules.fallbackWarning(it, plan.named) })
                return SpeakReport(output.enginePackage, plan.language, plan.voice?.name?.takeIf { own }, completed, durationMillis, warnings)
            }
            throw PluginToolException(PluginToolErrorCodes.NOT_READY, SpeakRules.unavailable(plan.language, problems))
        } finally {
            if (!reading.detached) release(reading)
        }
    }

    private fun release(reading: Reading) {
        reading.close()
        lock.withLock { if (current === reading) current = null }
    }

    /** One reading: driven by the engine's callbacks, awaited by the tool's worker thread. zh-CN: 一次朗读. */
    private inner class Reading(private val wait: Boolean, private val cancel: ToolCancellation) : SpeakListener {
        private val lock = ReentrantLock()
        private val changed = lock.newCondition()
        private var output: SpeechOutput? = null
        private var queuedAt = -1L
        private var startedAt = -1L
        private var endedAt = -1L
        private var end: End? = null
        private var closed = false

        /** The call returned while this reading goes on: it releases the speaker when it ends. zh-CN: 调用已返回而朗读继续. */
        @Volatile
        var detached = false
            private set

        init {
            cancel.onCancel { locked { changed.signalAll() } }
        }

        fun start(output: SpeechOutput, text: String): Boolean {
            locked {
                this.output = output
                queuedAt = clock()
            }
            if (output.speak(text, this)) return true
            locked { this.output = null }
            output.shutdown()
            return false
        }

        /** Whether this reading still holds the speaker. zh-CN: 此朗读是否仍持有 speaker. */
        fun holds(): Boolean {
            val speaking = locked {
                if (end != null) return false
                val current = output ?: return true
                if (clock() - queuedAt < SpeakRules.GRACE_MILLIS) return true
                current
            }
            return speaking.isSpeaking()
        }

        /** Completed and the reading time; the end of a waiting call, or the start without `wait`. zh-CN: 是否读完与朗读时长. */
        fun await(deadline: Long): Pair<Boolean, Long?> {
            var stop: Stop? = null
            var stoppedAt = 0L
            var silentSince = -1L
            var polledAt = -1L
            while (true) {
                var request: Stop? = null
                var poll: SpeechOutput? = null
                locked {
                    end?.let { return finish(it, stop) }
                    if (!wait && startedAt >= 0) {
                        detached = true
                        return false to null
                    }
                    val now = clock()
                    when {
                        stop != null -> if (now - stoppedAt >= SpeakRules.END_WAIT_MILLIS) return finish(null, stop)
                        cancel.cancelled -> request = Stop.CANCEL
                        now >= deadline -> request = Stop.EXPIRE
                        now - queuedAt >= SpeakRules.GRACE_MILLIS && now - polledAt >= POLL_MILLIS -> {
                            polledAt = now
                            poll = output
                        }
                    }
                }
                request?.let {
                    stop = it
                    stoppedAt = clock()
                    locked { output }?.stop()
                }
                poll?.let { output ->
                    if (output.isSpeaking()) {
                        silentSince = -1
                    } else if (silentSince < 0) {
                        silentSince = clock()
                    } else if (clock() - silentSince >= SpeakRules.GRACE_MILLIS) {
                        locked { end }?.let { return finish(it, null) }
                        throw SpeakRules.silent(output.enginePackage)
                    }
                }
                locked { if (end == null) changed.await(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS) }
            }
        }

        private fun finish(end: End?, stop: Stop?): Pair<Boolean, Long?> {
            when (stop) {
                Stop.CANCEL -> throw SpeakRules.cancelled()
                Stop.EXPIRE -> throw SpeakRules.expired()
                null -> Unit
            }
            val (startedAt, endedAt) = locked { startedAt to endedAt }
            val duration = if (startedAt >= 0) endedAt - startedAt else 0L
            return when (end) {
                is End.Failed -> throw SpeakRules.engineError(end.code, locked { output }?.enginePackage.orEmpty())
                End.Done -> true to duration
                End.Stopped, null -> false to duration
            }
        }

        override fun started() = locked {
            if (startedAt < 0 && end == null) {
                startedAt = clock()
                changed.signalAll()
            }
        }

        override fun done() = ended(End.Done)

        override fun failed(code: Int) = ended(End.Failed(code))

        override fun stopped() = ended(End.Stopped)

        private fun ended(value: End) {
            val orphaned = locked {
                if (end != null) return
                end = value
                endedAt = clock()
                changed.signalAll()
                detached
            }
            if (orphaned) release(this)
        }

        fun close() {
            val open = locked {
                if (closed) return
                closed = true
                output
            }
            open?.shutdown()
        }

        private inline fun <T> locked(block: () -> T): T = lock.withLock(block)
    }

    private sealed interface End {
        data object Done : End
        data object Stopped : End
        data class Failed(val code: Int) : End
    }

    private enum class Stop { CANCEL, EXPIRE }

    private companion object {
        const val WAIT_SLICE_MILLIS = 100L
        const val POLL_MILLIS = 1000L
    }
}
