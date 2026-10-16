package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * One tar member as the reader reports it. [name] is untrusted text: callers map it to an installed path through the
 * catalog or a validated import path and never use it as a file name directly.
 * zh-CN: 读取器报告的单个 tar 成员. [name] 为不可信文本: 调用方经目录或已校验的导入路径映射到安装路径, 从不直接把它当作文件名.
 */
class TarEntry(val name: String, val size: Long, val isRegularFile: Boolean, val isDirectory: Boolean)

/**
 * Streaming tar reader in two modes. [canonical] = true accepts only the canonical mirror layout of Shared-Assets
 * (`distribution.read_archive`): every header byte equal to [CatalogRules.canonicalTarHeader], ascending unique paths,
 * zero padding and exactly two zero blocks before the end of the data. Otherwise it reads the GNU and POSIX archives of
 * the sherpa-onnx releases like Python's `tarfile` (header checksum, octal or base-256 sizes, GNU long names, pax
 * `path` / `size` records, the USTAR prefix, trailing slashes of directories). A short read anywhere is a truncation
 * error and member data can never run past its header size.
 * zh-CN: 两种模式的流式 tar 读取器. [canonical] 为 true 时只接受 Shared-Assets 镜像的规范布局 (`distribution.read_archive`): 每个头部字节
 * 都等于 [CatalogRules.canonicalTarHeader], 路径升序且唯一, 填充为零, 数据结束前恰好两个零块. 否则按 Python `tarfile` 的方式读取
 * sherpa-onnx Release 的 GNU 与 POSIX 归档 (头部校验和, 八进制或 base-256 大小, GNU 长名, pax `path` / `size` 记录, USTAR 前缀, 目录末尾的
 * 斜杠). 任何位置读不满都视为截断错误, 成员数据不会越过其头部声明的大小.
 */
class TarReader(private val input: InputStream, private val canonical: Boolean, private val maxEntries: Int = CatalogRules.MAX_FILES * 4) {

    private var remaining = 0L
    private var padding = 0L
    private var previous: String? = null
    private var entries = 0
    private var ended = false

    /** The next member, or null at the end of the archive; the previous member's data is skipped. zh-CN: 下一个成员; 结束时为 null. */
    fun next(): TarEntry? {
        if (ended) return null
        skip(remaining + padding)
        remaining = 0
        padding = 0
        var longName: String? = null
        var paxPath: String? = null
        var paxSize: Long? = null
        while (true) {
            val header = readBlock()
            if (header.all { it.toInt() == 0 }) {
                if (canonical) {
                    if (!readBlock().all { it.toInt() == 0 } || input.read() != -1) throw ArchiveFormatException("Archive must end with exactly two zero blocks")
                }
                ended = true
                return null
            }
            if (++entries > maxEntries) throw ArchiveFormatException("Archive contains too many entries")
            if (canonical) return canonicalEntry(header)
            verifyChecksum(header)
            val type = header[156].toInt().toChar()
            val size = number(header, 124, 12)
            if (size < 0 || size > CatalogRules.MAX_FILE_BYTES) throw ArchiveFormatException("Archive member size out of range")
            when (type) {
                'L' -> {
                    longName = readText(size)
                    continue
                }
                'x' -> {
                    val records = parsePax(readText(size))
                    records["path"]?.let { paxPath = it }
                    records["size"]?.let { value ->
                        paxSize = value.toLongOrNull()?.takeIf { it in 0..CatalogRules.MAX_FILE_BYTES }
                            ?: throw ArchiveFormatException("Invalid pax size")
                    }
                    continue
                }
                'g' -> {
                    readText(size)
                    continue
                }
            }
            val actualSize = paxSize ?: size
            var name = paxPath ?: longName ?: headerName(header, type)
            val directory = type == '5' || (type == '0' || type == Char(0)) && name.endsWith("/")
            if (directory) name = name.trimEnd('/')
            // Like tarfile's REGTYPE / AREGTYPE: contiguous ('7') and sparse files are not plain regular files.
            val regular = !directory && (type == '0' || type == Char(0))
            // Links, devices and FIFOs carry no data; directories and other types are skipped.
            remaining = if (type == '1' || type == '2' || type == '3' || type == '4' || type == '6') 0 else actualSize
            padding = CatalogRules.padded(remaining) - remaining
            return TarEntry(name, actualSize, regular, directory)
        }
    }

