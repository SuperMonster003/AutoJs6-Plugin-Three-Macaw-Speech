package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

/**
 * Asks for `RECORD_AUDIO` with the system dialog and closes (roadmap P3.1): the way to grant the microphone that a
 * `PERMISSION_DENIED` error names, next to the grant button of the home screen (roadmap P5.1). Exported behind the
 * plugin permission, so AutoJs6 (or `adb shell am start`) can open it; it has no side effect besides the request.
 * zh-CN: 以系统对话框申请 `RECORD_AUDIO` 后关闭 (路线图 P3.1): `PERMISSION_DENIED` 错误所指明的麦克风授权途径, 与首页 (路线图 P5.1) 的授权按钮
 * 并存. 以插件权限保护并导出, 因此 AutoJs6 (或 `adb shell am start`) 可以打开它; 除申请外没有副作用.
 */
class MicrophonePermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val permissions = buildList {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
            // The microphone foreground service shows its notification only with this permission (API 33+).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isEmpty()) finish() else requestPermissions(permissions.toTypedArray(), REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }

    private companion object {
        const val REQUEST = 1
    }
}
