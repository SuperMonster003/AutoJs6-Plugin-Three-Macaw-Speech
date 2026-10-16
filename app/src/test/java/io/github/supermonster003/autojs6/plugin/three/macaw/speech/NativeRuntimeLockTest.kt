package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.ZipFile

/**
 * The sherpa-onnx AAR and the VAD assets are the bytes their locks name, and every record repeats the same provenance
 * (roadmap D13, D14, P0.2, appendix D Q2). The ELF alignment itself is checked by `.python/verify_native_alignment.py`
 * and by the shared native-alignment gate of every assemble.
 * zh-CN: sherpa-onnx AAR 与 VAD 资产就是其锁文件所述的字节, 各处记录的来源一致 (路线图 D13, D14, P0.2, 附录 D Q2). ELF 对齐本身由
 * `.python/verify_native_alignment.py` 与每次 assemble 的共享原生对齐门禁检查.
 */
class NativeRuntimeLockTest {

    private val sherpa: Map<String, String> by lazy { LockFiles.read("locks/sherpa-onnx.lock") }

    private val vad: Map<String, String> by lazy { LockFiles.read("locks/vad-assets.lock") }

    @Test
    fun `the sherpa-onnx lock pins the official v1_13_8 release asset`() {
        assertEquals(
            setOf("format") + listOf("file", "version", "sha256", "url", "commit", "license").map { "sherpa-onnx.$it" },
            sherpa.keys,
        )
        val version = sherpa.getValue("sherpa-onnx.version")
        assertEquals("1.13.8", version)
        assertEquals("sherpa-onnx-$version.aar", sherpa.getValue("sherpa-onnx.file"))
        assertEquals("https://github.com/k2-fsa/sherpa-onnx/releases/download/v$version/sherpa-onnx-$version.aar", sherpa.getValue("sherpa-onnx.url"))
        assertEquals("11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf", sherpa.getValue("sherpa-onnx.commit"))
        assertEquals("Apache-2.0", sherpa.getValue("sherpa-onnx.license"))
        assertEquals(sherpa.getValue("sherpa-onnx.sha256"), LockFiles.sha256("libs/${sherpa.getValue("sherpa-onnx.file")}"))
    }

    @Test
    fun `the AAR carries the Kotlin API and four libraries for each of the four ABIs`() {
        val names = ZipFile(RepositoryFiles.path("libs/${sherpa.getValue("sherpa-onnx.file")}").toFile()).use { zip ->
            zip.entries().asSequence().map { it.name }.filter { !it.endsWith("/") }.toSet()
        }
        assertTrue("classes.jar" in names)
        val libraries = names.filter { it.startsWith("jni/") }.toSet()
        val expected = ABIS.flatMap { abi -> AAR_LIBRARIES.map { "jni/$abi/$it" } }.toSet()
        assertEquals(expected, libraries)
    }

    @Test
    fun `the human-readable records repeat the sherpa-onnx provenance`() {
        val digest = sherpa.getValue("sherpa-onnx.sha256")
        val commit = sherpa.getValue("sherpa-onnx.commit")
        listOf("libs/README.md", "docs/development/native-abi-report.md").forEach { relative ->
            assertTrue("$relative must repeat the AAR digest", RepositoryFiles.text(relative).contains(digest))
        }
        listOf("libs/README.md", "THIRD_PARTY_NOTICES.md").forEach { relative ->
            assertTrue("$relative must name the tag commit", RepositoryFiles.text(relative).contains(commit))
        }
        assertTrue(RepositoryFiles.text("THIRD_PARTY_NOTICES.md").contains("`libs/${sherpa.getValue("sherpa-onnx.file")}`"))
        assertTrue(RepositoryFiles.text("docs/development/native-abi-report.md").contains("every library is 16 KB aligned"))
    }

    @Test
    fun `the VAD assets are the locked silero model and test recording`() {
        val names = vad.keys.filter { it.endsWith(".path") }.map { it.removeSuffix(".path") }.sorted()
        assertEquals(listOf("silero-vad", "test-recording"), names)
        assertEquals(setOf("format") + names.flatMap { n -> listOf("path", "sha256", "url", "upstream", "license").map { "$n.$it" } }, vad.keys)
        names.forEach { name ->
            assertEquals("$name drifted from the lock", vad.getValue("$name.sha256"), LockFiles.sha256(vad.getValue("$name.path")))
            assertEquals("MIT", vad.getValue("$name.license"))
            assertTrue(vad.getValue("$name.url").startsWith("https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/"))
            assertTrue(vad.getValue("$name.upstream").startsWith("https://github.com/snakers4/silero-vad/"))
        }
        assertEquals("app/src/main/assets/vad/silero_vad_v5.onnx", vad.getValue("silero-vad.path"))
        assertTrue("the test recording never ships in the plugin APK", vad.getValue("test-recording.path").startsWith("app/src/androidTest/"))
    }

    private companion object {
        val ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        val AAR_LIBRARIES = listOf("libonnxruntime.so", "libsherpa-onnx-c-api.so", "libsherpa-onnx-cxx-api.so", "libsherpa-onnx-jni.so")
    }

}
