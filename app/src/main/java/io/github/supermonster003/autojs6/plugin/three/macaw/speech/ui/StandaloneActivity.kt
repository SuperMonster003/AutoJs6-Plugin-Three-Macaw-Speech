package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.HostAppearance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.MacawPalette
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.wrap
import java.lang.ref.WeakReference

internal val LocalMacawPalette = staticCompositionLocalOf<MacawPalette> { error("MacawPalette is only available inside MacawTheme") }

/** Type roles of the standalone settings specification (section 3), in sp. zh-CN: 独立设置规范第 3 节的文字角色, 以 sp 计. */
internal val MacawTypography: Typography = Typography().let { defaults ->
    defaults.copy(
        titleLarge = defaults.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        titleMedium = defaults.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        bodyLarge = defaults.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = defaults.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        bodySmall = defaults.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
        labelLarge = defaults.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = defaults.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    )
}

/**
 * Material 3 theme of the plugin's screens. The layout direction comes from the resolved locale: an Activity recreated
 * after an in-app language change may keep the previous View direction while its resources already use the new one.
 * zh-CN: 插件界面的 Material 3 主题. 布局方向取自解析后的区域: 应用内切换语言后重建的 Activity 可能仍沿用旧的 View 方向, 而资源已是新语言.
 */
@Composable
internal fun MacawTheme(palette: MacawPalette, content: @Composable () -> Unit) {
    val scheme = remember(palette) { palette.colorScheme() }
    val configuration = LocalConfiguration.current
    val direction = remember(configuration) {
        if (TextUtils.getLayoutDirectionFromLocale(configuration.locales[0]) == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
    CompositionLocalProvider(LocalMacawPalette provides palette, LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = scheme, typography = MacawTypography, content = content)
    }
}

/**
 * Base of the plugin's own screens (roadmap P5, standalone settings specification section 7), adapted from 3-Block Compose:
 * the appearance (language, night mode, theme color) follows AutoJs6 through the official settings Provider unless the
 * plugin's settings override it. The Activity is recreated only when the effective appearance changed, and never while
 * [holdsAppearance] says a recording or a dialog draft must not be interrupted (the change waits for the next resume).
 * zh-CN: 插件界面的基类 (路线图 P5, 独立设置规范第 7 节), 改编自 3-Block Compose: 外观 (语言, 夜间模式, 主题色) 经官方设置 Provider 跟随
 * AutoJs6, 除非插件设置另有选择. 只在有效外观变化时重建 Activity, 且 [holdsAppearance] 表示录音或对话框草稿不应被打断时不重建 (留待下次恢复).
 */
abstract class StandaloneActivity : ComponentActivity() {
    internal lateinit var appearance: HostAppearance
        private set
    internal lateinit var palette: MacawPalette
        private set
    private var refreshGeneration = 0

    override fun attachBaseContext(newBase: Context) {
        appearance = AppearancePreferences.resolve(newBase, HostAppearanceReader.cached())
        super.attachBaseContext(appearance.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        palette = MacawPalette.resolve(appearance)
        applyWindowAppearance()
        super.onCreate(savedInstanceState)
        LauncherIcons.normalizeAsync(this)
        setContent { MacawTheme(palette) { Content() } }
    }

    @Composable
    protected abstract fun Content()

    /** True while the screen must not be recreated for an appearance change. zh-CN: 为真时不因外观变化重建界面. */
    protected open fun holdsAppearance(): Boolean = false

    override fun onResume() {
        super.onResume()
        refreshIfChanged(AppearancePreferences.resolve(this, HostAppearanceReader.cached()))
        val expected = ++refreshGeneration
        val owner = WeakReference(this)
        val application = applicationContext
        HostAppearanceReader.worker.execute {
            val snapshot = HostAppearanceReader.read(application)
            main.post {
                owner.get()?.takeIf { !it.isFinishing && !it.isDestroyed && it.refreshGeneration == expected }?.let { activity ->
                    HostAppearanceReader.accept(snapshot)
                    activity.refreshIfChanged(AppearancePreferences.resolve(application, snapshot))
                }
            }
        }
    }

    override fun onPause() {
        refreshGeneration++
        super.onPause()
    }

    override fun onDestroy() {
        refreshGeneration++
        super.onDestroy()
    }

    /** Called by the settings screen after a confirmed appearance change. zh-CN: 设置页确认外观变化后调用. */
    internal fun applyAppearanceChange() {
        val next = AppearancePreferences.resolve(this, HostAppearanceReader.cached())
        if (next != appearance) recreate()
    }

    private fun refreshIfChanged(next: HostAppearance) {
        if (next != appearance && !holdsAppearance() && !isFinishing && !isDestroyed) recreate()
    }

    private fun applyWindowAppearance() {
        val transparent = android.graphics.Color.TRANSPARENT
        if (appearance.dark) {
            enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(transparent), navigationBarStyle = SystemBarStyle.dark(transparent))
        } else {
            // Below API 26 the navigation bar icons cannot turn dark (specification section 12): the light style then
            // paints the dark scrim behind them. zh-CN: API 26 以下导航栏图标无法变深 (规范第 12 节), 亮色样式此时以深色底衬托.
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = SystemBarStyle.light(transparent, DARK_NAVIGATION_SCRIM),
            )
        }
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(palette.background))
    }

    private companion object {
        val main = Handler(Looper.getMainLooper())

        /** The scrim AndroidX uses behind light icons it cannot darken. zh-CN: AndroidX 在无法变深的浅色图标后使用的底色. */
        const val DARK_NAVIGATION_SCRIM = 0x801b1b1b.toInt()
    }
}
