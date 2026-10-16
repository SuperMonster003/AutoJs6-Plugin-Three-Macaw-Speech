package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.app.Activity
import android.os.Bundle

/**
 * Invisible activation entry the Plugin Center starts on devices that keep newly installed apps stopped (ColorOS and
 * similar). It has no side effects, loads no model and finishes at once.
 * zh-CN: 插件中心在新装应用保持停止状态的设备 (ColorOS 等) 上启动的不可见激活入口. 无任何副作用, 不加载模型, 立即结束.
 */
class WakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
