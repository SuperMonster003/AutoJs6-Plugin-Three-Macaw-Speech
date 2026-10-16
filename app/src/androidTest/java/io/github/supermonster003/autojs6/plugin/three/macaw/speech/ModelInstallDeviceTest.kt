package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelCatalogRepository
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import org.autojs.plugin.speech.api.ISpeechPlugin
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechModelKeys
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Real network run of roadmap P2.3 / P2.4 on a device, opt-in because it downloads about 32 MB from GitHub:
 * `-Pandroid.testInstrumentationRunnerArguments.macawNetwork=true`. Installs `tts.vits.icefall.zh-aishell3` through
 * `ISpeechPlugin` (until the maintainer publishes `speech-models-v1` the mirror answers 404, so this also checks the
 * fallback to the sherpa-onnx release), cancels a first attempt after a few megabytes and resumes it, verifies every
 * installed file by SHA-256, removes the model, and refreshes the catalog (an error before the signed catalog is
 * promoted, with the bundled catalog kept). Results are logged under the tag `MacawDeviceRun`.
 * zh-CN: 路线图 P2.3 / P2.4 在设备上的真实网络运行, 因为要从 GitHub 下载约 32 MB, 需显式开启:
 * `-Pandroid.testInstrumentationRunnerArguments.macawNetwork=true`. 经 `ISpeechPlugin` 安装 `tts.vits.icefall.zh-aishell3` (维护者发布
 * `speech-models-v1` 之前镜像返回 404, 因此同时检查回退到 sherpa-onnx Release), 第一次下载几兆字节后取消再续传, 按 SHA-256 校验每个安装
 * 文件, 删除模型, 并刷新目录 (签名目录提升之前会报错, 内置目录保持不变). 结果以 `MacawDeviceRun` 标签写入日志.
 */
@RunWith(AndroidJUnit4::class)
class ModelInstallDeviceTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private class Callback : ISpeechProgressCallback.Stub() {
        val done = AtomicLong()
        val total = AtomicLong()
        val events = AtomicLong()
        val result = AtomicReference<Bundle>()
        val latch = CountDownLatch(1)
        override fun onProgress(opId: Int, done: Long, total: Long) {
            this.done.set(done)
            this.total.set(total)
            events.incrementAndGet()
        }

        override fun onCompleted(opId: Int, result: Bundle) {
            this.result.set(result)
            latch.countDown()
        }

