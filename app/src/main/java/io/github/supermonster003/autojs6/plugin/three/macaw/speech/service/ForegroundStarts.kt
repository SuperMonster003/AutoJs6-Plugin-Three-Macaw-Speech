package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * `Context.startForegroundService` with its answer. `ContextCompat.startForegroundService` drops the answer, which is
 * null when Android drops the start without an exception (see [ForegroundStartWatch]), so every foreground service
 * start of the plugin goes through here.
 * zh-CN: 带返回值的 `Context.startForegroundService`. `ContextCompat.startForegroundService` 丢弃返回值, 而 Android 不抛异常就丢弃启动时它为 null
 * (见 [ForegroundStartWatch]), 因此插件的前台服务启动都经过这里.
 */
internal object ForegroundStarts {

    /** Answers the started component, or null when Android dropped the start. zh-CN: 返回启动的组件; Android 丢弃启动时为 null. */
    fun request(context: Context, intent: Intent): ComponentName? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)

    /**
     * For the callers that only need to know whether the start went through: a dropped start throws like a refused one.
     * zh-CN: 供只需知道启动是否成功的调用方: 被丢弃的启动与被拒的启动一样抛出异常.
     */
    fun require(context: Context, intent: Intent): ComponentName =
        request(context, intent) ?: throw IllegalStateException(ForegroundStartWatch.REASON_DROPPED)
}
