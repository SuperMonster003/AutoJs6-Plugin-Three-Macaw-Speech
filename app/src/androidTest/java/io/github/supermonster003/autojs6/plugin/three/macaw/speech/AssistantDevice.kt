package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Shared pieces of the assistant, recognition service and takeover device tests (roadmap P5.7 E3): the sound source
 * (the [VirtualRoom] with `-e macawRoom virtual` on emulators, the speaker on phones), the broadcasts of the test
 * targets, a wake session of a test client, the Compose lookups and the screenshots.
 * zh-CN: 助理, 识别服务与接管设备测试的共用部分 (路线图 P5.7 E3): 声源 (模拟器上以 `-e macawRoom virtual` 使用 [VirtualRoom], 手机上用扬声器), 测试目标
 * 的广播, 测试客户端的唤醒会话, Compose 查找与截图.
 */
internal object AssistantDevice {
    const val PLUGIN = VoiceCommandTarget.PLUGIN
    const val ACTION_RECEIVED = VoiceCommandTarget.ACTION_RECEIVED
    const val LABEL_FIRST = "Macaw Test Target A"
    const val LABEL_SECOND = "Macaw Test Target B"

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val context: Context get() = instrumentation.targetContext

    val virtual: Boolean get() = SpeechDevice.argument("macawRoom") == "virtual"

    fun setUpRoom() {
        if (virtual) VirtualRoom.install()
    }

    fun tearDownRoom() {
        if (virtual) VirtualRoom.uninstall()
    }

    /** Plays an androidTest recording in the room and waits until it ended (plus [tail] ms). zh-CN: 播放测试录音并等待结束. */
    fun play(asset: String, tail: Long = 300) {
        if (virtual) {
            Thread.sleep(VirtualRoom.play(SpeechDevice.samples(asset)) + tail)
            return
        }
        val file = File(context.cacheDir, "assistant-${asset.substringAfterLast('/')}")
        instrumentation.context.assets.open(asset).use { input -> file.outputStream().use { input.copyTo(it) } }
        val done = CountDownLatch(1)
        val player = MediaPlayer()
        instrumentation.runOnMainSync {
            player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener { done.countDown() }
            player.prepare()
            player.start()
        }
        try {
            check(done.await(120, TimeUnit.SECONDS)) { "playback did not end" }
            Thread.sleep(tail)
        } finally {
            instrumentation.runOnMainSync { player.release() }
        }
    }

    /** Starts playing without waiting (the virtual room only needs the start). zh-CN: 开始播放而不等待. */
    fun playInBackground(asset: String): Thread = Thread { play(asset, tail = 0) }.apply { start() }

    fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes().toString(Charsets.UTF_8) }

    /** What the test targets received, in order. zh-CN: 测试目标按顺序收到的内容. */
    class Targets : AutoCloseable {
        val received = LinkedBlockingQueue<Bundle>()
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                received += intent.extras ?: Bundle()
            }
        }

        init {
            ContextCompat.registerReceiver(context, receiver, IntentFilter(ACTION_RECEIVED), ContextCompat.RECEIVER_EXPORTED)
        }

        fun next(seconds: Long = 15): Bundle? = received.poll(seconds, TimeUnit.SECONDS)

        override fun close() {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    /** A wake session of a test client, as a resident listener would hold it (roadmap D57). zh-CN: 测试客户端的唤醒会话, 如同常驻监听持有的那样. */
    class Wake(keyword: String) : AutoCloseable {
        val hits: MutableList<String> = Collections.synchronizedList(ArrayList())
        val events: MutableList<String> = Collections.synchronizedList(ArrayList())
        private val ready = CountDownLatch(1)
        private val endLatch = CountDownLatch(1)
        val session: ISpeechSession

        init {
            val options = SpeechDevice.request {
                putParcelableArrayList(SpeechOptionKeys.KEYWORDS, arrayListOf(Bundle().apply { putString(SpeechOptionKeys.KEYWORD, keyword) }))
                putString(SpeechOptionKeys.MODEL, SpeechDevice.KWS)
            }
            session = SpeechDevice.plugin.openWakeSession(options, object : ISpeechWakeCallback.Stub() {
                override fun onReady(event: Bundle?) {
                    events += "ready"
                    ready.countDown()
                }

                override fun onWake(event: Bundle?) {
                    events += "wake"
                    hits += event?.getString(SpeechResultKeys.KEYWORD).orEmpty()
                }

                override fun onWarning(event: Bundle?) {
                    events += "warning"
                }

                override fun onError(event: Bundle?) {
                    events += "error"
                }

                override fun onEnd(event: Bundle?) {
                    events += "end"
                    endLatch.countDown()
                }
            }, null)
            check(ready.await(60, TimeUnit.SECONDS)) { "the wake session did not become ready" }
        }

        val ended: Boolean get() = endLatch.count == 0L

        override fun close() {
            runCatching { session.cancel() }
            endLatch.await(15, TimeUnit.SECONDS)
        }
    }

    /** A script dictation holding the microphone. zh-CN: 占用麦克风的脚本听写. */
    fun dictation(ended: CountDownLatch): ISpeechSession = SpeechDevice.plugin.openSession(Bundle().apply { putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0) }, object : ISpeechSessionCallback.Stub() {
        override fun onReady(event: Bundle?) = Unit
        override fun onPartial(event: Bundle?) = Unit
        override fun onFinal(event: Bundle?) = Unit
        override fun onEndpoint() = Unit
        override fun onVoiceActivity(event: Bundle?) = Unit
        override fun onWarning(event: Bundle?) = Unit
        override fun onError(event: Bundle?) = Unit
        override fun onEnd(event: Bundle?) = ended.countDown()
    })

    /** A script session can open, so every entry released the microphone. zh-CN: 脚本会话能够打开, 说明各入口都已释放麦克风. */
    fun microphoneIsFree(): Boolean {
        val ended = CountDownLatch(1)
        val deadline = System.currentTimeMillis() + 5_000
        var session: ISpeechSession? = null
        while (session == null && System.currentTimeMillis() < deadline) {
            session = runCatching { dictation(ended) }.getOrNull()
            if (session == null) Thread.sleep(200)
        }
        session ?: return false
        session.cancel()
        return ended.await(15, TimeUnit.SECONDS)
    }

    /**
     * The "listening" and "hearing" texts as the sheet on the screen shows them: the sheet follows the language of
     * AutoJs6 or of the plugin's settings, which may differ from the device language of the test context.
     * zh-CN: 屏幕上弹层所显示的 "正在收音" 与 "正在听" 文字: 弹层跟随 AutoJs6 或插件设置的语言, 可能与测试 Context 的设备语言不同.
     */
    fun listeningTexts(): Set<String> {
        val sheet = resumed().firstOrNull { it.javaClass.name.endsWith(".AssistantActivity") || it.javaClass.name.endsWith(".VoiceInputActivity") } ?: context
        return setOf(sheet.getString(R.string.voice_input_listening), sheet.getString(R.string.voice_input_hearing))
    }

    fun resumed(): List<Activity> {
        var activities: List<Activity> = emptyList()
        instrumentation.runOnMainSync { activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList() }
        return activities
    }

    fun awaitGone(className: String, millis: Long = 10_000): Boolean {
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            if (resumed().none { it.javaClass.name == className }) return true
            Thread.sleep(100)
        }
        return false
    }

    private fun ComposeTestRule.nodes(tag: String) = onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()

    fun ComposeTestRule.exists(tag: String): Boolean = runCatching { nodes(tag).isNotEmpty() }.getOrDefault(false)

    fun ComposeTestRule.text(tag: String): String? = runCatching { nodes(tag).firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } }.getOrNull()

    fun ComposeTestRule.editable(tag: String): String? = runCatching { nodes(tag).firstOrNull()?.config?.getOrNull(SemanticsProperties.EditableText)?.text }.getOrNull()

    fun capture(prefix: String, name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val folder = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }
        val file = File(folder, "$prefix-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        SpeechDevice.log("screen ${file.name} ${bitmap.width}x${bitmap.height}")
    }
}
