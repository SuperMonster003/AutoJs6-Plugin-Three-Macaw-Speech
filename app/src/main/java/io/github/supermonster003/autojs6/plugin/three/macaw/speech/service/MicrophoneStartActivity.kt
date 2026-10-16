package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.app.Activity
import android.os.Bundle

/**
 * Transparent, internal activity that starts [MicrophoneService] while the plugin is the top app (API 34+, roadmap
 * appendix D Q5 (a)): only a foreground service started from a visible state gets microphone access since Android 14.
 * It shows nothing and closes as soon as the service reports (three seconds at the latest).
 * zh-CN: 透明的内部 Activity, 在插件为顶层应用时启动 [MicrophoneService] (API 34+, 路线图附录 D Q5 (a)): 自 Android 14 起, 只有在可见状态下启动
 * 的前台服务才能访问麦克风. 它不显示任何内容, 服务一有结果即关闭 (最迟三秒).
 */
class MicrophoneStartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }
        MicrophoneService.onStarterCreated(this)
    }
}
