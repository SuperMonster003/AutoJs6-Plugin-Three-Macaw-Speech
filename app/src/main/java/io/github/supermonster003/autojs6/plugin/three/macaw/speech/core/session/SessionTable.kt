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
 * only one takeover runs at a time. Pure Kotlin, thread safe.
 * zh-CN: 会话准入 (路线图 D16, `SpeechLimits`): 插件只有一个麦克风, 因此整个插件同一时刻只运行一个麦克风会话 (听写或唤醒), 无论由谁打开; 外部 PCM
 * 会话按调用方 uid (宿主 Binder token) 计数, 至多 `MAX_PCM_SESSIONS_PER_TOKEN`. 被拒绝的预留为 `SESSION_BUSY`. 路线图 D57: 插件自己的入口 (数字助理,
 * 语音输入弹层, 识别服务, 试听试说页) 的听写可以从声明为 [Yielding] 的唤醒会话手中临时接管麦克风: 唤醒会话保持打开但静默, 接管释放后恢复. 听写会话从不
 * 被接管, 同一时刻至多一个接管. 纯 Kotlin, 线程安全.
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
    class Slot internal constructor(val uid: Int, val microphone: Boolean, val takeover: Boolean = false) {
        internal var released = false
        internal var yielding: Yielding? = null
    }

    private var microphone: Slot? = null
    private var taker: Slot? = null
    private val external = HashMap<Int, Int>()

    /**
     * Reserves a place. [takeover] (microphone only) lets the reservation silence a [Yielding] holder instead of failing.
     * zh-CN: 预留位置. [takeover] (仅麦克风) 允许本次预留让可让出的持有者静默, 而不是失败.
     */
    @Synchronized
    fun reserve(uid: Int, microphone: Boolean, takeover: Boolean = false): Slot {
        if (microphone) {
            val holder = this.microphone
            if (holder == null && taker == null) return Slot(uid, true).also { this.microphone = it }
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
        if (slot.microphone && !slot.takeover && !slot.released) slot.yielding = yielding
    }

    /**
     * Frees [slot]; releasing a takeover resumes the silenced holder (outside the lock).
     * zh-CN: 释放 [slot]; 释放接管时恢复被静默的持有者 (在锁外进行).
     */
    fun release(slot: Slot) {
        val resumed: Yielding? = synchronized(this) {
            if (slot.released) return
            slot.released = true
            if (slot.microphone) {
                when {
                    taker === slot -> {
                        taker = null
                        microphone?.yielding
                    }
                    microphone === slot -> {
                        microphone = null
                        null
                    }
                    else -> null
                }
            } else {
                val left = (external[slot.uid] ?: 1) - 1
                if (left <= 0) external.remove(slot.uid) else external[slot.uid] = left
                null
            }
        }
        resumed?.resumeMicrophone()
    }

    @get:Synchronized
    val microphoneInUse: Boolean get() = microphone != null || taker != null

    /** A takeover silences a holder right now (roadmap D57). zh-CN: 当前有接管方正在静默某个持有者 (路线图 D57). */
    @get:Synchronized
    val takenOver: Boolean get() = taker != null && microphone != null

    @Synchronized
    fun externalSessions(uid: Int): Int = external[uid] ?: 0
}
