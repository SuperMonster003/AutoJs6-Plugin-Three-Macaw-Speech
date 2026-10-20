package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where the floating microphone rests (roadmap D62): a touch within the slop is a tap, a drag ends on the nearer side
 * edge, and the height is kept as a fraction so a rotation keeps the place.
 * zh-CN: 悬浮麦克风的停靠位置 (路线图 D62): 在阈值内的触摸是点按, 拖动结束后停靠在较近的一侧, 高度按比例保存, 旋转后位置不变.
 */
class FloatingMicPolicyTest {

    @Test
    fun `a touch within the slop is a tap`() {
        assertFalse(FloatingMicPolicy.isDrag(3f, 4f, slop = 5))
        assertFalse(FloatingMicPolicy.isDrag(0f, 0f, slop = 5))
        assertTrue(FloatingMicPolicy.isDrag(3f, 4.1f, slop = 5))
        assertTrue(FloatingMicPolicy.isDrag(-20f, 0f, slop = 16))
    }

    @Test
    fun `a drag ends on the nearer side edge`() {
        assertTrue(FloatingMicPolicy.restsRight(centerX = 540, screenWidth = 1080))
        assertFalse(FloatingMicPolicy.restsRight(centerX = 539, screenWidth = 1080))
        assertEquals(1080 - 144 - 24, FloatingMicPolicy.x(right = true, screenWidth = 1080, size = 144, margin = 24))
        assertEquals(24, FloatingMicPolicy.x(right = false, screenWidth = 1080, size = 144, margin = 24))
        // A screen narrower than the button never places it off screen.
        assertEquals(0, FloatingMicPolicy.x(right = true, screenWidth = 100, size = 144, margin = 24))
        assertEquals(0, FloatingMicPolicy.x(right = false, screenWidth = 100, size = 144, margin = 24))
    }

    @Test
    fun `the height is a fraction that survives a rotation`() {
        assertEquals(0, FloatingMicPolicy.y(0f, screenHeight = 2340, size = 144))
        assertEquals(2340 - 144, FloatingMicPolicy.y(1f, screenHeight = 2340, size = 144))
        assertEquals(878, FloatingMicPolicy.y(FloatingMicPolicy.DEFAULT_Y, screenHeight = 2340, size = 144))
        val fraction = FloatingMicPolicy.fraction(878, screenHeight = 2340, size = 144)
        assertEquals(0.4f, fraction, 0.001f)
        // The same fraction on the rotated screen.
        assertEquals(374, FloatingMicPolicy.y(fraction, screenHeight = 1080, size = 144))
        assertEquals(0f, FloatingMicPolicy.fraction(-10, screenHeight = 2340, size = 144))
        assertEquals(1f, FloatingMicPolicy.fraction(5000, screenHeight = 2340, size = 144))
        assertEquals(0f, FloatingMicPolicy.fraction(0, screenHeight = 100, size = 144))
    }
}
