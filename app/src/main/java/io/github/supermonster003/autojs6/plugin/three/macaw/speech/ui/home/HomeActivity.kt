package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.settings.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.UpdateDialogs
import java.util.concurrent.Executors

/**
 * The launcher target (roadmap D7 / D26 / P5.1): status card and model management. The four launcher aliases of roadmap
 * P5.5 point here; the system TTS settings' `INSTALL_TTS_DATA` opens it at the synthesis models. The automatic update
 * check of roadmap P5.4 runs here at most every 12 hours.
 * zh-CN: 启动器目标 (路线图 D7 / D26 / P5.1): 状态卡与模型管理. 路线图 P5.5 的四个启动器 alias 指向此处; 系统 TTS 设置的 `INSTALL_TTS_DATA` 打开它并定位到
 * 合成模型. 路线图 P5.4 的自动更新检查在此运行, 每 12 小时至多一次.
 */
class HomeActivity : StandaloneActivity() {

    private lateinit var controller: HomeController
    private lateinit var updates: AppUpdateController
    private lateinit var preferences: SpeechPreferences
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // A permanent denial no longer shows the system dialog: open the app's system settings instead.
        if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) openAppSettings()
        refreshStatus()
    }

    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val archive = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { controller.importChosen(ImportSource(it, tree = false, displayName = displayName(it))) }
    }

    private val folder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { controller.importChosen(ImportSource(it, tree = true, displayName = it.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/'))) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = SpeechPreferences(this)
        val environment = object : HomeEnvironment {
            override fun networkAvailable(): Boolean = network().first
            override fun networkMetered(): Boolean = network().second
            override var allowMeteredDownloads: Boolean
                get() = preferences.allowMeteredDownloads
                set(value) {
                    preferences.allowMeteredDownloads = value
                }

            override fun beforeFirstDownload() = askNotificationsOnce()
        }
        controller = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeController(ManagerBackend(applicationContext), OperationTracker.shared, environment, { task -> io.execute(task) }, { task -> main.post(task) }) as T
        })[HomeController::class.java]
        controller.environment = environment
        updates = AppUpdateController(AppUpdateStore.of(this), BuildConfig.VERSION_NAME)
        super.onCreate(savedInstanceState)
        controller.refresh()
        if (savedInstanceState == null) updates.checkAutomaticallyIfDue()
    }

    @Composable
    override fun Content() {
        val scrollTo = if (intent?.action == TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA || intent?.getBooleanExtra(EXTRA_SHOW_TTS, false) == true) "tts" else null
        HomeScreen(
            controller = controller,
            actions = HomeActions(
                onTryout = { startActivity(Intent(this, TryoutActivity::class.java)) },
                onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                onRequestMicrophone = { microphone.launch(Manifest.permission.RECORD_AUDIO) },
                onOpenTtsSettings = ::openTtsSettings,
                onPickArchive = { runCatching { archive.launch(arrayOf("*/*")) } },
                onPickFolder = { runCatching { folder.launch(null) } },
                onOpenUrl = ::openUrl,
            ),
            scrollToKind = scrollTo,
        )
        UpdateDialogs(updates, onOpenRelease = ::openUrl, onReleaseHistory = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) })
    }

    override fun onStart() {
        super.onStart()
        watchNetwork()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        controller.refresh()
        // Roadmap D74 / D77: a visible home screen resumes the resident wake word, or turns it off when its time ran out.
        ResidentWake.resume(this)
    }

    override fun onStop() {
        networkCallback?.let { callback -> runCatching { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(callback) } }
        networkCallback = null
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) updates.cancel()
        super.onDestroy()
    }

    /** A dialog draft or a running import must not be lost to an appearance change. zh-CN: 对话框草稿与进行中的导入不因外观变化丢失. */
    override fun holdsAppearance(): Boolean = controller.dialog != null || updates.state != null

    private fun refreshStatus() {
        val granted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val selected = runCatching { Settings.Secure.getString(contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH) == packageName }.getOrDefault(false)
        controller.status = HomeStatus(runtimeVersion = BuildConfig.SHERPA_ONNX_VERSION, microphoneGranted = granted, ttsEngineSelected = selected)
        controller.networkAvailable = network().first
    }

    /** (connected, metered) of the active network. zh-CN: 当前网络的 (已连接, 计量). */
    private fun network(): Pair<Boolean, Boolean> {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return true to false
        val capabilities = runCatching { manager.getNetworkCapabilities(manager.activeNetwork) }.getOrNull() ?: return false to false
        val connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return connected to manager.isActiveNetworkMetered
    }

    private fun watchNetwork() {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                update()
            }

            override fun onLost(network: Network) {
                update()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                update()
            }

            private fun update() {
                main.post { if (!isDestroyed) controller.networkAvailable = network().first }
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }.onSuccess { networkCallback = callback }
    }

    private fun askNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || preferences.notificationsAsked) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        preferences.notificationsAsked = true
        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openTtsSettings() {
        val opened = runCatching { startActivity(Intent(ACTION_TTS_SETTINGS)) }.isSuccess
        if (!opened) runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun openAppSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    companion object {
        /** Opens the home screen at the synthesis models (the settings' "install voices"). zh-CN: 打开首页并定位到合成模型. */
        const val EXTRA_SHOW_TTS = "showTts"
        private const val ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"
        private val io = Executors.newSingleThreadExecutor { Thread(it, "MacawHome").apply { isDaemon = true } }
        private val main = Handler(Looper.getMainLooper())

        fun intent(context: Context, showTts: Boolean = false): Intent = Intent(context, HomeActivity::class.java).putExtra(EXTRA_SHOW_TTS, showTts)
    }
}