    /** Data of the current member; reading past its size returns end of stream. zh-CN: 当前成员的数据. */
    fun entryStream(): InputStream = object : InputStream() {
        override fun read(): Int {
            if (remaining <= 0) return -1
            val value = input.read()
            if (value < 0) throw EOFException("Archive is truncated")
            remaining--
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0) return -1
            val count = input.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (count < 0) throw EOFException("Archive is truncated")
            remaining -= count
            return count
        }
    }

    private fun canonicalEntry(header: ByteArray): TarEntry {
        val name = cString(header, 0, 100)
        val prefix = cString(header, 345, 155)
        val size = header.copyOfRange(124, 135).toString(Charsets.ISO_8859_1).toLongOrNull(8)
            ?: throw ArchiveFormatException("Archive header is not canonical")
        val member = if (prefix.isEmpty()) name else "$prefix/$name"
        val expected = try {
            CatalogRules.canonicalTarHeader(member, size)
        } catch (e: IllegalArgumentException) {
            throw ArchiveFormatException("Archive header is not canonical")
        }
        if (!expected.contentEquals(header)) throw ArchiveFormatException("Archive header is not canonical: $member")
        previous?.let { if (member <= it) throw ArchiveFormatException("Archive members are not in ascending path order") }
        previous = member
        remaining = size
        padding = CatalogRules.padded(size) - size
        return TarEntry(member, size, isRegularFile = true, isDirectory = false)
    }

    /** Skips the current member's padding after its data was read, checking the zero bytes in canonical mode. */
    private fun skip(count: Long) {
        var left = count
        val buffer = ByteArray(8192)
        // Padding is the tail of [count]; canonical archives must pad with zero bytes.
        val dataPart = count - padding
        var position = 0L
        while (left > 0) {
            val read = input.read(buffer, 0, minOf(left, buffer.size.toLong()).toInt())
            if (read < 0) throw ArchiveFormatException("Archive is truncated", EOFException())
            if (canonical) {
                for (i in 0 until read) if (position + i >= dataPart && buffer[i].toInt() != 0) throw ArchiveFormatException("Archive padding must be zero bytes")
            }
            position += read
            left -= read
        }
    }

    private fun readBlock(): ByteArray {
        val block = ByteArray(CatalogRules.BLOCK)
        var offset = 0
        while (offset < block.size) {
            val read = input.read(block, offset, block.size - offset)
            if (read < 0) throw ArchiveFormatException("Archive is truncated", EOFException())
            offset += read
        }
        return block
    }

    private fun readText(size: Long): String {
        if (size > MAX_METADATA_BYTES) throw ArchiveFormatException("Archive metadata record is too large")
        val bytes = ByteArray(size.toInt())
        var offset = 0
        while (offset < bytes.size) {
            val read = input.read(bytes, offset, bytes.size - offset)
            if (read < 0) throw ArchiveFormatException("Archive is truncated", EOFException())
            offset += read
        }
        skipExact(CatalogRules.padded(size) - size)
        val end = bytes.indexOf(0).let { if (it < 0) bytes.size else it }
        return String(bytes, 0, end, Charsets.UTF_8)
    }

    private fun skipExact(count: Long) {
        var left = count
        while (left > 0) {
            if (input.read() < 0) throw ArchiveFormatException("Archive is truncated", EOFException())
            left--
        }
    }

    private fun headerName(header: ByteArray, type: Char): String {
        val name = cString(header, 0, 100)
        val posix = header.copyOfRange(257, 263).contentEquals(POSIX_MAGIC)
        val prefix = if (posix && type !in GNU_TYPES) cString(header, 345, 155) else ""
        return if (prefix.isEmpty()) name else "$prefix/$name"
    }

    private fun verifyChecksum(header: ByteArray) {
        val stored = number(header, 148, 8)
        var unsigned = 0L
        var signed = 0L
        for (i in header.indices) {
            val value = if (i in 148 until 156) ' '.code.toByte() else header[i]
            unsigned += value.toInt() and 0xFF
            signed += value.toInt()
        }
        if (stored != unsigned && stored != signed) throw ArchiveFormatException("Archive header checksum mismatch")
    }

    /** `tarfile.nti`: octal text, or GNU base-256 when the first byte has its high bit set. */
    private fun number(header: ByteArray, offset: Int, length: Int): Long {
        val first = header[offset].toInt() and 0xFF
        if (first == 0x80 || first == 0xFF) {
            var value = 0L
            for (i in 1 until length) {
                if (value ushr 55 != 0L) throw ArchiveFormatException("Archive number overflows")
                value = (value shl 8) or (header[offset + i].toLong() and 0xFF)
            }
            return if (first == 0xFF) value - (1L shl (8 * (length - 1))) else value
        }
        val text = cString(header, offset, length).trim()
        if (text.isEmpty()) return 0
        return text.toLongOrNull(8) ?: throw ArchiveFormatException("Invalid number in archive header")
    }

    private fun cString(header: ByteArray, offset: Int, length: Int): String {
        var end = offset
        while (end < offset + length && header[end].toInt() != 0) end++
        return String(header, offset, end - offset, Charsets.ISO_8859_1)
    }

    /** `length key=value\n` records (POSIX.1-2001 extended headers). */
    private fun parsePax(text: String): Map<String, String> {
        val records = HashMap<String, String>()
        var at = 0
        val bytes = text.toByteArray(Charsets.UTF_8)
        while (at < bytes.size) {
            val space = (at until bytes.size).firstOrNull { bytes[it] == ' '.code.toByte() } ?: throw ArchiveFormatException("Invalid pax header")
            val length = String(bytes, at, space - at, Charsets.US_ASCII).toIntOrNull() ?: throw ArchiveFormatException("Invalid pax header")
            if (length <= space - at + 1 || at + length > bytes.size || bytes[at + length - 1] != '\n'.code.toByte()) throw ArchiveFormatException("Invalid pax header")
            val record = String(bytes, space + 1, at + length - space - 2, Charsets.UTF_8)
            val equals = record.indexOf('=')
            if (equals <= 0) throw ArchiveFormatException("Invalid pax header")
            records[record.substring(0, equals)] = record.substring(equals + 1)
            at += length
        }
        return records
    }

    private companion object {
        const val MAX_METADATA_BYTES = 64L * 1024
        val POSIX_MAGIC = byteArrayOf('u'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(), 'r'.code.toByte(), 0)
        val GNU_TYPES = setOf('L', 'K', 'S')
    }
}
