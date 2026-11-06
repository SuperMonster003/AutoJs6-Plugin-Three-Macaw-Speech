package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.CheckRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MessageDialog
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The newest release of the plugin in the official plugin index (roadmap P11.2.4, D206): [tag] is `v` + its version
 * name (the release tag, also the key of an ignored update), [notes] the release notes the index copies from the GitHub
 * release (D207).
 * zh-CN: 官方插件索引中本插件的最新发行版 (路线图 P11.2.4, D206): [tag] 为 `v` + 版本名 (即发行版 tag, 也是忽略某次更新时记下的键), [notes] 为索引从 GitHub
 * 发行版复制的发行说明 (D207).
 */
internal data class AppRelease(val tag: String, val versionCode: Long, val name: String, val notes: String, val pageUrl: String)

/**
 * Roadmap P11.2.4 (D206): an index release is newer when its versionCode is greater than the installed one, as in the
 * Plugin Center of AutoJs6; the version name only names it. Pure Kotlin.
 * zh-CN: 路线图 P11.2.4 (D206): 索引中的发行版 versionCode 大于已安装的才算新版, 与 AutoJs6 插件中心一致; 版本名只用于显示. 纯 Kotlin.
 */
internal object UpdateVersionPolicy {
    fun isNewer(candidateVersionCode: Long, installedVersionCode: Long): Boolean = candidateVersionCode > installedVersionCode
}

/**
 * Update preferences (3-Stone AI rules): automatic checks on by default and at most once every 12 hours, ignored tags.
 * [clock] is injectable for the JVM tests.
 * zh-CN: 更新偏好 (沿用 3-Stone AI): 自动检查默认开启且每 12 小时至多一次, 已忽略的版本. [clock] 可注入, 供 JVM 测试使用.
 */
internal class AppUpdateStore(private val storage: Storage, private val clock: () -> Long = System::currentTimeMillis) {

    /** The persistence behind the store (SharedPreferences in the app). zh-CN: 存储后端 (应用中为 SharedPreferences). */
    interface Storage {
        fun getBoolean(key: String, default: Boolean): Boolean
        fun putBoolean(key: String, value: Boolean)
        fun getLong(key: String, default: Long): Long
        fun putLong(key: String, value: Long)
        fun getStrings(key: String): Set<String>
        fun putStrings(key: String, value: Set<String>)
    }

    var automaticChecks: Boolean
        get() = storage.getBoolean(KEY_AUTOMATIC, true)
        set(value) = storage.putBoolean(KEY_AUTOMATIC, value)

    val ignoredTags: Set<String> get() = storage.getStrings(KEY_IGNORED)

    fun ignore(tag: String) = storage.putStrings(KEY_IGNORED, ignoredTags + tag)

    fun stopIgnoring(tags: Collection<String>) = storage.putStrings(KEY_IGNORED, ignoredTags - tags.toSet())

    fun shouldCheckAutomatically(): Boolean {
        if (!automaticChecks) return false
        val last = storage.getLong(KEY_LAST_ATTEMPT, 0L)
        val now = clock()
        // A clock set back counts as due as well. zh-CN: 时钟被调回时同样视为到期.
        return last <= 0L || now < last || now - last >= INTERVAL_MILLIS
    }

    fun recordAutomaticAttempt() = storage.putLong(KEY_LAST_ATTEMPT, clock())

    companion object {
        val INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(12)
        /** The file and the key the settings transfer lists (`transfer/MacawSettingsTransfer`). zh-CN: 设置迁移列出的文件与键名. */
        internal const val FILE = "app-update-settings"
        internal const val KEY_AUTOMATIC = "automatic-checks"
        private const val KEY_IGNORED = "ignored-tags"
        private const val KEY_LAST_ATTEMPT = "last-automatic-attempt"

        fun of(context: Context): AppUpdateStore {
            val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            return AppUpdateStore(object : Storage {
                override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
                override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
                override fun getLong(key: String, default: Long) = preferences.getLong(key, default)
                override fun putLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()
                override fun getStrings(key: String): Set<String> = preferences.getStringSet(key, emptySet()).orEmpty().toSet()
                override fun putStrings(key: String, value: Set<String>) = preferences.edit().putStringSet(key, value).apply()
            })
        }
    }
}

/** What a check found. zh-CN: 检查结果. */
internal sealed interface UpdateResult {
    data class Release(val release: AppRelease) : UpdateResult

    /**
     * The official index has no release of the plugin (no entry, or HTTP 404), which counts as no newer release.
     * zh-CN: 官方索引中没有本插件的发行版 (没有条目或 HTTP 404), 视为没有新版.
     */
    data object NoRelease : UpdateResult
}

