package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core

import org.autojs.plugin.speech.api.SpeechErrorCodes

/**
 * Failure with the contract error code the host shows the script (roadmap D27, `SpeechErrorCodes`): model and catalog
 * operations ([io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException]), audio input,
 * options, sessions and the runtime. Messages are English, as the protocol requires.
 * zh-CN: 带宿主展示给脚本的契约错误码的失败 (路线图 D27, `SpeechErrorCodes`): 模型与目录操作, 音频输入, 选项, 会话与运行时. 按协议要求, 信息为英文.
 */
open class SpeechException(val code: String, message: String, cause: Throwable? = null) : Exception(message, cause) {
    init {
        require(SpeechErrorCodes.isKnown(code)) { "Unknown speech error code: $code" }
    }
}
