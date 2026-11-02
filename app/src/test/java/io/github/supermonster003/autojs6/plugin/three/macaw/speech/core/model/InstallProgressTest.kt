package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two stages of a model installation on the screens (roadmap D181). zh-CN: 界面上模型安装的两个阶段 (路线图 D181). */
class InstallProgressTest {

    @Test
    fun `the download counts against the size of the model`() {
        val progress = InstallProgress.of(done = 40, total = 180, unpackBytes = 100)
        assertEquals(InstallProgress(false, 40, 80), progress)
        assertEquals(50, progress.percent)
    }

    @Test
    fun `the unpacking starts over from zero when the download ends`() {
        val progress = InstallProgress.of(done = 80, total = 180, unpackBytes = 100)
        assertEquals(InstallProgress(true, 0, 100), progress)
        assertEquals(0, progress.percent)
    }

    @Test
    fun `the unpacking ends at its own size`() {
        val progress = InstallProgress.of(done = 180, total = 180, unpackBytes = 100)
        assertTrue(progress.unpacking)
        assertEquals(100, progress.percent)
    }

    @Test
    fun `without unpacking bytes the operation is one stage`() {
        val progress = InstallProgress.of(done = 70, total = 70, unpackBytes = 0)
        assertEquals(InstallProgress(false, 70, 70), progress)
    }

    @Test
    fun `an unknown size has no fraction`() {
        val progress = InstallProgress.of(done = 5, total = 0, unpackBytes = 0)
        assertFalse(progress.unpacking)
        assertNull(progress.fraction)
        assertEquals(0, progress.percent)
    }
}