/**
 * Reads the newest release of the plugin from the official plugin index (roadmap P11.2.4, D206), the file and the
 * mirrors the Plugin Center of AutoJs6 reads (`PluginIndexRepository`, `PluginIndexMirrors`): raw.githubusercontent.com
 * first, then the two jsDelivr hosts when a request fails; HTTP 404 ends the check as [UpdateResult.NoRelease]. The
 * limits of 3-Stone AI stay (D36: 15 s connect, 20 s read, 30 s in total for every attempt together, no redirects, at
 * most 6000 characters of notes); the response may hold up to 8 MiB, since the index lists every official plugin.
 * [Call.cancel] aborts the connection.
 * zh-CN: 从官方插件索引读取本插件的最新发行版 (路线图 P11.2.4, D206), 文件与镜像同 AutoJs6 插件中心 (`PluginIndexRepository`, `PluginIndexMirrors`):
 * 先 raw.githubusercontent.com, 请求失败再依次换 jsDelivr 的两个域名; HTTP 404 即以 [UpdateResult.NoRelease] 结束. 沿用 3-Stone AI 的限制 (D36:
 * 连接 15 秒, 读取 20 秒, 所有尝试合计 30 秒, 不跟随重定向, 说明至多 6000 字符); 索引列出全部官方插件, 响应至多 8 MiB. [Call.cancel] 中止连接.
 */
internal class AppUpdateChecker(private val indexUrls: List<String> = INDEX_URLS, private val packageName: String = BuildConfig.APPLICATION_ID) {

    inner class Call internal constructor() {
        private val cancelled = AtomicBoolean(false)
        @Volatile var connection: HttpURLConnection? = null

        val isCancelled: Boolean get() = cancelled.get()

        /** The total time limit closed the connection (a failure, not a user cancel). zh-CN: 总时限关闭了连接 (属于失败, 不是用户取消). */
        @Volatile var timedOut = false

        fun cancel() {
            cancelled.set(true)
            runCatching { connection?.disconnect() }
        }
    }

    fun check(callback: (Call, Result<UpdateResult>) -> Unit): Call {
        val call = Call()
        worker.execute {
            // The total limit: the connection is closed when the check takes longer. zh-CN: 总时限: 超时即关闭连接.
            val timeout = Runnable {
                call.timedOut = true
                call.cancel()
            }
            main.postDelayed(timeout, TOTAL_TIMEOUT_MILLIS)
            val result = runCatching { fetch(call) }
            main.removeCallbacks(timeout)
            callback(call, result)
        }
        return call
    }

    internal fun fetch(call: Call): UpdateResult {
        var failure: Exception? = null
        for (url in indexUrls) {
            if (call.isCancelled) throw IOException("cancelled")
            try {
                val text = download(call, url) ?: return UpdateResult.NoRelease
                return decode(text, packageName)?.let(UpdateResult::Release) ?: UpdateResult.NoRelease
            } catch (e: Exception) {
                // A cancel or the total limit ends every attempt; any other failure tries the next mirror.
                // zh-CN: 取消或总时限结束全部尝试; 其他失败换下一个镜像.
                if (call.isCancelled) throw e
                failure = failure ?: e
            }
        }
        throw failure ?: IOException("No index address")
    }

