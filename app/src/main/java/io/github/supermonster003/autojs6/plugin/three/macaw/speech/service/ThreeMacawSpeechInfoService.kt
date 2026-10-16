package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginInfo

/**
 * Answers `org.autojs.plugin.INFO` (category `three-macaw-speech`) so the Plugin Center lists the speech identity of this
 * package before and independently of the capability service.
 * zh-CN: 响应 `org.autojs.plugin.INFO` (category `three-macaw-speech`), 使插件中心独立于能力服务显示本包的语音身份.
 */
class ThreeMacawSpeechInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo(): PluginInfo = pluginRuntimeFields().toPluginInfo()
    }

    override fun onBind(intent: Intent?): IBinder = binder

}
