package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.util.Locale

/**
 * User-facing texts of the model screens; error codes become sentences, never raw exception messages (standalone
 * settings specification section 3).
 * zh-CN: 模型界面面向用户的文字; 错误码转为句子, 从不直接显示异常信息 (独立设置规范第 3 节).
 */
internal object HomeTexts {

    @StringRes
    fun kindTitle(kind: String): Int = when (kind) {
        "asr" -> R.string.model_kind_asr
        "kws" -> R.string.model_kind_kws
        "langid" -> R.string.model_kind_langid
        "punct" -> R.string.model_kind_punct
        "segment" -> R.string.model_kind_segment
        "speaker" -> R.string.model_kind_speaker
        "tagger" -> R.string.model_kind_tagger
        "tts" -> R.string.model_kind_tts
        else -> R.string.model_kind_vad
    }

    /** What the models of [kind] are for, when the kind name alone does not say it. zh-CN: [kind] 类模型的用途, 仅在类别名不足以说明时提供. */
    @StringRes
    fun kindAbout(kind: String): Int? = when (kind) {
        "langid" -> R.string.model_langid_about
        "punct" -> R.string.model_punct_about
        "segment" -> R.string.model_segment_about
        "speaker" -> R.string.model_speaker_about
        "tagger" -> R.string.model_tagger_about
        else -> null
    }

    @StringRes
    fun license(tag: LicenseTag): Int = when (tag) {
        LicenseTag.COMMERCIAL -> R.string.model_license_commercial
        LicenseTag.NON_COMMERCIAL -> R.string.model_license_non_commercial
        LicenseTag.UNKNOWN -> R.string.model_license_unknown
    }

    /**
     * Language names in the screen's locale (`yue` reads as Cantonese where the platform knows it); `zxx` (no
     * linguistic content: segmentation and tagging models, roadmap P8.4) is left out.
     * zh-CN: 以界面区域显示的语言名; 省略 `zxx` (无语言内容: 分割与标签模型, 路线图 P8.4).
     */
    fun languages(context: Context, codes: List<String>): String {
        val locale = context.resources.configuration.locales[0]
        return codes.filter { it != NO_LANGUAGE }.joinToString(", ") { code ->
            Locale.forLanguageTag(code).getDisplayName(locale).takeIf { it.isNotBlank() && it != code } ?: code
        }
    }

    private const val NO_LANGUAGE = "zxx"

    fun size(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)

    /** One line under the model name: languages, size and license expression. zh-CN: 模型名下的一行: 语言, 大小与许可表达式. */
    fun summary(context: Context, row: ModelRow): String {
        val parts = ArrayList<String>()
        languages(context, row.languages).takeIf { it.isNotEmpty() }?.let { parts += it }
        val bytes = if (row.state == RowState.Installed) row.installedBytes else row.sizeBytes.takeIf { it > 0 }
        bytes?.let { parts += size(context, it) }
        parts += row.licenseSpdx
        return parts.joinToString(" \u00b7 ")
    }

    @StringRes
    fun error(code: String, insufficientSpace: Boolean): Int = when {
        insufficientSpace -> R.string.error_insufficient_space
        code == SpeechErrorCodes.MODEL_DOWNLOAD_FAILED -> R.string.error_download_failed
        code == SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH -> R.string.error_checksum
        code == SpeechErrorCodes.UNSUPPORTED_FORMAT -> R.string.error_unsupported_format
        code == SpeechErrorCodes.LIMIT_EXCEEDED -> R.string.error_too_large
        code == SpeechErrorCodes.SESSION_BUSY -> R.string.error_model_in_use
        code == SpeechErrorCodes.MODEL_MISSING -> R.string.error_model_missing
        code == SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED -> R.string.error_license_not_accepted
        code == SpeechErrorCodes.INVALID_OPTIONS -> R.string.error_invalid_import
        code == SpeechErrorCodes.IO_FAILED -> R.string.error_io
        code == SpeechErrorCodes.PERMISSION_DENIED -> R.string.error_permission
        else -> R.string.error_generic
    }
}
