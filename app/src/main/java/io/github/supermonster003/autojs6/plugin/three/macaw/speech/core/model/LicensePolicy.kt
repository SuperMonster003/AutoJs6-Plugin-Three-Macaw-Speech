package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes

/**
 * Roadmap D11: a model whose `commercialUse` is `false` or `"unknown"` is installed only after its license was accepted,
 * either now (`acceptLicense: true` from a script, or the confirmation dialog of the screens in P5) or earlier for the
 * same model version and license expression; acceptances are remembered in the store. The user-facing notice is the
 * string named by `noticeKey` (10 languages, `LicenseNotices`).
 * zh-CN: 路线图 D11: `commercialUse` 为 `false` 或 `"unknown"` 的模型, 只有在其许可已被接受后才安装, 接受可以发生在本次 (脚本传入
 * `acceptLicense: true`, 或 P5 界面的确认对话框), 也可以是此前对同一模型版本与许可表达式的接受; 接受记录保存在存储中. 面向用户的提示为
 * `noticeKey` 指向的字符串 (10 种语言, `LicenseNotices`).
 */
class LicensePolicy(private val store: ModelStore) {

    /** Throws `MODEL_LICENSE_NOT_ACCEPTED` unless the license is commercial or accepted. zh-CN: 许可未被接受时抛出异常. */
    @Throws(ModelException::class)
    fun require(modelId: String, version: CatalogModelVersion, acceptLicense: Boolean) {
        val license = version.license
        if (!license.requiresAcceptance) return
        if (acceptLicense) {
            store.rememberLicense(modelId, version.versionCode, license.spdx)
            return
        }
        if (store.hasAcceptedLicense(modelId, version.versionCode, license.spdx)) return
        val reason = if (license.commercialUse == CommercialUse.UNKNOWN) "declares no license" else "is limited to non-commercial use"
        throw ModelException(
            SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED,
            "$modelId $reason (${license.spdx}); pass acceptLicense: true to accept its license",
        )
    }

    fun isAccepted(modelId: String, version: CatalogModelVersion): Boolean =
        !version.license.requiresAcceptance || store.hasAcceptedLicense(modelId, version.versionCode, version.license.spdx)
}
