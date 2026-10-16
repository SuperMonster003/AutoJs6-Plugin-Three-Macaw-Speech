package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * A bzip2 decoder for the upstream `tar.bz2` fallback (roadmap D9). The plugin carries its own instead of Apache
 * Commons Compress: 1.26 and later read through commons-io builders that need `java.nio.file` (API 26, the plugin's
 * minSdk is 24), and 1.25 and earlier are flagged by CVE-2024-25710 / CVE-2024-26308. Every block CRC and the stream
 * CRC are checked, concatenated streams are read in turn (like `bz2.open` of Python, data after the last complete
 * stream that is not another stream is ignored), and randomized blocks (never written since bzip2 0.9.5) are refused.
 * The output also passes the file-level SHA-256 checks, so a decoder fault can never install wrong bytes.
 * zh-CN: 用于上游 `tar.bz2` 回退 (路线图 D9) 的 bzip2 解码器. 插件自带实现而不用 Apache Commons Compress: 1.26 及之后的版本经 commons-io
 * 的构建器读取, 需要 `java.nio.file` (API 26, 而插件 minSdk 为 24); 1.25 及之前的版本带有 CVE-2024-25710 / CVE-2024-26308. 每个块的 CRC
 * 与流 CRC 都会校验, 依次读取拼接的多个流 (与 Python 的 `bz2.open` 相同, 最后一个完整流之后若不是新流则忽略), 拒绝随机化块 (bzip2 0.9.5
 * 起不再写出). 输出还要通过文件级 SHA-256 校验, 解码器缺陷不可能安装错误的字节.
 */
class Bzip2InputStream(input: InputStream) : InputStream() {

    private val source = if (input.markSupported()) input else java.io.BufferedInputStream(input, 64 * 1024)

    private var bitBuffer = 0L
    private var bitCount = 0

    private var blockSize100k = 0
    private var streamCrc = 0
    private var finished = false
    private var started = false

