package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits

/**
 * Admission of sessions (roadmap D16, `SpeechLimits`): the plugin owns one microphone, so one microphone session
 * (dictation or wake) runs at a time in the whole plugin, whoever opened it; external PCM sessions are counted per
 * caller uid (the host Binder token), at most `MAX_PCM_SESSIONS_PER_TOKEN`. A refused reservation is `SESSION_BUSY`.
 * Pure Kotlin, thread safe.
 * zh-CN: 会话准入 (路线图 D16, `SpeechLimits`): 插件只有一个麦克风, 因此整个插件同一时刻只运行一个麦克风会话 (听写或唤醒), 无论由谁打开; 外部 PCM
 * 会话按调用方 uid (宿主 Binder token) 计数, 至多 `MAX_PCM_SESSIONS_PER_TOKEN`. 被拒绝的预留为 `SESSION_BUSY`. 纯 Kotlin, 线程安全.
 */
class SessionTable {

    /** A reserved place; [release] it exactly once. zh-CN: 已预留的位置; 恰好 [release] 一次. */
    class Slot internal constructor(val uid: Int, val microphone: Boolean) {
        internal var released = false
    }

    private var microphone: Slot? = null
    private val external = HashMap<Int, Int>()

    @Synchronized
    fun reserve(uid: Int, microphone: Boolean): Slot {
        if (microphone) {
            if (this.microphone != null) throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "The microphone is in use by another speech session")
            return Slot(uid, true).also { this.microphone = it }
        }
        val count = external[uid] ?: 0
        if (count >= SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN) {
            throw SpeechException(SpeechErrorCodes.SESSION_BUSY, "At most ${SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN} external PCM sessions may be open at a time")
        }
        external[uid] = count + 1
        return Slot(uid, false)
    }

    @Synchronized
    fun release(slot: Slot) {
        if (slot.released) return
        slot.released = true
        if (slot.microphone) {
            if (microphone === slot) microphone = null
        } else {
            val left = (external[slot.uid] ?: 1) - 1
            if (left <= 0) external.remove(slot.uid) else external[slot.uid] = left
        }
    }

    @get:Synchronized
    val microphoneInUse: Boolean get() = microphone != null

    @Synchronized
    fun externalSessions(uid: Int): Int = external[uid] ?: 0
}
