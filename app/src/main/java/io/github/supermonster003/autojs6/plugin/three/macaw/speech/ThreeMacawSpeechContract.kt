package io.github.supermonster003.autojs6.plugin.three.macaw.speech

/**
 * Identity and protocol constants of the plugin (roadmap 5.5).
 *
 * The capability action, category, Binder descriptor and capability keys live here only until the host contract module
 * `plugin-api/speech-api` exists (roadmap P1.1, D21); P1.1 replaces them with the contract constants so the host and the
 * plugin cannot drift apart.
 *
 * zh-CN: 插件的身份与协议常量 (路线图 5.5). 能力服务的 action, category, Binder 描述符与能力键只在宿主契约模块
 * `plugin-api/speech-api` 落地 (路线图 P1.1, D21) 之前暂存于此; P1.1 改用契约常量, 避免宿主与插件两侧漂移.
 */
internal object ThreeMacawSpeechContract {

    const val PLUGIN_ID = "three-macaw-speech"
    const val ENGINE = "speech"
    const val VARIANT = "default"

    /** Plugin Center identity service (roadmap 5.5). zh-CN: 插件中心身份服务 (路线图 5.5). */
    const val INFO_ACTION = "org.autojs.plugin.INFO"
    const val INFO_CATEGORY = PLUGIN_ID

    /** Capability service discovery (roadmap D21). zh-CN: 能力服务的发现 action 与 category (路线图 D21). */
    const val SERVICE_ACTION = "org.autojs.plugin.SPEECH"
    const val SERVICE_CATEGORY = "speech"

    /** Descriptor of the future `ISpeechPlugin` (roadmap appendix C.1). zh-CN: 未来 `ISpeechPlugin` 的描述符 (附录 C.1). */
    const val CAPABILITY_DESCRIPTOR = "org.autojs.plugin.speech.api.ISpeechPlugin"

    /**
     * Minimum host build (roadmap D28, written in P6.4): the first host build with every speech change of the host, up to
     * commit 11f75efbe (`VERSION_BUILD` 3804 + 1652), equal to `SpeechPluginIds.REQUIRED_HOST_VERSION_CODE` of the staged
     * contract and mirrored by `plugin_requires_host_version` in `app/build.gradle.kts` (the P1.3 temporary value was 5451).
     * zh-CN: 最低宿主构建号 (路线图 D28, P6.4 回填): 含宿主全部 speech 改动 (至提交 11f75efbe) 的首个宿主构建 (`VERSION_BUILD` 3804 + 1652),
     * 与暂存契约的 `SpeechPluginIds.REQUIRED_HOST_VERSION_CODE` 相同, 并与 `app/build.gradle.kts` 中的 `plugin_requires_host_version`
     * 一致 (P1.3 临时值为 5451).
     */
    const val REQUIRED_HOST_VERSION = 5456L

    /** Speech contract version (roadmap D21 / D28). zh-CN: 语音契约版本 (路线图 D21 / D28). */
    const val CONTRACT_VERSION = 1

    /**
     * Capability keys of roadmap D28; P1.1 moves them to `SpeechCapabilityKeys`.
     * zh-CN: 路线图 D28 的能力键; P1.1 将其移入 `SpeechCapabilityKeys`.
     */
    const val CAPABILITY_CONTRACT_VERSION = "speechContractVersion"
    const val CAPABILITY_FEATURES = "speechFeatures"
    const val CAPABILITY_CATALOG_VERSION = "speechCatalogVersion"

    /**
     * Optional features this build implements: model management (`models`: install, import, remove, default model and
     * catalog refresh, roadmap P2.3 / P2.4), file and external PCM recognition (`asr`), microphone dictation
     * (`asrStreaming`), voice activity detection (`vad`), wake sessions (`kws`, roadmap P3), synthesis to a file (`tts`),
     * the system TTS engine (`ttsSystemEngine`, roadmap P4) and audio files beyond WAV (`audioDecode`, roadmap D84: the
     * host passes such files only to a plugin that declares it); the host never relies on a feature that is not advertised.
     * zh-CN: 本构建实现的可选功能: 模型管理 (`models`: 安装, 导入, 删除, 默认模型与目录刷新, 路线图 P2.3 / P2.4), 文件与外部 PCM 识别 (`asr`),
     * 麦克风听写 (`asrStreaming`), 语音活动检测 (`vad`), 唤醒会话 (`kws`, 路线图 P3), 合成到文件 (`tts`), 系统 TTS 引擎 (`ttsSystemEngine`, 路线图 P4)
     * 与 WAV 以外的音频文件 (`audioDecode`, 路线图 D84: 宿主只把这类文件交给声明了它的插件); 宿主不会依赖未声明的功能.
     */
    val FEATURES: List<String> = listOf("models", "asr", "asrStreaming", "vad", "kws", "tts", "ttsSystemEngine", "audioDecode")

    /**
     * Version of the model catalog bundled in `assets/catalog/catalog-v1.json` (roadmap D28, P2.4); `BundledCatalogTest`
     * keeps it equal to the asset. `getCapabilities` reports the catalog in use, which a refresh may advance.
     * zh-CN: `assets/catalog/catalog-v1.json` 中内置模型目录的版本 (路线图 D28, P2.4); `BundledCatalogTest` 保证其与资产一致.
     * `getCapabilities` 报告当前使用的目录, 刷新后可能更新.
     */
    const val CATALOG_VERSION = 1

    /** Resource reference of the localized Plugin Center instruction. zh-CN: 插件中心本地化说明的资源引用. */
    const val INSTRUCTION_REFERENCE = "@raw/plugin_instruction"

}
