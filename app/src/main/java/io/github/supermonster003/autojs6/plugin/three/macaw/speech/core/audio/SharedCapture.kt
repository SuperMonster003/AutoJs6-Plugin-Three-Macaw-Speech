package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException

/**
 * One recording shared by the background holders of the session table (roadmap D190, D228): the resident wake word and
 * the script wake sessions opened with `background` record together while the microphone is free, but Android before 10
 * lets one `AudioRecord` capture at a time, and later versions do not promise two captures of one app the same audio.
 * [member] gives each holder an [AudioCapture] of its own: the first one that starts opens the recording through [open],
 * every frame reaches every started member (each its own copy), and the last one that stops ends it. The read step is
 * the shortest step of the members ([Steppable]); a member that joins with a shorter step shortens it. A read failure
 * reaches every member and retires the recording, so the next start opens a new one. Thread safe; the capture thread
 * never waits on the lock.
 * zh-CN: 会话表的后台持有者共用的一路录音 (路线图 D190, D228): 常驻唤醒与以 `background` 打开的脚本唤醒会话在麦克风空闲时一起录音, 但 Android 10
 * 之前同一时刻只允许一个 `AudioRecord` 采集, 之后的版本也不保证同一应用的两路采集得到相同音频. [member] 给每个持有者一个自己的 [AudioCapture]: 第一个
 * 开始的成员经 [open] 打开录音, 每一帧交给每个已开始的成员 (各自一份副本), 最后一个停止的成员结束录音. 读取步长取成员中最短的步长 ([Steppable]);
 * 以更短步长加入的成员会缩短它. 读取失败通知每个成员并作废该录音, 下一次开始时打开新的录音. 线程安全; 采集线程从不等待锁.
 */
class SharedCapture(private val open: (MicrophoneSource.Listener, Int) -> AudioCapture) {

    /** A recording whose read step can change while it records. zh-CN: 录音期间可调整读取步长的录音. */
    interface Steppable {
        fun step(frameSamples: Int)
    }

    private inner class Recording : MicrophoneSource.Listener {
        @Volatile
        var members: List<Member> = emptyList()
        var capture: AudioCapture? = null
        var step = 0

        @Volatile
        var retired = false

        override fun onAudio(samples: FloatArray) {
            val current = members
            if (current.size == 1) current[0].listener.onAudio(samples) else current.forEach { it.listener.onAudio(samples.copyOf()) }
        }

        override fun onSilenced(silenced: Boolean) = members.forEach { it.listener.onSilenced(silenced) }

        override fun onFailure(error: SpeechException) {
            synchronized(lock) {
                retired = true
                if (current === this) current = null
            }
            members.forEach { it.listener.onFailure(error) }
        }
    }

    private inner class Member(val listener: MicrophoneSource.Listener, val frameSamples: Int) : AudioCapture {
        var recording: Recording? = null

        override fun start() = join(this)

        override fun stop() = leave(this)
    }

    private val lock = Any()
    private var current: Recording? = null

    /** A capture of [listener] in the shared recording. zh-CN: [listener] 在共用录音中的一路采集. */
    fun member(listener: MicrophoneSource.Listener, frameSamples: Int = MicrophoneSource.FRAME_SAMPLES): AudioCapture = Member(listener, frameSamples)

    /** Members that record right now (tests). zh-CN: 当前正在录音的成员数 (供测试). */
    val members: Int get() = synchronized(lock) { current?.members?.size ?: 0 }

    private fun join(member: Member) {
        synchronized(lock) {
            check(member.recording == null) { "A member records once" }
            val recording = current?.takeUnless { it.retired }
            if (recording != null) {
                recording.members = recording.members + member
                member.recording = recording
                if (member.frameSamples < recording.step) {
                    recording.step = member.frameSamples
                    (recording.capture as? Steppable)?.step(member.frameSamples)
                }
                return
            }
            val fresh = Recording()
            fresh.members = listOf(member)
            fresh.step = member.frameSamples
            val capture = open(fresh, member.frameSamples)
            // Opening failures are thrown to the member that started, as by a capture of its own.
            capture.start()
            fresh.capture = capture
            member.recording = fresh
            current = fresh
        }
    }

    private fun leave(member: Member) {
        val stopping = synchronized(lock) {
            val recording = member.recording ?: return
            member.recording = null
            recording.members = recording.members - member
            if (recording.members.isNotEmpty()) {
                val step = recording.members.minOf { it.frameSamples }
                if (step != recording.step) {
                    recording.step = step
                    (recording.capture as? Steppable)?.step(step)
                }
                return
            }
            if (current === recording) current = null
            recording.capture
        }
        // Outside the lock: stopping joins the capture thread. zh-CN: 在锁外进行: 停止时要等待采集线程.
        stopping?.stop()
    }
}
