package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import java.util.Locale

/**
 * Adds punctuation to one finished utterance (roadmap D107); `core/runtime/PunctuationEngine` adapts sherpa-onnx's
 * `OfflinePunctuation` (ct-transformer) and `OnlinePunctuation` (cnn-bilstm, also restores case) to it.
 * zh-CN: 给一个已结束的语句加标点 (路线图 D107); `core/runtime/PunctuationEngine` 把 sherpa-onnx 的 `OfflinePunctuation` (ct-transformer) 与
 * `OnlinePunctuation` (cnn-bilstm, 同时恢复大小写) 适配到它.
 */
fun interface Punctuator {
    fun punctuate(text: String): String
}

/** An installed punctuation model as the rules of roadmap D107 see it. zh-CN: 路线图 D107 的规则所见的已安装标点模型. */
data class PunctuationModel(val modelId: String, val modelType: String, val languages: List<String>, val isDefault: Boolean = false)

/**
 * Which punctuation model serves a recognizer and each of its finals (roadmap D107). A recognizer that punctuates
 * itself (SenseVoice) gets none; the others get, for each of their languages, the installed punctuation model that
 * covers it, the more specific first (the English case model before the Chinese and English one), then the default,
 * then by id.
 * Each final picks by its own text: one with a Han character is Chinese, one whose letters are all ASCII Latin is
 * English, anything else (Cyrillic, Hangul, accented Latin) stays as recognized, and so does a text that already has
 * punctuation or whose language the recognizer does not speak. Pure Kotlin.
 * zh-CN: 哪个标点模型服务某个识别器及其每句最终结果 (路线图 D107). 自带标点的识别器 (SenseVoice) 不用; 其他识别器对其每种语言使用覆盖该语言的
 * 已安装标点模型, 语言更专的优先 (英文大小写模型先于中英文模型), 其次默认模型, 再按 ID. 每句最终结果按自身文本选择: 含汉字为中文, 字母全是 ASCII
 * 拉丁字母为英文, 其他 (西里尔字母, 谚文, 带变音符的拉丁字母) 保持识别原样; 已有标点或识别器不说该语言的文本也保持原样. 纯 Kotlin.
 */
object PunctuationRules {

    const val CT_TRANSFORMER = "ct-transformer"
    const val CNN_BILSTM = "cnn-bilstm"

    /** Recognizer types whose text already has punctuation. zh-CN: 输出文本已带标点的识别器类型. */
    val SELF_PUNCTUATING_TYPES = setOf("sense-voice")

    private val ORDER = compareBy<PunctuationModel> { it.languages.size }.thenByDescending { it.isDefault }.thenBy { it.modelId }

    /**
     * The punctuation models a recognizer may use, in order of preference: for each of its languages a final can be
     * written in (`zh`, `en`), the first model covering it. zh-CN: 识别器可用的标点模型, 按优先顺序: 对其每种可作为最终结果语言的语言 (`zh`, `en`),
     * 取覆盖该语言的第一个模型.
     */
    fun candidates(recognizerType: String, recognizerLanguages: List<String>, installed: List<PunctuationModel>): List<PunctuationModel> {
        if (recognizerType in SELF_PUNCTUATING_TYPES) return emptyList()
        val ordered = installed.sortedWith(ORDER)
        return recognizerLanguages.map(::base).filter { it in TEXT_LANGUAGES }.distinct()
            .mapNotNull { language -> ordered.firstOrNull { model -> model.languages.any { base(it) == language } } }
            .distinct().sortedWith(ORDER)
    }

    /** The model for one final [text] among [candidates], or null to keep the text. zh-CN: 一句最终结果 [text] 所用的模型, null 表示保持原文. */
    fun choose(text: String, recognizerLanguages: List<String>, candidates: List<PunctuationModel>): PunctuationModel? {
        if (hasPunctuation(text)) return null
        val language = textLanguage(text) ?: return null
        if (recognizerLanguages.none { base(it) == language }) return null
        return candidates.firstOrNull { model -> model.languages.any { base(it) == language } }
    }

