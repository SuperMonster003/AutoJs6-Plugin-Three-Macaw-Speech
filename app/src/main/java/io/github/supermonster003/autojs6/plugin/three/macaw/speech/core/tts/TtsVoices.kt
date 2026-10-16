package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechWarning
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.util.Locale
import java.util.MissingResourceException

/**
 * The speakers of one TTS model: [count] as the engine (or the catalog) reports it, [names] from the model's
 * `speakers.txt` (one name per line, line `n` is speaker `n`; AISHELL-3 has one). A request names a speaker by its
 * number or by its name; a model with one speaker ignores any other choice with a warning (roadmap P4.1), a number or
 * name the model does not have is `INVALID_OPTIONS`. Pure Kotlin.
 * zh-CN: 一个 TTS 模型的说话人: [count] 为引擎 (或目录) 报告的数量, [names] 来自模型的 `speakers.txt` (每行一个名字, 第 `n` 行即说话人 `n`;
 * AISHELL-3 带有该文件). 请求以编号或名字指定说话人; 只有一个说话人的模型忽略其他选择并给出警告 (路线图 P4.1), 模型没有的编号或名字为
 * `INVALID_OPTIONS`. 纯 Kotlin.
 */
class TtsSpeakers(val count: Int, val names: List<String> = emptyList(), val defaultId: Int = 0) {

    data class Choice(val id: Int, val warning: SpeechWarning?)

    /** [speaker] as a number or a name; [defaultId] when the request names none. zh-CN: 以编号或名字解析 [speaker]; 未指定时为 [defaultId]. */
    fun resolve(modelId: String, speaker: String?): Choice {
        if (speaker == null) return Choice(defaultId.takeIf { it in 0 until count } ?: 0, null)
        val text = speaker.trim()
        val number = text.toIntOrNull()?.takeIf { text.all(Char::isDigit) }
        if (count <= 1) {
            if (number == 0 || (names.isNotEmpty() && text == names[0])) return Choice(0, null)
            return Choice(0, SpeechWarning(SpeechErrorCodes.INVALID_OPTIONS, "$modelId has a single speaker; speaker '$text' was ignored"))
        }
        if (number != null) {
            if (number >= count) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "$modelId has speakers 0 to ${count - 1}, got $number")
            return Choice(number, null)
        }
        val index = names.indexOf(text)
        if (index < 0 || index >= count) {
            val hint = if (names.isEmpty()) "it names no speakers; use a number from 0 to ${count - 1}" else "use a number from 0 to ${count - 1} or a name of its speakers.txt"
            throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "$modelId has no speaker '$text': $hint")
        }
        return Choice(index, null)
    }

    companion object {
        /** File of speaker names next to a model (one per line). zh-CN: 模型旁的说话人名字文件 (每行一个). */
        const val NAMES_FILE = "speakers.txt"

        fun parseNames(text: String): List<String> = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }
}

/**
 * What the voice table needs to know about one installed TTS model; [speakerLanguages] holds the first language of each
 * speaker when the speakers do not all speak the model's first language first ([TtsSpeakerHints]), [defaultSpeaker] the
 * speaker used when none is named.
 * zh-CN: 语音表所需的单个已安装 TTS 模型信息; 说话人的首要语言并非都是模型首个语言时, [speakerLanguages] 记录每个说话人的首要语言
 * ([TtsSpeakerHints]), [defaultSpeaker] 为未指定说话人时使用的说话人.
 */
data class TtsModelVoices(
    val modelId: String,
    val languages: List<String>,
    val speakers: Int,
    val sampleRate: Int?,
    val isDefault: Boolean,
    val speakerNames: List<String> = emptyList(),
    val speakerLanguages: List<String> = emptyList(),
    val defaultSpeaker: Int = 0,
)

/**
 * Speaker facts the catalog does not carry (roadmap P4.1): the catalog lists languages per model, while Kokoro
 * multi-lang v1.1 mixes 3 English speakers (0 to 2) with 100 Chinese ones (3 to 102). A device run showed the English
 * speakers reading mixed Chinese and English text with English phonetics (SenseVoice CER 1.0 against 0.13 to 0.28 for
 * Chinese speakers), so their first language is English and the model's default speaker is 3. Models without an entry
 * keep the catalog languages for every speaker and speaker 0 as the default. Pure Kotlin.
 * zh-CN: 目录未提供的说话人信息 (路线图 P4.1): 目录按模型列出语言, 而 Kokoro multi-lang v1.1 混合了 3 个英文说话人 (0 到 2) 与 100 个中文说话人
 * (3 到 102). 设备实测英文说话人以英文发音朗读中英混合文本 (SenseVoice CER 1.0, 中文说话人为 0.13 到 0.28), 因此其首要语言为英文, 模型的默认
 * 说话人为 3. 未列入的模型对每个说话人沿用目录语言, 默认说话人为 0. 纯 Kotlin.
 */
