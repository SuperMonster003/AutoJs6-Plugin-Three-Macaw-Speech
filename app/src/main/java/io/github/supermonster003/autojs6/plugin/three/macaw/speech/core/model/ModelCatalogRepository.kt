package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Trust material of the `speech-models` catalog, fixed by the build (roadmap D24): the Shared-Assets repository, the
 * family key id and the SPKI SHA-256 of `speech-models/keys/catalog-signing-public.pem`, and the refresh URLs on its
 * `main` branch. Nothing here is read from preferences.
 * zh-CN: `speech-models` 目录的信任材料, 由构建固定 (路线图 D24): Shared-Assets 仓库, 该类别的密钥 ID 与
 * `speech-models/keys/catalog-signing-public.pem` 的 SPKI SHA-256, 以及其 `main` 分支上的刷新 URL. 不从偏好设置读取任何内容.
 */
object ModelCatalogConfiguration {
    const val REPOSITORY = CatalogRules.OFFICIAL_REPOSITORY
    const val KEY_ID = "p256-bf5f0f579c7fbef1"
    const val PUBLIC_KEY_SHA256 = "bf5f0f579c7fbef188e81a02257fc3336189020273b2c69c69092cc402214f68"
    const val CATALOG_URL = "https://raw.githubusercontent.com/$REPOSITORY/main/speech-models/catalogs/${CatalogRules.CATALOG_FILE}"
    const val SIGNATURE_URL = "https://raw.githubusercontent.com/$REPOSITORY/main/speech-models/catalogs/${CatalogRules.SIGNATURE_FILE}"

    /**
     * Bundled assets (roadmap P2.4), named by their wire schema like the refresh files (roadmap D108):
     * [CatalogRules.BUNDLED_SCHEMA], the signed schema 3 catalog of release 3 since roadmap P8.5.2.
     * zh-CN: 内置资产 (路线图 P2.4), 与刷新文件一样按其传输格式命名 (路线图 D108): [CatalogRules.BUNDLED_SCHEMA], 路线图 P8.5.2 起为发布 3
     * 已签名的 schema 3 目录.
     */
    const val ASSET_CATALOG = "catalog/${CatalogRules.BUNDLED_CATALOG_FILE}"
    const val ASSET_SIGNATURE = "catalog/${CatalogRules.BUNDLED_SIGNATURE_FILE}"
    const val ASSET_PUBLIC_KEY = "catalog/catalog-signing-public.pem"

    /** Verifier for the bundled PEM after checking it against the pinned SPKI digest. zh-CN: 对照固定的 SPKI 摘要后创建校验器. */
    fun verifier(publicKeyPem: ByteArray): CatalogSignatureVerifier {
        val der = CatalogSignatureVerifier.derFromPem(publicKeyPem)
        if (CatalogSignatureVerifier.sha256Hex(der) != PUBLIC_KEY_SHA256) throw CatalogSignatureException("Catalog public key differs from its build pin")
        return CatalogSignatureVerifier(der, KEY_ID)
    }
}