    // Current block: BWT vector, remaining outputs, run-length state and CRC.
    private var tt = IntArray(0)
    private var blockLength = 0
    private var produced = 0
    private var tPos = 0
    private var expectedBlockCrc = 0
    private var blockCrc = -1
    private var lastByte = -1
    private var runLength = 0
    private var repeatByte = 0
    private var repeatLeft = 0
    private var blockActive = false

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || offset + length > buffer.size) throw IndexOutOfBoundsException()
        if (length == 0) return 0
        var written = 0
        while (written < length) {
            if (repeatLeft > 0) {
                buffer[offset + written++] = repeatByte.toByte()
                updateCrc(repeatByte)
                repeatLeft--
                continue
            }
            if (!blockActive || produced == blockLength) {
                if (blockActive) finishBlock()
                if (!nextBlock()) break
                continue
            }
            if (tPos < 0 || tPos >= blockLength) throw IOException("Corrupt bzip2 block")
            tPos = tt[tPos]
            val value = tPos and 0xFF
            tPos = tPos ushr 8
            produced++
            if (runLength == 4) {
                repeatByte = lastByte
                repeatLeft = value
                runLength = 0
                lastByte = -1
                continue
            }
            if (value == lastByte) runLength++ else {
                lastByte = value
                runLength = 1
            }
            buffer[offset + written++] = value.toByte()
            updateCrc(value)
        }
        return if (written == 0) -1 else written
    }

    override fun close() = source.close()

    private fun updateCrc(value: Int) {
        blockCrc = (blockCrc shl 8) xor CRC_TABLE[((blockCrc ushr 24) xor value) and 0xFF]
    }

    private fun finishBlock() {
        if (repeatLeft > 0) return
        val computed = blockCrc.inv()
        if (computed != expectedBlockCrc) throw IOException("bzip2 block CRC mismatch")
        streamCrc = ((streamCrc shl 1) or (streamCrc ushr 31)) xor computed
        blockActive = false
    }

    /** Reads the next block header, crossing stream boundaries; false at the end of the data. */
    private fun nextBlock(): Boolean {
        if (finished) return false
        if (!started) {
            if (!readStreamHeader(first = true)) {
                finished = true
                return false
            }
            started = true
        }
        while (true) {
            val magic = (readBits(24).toLong() shl 24) or readBits(24).toLong()
            when (magic) {
                BLOCK_MAGIC -> {
                    decodeBlock()
                    return true
                }
                END_MAGIC -> {
                    val stored = readBits(32)
                    if (stored != streamCrc) throw IOException("bzip2 stream CRC mismatch")
                    // Streams end on a byte boundary; another stream may follow.
                    bitCount -= bitCount % 8
                    if (!readStreamHeader(first = false)) {
                        finished = true
                        return false
                    }
                }
                else -> throw IOException("Bad bzip2 block magic")
            }
        }
    }

    private fun readStreamHeader(first: Boolean): Boolean {
        val b = readByteOrEof()
        if (b < 0) {
            if (first) throw EOFException("Empty bzip2 data")
            return false
        }
        val z = readByteOrEof()
        val h = readByteOrEof()
        val level = readByteOrEof()
        if (b != 'B'.code || z != 'Z'.code || h != 'h'.code || level !in '1'.code..'9'.code) {
            if (first) throw IOException("Not bzip2 data")
            // Trailing data after a complete stream is ignored, as Python's bz2 module does.
            return false
        }
        blockSize100k = level - '0'.code
        streamCrc = 0
        return true
    }

    private fun decodeBlock() {
        expectedBlockCrc = readBits(32)
        if (readBits(1) != 0) throw IOException("Randomized bzip2 blocks are not supported")
        val origPtr = readBits(24)

        // Symbol map.
        val seqToUnseq = IntArray(256)
        var inUseCount = 0
        val used16 = readBits(16)
        for (i in 0 until 16) {
            if ((used16 and (0x8000 ushr i)) != 0) {
                val bits = readBits(16)
                for (j in 0 until 16) {
                    if ((bits and (0x8000 ushr j)) != 0) seqToUnseq[inUseCount++] = i * 16 + j
                }
            }
        }
        if (inUseCount == 0) throw IOException("bzip2 block uses no symbols")
        val alphaSize = inUseCount + 2

        // Selectors.
        val groupCount = readBits(3)
        if (groupCount !in 2..6) throw IOException("Bad bzip2 group count")
        val selectorCount = readBits(15)
        if (selectorCount < 1) throw IOException("Bad bzip2 selector count")
        val selectors = ByteArray(minOf(selectorCount, MAX_SELECTORS))
        val mtfGroups = IntArray(groupCount) { it }
        for (i in 0 until selectorCount) {
            var j = 0
            while (readBits(1) == 1) {
                j++
                if (j >= groupCount) throw IOException("Bad bzip2 selector")
            }
            if (i < MAX_SELECTORS) {
                val value = mtfGroups[j]
                System.arraycopy(mtfGroups, 0, mtfGroups, 1, j)
                mtfGroups[0] = value
                selectors[i] = value.toByte()
            }
        }

        // Coding tables.
        val limit = Array(groupCount) { IntArray(MAX_CODE_LENGTH + 2) }
        val base = Array(groupCount) { IntArray(MAX_CODE_LENGTH + 2) }
        val perm = Array(groupCount) { IntArray(alphaSize) }
        val minLength = IntArray(groupCount)
        for (t in 0 until groupCount) {
            val lengths = IntArray(alphaSize)
            var current = readBits(5)
            for (i in 0 until alphaSize) {
                while (true) {
                    if (current < 1 || current > 20) throw IOException("Bad bzip2 code length")
                    if (readBits(1) == 0) break
                    current += if (readBits(1) == 0) 1 else -1
                }
                lengths[i] = current
            }
            val min = lengths.min()
            val max = lengths.max()
            minLength[t] = min
            createDecodeTables(limit[t], base[t], perm[t], lengths, min, max, alphaSize)
        }

        // Huffman, run-length (RUNA / RUNB) and move-to-front decoding.
        val blockMax = blockSize100k * 100_000
        if (tt.size < blockMax) tt = IntArray(blockMax)
        val counts = IntArray(256)
        val mtf = IntArray(256) { it }
        val endOfBlock = inUseCount + 1
        var groupIndex = -1
        var groupLeft = 0
        var count = 0

        fun nextSymbol(): Int {
            if (groupLeft == 0) {
                groupIndex++
                if (groupIndex >= selectors.size) throw IOException("bzip2 selectors exhausted")
                groupLeft = GROUP_SIZE
            }
            groupLeft--
            val group = selectors[groupIndex].toInt()
            var length = minLength[group]
            var code = readBits(length)
            while (code > limit[group][length]) {
                length++
                if (length > MAX_CODE_LENGTH) throw IOException("Bad bzip2 Huffman code")
                code = (code shl 1) or readBits(1)
            }
            val index = code - base[group][length]
            if (index < 0 || index >= alphaSize) throw IOException("Bad bzip2 Huffman code")
            return perm[group][index]
        }

        var symbol = nextSymbol()
        while (symbol != endOfBlock) {
            if (symbol == RUNA || symbol == RUNB) {
                var run = 0
                var weight = 1
                while (symbol == RUNA || symbol == RUNB) {
                    run += if (symbol == RUNA) weight else 2 * weight
                    weight = weight shl 1
                    if (weight >= 2 * 1024 * 1024) throw IOException("bzip2 run is too long")
                    symbol = nextSymbol()
                }
                val value = seqToUnseq[mtf[0]]
                if (count + run > blockMax) throw IOException("bzip2 block exceeds its declared size")
                counts[value] += run
                java.util.Arrays.fill(tt, count, count + run, value)
                count += run
            } else {
                if (count >= blockMax) throw IOException("bzip2 block exceeds its declared size")
                val position = symbol - 1
                if (position >= inUseCount) throw IOException("Bad bzip2 move-to-front index")
                val index = mtf[position]
                System.arraycopy(mtf, 0, mtf, 1, position)
                mtf[0] = index
                val value = seqToUnseq[index]
                counts[value]++
                tt[count++] = value
                symbol = nextSymbol()
            }
        }
        if (origPtr < 0 || origPtr >= count) throw IOException("bzip2 origin pointer out of range")

        // Inverse Burrows-Wheeler transform: the high 24 bits link each position to the next.
        val cumulative = IntArray(257)
        for (i in 0 until 256) cumulative[i + 1] = cumulative[i] + counts[i]
        for (i in 0 until count) {
            val value = tt[i] and 0xFF
            tt[cumulative[value]] = tt[cumulative[value]] or (i shl 8)
            cumulative[value]++
        }
        tPos = tt[origPtr] ushr 8
        blockLength = count
        produced = 0
        blockCrc = -1
        lastByte = -1
        runLength = 0
        repeatLeft = 0
        blockActive = true
    }

    private fun createDecodeTables(limit: IntArray, base: IntArray, perm: IntArray, lengths: IntArray, min: Int, max: Int, alphaSize: Int) {
        var p = 0
        for (i in min..max) for (j in 0 until alphaSize) if (lengths[j] == i) perm[p++] = j
        java.util.Arrays.fill(base, 0)
        for (i in 0 until alphaSize) base[lengths[i] + 1]++
        for (i in 1 until base.size) base[i] += base[i - 1]
        java.util.Arrays.fill(limit, 0)
        var vec = 0
        for (i in min..max) {
            vec += base[i + 1] - base[i]
            limit[i] = vec - 1
            vec = vec shl 1
        }
        for (i in min + 1..max) base[i] = ((limit[i - 1] + 1) shl 1) - base[i]
        // Lengths past max never match: keep their limit above any code.
        for (i in max + 1 until limit.size) limit[i] = Int.MAX_VALUE
    }

    private fun readByteOrEof(): Int {
        if (bitCount >= 8) return readBits(8)
        val next = source.read()
        if (next < 0) return -1
        bitBuffer = (bitBuffer shl 8) or next.toLong()
        bitCount += 8
        return readBits(8)
    }

    private fun readBits(count: Int): Int {
        while (bitCount < count) {
            val next = source.read()
            if (next < 0) throw EOFException("Truncated bzip2 data")
            bitBuffer = (bitBuffer shl 8) or next.toLong()
            bitCount += 8
        }
        bitCount -= count
        return ((bitBuffer ushr bitCount) and ((1L shl count) - 1)).toInt()
    }

    private companion object {
        const val BLOCK_MAGIC = 0x314159265359L
        const val END_MAGIC = 0x177245385090L
        const val MAX_SELECTORS = 18002
        const val MAX_CODE_LENGTH = 20
        const val GROUP_SIZE = 50
        const val RUNA = 0
        const val RUNB = 1

        val CRC_TABLE = IntArray(256) { index ->
            var crc = index shl 24
            repeat(8) { crc = if ((crc and Int.MIN_VALUE) != 0) (crc shl 1) xor 0x04C11DB7 else crc shl 1 }
            crc
        }
    }
}
