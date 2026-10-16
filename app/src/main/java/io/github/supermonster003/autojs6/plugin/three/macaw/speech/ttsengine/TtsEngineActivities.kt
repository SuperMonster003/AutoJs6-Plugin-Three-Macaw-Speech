package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ttsengine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime

/**
 * Answers `TextToSpeech.Engine.ACTION_CHECK_TTS_DATA` (roadmap P4.2): the system TTS settings enable the sample and the
 * language list only for locales an engine lists here (`<iso3 language>-<iso3 country>`, [TtsVoiceTable.availableLocales]);
 * the languages only catalog models speak are listed as unavailable. No side effects; exported without a permission
 * because the system settings start it.
 * zh-CN: 应答 `TextToSpeech.Engine.ACTION_CHECK_TTS_DATA` (路线图 P4.2): 系统 TTS 设置只对引擎在此列出的区域设置 (`<iso3 语言>-<iso3 国家>`,
 * [TtsVoiceTable.availableLocales]) 启用试听与语言列表; 只有目录模型支持的语言列为不可用. 无副作用; 由系统设置启动, 因此无权限导出.
 */
class CheckVoiceDataActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val table = SpeechRuntime.get(this).voiceTable()
        val available = ArrayList(table.availableLocales())
        val result = Intent()
            .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, available)
            .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, ArrayList(table.missingLocales()))
        setResult(if (available.isEmpty()) TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL else TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, result)
        finish()
    }
}

/**
 * Answers `TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT` with the sample the system settings play for a language
 * (extras `language`, `country`, `variant`): a sentence in Chinese or English, the languages of the catalog models;
 * other languages get `LANG_NOT_SUPPORTED`, and the settings use their own sample. No side effects.
 * zh-CN: 以系统设置为某语言播放的示例文本应答 `TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT` (附加项 `language`, `country`, `variant`): 中文或英文的
 * 一句话, 即目录模型的语言; 其他语言返回 `LANG_NOT_SUPPORTED`, 系统设置改用自己的示例. 无副作用.
 */
class GetSampleTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val language = intent?.getStringExtra("language")?.let(TtsVoiceTable::iso3Language)
        val sample = SAMPLES[language]
        if (sample == null) {
            setResult(TextToSpeech.LANG_NOT_SUPPORTED)
        } else {
            setResult(TextToSpeech.LANG_AVAILABLE, Intent().putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, sample))
        }
        finish()
    }

    private companion object {
        val SAMPLES = mapOf(
            "zho" to "\u4f60\u597d, \u8fd9\u662f\u79bb\u7ebf\u795e\u7ecf\u7f51\u7edc\u8bed\u97f3\u5408\u6210\u7684\u793a\u4f8b.",
            "eng" to "Hello, this is an example of offline neural speech synthesis.",
        )
    }
}