object TtsSpeakerHints {

    private class Hint(val defaultSpeaker: Int, val language: (Int) -> String)

    private val HINTS = mapOf(
        "tts.kokoro.multi-lang.v1_1" to Hint(defaultSpeaker = 3) { speaker -> if (speaker < 3) "en" else "zh" },
    )

    fun defaultSpeaker(modelId: String, count: Int): Int = HINTS[modelId]?.defaultSpeaker?.takeIf { it < count } ?: 0

    /** The first language of every speaker, or empty when they all speak the model's first language first. zh-CN: 每个说话人的首要语言. */
    fun speakerLanguages(modelId: String, count: Int): List<String> = HINTS[modelId]?.let { hint -> List(count) { hint.language(it) } }.orEmpty()
}

/**
 * One voice of the system TTS engine: a speaker of a model in one locale ([languageTag], with its ISO 639-2 and ISO
 * 3166 alpha-3 codes for `TextToSpeechService`).
 * zh-CN: 系统 TTS 引擎的一个语音: 某模型的一个说话人在一个区域设置下 ([languageTag], 以及 `TextToSpeechService` 使用的 ISO 639-2 与 ISO 3166
 * alpha-3 代码).
 */
data class TtsVoice(
    val name: String,
    val modelId: String,
    val speakerId: Int,
    val languageTag: String,
    val iso3Language: String,
    val iso3Country: String,
    val quality: Int,
    /** The voice in the model's first language, named `<modelId>/<speakerId>`. zh-CN: 模型首个语言的语音, 名为 `<modelId>/<speakerId>`. */
    val primary: Boolean,
)

/**
 * The voices of the system TTS engine (roadmap D19, appendix D Q6): every installed TTS model contributes one voice per
 * speaker and language. The voice in the model's first language is `<modelId>/<speakerId>` (what `speech.speak` asks
 * for), the same speaker in the model's other languages is `<modelId>/<speakerId>/<languageTag>`
 * (`tts.vits.melo.zh-en/0/en-US`), so every voice name is unique and every language finds a voice of its own. Locales
 * come from the catalog languages (`zh` -> `zh-CN`, `en` -> `en-US`). [availability] answers the `TextToSpeech`
 * language codes: a language of an installed model is available (with the country when it matches), a language only
 * models of the catalog speak is `LANG_MISSING_DATA`, anything else `LANG_NOT_SUPPORTED`. [defaultVoice] prefers the
 * default TTS model, then a voice in its first language, then the matching country, then the installed order, then the
 * model's default speaker (0, or what [TtsSpeakerHints] says), then the lowest speaker. A speaker's first language may
 * differ from the model's ([TtsModelVoices.speakerLanguages]): its voice in that language is then the one without a
 * suffix. Pure Kotlin.
 * zh-CN: 系统 TTS 引擎的语音 (路线图 D19, 附录 D Q6): 每个已安装的 TTS 模型为每个说话人与每种语言提供一个语音. 模型首个语言的语音名为
 * `<modelId>/<speakerId>` (即 `speech.speak` 请求的名字), 同一说话人在模型其他语言下的语音名为 `<modelId>/<speakerId>/<languageTag>`
 * (`tts.vits.melo.zh-en/0/en-US`), 因此语音名唯一, 每种语言都有自己的语音. 区域设置取自目录语言 (`zh` -> `zh-CN`, `en` -> `en-US`).
 * [availability] 返回 `TextToSpeech` 的语言代码: 已安装模型的语言为可用 (国家一致时带国家), 只有目录模型支持的语言为 `LANG_MISSING_DATA`,
 * 其他为 `LANG_NOT_SUPPORTED`. [defaultVoice] 依次优先默认 TTS 模型, 该语言为首要语言的语音, 国家一致, 安装顺序, 模型的默认说话人 (0, 或
 * [TtsSpeakerHints] 指定的说话人), 编号最小的说话人. 说话人的首要语言可以不同于模型首个语言 ([TtsModelVoices.speakerLanguages]): 此时该说话人在
 * 其首要语言下的语音不带后缀. 纯 Kotlin.
 */
class TtsVoiceTable(private val models: List<TtsModelVoices>, catalogLanguageCodes: Collection<String> = emptyList()) {