    /** `zh`, `en` or null (see the class). zh-CN: `zh`, `en` 或 null (见类说明). */
    fun textLanguage(text: String): String? {
        var han = false
        var latin = false
        for (char in text) {
            when {
                isHan(char) -> han = true
                char in 'a'..'z' || char in 'A'..'Z' -> latin = true
                char.isLetter() -> return null
            }
        }
        return when {
            han -> "zh"
            latin -> "en"
            else -> null
        }
    }

    /**
     * Sentence punctuation in [text] (apostrophes and decimal points do not count): the recognizer punctuated it.
     * zh-CN: [text] 中的句读标点 (撇号与小数点不算): 识别器已自行加了标点.
     */
    fun hasPunctuation(text: String): Boolean = text.indices.any { index ->
        val char = text[index]
        when (char) {
            '.' -> !(text.getOrNull(index - 1)?.isDigit() == true && text.getOrNull(index + 1)?.isDigit() == true)
            else -> char in SENTENCE_MARKS
        }
    }

    /**
     * What the model reads: the case model was trained on lower case text, and the streaming transducers write English
     * in capitals. zh-CN: 模型读入的文本: 大小写模型以小写文本训练, 而流式 transducer 输出大写英文.
     */
    fun input(model: PunctuationModel, text: String): String = if (model.modelType == CNN_BILSTM) text.lowercase(Locale.ROOT) else text

    /**
     * The model's answer as the final text: in an English final the full width marks of the Chinese and English model
     * become ASCII ones; white space is collapsed.
     * zh-CN: 把模型的应答整理为最终文本: 英文结果中, 中英文模型输出的全角标点改为 ASCII 标点; 合并空白.
     */
    fun output(text: String, language: String): String {
        val out = if (language != "en") text else buildString(text.length) {
            for (char in text) {
                val ascii = FULL_WIDTH_TO_ASCII[char]
                if (ascii == null) append(char) else append(ascii).append(' ')
            }
        }
        return SpeechText.clean(out, punctuation = true)
    }

    private fun base(language: String): String = language.substringBefore('-').lowercase(Locale.ROOT)

    private fun isHan(char: Char): Boolean {
        val code = char.code
        return code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF || code in 0xF900..0xFAFF
    }

    /** The languages [textLanguage] tells. zh-CN: [textLanguage] 能分辨的语言. */
    private val TEXT_LANGUAGES = setOf("zh", "en")

    private const val SENTENCE_MARKS = ",?!;:\uff0c\u3002\uff1f\uff01\uff1b\uff1a\u3001"

    private val FULL_WIDTH_TO_ASCII = mapOf(
        '\uff0c' to ',', '\u3001' to ',', '\u3002' to '.', '\uff1f' to '?', '\uff01' to '!', '\uff1b' to ';', '\uff1a' to ':',
    )
}

/**
 * The punctuation of one dictation or file (roadmap D107): the candidate engines of its recognizer, chosen per final
 * by [PunctuationRules]. A model that fails leaves the text as recognized.
 * zh-CN: 一次听写或一个文件的标点 (路线图 D107): 其识别器的候选引擎, 按 [PunctuationRules] 逐句选择. 模型出错时保持识别原文.
 */
class UtterancePunctuation(
    private val recognizerLanguages: List<String>,
    private val engines: List<Pair<PunctuationModel, Punctuator>>,
    private val onFailure: (PunctuationModel, Throwable) -> Unit = { _, _ -> },
) : Punctuator {

    override fun punctuate(text: String): String {
        val model = PunctuationRules.choose(text, recognizerLanguages, engines.map { it.first }) ?: return text
        val language = PunctuationRules.textLanguage(text) ?: return text
        val engine = engines.first { it.first == model }.second
        val answer = try {
            engine.punctuate(PunctuationRules.input(model, text))
        } catch (e: RuntimeException) {
            onFailure(model, e)
            return text
        }
        return PunctuationRules.output(answer, language).ifEmpty { text }
    }
}