/**
 * Which catalog the plugin uses (roadmap D24, appendix D Q12). The APK bundles a snapshot and its signature, which must
 * verify (P2.5 for v1, D108 for v2; `BundledCatalogTest` requires the signature of a released snapshot); a development
 * snapshot without a signature is used as the first catalog marked [Trust.UNSIGNED_BUNDLED]. A refreshed catalog is
 * used only after its signature verified; it may not be older than any catalog seen before (the bundled one included),
 * and a catalog of the same version must have the same bytes, except that a signed catalog replaces an unsigned
 * bundled snapshot of its version and that one release published in two wire schemas (`catalog-v2.json` and
 * `catalog-v3.json` of release 3, roadmap P8.3) is the same version in both.
 * Every refreshed catalog has the wire schema of [CatalogRules.CATALOG_FILE] (roadmap D106), which also keeps the
 * schema from going back; a cache of an older file name (`catalog-v2.json` of 1.2.0) is deleted on load, and its
 * accepted version only sets the floor for later refreshes. The bundled snapshot is read with the schema of
 * [bundledFile], which may be older (roadmap P8.3: the signed release 2 until P8.5.2), and a refresh replaces it.
 * zh-CN: 插件使用哪一份目录 (路线图 D24, 附录 D Q12). APK 内置一份快照及其签名, 签名必须验签通过 (v1 为 P2.5, v2 为 D108; 发布的快照由
 * `BundledCatalogTest` 强制要求签名); 没有签名的开发快照作为首个目录使用并标注为 [Trust.UNSIGNED_BUNDLED]. 刷新得到的目录只在验签
 * 通过后使用; 它不得早于此前见过的任何目录 (含内置目录), 同版本目录的字节必须相同, 例外是已签名目录可替换同版本的未签名内置快照, 以及同一发布的
 * 两种传输格式 (发布 3 的 `catalog-v2.json` 与 `catalog-v3.json`, 路线图 P8.3) 算作同一版本.
 * 每份刷新得到的目录都是 [CatalogRules.CATALOG_FILE] 对应的传输格式 (路线图 D106), 这也保证格式不会回退; 旧文件名的缓存 (1.2.0 的
 * `catalog-v2.json`) 在加载时删除, 其已接受版本只作为之后刷新的下限. 内置快照按 [bundledFile] 的格式读取, 可以较旧 (路线图 P8.3: P8.5.2 之前
 * 为已签名的发布 2), 刷新后被替换.
 */
