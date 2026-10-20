package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The audio a sheet records while its model loads ([EarlyAudio]): order, the limit on the newest audio, one hand-over.
 * zh-CN: 弹层在模型加载期间录到的音频 ([EarlyAudio]): 顺序, 只保留最新音频的上限, 只交出一次.
 */
class EarlyAudioTest {

    @Test
    fun `chunks come out in order once`() {
        val early = EarlyAudio(limitSamples = 100)
        early.add(floatArrayOf(1f, 2f))
        early.add(FloatArray(0))
        early.add(floatArrayOf(3f))
        assertEquals(3L, early.samples)
        val out = ArrayList<Float>()
        early.drain { out += it.toList() }
        assertEquals(listOf(1f, 2f, 3f), out)
        assertEquals(0L, early.samples)
        early.drain { throw AssertionError("drained twice") }
    }

    @Test
    fun `only the newest audio within the limit is kept`() {
        val early = EarlyAudio(limitSamples = 5)
        repeat(4) { index -> early.add(FloatArray(2) { index.toFloat() }) }
        // 8 samples recorded; whole chunks are dropped from the front until at most 5 remain.
        assertEquals(4L, early.samples)
        val out = ArrayList<Float>()
        early.drain { out += it.toList() }
        assertEquals(listOf(2f, 2f, 3f, 3f), out)
    }

    @Test
    fun `a chunk longer than the limit is kept alone`() {
        val early = EarlyAudio(limitSamples = 3)
        early.add(FloatArray(2))
        early.add(FloatArray(10) { 7f })
        assertEquals(10L, early.samples)
        var chunks = 0
        early.drain { chunks++ }
        assertEquals(1, chunks)
    }

    @Test
    fun `the default keeps ten seconds`() {
        val early = EarlyAudio()
        repeat(600) { early.add(FloatArray(MicrophoneSource.FRAME_SAMPLES)) }
        assertEquals(AudioInput.samples(EarlyAudio.LIMIT_MILLIS), early.samples)
    }
}
