package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R

/**
 * The "Tap to speak" home screen widget (roadmap D63): one round microphone button whose tap opens the assistant sheet
 * through [TriggerActivity] (D60). It never updates on a schedule and records nothing (D58).
 * zh-CN: "按一下说话" 桌面小部件 (路线图 D63): 一个圆形麦克风按钮, 点按经 [TriggerActivity] 打开助理弹层 (D60). 它不定时更新, 也不录音 (D58).
 */
class VoiceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        runCatching { manager.updateAppWidget(ids, views(context)) }.onFailure { Log.w(TAG, "The widget could not be updated: $it") }
    }

    companion object {
        private const val TAG = "MacawTrigger"
        private const val REQUEST_WIDGET = 0x5747

        fun component(context: Context) = ComponentName(context, VoiceWidgetProvider::class.java)

        internal fun views(context: Context): RemoteViews = RemoteViews(context.packageName, R.layout.widget_voice).apply {
            setOnClickPendingIntent(R.id.widget_voice_button, pendingIntent(context))
        }

        /** The tap of the widget: the trigger activity, as the launcher sends it. zh-CN: 小部件的点按: 由启动器发出的触发 Activity. */
        internal fun pendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context, REQUEST_WIDGET, TriggerActivity.intent(context, TriggerSource.WIDGET), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** The launcher accepts pin requests (API 26+). zh-CN: 启动器接受固定请求 (API 26+). */
        fun canRequestPin(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            runCatching { context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true }.getOrDefault(false)

        /**
         * API 26+: asks the launcher to place the widget (the launcher shows its own confirmation); false when the launcher
         * cannot, so the settings screen shows the manual steps instead.
         * zh-CN: API 26+: 请启动器放置小部件 (启动器自行显示确认); 启动器不支持时为 false, 设置页改为显示手动步骤.
         */
        fun requestPin(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
            return runCatching { manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(component(context), null, null) }
                .onFailure { Log.w(TAG, "The widget pin request failed: $it") }
                .getOrDefault(false)
        }
    }
}
