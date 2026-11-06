package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakePower
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * The rules of the resident wake word (roadmap P7.4, decided with the maintainer on 2026-10-21 as D69 - D74, derived as
 * D75 - D80). Pure Kotlin; the Android side lives in [ResidentWake] and [ResidentWakeService].
 * zh-CN: 常驻唤醒的规则 (路线图 P7.4, 2026-10-21 与维护者确定为 D69 - D74, 派生 D75 - D80). 纯 Kotlin; Android 部分在 [ResidentWake] 与
 * [ResidentWakeService].
 */
internal enum class WakeSensitivity(val id: String, val threshold: Float?) {
    /** Wakes more easily. zh-CN: 更容易唤醒. */
    HIGH("high", 0.15f),

    /** The model default (0.25). zh-CN: 模型默认值 (0.25). */
    MEDIUM("medium", null),

    /** Fewer false wakes. zh-CN: 更少误唤醒. */
    LOW("low", 0.35f),
    ;

    companion object {
        fun of(id: String?): WakeSensitivity = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

/**
 * The wake words (roadmap D70, D188): 1 to 10, each 1 to 32 characters, no repeats; "xiao ying xiao ying" and "Hey Macaw"
 * by default, the counterparts of "xiao shi xiao shi" / "Hey Stone" (3-Stone AI) and "xiao lu xiao lu" / "Hey Stove"
 * (3-Stove Agent). Whether the model can spell them is checked against the model when they are saved (D18).
 * zh-CN: 唤醒词 (路线图 D70, D188): 1 到 10 条, 每条 1 到 32 个字符, 不重复; 默认 "小鹦小鹦" 与 "Hey Macaw", 与 3-Stone AI 的 "小石小石" /
 * "Hey Stone" 和 3-Stove Agent 的 "小炉小炉" / "Hey Stove" 同形. 模型能否拼写在保存时按模型核对 (D18).
 */
internal object ResidentKeywords {
    const val MAX_KEYWORDS = 10
    const val MAX_CHARACTERS = 32
    val DEFAULT = listOf("\u5c0f\u9e66\u5c0f\u9e66", "Hey Macaw")

    fun valid(keywords: List<String>): Boolean = keywords.size in 1..MAX_KEYWORDS &&
        keywords.map { it.lowercase(Locale.ROOT) }.distinct().size == keywords.size &&
        keywords.all(::validKeyword)

    fun validKeyword(keyword: String): Boolean = keyword == keyword.trim() && keyword.codePointCount(0, keyword.length) in 1..MAX_CHARACTERS &&
        keyword.none { Character.isISOControl(it) || it.isSurrogate() }

    /** The keywords of the settings field (one per line, blank lines ignored), or null when the list breaks a rule. zh-CN: 设置输入框中的唤醒词. */
    fun parse(text: String): List<String>? = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.takeIf(::valid)
}

/**
 * What a hit of one wake word does (roadmap D186, P11.3.1): [OPEN] opens the assistant sheet (the 1.5.0 behavior and the
 * default of every older word), [SCRIPT] runs a script of AutoJs6 (D187), [BROADCAST] sends
 * `org.autojs.plugin.speech.action.RESIDENT_HIT` to AutoJs6 (D194), [NOTIFY] only posts "Tap to speak". The ids are the
 * contract's `SpeechContract.RESIDENT_ACTION_*`.
 * zh-CN: 单个唤醒词命中后做什么 (路线图 D186, P11.3.1): [OPEN] 打开助理弹层 (1.5.0 的行为, 也是旧词的默认值), [SCRIPT] 运行 AutoJs6 的脚本 (D187),
 * [BROADCAST] 向 AutoJs6 发送 `org.autojs.plugin.speech.action.RESIDENT_HIT` (D194), [NOTIFY] 只发 "点此说话" 通知. id 即契约的
 * `SpeechContract.RESIDENT_ACTION_*`.
 */
internal enum class ResidentAction(val id: String) {
    OPEN("open"),
    SCRIPT("script"),
    BROADCAST("broadcast"),
    NOTIFY("notify"),
    ;

    companion object {
        fun of(id: String?): ResidentAction? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One wake word and its action (roadmap P11.3.1). [script] (an absolute path, D224) and the optional [arguments] (handed
 * to the script as `execArgv.arguments`) belong to [ResidentAction.SCRIPT] only.
 * zh-CN: 一个唤醒词及其动作 (路线图 P11.3.1). [script] (绝对路径, D224) 与可选的 [arguments] (作为 `execArgv.arguments` 交给脚本) 只属于
 * [ResidentAction.SCRIPT].
 */
internal data class ResidentRule(
    val keyword: String,
    val action: ResidentAction = ResidentAction.OPEN,
    val script: String? = null,
    val arguments: String? = null,
)

/**
 * Checks and stores the rules of the resident wake words (roadmap P11.3.1, D188, D223, D224). Pure Kotlin on `org.json`.
 * zh-CN: 常驻唤醒规则的校验与存储格式 (路线图 P11.3.1, D188, D223, D224). 纯 Kotlin, 使用 `org.json`.
 */
internal object ResidentRules {
    const val MAX_SCRIPT_CHARS = 1024
    const val MAX_ARGUMENTS_CHARS = 1024

    val DEFAULT: List<ResidentRule> get() = ResidentKeywords.DEFAULT.map { ResidentRule(it) }

    /**
     * Why [rules] cannot be stored, or null: the first problem with its contract code ([Problem.limit] for too many or too
     * long, else `INVALID_OPTIONS`).
     * zh-CN: [rules] 不能保存的原因, 或 null: 第一个问题及其契约 code ([Problem.limit] 表示超量或超长, 否则为 `INVALID_OPTIONS`).
     */
    fun problem(rules: List<ResidentRule>): Problem? {
        if (rules.isEmpty()) return Problem(false, "at least one wake word is required")
        if (rules.size > ResidentKeywords.MAX_KEYWORDS) return Problem(true, "at most ${ResidentKeywords.MAX_KEYWORDS} wake words, not ${rules.size}")
        rules.forEachIndexed { i, rule ->
            val label = "rules[$i]"
            if (!ResidentKeywords.validKeyword(rule.keyword)) {
                val long = rule.keyword.codePointCount(0, rule.keyword.length) > ResidentKeywords.MAX_CHARACTERS
                return Problem(long, "$label.keyword must have 1 to ${ResidentKeywords.MAX_CHARACTERS} characters, no control characters and no surrounding spaces")
            }
            if (rule.action == ResidentAction.SCRIPT) {
                val script = rule.script ?: return Problem(false, "$label.script is required for the \"script\" action")
                if (script.length > MAX_SCRIPT_CHARS) return Problem(true, "$label.script exceeds $MAX_SCRIPT_CHARS characters")
                if (!validScript(script)) return Problem(false, "$label.script must be an absolute path in one line")
                rule.arguments?.let { arguments ->
                    if (arguments.length > MAX_ARGUMENTS_CHARS) return Problem(true, "$label.arguments exceeds $MAX_ARGUMENTS_CHARS characters")
                    if (arguments.contains(Char(0))) return Problem(false, "$label.arguments must not contain NUL")
                }
            } else if (rule.script != null || rule.arguments != null) {
                return Problem(false, "$label.script and $label.arguments belong to the \"script\" action, not \"${rule.action.id}\"")
            }
        }
        if (!ResidentKeywords.valid(rules.map { it.keyword })) return Problem(false, "the wake words must not repeat (case is ignored)")
        return null
    }

    data class Problem(val limit: Boolean, val message: String)

    fun valid(rules: List<ResidentRule>): Boolean = problem(rules) == null

    /** An absolute path in one line. zh-CN: 单行的绝对路径. */
    fun validScript(path: String): Boolean =
        path.startsWith("/") && path.length in 2..MAX_SCRIPT_CHARS && path.trim() == path && path.none { Character.isISOControl(it) }

    /** The `rules` preference (D223): a JSON array of `{ keyword, action, script?, arguments? }`. zh-CN: `rules` 偏好 (D223). */
    fun encode(rules: List<ResidentRule>): String = JSONArray().apply {
        rules.forEach { rule ->
            put(
                JSONObject().apply {
                    put(KEY_KEYWORD, rule.keyword)
                    put(KEY_ACTION, rule.action.id)
                    rule.script?.let { put(KEY_SCRIPT, it) }
                    rule.arguments?.let { put(KEY_ARGUMENTS, it) }
                },
            )
        }
    }.toString()

    /** The rules of [text], or null when it is not a valid list. zh-CN: [text] 中的规则; 不是有效列表时为 null. */
    fun decode(text: String?): List<ResidentRule>? {
        if (text == null) return null
        val array = runCatching { JSONArray(text) }.getOrNull() ?: return null
        val rules = ArrayList<ResidentRule>(array.length())
        for (i in 0 until array.length()) {
            val item = array.opt(i) as? JSONObject ?: return null
            val keyword = item.opt(KEY_KEYWORD) as? String ?: return null
            val action = ResidentAction.of(item.opt(KEY_ACTION) as? String) ?: return null
            val script = optionalString(item, KEY_SCRIPT) ?: return null
            val arguments = optionalString(item, KEY_ARGUMENTS) ?: return null
            rules += ResidentRule(keyword, action, script.value, arguments.value)
        }
        return rules.takeIf(::valid)
    }

    private class Optional(val value: String?)

    /** Absent (or JSON null) as an empty [Optional], a string as its value, anything else as null. zh-CN: 缺失为空值, 字符串为其值, 其他为 null. */
    private fun optionalString(item: JSONObject, key: String): Optional? = when (val value = item.opt(key)) {
        null, JSONObject.NULL -> Optional(null)
        is String -> Optional(value)
        else -> null
    }

    /**
     * The rules of the stored preferences (D223): [lines] (the 1.5.0 `keywords`, one per line) decides the words when it
     * is valid and differs from [stored] (an imported 1.5.0 document, or a downgrade and upgrade), each word keeping the
     * action [stored] gave it, a new one opening the sheet; otherwise [stored], else [lines], else the defaults.
     * zh-CN: 存储偏好中的规则 (D223): [lines] (1.5.0 的 `keywords`, 每行一个) 有效且与 [stored] 不一致时 (导入了 1.5.0 的文档, 或降级后再升级) 由它
     * 决定唤醒词, 每个词沿用 [stored] 给它的动作, 新词打开弹层; 否则用 [stored], 再否则用 [lines], 最后用默认值.
     */
    fun merge(lines: List<String>?, stored: List<ResidentRule>?): List<ResidentRule> {
        val words = lines?.takeIf(ResidentKeywords::valid)
        return when {
            stored != null && (words == null || words == stored.map { it.keyword }) -> stored
            words != null -> withKeywords(stored.orEmpty(), words)
            else -> DEFAULT
        }
    }

    /** [words] in order, each keeping the action of its old rule (case ignored), a new one opening the sheet. zh-CN: 按 [words] 顺序, 各词沿用旧规则的动作. */
    fun withKeywords(rules: List<ResidentRule>, words: List<String>): List<ResidentRule> =
        words.map { word -> rules.firstOrNull { it.keyword.equals(word, ignoreCase = true) }?.copy(keyword = word) ?: ResidentRule(word) }

    /** The rule of a hit's keyword: exact first, then without case. zh-CN: 命中词对应的规则: 先精确, 再不区分大小写. */
    fun ruleOf(rules: List<ResidentRule>, keyword: String?): ResidentRule? =
        keyword?.let { word -> rules.firstOrNull { it.keyword == word } ?: rules.firstOrNull { it.keyword.equals(word, ignoreCase = true) } }

    private const val KEY_KEYWORD = "keyword"
    private const val KEY_ACTION = "action"
    private const val KEY_SCRIPT = "script"
    private const val KEY_ARGUMENTS = "arguments"
}

/**
 * How long the resident wake word stays on once turned on (roadmap D67, D77); it then turns its switch off.
 * zh-CN: 常驻唤醒开启后保持多久 (路线图 D67, D77); 到时关闭开关.
 */
internal enum class ListeningDuration(val id: String, val minutes: Int?) {
    MINUTES_10("10", 10),
    MINUTES_30("30", 30),
    MINUTES_60("60", 60),
    ALWAYS("always", null),
    ;

    /** When a switch turned on at [now] (epoch milliseconds) turns off, or null when it stays on. zh-CN: 在 [now] 打开的开关何时关闭. */
    fun deadline(now: Long): Long? = minutes?.let { now + it * 60_000L }

    companion object {
        fun of(id: String?): ListeningDuration = entries.firstOrNull { it.id == id } ?: ALWAYS
    }
}

/**
 * The preferences of the resident wake word; the switch itself is the component state (roadmap D75). [deadline] (epoch
 * milliseconds, wall clock, so that it holds across a reboot) ends the current listening time: turning the switch on, or
 * choosing another [duration] while it is on, sets it; null keeps it on. [readAloud] is D69, [powerSaving] D123 (on by
 * default; what it does is [ResidentPower]), [onlyEnrolled] D139 (a hit counts only when an enrolled speaker says it).
 * zh-CN: 常驻唤醒的偏好; 开关本身是组件状态 (路线图 D75). [deadline] (墙上时间毫秒, 重启后仍有效) 结束当前的监听时长: 打开开关, 或开着时改选
 * [duration] 会设置它; null 表示一直开着. [readAloud] 即 D69, [powerSaving] 即 D123 (默认开; 其做法见 [ResidentPower]), [onlyEnrolled] 即 D139
 * (只有已注册的说话人说出唤醒词才算命中).
 */
internal data class ResidentSettings(
    val rules: List<ResidentRule> = ResidentRules.DEFAULT,
    val sensitivity: WakeSensitivity = WakeSensitivity.MEDIUM,
    val duration: ListeningDuration = ListeningDuration.ALWAYS,
    val chargingOnly: Boolean = false,
    val readAloud: Boolean = true,
    val deadline: Long? = null,
    val powerSaving: Boolean = true,
    val onlyEnrolled: Boolean = false,
    /** "Send a broadcast" also as an implicit broadcast (Q170 (c), D225); off sends it to AutoJs6 only. zh-CN: "发系统广播" 同时发隐式广播; 关时只发给 AutoJs6. */
    val broadcastToAll: Boolean = false,
) {
    init {
        require(ResidentRules.valid(rules))
        require(deadline == null || deadline > 0)
    }

    /** The wake words in order. zh-CN: 按顺序的唤醒词. */
    val keywords: List<String> get() = rules.map { it.keyword }

    /** Other words, each keeping its action (P11.3.1). zh-CN: 换词, 各词沿用其动作. */
    fun withKeywords(words: List<String>): ResidentSettings = copy(rules = ResidentRules.withKeywords(rules, words))

    /** The listening time of a switch turned on at [now]. zh-CN: 在 [now] 打开开关时的监听时长. */
    fun turnedOn(now: Long): ResidentSettings = copy(deadline = duration.deadline(now))

    /**
     * Another duration: an enabled switch counts it from [now], a disabled one only remembers it for the next start.
     * zh-CN: 改选时长: 开着时从 [now] 起算, 关着时只记住选择, 下次打开才计时.
     */
    fun withDuration(value: ListeningDuration, enabled: Boolean, now: Long): ResidentSettings =
        copy(duration = value, deadline = if (enabled) value.deadline(now) else deadline)

    /** The listening time of an enabled switch is over. zh-CN: 开着的开关的监听时长已到. */
    fun expired(now: Long): Boolean = deadline != null && now >= deadline

    /**
     * A running wake session must reopen to follow [next]: other wake words, sensitivity or power saving (D70, D123), which
     * take effect at once that way; the other options are rules the service applies to the running session.
     * zh-CN: 运行中的唤醒会话须重开才能跟随 [next]: 唤醒词, 灵敏度或省电模式改变 (D70, D123), 重开后立即生效; 其他选项由服务对运行中的会话应用规则.
     */
    fun reopensFor(next: ResidentSettings): Boolean = keywords != next.keywords || sensitivity != next.sensitivity || powerSaving != next.powerSaving
}

/**
 * The power saving mode of the resident wake word (roadmap P8.1, D123, D128): on, a voice gate keeps the quiet room away
 * from the keyword spotter and the microphone hands its audio over in [SAVING_FRAME_MILLIS] steps; off, the wake session
 * runs as in 1.2.0. A change of the switch reopens the wake session at once (`ResidentWake.restart`).
 * zh-CN: 常驻唤醒的省电模式 (路线图 P8.1, D123, D128): 开启时语音门把安静的房间声挡在唤醒词检测器之外, 麦克风以 [SAVING_FRAME_MILLIS] 的步长交出
 * 音频; 关闭时唤醒会话与 1.2.0 相同. 切换开关会立即重开唤醒会话 (`ResidentWake.restart`).
 */
internal object ResidentPower {
    const val SAVING_FRAME_MILLIS = 200

    val SAVING = WakePower(voiceGate = true, frameMillis = SAVING_FRAME_MILLIS)

    fun of(powerSaving: Boolean): WakePower = if (powerSaving) SAVING else WakePower.OFF

    /**
     * The measured variants of P8.1.1, by name, for the debug switch and the device tests: `off`, `gate` (a), `vad` (a
     * with the silero VAD as well), `step` (b) and `both` (c); null for any other name.
     * zh-CN: P8.1.1 实测的各做法, 按名字取, 供调试开关与设备测试使用: `off`, `gate` (a), `vad` (a 再加 silero VAD), `step` (b), `both` (c);
     * 其他名字为 null.
     */
    fun variant(name: String?): WakePower? = when (name) {
        "off" -> WakePower.OFF
        "gate" -> WakePower(voiceGate = true, frameMillis = WakePower.DEFAULT_FRAME_MILLIS)
        "vad" -> WakePower(voiceGate = true, frameMillis = WakePower.DEFAULT_FRAME_MILLIS, silero = true)
        "step" -> WakePower(voiceGate = false, frameMillis = SAVING_FRAME_MILLIS)
        "both" -> SAVING
        else -> null
    }
}

/**
 * What an enabled resident wake word does now (roadmap D77). Every start, every visible screen that resumes it (D74) and
 * every power change asks this first, so the listening time also ends while the service is not running.
 * zh-CN: 开着的常驻唤醒此刻该做什么 (路线图 D77). 每次启动, 每个恢复它的可见界面 (D74) 与每次电源变化都先问这里, 因此服务不运行时监听时长同样会结束.
 */
internal enum class ResidentGate {
    /** The listening time is over: turn the switch off, withdraw the notification. zh-CN: 监听时长已到: 关闭开关, 撤下通知. */
    EXPIRED,

    /** Only while charging, and unplugged: no wake session until the power comes back. zh-CN: 只在充电时监听且已拔电: 接电前不开唤醒会话. */
    PAUSED,
    LISTEN,
    ;

    companion object {
        fun of(settings: ResidentSettings, now: Long, plugged: Boolean): ResidentGate = when {
            settings.expired(now) -> EXPIRED
            settings.chargingOnly && !plugged -> PAUSED
            else -> LISTEN
        }
    }
}

/**
 * Roadmap D74 with D77: what a visible screen, a reboot or an update does with the switch.
 * zh-CN: 路线图 D74 与 D77: 可见界面, 重启或更新时如何处理开关.
 */
internal enum class ResidentResume {
    NOTHING,
    TURN_OFF,
    RESUME,
    ;

    companion object {
        /**
         * An enabled switch resumes (or, after a reboot or an update, reminds), unless its listening time ran out meanwhile.
         * zh-CN: 开着的开关恢复 (重启或更新后为提醒), 除非监听时长在此期间已到.
         */
        fun of(enabled: Boolean, settings: ResidentSettings, now: Long): ResidentResume = when {
            !enabled -> NOTHING
            settings.expired(now) -> TURN_OFF
            else -> RESUME
        }
    }
}

/**
 * Why the switch is usable or not (roadmap D78); the settings row names the first missing piece.
 * zh-CN: 开关是否可用及原因 (路线图 D78); 设置行写明缺少的第一项.
 */
internal enum class ResidentAvailability {
    MICROPHONE,
    NO_WAKE_MODEL,
    NO_RECOGNITION_MODEL,
    READY,
    ;

    companion object {
        fun of(microphoneGranted: Boolean, wakeModels: Int, recognitionModels: Int): ResidentAvailability = when {
            !microphoneGranted -> MICROPHONE
            wakeModels == 0 -> NO_WAKE_MODEL
            recognitionModels == 0 -> NO_RECOGNITION_MODEL
            else -> READY
        }
    }
}

/**
 * What a hit does (roadmap D69, D71, D79).
 * zh-CN: 命中后做什么 (路线图 D69, D71, D79).
 */
internal enum class HitRoute {
    /** The sheet is visible: a hit may be its own read-aloud text. zh-CN: 弹层可见: 命中可能来自它朗读的文字. */
    IGNORE,

    /** Open the sheet through [io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.AssistantEntry]. zh-CN: 打开弹层. */
    OPEN,

    /** The plugin may not open a screen from the background: post "Tap to speak". zh-CN: 插件无法从后台打开界面: 发 "点此说话" 通知. */
    NOTIFY,
    ;

    companion object {
        /** API level of the background activity start restriction. zh-CN: 后台启动 Activity 限制的 API 级别. */
        const val BACKGROUND_START_API = 29

        fun of(sheetVisible: Boolean, assistantActive: Boolean, overlayGranted: Boolean, sdk: Int): HitRoute = when {
            sheetVisible -> IGNORE
            canOpen(assistantActive, overlayGranted, sdk) -> OPEN
            else -> NOTIFY
        }

        /**
         * The default assistant shows a session from the background, the overlay permission allows a background activity
         * start, and Android 9 and lower have no such restriction (D71).
         * zh-CN: 默认助理可从后台显示会话, 悬浮窗权限允许后台启动 Activity, Android 9 及以下没有该限制 (D71).
         */
        fun canOpen(assistantActive: Boolean, overlayGranted: Boolean, sdk: Int): Boolean =
            assistantActive || overlayGranted || sdk < BACKGROUND_START_API

        /** D71 / D78: turning the switch on asks once for the overlay permission when nothing else opens the sheet. zh-CN: 开启时询问一次悬浮窗权限. */
        fun askOverlay(assistantActive: Boolean, overlayGranted: Boolean, sdk: Int): Boolean = !canOpen(assistantActive, overlayGranted, sdk)
    }
}

/**
 * The script path of a file picked on the settings screen (roadmap D224): only the external storage provider names a
 * file path (`primary:Scripts/a.js` on the primary volume, `1234-ABCD:a.js` on an SD card); any other document, or a
 * folder, has none and the user types the path instead.
 * zh-CN: 设置页所选文件的脚本路径 (路线图 D224): 只有外部存储提供方能给出文件路径 (主卷为 `primary:Scripts/a.js`, SD 卡为 `1234-ABCD:a.js`); 其他文档
 * 或文件夹没有路径, 由用户直接输入.
 */
internal object ResidentScriptPaths {
    const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun fromDocument(authority: String?, documentId: String?, primaryRoot: String): String? {
        if (authority != EXTERNAL_STORAGE_AUTHORITY || documentId == null) return null
        val volume = documentId.substringBefore(':', "")
        val relative = documentId.substringAfter(':', "").trim('/')
        if (volume.isEmpty() || relative.isEmpty() || volume.contains('/')) return null
        val root = if (volume.equals("primary", ignoreCase = true)) primaryRoot.trimEnd('/') else "/storage/$volume"
        return "$root/$relative".takeIf(ResidentRules::validScript)
    }
}

/**
 * The step a hit takes for the action of its wake word (roadmap P11.3.1 - P11.3.3, D187, D194, D220 - D222).
 * zh-CN: 命中按其唤醒词的动作采取的步骤 (路线图 P11.3.1 - P11.3.3, D187, D194, D220 - D222).
 */
internal enum class HitStep {
    /** The sheet is visible: its read-aloud text may hold any wake word, so no action runs (D79). zh-CN: 弹层可见: 其朗读的文字可能含任意唤醒词, 不执行动作. */
    IGNORE,

    /** Opens the sheet (D71). zh-CN: 打开弹层. */
    OPEN_SHEET,

    /** Posts "Tap to speak": the action [ResidentAction.NOTIFY], or an [ResidentAction.OPEN] that may not open a screen. zh-CN: 发 "点此说话". */
    SPEAK_NOTICE,

    /** AutoJs6 6.8.1+ runs the script through its PLUGIN receiver, from the background as well (D220). zh-CN: AutoJs6 6.8.1+ 经其 PLUGIN 接收器运行脚本. */
    RUN_HOST,

    /** An older AutoJs6: its `RunIntentActivity` opens the script, allowed like the sheet (D222). zh-CN: 较旧的 AutoJs6: 经 `RunIntentActivity` 运行. */
    RUN_ACTIVITY,

    /** An older AutoJs6 and no screen may open: "Tap to run" (D222, D74 style). zh-CN: 较旧的 AutoJs6 且不能打开界面: "点击运行" 通知. */
    RUN_NOTICE,

    /** Sends `RESIDENT_HIT` to AutoJs6 (D194, D225). zh-CN: 向 AutoJs6 发送 `RESIDENT_HIT`. */
    BROADCAST,
    ;

    companion object {
        /**
         * [canOpen] is [HitRoute.canOpen]; [hostReceiver] tells whether AutoJs6 has the run receiver of D220 (D221).
         * zh-CN: [canOpen] 即 [HitRoute.canOpen]; [hostReceiver] 表示 AutoJs6 是否有 D220 的运行接收器 (D221).
         */
        fun of(action: ResidentAction, sheetVisible: Boolean, canOpen: Boolean, hostReceiver: Boolean): HitStep = when {
            sheetVisible -> IGNORE
            action == ResidentAction.OPEN -> if (canOpen) OPEN_SHEET else SPEAK_NOTICE
            action == ResidentAction.NOTIFY -> SPEAK_NOTICE
            action == ResidentAction.BROADCAST -> BROADCAST
            hostReceiver -> RUN_HOST
            canOpen -> RUN_ACTIVITY
            else -> RUN_NOTICE
        }
    }
}

/**
 * Roadmap D76: a wake session opened from the background (after the power came back) that reports a silenced or refused
 * microphone within [CHECK_MILLIS] got no microphone there, and waits for a tap ("Tap to continue listening"). Later, or
 * after an open from a visible screen, the same warning is kept: another app (a call) may hold the microphone for a while.
 * zh-CN: 路线图 D76: 从后台 (接电后) 打开的唤醒会话若在 [CHECK_MILLIS] 内报告麦克风被静音或被拒, 说明后台拿不到麦克风, 等待点按 ("点此继续监听").
 * 之后的同类警告, 或从可见界面打开的会话收到的警告, 都保留会话: 其他应用 (通话) 可能暂时占用麦克风.
 */
internal object ResidentBlock {
    const val CHECK_MILLIS = 15_000L

    fun blocked(code: String?, openedInBackgroundAt: Long?, now: Long): Boolean =
        code == SpeechErrorCodes.PERMISSION_DENIED && openedInBackgroundAt != null && now - openedInBackgroundAt < CHECK_MILLIS
}

/**
 * Why the resident wake word stopped by itself; the switch stays on and the next visible start retries, except for
 * [CONFLICT], which turns it off (two enabled resident listeners would break the convention, D75).
 * zh-CN: 常驻唤醒自行停止的原因; 开关保持开启, 下次可见时重试; [CONFLICT] 除外, 它会关闭开关 (两个已启用的常驻监听会破坏约定, D75).
 */
internal enum class ResidentStop {
    CONFLICT,
    MICROPHONE,
    MODEL,
    KEYWORDS,
    ENDED,
    ;

    companion object {
        /** A speech contract error code of an exception or an error event. zh-CN: 异常或错误事件中的契约错误码. */
        fun of(code: String?): ResidentStop = when (code) {
            SpeechErrorCodes.PERMISSION_DENIED -> MICROPHONE
            SpeechErrorCodes.MODEL_MISSING -> MODEL
            SpeechErrorCodes.INVALID_OPTIONS -> KEYWORDS
            else -> ENDED
        }
    }
}

/**
 * Another app whose resident listener is on (3-Stone AI D31, roadmap D75); [settingsClass] is the activity its meta-data
 * names, when valid.
 * zh-CN: 常驻监听已开启的其他应用 (3-Stone AI D31, 路线图 D75); [settingsClass] 是其 meta-data 指定且有效的 Activity.
 */
internal data class ResidentListenerApp(val packageName: String, val label: String, val settingsClass: String?)

internal object ResidentListenerApps {
    /**
     * The other apps, one entry each, sorted by label; a relative class name (".Settings") is resolved against the package.
     * zh-CN: 其他应用, 每个一条, 按名称排序; 相对类名 (".Settings") 按包名补全.
     */
    fun others(found: List<ResidentListenerApp>, ownPackage: String): List<ResidentListenerApp> = found
        .filter { it.packageName != ownPackage }
        .distinctBy { it.packageName }
        .map { app ->
            app.copy(
                settingsClass = app.settingsClass?.trim()?.let { name -> if (name.startsWith(".")) app.packageName + name else name }?.takeIf { it.matches(CLASS_NAME) },
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

    private val CLASS_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+")
}

/**
 * Roadmap D69: which engine reads the recognized text of a sheet opened by the wake word, and in which language.
 * zh-CN: 路线图 D69: 由唤醒词打开的弹层用哪个引擎, 以什么语言朗读识别文字.
 */
internal object ReadAloudPolicy {
    /**
     * The language of the text: the model's (detected or only) language, else Chinese when the text has Han characters,
     * else English.
     * zh-CN: 文字的语言: 模型 (检测到的或唯一的) 语言, 否则文字含汉字时为中文, 否则为英文.
     */
    fun language(modelLanguage: String?, text: String): String =
        modelLanguage?.takeIf { it.isNotBlank() } ?: if (text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }) "zh" else "en"

    /**
     * 3-Macaw Speech's own engine when one of its installed TTS models speaks [language] (primary subtags compared),
     * else the system default engine.
     * zh-CN: 已装 TTS 模型中有说 [language] 的 (按主语言子标签比较) 时用 3-Macaw Speech 自己的引擎, 否则用系统默认引擎.
     */
    fun ownEngine(modelLanguages: List<List<String>>, language: String): Boolean {
        val wanted = primary(language)
        return modelLanguages.any { languages -> languages.any { primary(it) == wanted } }
    }

    private fun primary(tag: String): String = tag.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
}
