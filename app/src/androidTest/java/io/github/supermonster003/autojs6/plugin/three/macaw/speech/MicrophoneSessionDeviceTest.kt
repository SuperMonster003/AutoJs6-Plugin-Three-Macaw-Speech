package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.MicrophonePermissionActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.MicrophoneService
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Microphone sessions on a device (roadmap P3.1): without `RECORD_AUDIO` both session kinds fail with
 * `PERMISSION_DENIED` naming the permission screen; with it a microphone session gets `ready`, holds the microphone
 * foreground service, keeps every other microphone session out (`SESSION_BUSY`) while external sessions stay possible
 * up to their limit, and releases the recorder within one second of its end. The permission itself is granted by the
 * maintainer in the system dialog of [MicrophonePermissionActivity]; the test never grants it.
 * zh-CN: 设备上的麦克风会话 (路线图 P3.1): 没有 `RECORD_AUDIO` 时两种会话都以指明授权界面的 `PERMISSION_DENIED` 失败; 有权限时麦克风会话得到
 * `ready`, 持有麦克风前台服务, 阻止其他麦克风会话 (`SESSION_BUSY`) 而外部会话仍可开到上限, 并在结束后一秒内释放录音器. 权限本身由维护者在
 * [MicrophonePermissionActivity] 的系统对话框中授予; 测试从不自行授权.
 */
@RunWith(AndroidJUnit4::class)
class MicrophoneSessionDeviceTest {

    private val context get() = SpeechDevice.context

    private val granted get() = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @Before
    fun model() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(SpeechDevice.STREAMING))
    }

    private val wakeCallback = object : ISpeechWakeCallback.Stub() {
        override fun onReady(info: Bundle?) = Unit
        override fun onWake(hit: Bundle?) = Unit
        override fun onWarning(warning: Bundle?) = Unit
        override fun onError(error: Bundle?) = Unit
        override fun onEnd(end: Bundle?) = Unit
    }

    private fun microphone(build: Bundle.() -> Unit = {}) = SpeechDevice.request {
        putString(SpeechOptionKeys.MODEL, SpeechDevice.STREAMING)
        putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0)
        build()
    }

    private fun wakeOptions() = SpeechDevice.request {
        putParcelableArrayList(SpeechOptionKeys.KEYWORDS, arrayListOf(Bundle().apply { putString(SpeechOptionKeys.KEYWORD, "小六小六") }))
    }

    private fun expect(code: String, block: () -> Unit): String {
        try {
            block()
        } catch (e: RuntimeException) {
            val message = e.message.orEmpty()
            assertTrue("expected $code, got $message", message.startsWith(code))
            return message
        }
        fail("expected $code")
        throw AssertionError()
    }

    @Test
    fun withoutThePermissionSessionsAreRefused() {
        assumeTrue("RECORD_AUDIO is granted on this device", !granted)
        val message = expect(SpeechErrorCodes.PERMISSION_DENIED) { SpeechDevice.plugin.openSession(microphone(), SpeechDevice.Events()) }
        assertTrue(message, message.contains(MicrophonePermissionActivity::class.java.name))
        if (SpeechDevice.installed(SpeechDevice.KWS)) expect(SpeechErrorCodes.PERMISSION_DENIED) { SpeechDevice.plugin.openWakeSession(wakeOptions(), wakeCallback, null) }
        // External sessions need no microphone.
        val external = SpeechDevice.Events()
        val session = SpeechDevice.plugin.openSession(microphone { putString(SpeechOptionKeys.SOURCE, SpeechContract.SOURCE_EXTERNAL) }, external)
        session.cancel()
        assertTrue(external.awaitEnd(60))
        SpeechDevice.log("microphone permission=denied refused=ok")
    }

    @Test
    fun oneMicrophoneSessionAtATimeAndTheRecorderIsReleased() {
        assumeTrue("RECORD_AUDIO is not granted; the maintainer grants it in MicrophonePermissionActivity", granted)
        val events = SpeechDevice.Events()
        val opened = SystemClock.elapsedRealtime()
        val session = SpeechDevice.plugin.openSession(microphone(), events)
        assertTrue("no ready", events.ready.await(120, TimeUnit.SECONDS))
        val readyMillis = SystemClock.elapsedRealtime() - opened
        SystemClock.sleep(1500)
        val serviceState = MicrophoneService.state
        val audio = context.getSystemService(AudioManager::class.java)
        val recording = audio.activeRecordingConfigurations.size
        assertTrue("the plugin records", recording >= 1)

        expect(SpeechErrorCodes.SESSION_BUSY) { SpeechDevice.plugin.openSession(microphone(), SpeechDevice.Events()) }
        if (SpeechDevice.installed(SpeechDevice.KWS)) expect(SpeechErrorCodes.SESSION_BUSY) { SpeechDevice.plugin.openWakeSession(wakeOptions(), wakeCallback, null) }
        val externals = (1..SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN).map {
            SpeechDevice.Events().let { callback -> callback to SpeechDevice.plugin.openSession(microphone { putString(SpeechOptionKeys.SOURCE, SpeechContract.SOURCE_EXTERNAL) }, callback) }
        }
        expect(SpeechErrorCodes.SESSION_BUSY) { SpeechDevice.plugin.openSession(microphone { putString(SpeechOptionKeys.SOURCE, SpeechContract.SOURCE_EXTERNAL) }, SpeechDevice.Events()) }
        externals.forEach { (_, external) -> external.cancel() }
        externals.forEach { (callback, _) -> assertTrue(callback.awaitEnd(60)) }

        val stopped = SystemClock.elapsedRealtime()
        session.finish()
        assertTrue("no end", events.awaitEnd(30))
        var released = -1L
        while (SystemClock.elapsedRealtime() - stopped < 5000) {
            if (audio.activeRecordingConfigurations.isEmpty()) {
                released = SystemClock.elapsedRealtime() - stopped
                break
            }
            SystemClock.sleep(20)
        }
        assertTrue("the recorder was not released ($released ms)", released in 0..1000)
        assertEquals(SpeechContract.REASON_STOPPED, events.end()?.getString(SpeechResultKeys.REASON))
        assertEquals("ready", events.types().first())
        assertFalse(session.isActive)
        SystemClock.sleep(500)
        SpeechDevice.log(
            "microphone permission=granted readyMs=$readyMillis recordingClients=$recording foregroundService=$serviceState " +
                "afterEnd=${MicrophoneService.state} releasedMs=$released events=${events.types().distinct().joinToString(",")}",
        )
        // A second session may open once the first ended.
        val again = SpeechDevice.Events()
        val next = SpeechDevice.plugin.openSession(microphone(), again)
        assertTrue(again.ready.await(60, TimeUnit.SECONDS))
        next.cancel()
        assertTrue(again.awaitEnd(30))
        assertEquals(SpeechContract.REASON_CANCELLED, again.end()?.getString(SpeechResultKeys.REASON))
    }
}
