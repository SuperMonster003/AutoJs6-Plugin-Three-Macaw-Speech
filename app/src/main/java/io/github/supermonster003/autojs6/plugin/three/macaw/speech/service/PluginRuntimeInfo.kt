package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ThreeMacawSpeechContract
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.speech.api.SpeechCapabilityKeys

/**
 * The PluginInfo fields as plain values, so JVM tests can freeze the mapping without Android.
 * zh-CN: 以纯值表示的 PluginInfo 字段, 使 JVM 测试无需 Android 即可冻结映射.
 */
internal data class PluginRuntimeFields(
    val name: String,
    val description: String,
    val instruction: String,
    val author: String,
    val id: String,
    val engine: String,
    val variant: String,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
    /** The ABIs the plugin is built for (roadmap D14), not only the installed split. zh-CN: 插件构建支持的 ABI (路线图 D14), 不只是已安装分包. */
    val supportedAbis: List<String>,
    val requiredHostVersion: Long,
    /** Speech contract version, features and catalog version (roadmap D28). zh-CN: 语音契约版本, 功能集合与目录版本 (路线图 D28). */
    val contractVersion: Int,
    val features: List<String>,
    val catalogVersion: Int,
)

/**
 * Parses the comma separated ABI list the build publishes (`BuildConfig.SUPPORTED_ABIS`), rejecting blanks and duplicates.
 * zh-CN: 解析构建发布的逗号分隔 ABI 列表 (`BuildConfig.SUPPORTED_ABIS`), 拒绝空项与重复项.
 */
internal fun parseSupportedAbis(value: String): List<String> {
    val abis = value.split(',').map(String::trim)
    require(abis.none(String::isEmpty) && abis.toSet().size == abis.size) { "Invalid ABI list: \"$value\"" }
    return abis
}

/**
 * Assembles the fields; [id], [engine] and [variant] come from the build resources, which the JVM tests keep equal to
 * [ThreeMacawSpeechContract] and the README sources.
 * zh-CN: 组装字段; [id], [engine] 与 [variant] 来自构建资源, JVM 测试保证其与 [ThreeMacawSpeechContract] 及 README 源一致.
 */
internal fun pluginRuntimeFields(
    name: String,
    description: String,
    author: String,
    id: String,
    engine: String,
    variant: String,
    versionName: String,
    versionCode: Long,
    versionDate: String,
    supportedAbis: String,
): PluginRuntimeFields = PluginRuntimeFields(
    name = name,
    description = description,
    instruction = ThreeMacawSpeechContract.INSTRUCTION_REFERENCE,
    author = author,
    id = id,
    engine = engine,
    variant = variant,
    versionName = versionName,
    versionCode = versionCode,
    versionDate = versionDate,
    supportedAbis = parseSupportedAbis(supportedAbis),
    requiredHostVersion = ThreeMacawSpeechContract.REQUIRED_HOST_VERSION,
    contractVersion = ThreeMacawSpeechContract.CONTRACT_VERSION,
    features = ThreeMacawSpeechContract.FEATURES,
    catalogVersion = ThreeMacawSpeechContract.CATALOG_VERSION,
)

/**
 * The identity answered by the INFO service (and by `ISpeechPlugin.getInfo` once roadmap P1.1 lands).
 * zh-CN: INFO 服务返回的身份 (路线图 P1.1 落地后 `ISpeechPlugin.getInfo` 也返回它).
 */
internal fun PluginRuntimeFields.toPluginInfo(): PluginInfo = PluginInfo().also { info ->
    info.name = name
    info.description = description
    info.instruction = instruction
    info.author = author
    info.id = id
    info.engine = engine
    info.variant = variant
    info.versionName = versionName
    info.versionCode = versionCode
    info.versionDate = versionDate
    // ABI specific: sherpa-onnx ships native code for exactly these four ABIs (roadmap D14).
    // zh-CN: 与 ABI 相关: sherpa-onnx 恰好为这四个 ABI 提供原生代码 (路线图 D14).
    info.supportedAbis = supportedAbis.toTypedArray()
    info.capabilities = Bundle().apply {
        putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiredHostVersion)
        putInt(ThreeMacawSpeechContract.CAPABILITY_CONTRACT_VERSION, contractVersion)
        // Roadmap D136: the base version stays 1 for the hosts before contract version 2, the highest one is separate.
        putInt(SpeechCapabilityKeys.MAX_CONTRACT_VERSION, ThreeMacawSpeechContract.MAX_CONTRACT_VERSION)
        putStringArray(ThreeMacawSpeechContract.CAPABILITY_FEATURES, features.toTypedArray())
        putInt(ThreeMacawSpeechContract.CAPABILITY_CATALOG_VERSION, catalogVersion)
    }
}

/**
 * Reads the localized description, build resources and the installed package version.
 * zh-CN: 读取本地化描述, 构建资源与已安装包的版本.
 */
internal fun Context.pluginRuntimeFields(): PluginRuntimeFields {
    val appContext = applicationContext
    val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    return pluginRuntimeFields(
        name = appContext.getString(R.string.app_name),
        description = appContext.getString(R.string.plugin_description),
        author = appContext.getString(R.string.plugin_author),
        id = appContext.getString(R.string.plugin_id),
        engine = appContext.getString(R.string.plugin_engine),
        variant = appContext.getString(R.string.plugin_variant),
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = packageInfo.versionCodeCompat(),
        versionDate = appContext.getString(R.string.plugin_version_date),
        supportedAbis = BuildConfig.SUPPORTED_ABIS,
    )
}

private fun PackageInfo.versionCodeCompat(): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return longVersionCode
    @Suppress("DEPRECATION")
    return versionCode.toLong()
}
