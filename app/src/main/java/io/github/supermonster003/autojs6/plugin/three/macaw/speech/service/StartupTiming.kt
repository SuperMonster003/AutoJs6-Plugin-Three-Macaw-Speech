package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * The startup log of the first `speech.listen` (roadmap P11.1.1, F9): one line when the speech service is created and
 * bound, measured from the start of the plugin process, and one when a session is ready, with the time its engines
 * took. `.python/f9_startup_timing.py` reads these lines (tag [TAG]) together with the host's binding log to split the
 * start of a session into its four parts. Text never appears in these lines.
 * zh-CN: 首次 `speech.listen` 的启动日志 (路线图 P11.1.1, F9): 语音服务创建与绑定时各一行 (从插件进程启动起算), 会话就绪时一行 (含引擎耗时).
 * `.python/f9_startup_timing.py` 结合宿主的绑定日志读取这些行 (tag [TAG]), 把会话启动拆成四段. 这些行从不包含识别文本.
 */
internal object StartupTiming {

    const val TAG = "MacawStartup"

    /** Milliseconds since the plugin process started. zh-CN: 插件进程启动以来的毫秒数. */
    fun processMillis(): Long = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()

    fun log(message: String) {
        Log.i(TAG, message)
    }

    /**
     * The checks of a session before its binder exists: the speech runtime (created on the first call of the process),
     * the microphone permission and the model's resolution, logged as one line.
     * zh-CN: 会话绑定器创建之前的检查: 语音运行时 (进程内首次调用时创建), 麦克风权限与模型解析, 记为一行.
     */
    class Checks {
        private val began = SystemClock.elapsedRealtime()
        private var runtimeAt = began
        private var resolvedAt = began
        private var created = false

        fun runtime(created: Boolean) {
            this.created = created
            runtimeAt = SystemClock.elapsedRealtime()
        }

        /** After the permission checks and the model's resolution. zh-CN: 权限检查与模型解析之后. */
        fun resolved() {
            resolvedAt = SystemClock.elapsedRealtime()
        }

        fun log(kind: String) {
            val now = SystemClock.elapsedRealtime()
            val runtime = if (created) "created" else "ready"
            StartupTiming.log("$kind checked in ${now - began} ms (runtime $runtime after ${runtimeAt - began} ms, permission and model in ${resolvedAt - runtimeAt} ms)")
        }
    }
}
