package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Archive boundaries of roadmap P2.3: the canonical mirror tar of Shared-Assets and the GNU tar.bz2 of the upstream
 * fixtures unpack to exactly the catalog files; path traversal, oversized members, truncation, non-canonical headers,
 * trailing data and zip bombs are refused before anything lands outside the staging directory.
 * zh-CN: 路线图 P2.3 的归档边界: Shared-Assets 的规范镜像 tar 与上游夹具的 GNU tar.bz2 恰好解包出目录文件; 路径穿越, 超大成员, 截断,
 * 非规范头部, 尾随数据与 zip 炸弹在任何内容落到暂存目录之外之前即被拒绝.
 */
class ArchiveExtractorTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private val extractor = ArchiveExtractor()

    @Test
    fun `the canonical mirror archive unpacks to the catalog files, including a long USTAR path`() {
        val version = Fixtures.tiny.latestVersion
        val target = temporary.newFolder("mirror")
        extractor.extractMirror(Fixtures.file(version.mirror!!.fileName), version.files, target, CancelToken())
        assertEquals(emptyList<String>(), Verifier.verifyInstalled(target, version.files))
        assertTrue(version.files.any { it.path.length > 100 && it.path.contains(' ') && it.path.contains('!') })
    }

    @Test
    fun `the upstream tar bz2 installs only the mapped members and the raw part moves into place`() {
        val version = Fixtures.tiny.latestVersion
        val target = temporary.newFolder("upstream")
        val files = version.files.associateBy { it.path }
        val archive = version.upstream.filterIsInstance<UpstreamPart.TarBz2>().single()
        extractor.extractUpstream(Fixtures.file("tiny-model.tar.bz2"), archive.members, files, target, CancelToken())
        val raw = version.upstream.filterIsInstance<UpstreamPart.RawFile>().single()
        val downloaded = temporary.newFile("raw.bin").also { Fixtures.file("hifigan_test.onnx").copyTo(it, overwrite = true) }
        extractor.installRawFile(downloaded, raw, files.getValue(raw.path), target)
        assertEquals(emptyList<String>(), Verifier.verifyInstalled(target, version.files))
        val installed = target.walkTopDown().filter { it.isFile }.map { it.relativeTo(target).invariantSeparatorsPath }.toSet()
        assertEquals("extra members such as test.wav are not written", version.files.map { it.path }.toSet(), installed)
    }

    @Test
    fun `hostile upstream archives fail before a wrong byte is installed`() {
        // Written by Python's tarfile + bz2 (.python/generate_model_fixtures.py): a 4096 byte member, a selected member
        // that appears twice, and a selected member that is a symbolic link.
        val file = CatalogFile("model.onnx", 4, "0".repeat(64))
        val members = mapOf("top/model.onnx" to "model.onnx")
        val files = mapOf("model.onnx" to file)
        val oversized = temporary.newFolder("oversized")
        assertThrows(IntegrityException::class.java) {
            extractor.extractUpstream(Fixtures.file("hostile/oversized.tar.bz2"), members, files, oversized, CancelToken())
        }
        assertFalse(File(oversized, "model.onnx").exists())
        val small = CatalogFile("model.onnx", 4, sha("tiny".toByteArray()))
        val twice = members + ("top/other.onnx" to "other.onnx")
        val twiceFiles = mapOf("model.onnx" to small, "other.onnx" to small.copy(path = "other.onnx"))
        assertThrows(ArchiveFormatException::class.java) {
            extractor.extractUpstream(Fixtures.file("hostile/duplicate.tar.bz2"), twice, twiceFiles, temporary.newFolder(), CancelToken())
        }
        assertThrows(ArchiveFormatException::class.java) {
            extractor.extractUpstream(Fixtures.file("hostile/symlink.tar.bz2"), members, mapOf("model.onnx" to small), temporary.newFolder(), CancelToken())
        }
    }

    @Test
    fun `truncated archives are refused`() {
        val version = Fixtures.tiny.latestVersion
        val bytes = Fixtures.bytes(version.mirror!!.fileName)
        listOf(100, 700, bytes.size - 600, bytes.size - 1).forEach { cut ->
            val archive = temporary.newFile().also { it.writeBytes(bytes.copyOf(cut)) }
            assertThrows("cut at $cut", ModelException::class.java) {
                extractor.extractMirror(archive, version.files, temporary.newFolder(), CancelToken())
            }
        }
        val upstream = Fixtures.bytes("tiny-model.tar.bz2")
        val cut = temporary.newFile().also { it.writeBytes(upstream.copyOf(upstream.size / 2)) }
        val archive = version.upstream.filterIsInstance<UpstreamPart.TarBz2>().single()
        assertThrows(ArchiveFormatException::class.java) {
            extractor.extractUpstream(cut, archive.members, version.files.associateBy { it.path }, temporary.newFolder(), CancelToken())
        }
    }

    @Test
    fun `mirror archives must be canonical and end exactly after two zero blocks`() {
        val content = "hello".toByteArray()
        val file = CatalogFile("a.txt", 5, sha(content))
        fun attempt(bytes: ByteArray) = extractor.extractMirror(temporary.newFile().also { it.writeBytes(bytes) }, listOf(file), temporary.newFolder(), CancelToken())
        val canonical = CatalogRules.canonicalTarHeader("a.txt", 5) + content + ByteArray(507) + ByteArray(1024)
        attempt(canonical)
        assertThrows(ArchiveFormatException::class.java) { attempt(TarBuilder.archive(TarBuilder.entry("a.txt", content, mode = 493))) }
        assertThrows(ArchiveFormatException::class.java) { attempt(canonical + ByteArray(1)) }
        assertThrows(ArchiveFormatException::class.java) { attempt(canonical.copyOf().also { it[520] = 1 }) }
        assertThrows(IntegrityException::class.java) { attempt(canonical.copyOf().also { it[512] = 'j'.code.toByte() }) }
    }

    @Test
    fun `imports refuse traversal and absolute paths, skip links and keep names out of the file system`() {
        val blobs = temporary.newFolder("blobs")
        val tar = TarBuilder.archive(
            TarBuilder.entry("model/../../evil.txt", "x".toByteArray()),
            TarBuilder.entry("model/link", ByteArray(0), type = '2'),
            TarBuilder.entry("model/tokens.txt", "a 1\n".toByteArray()),
        )
        val archive = temporary.newFile("import.tar").also { it.writeBytes(tar) }
        val files = extractor.extractForImport(archive, blobs, ArchiveExtractor.ImportLimits(1 shl 20), CancelToken())
        assertEquals(listOf("model/../../evil.txt", "model/tokens.txt"), files.map { it.name })
        assertEquals(listOf("0", "1"), blobs.list()!!.sorted())
        assertFalse(File(temporary.root, "evil.txt").exists())
        assertThrows(ArchiveFormatException::class.java) { ArchiveExtractor.installTarget(temporary.root, "../evil.txt") }
        assertThrows(ArchiveFormatException::class.java) { ArchiveExtractor.installTarget(temporary.root, "/etc/passwd") }
        assertThrows(ArchiveFormatException::class.java) { ArchiveExtractor.installTarget(temporary.root, "a/./b") }
    }

    @Test
    fun `zip imports are bounded in size and count`() {
        val zip = temporary.newFile("bomb.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("big.bin"))
            out.write(ByteArray(4 shl 20))
            out.closeEntry()
            out.putNextEntry(ZipEntry("dir/"))
            out.closeEntry()
        }
        assertTrue("the zip compresses far below its content", zip.length() < 64 * 1024)
        val error = assertThrows(InsufficientSpaceException::class.java) {
            extractor.extractForImport(zip, temporary.newFolder(), ArchiveExtractor.ImportLimits(1 shl 20, overflow = { InsufficientSpaceException(1 shl 21, 1 shl 20) }), CancelToken())
        }
        assertEquals(1L shl 20, error.available)
        val many = temporary.newFile("many.zip")
        ZipOutputStream(many.outputStream()).use { out ->
            repeat(5) {
                out.putNextEntry(ZipEntry("f$it"))
                out.write(it)
                out.closeEntry()
            }
        }
        assertThrows(ArchiveFormatException::class.java) {
            extractor.extractForImport(many, temporary.newFolder(), ArchiveExtractor.ImportLimits(1 shl 20, maxEntries = 4), CancelToken())
        }
    }

    @Test
    fun `formats are recognized by content`() {
        assertEquals(ArchiveExtractor.ImportFormat.TAR_BZ2, extractor.detectFormat(Fixtures.file("tiny-model.tar.bz2")))
        assertEquals(ArchiveExtractor.ImportFormat.TAR, extractor.detectFormat(Fixtures.file(Fixtures.tiny.latestVersion.mirror!!.fileName)))
        val zip = temporary.newFile("a.zip").also { file -> ZipOutputStream(file.outputStream()).use { it.putNextEntry(ZipEntry("a")); it.write(1); it.closeEntry() } }
        assertEquals(ArchiveExtractor.ImportFormat.ZIP, extractor.detectFormat(zip))
        assertThrows(ArchiveFormatException::class.java) { extractor.detectFormat(temporary.newFile().also { it.writeText("plain text") }) }
    }

    @Test
    fun `GNU long names and pax paths are read like tarfile`() {
        val longName = "dir/" + "n".repeat(150)
        val tar = TarBuilder.archive(
            TarBuilder.entry("././@LongLink", (longName + Char(0)).toByteArray(), type = 'L'),
            TarBuilder.entry(longName.take(100), "long".toByteArray()),
            TarBuilder.entry("PaxHeaders/x", paxRecord("path", "pax/name.txt"), type = 'x'),
            TarBuilder.entry("short", "pax".toByteArray()),
        )
        val reader = TarReader(ByteArrayInputStream(tar), canonical = false)
        assertEquals(longName, reader.next()!!.name)
        assertEquals("pax/name.txt", reader.next()!!.name)
        assertEquals(null, reader.next())
    }

    private fun paxRecord(key: String, value: String): ByteArray {
        val body = " $key=$value\n"
        var length = body.length + 1
        while ((length.toString() + body).length != length) length = (length.toString() + body).length
        return (length.toString() + body).toByteArray()
    }

    private fun sha(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
}
