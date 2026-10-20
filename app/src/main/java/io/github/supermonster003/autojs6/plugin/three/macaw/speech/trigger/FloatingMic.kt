package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * The placement rules of the floating microphone (roadmap D62). Pure Kotlin.
 * zh-CN: 悬浮麦克风的位置规则 (路线图 D62). 纯 Kotlin.
 */
internal object FloatingMicPolicy {
    /** Where a new floating microphone rests on its edge (0 top, 1 bottom). zh-CN: 新的悬浮麦克风在边缘上的位置 (0 顶部, 1 底部). */
    const val DEFAULT_Y = 0.4f

    /** A touch that moved farther than [slop] pixels is a drag, never a tap. zh-CN: 移动超过 [slop] 像素的触摸是拖动而不是点按. */
    fun isDrag(dx: Float, dy: Float, slop: Int): Boolean = dx * dx + dy * dy > slop.toFloat() * slop

    /** D62: after a drag the button rests on the nearer side edge. zh-CN: D62: 拖动后按钮停靠在较近的一侧. */
    fun restsRight(centerX: Int, screenWidth: Int): Boolean = centerX * 2 >= screenWidth

    fun x(right: Boolean, screenWidth: Int, size: Int, margin: Int): Int =
        if (right) (screenWidth - size - margin).coerceAtLeast(0) else margin.coerceAtMost((screenWidth - size).coerceAtLeast(0))

    fun y(fraction: Float, screenHeight: Int, size: Int): Int = ((screenHeight - size).coerceAtLeast(0) * fraction.coerceIn(0f, 1f)).roundToInt()

    fun fraction(y: Int, screenHeight: Int, size: Int): Float = if (screenHeight <= size) 0f else (y.toFloat() / (screenHeight - size)).coerceIn(0f, 1f)
}

/**
 * Whether the assistant sheet is on screen (main thread): the floating microphone hides meanwhile, so it never covers
 * the sheet's buttons.
 * zh-CN: 助理弹层是否在屏幕上 (主线程): 期间悬浮麦克风隐藏, 不会遮住弹层的按钮.
 */
internal object SheetVisibility {
    private val listeners = LinkedHashSet<(Boolean) -> Unit>()
    private var shown = 0

    val visible: Boolean get() = shown > 0

    fun shown() {
        if (shown++ == 0) listeners.toList().forEach { it(true) }
    }

    fun hidden() {
        if (shown == 0) return
        if (--shown == 0) listeners.toList().forEach { it(false) }
    }

    fun listen(listener: (Boolean) -> Unit) {
        listeners += listener
    }

    fun forget(listener: (Boolean) -> Unit) {
        listeners -= listener
    }
}

/**
 * The floating microphone (roadmap D62): a small round button over other apps that the user drags along the screen
 * edge; a tap opens the assistant sheet (D60). Off by default; it needs the overlay permission. It records nothing: the
 * service only keeps the overlay window, as a foreground service of type `specialUse` on API 34+ with a quiet
 * notification whose action turns it off; it comes back after a reboot or an update while the switch is on. It hides
 * while the sheet is visible and is independent of the floating ball of 3-Stove Agent; both may show at once.
 * zh-CN: 悬浮麦克风 (路线图 D62): 显示在其他应用之上的小圆形按钮, 用户可沿屏幕边缘拖动; 点按即打开助理弹层 (D60). 默认关; 需要悬浮窗权限. 它不录音:
 * 服务只保持悬浮窗, API 34+ 为 `specialUse` 类型的前台服务, 通知静默, 其操作可关闭悬浮麦克风; 开关打开时重启或更新后自动恢复. 弹层可见时隐藏;
 * 与 3-Stove Agent 的悬浮球相互独立, 可以同时显示.
 */
class FloatingMicService : Service() {