class ModelCatalogRepository(
    private val cacheDirectory: File,
    private val bundledCatalog: ByteArray,
    private val bundledSignature: ByteArray?,
    private val verifier: CatalogSignatureVerifier,
    private val transport: HttpTransport,
    private val parser: ModelCatalogParser = ModelCatalogParser(),
    private val catalogUrl: String = ModelCatalogConfiguration.CATALOG_URL,
    private val signatureUrl: String = ModelCatalogConfiguration.SIGNATURE_URL,
    private val bundledFile: String = CatalogRules.BUNDLED_CATALOG_FILE,
) {

    enum class Trust {
        /** The APK snapshot without a detached signature (before P2.5). zh-CN: 没有分离签名的 APK 快照 (P2.5 之前). */
        UNSIGNED_BUNDLED,

        /** The APK snapshot with a verified signature. zh-CN: 签名已验证的 APK 快照. */
        SIGNED_BUNDLED,

        /** A refreshed catalog whose signature verified. zh-CN: 签名已验证的刷新目录. */
        SIGNED_REFRESHED,
    }

    data class Snapshot(val catalog: ModelCatalog, val trust: Trust, val sha256: String)

    data class RefreshResult(val catalogVersion: Int, val updated: Boolean)

    private val cacheCatalog = File(cacheDirectory, CatalogRules.CATALOG_FILE)
    private val cacheSignature = File(cacheDirectory, CatalogRules.SIGNATURE_FILE)

    /**
     * Version and SHA-256 of the newest catalog ever accepted, kept apart from the cache so deleting a damaged cache
     * cannot reopen an older signed catalog.
     * zh-CN: 曾接受的最新目录的版本与 SHA-256, 与缓存分开保存, 删除损坏的缓存也不会重新放行更旧的已签名目录.
     */
    private val acceptedFile = File(cacheDirectory, "catalog.accepted")
    private val lock = Any()

    @Volatile
    private var current: Snapshot? = null

    /** The bundled snapshot, verified when it carries a signature. zh-CN: 内置快照; 带签名时已验签. */
    val bundled: Snapshot by lazy {
        val trust = if (bundledSignature != null) {
            verifier.verify(bundledCatalog, bundledSignature, bundledFile)
            Trust.SIGNED_BUNDLED
        } else {
            Trust.UNSIGNED_BUNDLED
        }
        Snapshot(parse(bundledCatalog, bundledFile), trust, CatalogSignatureVerifier.sha256Hex(bundledCatalog))
    }

    /**
     * Parses a catalog of [file]: the schema must name that file, as the signature envelope does.
     * zh-CN: 解析 [file] 的目录: 其 schema 推出的文件名必须就是该文件, 与签名信封一致.
     */
    private fun parse(bytes: ByteArray, file: String = CatalogRules.CATALOG_FILE): ModelCatalog {
        val catalog = parser.parse(bytes)
        if (CatalogRules.catalogFile(catalog.schemaVersion) != file) {
            throw CatalogException("Catalog schemaVersion ${catalog.schemaVersion} does not belong to $file")
        }
        return catalog
    }

    /**
     * The catalog in use: the verified cache when it is at least as new as the bundled snapshot, otherwise the bundled
     * snapshot. A cache that fails verification is deleted and ignored.
     * zh-CN: 当前使用的目录: 已验签的缓存不早于内置快照时用缓存, 否则用内置快照. 验签失败的缓存被删除并忽略.
     */
    fun current(): Snapshot = synchronized(lock) {
        current ?: load().also { current = it }
    }

    private fun load(): Snapshot {
        CatalogRules.SCHEMAS.filter { it != CatalogRules.SCHEMA_VERSION }.forEach { schema ->
            File(cacheDirectory, CatalogRules.catalogFile(schema)).delete()
            File(cacheDirectory, CatalogRules.signatureFile(schema)).delete()
        }
        val cached = runCatching { readCache() }.getOrElse {
            cacheCatalog.delete()
            cacheSignature.delete()
            null
        }
        val base = bundled
        val accepted = readAccepted()
        return if (cached != null && isNotOlder(cached, base) && (accepted == null || isNotOlder(cached, accepted))) cached else base
    }

    private fun readCache(): Snapshot? {
        if (!cacheCatalog.isFile || !cacheSignature.isFile) return null
        if (cacheCatalog.length() > CatalogRules.MAX_CATALOG_BYTES || cacheSignature.length() > CatalogRules.MAX_SIGNATURE_BYTES) {
            throw CatalogException("Cached catalog exceeds its size limit")
        }
        val bytes = cacheCatalog.readBytes()
        verifier.verify(bytes, cacheSignature.readBytes())
        return Snapshot(parse(bytes), Trust.SIGNED_REFRESHED, CatalogSignatureVerifier.sha256Hex(bytes))
    }

    /**
     * Whether [candidate] may be used instead of [baseline]: newer, or the same version with the same bytes, of an unsigned
     * bundled snapshot or of an older wire schema (the same release in another file).
     * zh-CN: [candidate] 是否可代替 [baseline]: 更新, 或同版本且字节相同, 或同版本而基线是未签名内置快照或较旧的传输格式 (同一发布的另一文件).
     */
    private fun isNotOlder(candidate: Snapshot, baseline: Snapshot): Boolean = when {
        candidate.catalog.catalogVersion != baseline.catalog.catalogVersion -> candidate.catalog.catalogVersion > baseline.catalog.catalogVersion
        else -> candidate.sha256 == baseline.sha256 || baseline.trust == Trust.UNSIGNED_BUNDLED || baseline.catalog.schemaVersion < candidate.catalog.schemaVersion
    }

    /**
     * Downloads [CatalogRules.CATALOG_FILE] and its signature from Shared-Assets `main`, verifies the signature over the
     * exact bytes, validates the catalog, refuses rollback and then replaces the cache atomically. On any failure the
     * catalog in use stays as it was (roadmap D24); only explicit calls refresh (roadmap D29 / D108, no automatic network
     * use).
     * zh-CN: 从 Shared-Assets `main` 下载 [CatalogRules.CATALOG_FILE] 及其签名, 对原始字节验签, 校验目录, 拒绝回退, 然后原子替换缓存. 任何失败
     * 都保持当前目录不变 (路线图 D24); 只有显式调用才会刷新 (路线图 D29 / D108, 不自动联网).
     */
    @Throws(ModelException::class)
    fun refresh(cancel: CancelToken = CancelToken()): RefreshResult {
        val catalogBytes = fetch(catalogUrl, CatalogRules.MAX_CATALOG_BYTES, cancel)
        val signatureBytes = fetch(signatureUrl, CatalogRules.MAX_SIGNATURE_BYTES, cancel)
        verifier.verify(catalogBytes, signatureBytes)
        val catalog = parse(catalogBytes)
        val candidate = Snapshot(catalog, Trust.SIGNED_REFRESHED, CatalogSignatureVerifier.sha256Hex(catalogBytes))
        cancel.throwIfCancelled()
        return synchronized(lock) {
            val inUse = current()
            listOfNotNull(bundled, inUse, readAccepted()).forEach { baseline ->
                if (!isNotOlder(candidate, baseline)) {
                    throw CatalogRollbackException(
                        if (candidate.catalog.catalogVersion < baseline.catalog.catalogVersion) {
                            "Catalog rollback rejected: ${candidate.catalog.catalogVersion} < ${baseline.catalog.catalogVersion}"
                        } else {
                            "Catalog version ${candidate.catalog.catalogVersion} changed without a version increment"
                        },
                    )
                }
            }
            val updated = candidate.sha256 != inUse.sha256 || inUse.trust != Trust.SIGNED_REFRESHED
            if (updated) {
                writeCache(catalogBytes, signatureBytes)
                replace(acceptedFile, "${candidate.catalog.catalogVersion}\n${candidate.sha256}\n${candidate.catalog.schemaVersion}\n".toByteArray(Charsets.US_ASCII))
                current = candidate
            }
            RefreshResult(candidate.catalog.catalogVersion, updated = candidate.sha256 != inUse.sha256)
        }
    }

    /** The accepted state as a baseline; a damaged state file accepts nothing older than the bundled catalog. */
    private fun readAccepted(): Snapshot? {
        if (!acceptedFile.isFile || acceptedFile.length() > 256) return null
        val lines = runCatching { acceptedFile.readLines(Charsets.US_ASCII) }.getOrNull() ?: return null
        val version = lines.getOrNull(0)?.toIntOrNull() ?: return null
        val sha = lines.getOrNull(1)?.takeIf { CatalogRules.SHA256.matches(it) } ?: return null
        // The schema line came with 1.3.0 (roadmap P8.3); a record of 1.2.0 or earlier belongs to an older wire schema.
        // zh-CN: schema 行自 1.3.0 起记录 (路线图 P8.3); 1.2.0 及更早的记录属于较旧的传输格式.
        val schema = lines.getOrNull(2)?.toIntOrNull()?.takeIf { it in CatalogRules.SCHEMAS } ?: (CatalogRules.SCHEMA_VERSION - 1)
        val catalog = ModelCatalog(schema, version, "speech-models-v$version", "", "", emptyList())
        return Snapshot(catalog, Trust.SIGNED_REFRESHED, sha)
    }

    private fun fetch(url: String, maxBytes: Long, cancel: CancelToken): ByteArray = try {
        transport.fetchBytes(url, maxBytes, cancel)
    } catch (e: OperationCancelledException) {
        throw e
    } catch (e: HttpStatusException) {
        throw ModelException(SpeechErrorCodes.IO_FAILED, "Catalog refresh failed: HTTP ${e.status}", e)
    } catch (e: IOException) {
        throw ModelException(SpeechErrorCodes.IO_FAILED, "Catalog refresh failed: ${e.message}", e)
    }

    /** Signature first, catalog last: the catalog rename publishes the pair. zh-CN: 先签名后目录: 目录改名即发布这一对文件. */
    private fun writeCache(catalogBytes: ByteArray, signatureBytes: ByteArray) {
        if (!cacheDirectory.isDirectory && !cacheDirectory.mkdirs()) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot create the catalog cache")
        cacheCatalog.delete()
        replace(cacheSignature, signatureBytes)
        replace(cacheCatalog, catalogBytes)
    }

    private fun replace(file: File, bytes: ByteArray) {
        val temporary = File(cacheDirectory, ".${file.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temporary).use {
                it.write(bytes)
                it.flush()
                it.fd.sync()
            }
            if (!temporary.renameTo(file)) {
                file.delete()
                if (!temporary.renameTo(file)) throw ModelException(SpeechErrorCodes.IO_FAILED, "Cannot write ${file.name}")
            }
        } finally {
            temporary.delete()
        }
    }
}
