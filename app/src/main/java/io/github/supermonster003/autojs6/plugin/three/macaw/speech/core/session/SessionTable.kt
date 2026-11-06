package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits

/**
 * Admission of sessions (roadmap D16, `SpeechLimits`): the plugin owns one microphone, so one microphone session
 * (dictation or wake) runs at a time in the whole plugin, whoever opened it; external PCM sessions are counted per
 * caller uid (the host Binder token), at most `MAX_PCM_SESSIONS_PER_TOKEN`. A refused reservation is `SESSION_BUSY`.
 * Roadmap D57: a dictation of the plugin's own entries (the digital assistant, the voice input sheet, the recognition
 * service, the tryout screen) may take the microphone over from a wake session that declared itself [Yielding]: the
 * wake session stays open but silent, and resumes when the takeover is released. A dictation is never taken over, and
 * only one takeover runs at a time. Roadmap D72 / D76 / D190: background holders sit beside them, the resident wake word
 * and the script wake sessions opened with `background` (at most [MAX_BACKGROUND_WAKE_SESSIONS] of those), in one
 * queue in the order they were reserved; they all record together while the microphone is free (one shared recording,
 * D228), every microphone reservation pauses all of them first instead of failing, and once the microphone is free again
 * they resume in that order. A hit of a background holder that starts a `thenListen` round makes it the microphone
 * holder until the round ends (D229): the other background holders pause, and a new reservation is refused as during
 * any dictation. Pauses and resumptions carry the [reason] of the reservation that caused them.
 * Pure Kotlin, thread safe.
 * zh-CN: 会话准入 (路线图 D16, `SpeechLimits`): 插件只有一个麦克风, 因此整个插件同一时刻只运行一个麦克风会话 (听写或唤醒), 无论由谁打开; 外部 PCM
 * 会话按调用方 uid (宿主 Binder token) 计数, 至多 `MAX_PCM_SESSIONS_PER_TOKEN`. 被拒绝的预留为 `SESSION_BUSY`. 路线图 D57: 插件自己的入口 (数字助理,
 * 语音输入弹层, 识别服务, 试听试说页) 的听写可以从声明为 [Yielding] 的唤醒会话手中临时接管麦克风: 唤醒会话保持打开但静默, 接管释放后恢复. 听写会话从不
 * 被接管, 同一时刻至多一个接管. 路线图 D72 / D76 / D190: 与它们并列的是后台持有者: 常驻唤醒与以 `background` 打开的脚本唤醒会话 (后者至多
 * [MAX_BACKGROUND_WAKE_SESSIONS] 个), 按预留顺序排成一个队列; 麦克风空闲时它们一起录音 (共用一路录音, D228), 任何麦克风预留都先让它们全部暂停, 而不是
 * 失败, 麦克风再次空闲后按该顺序恢复. 后台持有者的命中开始 `thenListen` 轮次时, 它在轮次结束前成为麦克风持有者 (D229): 其他后台持有者暂停, 新的预留
 * 与任何听写期间一样被拒绝. 暂停与恢复带有引起它们的预留的 [reason].
 * 纯 Kotlin, 线程安全.
 */
class SessionTable {

    /**
     * A microphone session that lends the microphone to a takeover (a wake session, roadmap D57) or, as a background
     * holder, to any microphone session (roadmap D76, D190).
     * zh-CN: 可以把麦克风让给接管方 (唤醒会话, 路线图 D57) 或作为后台持有者让给任何麦克风会话 (路线图 D76, D190) 的麦克风会话.
     */
    interface Yielding {
        /**
         * Silences the session for the session that [reason] names and stops its recording; false when it cannot (a
         * `thenListen` round, which is a dictation, is running). Called with the table locked: it must not reserve or release.
         * zh-CN: 为 [reason] 所指的会话静默本会话并停止其录音; 无法让出时 (正在进行 `thenListen` 轮次, 即听写) 返回 false. 调用时表已加锁: 不得预留或释放.
         */
        fun yieldMicrophone(reason: String): Boolean

        /**
         * The session that [reason] names ended: record and detect again. zh-CN: [reason] 所指的会话已结束: 重新录音与检测.
         */
        fun resumeMicrophone(reason: String)
    }

    /** A reserved place; [release] it exactly once. zh-CN: 已预留的位置; 恰好 [release] 一次. */
    class Slot internal constructor(
        val uid: Int,
        val microphone: Boolean,
        val takeover: Boolean = false,
        val background: Boolean = false,
        val reason: String = REASON_LISTEN,
        val resident: Boolean = false,
    ) {
        internal var released = false
        internal var yielding: Yielding? = null

        /** A background holder that another session paused (roadmap D76, D190). zh-CN: 被其他会话暂停的后台持有者 (路线图 D76, D190). */
        internal var silenced = false

        /** A background holder whose `thenListen` round holds the microphone (D229). zh-CN: 其 `thenListen` 轮次占用麦克风的后台持有者 (D229). */
        internal var rounding = false
    }

    private var microphone: Slot? = null
    private var taker: Slot? = null
    private val background = ArrayList<Slot>()
    private val external = HashMap<Int, Int>()

