package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Installs the three models the recognition device tests need (roadmap P3.5) from the `speech-models-v1` mirror with
 * `ModelManager`, opt-in because it downloads about 430 MB: `-e macawModels true`. Models already installed are kept;
 * every download logs its bytes, its source (`mirror` / `upstream`) and its time under `MacawDeviceRun`. `-e macawModelIds
 * a,b` installs those catalog models instead (a fresh AVD for the voice input of roadmap P5.6, for one).
 * zh-CN: 以 `ModelManager` 从 `speech-models-v1` 镜像安装识别设备测试所需的三个模型 (路线图 P3.5), 因下载约 430 MB 需显式开启:
 * `-e macawModels true`. 已安装的模型保留; 每次下载在 `MacawDeviceRun` 下记录字节数, 来源 (`mirror` / `upstream`) 与耗时. `-e macawModelIds a,b`
 * 改为安装这些目录模型 (例如为路线图 P5.6 的语音输入准备新的 AVD).
 */
@RunWith(AndroidJUnit4::class)
class SpeechModelSetupDeviceTest {

    @Test
    fun installsTheRecognitionModels() {
        assumeTrue("opt-in: -e macawModels true", SpeechDevice.argument("macawModels") == "true")
        val manager = SpeechModels.get(SpeechDevice.context)
        val ids = SpeechDevice.argument("macawModelIds")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: SpeechDevice.MODELS
        ids.forEach { id ->
            if (SpeechDevice.installed(id)) {
                SpeechDevice.log("setup $id already installed")
                return@forEach
            }
            val done = CountDownLatch(1)
            val completed = AtomicReference<OperationResult>()
            val failure = AtomicReference<ModelException>()
            val started = SystemClock.elapsedRealtime()
            manager.install(id, acceptLicense = false, listener = object : OperationListener {
                override fun onProgress(opId: Int, done: Long, total: Long) = Unit

                override fun onCompleted(opId: Int, result: OperationResult) {
                    completed.set(result)
                    done.countDown()
                }

                override fun onFailed(opId: Int, error: ModelException) {
                    failure.set(error)
                    done.countDown()
                }
            })
            assertTrue("$id did not finish within 30 minutes", done.await(30, TimeUnit.MINUTES))
            assertNull("$id failed: ${failure.get()?.code} ${failure.get()?.message}", failure.get())
            val model = completed.get() as OperationResult.Model
            val seconds = (SystemClock.elapsedRealtime() - started) / 1000.0
            SpeechDevice.log("setup $id installed origin=${model.origin} downloaded=${model.downloadedBytes} installed=${model.entry.installedBytes} seconds=${"%.1f".format(seconds)} mirrorFailure=${model.mirrorFailure}")
            assertTrue(SpeechDevice.installed(id))
        }
    }
}
