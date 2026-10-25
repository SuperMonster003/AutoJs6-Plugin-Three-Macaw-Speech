package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.Context
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R

/**
 * The 10-language license notices named by the catalog's `license.noticeKey` (roadmap D11, Shared-Assets license
 * audit): shown before a download or import that needs `acceptLicense` (screens of roadmap P5). An unknown key, from a
 * newer catalog, falls back to the "unknown license" notice. `StringResourceParityTest` checks every key of the bundled
 * catalog against this table.
 * zh-CN: 目录 `license.noticeKey` 所指的 10 语言许可提示 (路线图 D11, Shared-Assets 许可审核): 在需要 `acceptLicense` 的下载或导入之前
 * 显示 (路线图 P5 的界面). 来自较新目录的未知键回退为 "许可未知" 提示. `StringResourceParityTest` 用本表核对内置目录的每个键.
 */
internal object LicenseNotices {

    val KEYS: Map<String, Int> = mapOf(
        "speech_model_license_commercial" to R.string.speech_model_license_commercial,
        "speech_model_license_non_commercial" to R.string.speech_model_license_non_commercial,
        "speech_model_license_unknown" to R.string.speech_model_license_unknown,
        // Catalog v3 (roadmap D131): weights with a license but training data the publisher does not name.
        "speech_model_license_unknown_data" to R.string.speech_model_license_unknown_data,
    )

    @StringRes
    fun resource(noticeKey: String?): Int = KEYS[noticeKey] ?: R.string.speech_model_license_unknown

    fun text(context: Context, noticeKey: String?): String = context.getString(resource(noticeKey))
}
