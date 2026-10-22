package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The identification header of an Ogg Opus stream (RFC 7845 section 5.1). [raw] is the whole packet, which the system
 * Opus decoder takes as its first codec specific data.
 * zh-CN: Ogg Opus 流的识别头 (RFC 7845 第 5.1 节). [raw] 为整个包, 系统 Opus 解码器把它作为第一项编解码器专用数据.
 */
class OpusHead(val channels: Int, val preSkip: Int, val inputRate: Long, val mappingFamily: Int, val raw: ByteArray) {

    /** CSD-1 of the system decoder: the pre-skip in nanoseconds, native (little endian) order. zh-CN: 系统解码器的 CSD-1: 以纳秒计的预跳过. */
    fun codecDelay(): ByteArray = nanos(preSkip * 1_000_000_000L / OpusPackets.SAMPLE_RATE)

    /** CSD-2 of the system decoder: the 80 ms seek pre-roll of RFC 7845. zh-CN: 系统解码器的 CSD-2: RFC 7845 的 80 ms 预滚. */
    fun seekPreRoll(): ByteArray = nanos(SEEK_PRE_ROLL_NANOS)

    companion object {
        const val SEEK_PRE_ROLL_NANOS = 80_000_000L

        fun parse(packet: ByteArray): OpusHead {
            if (packet.size < 19 || String(packet, 0, 8, Charsets.ISO_8859_1) != "OpusHead") throw damagedHeader("no OpusHead packet")
            val version = packet[8].toInt() and 0xff
            if (version shr 4 != 0) throw unsupported("Ogg Opus version $version is not supported")
            val channels = packet[9].toInt() and 0xff
            val data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
            val preSkip = data.getShort(10).toInt() and 0xffff
            val inputRate = data.getInt(12).toLong() and 0xffffffffL
            val family = packet[18].toInt() and 0xff
            when (family) {
                0 -> if (channels !in 1..2) throw damagedHeader("$channels channels with mapping family 0")
                1 -> if (channels !in 1..PcmFrames.MAX_CHANNELS || packet.size < 21 + channels) throw damagedHeader("$channels channels with mapping family 1")
                else -> throw unsupported("Ogg Opus channel mapping family $family is not supported")
            }
            return OpusHead(channels, preSkip, inputRate, family, packet.copyOf())
        }

        private fun nanos(value: Long): ByteArray = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()

        private fun damagedHeader(detail: String) = unsupported("The Ogg Opus header is damaged: $detail")

        private fun unsupported(message: String) = SpeechException(SpeechErrorCodes.UNSUPPORTED_FORMAT, message)
    }
}

/**
 * Opus packet durations from the table of contents byte (RFC 6716 section 3.1). Pure JVM.
 * zh-CN: 由目录字节得出的 Opus 包时长 (RFC 6716 第 3.1 节). 纯 JVM.
 */
object OpusPackets {

    /** Opus always decodes at 48 kHz. zh-CN: Opus 总是以 48 kHz 解码. */
    const val SAMPLE_RATE = 48_000

    /** Samples per channel at 48 kHz, or 0 for an empty or invalid packet. zh-CN: 48 kHz 下每声道的采样数; 空包或无效包为 0. */
    fun samples(packet: ByteArray): Int {
        if (packet.isEmpty()) return 0
        val toc = packet[0].toInt() and 0xff
        val config = toc shr 3
        val frameSize = when {
            config < 12 -> intArrayOf(480, 960, 1920, 2880)[config and 3]
            config < 16 -> intArrayOf(480, 960)[config and 1]
            else -> intArrayOf(120, 240, 480, 960)[config and 3]
        }
        val frames = when (toc and 3) {
            0 -> 1
            1, 2 -> 2
            else -> if (packet.size < 2) 0 else packet[1].toInt() and 0x3f
        }
        return (frames * frameSize).takeIf { it <= MAX_PACKET_SAMPLES } ?: 0
    }

    /** 120 ms, the longest packet RFC 6716 allows. zh-CN: 120 ms, RFC 6716 允许的最长包. */
    private const val MAX_PACKET_SAMPLES = 5760
}

/** One Opus packet of [OggOpusStream] with its presentation time. zh-CN: [OggOpusStream] 的一个 Opus 包及其呈现时间. */
class OpusPacket(val data: ByteArray, val timeMicros: Long)

/**
 * The plugin's own Ogg reader for Opus (roadmap D81: the system extractor reads Ogg Opus only from Android 10): pages
 * with their CRC (RFC 3533), packets joined across pages, the first logical stream that starts with `OpusHead`, its
 * `OpusTags` skipped. A page that fails its checksum or loses the capture pattern is damage (`UNSUPPORTED_FORMAT`);
 * a file cut short ends with the packets it completes, as a truncated WAV does. Pure JVM.
 * zh-CN: 插件自带的 Opus 用 Ogg 读取器 (路线图 D81: 系统提取器从 Android 10 起才读 Ogg Opus): 带 CRC 的页 (RFC 3533), 跨页拼接的包, 以
 * `OpusHead` 开始的第一个逻辑流, 并跳过其 `OpusTags`. 校验失败或丢失捕获标记的页视为损坏 (`UNSUPPORTED_FORMAT`); 被截断的文件以其完整的包结束,
 * 与截断的 WAV 相同. 纯 JVM.
 */
class OggOpusStream(input: InputStream) : Closeable {

    private val input = input.buffered(64 * 1024)
    private val header = ByteArray(27)
    private val ready = ArrayDeque<ByteArray>()
    private val partial = ByteArrayOutputStream()
    private var serial: Int? = null
    private var ended = false
    private var samples = 0L