    private lateinit var preferences: TriggerPreferences
    private var manager: WindowManager? = null
    private var button: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var uiMode = 0
    private val onSheet: (Boolean) -> Unit = { visible -> button?.visibility = if (visible) View.GONE else View.VISIBLE }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        preferences = TriggerPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // The notification action: the switch goes off as well.
            preferences.floating = false
            shutDown()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), foregroundType())
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (API 31+) or a SecurityException: no button without the service.
            Log.w(TAG, "The floating microphone service was refused: $e")
            shutDown()
            return START_NOT_STICKY
        }
        if (!preferences.floating || !Settings.canDrawOverlays(this)) {
            Log.i(TAG, "The floating microphone is off or lacks the overlay permission")
            shutDown()
            return START_NOT_STICKY
        }
        current = WeakReference(this)
        show()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (button == null) return
        if (newConfig.uiMode != uiMode) {
            // Light or dark: draw the button again in the new colors.
            remove()
            show()
        } else {
            // Rotation or another screen size: keep the edge and the relative height.
            place()
        }
    }

    override fun onDestroy() {
        remove()
        if (running === this) current = null
        super.onDestroy()
    }

    private fun shutDown() {
        remove()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (running === this) current = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun show() {
        if (button != null) return
        val type = overlayType()
        val context = windowContext(type)
        val windows = context.getSystemService(WindowManager::class.java) ?: return
        val size = resources.getDimensionPixelSize(R.dimen.floating_mic_size)
        val view = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic_24)
            imageTintList = ContextCompat.getColorStateList(context, R.color.trigger_button_icon)
            setBackgroundResource(R.drawable.trigger_button_background)
            val padding = resources.getDimensionPixelSize(R.dimen.floating_mic_padding)
            setPadding(padding, padding, padding, padding)
            contentDescription = getString(R.string.tile_voice_label)
            isClickable = true
            isFocusable = false
            setOnClickListener { open() }
            setOnTouchListener(Dragger(ViewConfiguration.get(context).scaledTouchSlop))
            visibility = if (SheetVisibility.visible) View.GONE else View.VISIBLE
        }
        val layout = WindowManager.LayoutParams(
            size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = WINDOW_TITLE
        }
        try {
            windows.addView(view, layout)
        } catch (e: RuntimeException) {
            Log.w(TAG, "The floating microphone could not be shown: $e")
            return
        }
        manager = windows
        button = view
        params = layout
        uiMode = resources.configuration.uiMode
        SheetVisibility.listen(onSheet)
        place()
        Log.i(TAG, "Floating microphone shown")
    }

    private fun remove() {
        SheetVisibility.forget(onSheet)
        val view = button ?: return
        button = null
        params = null
        runCatching { manager?.removeView(view) }
        manager = null
    }

    /** Puts the button on its saved edge and height. zh-CN: 把按钮放到保存的一侧与高度. */
    private fun place() {
        val view = button ?: return
        val layout = params ?: return
        val (width, height) = screenSize()
        val size = layout.width
        layout.x = FloatingMicPolicy.x(preferences.floatingRight, width, size, margin())
        layout.y = FloatingMicPolicy.y(preferences.floatingY, height, size)
        runCatching { manager?.updateViewLayout(view, layout) }
    }

    private fun margin(): Int = resources.getDimensionPixelSize(R.dimen.floating_mic_margin)

    private fun screenSize(): Pair<Int, Int> {
        val windows = manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windows != null) {
            val bounds = windows.currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val metrics = resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun open() {
        Log.i(TAG, "Floating microphone tapped")
        runCatching { AssistantEntry.open(this, TriggerSource.FLOATING) }.onFailure { Log.w(TAG, "The floating microphone could not open the sheet: $it") }
    }

    /** Drags the button; a touch that does not move is a tap. zh-CN: 拖动按钮; 不移动的触摸算点按. */
    private inner class Dragger(private val slop: Int) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val layout = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = layout.x
                    startY = layout.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && FloatingMicPolicy.isDrag(dx, dy, slop)) dragging = true
                    if (dragging) {
                        val (width, height) = screenSize()
                        layout.x = (startX + dx.roundToInt()).coerceIn(0, (width - layout.width).coerceAtLeast(0))
                        layout.y = (startY + dy.roundToInt()).coerceIn(0, (height - layout.height).coerceAtLeast(0))
                        runCatching { manager?.updateViewLayout(view, layout) }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        val (width, height) = screenSize()
                        preferences.floatingRight = FloatingMicPolicy.restsRight(layout.x + layout.width / 2, width)
                        preferences.floatingY = FloatingMicPolicy.fraction(layout.y, height, layout.height)
                        place()
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        view.performClick()
                    }
                    dragging = false
                }
            }
            return true
        }
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun windowContext(type: Int): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return this
        val display = getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return this
        return createDisplayContext(display).createWindowContext(type, null)
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager?.getNotificationChannel(CHANNEL_ID) == null) {
            manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.notification_floating_channel), NotificationManager.IMPORTANCE_LOW))
        }
        val stop = PendingIntent.getService(this, REQUEST_STOP, Intent(this, FloatingMicService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val settings = PendingIntent.getActivity(
            this, REQUEST_SETTINGS, Intent().setClassName(this, "$packageName$TRIGGER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_touch_app_24)
            .setContentTitle(getString(R.string.notification_floating_title))
            .setContentText(getString(R.string.notification_floating_text))
            .setContentIntent(settings)
            .addAction(0, getString(R.string.notification_floating_off), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun foregroundType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

    companion object {
        private const val TAG = "MacawTrigger"
        private const val CHANNEL_ID = "floating_microphone"
        private const val NOTIFICATION_ID = 0x464D
        private const val REQUEST_STOP = 0x4653
        private const val REQUEST_SETTINGS = 0x4654
        private const val ACTION_STOP = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.action.FLOATING_MICROPHONE_OFF"
        private const val TRIGGER_SETTINGS = ".ui.settings.TriggerSettingsAlias"

        /** The window title, for `dumpsys window` in device runs. zh-CN: 窗口标题, 供设备测试中的 `dumpsys window` 使用. */
        const val WINDOW_TITLE = "3-Macaw Speech floating microphone"

        private var current: WeakReference<FloatingMicService>? = null

        /** The running service while the button may show (main thread). zh-CN: 按钮可显示期间运行的服务 (主线程). */
        internal val running: FloatingMicService? get() = current?.get()

        /** The button on screen, for device tests. zh-CN: 屏幕上的按钮, 供设备测试使用. */
        internal val shownButton: View? get() = running?.button

        /** On when the user turned it on and the overlay permission is held. zh-CN: 用户开启且持有悬浮窗权限时为开. */
        fun isOn(context: Context): Boolean = TriggerPreferences(context).floating && Settings.canDrawOverlays(context)

        /**
         * Starts the service when the floating microphone is on (the settings switch, a reboot, an update); answers false
         * when it is off or Android refused the start.
         * zh-CN: 悬浮麦克风为开时启动服务 (设置开关, 重启, 更新后); 关闭或 Android 拒绝启动时返回 false.
         */
        fun start(context: Context): Boolean {
            if (!isOn(context)) return false
            return runCatching { ContextCompat.startForegroundService(context, Intent(context, FloatingMicService::class.java)) }
                .onFailure { Log.w(TAG, "The floating microphone service could not start: $it") }
                .isSuccess
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, FloatingMicService::class.java)) }
        }
    }
}