    /** The index text, or null for HTTP 404. zh-CN: 索引文本, HTTP 404 时为 null. */
    private fun download(call: Call, url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Three-Macaw-Speech-update-checker/${BuildConfig.VERSION_NAME}")
        }
        call.connection = connection
        try {
            if (call.isCancelled) throw IOException("cancelled")
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Index service returned HTTP $code")
            val declared = connection.contentLengthLong
            if (declared > MAXIMUM_RESPONSE_BYTES) throw IOException("Index response is too large")
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    if (output.size() > MAXIMUM_RESPONSE_BYTES) throw IOException("Index response is too large")
                }
            }
            return output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val RELEASES_PAGE = "https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"
        private const val INDEX_OWNER = "SuperMonster003"
        private const val INDEX_REPO = "AutoJs6-Official-Plugins-Index"
        private const val INDEX_BRANCH = "main"
        private const val INDEX_PATH = "plugins.official.generated.json"

        /**
         * The generated official index on its branch: GitHub's raw host, then jsDelivr's GitHub mirror on two hosts (the
         * order of `PluginIndexMirrors` in AutoJs6).
         * zh-CN: 分支上生成的官方索引: 先 GitHub raw 域名, 再 jsDelivr 的 GitHub 镜像的两个域名 (同 AutoJs6 `PluginIndexMirrors` 的顺序).
         */
        val INDEX_URLS = listOf(
            "https://raw.githubusercontent.com/$INDEX_OWNER/$INDEX_REPO/refs/heads/$INDEX_BRANCH/$INDEX_PATH",
            "https://cdn.jsdelivr.net/gh/$INDEX_OWNER/$INDEX_REPO@$INDEX_BRANCH/$INDEX_PATH",
            "https://fastly.jsdelivr.net/gh/$INDEX_OWNER/$INDEX_REPO@$INDEX_BRANCH/$INDEX_PATH",
        )
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 20_000
        const val TOTAL_TIMEOUT_MILLIS = 30_000L
        const val MAXIMUM_RESPONSE_BYTES = 8 * 1024 * 1024
        const val MAXIMUM_NOTES_CHARACTERS = 6_000
        private const val DEFAULT_TITLE = "3-Macaw Speech"
        private val VERSION_NAME_PATTERN = Regex("^[0-9A-Za-z._-]{1,100}$")
        private val worker = Executors.newSingleThreadExecutor { Thread(it, "MacawUpdateCheck").apply { isDaemon = true } }
        // Lazy, so [decode] stays usable on the JVM without a main looper. zh-CN: 延迟创建, 使 [decode] 在没有主 Looper 的 JVM 上也能用.
        private val main by lazy { Handler(Looper.getMainLooper()) }

        /**
         * The newest release of [packageName] in the generated official index, or null when the index has no entry or no
         * usable release for it. A release counts when it is not a prerelease and has a positive versionCode and a valid
         * version name; the greatest versionCode wins. Members are read with `opt` and a type check, since Android's
         * `optString` turns a JSON null into "null". Pure apart from `org.json`.
         * zh-CN: 生成的官方索引中 [packageName] 的最新发行版; 索引没有该条目或没有可用的发行版时为 null. 可用指不是预发布版, versionCode 为正且版本名有效;
         * 取 versionCode 最大的一个. 成员用 `opt` 加类型判断读取, 因为 Android 的 `optString` 把 JSON null 变成 "null". 除 `org.json` 外是纯函数.
         */
        fun decode(text: String, packageName: String): AppRelease? {
            val items = JSONObject(text).getJSONArray("items")
            val item = items.objects().firstOrNull { it.opt("packageName") == packageName } ?: return null
            val release = (item.opt("releases") as? JSONArray)?.objects()
                ?.filter { it.opt("prerelease") != true && it.versionCode() > 0L && (it.text("versionName")?.let(VERSION_NAME_PATTERN::matches) == true) }
                ?.maxByOrNull { it.versionCode() }
                ?: return null
            val versionName = release.text("versionName")!!
            val tag = "v$versionName"
            return AppRelease(
                tag = tag,
                versionCode = release.versionCode(),
                name = "${item.text("title") ?: DEFAULT_TITLE} $versionName",
                notes = release.text("changelogText").orEmpty().take(MAXIMUM_NOTES_CHARACTERS),
                pageUrl = "$RELEASES_PAGE/tag/$tag",
            )
        }

        private fun JSONArray.objects(): Sequence<JSONObject> = (0 until length()).asSequence().mapNotNull { opt(it) as? JSONObject }

        private fun JSONObject.text(key: String): String? = (opt(key) as? String)?.trim()?.takeIf(String::isNotEmpty)

        /** A whole positive number, else 0. zh-CN: 正整数, 否则为 0. */
        private fun JSONObject.versionCode(): Long = when (val value = opt("versionCode")) {
            is Int -> value.toLong()
            is Long -> value
            else -> 0L
        }.coerceAtLeast(0L)
    }
}

/**
 * The update check of the screens (roadmap P5.4, as in 3-Stone AI): a manual check shows a cancellable progress dialog
 * and every result; an automatic check (home screen, at most every 12 hours) stays silent unless a newer release that
 * is not ignored exists. Compose state, main thread only.
 * zh-CN: 界面的更新检查 (路线图 P5.4, 与 3-Stone AI 相同): 手动检查显示可取消的进度对话框与每种结果; 自动检查 (首页, 每 12 小时至多一次) 只在存在未被
 * 忽略的新版本时提示. Compose 状态, 仅在主线程使用.
 */