        override fun onFailed(opId: Int, error: Bundle) {
            this.result.set(error)
            latch.countDown()
        }
    }

    @Test
    fun installsResumesVerifiesAndRemovesARealModel() {
        assumeTrue("opt-in: -Pandroid.testInstrumentationRunnerArguments.macawNetwork=true", InstrumentationRegistry.getArguments().getString("macawNetwork") == "true")
        // The service object itself (same process); ThreeMacawSpeechContractTest covers the marshalled transactions.
        val plugin: ISpeechPlugin = SpeechPluginBinder(context)
        val manager = SpeechModels.get(context)
        val request = Bundle().apply {
            putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CONTRACT_VERSION)
            putString(SpeechOptionKeys.MODEL, MODEL)
        }
        plugin.removeModel(request)
        File(context.cacheDir, "downloads").deleteRecursively()
        val catalogVersion = manager.catalogVersion()
        val version = manager.catalogs.current().catalog.model(MODEL)!!.installableVersion!!
        report("device ${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT} ${Build.SUPPORTED_ABIS.first()}, catalog v$catalogVersion (${manager.catalogs.current().trust})")

        // Without acceptLicense the unknown license of aishell3 is refused before any download (roadmap D11).
        val refused = Callback()
        plugin.installModel(request, refused)
        assertTrue(refused.latch.await(60, TimeUnit.SECONDS))
        assertEquals(SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED, refused.result.get().getString(SpeechContract.KEY_ERROR_CODE))

        // First attempt, cancelled after a few megabytes of the upstream archive.
        request.putBoolean(SpeechOptionKeys.ACCEPT_LICENSE, true)
        val first = Callback()
        val firstId = plugin.installModel(request, first)
        val deadline = SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(10)
        while (first.done.get() < CANCEL_AFTER_BYTES && first.latch.count > 0 && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        plugin.cancelOperation(firstId)
        assertTrue(first.latch.await(60, TimeUnit.SECONDS))
        assertEquals(SpeechErrorCodes.SESSION_CANCELLED, first.result.get().getString(SpeechContract.KEY_ERROR_CODE))
        val partial = File(File(context.cacheDir, "downloads"), "${version.upstream.first().sha256}.part").length()
        report("cancelled at ${first.done.get()} of ${first.total.get()} progress bytes, partial upstream file $partial bytes")
        assertTrue(partial > 0)

        // Second attempt resumes and completes.
        val second = Callback()
        val started = SystemClock.elapsedRealtime()
        plugin.installModel(request, second)
        assertTrue("install timed out", second.latch.await(30, TimeUnit.MINUTES))
        val elapsed = SystemClock.elapsedRealtime() - started
        val answer = second.result.get()
        assertFalse("install failed: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.containsKey(SpeechContract.KEY_ERROR_CODE))
        val model = requireNotNull(answer.getBundle(SpeechResultKeys.MODEL))
        assertTrue(model.getBoolean(SpeechModelKeys.INSTALLED))
        val installed = requireNotNull(manager.store.installed(MODEL))
        val verifyStarted = SystemClock.elapsedRealtime()
        val damaged = manager.store.verify(installed)
        report(
            "installed $MODEL from ${installed.origin}: upstream ${version.upstreamBytes} bytes (resumed from $partial), " +
                "${installed.installedBytes} bytes in ${installed.files.size} files, ${elapsed} ms, ${second.events.get()} progress events, " +
                "SHA-256 of every file ${if (damaged.isEmpty()) "matches" else "differs: $damaged"} (${SystemClock.elapsedRealtime() - verifyStarted} ms)",
        )
        assertEquals(emptyList<String>(), damaged)
        assertEquals("the mirror Release does not exist before roadmap P2.5", "upstream", installed.origin)

        // listModels sees it, removeModel deletes it.
        @Suppress("DEPRECATION")
        val listed = plugin.listModels(Bundle().apply { putString(SpeechOptionKeys.KIND, "tts") }).getParcelableArrayList<Bundle>(SpeechResultKeys.MODELS)!!
        assertTrue(listed.single { it.getString(SpeechModelKeys.ID) == MODEL }.getBoolean(SpeechModelKeys.INSTALLED))
        assertTrue(plugin.removeModel(request).getBoolean(SpeechResultKeys.REMOVED))
        assertFalse(installed.directory.exists())

        // A real catalog refresh: before the maintainer promotes the signed catalog the raw URL answers 404.
        val refresh = Callback()
        plugin.refreshCatalog(refresh)
        assertTrue(refresh.latch.await(3, TimeUnit.MINUTES))
        val refreshed = refresh.result.get()
        report("catalog refresh: ${refreshed.getString(SpeechContract.KEY_ERROR_CODE) ?: "v${refreshed.getInt(SpeechResultKeys.CATALOG_VERSION)} updated ${refreshed.getBoolean(SpeechResultKeys.UPDATED)}"} ${refreshed.getString(SpeechContract.KEY_ERROR_MESSAGE).orEmpty()}")
        if (refreshed.containsKey(SpeechContract.KEY_ERROR_CODE)) {
            assertEquals(SpeechErrorCodes.IO_FAILED, refreshed.getString(SpeechContract.KEY_ERROR_CODE))
            assertEquals(ModelCatalogRepository.Trust.UNSIGNED_BUNDLED, manager.catalogs.current().trust)
            assertEquals(catalogVersion, manager.catalogVersion())
        }
    }

    private fun report(message: String) {
        Log.i("MacawDeviceRun", message)
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply { putString("MacawDeviceRun", message) })
    }

    private companion object {
        const val MODEL = "tts.vits.icefall.zh-aishell3"
        const val CANCEL_AFTER_BYTES = 4L * 1024 * 1024
    }
}
