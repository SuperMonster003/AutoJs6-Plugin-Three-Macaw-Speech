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
     * Placeholder minimum host build at the roadmap baseline (D28), mirrored by `plugin_requires_host_version` in
     * `app/build.gradle.kts`; P1.3 and P6.4 replace both together.
     * zh-CN: 路线图基线的最低宿主构建号占位值 (D28), 与 `app/build.gradle.kts` 中的 `plugin_requires_host_version` 一致; P1.3 与
     * P6.4 同时替换两者.
     */
    const val REQUIRED_HOST_VERSION = 5448L

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
     * Optional features this build implements. The skeleton implements none; roadmap P3 / P4 add `asr`, `asrStreaming`,
     * `vad`, `kws`, `tts` and `ttsSystemEngine` as each one lands, and the host never relies on a feature that is not
     * advertised.
     * zh-CN: 本构建实现的可选功能. 骨架尚未实现任何功能; 路线图 P3 / P4 在各项落地时依次加入 `asr`, `asrStreaming`, `vad`, `kws`,
     * `tts` 与 `ttsSystemEngine`, 宿主不会依赖未声明的功能.
     */
    val FEATURES: List<String> = emptyList()

    /** Version of the bundled model catalog; 0 until roadmap P2.4 bundles one. zh-CN: 内置模型目录版本; 路线图 P2.4 之前为 0. */
    const val CATALOG_VERSION = 0

    /** Resource reference of the localized Plugin Center instruction. zh-CN: 插件中心本地化说明的资源引用. */
    const val INSTRUCTION_REFERENCE = "@raw/plugin_instruction"

}
