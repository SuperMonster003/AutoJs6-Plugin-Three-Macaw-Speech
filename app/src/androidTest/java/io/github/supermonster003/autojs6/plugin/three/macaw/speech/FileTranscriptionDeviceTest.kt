package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.SpeechDevice.STREAMING
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageSegment
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagLabel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagScore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAnalysis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileProblem
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PlaybackRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.FileRecognitionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FOLLOW_HOLD_MILLIS
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileDescription
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FilePlayback
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileRun
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileRunOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileTranscription
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutController
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * The file tab of the tryout screen on a device (roadmap D110 - D116, P7.8.4): the system picker is the maintainer's
 * step, so the tests hand files to [FileTranscription] the way the picker result does. A missing model fails with
 * `MODEL_MISSING` and the cached JSON equals the shown record; the foreground service runs as `mediaProcessing` on
 * Android 15+, `dataSync` on 10 - 14 and without a type before, survives leaving the screen and leaves a result
 * notification that names no file; MP3 and Ogg Opus give the text of `ISpeechPlugin.recognizeFile` with the same
 * options while the progress only grows up to the declared length; a pipe is copied to the cache, gives the same text
 * and the copy is deleted; a cancel keeps the segments so far; the switch turns the automatic punctuation on and off;
 * the exports reach a document unchanged; the tab shows segments, actions, the localized hint with the English
 * reason, and no result notification while it is visible; the player of D164 plays, seeks, skips and changes speed, plays
 * what only the plugin decodes from a temporary WAV and a pipe from a copy, takes turns with the synthesis, and the five
 * results follow it, with a tap on a row, the lyrics view, the record read back and the pause of the page's scrolling
 * after a manual scroll. The player cases need no model and play through the speaker. The model resolves before the file is decoded, so the
 * recognition cases need the streaming model of [SpeechModelSetupDeviceTest] (skipped otherwise) and the punctuation
 * case also the models of [PunctuationDeviceTest]. Notifications are checked only where the maintainer allowed them.
 * Results are `MacawDeviceRun` lines; screenshots go to the external files folder `screens/`.
 * zh-CN: 设备上的试听试说页文件标签 (路线图 D110 - D116, P7.8.4): 系统文件选择器由维护者操作, 测试按选择结果的方式把文件交给 [FileTranscription].
 * 缺少模型时以 `MODEL_MISSING` 失败, 缓存的 JSON 与显示的记录相同; 前台服务在 Android 15+ 为 `mediaProcessing`, 10 - 14 为 `dataSync`, 更早无类型,
 * 离开页面后继续, 结束时留下不含文件名的结果通知; MP3 与 Ogg Opus 的文字与相同选项下 `ISpeechPlugin.recognizeFile` 的一致, 进度只增不减并到达
 * 声明的时长; 管道先复制到缓存, 文字相同, 副本随后删除; 取消保留已有分段; 开关控制自动标点; 导出的内容原样写入文档; 标签显示分段, 操作按钮,
 * 本地化提示与英文原因, 页面可见时不发结果通知; D164 的播放器能播放, 定位, 快进退与变速, 只有插件能解码的文件播放临时 WAV, 管道播放副本, 与合成互斥, 五种结果
 * 随之同步, 含点击行, 歌词式视图, 读回的记录以及手动滚动后暂停页面滚动. 播放器用例不需要模型, 经扬声器发声. 模型先于解码解析, 因此识别用例需要 [SpeechModelSetupDeviceTest] 安装的流式模型 (否则跳过),
 * 标点用例还需要 [PunctuationDeviceTest] 的模型. 只在维护者允许通知时检查通知. 结果为 `MacawDeviceRun` 日志行, 截图保存到外部文件目录 `screens/`.
 */
