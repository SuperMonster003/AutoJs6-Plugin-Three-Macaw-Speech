package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R

/**
 * The quick settings tile "Voice command" (roadmap D61): a tap opens the assistant sheet (D60) and collapses the panel.
 * On the lock screen the sheet shows over it and dictates; sending unlocks first (D55). The tile is an action, never a
 * switch, so it stays inactive; it records nothing (D58).
 * zh-CN: 快速设置磁贴 "语音指令" (路线图 D61): 点按即打开助理弹层 (D60) 并收起面板. 锁屏上弹层显示在锁屏之上并可听写, 发送前先解锁 (D55). 磁贴是一个
 * 操作而不是开关, 因此始终为未激活状态; 它不录音 (D58).
 */
class VoiceTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_voice_label)
        tile.contentDescription = getString(R.string.tile_voice_label)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        // D61: over the lock screen only while it shows; unlocked, the plain target lets the system collapse the shade.
        val intent = TriggerActivity.intent(this, TriggerSource.TILE, overKeyguard = isLocked)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, REQUEST_TILE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            } else {
                // Below API 34 the PendingIntent variant does not exist; the Intent variant throws only from API 34.
                startWithIntent(intent)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "The tile could not open the sheet: $e")
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun startWithIntent(intent: Intent) = startActivityAndCollapse(intent)

    companion object {
        private const val TAG = "MacawTrigger"
        private const val REQUEST_TILE = 0x5449

        fun component(context: Context) = ComponentName(context, VoiceTileService::class.java)

        /**
         * Android 13+ (D61): the system asks the user to add the tile; [result] receives the result code of
         * `StatusBarManager.requestAddTileService`, or [TriggerPolicy.TILE_REQUEST_UNAVAILABLE] when the request could not
         * be made.
         * zh-CN: Android 13+ (D61): 由系统询问用户是否添加磁贴; [result] 收到 `StatusBarManager.requestAddTileService` 的结果码, 无法发出请求时为
         * [TriggerPolicy.TILE_REQUEST_UNAVAILABLE].
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun requestAdd(context: Context, result: (Int) -> Unit) {
            val manager = context.getSystemService(StatusBarManager::class.java) ?: return result(TriggerPolicy.TILE_REQUEST_UNAVAILABLE)
            runCatching {
                manager.requestAddTileService(
                    component(context),
                    context.getString(R.string.tile_voice_label),
                    Icon.createWithResource(context, R.drawable.ic_mic_24),
                    context.mainExecutor,
                ) { code -> result(code) }
            }.onFailure {
                Log.w(TAG, "The tile request failed: $it")
                result(TriggerPolicy.TILE_REQUEST_UNAVAILABLE)
            }
        }
    }
}
