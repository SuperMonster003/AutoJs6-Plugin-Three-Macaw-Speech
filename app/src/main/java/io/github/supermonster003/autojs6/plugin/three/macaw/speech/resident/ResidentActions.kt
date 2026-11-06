package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteCallbackList
import android.util.Log
import org.autojs.plugin.speech.api.ISpeechResidentObserver
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as C

/**
 * One hit of a resident wake word as it is acted on and delivered (roadmap D194, D226): [keyword] is the word of the
 * rule, [matchedText] the text the keyword spotter reported, [timeMillis] the wall clock of the hit.
 * zh-CN: 一次常驻唤醒词命中, 用于执行动作与投递 (路线图 D194, D226): [keyword] 为规则中的词, [matchedText] 为唤醒词检测器报告的文字, [timeMillis] 为命中时的墙上时间.
 */
internal data class ResidentHit(val keyword: String, val matchedText: String?, val action: ResidentAction, val timeMillis: Long) {

    /** The observer Bundle of contract version 3 (`ISpeechResidentObserver.onHit`). zh-CN: 契约版本 3 的观察者 Bundle. */
    fun bundle(): Bundle = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CALL_VERSION_3)
        putString(SpeechResultKeys.KEYWORD, keyword)
        matchedText?.let { putString(SpeechResultKeys.MATCHED_TEXT, it) }
        putString(SpeechResultKeys.ACTION, action.id)
        putLong(SpeechResultKeys.TIME_MILLIS, timeMillis)
    }
}

/**
 * The actions that reach AutoJs6 (roadmap P11.3.2, P11.3.3, D187, D194, D220 - D222, D225).
 *
 * - "Run script" on AutoJs6 6.8.1+: an explicit ordered broadcast [ACTION_RUN] to the host's receiver, which only
 *   packages signed like the host may send ([PLUGIN_PERMISSION]); explicit broadcasts reach a manifest receiver from the
 *   background on every Android version, as for 3-Chime Alarm. The script sees `execArgv.keyword`, `matchedText`,
 *   `arguments`, `timeMillis` and `source` ([SOURCE]).
 * - On an older AutoJs6 (no such receiver, D221): its exported `RunIntentActivity` with the path and the same extras,
 *   started like the sheet or from a "Tap to run" notification (D222).
 * - "Send broadcast": [ACTION_HIT] to the host package only, never implicit (D225), for its broadcast tasks.
 *
 * zh-CN: 到达 AutoJs6 的动作 (路线图 P11.3.2, P11.3.3, D187, D194, D220 - D222, D225).
 * - AutoJs6 6.8.1+ 的 "运行脚本": 向宿主接收器发送显式有序广播 [ACTION_RUN], 只有与宿主同签名的包能发送 ([PLUGIN_PERMISSION]); 显式广播在各 Android
 *   版本上都能从后台到达清单接收器, 与 3-Chime Alarm 相同. 脚本看到 `execArgv.keyword`, `matchedText`, `arguments`, `timeMillis` 与 `source` ([SOURCE]).
 * - 较旧的 AutoJs6 (没有该接收器, D221): 用其导出的 `RunIntentActivity` 带路径与同样的 extras, 按弹层的规则启动, 或经 "点击运行" 通知 (D222).
 * - "发系统广播": [ACTION_HIT] 只发给宿主包, 从不发隐式广播 (D225), 供其广播型任务使用.
 */
internal object ResidentHost {
    private const val TAG = "MacawResident"

    const val HOST_PACKAGE: String = C.HOST_PACKAGE_NAME
    const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"

    /** The host's run receiver (6.8.1+, D220). zh-CN: 宿主的运行接收器 (6.8.1+, D220). */
    const val ACTION_RUN = "org.autojs.autojs6.action.SPEECH_RESIDENT_RUN"

    /** The "Send broadcast" action (D194). zh-CN: "发系统广播" 动作 (D194). */
    const val ACTION_HIT = "org.autojs.plugin.speech.action.RESIDENT_HIT"

    const val RUN_VERSION = 1
    const val EXTRA_CONTRACT_VERSION = "contractVersion"
    const val EXTRA_PATH = "path"
    const val EXTRA_KEYWORD = "keyword"
    const val EXTRA_MATCHED_TEXT = "matchedText"
    const val EXTRA_ARGUMENTS = "arguments"
    const val EXTRA_TIME_MILLIS = "timeMillis"
    const val EXTRA_SOURCE = "source"
    const val SOURCE = "resident-wake"
    const val RESULT_LAUNCHED = "launched"
    const val RESULT_SKIPPED = "skipped"

    /** AutoJs6's script runner of every version (D222). zh-CN: 各版本 AutoJs6 的脚本运行入口 (D222). */
    const val RUN_INTENT_ACTIVITY = "org.autojs.autojs.external.open.RunIntentActivity"

    /** The host runs scripts through [ACTION_RUN] (D221: by the receiver, not by a version number). zh-CN: 宿主经 [ACTION_RUN] 运行脚本. */
    fun hasRunReceiver(context: Context): Boolean = runCatching {
        context.packageManager.queryBroadcastReceivers(Intent(ACTION_RUN).setPackage(HOST_PACKAGE), 0).isNotEmpty()
    }.getOrDefault(false)