@RunWith(AndroidJUnit4::class)
class FileTranscriptionDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val transcription = FileTranscription.get(context)
    private val cached = File(context.cacheDir, "tryout/${FileTranscription.RESULT_NAME}")
    private val input = File(context.cacheDir, "tryout/${FileTranscription.INPUT_NAME}")
    private val notifications: Boolean get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    @Before
    fun idle() {
        // A run left by a failed case. zh-CN: 失败用例留下的识别.
        main { transcription.cancel() }
        await("no run in progress", 120_000) { main { transcription.run } == null }
        main { transcription.clear() }
        await("the cache is empty", 5_000) { !cached.exists() }
        FileRecognitionService.clearResult(context)
        SpeechPreferences(context).fileLyrics = false
    }

    @Test
    fun aMissingModelFailsAndTheCacheHoldsTheRecord() {
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val record = recognize(FileSource.of(file), options(MISSING))
        assertEquals(FileTranscriptRecord.State.FAILED, record.state)
        assertEquals(SpeechErrorCodes.MODEL_MISSING, record.failure?.code)
        assertEquals(FileProblem.MODEL_MISSING, record.failure?.problem)
        assertEquals("en.mp3", record.displayName)
        assertEquals(file.length(), record.sizeBytes)
        assertTrue(record.segments.isEmpty())
        assertEquals("the cache holds the shown record", record, cachedRecord())
        assertFalse("no copy is left", input.exists())
        SpeechDevice.log("file missingModel state=${record.state} code=${record.failure?.code} cachedEqual=true")
    }

    @Test
    fun theServiceTypeFollowsTheAndroidVersionAndNoNotificationNamesTheFile() {
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val gate = CountDownLatch(1)
        // Holds the run in PREPARING until the screen is gone. zh-CN: 让识别停在 PREPARING, 直到页面关闭.
        val source = object : FileSource {
            override fun describe() = FileDescription(SECRET_NAME, file.length())

            override fun open(): ParcelFileDescriptor {
                gate.await(60, TimeUnit.SECONDS)
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }
        val expected = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else -> 0
        }
        val shown = notifications
        var type: Int? = null
        var ongoing: Notification? = null
        try {
            ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
                // The run starts from the visible screen, as after the picker. zh-CN: 与选择文件后一样, 从可见的页面开始识别.
                assertTrue(main { transcription.start(source, options(MISSING)) })
                assertFalse("one file at a time", main { transcription.start(source, options(MISSING)) })
                await("the foreground service runs", 10_000) { main { FileRecognitionService.runningType } != null }
                type = main { FileRecognitionService.runningType }
                assertEquals(expected, type)
                if (shown) {
                    await("the ongoing notification", 5_000) { active(FileRecognitionService.RUNNING_ID) != null }
                    ongoing = active(FileRecognitionService.RUNNING_ID)
                }
            }
        } finally {
            gate.countDown()
        }
        // The screen is gone; the run goes on. zh-CN: 页面已关闭, 识别继续.
        val record = finished()
        assertEquals(FileTranscriptRecord.State.FAILED, record.state)
        assertEquals(SECRET_NAME, record.displayName)
        await("the service stops", 10_000) { main { FileRecognitionService.runningType } == null }
        var result: Notification? = null
        if (shown) {
            await("the result notification", 5_000) { active(FileRecognitionService.RESULT_ID) != null }
            result = active(FileRecognitionService.RESULT_ID)
            assertNull("the ongoing notification is gone", active(FileRecognitionService.RUNNING_ID))
            listOf(ongoing!!, result!!).forEach { notification ->
                assertFalse("no notification names the file", texts(notification).contains(SECRET))
                assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
            }
            assertTrue("the result can be dismissed", result.flags and Notification.FLAG_ONGOING_EVENT == 0)
        }
        FileRecognitionService.clearResult(context)
        SpeechDevice.log(
            "file service sdk=${Build.VERSION.SDK_INT} type=$type expected=$expected notifications=$shown " +
                "ongoingActions=${ongoing?.actions?.size} resultShown=${result != null}",
        )
    }

    @Test
    fun encodedFilesAndPipesGiveTheTextOfTheScriptApi() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING))
        listOf("en.mp3", "en.opus").forEach { name ->
            val file = SpeechDevice.asset("encoded-audio/$name")
            val samples = ArrayList<FileRun>()
            val record = recognize(FileSource.of(file), options(STREAMING), samples)
            assertEquals("$name: ${record.failure}", FileTranscriptRecord.State.COMPLETED, record.state)
            assertTrue("$name: segments", record.segments.isNotEmpty())
            assertEquals("$name: the text of recognizeFile", script(file), record.text)
            val progress = samples.map { it.recognizedMillis }
            assertEquals("$name: the progress never goes back", progress.sorted(), progress)
            assertTrue("$name: the run was seen recognizing", samples.any { it.phase == FileRun.Phase.RUNNING })
            val declared = record.declaredMillis
            assertNotNull("$name: the declared length", declared)
            assertTrue("$name: recognized ${record.recognizedMillis} of $declared", abs(record.recognizedMillis - declared!!) <= maxOf(500L, declared / 20))
            assertTrue("$name: the last progress ${progress.lastOrNull()} is within the record", (progress.lastOrNull() ?: 0) <= record.recognizedMillis)
            SpeechDevice.log(
                "file encoded name=$name segments=${record.segments.size} declared=$declared recognized=${record.recognizedMillis} " +
                    "samples=${samples.size} cer=${"%.3f".format(SpeechDevice.cer(SpeechDevice.EN_REFERENCE, record.text))} sameAsScript=true text=${record.text}",
            )
        }
        // Some cloud providers return a pipe: it is copied to the cache first. zh-CN: 部分云盘返回管道: 先复制到缓存.
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val piped = recognize(pipe(file), options(STREAMING))
        assertEquals("${piped.failure}", FileTranscriptRecord.State.COMPLETED, piped.state)
        assertEquals("a pipe gives the text of the file", script(file), piped.text)
        assertNull(piped.displayName)
        assertNull(piped.sizeBytes)
        assertFalse("the copy is deleted", input.exists())

        // The exports reach a document unchanged (D116). zh-CN: 导出内容原样写入文档 (D116).
        val controller = main { TryoutController(context) }
        listOf("txt" to TranscriptExport.text(piped.segments), "srt" to TranscriptExport.srt(piped.segments)).forEach { (extension, bytes) ->
            val target = File(context.cacheDir, "device-test/file-tab-export.$extension")
            target.parentFile?.mkdirs()
            target.writeText("an older and longer content that the export must replace completely")
            val written = CountDownLatch(1)
            var ok = false
            main { controller.writeDocument(Uri.fromFile(target), bytes) { ok = it; written.countDown() } }
            assertTrue(written.await(10, TimeUnit.SECONDS))
            assertTrue("$extension written", ok)
            assertArrayEquals("$extension: the document holds the export", bytes, target.readBytes())
            target.delete()
        }
        val srt = String(TranscriptExport.srt(piped.segments))
        SpeechDevice.log("file pipe segments=${piped.segments.size} sameAsScript=true copyDeleted=true export=txt,srt srtCues=${srt.lines().count { it.contains(" --> ") }}")
    }

    @Test
    fun aCancelKeepsTheSegmentsSoFar() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING))
        // About 100 s: the clip twelve times with 1.5 s of silence after each. zh-CN: 约 100 秒: 片段重复 12 次, 每次后接 1.5 秒静音.
        val clip = SpeechDevice.samples("test-audio/en.wav")
        val round = clip.size + 24_000
        val long = FloatArray(round * 12).also { samples -> repeat(12) { clip.copyInto(samples, it * round) } }
        val file = File(context.cacheDir, "device-test/file-tab-long.wav")
        SpeechDevice.writeWav(file, long)
        try {
            assertTrue(main { transcription.start(FileSource.of(file), options(STREAMING)) })
            await("the first segment", 120_000) { main { transcription.run?.segments?.isNotEmpty() ?: true } }
            // One main thread task: a fast worker may finish the run between two of them. zh-CN: 放在同一个主线程任务里: 工作线程可能在两次任务之间就结束识别.
            assertEquals(FileRun.Phase.CANCELLING, main { transcription.cancel(); transcription.run?.phase })
            val record = finished()
            assertEquals(FileTranscriptRecord.State.CANCELLED, record.state)
            assertNull(record.failure)
            assertTrue("the segments so far", record.segments.isNotEmpty())
            val declared = record.declaredMillis ?: 0
            assertTrue("stopped early: ${record.recognizedMillis} of $declared", record.recognizedMillis < declared - 10_000)
            assertEquals("the cache holds the cancelled record", record, cachedRecord())
            SpeechDevice.log("file cancel segments=${record.segments.size} recognized=${record.recognizedMillis} declared=$declared")
        } finally {
            file.delete()
        }
    }

    @Test
    fun theSwitchTurnsTheAutomaticPunctuationOnAndOff() {
        assumeTrue(
            "install the models first (SpeechModelSetupDeviceTest, -e macawModels true -e macawModelIds ...)",
            (listOf(STREAMING) + PunctuationDeviceTest.MODELS).all(SpeechDevice::installed),
        )
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val on = recognize(FileSource.of(file), options(STREAMING, punctuation = true))
        val off = recognize(FileSource.of(file), options(STREAMING, punctuation = false))
        assertEquals(FileTranscriptRecord.State.COMPLETED, on.state)
        assertEquals(FileTranscriptRecord.State.COMPLETED, off.state)
        assertTrue(on.punctuation)
        assertFalse(off.punctuation)
        val marks = on.text.count { it in MARKS }
        assertTrue("punctuation on: marks", marks > 0)
        assertEquals("punctuation off: no marks", 0, off.text.count { it in MARKS })
        SpeechDevice.log("file punctuation marksOn=$marks marksOff=0 text=${on.text}")
    }

    @Test
    fun theTabShowsSegmentsActionsAndTheReasonOfAFailure() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING))
        val prefix = SpeechDevice.argument("macawPrefix") ?: "file"
        ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
            compose.waitUntil(15_000) { exists("macaw.tryout.file.pick") }
            assertTrue(exists("macaw.tryout.file.model"))
            assertTrue(exists("macaw.tryout.file.punctuation"))
            capture(prefix, "empty")
            val file = SpeechDevice.asset("encoded-audio/en.mp3")
            assertTrue(main { transcription.start(FileSource.of(file), options(STREAMING)) })
            val record = finished()
            assertEquals(FileTranscriptRecord.State.COMPLETED, record.state)
            compose.waitUntil(10_000) { exists("macaw.tryout.file.summary") }
            compose.waitForIdle()
            val shown = record.segments.count { it.text.isNotBlank() }
            assertTrue(exists("macaw.tryout.file.segment.0"))
            assertTrue(exists("macaw.tryout.file.segment.${shown - 1}"))
            assertFalse(exists("macaw.tryout.file.segment.$shown"))
            listOf("name", "saveText", "saveSrt", "copy", "clear").forEach { assertTrue(it, exists("macaw.tryout.file.$it")) }
            assertFalse("the run is over", exists("macaw.tryout.file.cancel"))
            compose.onNodeWithTag("macaw.tryout.file.saveSrt", useUnmergedTree = true).performScrollTo()
            capture(prefix, "result")
            assertNull("no result notification while the tab is visible", active(FileRecognitionService.RESULT_ID))

            compose.onNodeWithTag("macaw.tryout.file.clear", useUnmergedTree = true).performScrollTo().performClick()
            compose.waitUntil(5_000) { !exists("macaw.tryout.file.summary") }
            assertFalse(exists("macaw.tryout.file.segment.0"))
            assertTrue(exists("macaw.tryout.file.pick"))
            await("the cache is cleared", 5_000) { !cached.exists() }

            // A video without an audio track. zh-CN: 没有音轨的视频.
            val video = SpeechDevice.asset("encoded-audio/video-only.mp4")
            assertTrue(main { transcription.start(FileSource.of(video), options(STREAMING)) })
            val failed = finished()
            assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, failed.failure?.code)
            compose.waitUntil(10_000) { exists("macaw.tryout.file.error") }
            assertTrue(exists("macaw.tryout.file.errorHint"))
            val detail = text("macaw.tryout.file.errorDetail").orEmpty()
            assertTrue(detail, detail.startsWith("${SpeechErrorCodes.UNSUPPORTED_FORMAT}: "))
            assertFalse("no save for a failure", enabled("macaw.tryout.file.saveSrt"))
            compose.onNodeWithTag("macaw.tryout.file.errorDetail", useUnmergedTree = true).performScrollTo()
            capture(prefix, "error")
            SpeechDevice.log("file tab segments=$shown summary=true clear=true errorDetail=$detail")
        }
    }

    @Test
    fun theTabListsTheLanguageOfEachSentence() {
        assumeTrue("the language identification model is installed", SpeechDevice.installed(LANGID))
        ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
            compose.waitUntil(15_000) { exists("macaw.tryout.file.pick") }
            val file = SpeechDevice.asset("analysis-audio/language-mixed.wav")
            assertTrue(main { transcription.start(FileSource.of(file), FileRunOptions(null, null, false, true, FileAction.LANGUAGE, languageModel = LANGID)) })
            val record = finished()
            assertEquals(FileTranscriptRecord.State.COMPLETED, record.state)
            val sentences = (record.analysis as FileAnalysis.Languages).segments
            assertEquals(setOf("zh", "en", "ru"), sentences.map { it.language }.toSet())
            compose.waitUntil(10_000) { exists("macaw.tryout.file.languageResult") }
            compose.waitForIdle()
            sentences.indices.forEach { assertTrue("sentence $it", exists("macaw.tryout.file.sentence.$it")) }
            assertFalse(exists("macaw.tryout.file.sentence.${sentences.size}"))
            compose.onNodeWithTag("macaw.tryout.file.sentence.${sentences.lastIndex}", useUnmergedTree = true).performScrollTo()
            capture(SpeechDevice.argument("macawPrefix") ?: "file", "language")
            SpeechDevice.log("file tab language sentences=${sentences.size} languages=${sentences.joinToString(",") { it.language }}")
        }
    }

    // Player (roadmap D164, D169 - D172). The rows and lines need no model: the records are written to the cache as a
    // restarted process finds them. zh-CN: 播放器 (路线图 D164, D169 - D172). 各行不需要模型: 记录写入缓存, 与重启后的进程读到的相同.

    @Test
    fun thePlayerPlaysSeeksSkipsAndChangesSpeed() {
        val file = SpeechDevice.asset(DIALOGUE)
        var started = 0
        val playback = main { FilePlayback(context) { started += 1 } }
        try {
            ready(playback, FileSource.of(file))
            assertFalse("the system plays the file itself", playback.fromDecoded)
            val duration = main { playback.durationMillis }
            assertTrue("length $duration", abs(duration - DIALOGUE_MILLIS) < 1000)
            main { playback.play() }
            assertEquals("the synthesis player is told", 1, started)
            await("the position grows", 5_000) { position(playback) > 300 }
            main { playback.pause() }
            val paused = position(playback)
            Thread.sleep(500)
            assertEquals("a paused player stays", paused, position(playback))
            assertFalse(main { playback.playing })

            main { playback.seekTo(20_000) }
            assertEquals(20_000L, position(playback))
            main { playback.skip(PlaybackRules.SKIP_MILLIS) }
            assertEquals(25_000L, position(playback))
            main { playback.skip(-PlaybackRules.SKIP_MILLIS) }
            assertEquals(20_000L, position(playback))
            main { playback.seekTo(1000); playback.skip(-PlaybackRules.SKIP_MILLIS) }
            assertEquals("the start stops a skip", 0L, position(playback))
            main { playback.seekTo(duration - 1000); playback.skip(PlaybackRules.SKIP_MILLIS) }
            assertEquals("the end stops a skip", duration, position(playback))

            val rates = PlaybackRules.SPEEDS.associateWith { speed ->
                main { playback.seekTo(5_000); playback.changeSpeed(speed); playback.play() }
                Thread.sleep(600)
                val (fromPosition, fromTime) = main { playback.positionMillis to SystemClock.uptimeMillis() }
                Thread.sleep(2_000)
                val (toPosition, toTime) = main { playback.positionMillis to SystemClock.uptimeMillis() }
                main { playback.pause() }
                (toPosition - fromPosition).toFloat() / (toTime - fromTime)
            }
            rates.forEach { (speed, rate) -> assertTrue("speed $speed played at $rate", abs(rate - speed) <= speed * 0.2f) }

            // The speed set while paused waits for the next start. zh-CN: 暂停时设置的倍速等到下次开始才生效.
            main { playback.changeSpeed(2f) }
            Thread.sleep(300)
            assertFalse("no start while paused", main { playback.playing })
            val still = position(playback)
            Thread.sleep(300)
            assertEquals(still, position(playback))
            main { playback.changeSpeed(1f) }

            main { playback.seekTo(duration - 500); playback.play() }
            await("the end of the file", 5_000) { !main { playback.playing } }
            assertEquals("the end shows", duration, position(playback))
            main { playback.play() }
            assertTrue("play starts over", position(playback) < 1500)
            main { playback.pause() }
            SpeechDevice.log("file player duration=$duration rates=${rates.entries.joinToString(",") { "${it.key}=${"%.2f".format(it.value)}" }} seek=exact skip=clamped")
        } finally {
            main { playback.close() }
        }
    }

    @Test
    fun aFileOnlyThePluginDecodesPlaysFromATemporaryWavThatGoesWithTheScreen() {
        val file = SpeechDevice.asset("encoded-audio/en.opus")
        val wav = File(context.cacheDir, "tryout/${FilePlayback.DECODED_NAME}")
        val playback = main { FilePlayback(context) { } }
        AudioFiles.forceOwnOggOpus = true
        FilePlayback.forceDecoding = true
        try {
            ready(playback, FileSource.of(file), timeoutMillis = 60_000)
            assertTrue(main { playback.fromDecoded })
            assertTrue("the temporary WAV", wav.isFile)
            assertTrue("a length", main { playback.durationMillis } > 1000)
            main { playback.play() }
            await("the position grows", 5_000) { position(playback) > 300 }
            main { playback.seekTo(1500) }

            // Leaving the screen and coming back. zh-CN: 离开页面再回来.
            main { playback.release() }
            await("the WAV is deleted", 5_000) { !wav.exists() }
            assertEquals("the position stays", 1500L, position(playback))
            assertFalse(main { playback.playing })
            main { playback.restore() }
            await("ready again", 60_000) { main { playback.state } == FilePlayback.State.READY }
            assertEquals(1500L, position(playback))
            assertTrue(wav.isFile)
            main { playback.close() }
            await("the WAV is deleted", 5_000) { !wav.exists() }
            assertEquals(FilePlayback.State.CLOSED, main { playback.state })
            SpeechDevice.log("file player decoded=true sdk=${Build.VERSION.SDK_INT} restored=true wavDeleted=true")
        } finally {
            FilePlayback.forceDecoding = false
            AudioFiles.forceOwnOggOpus = false
            main { playback.close() }
        }
        // Without the switches: what this Android does with Ogg Opus by itself. zh-CN: 不用开关: 此版本 Android 自己如何处理 Ogg Opus.
        val plain = main { FilePlayback(context) { } }
        try {
            ready(plain, FileSource.of(file), timeoutMillis = 60_000)
            main { plain.play() }
            await("the position grows", 5_000) { position(plain) > 300 }
            SpeechDevice.log("file player oggOpus sdk=${Build.VERSION.SDK_INT} fromDecoded=${main { plain.fromDecoded }}")
        } finally {
            main { plain.close() }
        }
    }

    @Test
    fun aPipePlaysFromACopyAndAFileThatIsGoneIsUnavailable() {
        val file = SpeechDevice.asset(DIALOGUE)
        val copy = File(context.cacheDir, "tryout/${FilePlayback.COPY_NAME}")
        val playback = main { FilePlayback(context) { } }
        try {
            ready(playback, pipe(file), timeoutMillis = 30_000)
            assertFalse(main { playback.fromDecoded })
            assertTrue("the copy", copy.isFile)
            main { playback.close() }
            await("the copy is deleted", 5_000) { !copy.exists() }

            main { playback.open(Any(), FileSource.of(context, Uri.fromFile(File(context.cacheDir, "device-test/gone.wav")))) }
            await("a file that is gone", 10_000) { main { playback.state } == FilePlayback.State.UNAVAILABLE }
            main { playback.open(Any(), null) }
            assertEquals("a record without a source", FilePlayback.State.UNAVAILABLE, main { playback.state })
            main { playback.play() }
            assertFalse(main { playback.playing })
            SpeechDevice.log("file player pipe=copied gone=unavailable")
        } finally {
            main { playback.close() }
        }
    }

    @Test
    fun theFilePlayerAndTheSynthesisPlayerExcludeEachOther() {
        val file = SpeechDevice.asset(DIALOGUE)
        val controller = main { TryoutController(context) }
        val playback = controller.playback
        try {
            ready(playback, FileSource.of(file))
            main { playback.play() }
            assertTrue(main { playback.playing })
            main { controller.startPlayer(file) }
            assertTrue("the synthesis plays", main { controller.playing })
            assertFalse("the file stops", main { playback.playing })
            main { playback.play() }
            assertTrue("the file plays", main { playback.playing })
            assertFalse("the synthesis stops", main { controller.playing })
            SpeechDevice.log("file player exclusive=true")
        } finally {
            main { controller.release(); playback.close() }
        }
    }

    @Test
    fun theRowsFollowTheSoundAndATapPlaysOnFromThere() {
        val file = SpeechDevice.asset(DIALOGUE)
        show(transcribed(file))
        ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
            compose.waitUntil(15_000) { exists(TOGGLE) }
            assertEquals("00:42", text(DURATION))
            click(TOGGLE)
            compose.waitUntil(5_000) { selected("macaw.tryout.file.segment.0") }
            click(TOGGLE)
            // Silence between the first two turns: nothing is highlighted. zh-CN: 前两个轮次之间的静音: 没有高亮.
            seek(6_850f)
            compose.waitUntil(5_000) { !selected("macaw.tryout.file.segment.0") }
            assertFalse(selected("macaw.tryout.file.segment.1"))
            seek(7_300f)
            compose.waitUntil(5_000) { selected("macaw.tryout.file.segment.1") }
            assertEquals("00:07", text(POSITION))

            click("macaw.tryout.file.segment.3")
            compose.waitUntil(5_000) { selected("macaw.tryout.file.segment.3") }
            compose.waitUntil(5_000) { text(POSITION) == "00:22" }
            capture(SpeechDevice.argument("macawPrefix") ?: "file", "player")
            click("macaw.tryout.file.player.back")
            compose.waitUntil(5_000) { selected("macaw.tryout.file.segment.2") }
            click("macaw.tryout.file.player.forward")
            compose.waitUntil(5_000) { selected("macaw.tryout.file.segment.3") }
            click("macaw.tryout.file.player.speed")
            compose.waitUntil(5_000) { exists("macaw.tryout.file.player.speed.4") }
            click("macaw.tryout.file.player.speed.4")
            compose.waitUntil(5_000) { !exists("macaw.tryout.file.player.speed.4") }
            assertEquals("1.5x", text(SPEED))
            SpeechDevice.log("file tab player highlight=true gap=none tap=true skip=true speed=1.5x")
            // Other tabs must not hear it (D171). zh-CN: 其他标签不能听到它 (D171).
            click("macaw.tryout.tab.0")
            compose.waitUntil(5_000) { !exists(TOGGLE) }
            click("macaw.tryout.tab.3")
            compose.waitUntil(5_000) { exists(TOGGLE) }
            assertEquals("kept for the screen (D169)", "1.5x", text(SPEED))
            val paused = text(POSITION)
            Thread.sleep(2_500)
            assertEquals("switching tabs paused the sound", paused, text(POSITION))
        }
    }

    @Test
    fun everyResultFollowsAndTurnsIntoTheLyricsView() {
        val dialogue = SpeechDevice.asset(DIALOGUE)
        val mixed = SpeechDevice.asset(MIXED)
        val prefix = SpeechDevice.argument("macawPrefix") ?: "file"
        ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use { scenario ->
            var names = emptyList<String>()
            scenario.onActivity { activity ->
                val locale = activity.resources.configuration.locales[0]
                names = listOf(activity.getString(R.string.tryout_file_speaker, 1), activity.getString(R.string.tryout_file_speaker, 2)) + listOf("zh", "en", "ru").map { Locale.forLanguageTag(it).getDisplayName(locale) }
            }
            val (speaker1, speaker2, chinese, english, russian) = names
            data class Case(val name: String, val record: FileTranscriptRecord, val row: String, val at: Float, val current: String, val previous: String?, val next: String?)
            val cases = listOf(
                Case("transcribe", record(FileAction.TRANSCRIBE, dialogue, segments = TURNS.mapIndexed { index, turn -> Utterance("line $index", index, turn.startMillis, turn.endMillis) }), "segment.1", 8_000f, "line 1", "line 0", "line 2"),
                Case("speakersText", transcribed(dialogue), "segment.1", 8_000f, "line 1", "line 0", "line 2"),
                Case("speakers", record(FileAction.DIARIZE, dialogue, analysis = FileAnalysis.Speakers(TURNS, 2, "s")), "turn.1", 8_000f, speaker2, speaker1, speaker1),
                Case("language", record(FileAction.LANGUAGE, mixed, analysis = FileAnalysis.Languages("en", listOf("en" to 1f), SENTENCES)), "sentence.1", 6_000f, english, chinese, russian),
                Case(
                    "tags",
                    record(
                        FileAction.TAG, dialogue,
                        analysis = FileAnalysis.Tags(listOf(TagScore(TagLabel(0, "/m/a", "Speech"), 0.9f, 0, 10_000), TagScore(TagLabel(1, "/m/b", "Music"), 0.5f, 10_000, 20_000), TagScore(TagLabel(2, "/m/c", "Dog"), 0.4f, 20_000, 30_000))),
                    ),
                    "tag.1", 12_000f, "Music", "Speech", "Dog",
                ),
            )
            cases.forEach { case ->
                show(case.record)
                compose.waitUntil(15_000) { exists(TOGGLE) && enabled(LYRICS) }
                seek(case.at)
                compose.waitUntil(5_000) { selected("macaw.tryout.file.${case.row}") }
                if (case.name == "tags") assertTrue(text("macaw.tryout.file.tag.now").orEmpty().contains("Music"))
                capture(prefix, "list-${case.name}")

                click(LYRICS)
                compose.waitUntil(5_000) { exists("macaw.tryout.file.lyrics.current") }
                assertTrue("written for the next visit", SpeechPreferences(context).fileLyrics)
                assertFalse("the list is gone", exists("macaw.tryout.file.${case.row}"))
                assertTrue(case.name, text("macaw.tryout.file.lyrics.current").orEmpty().contains(case.current))
                assertTrue(case.name, text("macaw.tryout.file.lyrics.previous").orEmpty().contains(case.previous!!))
                assertTrue(case.name, text("macaw.tryout.file.lyrics.next").orEmpty().contains(case.next!!))
                capture(prefix, "lyrics-${case.name}")
                click("macaw.tryout.file.lyrics.next")
                compose.waitUntil(5_000) { text("macaw.tryout.file.lyrics.current").orEmpty().contains(case.next) && selected("macaw.tryout.file.lyrics.current") }
                click(TOGGLE)
                click(LYRICS)
                compose.waitUntil(5_000) { exists("macaw.tryout.file.${case.row}") }
                assertFalse(SpeechPreferences(context).fileLyrics)
                main { transcription.clear() }
                compose.waitUntil(5_000) { !exists(TOGGLE) }
                SpeechDevice.log("file tab result=${case.name} row=true lyrics=true")
            }
        }
    }

    @Test
    fun aRecordReadBackPlaysWhileItsFileOpensAndExplainsOtherwise() {
        val copy = File(context.cacheDir, "device-test/read-back.wav")
        copy.parentFile?.mkdirs()
        SpeechDevice.asset(DIALOGUE).copyTo(copy, overwrite = true)
        val record = transcribed(copy)
        try {
            show(record)
            ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
                compose.waitUntil(15_000) { exists(TOGGLE) }
                assertTrue(exists("macaw.tryout.file.segment.0"))
                copy.delete()
                show(record.copy(segments = record.segments.take(2), segmentSpeakers = record.segmentSpeakers.take(2)))
                compose.waitUntil(15_000) { exists(UNAVAILABLE) }
                assertFalse("no controls", exists(TOGGLE))
                assertTrue("the result stays", exists("macaw.tryout.file.segment.1"))
                assertFalse("not playable, not a target", enabled("macaw.tryout.file.segment.1"))
                capture(SpeechDevice.argument("macawPrefix") ?: "file", "unavailable")
                // A record of 1.3.0 names no file. zh-CN: 1.3.0 的记录没有写明文件.
                show(record.copy(sourceUri = null, segments = record.segments.take(3), segmentSpeakers = record.segmentSpeakers.take(3)))
                compose.waitUntil(15_000) { exists("macaw.tryout.file.segment.2") }
                compose.waitUntil(15_000) { exists(UNAVAILABLE) }
                SpeechDevice.log("file tab readBack playable=true gone=explained noSource=explained")
            }
        } finally {
            copy.delete()
        }
    }

    @Test
    fun thePageFollowsTheSoundUntilTheUserScrolls() {
        // A minute of silence with a row per second, far longer than the screen. zh-CN: 一分钟静音, 每秒一行, 远长于屏幕.
        val file = File(context.cacheDir, "device-test/file-tab-silence.wav")
        SpeechDevice.writeWav(file, FloatArray(16_000 * 60))
        val segments = List(60) { Utterance("line $it", it, it * 1000L, it * 1000L + 900) }
        show(record(FileAction.TRANSCRIBE, file, segments = segments))
        try {
            ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
                compose.waitUntil(15_000) { exists(TOGGLE) }
                click(TOGGLE)
                compose.waitUntil(40_000) { selected("macaw.tryout.file.segment.25") }
                compose.waitUntil(3_000) { sounding()?.let(::visible) == true }
                assertTrue("the page has scrolled", offset() > 0)
                capture(SpeechDevice.argument("macawPrefix") ?: "file", "follow")

                // A fling back to the start. zh-CN: 一次滑回开头的惯性滚动.
                compose.onNode(PAGE, useUnmergedTree = true).performTouchInput { swipeDown(startY = height * 0.4f, endY = bottom - 10f, durationMillis = 120) }
                val began = SystemClock.uptimeMillis()
                var last = offset()
                var moved = began
                while (SystemClock.uptimeMillis() - moved < 300) {
                    Thread.sleep(16)
                    val now = offset()
                    if (now != last) {
                        last = now
                        moved = SystemClock.uptimeMillis()
                    }
                }
                val fling = moved - began
                val rested = moved
                Thread.sleep(FOLLOW_HOLD_MILLIS - 1500 - (SystemClock.uptimeMillis() - rested).coerceAtLeast(0))
                assertEquals("the page stays where the user left it", last, offset())
                compose.waitUntil(FOLLOW_HOLD_MILLIS + 4_000) { sounding()?.let(::visible) == true }
                val back = SystemClock.uptimeMillis() - rested
                assertTrue("back after $back ms", back >= FOLLOW_HOLD_MILLIS - 200)
                SpeechDevice.log("file tab follow flingMillis=$fling backAfterMillis=$back holdMillis=$FOLLOW_HOLD_MILLIS")
            }
        } finally {
            file.delete()
        }
    }

    // Helpers ----------------------------------------------------------------------------------------------------

    private fun ready(playback: FilePlayback, source: FileSource, timeoutMillis: Long = 15_000) {
        main { playback.open(Any(), source) }
        await("the player is ready", timeoutMillis) { main { playback.state } == FilePlayback.State.READY }
    }

    private fun position(playback: FilePlayback): Long = main { playback.positionMillis }

    /** What a restarted process would show. zh-CN: 重启后的进程会显示的内容. */
    private fun show(record: FileTranscriptRecord) {
        cached.parentFile?.mkdirs()
        cached.writeBytes(record.toJson())
        main { transcription.readBack() }
    }

    private fun record(action: FileAction, file: File, segments: List<Utterance> = emptyList(), analysis: FileAnalysis? = null, speakers: List<Int> = emptyList()) = FileTranscriptRecord(
        state = FileTranscriptRecord.State.COMPLETED,
        displayName = file.name,
        sizeBytes = file.length(),
        modelId = "device-test",
        language = null,
        punctuation = false,
        declaredMillis = DIALOGUE_MILLIS,
        recognizedMillis = DIALOGUE_MILLIS,
        segments = segments,
        action = action,
        analysis = analysis,
        segmentSpeakers = speakers,
        sourceUri = Uri.fromFile(file).toString(),
    )

    private fun transcribed(file: File) = record(
        FileAction.DIARIZE_TRANSCRIBE, file,
        segments = TURNS.mapIndexed { index, turn -> Utterance("line $index", index, turn.startMillis, turn.endMillis) },
        analysis = FileAnalysis.Speakers(TURNS, 2, "device-test"),
        speakers = TURNS.map { it.speaker },
    )

    private fun click(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().performClick()
    }

    private fun seek(millis: Float) {
        compose.onNodeWithTag("macaw.tryout.file.player.seek", useUnmergedTree = true).performSemanticsAction(SemanticsActions.SetProgress) { it(millis) }
    }

    private fun selected(tag: String): Boolean = compose.nodes(tag).firstOrNull()?.config?.getOrNull(SemanticsProperties.Selected) == true

    private fun sounding() = compose.onAllNodes(SOUNDING, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()

    private fun visible(row: SemanticsNode): Boolean {
        val bar = compose.nodes("macaw.tryout.file.player").first().boundsInRoot
        return row.boundsInRoot.top >= bar.bottom - 1 && row.boundsInRoot.bottom <= compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
    }

    private fun offset(): Float = compose.onNode(PAGE, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun options(model: String, punctuation: Boolean = false) = FileRunOptions(model, null, punctuation, vad = true)

    /** Runs one file to its end, sampling the run every 50 ms. zh-CN: 识别一个文件直到结束, 每 50 毫秒采样一次. */
    private fun recognize(source: FileSource, options: FileRunOptions, samples: MutableList<FileRun>? = null): FileTranscriptRecord {
        assertTrue("a run starts", main { transcription.start(source, options) })
        return finished(samples)
    }

    private fun finished(samples: MutableList<FileRun>? = null): FileTranscriptRecord {
        var record: FileTranscriptRecord? = null
        await("the run ends", 300_000) {
            val (run, last) = main { transcription.run to transcription.record }
            run?.let { samples?.add(it) }
            record = last.takeIf { run == null }
            record != null
        }
        return record!!
    }

    /** The same options through the script API. zh-CN: 相同选项经脚本接口识别. */
    private fun script(file: File): String {
        val answer = SpeechDevice.recognize(file, SpeechDevice.request {
            putString(SpeechOptionKeys.MODEL, STREAMING)
            putBoolean(SpeechOptionKeys.PUNCTUATION, false)
            putBoolean(SpeechOptionKeys.VAD, true)
        })
        assertNull("${file.name}: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
        return answer.getString(SpeechResultKeys.TEXT).orEmpty()
    }

    /** A source that streams the file through a pipe and tells nothing about itself. zh-CN: 经管道传送文件且不提供自身信息的来源. */
    private fun pipe(file: File) = object : FileSource {
        override fun describe() = FileDescription(null, null)

        override fun open(): ParcelFileDescriptor {
            val (read, write) = ParcelFileDescriptor.createPipe()
            Thread {
                // A reader that stops early breaks the pipe; that must not end the process. zh-CN: 读取方提前停止会断开管道, 不能因此结束进程.
                runCatching { ParcelFileDescriptor.AutoCloseOutputStream(write).use { output -> file.inputStream().use { it.copyTo(output) } } }
            }.apply { isDaemon = true }.start()
            return read
        }
    }

    private fun cachedRecord(): FileTranscriptRecord? = FileTranscriptRecord.parse(cached.readBytes())

    private fun <T> main(block: () -> T): T {
        var value: Result<T>? = null
        instrumentation.runOnMainSync { value = runCatching(block) }
        return value!!.getOrThrow()
    }

    private fun await(what: String, timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            assertTrue("timed out: $what", System.currentTimeMillis() < deadline)
            Thread.sleep(50)
        }
    }

    private fun active(id: Int): Notification? = context.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull { it.id == id }?.notification

    private fun texts(notification: Notification): String = listOf(
        Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_INFO_TEXT,
    ).mapNotNull { notification.extras.getCharSequence(it)?.toString() }.plus(notification.tickerText?.toString().orEmpty()).joinToString("\n")

    private fun SemanticsNodeInteractionsProvider.nodes(tag: String) = onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()

    private fun exists(tag: String): Boolean = compose.nodes(tag).isNotEmpty()

    private fun enabled(tag: String): Boolean = compose.nodes(tag).firstOrNull()?.let { !it.config.contains(SemanticsProperties.Disabled) } ?: false

    private fun text(tag: String): String? = compose.nodes(tag).firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }

    private fun capture(prefix: String, name: String) {
        compose.waitForIdle()
        Thread.sleep(500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val folder = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }
        val file = File(folder, "$prefix-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        SpeechDevice.log("screen ${file.name} ${bitmap.width}x${bitmap.height}")
    }

    private companion object {
        const val MISSING = "asr.not-installed.file-tab-test"
        const val LANGID = "langid.whisper.tiny.int8"
        const val SECRET = "macaw-secret-4711"
        const val SECRET_NAME = "$SECRET.mp3"
        const val MARKS = ",.?!;:"
        val PAGE = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        val SOUNDING = SemanticsMatcher("the sounding row") { it.config.getOrNull(SemanticsProperties.Selected) == true && it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("macaw.tryout.file.segment.") == true }
        const val DIALOGUE = "analysis-audio/two-speakers.wav"
        const val MIXED = "analysis-audio/language-mixed.wav"
        const val TOGGLE = "macaw.tryout.file.player.toggle"
        const val POSITION = "macaw.tryout.file.player.position"
        const val DURATION = "macaw.tryout.file.player.duration"
        const val SPEED = "macaw.tryout.file.player.speed.value"
        const val LYRICS = "macaw.tryout.file.player.lyrics"
        const val UNAVAILABLE = "macaw.tryout.file.player.unavailable"
        const val DIALOGUE_MILLIS = 42_500L

        /** The fixture of `two-speakers.json`. zh-CN: `two-speakers.json` 的固定内容. */
        val TURNS = List(6) { SpeakerTurn(it * 7200L, it * 7200L + 6500, it % 2) }

        /** The fixture of `language-mixed.json`. zh-CN: `language-mixed.json` 的固定内容. */
        val SENTENCES = listOf(LanguageSegment(500, 4315, "zh"), LanguageSegment(4815, 11_440, "en"), LanguageSegment(11_940, 15_084, "ru"))
    }
}
