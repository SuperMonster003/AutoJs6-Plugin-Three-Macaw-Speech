package io.github.supermonster003.autojs6.plugin.three.macaw.speech;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Process;

import com.k2fsa.sherpa.onnx.SileroVadModelConfig;
import com.k2fsa.sherpa.onnx.SpeechSegment;
import com.k2fsa.sherpa.onnx.Vad;
import com.k2fsa.sherpa.onnx.VadModelConfig;
import com.k2fsa.sherpa.onnx.VersionInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Platform-only probe that drives an installed minified release build (roadmap P0.2, R8 item). It avoids AndroidX, JUnit
 * and the plugin's own classes, which R8 may rename or remove, and calls only the sherpa-onnx API that
 * `proguard-rules.pro` keeps: loading `libsherpa-onnx-jni.so`, reading the versions and running the bundled silero VAD
 * over the test recording. Selected with `-PmacawReleaseProbe=true` (see `.python/device_release_probe.py`).
 *
 * zh-CN: 驱动已安装的 R8 release 构建的纯平台探针 (路线图 P0.2 的 R8 条目). 它不依赖 AndroidX, JUnit 与可能被 R8 改名或移除的插件
 * 自有类, 只调用 `proguard-rules.pro` 保留的 sherpa-onnx API: 加载 `libsherpa-onnx-jni.so`, 读取版本, 用随包 silero VAD 处理测试录音.
 * 通过 `-PmacawReleaseProbe=true` 选用 (见 `.python/device_release_probe.py`).
 */
public final class SherpaReleaseProbeInstrumentation extends Instrumentation {

    private static final String VAD_MODEL_ASSET = "vad/silero_vad_v5.onnx";
    private static final String RECORDING_ASSET = "test_silero_vad.wav";
    private static final int WINDOW = 512;
    private static final int EXPECTED_SEGMENTS = 15;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            Context target = getTargetContext();
            ApplicationInfo info = target.getApplicationInfo();
            if ((info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                throw new IllegalStateException("the probe must run against the release build");
            }
            String version = VersionInfo.Companion.getVersion();
            String onnxruntime = VersionInfo.Companion.getOnnxruntimeVersion();
            if (!"1.13.8".equals(version)) {
                throw new IllegalStateException("unexpected sherpa-onnx version " + version);
            }
            float[] samples = readPcm16Mono(getContext().getAssets().open(RECORDING_ASSET));
            VadModelConfig config = new VadModelConfig();
            config.setSileroVadModelConfig(new SileroVadModelConfig(VAD_MODEL_ASSET, 0.5f, 0.25f, 0.25f, WINDOW, 5f));
            config.setSampleRate(16000);
            config.setNumThreads(1);
            Vad vad = new Vad(target.getAssets(), config);
            int segments = 0;
            long voiced = 0;
            try {
                for (int offset = 0; offset + WINDOW <= samples.length; offset += WINDOW) {
                    vad.acceptWaveform(Arrays.copyOfRange(samples, offset, offset + WINDOW));
                    while (!vad.empty()) {
                        SpeechSegment segment = vad.front();
                        segments++;
                        voiced += segment.getSamples().length;
                        vad.pop();
                    }
                }
                vad.flush();
                while (!vad.empty()) {
                    segments++;
                    voiced += vad.front().getSamples().length;
                    vad.pop();
                }
            } finally {
                vad.release();
            }
            if (segments != EXPECTED_SEGMENTS) {
                throw new IllegalStateException("expected " + EXPECTED_SEGMENTS + " segments, got " + segments);
            }
            result.putString("stream", "\nRELEASE_RUNTIME_OK abi=" + installedAbi(target) + " process64=" + Process.is64Bit()
                    + " sherpa=" + version + " onnxruntime=" + onnxruntime + " segments=" + segments + " voicedSamples=" + voiced + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable throwable) {
            StringWriter stack = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stack));
            result.putString("stream", "\nRELEASE_RUNTIME_FAILED " + throwable + "\n" + stack + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    /** The single ABI directory of the installed split, read from the APK itself. */
    private static String installedAbi(Context target) throws Exception {
        java.util.Set<String> abis = new java.util.TreeSet<>();
        try (java.util.zip.ZipFile apk = new java.util.zip.ZipFile(target.getApplicationInfo().sourceDir)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = apk.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("lib/") && name.endsWith(".so")) {
                    abis.add(name.split("/")[1]);
                }
            }
        }
        return String.join(",", abis);
    }

    private static float[] readPcm16Mono(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream stream = input) {
            byte[] buffer = new byte[8192];
            for (int count; (count = stream.read(buffer)) > 0; ) {
                output.write(buffer, 0, count);
            }
        }
        byte[] bytes = output.toByteArray();
        ByteBuffer view = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int offset = 12;
        while (offset + 8 <= bytes.length) {
            String id = new String(bytes, offset, 4, "US-ASCII");
            int size = view.getInt(offset + 4);
            if ("data".equals(id)) {
                float[] samples = new float[size / 2];
                for (int index = 0; index < samples.length; index++) {
                    samples[index] = view.getShort(offset + 8 + index * 2) / 32768f;
                }
                return samples;
            }
            offset += 8 + size + (size & 1);
        }
        throw new IllegalStateException("no data chunk in " + RECORDING_ASSET);
    }

}