    val voices: List<TtsVoice> = models.flatMap { model ->
        val tags = model.languages.map(::languageTag).distinct()
        (0 until maxOf(1, model.speakers)).flatMap { speaker ->
            val first = model.speakerLanguages.getOrNull(speaker)?.let(::languageTag)?.takeIf { it in tags }
            val ordered = if (first == null) tags else listOf(first) + (tags - first)
            ordered.mapIndexed { index, tag -> voice(model, speaker, tag, primary = index == 0) }
        }
    }

    private val byName: Map<String, TtsVoice> = voices.associateBy { it.name }

    /** ISO 639-2 code -> catalog code of every language a catalog model speaks. zh-CN: 目录模型所说的每种语言. */
    private val catalogLanguages: Map<String, String> = catalogLanguageCodes.mapNotNull { code -> iso3Language(code)?.let { it to Locale.forLanguageTag(code).language } }.toMap()

    fun isEmpty(): Boolean = voices.isEmpty()

    /**
     * The voice of [name]; besides the listed names a speaker may be written by its name (`<modelId>/<speakerName>`),
     * as `speech.speak` does when the script names one.
     * zh-CN: [name] 对应的语音; 除列出的名字外, 说话人也可写作名字 (`<modelId>/<speakerName>`), 与脚本给出说话人名字时 `speech.speak` 的写法一致.
     */
    fun voice(name: String): TtsVoice? {
        byName[name]?.let { return it }
        val modelId = name.substringBeforeLast('/', "")
        val speaker = name.substringAfterLast('/')
        val model = models.firstOrNull { it.modelId == modelId } ?: return null
        val index = model.speakerNames.indexOf(speaker).takeIf { it in 0 until model.speakers } ?: return null
        return byName[voiceName(modelId, index)]
    }

    /** `TextToSpeech.LANG_*` for [language] (ISO 639 code, 2 or 3 letters) and [country]. zh-CN: [language] 与 [country] 的 `TextToSpeech.LANG_*`. */
    fun availability(language: String?, country: String?): Int {
        val lang = language?.let(::iso3Language) ?: return LANG_NOT_SUPPORTED
        val matching = voices.filter { it.iso3Language == lang }
        if (matching.isEmpty()) return if (lang in catalogLanguages) LANG_MISSING_DATA else LANG_NOT_SUPPORTED
        val wanted = country?.let(::iso3Country)
        return if (wanted != null && matching.any { it.iso3Country == wanted }) LANG_COUNTRY_AVAILABLE else LANG_AVAILABLE
    }

    /** The voice the engine uses for [language] / [country] when the client names none; null when none speaks it. zh-CN: 客户端未指定语音时使用的语音. */
    fun defaultVoice(language: String?, country: String?): TtsVoice? {
        if (language.isNullOrEmpty()) {
            val model = models.firstOrNull { it.isDefault } ?: models.firstOrNull() ?: return null
            return byName[voiceName(model.modelId, model.defaultSpeaker)]
        }
        val lang = iso3Language(language) ?: return null
        val wanted = country?.let(::iso3Country)
        val order = models.withIndex().associate { it.value.modelId to it.index }
        val defaults = models.filter { it.isDefault }.map { it.modelId }.toSet()
        val defaultSpeakers = models.associate { it.modelId to it.defaultSpeaker }
        return voices.filter { it.iso3Language == lang }.sortedWith(
            compareByDescending<TtsVoice> { it.modelId in defaults }
                .thenByDescending { it.primary }
                .thenByDescending { wanted != null && it.iso3Country == wanted }
                .thenBy { order[it.modelId] ?: Int.MAX_VALUE }
                .thenByDescending { it.speakerId == defaultSpeakers[it.modelId] }
                .thenBy { it.speakerId },
        ).firstOrNull()
    }

    /**
     * `<iso3 language>-<iso3 country>` of every installed language (the `CHECK_TTS_DATA` answer), with the common
     * regions of Chinese and English as well: the system settings compare the device locale with this list, and the
     * models read their language whatever the region (`onIsLanguageAvailable` answers `LANG_AVAILABLE` for them).
     * zh-CN: 每种已安装语言的 ISO3 语言-国家代码 (`CHECK_TTS_DATA` 的应答), 另含中文与英文的常见地区: 系统设置以设备区域设置与此列表比较, 而模型
     * 不论地区都能朗读其语言 (`onIsLanguageAvailable` 对它们返回 `LANG_AVAILABLE`).
     */
    fun availableLocales(): List<String> {
        val own = voices.map { "${it.iso3Language}-${it.iso3Country}".trimEnd('-') }
        val regions = voices.map { it.languageTag.substringBefore('-') }.distinct().flatMap { language ->
            val lang = iso3Language(language) ?: return@flatMap emptyList()
            REGION_VARIANTS[language].orEmpty().mapNotNull { region -> iso3Country(region)?.let { "$lang-$it" } }
        }
        return (own + regions).distinct()
    }

