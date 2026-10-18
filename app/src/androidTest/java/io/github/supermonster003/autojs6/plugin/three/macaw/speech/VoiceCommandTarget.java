package io.github.supermonster003.autojs6.plugin.three.macaw.speech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A test target of the digital assistant (roadmap D50): another app (the test package in its own process) that answers
 * VOICE_COMMAND and reports the intent it got to the plugin process by broadcast. Plain Java: the test APK carries no
 * Kotlin runtime of its own when it runs outside the instrumentation.
 * zh-CN: 数字助理的测试目标 (路线图 D50): 应答 VOICE_COMMAND 的另一个应用 (测试包, 独立进程), 以广播把收到的 Intent 报告给插件进程. 使用纯 Java:
 * 测试 APK 在 instrumentation 之外运行时不带 Kotlin 运行库.
 */
public class VoiceCommandTarget extends Activity {
    static final String ACTION_RECEIVED = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.test.VOICE_COMMAND_RECEIVED";
    static final String PLUGIN = "io.github.supermonster003.autojs6.plugin.three.macaw.speech";
    static final String EXTRA_VOICE_LANGUAGE = "org.autojs.plugin.extra.VOICE_LANGUAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent received = getIntent();
        Bundle extras = received.getExtras();
        ArrayList<String> keys = new ArrayList<>();
        if (extras != null) keys.addAll(extras.keySet());
        Collections.sort(keys);
        sendBroadcast(new Intent(ACTION_RECEIVED).setPackage(PLUGIN)
                .putExtra("target", getClass().getName())
                .putExtra("action", received.getAction())
                .putExtra("text", received.getStringExtra(Intent.EXTRA_TEXT))
                .putExtra("language", received.getStringExtra(EXTRA_VOICE_LANGUAGE))
                .putExtra("newTask", (received.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
                .putExtra("keys", TextUtils.join(",", keys)));
        finish();
    }
}
