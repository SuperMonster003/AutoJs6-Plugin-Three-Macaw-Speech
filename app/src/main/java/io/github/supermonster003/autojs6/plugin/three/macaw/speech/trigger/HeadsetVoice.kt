package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/** What the sheet opened by the headset voice key does about the Bluetooth permission (roadmap D68). zh-CN: 耳机语音键打开的弹层如何处理蓝牙权限. */
internal enum class HeadsetAccess {
    /** The headset's voice recognition starts with the dictation. zh-CN: 耳机的语音识别随听写开始. */
    START,

    /** "Nearby devices" is asked first; the dictation starts after the answer. zh-CN: 先申请 "附近的设备", 回答后再开始听写. */
    REQUEST,

    /** The phone's microphone only; the headset stays as it is. zh-CN: 只用手机麦克风, 不动耳机. */
    SKIP,
}

/**
 * The rules of the headset voice key (roadmap D68, decided with the maintainer on 2026-10-20). Pure Kotlin.
 * zh-CN: 耳机语音键的规则 (路线图 D68, 2026-10-20 与维护者确定). 纯 Kotlin.
 */
internal object HeadsetVoicePolicy {
    /** API level of `BLUETOOTH_CONNECT`; below it the install-time `BLUETOOTH` suffices. zh-CN: `BLUETOOTH_CONNECT` 的 API 级别. */
    const val CONNECT_PERMISSION_API = 31

    /** How often a start the Bluetooth service refused is tried again, and the pause between. zh-CN: 被拒的启动重试次数与间隔. */
    const val START_ATTEMPTS = 3
    const val RETRY_MILLIS = 400L

    /**
     * Only a sheet opened by the headset voice key touches the headset. On Android 12+ the start needs "Nearby devices";
     * it is asked in the sheet unless the keyguard is up (the system dialog does not show over it), where the phone's
     * microphone is used and the headset keeps its own state.
     * zh-CN: 只有由耳机语音键打开的弹层才操作耳机. Android 12+ 需要 "附近的设备"; 在弹层中申请, 锁屏时除外 (系统对话框不显示在锁屏之上), 此时用手机麦克风,
     * 耳机保持自身状态.
     */
    fun access(source: TriggerSource?, sdk: Int, granted: Boolean, locked: Boolean): HeadsetAccess = when {
        source != TriggerSource.HEADSET -> HeadsetAccess.SKIP
        sdk < CONNECT_PERMISSION_API || granted -> HeadsetAccess.START
        locked -> HeadsetAccess.SKIP
        else -> HeadsetAccess.REQUEST
    }

    /**
     * A drop of the headset's audio that ends the listening: the headset's own (its voice key pressed again, it went out
     * of range), after the audio was up and before this side stopped it.
     * zh-CN: 结束收音的耳机音频断开: 由耳机一方造成 (再次按语音键, 超出范围), 发生在音频接通之后, 本方停止之前.
     */
    fun endedByHeadset(audioWasConnected: Boolean, stopped: Boolean): Boolean = audioWasConnected && !stopped
}

/**
 * The voice recognition of the Bluetooth headset whose voice key opened the sheet (roadmap D68): [start] tells the
 * Bluetooth service that 3-Macaw Speech took the request (`BluetoothHeadset.startVoiceRecognition`), which answers the
 * headset and opens its audio link, so the dictation hears the headset's microphone; [stop] ends it and closes the link.
 * Without it the service answers the headset with an error after 5 s and many headsets then take the next press for
 * "stop", so only every other press opened the sheet. A drop of the link that the headset caused (its voice key pressed
 * again) calls [onEnded] once. One instance runs once; main thread.
 * zh-CN: 打开弹层的蓝牙耳机的语音识别 (路线图 D68): [start] 告诉蓝牙服务 3-Macaw Speech 接下了请求 (`BluetoothHeadset.startVoiceRecognition`),
 * 蓝牙服务应答耳机并接通音频链路, 听写因此收到耳机麦克风的声音; [stop] 结束识别并断开链路. 不这样做时蓝牙服务 5 秒后向耳机回复错误, 很多耳机随后把下一次
 * 按键当作 "停止", 于是隔一次才能打开弹层. 由耳机一方造成的链路断开 (再次按语音键) 调用一次 [onEnded]. 一个实例只运行一次; 主线程.
 */
