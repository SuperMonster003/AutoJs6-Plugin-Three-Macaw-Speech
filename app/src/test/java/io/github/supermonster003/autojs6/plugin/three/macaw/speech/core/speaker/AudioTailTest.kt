package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The audio before a wake word hit (roadmap D139): the newest samples in order, across the ring's wrap, and nothing after
 * a clear.
 * zh-CN: 唤醒词命中前的音频 (路线图 D139): 按顺序取最新的采样, 跨越环形缓冲的回绕, 清空后为空.
 */
class AudioTailTest {

    private fun floats(vararg values: Int) = FloatArray(values.size) { values[it].toFloat() }

    @Test
    fun `the newest samples come back oldest first across the wrap`() {
        val tail = AudioTail(5)
        assertEquals(0, tail.last(3).size)
        tail.write(floats(1, 2, 3))
        assertArrayEquals(floats(2, 3), tail.last(2), 0f)
        assertArrayEquals(floats(1, 2, 3), tail.last(10), 0f)
        tail.write(floats(4, 5, 6, 7))
        assertArrayEquals(floats(3, 4, 5, 6, 7), tail.last(5), 0f)
        assertArrayEquals(floats(6, 7), tail.last(2), 0f)
    }

    @Test
    fun `a frame longer than the ring keeps its end, and a clear empties it`() {
        val tail = AudioTail(3)
        tail.write(floats(1, 2, 3, 4, 5, 6, 7))
        assertArrayEquals(floats(5, 6, 7), tail.last(3), 0f)
        tail.clear()
        assertEquals(0, tail.last(3).size)
        tail.write(floats(8))
        assertArrayEquals(floats(8), tail.last(3), 0f)
        assertThrows(IllegalArgumentException::class.java) { AudioTail(0) }
    }
}
