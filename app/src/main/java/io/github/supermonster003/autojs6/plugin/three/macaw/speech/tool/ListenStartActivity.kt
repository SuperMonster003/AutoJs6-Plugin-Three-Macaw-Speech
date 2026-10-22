package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

/**
 * The transparent start screen of `speech_listen` (roadmap D89 (2) and (3)): opened from the background or by the "Tap to
 * speak" notification, it makes the plugin the top app while the dictation starts the microphone foreground service,
 * then closes ([ListenStarts.LIFETIME_MILLIS] at the latest). It does not show over the lock screen: a locked phone is
 * unlocked first, and a screen created behind the lock screen is never resumed, so the call moves to the next route.
 * Internal; it shows nothing.
 * zh-CN: `speech_listen` 的透明启动页 (路线图 D89 (2) 与 (3)): 从后台打开或由 "点此说话" 通知打开, 在听写启动麦克风前台服务期间让插件成为顶层应用,
 * 随后关闭 (最迟 [ListenStarts.LIFETIME_MILLIS]). 不显示在锁屏之上: 锁定的手机先解锁, 在锁屏之后创建的页面不会进入 resumed, 调用随即改走下一途径.
 * 不导出, 不显示任何内容.
 */
class ListenStartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null || !ListenStarts.onCreated(this)) {
            finish()
            return
        }
        window.decorView.postDelayed({ if (!isFinishing) finish() }, ListenStarts.LIFETIME_MILLIS)
    }

    override fun onResume() {
        super.onResume()
        ListenStarts.onResumed(this)
    }

    companion object {
        internal fun intent(context: Context): Intent = Intent(context, ListenStartActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}

/**
 * The tool call that waits for [ListenStartActivity]: one at a time (the dictation is exclusive anyway). A start screen
 * that comes when no call waits, or a second one, closes at once.
 * zh-CN: 等待 [ListenStartActivity] 的工具调用: 同一时刻只有一个 (听写本身也是独占的). 没有调用在等或重复出现的启动页立即关闭.
 */
internal object ListenStarts {
    /** The start screen closes after this at the latest. zh-CN: 启动页最迟在此后关闭. */
    const val LIFETIME_MILLIS = 6000L

    private val main = Handler(Looper.getMainLooper())
    private var waiting: Waiter? = null

    class Waiter {
        /** Counted down when the start screen is resumed, or by [abandon]. zh-CN: 启动页进入 resumed 或 [abandon] 时计数归零. */
        val shown = CountDownLatch(1)

        @Volatile
        private var screen: WeakReference<Activity>? = null

        /** The start screen that claimed this call, while it lives. zh-CN: 认领此调用的启动页 (存活期间). */
        val activity: Activity? get() = screen?.get()

        internal val claimed get() = screen != null

        @Volatile
        var resumed = false
            private set

        internal fun claim(activity: Activity) {
            screen = WeakReference(activity)
        }

        internal fun resume() {
            resumed = true
            shown.countDown()
        }

        /** Wakes the waiting call without a start screen (cancellation). zh-CN: 不经启动页唤醒等待中的调用 (取消). */
        fun abandon() = shown.countDown()
    }

    @Synchronized
    fun arm(): Waiter = Waiter().also { waiting = it }

    @Synchronized
    fun onCreated(activity: Activity): Boolean {
        val waiter = waiting ?: return false
        if (waiter.claimed || waiter.shown.count == 0L) return false
        waiter.claim(activity)
        return true
    }

    @Synchronized
    fun onResumed(activity: Activity) {
        waiting?.takeIf { it.activity === activity }?.resume()
    }

    /** The call no longer needs the start screen: it closes. zh-CN: 调用不再需要启动页: 关闭它. */
    fun release(waiter: Waiter) {
        synchronized(this) {
            if (waiting === waiter) waiting = null
        }
        waiter.abandon()
        main.post { waiter.activity?.takeIf { !it.isFinishing }?.finish() }
    }
}
