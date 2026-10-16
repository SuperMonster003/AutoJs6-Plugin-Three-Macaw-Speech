package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaRuntime.VadSettings
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaRuntime.VoiceSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The VAD options are validated before any native code runs, and segment times convert at 16 kHz (roadmap D15, P0.2).
 * zh-CN: VAD 选项在调用任何原生代码之前即被校验, 语音段时间按 16 kHz 换算 (路线图 D15, P0.2).
 */
class VadSettingsTest {

    @Test
    fun `the defaults are the upstream silero values`() {
        val settings = VadSettings()
        assertEquals(0.5f, settings.threshold)
        assertEquals(0.25f, settings.minSilenceSeconds)
        assertEquals(0.25f, settings.minSpeechSeconds)
        assertEquals(5f, settings.maxSpeechSeconds)
        assertEquals(16000, SherpaRuntime.SAMPLE_RATE)
        assertEquals(512, SherpaRuntime.VAD_WINDOW_SIZE)
        assertEquals("vad/silero_vad_v5.onnx", SherpaRuntime.VAD_MODEL_ASSET)
    }

    @Test
    fun `boundary values are accepted`() {
        VadSettings(threshold = 0.01f, minSilenceSeconds = 0f, minSpeechSeconds = 0f, maxSpeechSeconds = 1800f)
        VadSettings(threshold = 0.99f, minSilenceSeconds = 1800f, minSpeechSeconds = 1799f, maxSpeechSeconds = 1800f)
    }

    @Test
    fun `out of range values are rejected`() {
        listOf(
            { VadSettings(threshold = 0f) },
            { VadSettings(threshold = 1f) },
            { VadSettings(threshold = Float.NaN) },
            { VadSettings(minSilenceSeconds = -0.1f) },
            { VadSettings(minSilenceSeconds = 1800.5f) },
            { VadSettings(minSpeechSeconds = -1f) },
            { VadSettings(minSpeechSeconds = 5f, maxSpeechSeconds = 5f) },
            { VadSettings(maxSpeechSeconds = 1801f) },
            { VadSettings(maxSpeechSeconds = Float.POSITIVE_INFINITY) },
        ).forEachIndexed { index, create ->
            assertThrows("case $index must be rejected", IllegalArgumentException::class.java) { create() }
        }
    }

    @Test
    fun `segment times convert samples to milliseconds at 16 kHz`() {
        val segment = VoiceSegment(startSample = 42464, sampleCount = 67200)
        assertEquals(2654L, segment.startMillis)
        assertEquals(6854L, segment.endMillis)
        assertEquals(0L, VoiceSegment(0, 0).endMillis)
    }

}