    /** The same for the languages only catalog models speak. zh-CN: 只有目录模型支持的语言. */
    fun missingLocales(): List<String> {
        val installed = voices.map { it.iso3Language }.toSet()
        return catalogLanguages.filterKeys { it !in installed }.map { (lang, code) ->
            DEFAULT_REGIONS[code]?.let(::iso3Country)?.let { "$lang-$it" } ?: lang
        }.sorted()
    }

    private fun voice(model: TtsModelVoices, speaker: Int, tag: String, primary: Boolean): TtsVoice {
        val locale = Locale.forLanguageTag(tag)
        return TtsVoice(
            name = if (primary) voiceName(model.modelId, speaker) else "${voiceName(model.modelId, speaker)}/$tag",
            modelId = model.modelId,
            speakerId = speaker,
            languageTag = tag,
            iso3Language = iso3Language(locale.language) ?: locale.language,
            iso3Country = iso3Country(locale.country) ?: "",
            quality = quality(model.sampleRate),
            primary = primary,
        )
    }

    companion object {
        // Values of android.speech.tts.TextToSpeech and Voice, repeated so this table stays pure Kotlin.
        const val LANG_COUNTRY_AVAILABLE = 1
        const val LANG_AVAILABLE = 0
        const val LANG_MISSING_DATA = -1
        const val LANG_NOT_SUPPORTED = -2
        const val QUALITY_LOW = 200
        const val QUALITY_NORMAL = 300
        const val QUALITY_HIGH = 400

        /** The region a catalog language is reported with. zh-CN: 目录语言报告时附带的地区. */
        val DEFAULT_REGIONS = mapOf(
            "zh" to "CN", "en" to "US", "ja" to "JP", "ko" to "KR", "yue" to "HK", "de" to "DE", "fr" to "FR",
            "es" to "ES", "it" to "IT", "pt" to "BR", "ru" to "RU", "ar" to "SA",
        )

        /** Further regions listed for `CHECK_TTS_DATA`. zh-CN: `CHECK_TTS_DATA` 另外列出的地区. */
        val REGION_VARIANTS = mapOf(
            "zh" to listOf("CN", "TW", "HK", "SG", "MO"),
            "en" to listOf("US", "GB", "AU", "CA", "IN", "IE", "NZ", "SG", "ZA"),
        )

        fun voiceName(modelId: String, speakerId: Int): String = "$modelId/$speakerId"

        /** `zh` -> `zh-CN`; a tag that already has a region stays. zh-CN: `zh` -> `zh-CN`; 已带地区的标签不变. */
        fun languageTag(language: String): String {
            val locale = Locale.forLanguageTag(language)
            if (locale.country.isNotEmpty()) return locale.toLanguageTag()
            val region = DEFAULT_REGIONS[locale.language] ?: return locale.toLanguageTag()
            return Locale.Builder().setLocale(locale).setRegion(region).build().toLanguageTag()
        }

        fun quality(sampleRate: Int?): Int = when {
            sampleRate == null -> QUALITY_NORMAL
            sampleRate >= 22050 -> QUALITY_HIGH
            sampleRate >= 16000 -> QUALITY_NORMAL
            else -> QUALITY_LOW
        }

        /** ISO 639-2 code of a 2 or 3 letter language code; `cmn` reads as Chinese. zh-CN: 2 或 3 字母语言代码的 ISO 639-2 代码. */
        fun iso3Language(language: String): String? {
            val code = language.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
            if (code.isEmpty()) return null
            if (code == "cmn") return "zho"
            return try {
                Locale.forLanguageTag(code).isO3Language.takeIf { it.isNotEmpty() }
            } catch (_: MissingResourceException) {
                null
            }
        }

        fun iso3Country(country: String): String? {
            val code = country.trim().uppercase(Locale.ROOT)
            if (code.isEmpty()) return null
            if (code.length == 3) return code
            return try {
                Locale.forLanguageTag("und-$code").isO3Country.takeIf { it.isNotEmpty() }
            } catch (_: MissingResourceException) {
                null
            }
        }
    }
}