internal class AppUpdateController(
    private val store: AppUpdateStore,
    private val installedVersionCode: Long,
    private val checker: AppUpdateChecker = AppUpdateChecker(),
) {
    sealed interface State {
        data object Checking : State
        data class UpToDate(val noRelease: Boolean) : State
        data class Available(val release: AppRelease) : State
        data object Failed : State
    }

    var state by mutableStateOf<State?>(null)
        private set

    private var active: AppUpdateChecker.Call? = null

    fun checkManually() = check(manual = true)

    fun checkAutomaticallyIfDue() {
        if (active != null || !store.shouldCheckAutomatically()) return
        store.recordAutomaticAttempt()
        check(manual = false)
    }

    fun dismiss() {
        if (state == State.Checking) active?.cancel()
        state = null
    }

    fun ignore(tag: String) = store.ignore(tag)

    fun cancel() {
        active?.cancel()
        active = null
        if (state == State.Checking) state = null
    }

    private fun check(manual: Boolean) {
        if (active != null) return
        if (manual) state = State.Checking
        active = checker.check { call, result ->
            mainHandler.post {
                if (active === call) active = null
                if (call.isCancelled && !call.timedOut) {
                    if (state == State.Checking) state = null
                    return@post
                }
                result.fold(
                    onSuccess = { found ->
                        when (found) {
                            UpdateResult.NoRelease -> if (manual) state = State.UpToDate(noRelease = true)
                            is UpdateResult.Release -> {
                                val release = found.release
                                when {
                                    !UpdateVersionPolicy.isNewer(release.versionCode, installedVersionCode) -> if (manual) state = State.UpToDate(noRelease = false)
                                    !manual && release.tag in store.ignoredTags -> Unit
                                    else -> state = State.Available(release)
                                }
                            }
                        }
                    },
                    onFailure = { if (manual) state = State.Failed },
                )
            }
        }
    }

    private companion object {
        val mainHandler = Handler(Looper.getMainLooper())
    }
}

/**
 * The dialogs of [AppUpdateController]: progress (cancel), up to date, failure, and the release dialog with "view
 * release", "later", the neutral "release history" button and the "ignore this version" check box.
 * zh-CN: [AppUpdateController] 的对话框: 进度 (可取消), 已是最新, 失败, 以及带 "查看发行版", "稍后", Neutral "发行历史" 按钮与 "忽略此版本" 复选框的发行版对话框.
 */
@Composable
internal fun UpdateDialogs(controller: AppUpdateController, onOpenRelease: (String) -> Unit, onReleaseHistory: () -> Unit) {
    when (val state = controller.state) {
        null -> Unit
        AppUpdateController.State.Checking -> MessageDialog(
            title = stringResource(R.string.update_checking),
            message = stringResource(R.string.update_checking_summary),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_cancel),
            dismissText = null,
            tag = "macaw.update.checking",
        )
        is AppUpdateController.State.UpToDate -> MessageDialog(
            title = stringResource(R.string.update_up_to_date),
            message = if (state.noRelease) stringResource(R.string.update_no_release, BuildConfig.VERSION_NAME) else stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_confirm),
            dismissText = null,
            tag = "macaw.update.current",
        )
        AppUpdateController.State.Failed -> MessageDialog(
            title = stringResource(R.string.update_check),
            message = stringResource(R.string.update_check_failed),
            onDismissRequest = controller::dismiss,
            onConfirm = controller::dismiss,
            confirmText = stringResource(R.string.ui_dialog_confirm),
            dismissText = null,
            tag = "macaw.update.failed",
        )
        is AppUpdateController.State.Available -> {
            val release = state.release
            var ignore by rememberSaveable(release.tag) { mutableStateOf(false) }
            fun close(open: Boolean) {
                if (ignore) controller.ignore(release.tag)
                controller.dismiss()
                if (open) onOpenRelease(release.pageUrl)
            }
            MacawDialog(
                title = release.name,
                onDismissRequest = { close(open = false) },
                confirmText = stringResource(R.string.update_view_release),
                onConfirm = { close(open = true) },
                dismissText = stringResource(R.string.update_later),
                neutralText = stringResource(R.string.release_history_title),
                onNeutral = {
                    if (ignore) controller.ignore(release.tag)
                    controller.dismiss()
                    onReleaseHistory()
                },
                tag = "macaw.update.available",
            ) {
                Text(
                    stringResource(R.string.update_available_summary, release.tag) + if (release.notes.isNotBlank()) "\n\n" + release.notes else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding),
                )
                CheckRow(Choice(stringResource(R.string.update_ignore_version)), ignore, { ignore = it }, Modifier.testTag("macaw.update.ignore"))
            }
        }
    }
}
