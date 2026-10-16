package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.security.MessageDigest

/**
 * The device test recordings of roadmap P3.5 are exactly the files of `locks/test-audio.lock`: every one pinned to an
 * upstream sherpa-onnx archive member with its license, nothing extra in the asset directory, never in the plugin APK,
 * and the pinyin table of the keyword spotter is generated from the pinned pinyin-data.
 * zh-CN: 路线图 P3.5 的设备测试录音恰好是 `locks/test-audio.lock` 中的文件: 每个都固定到带许可的上游 sherpa-onnx 归档成员, 资产目录中没有多余
 * 文件, 也从不进入插件 APK; 关键词检测器的拼音表由固定的 pinyin-data 生成.
 */
class TestAudioLockTest {

    private val lock = LockFiles.read("locks/test-audio.lock")

    @Test
    fun `every recording matches its lock entry`() {
        val names = lock.keys.filter { it.endsWith(".path") }.map { it.removeSuffix(".path") }.sorted()
        assertEquals(listOf("en", "kws-zh-0", "kws-zh-1", "kws-zh-2", "kws-zh-3", "kws-zh-4", "kws-zh-5", "kws-zh-6", "mixed-zh-en", "zh", "zh-8k"), names)
        names.forEach { name ->
            val path = lock.getValue("$name.path")
            assertEquals("app/src/androidTest/assets/test-audio/$name.wav", path)
            val digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(RepositoryFiles.path(path))).joinToString("") { "%02x".format(it) }
            assertEquals(name, lock.getValue("$name.sha256"), digest)
            assertTrue(lock.getValue("$name.url").startsWith("https://github.com/k2-fsa/sherpa-onnx/releases/download/"))
            assertTrue(lock.getValue("$name.member").endsWith(".wav"))
            assertTrue(lock.getValue("$name.license") in setOf("Apache-2.0", "CC-BY-4.0"))
        }
        val files = Files.list(RepositoryFiles.path("app/src/androidTest/assets/test-audio")).use { stream -> stream.map { it.fileName.toString() }.sorted().toList() }
        assertEquals(names.map { "$it.wav" }.toSet(), files.toSet())
    }

    @Test
    fun `the gradle build verifies the lock`() {
        assertTrue(RepositoryFiles.text("app/build.gradle.kts").contains("readLock(\"locks/test-audio.lock\")"))
    }

    @Test
    fun `the keyword pinyin table is generated from the pinned pinyin data`() {
        val table = RepositoryFiles.text("app/src/main/assets/kws/pinyin.txt")
        assertTrue(table.startsWith("# 3-Macaw Speech keyword pinyin format 1"))
        val lines = table.lines().filter { it.isNotEmpty() && !it.startsWith("#") }
        assertTrue(lines.size > 20_000)
        assertTrue(lines.all { it.length >= 3 && it[1] == '\t' })
        assertTrue(lines.contains("小\txiǎo"))
        assertTrue(RepositoryFiles.text("app/src/main/assets/kws/NOTICE.txt").contains("pinyin-data"))
        val digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(RepositoryFiles.path("third_party/pinyin-data/pinyin.txt"))).joinToString("") { "%02x".format(it) }
        assertEquals("621f8ca9eff8519f47e2b17b564fd318161e13bca07eea8c8e04993cd5d3b52e", digest)
    }
}
