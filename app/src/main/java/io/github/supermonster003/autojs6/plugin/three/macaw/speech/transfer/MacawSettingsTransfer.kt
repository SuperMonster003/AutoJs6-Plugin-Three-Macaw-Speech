package io.github.supermonster003.autojs6.plugin.three.macaw.speech.transfer

import android.content.SharedPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferencePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ListeningDuration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentKeywords
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.WakeSensitivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.KeyGesture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateStore
import org.autojs.plugin.common.api.PluginSettingsTransferProvider.Exported
import org.autojs.plugin.common.api.PluginSettingsTransferProvider.Imported
import org.autojs.plugin.common.api.SharedPreferencesSettingsTransfer
import org.autojs.plugin.common.api.SharedPreferencesSettingsTransfer.Key
import org.autojs.plugin.common.api.SharedPreferencesSettingsTransfer.Store
import org.autojs.plugin.common.api.SharedPreferencesSettingsTransfer.Type
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import org.autojs.plugin.common.api.AutoJs6PluginSettingsTransferContract as Contract

/**
 * The settings document of AutoJs6's settings transfer (contract v1, roadmap P10.1, host settings roadmap P18.1): the
 * user's choices in five SharedPreferences files, each a store named after its file, in the typed form of
 * [SharedPreferencesSettingsTransfer]. Models, the catalog cache, downloads, enrolled speakers, component switches (the
 * resident wake word, the voice input, the headset, the launcher icon) and records of this device (the notification
 * question, the tryout file tab, ignored update tags, the last update check, the listening deadline) stay out. Nothing
 * is a secret, so an export is never sensitive. Import is lenient: a known key whose value the settings screen could not
 * have saved (an endpoint outside the presets, an unknown key gesture, invalid wake words, ...) is skipped like an
 * unknown key, the rest is written with one commit per file. Pure JVM; [preferences] opens a file by name.
 * zh-CN: AutoJs6 设置迁移 (契约 v1, 路线图 P10.1, 宿主设置路线图 P18.1) 的设置文档: 五个 SharedPreferences 文件中的用户选择, 每个文件一个
 * 以文件名命名的 store, 采用 [SharedPreferencesSettingsTransfer] 的类型化形式. 模型, 目录缓存, 下载, 已登记说话人, 组件开关 (常驻唤醒,
 * 语音输入, 耳机, 启动器图标) 与本机记录 (通知询问, 试听文件标签, 已忽略的更新, 上次检查更新, 监听截止时刻) 不在其中. 没有秘密, 导出从不
 * 标为敏感. 导入宽松: 已知键的值若设置页不可能保存 (端点不在预设中, 未知的按键手势, 无效的唤醒词等), 与未知键一样计为跳过, 其余每个文件
 * 一次 commit 写入. 纯 JVM; [preferences] 按名称打开文件.
 */
internal class MacawSettingsTransfer(private val preferences: (String) -> SharedPreferences) {

    /** The listed keys that hold a value. zh-CN: 有值的已列出键. */
    fun exportSettings(): Exported {
        val exported = transfer(preferences(SpeechPreferences.FILE).all.keys).exportSettings()
        return Exported(exported.settings, exported.entries, sensitive = false)
    }

    /**
     * Writes the valid entries of [settings]; `skipped` counts unknown stores and keys, other tags and invalid values.
     * zh-CN: 写入 [settings] 中有效的条目; `skipped` 统计未知的 store 与键, 类型标签不符与无效的值.
     */
    @Throws(IOException::class)
    fun importSettings(settings: JSONObject): Imported {
        val accepted = JSONObject()
        var rejected = 0
        settings.keys().forEach { storeName ->
            val value = settings.opt(storeName)
            val rules = STORES[storeName]
            if (rules == null || value !is JSONObject) {
                // The helper counts what it does not know. zh-CN: 由辅助类统计它不认识的内容.
                accepted.put(storeName, value)
                return@forEach
            }
            val kept = JSONObject()
            value.keys().forEach { name ->
                val entry = value.opt(name)
                val rule = rules.ruleFor(name)
                val invalid = rule != null && entry is JSONObject && entry.opt(Contract.VALUE_KEY_TYPE) == rule.type.tag &&
                    !rule.accepts(entry.opt(Contract.VALUE_KEY_VALUE))
                if (invalid) rejected++ else kept.put(name, entry)
            }
            accepted.put(storeName, kept)
        }
        val speechKeys = (accepted.opt(SpeechPreferences.FILE) as? JSONObject)?.keys()?.asSequence()?.toList().orEmpty()
        val imported = transfer(speechKeys).importSettings(accepted)
        return Imported(imported.written, imported.skipped + rejected)
    }

    /** The helper over every store; [speechKeys] brings the keys of the speaker and voice families. zh-CN: 覆盖全部 store 的辅助类. */
    private fun transfer(speechKeys: Collection<String>) = SharedPreferencesSettingsTransfer(
        STORES.map { (name, rules) -> Store(name, preferences(name), rules.keys(if (name == SpeechPreferences.FILE) speechKeys else emptyList())) },
    )

    /** The type of a key and the values the settings screen can save. zh-CN: 键的类型及设置页可保存的值. */
    private class Rule(val type: Type, val accepts: (Any?) -> Boolean = { true })

