package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Keyword
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakePower
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechOptionKeys

/**
 * The switch of the resident wake word (roadmap D75, the 3-Stone AI D31 convention): [ResidentWakeService] is enabled
 * exactly while it is on, with no separate preference, so 3-Stone AI and 3-Stove Agent see whether 3-Macaw Speech
 * listens. The service only starts from a visible state (a screen of the plugin, the notification trampoline), since
 * Android lets a microphone foreground service record only then.
 * zh-CN: 常驻唤醒的开关 (路线图 D75, 3-Stone AI D31 约定): [ResidentWakeService] 恰在开关打开时启用, 不另存偏好, 因此 3-Stone AI 与 3-Stove Agent
 * 能看到 3-Macaw Speech 是否在监听. 服务只从可见状态启动 (插件的界面, 通知跳板), 因为 Android 只在此时允许麦克风前台服务录音.
 */
internal object ResidentWake {
    private const val TAG = "MacawResident"

    /** The action of the mutual exclusion convention (3-Stone AI D31). zh-CN: 互斥约定的 action (3-Stone AI D31). */
    const val ACTION_RESIDENT_LISTENER = "org.autojs.plugin.speech.action.RESIDENT_LISTENER"

    /** The meta-data naming the settings activity another app opens. zh-CN: 指明供对方打开的设置 Activity 的 meta-data. */
    const val META_SETTINGS = "org.autojs.plugin.speech.RESIDENT_LISTENER_SETTINGS"

    fun component(context: Context) = ComponentName(context, ResidentWakeService::class.java)

