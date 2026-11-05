package io.github.supermonster003.autojs6.plugin.three.macaw.speech.transfer

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentSettings
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.FloatingMicService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPreferences
import org.autojs.plugin.common.api.PluginSettingsTransferProvider
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * The settings transfer provider of AutoJs6 (contract v1, authority `<package>.autojs6.settings`, behind the PLUGIN
 * permission; roadmap P10.1, host settings roadmap P18.1). The document is [MacawSettingsTransfer]'s. After an import
 * the running parts follow at once, as when the settings screen saves: the idle release time of loaded engines, the
 * resident wake word (a change of wake words, sensitivity or power saving reopens its session, a new listening time
 * counts from now while the switch is on) and the floating microphone (turned off or on). `restartNeeded` is true only
 * when Android refused to start a service from the background, so the change shows the next time the plugin opens; the
 * process is never ended (`restartProcessAfterImport` stays false), since a restart would not start those services either.
 * zh-CN: AutoJs6 的设置迁移 Provider (契约 v1, 授权名 `<包名>.autojs6.settings`, 受 PLUGIN 权限保护; 路线图 P10.1, 宿主设置路线图 P18.1).
 * 文档见 [MacawSettingsTransfer]. 导入后, 运行中的部分与设置页保存时一样立即跟随: 已加载引擎的空闲释放时间, 常驻唤醒 (唤醒词, 灵敏度或省电
 * 模式改变时重开会话, 开关开着时新的监听时长从现在起算) 与悬浮麦克风 (关闭或打开). 仅当 Android 拒绝从后台启动服务时 `restartNeeded` 为
 * true, 改动在下次打开插件时显示; 从不结束进程 (`restartProcessAfterImport` 保持 false), 因为重启同样启动不了这些服务.
 */
class SettingsTransferProvider : PluginSettingsTransferProvider() {

    override fun exportSettings(request: Request): Exported = MacawSettingsTransfer(preferencesOf(appContext())).exportSettings()

    override fun importSettings(settings: JSONObject, source: Source, request: Request): Imported {
        val context = appContext()
        val residentBefore = ResidentPreferences(context).read()
        val floatingBefore = TriggerPreferences(context).floating
        val imported = MacawSettingsTransfer(preferencesOf(context)).importSettings(settings)
        if (imported.written == 0) return imported
        val applied = onMain { follow(context, residentBefore, floatingBefore) }
        if (!applied) Log.i(TAG, "Imported settings wait for the plugin to open")
        return Imported(imported.written, imported.skipped, restartNeeded = !applied)
    }

    private fun appContext(): Context = checkNotNull(context) { "The provider is not attached" }.applicationContext

    /** Runs on the main thread, like the settings screen; false when a service could not start. zh-CN: 在主线程运行; 服务无法启动时为 false. */
    private fun follow(context: Context, residentBefore: ResidentSettings, floatingBefore: Boolean): Boolean {
        var applied = true
        SpeechRuntime.existing?.engines?.idleMillis = SpeechPreferences(context).idleMillis

        val resident = ResidentPreferences(context)
        var residentAfter = resident.read()
        if (residentAfter.duration != residentBefore.duration) {
            residentAfter = resident.update { it.withDuration(it.duration, ResidentWake.enabled(context), System.currentTimeMillis()) }
        }
        if (residentAfter != residentBefore) {
            applied = if (residentBefore.reopensFor(residentAfter)) ResidentWake.restart(context) else ResidentWake.refresh(context)
        }

        val floating = TriggerPreferences(context).floating
        if (floatingBefore && !floating) {
            FloatingMicService.stop(context)
        } else if (!floatingBefore && floating && FloatingMicService.isOn(context) && !FloatingMicService.start(context)) {
            applied = false
        }
        return applied
    }

    /** A main thread that does not answer in time counts as not applied. zh-CN: 主线程未及时响应时视为未生效. */
    private fun onMain(block: () -> Boolean): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val task = FutureTask(Callable(block))
        Handler(Looper.getMainLooper()).post(task)
        return runCatching { task.get(MAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            .onFailure { Log.w(TAG, "Imported settings could not be applied now: $it") }
            .getOrDefault(false)
    }

    private companion object {
        const val TAG = "MacawSettings"
        const val MAIN_TIMEOUT_SECONDS = 3L

        fun preferencesOf(context: Context): (String) -> SharedPreferences =
            { name -> context.getSharedPreferences(name, Context.MODE_PRIVATE) }
    }
}