    val head: OpusHead

    init {
        head = OpusHead.parse(firstPacket())
        val tags = packet()
        if (tags != null && !(tags.size >= 8 && String(tags, 0, 8, Charsets.ISO_8859_1) == "OpusTags")) ready.addFirst(tags)
    }

    /** The next audio packet, or null at the end of the stream. zh-CN: 下一个音频包; 流结束时为 null. */
    fun next(): OpusPacket? {
        val data = packet() ?: return null
        val time = samples * 1_000_000L / OpusPackets.SAMPLE_RATE
        samples += OpusPackets.samples(data)
        return OpusPacket(data, time)
    }

    /** Milliseconds of the packets handed out so far. zh-CN: 已交出的包的毫秒数. */
    val millis: Long get() = samples * 1000L / OpusPackets.SAMPLE_RATE

    override fun close() = input.close()

    /** Picks the first beginning-of-stream page whose first packet is `OpusHead`. zh-CN: 选出首包为 `OpusHead` 的第一个流起始页. */
    private fun firstPacket(): ByteArray {
        while (true) {
            val page = page() ?: throw SpeechException(SpeechErrorCodes.UNSUPPORTED_FORMAT, "The Ogg file has no Opus stream")
            if ((page.type and BOS) == 0) throw SpeechException(SpeechErrorCodes.UNSUPPORTED_FORMAT, "The Ogg file has no Opus stream")
            val first = page.packets.firstOrNull() ?: continue
            if (first.size >= 8 && String(first, 0, 8, Charsets.ISO_8859_1) == "OpusHead" && page.complete.firstOrNull() == true) {
                serial = page.serial
                page.packets.drop(1).forEachIndexed { index, packet -> if (page.complete[index + 1]) ready.addLast(packet) else partial.write(packet) }
                if ((page.type and EOS) != 0) ended = true
                return first
            }
        }
    }

    private fun packet(): ByteArray? {
        while (ready.isEmpty()) {
            if (ended) return null
            val page = page() ?: return null.also { ended = true }
            if (page.serial != serial) continue
            page.packets.forEachIndexed { index, bytes ->
                // A page that does not continue a packet drops a leftover fragment (the previous page was lost).
                if (index == 0 && (page.type and CONTINUED) == 0) partial.reset()
                partial.write(bytes)
                if (page.complete[index]) {
                    ready.addLast(partial.toByteArray())
                    partial.reset()
                }
            }
            if ((page.type and EOS) != 0) ended = true
        }
        return ready.removeFirst()
    }

    private class Page(val type: Int, val serial: Int, val packets: List<ByteArray>, val complete: List<Boolean>)

    /** Reads one page, or null at the end of the file (or of a truncated page). zh-CN: 读取一页; 文件结束 (或页被截断) 时为 null. */
    private fun page(): Page? {
        if (!fill(header, 0, 27)) return null
        if (String(header, 0, 4, Charsets.ISO_8859_1) != "OggS" || header[4].toInt() != 0) throw damaged("lost the Ogg page capture pattern")
        val type = header[5].toInt() and 0xff
        val pageSerial = ByteBuffer.wrap(header, 14, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val expected = ByteBuffer.wrap(header, 22, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val count = header[26].toInt() and 0xff
        val lacing = ByteArray(count)
        if (!fill(lacing, 0, count)) return null
        val body = ByteArray(lacing.sumOf { it.toInt() and 0xff })
        if (!fill(body, 0, body.size)) return null
        val check = header.copyOf().also { it.fill(0, 22, 26) }
        var crc = crc(0, check, 0, check.size)
        crc = crc(crc, lacing, 0, lacing.size)
        crc = crc(crc, body, 0, body.size)
        if (crc != expected) throw damaged("an Ogg page fails its checksum")
        val packets = ArrayList<ByteArray>()
        val complete = ArrayList<Boolean>()
        var offset = 0
        var start = 0
        for (index in 0 until count) {
            val value = lacing[index].toInt() and 0xff
            offset += value
            if (value < 255 || index == count - 1) {
                packets += body.copyOfRange(start, offset)
                complete += value < 255
                start = offset
            }
        }
        return Page(type, pageSerial, packets, complete)
    }

    private fun fill(target: ByteArray, offset: Int, count: Int): Boolean {
        var filled = 0
        while (filled < count) {
            val read = input.read(target, offset + filled, count - filled)
            if (read < 0) return false
            filled += read
        }
        return true
    }

    private fun damaged(detail: String) = DecodeRules.damaged(millis, IllegalStateException(detail))

    companion object {
        private const val CONTINUED = 0x01
        private const val BOS = 0x02
        private const val EOS = 0x04

        /** The Ogg CRC-32: polynomial 0x04C11DB7, no reflection, zero start, no final xor. zh-CN: Ogg 的 CRC-32. */
        private val CRC_TABLE = IntArray(256) { index ->
            var value = index shl 24
            repeat(8) { value = if ((value and 0x80000000.toInt()) != 0) (value shl 1) xor 0x04C11DB7 else value shl 1 }
            value
        }

        internal fun crc(start: Int, bytes: ByteArray, offset: Int, count: Int): Int {
            var crc = start
            for (index in offset until offset + count) {
                crc = (crc shl 8) xor CRC_TABLE[((crc ushr 24) xor (bytes[index].toInt() and 0xff)) and 0xff]
            }
            return crc
        }
    }
}
