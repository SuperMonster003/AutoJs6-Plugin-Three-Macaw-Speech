package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

/**
 * Audio recorded before the dictation can take it (the model still loads): kept in order, at most [limitSamples] of the
 * newest, and handed over once with [drain]. The sheets record as soon as they open, so the first word is not lost while
 * a model loads (1.5 - 2 s on the phones measured when it is not cached). Pure JVM.
 * zh-CN: 听写尚不能接收时 (模型仍在加载) 录到的音频: 按顺序保留最新的至多 [limitSamples] 个采样, 由 [drain] 一次交出. 弹层一打开就录音,
 * 因此模型加载期间 (实测手机上未缓存时 1.5 - 2 秒) 不会丢掉第一个字. 纯 JVM.
 */
class EarlyAudio(private val limitSamples: Long = AudioInput.samples(LIMIT_MILLIS)) {

    private val chunks = ArrayDeque<FloatArray>()

    /** Samples kept. zh-CN: 保留的采样数. */
    var samples = 0L
        private set

    fun add(chunk: FloatArray) {
        if (chunk.isEmpty()) return
        chunks.addLast(chunk)
        samples += chunk.size
        while (samples > limitSamples && chunks.size > 1) samples -= chunks.removeFirst().size
    }

    /** Hands every kept chunk to [consumer] in order and forgets them. zh-CN: 按顺序把保留的块交给 [consumer] 并清空. */
    fun drain(consumer: (FloatArray) -> Unit) {
        while (chunks.isNotEmpty()) {
            val chunk = chunks.removeFirst()
            samples -= chunk.size
            consumer(chunk)
        }
        samples = 0
    }

    companion object {
        /** Longer than any model load measured; older audio is dropped. zh-CN: 长于实测的任何模型加载; 更早的音频被丢弃. */
        const val LIMIT_MILLIS = 10_000L
    }
}