    fun enabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(component(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    /** Off restores the Manifest default (disabled); the app is never killed. zh-CN: 关闭即恢复 Manifest 默认值 (禁用); 从不结束应用. */
    fun setEnabled(context: Context, enabled: Boolean) = context.packageManager.setComponentEnabledSetting(
        component(context),
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
        PackageManager.DONT_KILL_APP,
    )

    /**
     * Other apps whose resident listener is on: only enabled components answer the query; this package never counts.
     * zh-CN: 常驻监听已开启的其他应用: 只有已启用的组件会应答查询; 本包不计入.
     */
    fun others(context: Context): List<ResidentListenerApp> = runCatching {
        val manager = context.packageManager
        @Suppress("DEPRECATION")
        val found = manager.queryIntentServices(Intent(ACTION_RESIDENT_LISTENER), PackageManager.GET_META_DATA).map { info ->
            val service = info.serviceInfo
            ResidentListenerApp(service.packageName, service.applicationInfo.loadLabel(manager).toString(), service.metaData?.getString(META_SETTINGS))
        }
        ResidentListenerApps.others(found, context.packageName)
    }.getOrDefault(emptyList())

    /** "Open xxx settings": the activity the other app names, else its launcher entry. zh-CN: "打开 xxx 设置": 对方指定的 Activity, 否则其启动入口. */
    fun openSettings(context: Context, app: ResidentListenerApp): Boolean = listOfNotNull(
        app.settingsClass?.let { Intent().setComponent(ComponentName(app.packageName, it)) },
        context.packageManager.getLaunchIntentForPackage(app.packageName),
    ).any { intent ->
        runCatching { context.startActivity(if (context is Activity) intent else intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }

    /** "Only while charging" (D77): plugged into any power source, charging or not. zh-CN: "只在充电时监听" (D77): 接着任何电源, 无论是否正在充. */
    fun plugged(context: Context): Boolean = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }.getOrDefault(true)

    fun overlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Roadmap D78; lists the installed models, so off the main thread. zh-CN: 路线图 D78; 会列出已装模型, 因此在主线程之外调用. */
    fun availability(context: Context): ResidentAvailability {
        val runtime = SpeechRuntime.get(context)
        val models = runCatching { SpeechModels.get(context).list(installed = true) }.getOrDefault(emptyList())
        return ResidentAvailability.of(
            runtime.microphoneGranted(),
            models.count { it.kind == SpeechContract.KIND_KWS },
            models.count { it.kind == SpeechContract.KIND_ASR },
        )
    }

    /**
     * The first wake word the default wake word model cannot spell (roadmap D70, D18), or null when all can or no such
     * model is installed (the switch then says the model is missing). Off the main thread.
     * zh-CN: 默认唤醒词模型无法拼写的第一个唤醒词 (路线图 D70, D18); 全部可拼写或未安装该模型时为 null (此时开关说明缺模型). 在主线程之外调用.
     */
    fun unspellable(context: Context, keywords: List<String>): String? {
        val runtime = SpeechRuntime.get(context)
        val model = runCatching { runtime.resolve(SpeechContract.KIND_KWS, null) }.getOrNull() ?: return null
        return keywords.firstOrNull { keyword ->
            try {
                runtime.keywordBuffer(model, listOf(Keyword(keyword)))
                false
            } catch (_: SpeechException) {
                true
            } catch (_: RuntimeException) {
                true
            }
        }
    }

    /** Starts (or keeps) the service from a visible state. zh-CN: 从可见状态启动 (或保持) 服务. */
    fun start(context: Context, action: String? = null): Boolean = runCatching {
        ContextCompat.startForegroundService(context, Intent(context, ResidentWakeService::class.java).setAction(action))
    }.onFailure { Log.w(TAG, "The resident wake word service could not start: $it") }.isSuccess

    /** Turning the switch on starts its listening time (D77) and the service. zh-CN: 打开开关即开始计时 (D77) 并启动服务. */
    fun turnOn(context: Context): Boolean {
        ResidentPreferences(context).update { it.turnedOn(System.currentTimeMillis()) }
        setEnabled(context, true)
        return start(context)
    }

    fun turnOff(context: Context) {
        runCatching { context.stopService(Intent(context, ResidentWakeService::class.java)) }
        setEnabled(context, false)
        ResidentNotifications.cancelReminders(context)
    }

    /**
     * A screen of the plugin or the notification trampoline became visible (D74, D76): an enabled switch resumes (a
     * running service applies its rules again and reopens a session that waited for a visible state); one whose
     * listening time ran out while nothing ran is turned off instead (D77). The service checks the rival apps itself.
     * zh-CN: 插件界面或通知跳板变为可见 (D74, D76): 开着的开关恢复 (运行中的服务重新应用规则, 并重开等待可见状态的会话); 监听时长在无服务期间已到则改为
     * 关闭 (D77). 对方应用由服务自己检查.
     */
    fun resume(context: Context) {
        when (ResidentResume.of(enabled(context), ResidentPreferences(context).read(), System.currentTimeMillis())) {
            ResidentResume.TURN_OFF -> turnOff(context)
            ResidentResume.RESUME -> start(context)
            ResidentResume.NOTHING -> Unit
        }
    }

    /**
     * The power saving mode of the wake sessions opened from now on: the debug switch and the device tests set a
     * measured variant here (roadmap P8.1.1); production code never sets it, so the setting decides.
     * zh-CN: 此后打开的唤醒会话的省电做法: 调试开关与设备测试在此设置实测的做法 (路线图 P8.1.1); 正式代码从不设置它, 由设置决定.
     */
    @Volatile
    var powerOverride: WakePower? = null

    /** What the wake session of [settings] runs with. zh-CN: [settings] 的唤醒会话使用的省电做法. */
    fun power(settings: ResidentSettings): WakePower = powerOverride ?: ResidentPower.of(settings.powerSaving)

    /** Wake words, sensitivity or power saving changed: a running service reopens its wake session. zh-CN: 唤醒词, 灵敏度或省电模式已改: 运行中的服务重开唤醒会话. */
    fun restart(context: Context) {
        if (enabled(context) && ResidentWakeService.running != null) start(context, ResidentWakeService.ACTION_RESTART)
    }

    /** The listening time or the power rule changed: a running service follows them. zh-CN: 监听时长或电源规则已改: 运行中的服务随之调整. */
    fun refresh(context: Context) {
        if (enabled(context) && ResidentWakeService.running != null) start(context, ResidentWakeService.ACTION_REFRESH)
    }

    /** The options of the wake session (D70): one Bundle per keyword, the threshold of the sensitivity. zh-CN: 唤醒会话的选项. */
    fun options(settings: ResidentSettings): Bundle = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CONTRACT_VERSION)
        putParcelableArrayList(
            SpeechOptionKeys.KEYWORDS,
            ArrayList(
                settings.keywords.map { keyword ->
                    Bundle().apply {
                        putString(SpeechOptionKeys.KEYWORD, keyword)
                        settings.sensitivity.threshold?.let { putFloat(SpeechOptionKeys.THRESHOLD, it) }
                    }
                },
            ),
        )
    }
}

/**
 * The preferences of the resident wake word (roadmap D70, D77, D69), in the plugin's single process.
 * zh-CN: 常驻唤醒的偏好 (路线图 D70, D77, D69), 插件为单进程.
 */
internal class ResidentPreferences(context: Context) {

    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** A missing or invalid value means its default. zh-CN: 缺失或无效的值取默认值. */
    fun read(): ResidentSettings {
        val keywords = preferences.getString(KEY_KEYWORDS, null)?.split(LINE)?.takeIf(ResidentKeywords::valid) ?: ResidentKeywords.DEFAULT
        return ResidentSettings(
            keywords = keywords,
            sensitivity = WakeSensitivity.of(preferences.getString(KEY_SENSITIVITY, null)),
            duration = ListeningDuration.of(preferences.getString(KEY_DURATION, null)),
            chargingOnly = preferences.getBoolean(KEY_CHARGING, false),
            readAloud = preferences.getBoolean(KEY_READ_ALOUD, true),
            deadline = preferences.getLong(KEY_DEADLINE, 0L).takeIf { it > 0 },
            powerSaving = preferences.getBoolean(KEY_POWER_SAVING, true),
        )
    }

    /** Written at once, so that the service started next reads it. zh-CN: 立即写入, 随后启动的服务读到的就是它. */
    fun write(value: ResidentSettings) {
        preferences.edit(commit = true) {
            putString(KEY_KEYWORDS, value.keywords.joinToString(LINE))
            putString(KEY_SENSITIVITY, value.sensitivity.id)
            putString(KEY_DURATION, value.duration.id)
            putBoolean(KEY_CHARGING, value.chargingOnly)
            putBoolean(KEY_READ_ALOUD, value.readAloud)
            putBoolean(KEY_POWER_SAVING, value.powerSaving)
            if (value.deadline == null) remove(KEY_DEADLINE) else putLong(KEY_DEADLINE, value.deadline)
        }
    }

    @Synchronized
    fun update(transform: (ResidentSettings) -> ResidentSettings): ResidentSettings = transform(read()).also(::write)

    private companion object {
        const val FILE = "resident-wake"
        const val KEY_KEYWORDS = "keywords"
        const val KEY_SENSITIVITY = "sensitivity"
        const val KEY_DURATION = "duration"
        const val KEY_CHARGING = "chargingOnly"
        const val KEY_READ_ALOUD = "readAloud"
        const val KEY_DEADLINE = "deadline"
        const val KEY_POWER_SAVING = "powerSaving"
        val LINE = Char(10).toString()
    }
}
