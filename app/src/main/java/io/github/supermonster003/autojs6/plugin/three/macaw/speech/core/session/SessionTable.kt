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
 * only one takeover runs at a time. Roadmap D72 / D76: the resident wake word of the plugin is a background holder
 * beside them; every microphone reservation silences it first, and it records again once the microphone is free.
 * Pure Kotlin, thread safe.
 * zh-CN: 会话准入 (路线图 D16, `SpeechLimits`): 插件只有一个麦克风, 因此整个插件同一时刻只运行一个麦克风会话 (听写或唤醒), 无论由谁打开; 外部 PCM
 * 会话按调用方 uid (宿主 Binder token) 计数, 至多 `MAX_PCM_SESSIONS_PER_TOKEN`. 被拒绝的预留为 `SESSION_BUSY`. 路线图 D57: 插件自己的入口 (数字助理,
 * 语音输入弹层, 识别服务, 试听试说页) 的听写可以从声明为 [Yielding] 的唤醒会话手中临时接管麦克风: 唤醒会话保持打开但静默, 接管释放后恢复. 听写会话从不
 * 被接管, 同一时刻至多一个接管. 路线图 D72 / D76: 插件的常驻唤醒是与它们并列的后台持有者; 任何麦克风预留都先让它静默, 麦克风空闲后它重新录音.
 * 纯 Kotlin, 线程安全.
 */
class SessionTable {

    /**
     * A microphone session that lends the microphone to a takeover (a wake session, roadmap D57).
     * zh-CN: 可以把麦克风让给接管方的麦克风会话 (唤醒会话, 路线图 D57).
     */
    interface Yielding {
        /**
         * Silences the session for a takeover and stops its recording; false when it cannot (a `thenListen` round, which is
         * a dictation, is running). Called with the table locked: it must not reserve or release.
         * zh-CN: 为接管而静默会话并停止其录音; 无法让出时 (正在进行 `thenListen` 轮次, 即听写) 返回 false. 调用时表已加锁: 不得预留或释放.
         */
        fun yieldMicrophone(): Boolean

        /** The takeover ended: record and detect again. zh-CN: 接管已结束: 重新录音与检测. */
        fun resumeMicrophone()
    }

    /** A reserved place; [release] it exactly once. zh-CN: 已预留的位置; 恰好 [release] 一次. */
    class Slot internal constructor(val uid: Int, val microphone: Boolean, val takeover: Boolean = false, val background: Boolean = false) {
        internal var released = false
        internal var yielding: Yielding? = null

        /** A background holder that another session silenced (roadmap D76). zh-CN: 被其他会话静默的后台持有者 (路线图 D76). */
        internal var silenced = false
    }

    private var microphone: Slot? = null
    private var taker: Slot? = null
    private var background: Slot? = null
    private val external = HashMap<Int, Int>()

    /**
     * Reserves a place. [takeover] (microphone only) lets the reservation silence a [Yielding] holder instead of failing.
     * [background] (microphone only) reserves the background holder of the resident wake word (roadmap D76): there is
     * one at most, it never makes another reservation fail, and it starts silenced while the microphone is in use.
     * zh-CN: 预留位置. [takeover] (仅麦克风) 允许本次预留让可让出的持有者静默, 而不是失败. [background] (仅麦克风) 预留常驻唤醒的后台持有者
     * (路线图 D76): 至多一个, 从不让其他预留失败, 麦克风正被使用时以静默状态开始.
     */
    @Synchronized
    fun reserve(uid: Int, microphone: Boolean, takeover: Boolean = false, background: Boolean = false): Slot {
        if (microphone && background) {
            if (this.background != null) throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "The resident wake word is listening already")
            return Slot(uid, true, background = true).also {
                it.silenced = this.microphone != null || taker != null
                this.background = it
            }
        }
        if (microphone) {
            val holder = this.microphone
            if (holder == null && taker == null) {
                silenceBackground()
                return Slot(uid, true).also { this.microphone = it }
            }
            if (takeover && taker == null && holder != null && holder.yielding?.yieldMicrophone() == true) {
                return Slot(uid, true, takeover = true).also { taker = it }
            }
            throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "The microphone is in use by another speech session")
        }
        val count = external[uid] ?: 0
        if (count >= SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN) {
            throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "At most ${SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN} external PCM sessions may be open at a time")
        }
        external[uid] = count + 1
        return Slot(uid, false)
    }

    /**
     * The session of [slot] may be taken over from now on (roadmap D57). zh-CN: 从此刻起 [slot] 的会话可被接管 (路线图 D57).
     */
    @Synchronized
    fun yieldable(slot: Slot, yielding: Yielding) {
        if (!slot.microphone || slot.takeover || slot.released) return
        slot.yielding = yielding
        // A background holder reserved while the microphone was in use starts silenced (roadmap D76).
        if (slot.background && slot.silenced) yielding.yieldMicrophone()
    }

    /**
     * Roadmap D76: a reservation of the microphone silences the background holder first; one that cannot yield (a
     * `thenListen` round) keeps the microphone.
     * zh-CN: 路线图 D76: 预留麦克风时先让后台持有者静默; 无法让出的 (正在进行 `thenListen` 轮次) 保留麦克风.
     */
    private fun silenceBackground() {
        val resident = background ?: return
        if (resident.silenced) return
        if (resident.yielding?.yieldMicrophone() == false) {
            throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "The microphone is in use by another speech session")
        }
        resident.silenced = true
    }

    /**
     * Frees [slot]; releasing a takeover resumes the silenced holder (outside the lock).
     * zh-CN: 释放 [slot]; 释放接管时恢复被静默的持有者 (在锁外进行).
     */
    fun release(slot: Slot) {
        val resumed: List<Yielding> = synchronized(this) {
            if (slot.released) return
            slot.released = true
            if (slot.microphone) {
                val resumed = ArrayList<Yielding>(2)
                when {
                    background === slot -> background = null
                    taker === slot -> {
                        taker = null
                        microphone?.yielding?.let(resumed::add)
                    }
                    microphone === slot -> microphone = null
                }
                // The microphone is free again: the background holder records (roadmap D76).
                val resident = background
                if (resident != null && resident.silenced && microphone == null && taker == null) {
                    resident.silenced = false
                    resident.yielding?.let(resumed::add)
                }
                resumed
            } else {
                val left = (external[slot.uid] ?: 1) - 1
                if (left <= 0) external.remove(slot.uid) else external[slot.uid] = left
                emptyList()
            }
        }
        resumed.forEach { it.resumeMicrophone() }
    }

    @get:Synchronized
    val microphoneInUse: Boolean get() = microphone != null || taker != null

    /** The background holder records right now (roadmap D76). zh-CN: 后台持有者正在录音 (路线图 D76). */
    @get:Synchronized
    val backgroundListening: Boolean get() = background?.silenced == false

    /** A takeover silences a holder right now (roadmap D57). zh-CN: 当前有接管方正在静默某个持有者 (路线图 D57). */
    @get:Synchronized
    val takenOver: Boolean get() = taker != null && microphone != null

    @Synchronized
    fun externalSessions(uid: Int): Int = external[uid] ?: 0
}
