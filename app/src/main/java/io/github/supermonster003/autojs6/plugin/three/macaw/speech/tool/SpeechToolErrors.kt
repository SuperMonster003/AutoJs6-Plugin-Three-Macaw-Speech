package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException

/**
 * Roadmap D100: the speech error codes of the plugin as tool error codes. A missing permission or model (and a model
 * whose license is not accepted yet) is `NOT_READY`, a running dictation `BUSY`, a wrong option or an unknown speaker
 * name (roadmap P8.3.5) `INVALID_ARGUMENTS`, an unreadable input file or one with too little speech `INPUT_UNREADABLE`
 * (only when [input] says the failure is about the input), a ceiling
 * `LIMIT_EXCEEDED`, a cancelled session `CANCELLED`; the rest is `FAILED`. The message keeps the speech code in front of
 * the plugin's English detail, which never holds recognized text, the text to speak or a file name. Pure Kotlin.
 * zh-CN: 路线图 D100: 把插件的语音错误码换成工具错误码. 缺权限或模型 (以及尚未接受许可的模型) 为 `NOT_READY`, 正在听写为 `BUSY`, 选项错误为
 * `INVALID_ARGUMENTS` (未注册的说话人名字也是, 路线图 P8.3.5), 输入文件读不了或语音太少为 `INPUT_UNREADABLE` (仅当 [input] 表明失败与输入有关),
 * 上限为 `LIMIT_EXCEEDED`, 会话被取消为
 * `CANCELLED`; 其余为 `FAILED`. 信息在插件的英文详情前保留语音错误码, 详情从不包含识别文字, 朗读文字或文件名. 纯 Kotlin.
 */
internal object SpeechToolErrors {

    fun code(speechCode: String, input: Boolean = false): String = when (speechCode) {
        SpeechErrorCodes.PERMISSION_DENIED, SpeechErrorCodes.MODEL_MISSING, SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED -> PluginToolErrorCodes.NOT_READY
        SpeechErrorCodes.SESSION_BUSY -> PluginToolErrorCodes.BUSY
        SpeechErrorCodes.INVALID_OPTIONS, SpeechErrorCodes.SPEAKER_NOT_FOUND -> PluginToolErrorCodes.INVALID_ARGUMENTS
        SpeechErrorCodes.UNSUPPORTED_FORMAT, SpeechErrorCodes.IO_FAILED, SpeechErrorCodes.AUDIO_TOO_SHORT -> if (input) PluginToolErrorCodes.INPUT_UNREADABLE else PluginToolErrorCodes.FAILED
        SpeechErrorCodes.LIMIT_EXCEEDED -> PluginToolErrorCodes.LIMIT_EXCEEDED
        SpeechErrorCodes.SESSION_CANCELLED -> PluginToolErrorCodes.CANCELLED
        else -> PluginToolErrorCodes.FAILED
    }

    fun of(e: SpeechException, input: Boolean = false): PluginToolException =
        PluginToolException(code(e.code, input), "${e.code}: ${e.message ?: "no detail"}", e)

    /** A failure message of the speech binder, `CODE: detail` (SpeechErrorCodes.decode). zh-CN: 语音 Binder 的失败信息 `CODE: detail`. */
    fun of(code: String, detail: String, input: Boolean = false): PluginToolException =
        PluginToolException(code(code, input), "$code: ${detail.ifEmpty { "no detail" }}")

    fun notReady(message: String): PluginToolException = PluginToolException(PluginToolErrorCodes.NOT_READY, message)

}