    /**
     * Reserves a place. [takeover] (microphone only) lets the reservation silence a [Yielding] holder instead of failing.
     * [background] (microphone only) reserves a background holder (roadmap D76, D190): it never makes another reservation
     * fail, joins the end of the queue and starts paused while the microphone is in use; [resident] marks the resident
     * wake word, which the limit of script background sessions leaves out. [reason] names the session for the pauses of
     * the background holders (`listen`, `wake` or `plugin`).
     * zh-CN: 预留位置. [takeover] (仅麦克风) 允许本次预留让可让出的持有者静默, 而不是失败. [background] (仅麦克风) 预留后台持有者 (路线图 D76,
     * D190): 从不让其他预留失败, 排到队尾, 麦克风正被使用时以暂停状态开始; [resident] 表示常驻唤醒, 不计入脚本后台会话的上限. [reason] 是暂停后台
     * 持有者时给出的会话类别 (`listen`, `wake` 或 `plugin`).
     */
    fun reserve(
        uid: Int,
        microphone: Boolean,
        takeover: Boolean = false,
        background: Boolean = false,
        reason: String = REASON_LISTEN,
        resident: Boolean = false,
    ): Slot {
        val undone = ArrayList<Yielding>(2)
        try {
            return synchronized(this) { admit(uid, microphone, takeover, background, reason, resident, undone) }
        } catch (e: SpeechException) {
            // Background holders paused before one refused record again (outside the lock). zh-CN: 在某个持有者拒绝之前已暂停的后台持有者重新录音 (在锁外).
            undone.forEach { it.resumeMicrophone(reason) }
            throw e
        }
    }

