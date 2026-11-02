package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The stable espeak-ng data directory of roadmap D175: the first model's data is linked and stays linked for the others,
 * the link leaves a directory before it is deleted, and a link that leads nowhere is repaired by the next model.
 * zh-CN: 路线图 D175 的稳定 espeak-ng 数据目录: 链接到第一个模型的数据, 其他模型沿用; 目录删除之前链接先移走; 指向已不存在位置的链接由下一个模型修复.
 */
class EspeakHomeTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private class FakeLink(override val path: File) : EspeakHome.Link {
        var pointsTo: File? = null
        var refuses = false
        var moves = 0

        override fun target(): File? = pointsTo

        override fun point(target: File): Boolean {
            if (refuses) return false
            pointsTo = target
            moves++
            return true
        }
    }

    private val link by lazy { FakeLink(File(temporary.root, "home/espeak-ng-data")) }
    private val home by lazy { EspeakHome(link) }

    private fun data(model: String): File = temporary.newFolder(model, "1", "espeak-ng-data").also { File(it, "phontab").writeText("x") }

    private fun version(data: File): File = data.parentFile!!

    @Test
    fun `the first model is linked and the others share it`() {
        val first = data("first")
        val second = data("second")
        assertEquals(link.path, home.dataDir(first))
        assertEquals(first, link.pointsTo)
        assertEquals(link.path, home.dataDir(second))
        assertEquals("the link stays while its data is complete", first, link.pointsTo)
        assertEquals(1, link.moves)
    }

    @Test
    fun `the link leaves a directory before it is deleted`() {
        val first = data("first")
        val second = data("second")
        home.dataDir(first)
        home.leave(version(second)) { listOf(first) }
        assertEquals("another model's deletion does not move it", first, link.pointsTo)
        home.leave(version(first)) { listOf(temporary.newFolder("incomplete"), second) }
        assertEquals("incomplete data is skipped", second, link.pointsTo)
        assertEquals(2, link.moves)
    }

    @Test
    fun `without another model the link stays until the next model repairs it`() {
        val first = data("first")
        home.dataDir(first)
        home.leave(version(first)) { emptyList() }
        assertEquals(first, link.pointsTo)
        version(first).deleteRecursively()
        val second = data("second")
        assertEquals(link.path, home.dataDir(second))
        assertEquals(second, link.pointsTo)
    }

    @Test
    fun `leaving without a link does nothing`() {
        home.leave(version(data("first"))) { error("nothing to move") }
        assertEquals(0, link.moves)
    }

    @Test
    fun `a file system that refuses links gets the model's own directory`() {
        val first = data("first")
        link.refuses = true
        assertEquals(first, home.dataDir(first))
    }
}
