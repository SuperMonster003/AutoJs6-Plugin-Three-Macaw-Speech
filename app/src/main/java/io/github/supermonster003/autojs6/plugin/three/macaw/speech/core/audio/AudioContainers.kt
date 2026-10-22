package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

/**
 * What the first bytes of an audio file look like (roadmap D81, D85). WAV goes to [WavReader], everything else to the
 * system extractor, and an Ogg file the system refuses to the plugin's own Ogg Opus parser; the label explains errors.
 * zh-CN: 音频文件开头字节看起来是什么 (路线图 D81, D85). WAV 交给 [WavReader], 其他交给系统提取器, 系统拒绝的 Ogg 文件再交给插件自带的 Ogg
 * Opus 解析; 标签用于说明错误.
 */
enum class AudioContainer(val label: String) {
    WAV("WAV"),
    MP3("MP3"),
    ADTS("AAC (ADTS)"),
    MP4("MP4 / M4A / 3GP"),
    OGG_OPUS("Ogg Opus"),
    OGG_VORBIS("Ogg Vorbis"),
    OGG("Ogg"),
    FLAC("FLAC"),
    AMR("AMR"),
    MATROSKA("Matroska / WebM"),
    UNKNOWN("an unknown format"),
}

/**
 * Container sniffing on the first [SNIFF_BYTES] bytes of a file. Pure JVM.
 * zh-CN: 根据文件前 [SNIFF_BYTES] 字节判断容器. 纯 JVM.
 */
object AudioContainers {

    const val SNIFF_BYTES = 64

    fun sniff(head: ByteArray): AudioContainer = when {
        ascii(head, 0, "RIFF") && ascii(head, 8, "WAVE") -> AudioContainer.WAV
        ascii(head, 0, "OggS") -> ogg(head)
        ascii(head, 0, "fLaC") -> AudioContainer.FLAC
        ascii(head, 0, "#!AMR") -> AudioContainer.AMR
        ascii(head, 4, "ftyp") -> AudioContainer.MP4
        head.size >= 4 && u8(head, 0) == 0x1A && u8(head, 1) == 0x45 && u8(head, 2) == 0xDF && u8(head, 3) == 0xA3 -> AudioContainer.MATROSKA
        ascii(head, 0, "ID3") -> AudioContainer.MP3
        // ADTS: 12 sync bits and layer 00; an MPEG audio frame has the same sync with a non-zero layer.
        head.size >= 2 && u8(head, 0) == 0xFF && (u8(head, 1) and 0xF6) == 0xF0 -> AudioContainer.ADTS
        head.size >= 2 && u8(head, 0) == 0xFF && (u8(head, 1) and 0xE0) == 0xE0 && (u8(head, 1) and 0x06) != 0 -> AudioContainer.MP3
        else -> AudioContainer.UNKNOWN
    }

    /** The codec of the first packet of an Ogg file's first page. zh-CN: Ogg 文件首页首包的编码. */
    private fun ogg(head: ByteArray): AudioContainer {
        if (head.size < 27) return AudioContainer.OGG
        val body = 27 + u8(head, 26)
        return when {
            ascii(head, body, "OpusHead") -> AudioContainer.OGG_OPUS
            head.size > body && u8(head, body) == 1 && ascii(head, body + 1, "vorbis") -> AudioContainer.OGG_VORBIS
            else -> AudioContainer.OGG
        }
    }

    private fun u8(bytes: ByteArray, offset: Int) = bytes[offset].toInt() and 0xff

    private fun ascii(bytes: ByteArray, offset: Int, text: String): Boolean =
        bytes.size >= offset + text.length && text.indices.all { bytes[offset + it] == text[it].code.toByte() }
}
