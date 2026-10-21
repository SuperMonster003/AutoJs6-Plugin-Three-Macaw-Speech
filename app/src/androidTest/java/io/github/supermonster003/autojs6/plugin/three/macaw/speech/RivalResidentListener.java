package io.github.supermonster003.autojs6.plugin.three.macaw.speech;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;

/**
 * Another app's resident listener (roadmap D75, the 3-Stone AI D31 convention) for ResidentWakeDeviceTest: disabled by
 * the test Manifest and turned on by {@link Switch} only while a test needs a rival. It is only queried, never started.
 * zh-CN: 供 ResidentWakeDeviceTest 使用的其他应用的常驻监听 (路线图 D75, 3-Stone AI D31 约定): 测试 Manifest 默认禁用, 仅在测试需要对方时由
 * {@link Switch} 启用. 只被查询, 从不被启动.
 */
public class RivalResidentListener extends Service {
    public static final String EXTRA_ENABLED = "enabled";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Sets the rival's state from the test APK's own process: shell (`pm enable`) may only change the components of a
     * test-only package, an app may always change its own. DONT_KILL_APP keeps the test APK's processes; off restores
     * the Manifest default.
     * zh-CN: 在测试 APK 自己的进程中设置对方的状态: shell (`pm enable`) 只能修改 test-only 包的组件, 应用总能修改自己的组件. DONT_KILL_APP
     * 保留测试 APK 的进程; 关闭即恢复 Manifest 默认值.
     */
    public static final class Switch extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getBooleanExtra(EXTRA_ENABLED, false)
                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, RivalResidentListener.class), state,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