internal class HeadsetVoice(context: Context, private val onEnded: () -> Unit) {

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val adapter: BluetoothAdapter? = runCatching { app.getSystemService(BluetoothManager::class.java)?.adapter }.getOrNull()
    private var headset: BluetoothHeadset? = null
    private var started = false
    private var stopped = false
    private var audioConnected = false
    private var receiving = false

    private val listener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            val connected = proxy as? BluetoothHeadset ?: return
            if (stopped) {
                close(connected)
                return
            }
            headset = connected
            begin(connected, 1)
        }

        override fun onServiceDisconnected(profile: Int) {
            headset = null
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                BluetoothHeadset.STATE_AUDIO_CONNECTED -> {
                    audioConnected = true
                    Log.i(TAG, "Headset audio connected")
                }
                BluetoothHeadset.STATE_AUDIO_DISCONNECTED -> {
                    val ended = HeadsetVoicePolicy.endedByHeadset(audioConnected, stopped)
                    audioConnected = false
                    if (ended) {
                        Log.i(TAG, "Headset audio ended by the headset")
                        onEnded()
                    }
                }
            }
        }
    }

    fun start() {
        val adapter = adapter
        if (adapter == null || !permitted(app)) {
            Log.i(TAG, "Headset voice recognition skipped (${if (adapter == null) "no Bluetooth" else "no permission"})")
            return
        }
        try {
            if (!adapter.isEnabled) {
                Log.i(TAG, "Headset voice recognition skipped (Bluetooth off)")
                return
            }
            // A protected broadcast of the Bluetooth service (not the system uid on every version), hence exported.
            ContextCompat.registerReceiver(app, receiver, IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
            receiving = true
            if (!adapter.getProfileProxy(app, listener, BluetoothProfile.HEADSET)) Log.w(TAG, "The headset profile is not available")
        } catch (e: SecurityException) {
            Log.w(TAG, "Headset voice recognition refused: $e")
        }
    }

    fun stop() {
        if (stopped) return
        stopped = true
        main.removeCallbacksAndMessages(null)
        if (receiving) {
            runCatching { app.unregisterReceiver(receiver) }
            receiving = false
        }
        val current = headset ?: return
        headset = null
        if (started) {
            // The service stops it only for its active device, which is the requesting headset (it may not be the first).
            val ended = runCatching { current.connectedDevices.count { device -> current.stopVoiceRecognition(device) } }
            Log.i(TAG, "Headset voice recognition stopped (${ended.getOrElse { it.toString() }})")
        }
        close(current)
    }

    @SuppressLint("MissingPermission")
    private fun begin(current: BluetoothHeadset, attempt: Int) {
        if (stopped) return
        try {
            // Any connected device: while the headset's request is pending the service uses the requesting one.
            val devices = current.connectedDevices
            val device = devices.firstOrNull()
            if (device == null) {
                Log.i(TAG, "No headset connected")
                return
            }
            started = current.startVoiceRecognition(device)
            Log.i(TAG, "Headset voice recognition ${if (started) "started" else "refused"} (attempt $attempt, ${devices.size} connected)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Headset voice recognition refused: $e")
            return
        }
        // Refused while the audio of an earlier recognition still closes; tried again shortly.
        if (!started && attempt < HeadsetVoicePolicy.START_ATTEMPTS) main.postDelayed({ begin(current, attempt + 1) }, HeadsetVoicePolicy.RETRY_MILLIS)
    }

    private fun close(proxy: BluetoothHeadset) {
        runCatching { adapter?.closeProfileProxy(BluetoothProfile.HEADSET, proxy) }
    }

    companion object {
        private const val TAG = "MacawHeadset"

        /** The permission [start] needs on this device, null when it is granted at install. zh-CN: 本机上 [start] 需要的运行时权限. */
        val permission: String? = if (Build.VERSION.SDK_INT >= HeadsetVoicePolicy.CONNECT_PERMISSION_API) Manifest.permission.BLUETOOTH_CONNECT else null

        fun permitted(context: Context): Boolean = permission?.let { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED } ?: true
    }
}