    private fun admit(uid: Int, microphone: Boolean, takeover: Boolean, background: Boolean, reason: String, resident: Boolean, undone: MutableList<Yielding>): Slot {
        if (microphone && background) {
            if (!resident && this.background.count { !it.resident } >= MAX_BACKGROUND_WAKE_SESSIONS) {
                throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "At most $MAX_BACKGROUND_WAKE_SESSIONS background wake sessions may be open at a time")
            }
            return Slot(uid, true, background = true, reason = reason, resident = resident).also {
                it.silenced = this.microphone != null || taker != null
                this.background += it
            }
        }
        if (microphone) {
            val holder = this.microphone
            if (holder == null && taker == null) {
                pauseBackground(reason, undone)
                return Slot(uid, true, reason = reason).also { this.microphone = it }
            }
            if (takeover && taker == null && holder != null && holder.yielding?.yieldMicrophone(reason) == true) {
                return Slot(uid, true, takeover = true, reason = reason).also { taker = it }
            }
            throw busy()
        }
        val count = external[uid] ?: 0
        if (count >= SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN) {
            throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "At most ${SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN} external PCM sessions may be open at a time")
        }
        external[uid] = count + 1
        return Slot(uid, false)
    }

    /**
     * The session of [slot] may be taken over or paused from now on (roadmap D57, D76). A background holder reserved while
     * the microphone was in use pauses here, before it records.
     * zh-CN: 从此刻起 [slot] 的会话可被接管或暂停 (路线图 D57, D76). 在麦克风被使用时预留的后台持有者在此暂停, 尚未开始录音.
     */
    @Synchronized
    fun yieldable(slot: Slot, yielding: Yielding) {
        if (!slot.microphone || slot.takeover || slot.released) return
        slot.yielding = yielding
        if (slot.background && slot.silenced) yielding.yieldMicrophone(currentReason())
    }

    /**
     * Roadmap D76 / D190: a reservation of the microphone pauses every background holder first, in queue order; one that
     * cannot yield (a `thenListen` round about to hold the microphone) refuses the reservation, and the holders paused
     * before it are given back in [undone].
     * zh-CN: 路线图 D76 / D190: 预留麦克风时先按队列顺序暂停全部后台持有者; 无法让出的 (即将占用麦克风的 `thenListen` 轮次) 使预留被拒绝, 在它之前已暂停的
     * 持有者经 [undone] 交还.
     */
    private fun pauseBackground(reason: String, undone: MutableList<Yielding>) {
        val paused = ArrayList<Slot>(background.size)
        for (holder in background) {
            if (holder.silenced) continue
            if (holder.yielding?.yieldMicrophone(reason) == false) {
                paused.forEach { it.silenced = false }
                paused.mapNotNullTo(undone) { it.yielding }
                throw busy()
            }
            holder.silenced = true
            paused += holder
        }
    }

    /**
     * Roadmap D229: a hit of the background holder [slot] started a `thenListen` round, which holds the microphone like a
     * dictation until [endRound]; the other background holders pause for it (`listen`). False when the microphone is in
     * use already (the round then cannot record; the caller ends it).
     * zh-CN: 路线图 D229: 后台持有者 [slot] 的命中开始了 `thenListen` 轮次, 该轮次像听写一样占用麦克风直到 [endRound]; 其他后台持有者为它暂停
     * (`listen`). 麦克风已被占用时返回 false (轮次无法录音, 由调用方结束它).
     */
    @Synchronized
    fun startRound(slot: Slot): Boolean {
        if (!slot.background || slot.released) return false
        if (slot.rounding) return true
        if (microphone != null || taker != null || slot.silenced) return false
        for (holder in background) {
            if (holder === slot || holder.silenced) continue
            // A holder that cannot yield keeps recording beside the round; the shared recording feeds both (D228).
            if (holder.yielding?.yieldMicrophone(REASON_LISTEN) != false) holder.silenced = true
        }
        slot.rounding = true
        microphone = slot
        return true
    }

    /**
     * The `thenListen` round of [slot] ended (D229): the microphone is free again and the paused background holders
     * resume in queue order (outside the lock).
     * zh-CN: [slot] 的 `thenListen` 轮次已结束 (D229): 麦克风再次空闲, 暂停的后台持有者按队列顺序恢复 (在锁外进行).
     */
    fun endRound(slot: Slot) {
        val resumed = synchronized(this) {
            if (!slot.rounding) return
            slot.rounding = false
            if (microphone === slot) microphone = null
            resumeBackground()
        }
        resumed.forEach { it.resumeMicrophone(REASON_LISTEN) }
    }

    /**
     * Frees [slot]; releasing a takeover resumes the silenced holder, and the microphone becoming free resumes the
     * background holders in queue order (outside the lock), with the [Slot.reason] of [slot].
     * zh-CN: 释放 [slot]; 释放接管时恢复被静默的持有者, 麦克风变为空闲时按队列顺序恢复后台持有者 (在锁外进行), 原因为 [slot] 的 [Slot.reason].
     */
    fun release(slot: Slot) {
        val resumed: List<Yielding> = synchronized(this) {
            if (slot.released) return
            slot.released = true
            if (slot.microphone) {
                val resumed = ArrayList<Yielding>(2)
                when {
                    slot.background -> {
                        background.remove(slot)
                        slot.rounding = false
                        if (microphone === slot) microphone = null
                    }
                    taker === slot -> {
                        taker = null
                        microphone?.yielding?.let(resumed::add)
                    }
                    microphone === slot -> microphone = null
                }
                resumed += resumeBackground()
                resumed
            } else {
                val left = (external[slot.uid] ?: 1) - 1
                if (left <= 0) external.remove(slot.uid) else external[slot.uid] = left
                emptyList()
            }
        }
        resumed.forEach { it.resumeMicrophone(slot.reason) }
    }

    /**
     * With the microphone free, the paused background holders in queue order (marked as recording). zh-CN: 麦克风空闲时, 按队列顺序列出暂停的后台持有者 (并标为录音中).
     */
    private fun resumeBackground(): List<Yielding> {
        if (microphone != null || taker != null) return emptyList()
        return background.filter { it.silenced }.mapNotNull { holder ->
            holder.silenced = false
            holder.yielding
        }
    }

    /** The category of the session that pauses the background holders now. zh-CN: 当前使后台持有者暂停的会话类别. */
    private fun currentReason(): String = (microphone ?: taker)?.reason ?: REASON_LISTEN

    @get:Synchronized
    val microphoneInUse: Boolean get() = microphone != null || taker != null

    /** Some background holder records right now (roadmap D76, D190). zh-CN: 当前有后台持有者正在录音 (路线图 D76, D190). */
    @get:Synchronized
    val backgroundListening: Boolean get() = background.any { !it.silenced }

    /** The background holders in queue order, as `uid` (tests and logs). zh-CN: 按队列顺序列出的后台持有者 uid (供测试与日志). */
    @get:Synchronized
    val backgroundHolders: List<Int> get() = background.map { it.uid }

    /** A takeover silences a holder right now (roadmap D57). zh-CN: 当前有接管方正在静默某个持有者 (路线图 D57). */
    @get:Synchronized
    val takenOver: Boolean get() = taker != null && microphone != null

    @Synchronized
    fun externalSessions(uid: Int): Int = external[uid] ?: 0

    private fun busy() = SpeechException(SpeechErrorCodes.SESSION_BUSY, "The microphone is in use by another speech session")

    companion object {
        /**
         * Script wake sessions opened with `background` at a time, in the whole plugin (D229); the resident wake word comes
         * on top. Every background session spots on the one ASR thread.
         * zh-CN: 整个插件同时以 `background` 打开的脚本唤醒会话个数 (D229); 常驻唤醒另计. 每个后台会话都在同一个 ASR 线程上检测.
         */
        const val MAX_BACKGROUND_WAKE_SESSIONS = 4

        /** Paused for a dictation: a script's `listen` or a `thenListen` round. zh-CN: 为听写暂停: 脚本的 `listen` 或 `thenListen` 轮次. */
        const val REASON_LISTEN = "listen"

        /** Paused for a script's wake session without `background`. zh-CN: 为脚本未带 `background` 的唤醒会话暂停. */
        const val REASON_WAKE = "wake"

        /**
         * Paused for an entry of the plugin itself: a sheet, the assistant, the recognition service, the tryout screen, the
         * voiceprint recorder. zh-CN: 为插件自己的入口暂停: 弹层, 助理, 识别服务, 试听试说页, 声纹录音.
         */
        const val REASON_PLUGIN = "plugin"
    }
}
