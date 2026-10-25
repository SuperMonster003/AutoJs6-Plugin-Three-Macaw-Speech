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
class TtsSpeakers(val count: Int, val names: List<String> = emptyList(), val defaultId: Int = 0, val unavailable: Set<Int> = emptySet()) {

    data class Choice(val id: Int, val warning: SpeechWarning?)

    /**
     * [speaker] as a number or a name; [defaultId] when the request names none. A speaker in [unavailable] (one whose
     * language sherpa-onnx cannot read, [TtsSpeakerHints]) is `INVALID_OPTIONS`.
     * zh-CN: 以编号或名字解析 [speaker]; 未指定时为 [defaultId]. [unavailable] 中的说话人 (sherpa-onnx 读不了其语言, [TtsSpeakerHints]) 为
     * `INVALID_OPTIONS`.
     */
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
            return Choice(available(modelId, number), null)
        }
        val index = names.indexOf(text)
        if (index < 0 || index >= count) {
            val hint = if (names.isEmpty()) "it names no speakers; use a number from 0 to ${count - 1}" else "use a number from 0 to ${count - 1} or a name of its speakers"
            throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "$modelId has no speaker '$text': $hint")
        }
        return Choice(available(modelId, index), null)
    }

    private fun available(modelId: String, id: Int): Int {
        if (id in unavailable) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "Speaker $id of $modelId is not offered: sherpa-onnx cannot read its language for this model")
        return id
    }

    companion object {
        /** File of speaker names next to a model (one per line). zh-CN: 模型旁的说话人名字文件 (每行一个). */
        const val NAMES_FILE = "speakers.txt"

        fun parseNames(text: String): List<String> = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }
}

/**
 * What the voice table needs to know about one installed TTS model; [speakerLanguages] holds the languages of each
 * speaker, its first language first, when the speakers do not all speak every catalog language with the model's first
 * language first ([TtsSpeakerHints]; an empty list offers no voice for that speaker), [defaultSpeaker] the speaker used
 * when none is named.
 * zh-CN: 语音表所需的单个已安装 TTS 模型信息; 说话人并非都以模型首个语言为首说目录全部语言时, [speakerLanguages] 记录每个说话人的语言, 首要语言
 * 在前 ([TtsSpeakerHints]; 空列表表示该说话人不提供语音), [defaultSpeaker] 为未指定说话人时使用的说话人.
 */
data class TtsModelVoices(
    val modelId: String,
    val languages: List<String>,
    val speakers: Int,
    val sampleRate: Int?,
    val isDefault: Boolean,
    val speakerNames: List<String> = emptyList(),
    val speakerLanguages: List<List<String>> = emptyList(),
    val defaultSpeaker: Int = 0,
)

/**
 * Speaker facts the catalog does not carry (roadmap P4.1, D131): the catalog lists languages per model.
 * - Kokoro multi-lang v1.1 mixes 3 English speakers (0 to 2) with 100 Chinese ones (3 to 102). A device run showed the
 *   English speakers reading mixed Chinese and English text with English phonetics (SenseVoice CER 1.0 against 0.13 to
 *   0.28 for Chinese speakers), so their first language is English and the model's default speaker is 3.
 * - Kokoro v1.0 int8 has 54 speakers of nine languages (the names of its ONNX metadata). Only Chinese and English text
 *   goes through the lexicons; the Spanish, French, Hindi, Italian and Brazilian Portuguese speakers need the sherpa-onnx
 *   `lang` of their language ([engineLanguage], one engine instance per language), and each speaks only that language.
 *   The five Japanese speakers are not offered: sherpa-onnx 1.13.8 accepts `lang` only for en, es, fr, hi, it and
 *   pt-br. The default speaker is 3 (af_heart).
 * Models without an entry keep the catalog languages for every speaker and speaker 0 as the default. Pure Kotlin.
 * zh-CN: 目录未提供的说话人信息 (路线图 P4.1, D131): 目录按模型列出语言.
 * - Kokoro multi-lang v1.1 混合了 3 个英文说话人 (0 到 2) 与 100 个中文说话人 (3 到 102). 设备实测英文说话人以英文发音朗读中英混合文本 (SenseVoice
 *   CER 1.0, 中文说话人为 0.13 到 0.28), 因此其首要语言为英文, 模型的默认说话人为 3.
 * - Kokoro v1.0 int8 有九种语言的 54 个说话人 (名字取自其 ONNX 元数据). 只有中文与英文文字经词典; 西班牙语, 法语, 印地语, 意大利语与巴西葡萄牙语
 *   说话人需要传入其语言的 sherpa-onnx `lang` ([engineLanguage], 每种语言一个引擎实例), 且只说该语言. 5 个日语说话人不提供: sherpa-onnx 1.13.8
 *   的 `lang` 只接受 en, es, fr, hi, it 与 pt-br. 默认说话人为 3 (af_heart).
 * 未列入的模型对每个说话人沿用目录语言, 默认说话人为 0. 纯 Kotlin.
 */
object TtsSpeakerHints {

    const val KOKORO_V1_0 = "tts.kokoro.multi-lang.v1_0.int8"

    /** [languages] of a speaker (empty: not offered), the `lang` it needs (empty: none) and names. zh-CN: 说话人的语言, 所需 `lang` 与名字. */
    private class Hint(val defaultSpeaker: Int, val languages: (Int) -> List<String>, val engineLanguage: (Int) -> String = { "" }, val names: List<String> = emptyList())

