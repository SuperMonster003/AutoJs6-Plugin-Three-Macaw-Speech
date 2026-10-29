package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.pluginRuntimeFields
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.toPluginInfo
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.autojs.plugin.tool.api.PluginToolProviderService
import org.autojs.plugin.tool.api.PluginToolResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * The plugin tool provider of 3-Macaw Speech (AutoJs6 plugin tool contract v1, roadmap P7.6): AutoJs6 binds it with
 * `org.autojs.plugin.TOOL_PROVIDER` behind the plugin permission and forwards the calls of MCP Server, 3-Stove Agent and
 * `plugins.tools`. It runs in the plugin's only process (D88), beside the speech service, the assistant and the resident
 * wake word, so a tool shares their microphone session table and engine cache. The contract library's base class runs
 * each call on a worker thread and reports cancellation; speech errors become tool errors by [SpeechToolErrors] (D100).
 * Arguments, recognized text and the text to speak are never logged.
 * zh-CN: 3-Macaw Speech 的插件工具提供方 (AutoJs6 插件工具契约 v1, 路线图 P7.6): AutoJs6 以 `org.autojs.plugin.TOOL_PROVIDER` 在插件权限下
 * 绑定它, 转发 MCP Server, 3-Stove Agent 与 `plugins.tools` 的调用. 它运行在插件唯一的进程中 (D88), 与语音服务, 助理和常驻唤醒同处,
 * 因此工具共用它们的麦克风会话表与引擎缓存. 契约库的基类在工作线程上执行每次调用并报告取消; 语音错误由 [SpeechToolErrors] 换成工具错误
 * (D100). 参数, 识别文字与朗读文字从不写日志.
 */
class SpeechToolService : PluginToolProviderService() {

    private val listenTool by lazy { SpeechListenTool(this) }
    private val speakTool by lazy { SpeechSpeakTool(this) }
    private val transcribeTool by lazy { SpeechTranscribeTool(this) }
    private val synthesizeTool by lazy { SpeechSynthesizeTool(this) }
    private val speakerTool by lazy { SpeechSpeakerTool(this) }
    private val analysisTool by lazy { SpeechAnalysisTool(this) }

    override fun createPluginInfo(): PluginInfo = pluginRuntimeFields().toPluginInfo()

    override fun createCatalogJson(): String = SpeechToolCatalog.JSON

    override fun onCall(call: PluginToolCall): PluginToolResult {
        val arguments = call.arguments.toMap()
        Log.i(TAG, "Tool ${call.tool} called by ${call.consumer ?: "an unnamed consumer"}")
        try {
            return when (call.tool) {
                SpeechToolCatalog.TOOL_LISTEN -> listenTool.call(call, SpeechToolArguments.listen(arguments))
                SpeechToolCatalog.TOOL_SPEAK -> speakTool.call(call, SpeechToolArguments.speak(arguments))
                SpeechToolCatalog.TOOL_TRANSCRIBE -> transcribeTool.call(call, SpeechToolArguments.transcribe(arguments))
                SpeechToolCatalog.TOOL_SYNTHESIZE -> synthesizeTool.call(call, SpeechToolArguments.synthesize(arguments))
                SpeechToolCatalog.TOOL_SPEAKER_VERIFY -> speakerTool.verify(call, SpeechToolArguments.speakerVerify(arguments))
                SpeechToolCatalog.TOOL_SPEAKER_IDENTIFY -> speakerTool.identify(call, SpeechToolArguments.speakerIdentify(arguments))
                SpeechToolCatalog.TOOL_DIARIZE -> analysisTool.diarize(call, SpeechToolArguments.diarize(arguments))
                SpeechToolCatalog.TOOL_DETECT_LANGUAGE -> analysisTool.detectLanguage(call, SpeechToolArguments.detectLanguage(arguments))
                SpeechToolCatalog.TOOL_TAG -> analysisTool.tag(call, SpeechToolArguments.tag(arguments))
                else -> throw PluginToolException(PluginToolErrorCodes.UNKNOWN_TOOL, "3-Macaw Speech has no tool ${call.tool}")
            }
        } catch (e: SpeechException) {
            throw SpeechToolErrors.of(e)
        }
    }

    private companion object {
        const val TAG = "MacawTools"

        fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { plain(opt(it)) }

        fun plain(value: Any?): Any? = when (value) {
            null, JSONObject.NULL -> null
            is JSONObject -> value.toMap()
            is JSONArray -> (0 until value.length()).map { plain(value.opt(it)) }
            else -> value
        }
    }

}
