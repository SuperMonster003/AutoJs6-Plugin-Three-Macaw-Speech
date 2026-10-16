package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.abs

/**
 * The sherpa-onnx runtime on the running device (roadmap P0.2): the installed split carries exactly the JNI library,
 * onnxruntime and the Compose helper for one supported ABI (or all four in the universal APK), compressed and extracted
 * by the installer; the library loads and reports 1.13.8; the bundled silero VAD cuts the test recording into the
 * reference segments.
 * zh-CN: 运行设备上的 sherpa-onnx 运行时 (路线图 P0.2): 已安装分包恰好包含某个受支持 ABI (Universal APK 则为全部四个) 的 JNI 库,
 * onnxruntime 与 Compose 辅助库, 以压缩方式存储并由安装器解压; 库可加载并报告 1.13.8; 随包 silero VAD 将测试录音切成参考段.
 */
@RunWith(AndroidJUnit4::class)
class SherpaRuntimeDeviceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theInstalledApkCarriesTheRuntimeOfASupportedAbiCompressed() {
        val applicationInfo = context.applicationInfo
        val apks = listOfNotNull(applicationInfo.sourceDir) + applicationInfo.splitSourceDirs.orEmpty()
        val natives = apks.flatMap { apk ->
            ZipFile(apk).use { zip -> zip.entries().asSequence().filter { it.name.startsWith("lib/") }.map { it.name to it.method }.toList() }
        }
        val abis = natives.map { it.first.split('/')[1] }.toSet()
        val supported = BuildConfig.SUPPORTED_ABIS.split(',')
        assertTrue("installed ABIs $abis must be a split of $supported", abis.isNotEmpty() && supported.containsAll(abis))
        assertTrue("one ABI split or the universal APK", abis.size == 1 || abis == supported.toSet())
        assertEquals(
            abis.flatMap { abi -> LIBRARIES.map { "lib/$abi/$it" } }.toSet(),
            natives.map { it.first }.toSet(),
        )
        assertTrue("roadmap D31: native libraries are compressed", natives.all { it.second == ZipEntry.DEFLATED })
        // Compressed libraries are extracted at install time into nativeLibraryDir.
        val extracted = File(applicationInfo.nativeLibraryDir).list().orEmpty().toSet()
        assertTrue("extracted $extracted", extracted.containsAll(LIBRARIES))
        assertTrue("the C / C++ API libraries are not packaged", extracted.none { it.startsWith("libsherpa-onnx-c") })
        assertTrue("the device runs one of the installed ABIs", Build.SUPPORTED_ABIS.any { it in abis })
    }

    @Test
    fun theRuntimeLoadsAndReportsTheLockedVersions() {
        val versions = SherpaRuntime.versions()
        assertEquals(BuildConfig.SHERPA_ONNX_VERSION, versions.sherpaOnnx)
        assertEquals("1.13.8", versions.sherpaOnnx)
        assertEquals("onnxruntime bundled by the AAR", "1.28.2", versions.onnxruntime)
        assertTrue("git sha ${versions.gitSha1}", versions.gitSha1.isNotBlank())
    }

    @Test
    fun theBundledVadCutsTheTestRecordingIntoTheReferenceSegments() {
        val samples = InstrumentationRegistry.getInstrumentation().context.assets.open(VadReference.ASSET).use(VadReference::readPcm16Mono)
        assertEquals("60 s at 16 kHz", 960000, samples.size)
        val vad = SherpaRuntime.createVad(context.assets)
        val segments = try {
            SherpaRuntime.detectSegments(vad, samples)
        } finally {
            vad.release()
        }
        val actual = segments.map { it.startSample to it.sampleCount }
        assertEquals("segment count, actual $actual", VadReference.SEGMENTS.size, segments.size)
        VadReference.SEGMENTS.zip(segments).forEachIndexed { index, (expected, segment) ->
            val (start, count) = expected
            assertTrue("segment $index starts at ${segment.startSample}, expected $start", abs(segment.startSample - start) <= VadReference.TOLERANCE_SAMPLES)
            assertTrue(
                "segment $index ends at ${segment.startSample + segment.sampleCount}, expected ${start + count}",
                abs(segment.startSample + segment.sampleCount - (start + count)) <= VadReference.TOLERANCE_SAMPLES,
            )
        }
    }

    @Test
    fun silenceYieldsNoSegment() {
        val vad = SherpaRuntime.createVad(context.assets)
        try {
            assertTrue(SherpaRuntime.detectSegments(vad, FloatArray(SherpaRuntime.SAMPLE_RATE * 3)).isEmpty())
        } finally {
            vad.release()
        }
    }

    private companion object {
        val LIBRARIES = listOf("libsherpa-onnx-jni.so", "libonnxruntime.so", "libandroidx.graphics.path.so")
    }

}
