package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Container sniffing of roadmap D81 / D85 (P7.5.1): every signature and heads cut short; the encoded device recordings
 * of `locks/encoded-test-audio.lock` must sniff as what they are.
 * zh-CN: 路线图 D81 / D85 的容器判断 (P7.5.1): 各种签名与被截短的文件头; `locks/encoded-test-audio.lock` 的编码设备录音必须被判断为其实际
 * 容器.
 */
class AudioContainersTest {

    @Test
    fun `signatures map to their containers`() {
        assertEquals(AudioContainer.WAV, sniff("RIFF", 0, 0, 0, 0, "WAVE"))
        assertEquals(AudioContainer.UNKNOWN, sniff("RIFF", 0, 0, 0, 0, "AVI "))
        assertEquals(AudioContainer.FLAC, sniff("fLaC", 0))
        assertEquals(AudioContainer.AMR, sniff("#!AMR\n"))
        assertEquals(AudioContainer.MP4, sniff(0, 0, 0, 0x20, "ftypM4A "))
        assertEquals(AudioContainer.MATROSKA, sniff(0x1A, 0x45, 0xDF, 0xA3, 0x9F))
        assertEquals(AudioContainer.MP3, sniff("ID3", 4, 0))
        assertEquals(AudioContainer.MP3, sniff(0xFF, 0xFB, 0x90))
        assertEquals(AudioContainer.ADTS, sniff(0xFF, 0xF1, 0x50))
        assertEquals(AudioContainer.ADTS, sniff(0xFF, 0xF9, 0x50))
        assertEquals(AudioContainer.UNKNOWN, sniff("hello world"))
        assertEquals(AudioContainer.UNKNOWN, ByteArray(0).let(AudioContainers::sniff))
    }

    @Test
    fun `ogg names the codec of its first packet`() {
        assertEquals(AudioContainer.OGG_OPUS, AudioContainers.sniff(oggHead("OpusHead".toByteArray() + ByteArray(11))))
        assertEquals(AudioContainer.OGG_VORBIS, AudioContainers.sniff(oggHead(byteArrayOf(1) + "vorbis".toByteArray() + ByteArray(23))))
        assertEquals(AudioContainer.OGG, AudioContainers.sniff(oggHead("Speex   ".toByteArray())))
        // Too short to see the first packet.
        assertEquals(AudioContainer.OGG, AudioContainers.sniff(oggHead("OpusHead".toByteArray()).copyOf(20)))
        assertEquals(AudioContainer.OGG, AudioContainers.sniff(oggHead("OpusHead".toByteArray()).copyOf(30)))
    }

    @Test
    fun `the encoded device recordings sniff as what they are`() {
        val expected = mapOf(
            "mp3" to AudioContainer.MP3, "aac" to AudioContainer.ADTS, "m4a" to AudioContainer.MP4, "mp4" to AudioContainer.MP4,
            "ogg" to AudioContainer.OGG_VORBIS, "opus" to AudioContainer.OGG_OPUS, "flac" to AudioContainer.FLAC, "amr" to AudioContainer.AMR,
            "webm" to AudioContainer.MATROSKA,
        )
        val files = ENCODED.listFiles().orEmpty().filter { it.isFile }
        assertEquals(17, files.size)
        files.forEach { file ->
            val head = file.inputStream().use { it.readNBytes(AudioContainers.SNIFF_BYTES) }
            assertEquals(file.name, expected.getValue(file.extension), AudioContainers.sniff(head))
        }
        val wav = File(ENCODED.parentFile, "test-audio/en.wav").inputStream().use { it.readNBytes(AudioContainers.SNIFF_BYTES) }
        assertEquals(AudioContainer.WAV, AudioContainers.sniff(wav))
    }

    companion object {
        val ENCODED = File("src/androidTest/assets/encoded-audio")

        private fun sniff(vararg parts: Any): AudioContainer = AudioContainers.sniff(bytes(*parts))

        private fun bytes(vararg parts: Any): ByteArray = parts.fold(ByteArray(0)) { acc, part ->
            acc + when (part) {
                is String -> part.toByteArray(Charsets.ISO_8859_1)
                is Int -> byteArrayOf(part.toByte())
                else -> error(part)
            }
        }

        /** The first page of an Ogg file holding [packet] as its only segment. zh-CN: 以 [packet] 为唯一段的 Ogg 首页. */
        private fun oggHead(packet: ByteArray): ByteArray =
            bytes("OggS", 0, 2) + ByteArray(20) + byteArrayOf(1, packet.size.toByte()) + packet
    }
}
