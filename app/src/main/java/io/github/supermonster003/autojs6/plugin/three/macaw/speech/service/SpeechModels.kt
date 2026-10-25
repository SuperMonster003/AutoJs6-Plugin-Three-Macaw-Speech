package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ArchiveExtractor
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Downloader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Importer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.LicensePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelCatalogConfiguration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelCatalogRepository
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelInstaller
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.UrlConnectionTransport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaRuntime
import java.io.File
import java.io.FileNotFoundException

/**
 * The process-wide [ModelManager] (roadmap P2.3 / P2.4), shared by the capability service, the foreground service and
 * later the screens: models in `filesDir/models/`, the verified catalog cache in `filesDir/catalog/`, partial downloads
 * in `cacheDir/downloads/` and import copies in `cacheDir/imports/`.
 * zh-CN: 进程级的 [ModelManager] (路线图 P2.3 / P2.4), 由能力服务, 前台服务及后续界面共用: 模型位于 `filesDir/models/`, 已验签的目录缓存
 * 位于 `filesDir/catalog/`, 未完成的下载位于 `cacheDir/downloads/`, 导入副本位于 `cacheDir/imports/`.
 */
internal object SpeechModels {

    private const val TAG = "MacawModels"

    /** The silero VAD of the APK as a listed model (roadmap appendix A). zh-CN: 以模型条目列出的 APK 内置 silero VAD (路线图附录 A). */
    const val BUNDLED_VAD_ID = "vad.silero.v5"

    /** Bytes of `assets/vad/silero_vad_v5.onnx` (locks/vad-assets.lock). zh-CN: VAD 资产的字节数. */
    private const val BUNDLED_VAD_BYTES = 2313101L

    @Volatile
    private var instance: ModelManager? = null

    fun get(context: Context): ModelManager = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    fun importDirectory(context: Context): File = File(context.applicationContext.cacheDir, "imports")

    /**
     * Debug builds only (roadmap D127): an unsigned catalog at `filesDir/dev-catalog/catalog-v2.json` (pushed with
     * `run-as`, `.python/device_catalog_v3.py`) replaces the bundled snapshot, so device tests can install the models of
     * a pending Shared-Assets catalog without changing the bundled one. zh-CN: 仅调试构建: 以 `run-as` 推送到该路径的未签名目录代替内置快照,
     * 设备测试因此可以安装 Shared-Assets 待签名目录中的模型, 而不改动内置快照.
     */
    const val DEV_CATALOG = "dev-catalog/${CatalogRules.CATALOG_FILE}"

    private fun create(context: Context): ModelManager {
        val assets = context.assets
        val development = File(context.filesDir, DEV_CATALOG).takeIf { BuildConfig.DEBUG && it.isFile }
        val catalog = development?.readBytes() ?: assets.open(ModelCatalogConfiguration.ASSET_CATALOG).use { it.readBytes() }
        val signature = if (development != null) {
            Log.w(TAG, "Using the unsigned development catalog $DEV_CATALOG")
            null
        } else try {
            assets.open(ModelCatalogConfiguration.ASSET_SIGNATURE).use { it.readBytes() }
        } catch (_: FileNotFoundException) {
            // Appendix D Q12, roadmap D108: released builds bundle the signature (BundledCatalogTest); a development snapshot may lack it.
            null
        }
        val publicKey = assets.open(ModelCatalogConfiguration.ASSET_PUBLIC_KEY).use { it.readBytes() }
        val transport = UrlConnectionTransport(userAgent = "Three-Macaw-Speech/${BuildConfig.VERSION_NAME} (AutoJs6 plugin)")
        val store = ModelStore(File(context.filesDir, "models"))
        val catalogs = ModelCatalogRepository(
            cacheDirectory = File(context.filesDir, "catalog"),
            bundledCatalog = catalog,
            bundledSignature = signature,
            verifier = ModelCatalogConfiguration.verifier(publicKey),
            transport = transport,
        )
        val downloads = File(context.cacheDir, "downloads")
        val freeSpace = freeSpace(context)
        val extractor = ArchiveExtractor()
        val licensePolicy = LicensePolicy(store)
        val log: (String) -> Unit = { Log.i(TAG, it) }
        // Import copies belong to operations of a previous process; none can still be in use.
        importDirectory(context).deleteRecursively()
        return ModelManager(
            store = store,
            catalogs = catalogs,
            installer = ModelInstaller(store, Downloader(transport), extractor, downloads, freeSpace = freeSpace, log = log),
            importer = Importer(store, extractor, licensePolicy, freeSpace = freeSpace),
            licensePolicy = licensePolicy,
            downloadDirectory = downloads,
            builtins = listOf(bundledVad()),
            log = log,
            scratchDirectories = listOf(importDirectory(context)),
        ).also { manager ->
            Log.i(TAG, "Catalog v${manager.catalogs.current().catalog.catalogVersion} (${manager.catalogs.current().trust})")
            manager.activity = ModelManager.ActivityListener { activity -> ModelOperationService.onActivity(context, activity) }
        }
    }

    /**
     * Bytes the plugin may still write on the volume of a directory: on API 26+ including the cached data the system
     * would clear for it (`StorageManager.getAllocatableBytes`), otherwise the usable space.
     * zh-CN: 插件在某目录所在卷上还能写入的字节数: API 26+ 计入系统会为其清理的缓存数据 (`StorageManager.getAllocatableBytes`), 否则为可用空间.
     */
    private fun freeSpace(context: Context): (File) -> Long = { directory ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storage = context.getSystemService(StorageManager::class.java)
            runCatching { storage.getAllocatableBytes(storage.getUuidForPath(directory)) }.getOrElse { usableSpace(directory) }
        } else {
            usableSpace(directory)
        }
    }

    // API 24 / 25 have no allocatable-bytes query; the fallback above uses this as well.
    @SuppressLint("UsableSpace")
    private fun usableSpace(directory: File): Long = directory.usableSpace

    private fun bundledVad() = ModelEntry(
        id = BUNDLED_VAD_ID,
        kind = "vad",
        modelType = "silero",
        // A product name, the same in every language. zh-CN: 产品名, 各语言相同.
        name = "silero VAD v5",
        languages = emptyList(),
        sizeBytes = 0,
        installedBytes = BUNDLED_VAD_BYTES,
        installed = true,
        isDefault = true,
        recommended = true,
        version = 5,
        licenseSpdx = "MIT",
        commercialUse = CommercialUse.ALLOWED,
        noticeKey = "speech_model_license_commercial",
        noticeUrl = "https://github.com/snakers4/silero-vad",
        speakers = null,
        sampleRate = SherpaRuntime.SAMPLE_RATE,
        source = ModelStore.SOURCE_CATALOG,
    )
}
