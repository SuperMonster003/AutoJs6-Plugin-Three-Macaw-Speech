package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ThreeMacawSpeechContract

/**
 * Capability service for `org.autojs.plugin.SPEECH` (category `speech`, roadmap D21).
 *
 * Until the host contract `ISpeechPlugin` lands (roadmap P1.1), it answers a placeholder Binder that only carries the
 * contract descriptor, so discovery and binding can already be tested; any other transaction is rejected by the default
 * Binder. `getInfo` and `getCapabilities` are served by the INFO service and `PluginInfo.capabilities` meanwhile.
 *
 * zh-CN: `org.autojs.plugin.SPEECH` (category `speech`, 路线图 D21) 的能力服务. 在宿主契约 `ISpeechPlugin` 落地 (路线图 P1.1)
 * 之前, 它返回只携带契约描述符的占位 Binder, 以便提前测试发现与绑定; 其他事务都会被默认 Binder 拒绝. 在此期间 `getInfo` 与
 * `getCapabilities` 由 INFO 服务与 `PluginInfo.capabilities` 提供.
 */
class ThreeMacawSpeechService : Service() {

    private val binder = Binder().apply {
        attachInterface(null, ThreeMacawSpeechContract.CAPABILITY_DESCRIPTOR)
    }

    override fun onBind(intent: Intent?): IBinder = binder

}
