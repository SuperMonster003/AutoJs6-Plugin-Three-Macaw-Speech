package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * The manual update check against the live official plugin index (roadmap P11.2.4, D206): the controller of the
 * screens, as the "check for updates" row starts it, with the installed version code of the 1.4.0 release (172) finds
 * the newest indexed release (177 or later) and offers it; with the version code of this build it reports "up to date"
 * (a build of the repository never has a smaller code than a release made before it). The store is in memory, so no
 * ignored version or check time of the device changes. Opt-in because it needs the network: `-e macawNetwork true`.
 * Logs the found release under `MacawDeviceRun`.
 * zh-CN: 针对线上官方插件索引的手动更新检查 (路线图 P11.2.4, D206): 用界面的控制器 (与 "检查更新" 一行的启动方式相同), 以 1.4.0 正式版的已安装
 * versionCode (172) 找到索引中最新的发行版 (177 或更高) 并提示; 以本构建的 versionCode 则报告 "已是最新" (仓库构建的版本号不会小于其之前发布的版本).
 * 存储在内存中, 不改设备上的忽略版本与检查时间. 需要网络, 因此须显式开启: `-e macawNetwork true`. 在 `MacawDeviceRun` 下记录找到的发行版.
 */
@RunWith(AndroidJUnit4::class)
class AppUpdateIndexDeviceTest {

    @Before
    fun optIn() {
        assumeTrue("opt-in: -e macawNetwork true", SpeechDevice.argument("macawNetwork") == "true")
    }

    @Test
    fun theReleaseOf140IsOfferedTheNewestIndexedRelease() {
        val state = checkManuallyAs(RELEASE_1_4_0)
        assertTrue("expected a newer release, got $state", state is AppUpdateController.State.Available)
        val release = (state as AppUpdateController.State.Available).release
        SpeechDevice.log("update installed $RELEASE_1_4_0 offered ${release.tag} (${release.versionCode}), notes ${release.notes.length} characters")
        assertTrue("index release ${release.versionCode} older than 1.5.0", release.versionCode >= RELEASE_1_5_0)
        assertTrue(release.tag.startsWith("v"))
        assertTrue(release.pageUrl.endsWith("/tag/${release.tag}"))
    }

    @Test
    fun thisBuildIsUpToDate() {
        val state = checkManuallyAs(BuildConfig.VERSION_CODE.toLong())
        SpeechDevice.log("update installed ${BuildConfig.VERSION_CODE} result $state")
        assertEquals(AppUpdateController.State.UpToDate(noRelease = false), state)
    }

    /** Starts a manual check on the main thread and waits for its result (the check's own limit is 30 s). */
    private fun checkManuallyAs(installedVersionCode: Long): AppUpdateController.State? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val controller = AtomicReference<AppUpdateController>()
        instrumentation.runOnMainSync {
            controller.set(AppUpdateController(AppUpdateStore(MemoryStorage()), installedVersionCode).also { it.checkManually() })
        }
        val deadline = SystemClock.elapsedRealtime() + 45_000
        val state = AtomicReference<AppUpdateController.State?>()
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.runOnMainSync { state.set(controller.get().state) }
            if (state.get() != AppUpdateController.State.Checking) return state.get()
            SystemClock.sleep(200)
        }
        instrumentation.runOnMainSync { controller.get().cancel() }
        throw AssertionError("the update check did not finish within 45 s")
    }

    private class MemoryStorage : AppUpdateStore.Storage {
        private val values = mutableMapOf<String, Any>()
        override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        override fun getLong(key: String, default: Long) = values[key] as? Long ?: default
        override fun putLong(key: String, value: Long) { values[key] = value }
        @Suppress("UNCHECKED_CAST")
        override fun getStrings(key: String) = values[key] as? Set<String> ?: emptySet()
        override fun putStrings(key: String, value: Set<String>) { values[key] = value }
    }

    private companion object {
        const val RELEASE_1_4_0 = 172L
        const val RELEASE_1_5_0 = 177L
    }
}
