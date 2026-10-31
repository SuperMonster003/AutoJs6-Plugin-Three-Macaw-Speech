package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PlaybackRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.WavWriter
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 * The player of the file tab (roadmap D164, D171): what the system can decode plays from the picked file through
 * `MediaPlayer`, anything else (the Ogg branch of D86) from a temporary WAV of the decoded samples, and a descriptor that
 * cannot seek from a temporary copy. A file is prepared on its own worker thread as soon as the result shows, so a file
 * that cannot be opened any more is explained at once ([State.UNAVAILABLE]). [open] and [close] follow the shown result;
 * [release] and [restore] follow the visible screen, which keeps the position but gives the player and its temporary
 * files up. Only [speed] outlives a file; it lasts as long as the screen (D169). [onStart] stops the synthesis player:
 * one sound at a time. Compose state, changed on the main thread only; never log the source (D85).
 * zh-CN: 文件标签的播放器 (路线图 D164, D171): 系统能解码的文件直接用 `MediaPlayer` 播放所选文件, 其余 (D86 的 Ogg 分支) 播放解码采样写成的临时 WAV,
 * 不能定位的描述符播放临时副本. 结果一显示就在自己的工作线程上准备, 所以文件已打不开时立即说明 ([State.UNAVAILABLE]). [open] 与 [close] 跟随显示的结果;
 * [release] 与 [restore] 跟随页面是否可见, 保留位置但放弃播放器与临时文件. 只有 [speed] 不随文件结束, 保留到页面结束 (D169). [onStart] 停止合成的播放:
 * 同时只播一个. 均为 Compose 状态, 只在主线程上修改; 不记录来源 (D85).
 */
internal class FilePlayback(context: Context, private val onStart: () -> Unit) {

    enum class State { CLOSED, PREPARING, READY, UNAVAILABLE }

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val decoded = File(app.cacheDir, "tryout/$DECODED_NAME")
    private val copied = File(app.cacheDir, "tryout/$COPY_NAME")
    private var player: MediaPlayer? = null
    private var owner: Any? = null
    private var source: FileSource? = null
    private var seekingUntil = 0L

    /** The speed the player has now. zh-CN: 播放器当前的速度. */
    private var applied = PlaybackRules.NORMAL_SPEED

    /** Raised by every change of file, so a preparation that is out of date stops. zh-CN: 每次换文件时增加, 使过期的准备工作停止. */
    @Volatile
    private var generation = 0

    var state by mutableStateOf(State.CLOSED)
        private set
    var playing by mutableStateOf(false)
        private set
    var positionMillis by mutableLongStateOf(0L)
        private set
    var durationMillis by mutableLongStateOf(0L)
        private set
    var speed by mutableFloatStateOf(PlaybackRules.NORMAL_SPEED)
        private set

    /** Whether the sound comes from the temporary WAV. zh-CN: 声音是否来自临时 WAV. */
    var fromDecoded = false
        private set

    /** Shows the result [key] plays from [source]; the same key again keeps everything. zh-CN: 显示由 [source] 播放的结果 [key]; 相同的 key 不做改变. */
    fun open(key: Any, source: FileSource?) {
        if (owner === key) return
        close()
        owner = key
        this.source = source
        if (source == null) state = State.UNAVAILABLE else prepare(source, 0)
    }

    /** No result to play: gives everything up. zh-CN: 没有可播放的结果: 全部放弃. */
    fun close() {
        release()
        owner = null
        source = null
        positionMillis = 0
        durationMillis = 0
        state = State.CLOSED
    }

    /** The screen stopped: the player and its temporary files go, the position stays. zh-CN: 页面已停止: 放弃播放器与临时文件, 保留位置. */
    fun release() {
        generation += 1
        pause()
        main.removeCallbacks(ticker)
        player?.let { current -> runCatching { current.stop() }; runCatching { current.release() } }
        player = null
        applied = PlaybackRules.NORMAL_SPEED
        fromDecoded = false
        worker.execute(::deleteTemporary)
    }

