package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import kotlin.math.log10

/**
 * How the resident wake word saves power (roadmap P8.1, D123, D128): [voiceGate] lets only speech reach the keyword
 * spotter ([VoiceGatedKeywords]); [frameMillis] is the step at which the microphone hands its audio to the ASR thread
 * (20 ms in 1.2.0, so 50 hand-overs per second). [OFF] is the 1.2.0 behaviour. Pure Kotlin.
 * zh-CN: 常驻唤醒如何省电 (路线图 P8.1, D123, D128): [voiceGate] 只让语音进入唤醒词检测器 ([VoiceGatedKeywords]); [frameMillis] 是麦克风把音频交给
 * ASR 线程的步长 (1.2.0 为 20 ms, 即每秒 50 次). [OFF] 即 1.2.0 的行为. 纯 Kotlin.
 */
data class WakePower(val voiceGate: Boolean, val frameMillis: Int) {
    init {
        require(frameMillis in MIN_FRAME_MILLIS..MAX_FRAME_MILLIS) { "frameMillis out of range: $frameMillis" }
    }

    /** Samples per microphone read at 16 kHz. zh-CN: 16 kHz 下每次读取麦克风的采样数. */
    val frameSamples: Int get() = AudioInput.samples(frameMillis.toLong()).toInt()

    companion object {
        /** The microphone step of 1.2.0 (roadmap P3.1). zh-CN: 1.2.0 的麦克风步长 (路线图 P3.1). */
        const val DEFAULT_FRAME_MILLIS = 20
        const val MIN_FRAME_MILLIS = 10
        const val MAX_FRAME_MILLIS = 500

        val OFF = WakePower(voiceGate = false, frameMillis = DEFAULT_FRAME_MILLIS)
    }
}

/**
 * A keyword stream behind a voice gate (roadmap P8.1, D123, D128): while the silero VAD hears no speech and no window is
 * [riseDb] louder than the room, the audio only fills a lookback of [lookbackMillis] and the spotter decodes nothing; once
 * either fires, the lookback and then every window reach the spotter, until [hangoverMillis] after the last one. The
 * VAD hears its input [vadGain] times louder; the rise catches the quiet speech the VAD misses. The lookback reaches back
 * into the room before the first word, as the preroll of [StreamingDictation] does, and further, because the VAD fires
 * late on quiet speech and the spotter needs the room before a quiet keyword. Closing feeds the spotter a tail of
 * silence, so a keyword at the end of the speech is decoded, then starts it over, so nothing heard before can complete a
 * keyword in later speech. A hit found while closing waits for the next [poll]. [heard] and [passed] count the samples,
 * for the device log.
 * zh-CN: 置于语音门之后的关键词流 (路线图 P8.1, D123, D128): silero VAD 未听到语音, 且没有窗口比房间响 [riseDb] 时, 音频只填入 [lookbackMillis] 的回溯
 * 缓冲, 检测器不解码; 任一触发后, 回溯缓冲与之后的每个窗口都送入检测器, 直到最后一次触发之后 [hangoverMillis]. VAD 听到的输入放大 [vadGain] 倍; 响度
 * 上升补上 VAD 漏掉的轻声. 回溯缓冲与 [StreamingDictation] 的预滚一样要回到首个词之前的房间声, 而且更长, 因为 VAD 对很轻的语音触发得晚, 检测器也需要
 * 轻声唤醒词之前的房间声. 关闭时给检测器送一段静音, 让语音末尾的唤醒词得以解码, 然后重新开始检测, 之前听到的内容不会与之后的语音拼成唤醒词. 关闭时
 * 发现的命中留给下一次 [poll]. [heard] 与 [passed] 计采样数, 供设备日志使用.
 */