    /** Fixed keys and key families (`prefix` plus a non-empty name). zh-CN: 固定键与键族 (前缀加非空名称). */
    private class StoreRules(val fixed: Map<String, Rule>, val families: Map<String, Rule> = emptyMap()) {
        fun ruleFor(name: String): Rule? = fixed[name] ?: families.entries.firstOrNull { (prefix, _) -> name.length > prefix.length && name.startsWith(prefix) }?.value

        fun keys(present: Collection<String>): List<Key> {
            val members = present.filter { it !in fixed && ruleFor(it) != null }.distinct().sorted()
            return fixed.map { (name, rule) -> Key(name, rule.type) } + members.map { Key(it, ruleFor(it)!!.type) }
        }
    }

    companion object {
        /** The stores of the document, by file name. zh-CN: 文档的 store, 以文件名命名. */
        val STORE_NAMES: List<String> get() = STORES.keys.toList()

        private val STORES: Map<String, StoreRules> = linkedMapOf(
            SpeechPreferences.FILE to StoreRules(
                fixed = linkedMapOf(
                    SpeechPreferences.KEY_ENDPOINT to Rule(Type.LONG) { value -> integral(value) in SpeechPreferencePolicy.ENDPOINT_MILLIS },
                    SpeechPreferences.KEY_VAD to Rule(Type.BOOL),
                    SpeechPreferences.KEY_SPEED to Rule(Type.FLOAT) { value ->
                        finite(value)?.let { it >= SpeechPreferencePolicy.MIN_SPEED && it <= SpeechPreferencePolicy.MAX_SPEED } == true
                    },
                    SpeechPreferences.KEY_IDLE_MINUTES to Rule(Type.INT) { value -> integral(value)?.let { it.toInt().toLong() == it && it.toInt() in SpeechPreferencePolicy.IDLE_MINUTES } == true },
                    SpeechPreferences.KEY_METERED to Rule(Type.BOOL),
                ),
                families = linkedMapOf(
                    SpeechPreferences.KEY_SPEAKER_PREFIX to Rule(Type.INT) { value -> integral(value)?.let { it in 0..Int.MAX_VALUE.toLong() } == true },
                    SpeechPreferences.KEY_VOICE_PREFIX to Rule(Type.STRING) { value -> value is String && value.isNotEmpty() },
                ),
            ),
            TriggerPreferences.FILE to StoreRules(
                linkedMapOf(
                    TriggerPreferences.KEY_FLOATING to Rule(Type.BOOL),
                    TriggerPreferences.KEY_FLOATING_RIGHT to Rule(Type.BOOL),
                    TriggerPreferences.KEY_FLOATING_Y to Rule(Type.FLOAT) { value -> finite(value)?.let { it >= 0f && it <= 1f } == true },
                    TriggerPreferences.KEY_GESTURE to Rule(Type.STRING) { value -> KeyGesture.entries.any { it.id == value } },
                ),
            ),
            ResidentPreferences.FILE to StoreRules(
                linkedMapOf(
                    ResidentPreferences.KEY_KEYWORDS to Rule(Type.STRING) { value -> value is String && ResidentKeywords.valid(value.split(ResidentPreferences.LINE)) },
                    ResidentPreferences.KEY_SENSITIVITY to Rule(Type.STRING) { value -> WakeSensitivity.entries.any { it.id == value } },
                    ResidentPreferences.KEY_DURATION to Rule(Type.STRING) { value -> ListeningDuration.entries.any { it.id == value } },
                    ResidentPreferences.KEY_CHARGING to Rule(Type.BOOL),
                    ResidentPreferences.KEY_READ_ALOUD to Rule(Type.BOOL),
                    ResidentPreferences.KEY_POWER_SAVING to Rule(Type.BOOL),
                    ResidentPreferences.KEY_ONLY_ENROLLED to Rule(Type.BOOL),
                ),
            ),
            AppearancePreferences.FILE to StoreRules(
                linkedMapOf(
                    AppearancePreferences.KEY_LANGUAGE to Rule(Type.STRING) { value -> value in AppearancePreferences.LANGUAGES },
                    AppearancePreferences.KEY_DARK_MODE to Rule(Type.STRING) { value -> value in AppearancePreferences.DARK_MODES },
                    AppearancePreferences.KEY_COLOR to Rule(Type.INT),
                ),
            ),
            AppUpdateStore.FILE to StoreRules(
                linkedMapOf(AppUpdateStore.KEY_AUTOMATIC to Rule(Type.BOOL)),
            ),
        )

        /** A JSON number without a fraction, as the helper reads it. zh-CN: 不带小数部分的 JSON 数字, 与辅助类的读法一致. */
        private fun integral(value: Any?): Long? = when (value) {
            is Int -> value.toLong()
            is Long -> value
            is Short -> value.toLong()
            is Byte -> value.toLong()
            is BigInteger -> if (value.bitLength() < 64) value.toLong() else null
            is BigDecimal -> runCatching { value.longValueExact() }.getOrNull()
            is Double -> value.takeIf { it == Math.rint(it) && it >= -9.223372036854776E18 && it < 9.223372036854776E18 }?.toLong()
            is Float -> integral(value.toDouble())
            else -> null
        }

        /** A JSON number as a finite float. zh-CN: 有限的浮点数. */
        private fun finite(value: Any?): Float? = (value as? Number)?.toDouble()?.toFloat()?.takeIf { it.isFinite() }
    }
}
