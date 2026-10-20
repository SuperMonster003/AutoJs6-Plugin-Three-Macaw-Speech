package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager

/**
 * The launch target of the widget, the launcher shortcut and the quick settings tile (roadmap D61, D63): no window of
 * its own, it hands over to [AssistantEntry] (the assistant session or the sheet, D60) and finishes. It shows over the
 * lock screen, so a tile tapped there opens the sheet without unlocking (D55, D61). Internal; the launcher starts the
 * static shortcut as the plugin itself.
 * zh-CN: 桌面小部件, 启动器快捷方式与快速设置磁贴的启动目标 (路线图 D61, D63): 没有自己的窗口, 交给 [AssistantEntry] (助理会话或弹层, D60) 后即结束.
 * 它可以显示在锁屏之上, 因此在锁屏上点磁贴无需解锁即可打开弹层 (D55, D61). 不导出; 启动器以插件自身的身份启动静态快捷方式.
 */
class TriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        val source = TriggerSource.of(intent?.getStringExtra(AssistantEntry.EXTRA_SOURCE)) ?: TriggerSource.SHORTCUT
        runCatching { AssistantEntry.open(this, source) }.onFailure { Log.w(TAG, "Trigger ${source.id} failed: $it") }
        finish()
    }

    companion object {
        private const val TAG = "MacawTrigger"

        internal fun intent(context: Context, source: TriggerSource): Intent = Intent(context, TriggerActivity::class.java)
            .putExtra(AssistantEntry.EXTRA_SOURCE, source.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}