    private fun Intent.withHit(hit: ResidentHit, rule: ResidentRule): Intent = apply {
        putExtra(EXTRA_PATH, rule.script)
        putExtra(EXTRA_KEYWORD, hit.keyword)
        hit.matchedText?.let { putExtra(EXTRA_MATCHED_TEXT, it) }
        rule.arguments?.let { putExtra(EXTRA_ARGUMENTS, it) }
        putExtra(EXTRA_TIME_MILLIS, hit.timeMillis)
        putExtra(EXTRA_SOURCE, SOURCE)
    }

    /**
     * Asks AutoJs6 6.8.1+ to run [rule]'s script; [failed] runs on the main thread when the host answers that it did not
     * (the reason, e.g. `missing`), or does not answer at all (`unanswered`).
     * zh-CN: 请 AutoJs6 6.8.1+ 运行 [rule] 的脚本; 宿主应答未运行 (原因, 例如 `missing`) 或完全没有应答 (`unanswered`) 时在主线程调用 [failed].
     */
    fun run(context: Context, hit: ResidentHit, rule: ResidentRule, failed: (String) -> Unit) {
        val intent = Intent(ACTION_RUN)
            .setPackage(HOST_PACKAGE)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            .putExtra(EXTRA_CONTRACT_VERSION, RUN_VERSION)
            .withHit(hit, rule)
        val answer = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val extras = getResultExtras(false)
                val launched = resultCode == Activity.RESULT_OK && extras?.getBoolean(RESULT_LAUNCHED) == true
                val reason = if (resultCode != Activity.RESULT_OK || extras == null) "unanswered" else extras.getString(RESULT_SKIPPED) ?: "failed"
                Log.i(TAG, "Run script: " + if (launched) "launched" else reason)
                if (!launched) failed(reason)
            }
        }
        try {
            context.sendOrderedBroadcast(intent, PLUGIN_PERMISSION, answer, Handler(Looper.getMainLooper()), Activity.RESULT_CANCELED, null, null)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Run script: not sent (${e.javaClass.simpleName})")
            failed("unsent")
        }
    }

    /** `RunIntentActivity` of an older AutoJs6 (D222). zh-CN: 较旧 AutoJs6 的 `RunIntentActivity` (D222). */
    fun runIntent(hit: ResidentHit, rule: ResidentRule): Intent = Intent()
        .setComponent(ComponentName(HOST_PACKAGE, RUN_INTENT_ACTIVITY))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .withHit(hit, rule)

    /** "Send broadcast" (D194, D225): explicit, to AutoJs6 only. zh-CN: "发系统广播" (D194, D225): 显式, 只发给 AutoJs6. */
    fun broadcast(context: Context, hit: ResidentHit, toAll: Boolean = false) {
        for (intent in hitIntents(hit, toAll)) {
            runCatching { context.sendBroadcast(intent) }
                .onSuccess { Log.i(TAG, "Hit broadcast sent (${if (intent.`package` == null) "implicit" else "AutoJs6"})") }
                .onFailure { Log.w(TAG, "Hit broadcast not sent (${it.javaClass.simpleName})") }
        }
    }

    /**
     * The explicit broadcast to AutoJs6, and with [toAll] (Q170 (c), D225) an implicit one as well, which only receivers
     * registered at run time get on Android 8+.
     * zh-CN: 发给 AutoJs6 的显式广播; [toAll] 时另发一条隐式广播 (Android 8+ 只有运行时注册的接收器收得到).
     */
    fun hitIntents(hit: ResidentHit, toAll: Boolean): List<Intent> {
        fun intent() = Intent(ACTION_HIT)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            .putExtra(EXTRA_KEYWORD, hit.keyword)
            .putExtra(EXTRA_TIME_MILLIS, hit.timeMillis)
            .also { i -> hit.matchedText?.let { i.putExtra(EXTRA_MATCHED_TEXT, it) } }
        val toHost = intent().setPackage(HOST_PACKAGE)
        return if (toAll) listOf(toHost, intent()) else listOf(toHost)
    }
}

/**
 * The observers of `speech.resident.on('hit')` (roadmap D194, D226): every hit that is acted on reaches them, whatever
 * its action; a dead observer leaves by itself.
 * zh-CN: `speech.resident.on('hit')` 的观察者 (路线图 D194, D226): 每个被执行的命中都投递给它们, 无论其动作; 已死亡的观察者自动移除.
 */
internal object ResidentObservers {
    private val observers = RemoteCallbackList<ISpeechResidentObserver>()

    fun register(observer: ISpeechResidentObserver): Boolean = observers.register(observer)

    fun unregister(observer: ISpeechResidentObserver): Boolean = observers.unregister(observer)

    /** Observers registered now, for device tests. zh-CN: 当前注册的观察者数, 供设备测试使用. */
    val count: Int get() = observers.registeredCallbackCount

    @Synchronized
    fun deliver(hit: ResidentHit) {
        val bundle = hit.bundle()
        val n = observers.beginBroadcast()
        try {
            for (i in 0 until n) runCatching { observers.getBroadcastItem(i).onHit(bundle) }
        } finally {
            observers.finishBroadcast()
        }
    }
}

/** Runs [block] on the main thread and waits [seconds] at most; null when it did not finish. zh-CN: 在主线程运行并至多等待 [seconds] 秒. */
internal fun <T> onMainThread(seconds: Long = 3, block: () -> T): T? {
    if (Looper.myLooper() == Looper.getMainLooper()) return block()
    val task = FutureTask(Callable(block))
    Handler(Looper.getMainLooper()).post(task)
    return runCatching { task.get(seconds, TimeUnit.SECONDS) }.getOrNull()
}
