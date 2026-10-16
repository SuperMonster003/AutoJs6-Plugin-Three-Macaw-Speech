package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.autojs.plugin.speech.api.SpeechModelKeys

/**
 * Bundle encoding of the speech contract on the plugin side (`docs/dev/speech-plugin-protocol-v1.md`): every answer
 * and completion carries `contractVersion`, failures carry only `errorCode` and `errorMessage` (English, at most 4 KiB
 * of UTF-8), and calls that cannot answer with a Bundle throw `CODE: detail`.
 * zh-CN: 插件侧语音契约的 Bundle 编码 (`docs/dev/speech-plugin-protocol-v1.md`): 每个应答与完成回调都带 `contractVersion`, 失败只带
 * `errorCode` 与 `errorMessage` (英文, 不超过 4 KiB UTF-8), 无法以 Bundle 应答的调用抛出 `CODE: detail`.
 */
internal object SpeechBundles {

    fun answer(build: Bundle.() -> Unit = {}): Bundle = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CONTRACT_VERSION)
        build()
    }

    fun failure(code: String, message: String?): Bundle = answer {
        putString(SpeechContract.KEY_ERROR_CODE, code)
        putString(SpeechContract.KEY_ERROR_MESSAGE, bounded(message ?: code))
    }

    fun failure(error: ModelException): Bundle = failure(error.code, error.message)

    /** The exception a Binder call throws for [code] (decoded by the host's `SpeechErrorCodes.decode`). */
    fun thrown(code: String, message: String?): RuntimeException {
        val text = SpeechErrorCodes.encode(code, bounded(message ?: ""))
        return if (code == SpeechErrorCodes.INVALID_OPTIONS || code == SpeechErrorCodes.MODEL_MISSING) IllegalArgumentException(text) else IllegalStateException(text)
    }

    fun thrown(error: ModelException): RuntimeException = thrown(error.code, error.message)

    /**
     * One model as `SpeechModelKeys`. The contract's `commercialUse` is a Boolean, so the catalog value `"unknown"`
     * reads as false, which also means "needs `acceptLicense`" (roadmap D11).
     * zh-CN: 以 `SpeechModelKeys` 表示的单个模型. 契约中的 `commercialUse` 为布尔值, 目录取值 `"unknown"` 因此读作 false, 同样表示需要
     * `acceptLicense` (路线图 D11).
     */
    fun model(entry: ModelEntry): Bundle = Bundle().apply {
        putString(SpeechModelKeys.ID, entry.id)
        putString(SpeechModelKeys.KIND, entry.kind)
        putString(SpeechModelKeys.MODEL_TYPE, entry.modelType)
        putString(SpeechModelKeys.NAME, entry.name)
        putStringArray(SpeechModelKeys.LANGUAGES, entry.languages.toTypedArray())
        putLong(SpeechModelKeys.SIZE_BYTES, entry.sizeBytes)
        entry.installedBytes?.let { if (entry.installed) putLong(SpeechModelKeys.INSTALLED_BYTES, it) }
        putBoolean(SpeechModelKeys.INSTALLED, entry.installed)
        putBoolean(SpeechModelKeys.IS_DEFAULT, entry.isDefault)
        putBoolean(SpeechModelKeys.RECOMMENDED, entry.recommended)
        putInt(SpeechModelKeys.VERSION, entry.version)
        putBundle(SpeechModelKeys.LICENSE, Bundle().apply {
            putString(SpeechModelKeys.LICENSE_SPDX, entry.licenseSpdx)
            putBoolean(SpeechModelKeys.LICENSE_COMMERCIAL_USE, entry.commercialUse == CommercialUse.ALLOWED)
            entry.noticeUrl?.let { putString(SpeechModelKeys.LICENSE_NOTICE_URL, it) }
        })
        entry.speakers?.let { putInt(SpeechModelKeys.SPEAKERS, it) }
        entry.sampleRate?.let { putInt(SpeechModelKeys.SAMPLE_RATE, it) }
        putString(SpeechModelKeys.SOURCE, entry.source)
    }

    /** Truncates to the protocol's message ceiling on a character boundary. zh-CN: 按协议上限在字符边界截断. */
    fun bounded(message: String): String {
        if (message.toByteArray(Charsets.UTF_8).size <= SpeechLimits.MAX_ERROR_MESSAGE_BYTES) return message
        var end = minOf(message.length, SpeechLimits.MAX_ERROR_MESSAGE_BYTES)
        while (end > 0 && message.substring(0, end).toByteArray(Charsets.UTF_8).size > SpeechLimits.MAX_ERROR_MESSAGE_BYTES - 3) end--
        if (end > 0 && message[end - 1].isHighSurrogate()) end--
        return message.substring(0, end) + "..."
    }
}