    /** The screen came back: prepares the file again at the kept position. zh-CN: 页面回来了: 在保留的位置重新准备文件. */
    fun restore() {
        val current = source ?: return
        if (player == null && state != State.UNAVAILABLE) prepare(current, positionMillis)
    }

    fun toggle() = if (playing) pause() else play()

    fun play() {
        val current = player ?: return
        onStart()
        if (durationMillis - positionMillis < END_MILLIS) seekTo(0)
        try {
            // Non-zero params start a prepared player, so a paused one takes them here. zh-CN: 非零参数会启动已准备的播放器, 所以暂停时在此才设置.
            applySpeed(current)
            current.start()
            playing = true
            main.removeCallbacks(ticker)
            main.post(ticker)
        } catch (e: RuntimeException) {
            fail(e)
        }
    }

    fun pause() {
        val current = player
        if (playing && current != null) {
            runCatching { current.pause() }
            sync(current)
        }
        playing = false
    }

    fun seekTo(millis: Long) {
        val current = player ?: return
        val target = millis.coerceIn(0, durationMillis)
        positionMillis = target
        seekingUntil = SystemClock.uptimeMillis() + SEEK_GRACE_MILLIS
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) current.seekTo(target, MediaPlayer.SEEK_CLOSEST) else current.seekTo(target.toInt())
        } catch (e: RuntimeException) {
            fail(e)
        }
    }

    fun skip(deltaMillis: Long) = seekTo(positionMillis + deltaMillis)

    /** Plays on from [millis]: a tap on a line. zh-CN: 从 [millis] 继续播放: 点击某一行. */
    fun jump(millis: Long) {
        seekTo(millis)
        play()
    }

    fun changeSpeed(value: Float) {
        speed = value
        val current = player ?: return
        if (playing) applySpeed(current)
    }

    private fun applySpeed(current: MediaPlayer) {
        if (speed == applied) return
        try {
            current.playbackParams = current.playbackParams.setSpeed(speed)
            applied = speed
        } catch (e: RuntimeException) {
            // Some decoders cannot change the speed: the sound goes on as it was. zh-CN: 部分解码器不能变速: 声音照旧继续.
            Log.w(TAG, "The speed cannot change: ${e.javaClass.simpleName}")
            speed = applied
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            val current = player
            if (current == null || !playing) return
            if (SystemClock.uptimeMillis() >= seekingUntil) sync(current)
            main.postDelayed(this, TICK_MILLIS)
        }
    }

    private fun sync(current: MediaPlayer) {
        runCatching { current.currentPosition.toLong() }.onSuccess { positionMillis = it.coerceIn(0, durationMillis) }
    }

    private fun fail(error: Exception) {
        Log.w(TAG, "Playback failed: ${error.javaClass.simpleName}")
        release()
        state = State.UNAVAILABLE
    }

    private fun prepare(source: FileSource, position: Long) {
        val serial = ++generation
        state = State.PREPARING
        worker.execute {
            deleteTemporary()
            val prepared = try {
                load(source, serial)
            } catch (e: Exception) {
                Log.w(TAG, "The file cannot be played: ${e.javaClass.simpleName}")
                null
            }
            main.post { prepared(serial, prepared, position) }
        }
    }

    /** One opened file on the main thread. zh-CN: 主线程上已打开的文件. */
    private fun prepared(serial: Int, opened: Opened?, position: Long) {
        if (serial != generation) {
            opened?.player?.release()
            return
        }
        if (opened == null) {
            state = State.UNAVAILABLE
            return
        }
        val current = opened.player
        current.setOnCompletionListener {
            playing = false
            positionMillis = durationMillis
        }
        current.setOnSeekCompleteListener { seekingUntil = 0 }
        current.setOnErrorListener { _, what, _ ->
            Log.w(TAG, "The player failed: $what")
            release()
            state = State.UNAVAILABLE
            true
        }
        player = current
        fromDecoded = opened.decoded
        durationMillis = current.duration.toLong().coerceAtLeast(0)
        state = State.READY
        if (position > 0) seekTo(position)
    }

    private class Opened(val player: MediaPlayer, val decoded: Boolean)

    private fun load(source: FileSource, serial: Int): Opened? {
        decoded.parentFile?.mkdirs()
        val descriptor = source.open().let { if (seekable(it)) it else copy(it, serial) }
        return descriptor.use { opened ->
            val direct = if (forceDecoding) null else player(opened)
            when {
                direct != null -> Opened(direct, false)
                serial != generation -> null
                else -> {
                    decode(opened, serial)
                    ParcelFileDescriptor.open(decoded, ParcelFileDescriptor.MODE_READ_ONLY).use { wav -> player(wav)?.let { Opened(it, true) } }
                }
            }
        }
    }

    private fun seekable(descriptor: ParcelFileDescriptor): Boolean = try {
        Os.lseek(descriptor.fileDescriptor, 0, OsConstants.SEEK_CUR)
        true
    } catch (_: ErrnoException) {
        false
    }

    /** The system player cannot read a pipe; a copy within the 200 MB limit of D110 can. zh-CN: 系统播放器读不了管道; 在 D110 的 200 MB 限制内复制一份. */
    private fun copy(descriptor: ParcelFileDescriptor, serial: Int): ParcelFileDescriptor {
        var total = 0L
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
            FileOutputStream(copied).use { output ->
                val buffer = ByteArray(COPY_BUFFER_BYTES)
                while (true) {
                    if (serial != generation) throw IOException("Replaced while copying")
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > SpeechLimits.MAX_AUDIO_FILE_BYTES) throw IOException("The file is too large")
                    output.write(buffer, 0, read)
                }
            }
        }
        return ParcelFileDescriptor.open(copied, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    /** The prepared system player of [descriptor], or null when it cannot read the file. zh-CN: [descriptor] 已准备好的系统播放器; 读不了时为 null. */
    private fun player(descriptor: ParcelFileDescriptor): MediaPlayer? {
        val created = MediaPlayer()
        try {
            created.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            created.setDataSource(descriptor.fileDescriptor)
            created.prepare()
            return created
        } catch (e: IOException) {
            Log.i(TAG, "The system player refused the file: ${e.javaClass.simpleName}")
        } catch (e: RuntimeException) {
            Log.i(TAG, "The system player refused the file: ${e.javaClass.simpleName}")
        }
        created.release()
        return null
    }

    /** The decoded mono samples as a WAV at the file's own rate (D86, D171). zh-CN: 把解码后的单声道采样按文件本身的采样率写成 WAV (D86, D171). */
    private fun decode(descriptor: ParcelFileDescriptor, serial: Int) {
        AudioFiles.open(descriptor).use { audio ->
            FileOutputStream(decoded).use { output ->
                val writer = WavWriter(output.channel, audio.sampleRate, Long.MAX_VALUE)
                while (true) {
                    if (serial != generation) throw IOException("Replaced while decoding")
                    val samples = audio.read(DECODE_FRAMES)
                    if (samples.isEmpty()) break
                    writer.write(samples)
                }
                writer.finish()
            }
        }
    }

    private fun deleteTemporary() {
        decoded.delete()
        copied.delete()
    }

    companion object {
        private const val TAG = "MacawFilePlayer"
        const val DECODED_NAME = "playback.wav"
        const val COPY_NAME = "playback-input"
        private const val TICK_MILLIS = 100L
        private const val SEEK_GRACE_MILLIS = 1000L

        /** Closer to the end than this, "play" starts over. zh-CN: 距结尾不足此值时, "播放" 从头开始. */
        private const val END_MILLIS = 100L
        private const val COPY_BUFFER_BYTES = 64 * 1024
        private const val DECODE_FRAMES = 16 * 1024

        private val worker = Executors.newSingleThreadExecutor { Thread(it, "MacawFilePlayer").apply { isDaemon = true } }

        /** Device tests: take the path of the plugin's own decoders on every Android version. zh-CN: 设备测试用: 在任何 Android 版本上都走插件自带解码路径. */
        @VisibleForTesting
        @Volatile
        var forceDecoding = false
    }
}