    /** Kokoro v1.0 speakers by name prefix: catalog language (with region) and sherpa-onnx `lang`. zh-CN: Kokoro v1.0 说话人名字前缀. */
    private val KOKORO_V1_0_PREFIXES = mapOf(
        'a' to ("en" to ""), 'b' to ("en-GB" to ""), 'e' to ("es" to "es"), 'f' to ("fr" to "fr"), 'h' to ("hi" to "hi"),
        'i' to ("it" to "it"), 'p' to ("pt-BR" to "pt-br"), 'z' to ("zh" to ""),
    )

    private val KOKORO_V1_0_NAMES = listOf(
        "af_alloy", "af_aoede", "af_bella", "af_heart", "af_jessica", "af_kore", "af_nicole", "af_nova", "af_river", "af_sarah", "af_sky",
        "am_adam", "am_echo", "am_eric", "am_fenrir", "am_liam", "am_michael", "am_onyx", "am_puck", "am_santa",
        "bf_alice", "bf_emma", "bf_isabella", "bf_lily", "bm_daniel", "bm_fable", "bm_george", "bm_lewis",
        "ef_dora", "em_alex", "ff_siwis", "hf_alpha", "hf_beta", "hm_omega", "hm_psi", "if_sara", "im_nicola",
        "jf_alpha", "jf_gongitsune", "jf_nezumi", "jf_tebukuro", "jm_kumo", "pf_dora", "pm_alex", "pm_santa",
        "zf_xiaobei", "zf_xiaoni", "zf_xiaoxiao", "zf_xiaoyi", "zm_yunjian", "zm_yunxi", "zm_yunxia", "zm_yunyang", "em_santa",
    )

    private fun kokoroV1(speaker: Int): Pair<String, String>? = KOKORO_V1_0_NAMES.getOrNull(speaker)?.let { KOKORO_V1_0_PREFIXES[it[0]] }

    private val HINTS = mapOf(
        "tts.kokoro.multi-lang.v1_1" to Hint(defaultSpeaker = 3, languages = { speaker -> if (speaker < 3) listOf("en", "zh") else listOf("zh", "en") }),
        // Chinese speakers also read the English words of mixed text through the US lexicon.
        KOKORO_V1_0 to Hint(
            defaultSpeaker = 3,
            languages = { speaker -> kokoroV1(speaker)?.first?.let { if (it == "zh") listOf("zh", "en") else listOf(it) }.orEmpty() },
            engineLanguage = { speaker -> kokoroV1(speaker)?.second.orEmpty() },
            names = KOKORO_V1_0_NAMES,
        ),
    )

    fun defaultSpeaker(modelId: String, count: Int): Int = HINTS[modelId]?.defaultSpeaker?.takeIf { it < count } ?: 0

    /** The languages of every speaker, or empty when they all speak the catalog languages. zh-CN: 每个说话人的语言. */
    fun speakerLanguages(modelId: String, count: Int): List<List<String>> = HINTS[modelId]?.let { hint -> List(count) { hint.languages(it) } }.orEmpty()

    /** Speakers of [modelId] that are not offered (no language). zh-CN: [modelId] 中不提供的说话人. */
    fun unavailable(modelId: String, count: Int): Set<Int> = HINTS[modelId]?.let { hint -> (0 until count).filter { hint.languages(it).isEmpty() }.toSet() }.orEmpty()

    /** The sherpa-onnx Kokoro `lang` [speaker] needs; empty when the lexicons read its text. zh-CN: [speaker] 所需的 Kokoro `lang`. */
    fun engineLanguage(modelId: String, speaker: Int): String = HINTS[modelId]?.engineLanguage?.invoke(speaker).orEmpty()

    /** Speaker names the model does not ship in a `speakers.txt`. zh-CN: 模型未以 `speakers.txt` 提供的说话人名字. */
    fun names(modelId: String): List<String> = HINTS[modelId]?.names.orEmpty()
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
class TtsVoiceTable(
    private val models: List<TtsModelVoices>,
    catalogLanguageCodes: Collection<String> = emptyList(),
    /**
     * Voices the settings screen chose per language (language code -> voice name, roadmap P5.2); they win over the rules
     * above while they still exist. zh-CN: 设置页按语言选定的语音 (语言代码 -> 语音名, 路线图 P5.2); 仍存在时优先于上述规则.
     */
    preferredVoices: Map<String, String> = emptyMap(),
) {

    val voices: List<TtsVoice> = models.flatMap { model ->
        val tags = model.languages.map(::languageTag).distinct()
        (0 until maxOf(1, model.speakers)).flatMap { speaker ->
            val own = model.speakerLanguages.getOrNull(speaker)?.map(::languageTag)?.distinct()
            (own ?: tags).mapIndexed { index, tag -> voice(model, speaker, tag, primary = index == 0) }
        }
    }

    private val byName: Map<String, TtsVoice> = voices.associateBy { it.name }

    private val preferred: Map<String, TtsVoice> = preferredVoices.mapNotNull { (language, name) ->
        val voice = byName[name] ?: return@mapNotNull null
        val code = iso3Language(language) ?: return@mapNotNull null
        if (voice.iso3Language == code) code to voice else null
    }.toMap()

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
        preferred[lang]?.let { return it }
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
            "es" to "ES", "it" to "IT", "pt" to "BR", "ru" to "RU", "ar" to "SA", "hi" to "IN",
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
