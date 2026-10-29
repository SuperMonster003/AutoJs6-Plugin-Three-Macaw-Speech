package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelCatalogConfiguration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.UrlConnectionTransport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.fetchBytes
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * TLS trust of every GitHub host the plugin talks to (roadmap P5.1, the Android 7.0 trust anchor): the catalog on
 * `raw.githubusercontent.com`, a release asset behind `github.com` (redirected to `release-assets.githubusercontent.com`;
 * both carry a Let's Encrypt chain that ends at ISRG Root X1, which Android 7.0 lacks) and the release API on
 * `api.github.com` (Sectigo). Opt-in because it needs the network: `-e macawNetwork true`. Each probe logs PASS / FAIL
 * under `MacawDeviceRun`; only the first byte of the asset is read.
 * zh-CN: 插件访问的每个 GitHub 主机的 TLS 信任 (路线图 P5.1, Android 7.0 信任锚): `raw.githubusercontent.com` 上的目录, `github.com` 后面的
 * Release 资产 (重定向到 `release-assets.githubusercontent.com`; 两者都是终止于 ISRG Root X1 的 Let's Encrypt 证书链, Android 7.0 没有该根证书)
 * 与 `api.github.com` 上的发行 API (Sectigo). 需要网络, 因此须显式开启: `-e macawNetwork true`. 每个探针在 `MacawDeviceRun` 下记录 PASS / FAIL;
 * 资产只读第一个字节.
 */
@RunWith(AndroidJUnit4::class)
class TrustAnchorDeviceTest {

    @Before
    fun optIn() {
        assumeTrue("opt-in: -e macawNetwork true", SpeechDevice.argument("macawNetwork") == "true")
    }

    @Test
    fun everyGitHubHostCompletesItsHandshake() {
        SpeechDevice.log("trust API ${Build.VERSION.SDK_INT}, ISRG Root X1 among the default trust anchors: ${trustsIsrgRootX1()}")
        val transport = UrlConnectionTransport(userAgent = "Three-Macaw-Speech/${BuildConfig.VERSION_NAME} (AutoJs6 plugin; trust probe)")
        val probes = linkedMapOf<String, () -> String>(
            "catalog raw.githubusercontent.com" to { "${transport.fetchBytes(ModelCatalogConfiguration.SIGNATURE_URL, 64 * 1024).size} bytes" },
            "asset github.com -> release-assets.githubusercontent.com" to {
                transport.get(RELEASE_ASSET_SAMPLE, 0, CancelToken()).use { response ->
                    val first = response.body?.read() ?: -1
                    "HTTP ${response.status}, first byte $first"
                }
            },
            "release API api.github.com" to { "HTTP ${status(AppUpdateChecker.LATEST_RELEASE_API)}" },
            "releases page github.com" to { "HTTP ${status(AppUpdateChecker.RELEASES_PAGE)}" },
        )
        val failures = mutableListOf<String>()
        probes.forEach { (name, probe) ->
            runCatching(probe)
                .onSuccess { SpeechDevice.log("trust $name PASS $it") }
                .onFailure { failure ->
                    val cause = generateSequence(failure) { it.cause }.last()
                    SpeechDevice.log("trust $name FAIL ${failure.javaClass.simpleName} <- ${cause.javaClass.simpleName}: ${cause.message?.take(160)}")
                    failures += name
                }
        }
        assertEquals("hosts whose TLS handshake or request failed", emptyList<String>(), failures)
    }

    private fun status(url: String): Int = (URL(url).openConnection() as HttpURLConnection).run {
        connectTimeout = 15_000
        readTimeout = 20_000
        instanceFollowRedirects = false
        setRequestProperty("User-Agent", "Three-Macaw-Speech-update-checker/${BuildConfig.VERSION_NAME}")
        try {
            responseCode
        } finally {
            disconnect()
        }
    }

    /** Whether the platform's default trust manager (system store plus the network security config) names ISRG Root X1. */
    private fun trustsIsrgRootX1(): Boolean {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().any { manager ->
            manager.acceptedIssuers.any { it.subjectX500Principal.name.contains("ISRG Root X1") }
        }
    }

    private companion object {
        /** The smallest mirror asset of the bundled catalog (about 8.8 MB); only its first byte is read. */
        const val RELEASE_ASSET_SAMPLE =
            "https://github.com/SuperMonster003/AutoJs6-Shared-Assets/releases/download/speech-models-v1/autojs6-speech-kws.zipformer.zh-en.2025-12-20-v1-23b8757d.tar"
    }
}