class VoiceGatedKeywords(
    private val inner: KeywordStream,
    private val vad: VoiceDetector,
    lookbackMillis: Long = LOOKBACK_MILLIS,
    hangoverMillis: Long = HANGOVER_MILLIS,
    private val vadGain: Float = VAD_GAIN,
    private val riseDb: Float = RISE_DB,
) : KeywordStream {

    private val windows = WindowSplitter(vad.windowSize)
    private val lookback = ArrayDeque<FloatArray>()
    private val lookbackWindows = maxOf(1, ((AudioInput.samples(lookbackMillis) + vad.windowSize - 1) / vad.windowSize).toInt())
    private val hangover = AudioInput.samples(hangoverMillis)
    private var quiet = 0L
    private var fed = false
    private var pending: Int? = null
    // The levels (dBFS) of the last FLOOR_MILLIS of windows, a ring; the room's floor is their minimum.
    private val levels = FloatArray(maxOf(1, (AudioInput.samples(FLOOR_MILLIS) / vad.windowSize).toInt()))
    private val warmup = AudioInput.samples(WARMUP_MILLIS)
    private var measured = 0L
    private var level = 0

    /** True while audio reaches the spotter. zh-CN: 音频正送入检测器. */
    var open = false
        private set

    /** Samples heard since the start. zh-CN: 开始以来听到的采样数. */
    var heard = 0L
        private set

    /** Samples the spotter decoded, the lookbacks and tails included. zh-CN: 检测器解码过的采样数, 含回溯缓冲与尾部静音. */
    var passed = 0L
        private set

    /** How often the gate opened. zh-CN: 门打开的次数. */
    var openings = 0
        private set

    override fun accept(samples: FloatArray) = windows.split(samples, ::window)

    override fun poll(): Int? {
        pending?.let {
            pending = null
            return it
        }
        if (!fed) return null
        fed = false
        return inner.poll()
    }

    override fun restart() {
        inner.restart()
        vad.reset()
        // An incomplete window from before the takeover or the round is dropped.
        windows.remainder()
        lookback.clear()
        open = false
        quiet = 0
        fed = false
        pending = null
        measured = 0
        level = 0
    }

    override fun close() {
        runCatching { inner.close() }
        vad.close()
    }

    private fun window(window: FloatArray) {
        heard += window.size
        vad.accept(if (vadGain == 1f) window else FloatArray(window.size) { (window[it] * vadGain).coerceIn(-1f, 1f) })
        while (vad.poll() != null) Unit
        // Both run on every window: the floor follows the room even while the VAD hears speech.
        val rise = rises(window)
        val speech = vad.isSpeech() || rise
        if (open) {
            feed(window)
            quiet = if (speech) 0 else quiet + window.size
            if (quiet >= hangover) shut()
            return
        }
        lookback.addLast(window)
        while (lookback.size > lookbackWindows) lookback.removeFirst()
        if (!speech) return
        open = true
        openings++
        quiet = 0
        while (lookback.isNotEmpty()) feed(lookback.removeFirst())
    }

    /**
     * Whether [window] is at least [riseDb] louder than the room, whose floor is the quietest window of the last
     * [FLOOR_MILLIS]: a word has pauses and the room before it, a louder room lifts the floor within that time. The
     * first [WARMUP_MILLIS] after a start are not measured (the microphone ramps up from silence).
     * zh-CN: [window] 是否比房间至少响 [riseDb]; 房间底噪取最近 [FLOOR_MILLIS] 内最安静的窗口: 说话有停顿, 之前也有房间声, 房间变吵时底噪在这段时间内
     * 跟上. 开始后的前 [WARMUP_MILLIS] 不计 (麦克风从静音逐渐起来).
     */
    private fun rises(window: FloatArray): Boolean {
        if (riseDb <= 0f) return false
        measured += window.size
        if (measured <= warmup) return false
        var sum = 0.0
        for (sample in window) sum += sample * sample
        // Digital silence (a silenced microphone) says nothing about the room.
        if (sum / window.size < SILENT_POWER) return false
        val db = (10 * log10(sum / window.size)).toFloat()
        val filled = minOf(level, levels.size)
        var floor = Float.MAX_VALUE
        for (index in 0 until filled) floor = minOf(floor, levels[index])
        levels[level % levels.size] = db
        level++
        return filled > 0 && db - floor >= riseDb
    }

    private fun feed(samples: FloatArray) {
        inner.accept(samples)
        passed += samples.size
        fed = true
    }

    private fun shut() {
        feed(FloatArray(TAIL_SAMPLES))
        val hit = inner.poll()
        if (pending == null) pending = hit
        inner.restart()
        open = false
        quiet = 0
        fed = false
    }

    companion object {
        /**
         * Audio before the gate opened that reaches the spotter. A wake word heard from afar is quiet and the VAD fires
         * late on it: on a Sony G8441 the 2.3 s keyword played through its own speaker arrived at about -50 dBFS, and the
         * VAD fired 1.1 to 1.4 s after the playback started (2.2 s without [VAD_GAIN]); a 1 s lookback missed the keyword
         * every time, 2 s caught it. The spotter, started over, also wants the room before a quiet keyword (roadmap
         * P8.1.1).
         * zh-CN: 开门之前送入检测器的音频. 远处说出的唤醒词很轻, VAD 对它触发得晚: Sony G8441 自身扬声器播放的 2.3 秒关键词录到约 -50 dBFS, VAD 在播放
         * 开始 1.1 至 1.4 秒后才触发 (不加 [VAD_GAIN] 时 2.2 秒); 回溯 1 秒每次都漏掉, 2 秒能命中. 重新开始的检测器也需要轻声唤醒词之前的房间声
         * (路线图 P8.1.1).
         */
        const val LOOKBACK_MILLIS = 3000L

        /**
         * How long the gate stays open after the VAD last reported speech (its own 0.25 s minimum silence comes first) or
         * a window last rose over the room: pauses inside a wake word ("Hey, Macaw") keep it open.
         * zh-CN: VAD 最后一次报告语音 (之前还有 VAD 自身 0.25 秒的最短静音) 或最后一个窗口高出房间之后门保持打开的时长: 唤醒词中间的停顿 ("Hey,
         * Macaw") 不会关门.
         */
        const val HANGOVER_MILLIS = 1000L

        /**
         * Gain of the VAD's input only (about +18 dB, clipped; the spotter hears the audio as it is): the silero VAD hears
         * little speech in quiet input, and 30 s of the room alone never opened the gate with it. A wake word at a quarter
         * of the level (about -62 dBFS on the G8441, 2 to 3 dB over the room) it still hears late or not at all: the live
         * session heard it 2 and 0 times in 10 with the VAD alone (roadmap P8.1.1), so [RISE_DB] opens the gate as well.
         * zh-CN: 只作用于 VAD 输入的增益 (约 +18 dB, 截幅; 检测器听到的仍是原始音频): silero VAD 在很轻的输入中听不出语音; 加增益后 30 秒纯房间声
         * 一次也没开门. 四分之一音量的唤醒词 (G8441 上约 -62 dBFS, 比房间高 2 至 3 dB) 它仍然听得晚或听不到: 只用 VAD 时实时会话 10 次只听到 2 次与
         * 0 次 (路线图 P8.1.1), 所以 [RISE_DB] 也能开门.
         */
        const val VAD_GAIN = 8f

        /**
         * Rise of a window over the room's floor that opens the gate as speech does. On the G8441 (room about -64 dBFS),
         * 6 dB opened the gate 8 times in 30 s of the room alone (78 % of it decoded), 9 dB twice (30 %), 12 dB once but
         * missed the quiet keywords; with 9 dB the live session heard the quarter-level keyword 9 times in 10, the 1.2.0
         * session 10 (roadmap P8.1.1).
         * zh-CN: 窗口高出房间底噪多少即与语音一样开门. G8441 上 (房间约 -64 dBFS), 6 dB 时 30 秒纯房间声开门 8 次 (解码其中 78%), 9 dB 时 2 次
         * (30%), 12 dB 时 1 次但漏掉轻声关键词; 9 dB 时实时会话 10 次听到四分之一音量的关键词 9 次, 1.2.0 的会话 10 次 (路线图 P8.1.1).
         */
        const val RISE_DB = 9f

        /** The room's floor is the quietest window of this long. zh-CN: 房间底噪取这段时间内最安静的窗口. */
        const val FLOOR_MILLIS = 4000L

        /** Not measured after a start. zh-CN: 开始后不计的时长. */
        const val WARMUP_MILLIS = 250L

        /** -100 dBFS: quieter windows are digital silence. zh-CN: -100 dBFS: 更安静的窗口视为数字静音. */
        private const val SILENT_POWER = 1e-10

        /** Silence that flushes the spotter's last chunk when the gate closes. zh-CN: 关门时冲刷检测器最后一个块的静音. */
        private val TAIL_SAMPLES = AudioInput.samples(400).toInt()
    }
}
