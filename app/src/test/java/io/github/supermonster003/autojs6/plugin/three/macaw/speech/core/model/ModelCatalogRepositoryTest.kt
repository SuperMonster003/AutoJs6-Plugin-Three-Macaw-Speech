package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Catalog refresh and trust (roadmap P2.4, D24, appendix D): the unsigned bundled snapshot is used first and marked,
 * a bundled signature must verify, refreshed catalogs must be signed, monotonic and atomically cached; tampered
 * catalogs and signatures, other keys, rollbacks, same-version equivocation and network failures keep the previous
 * catalog. Since roadmap P8.3 the refresh reads `catalog-v3.json`; a bundled snapshot of an older schema (the signed release 2
 * `catalog-v2.json` of 1.3.0 until P8.5.2) stays readable, and one release published in both files is the same version.
 * zh-CN: 目录刷新与信任 (路线图 P2.4, D24, 附录 D): 未签名的内置快照先被使用并加以标注, 内置签名必须验签通过, 刷新的目录必须已签名, 版本
 * 单调并被原子缓存; 篡改的目录与签名, 其他密钥, 版本回退, 同版本不同内容与网络失败都保持原目录. 自路线图 P8.3 起刷新读取 `catalog-v3.json`;
 * 较旧 schema 的内置快照 (P8.5.2 之前 1.3.0 内置的已签名发布 2 `catalog-v2.json`) 仍可读取; 同一发布的两个文件算作同一版本.
 */
class ModelCatalogRepositoryTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private val server = FakeGitHub()
    private val signer = TestSigner()
    private lateinit var cache: File

    @After
    fun stop() = server.close()

    private fun repository(
        bundled: ByteArray = Fixtures.catalogBytes,
        bundledSignature: ByteArray? = null,
        verifier: CatalogSignatureVerifier = signer.verifier(),
        bundledFile: String = CatalogRules.CATALOG_FILE,
    ) = ModelCatalogRepository(
        cacheDirectory = if (::cache.isInitialized) cache else temporary.newFolder("catalog").also { cache = it },
        bundledCatalog = bundled,
        bundledSignature = bundledSignature,
        verifier = verifier,
        transport = server.transport,
        bundledFile = bundledFile,
    )

    /** The fixture catalog (release 3) at another version from 3 on, still valid. */
    private fun version(version: Int, generatedAt: String = "2026-11-${10 + version}T01:00:00Z"): ByteArray = Fixtures.catalogText
        .replace("\"catalogVersion\": 3", "\"catalogVersion\": $version")
        .replace("\"releaseTag\": \"speech-models-v3\"", "\"releaseTag\": \"speech-models-v$version\"")
        .replace("\"generatedAt\": \"2026-10-29T01:00:00Z\"", "\"generatedAt\": \"$generatedAt\"")
        .toByteArray()

    /** The schema 2 fixture at another version: valid, but not a catalog of `catalog-v3.json`. */
    private fun schema2(version: Int): ByteArray = Fixtures.catalogV2Bytes.toString(Charsets.UTF_8)
        .replace("\"catalogVersion\": 2", "\"catalogVersion\": $version")
        .replace("\"releaseTag\": \"speech-models-v2\"", "\"releaseTag\": \"speech-models-v$version\"")
        .toByteArray()

    private fun publish(catalog: ByteArray, signature: ByteArray = signer.envelope(catalog)) {
        server.serve(ModelCatalogConfiguration.CATALOG_URL, catalog)
        server.serve(ModelCatalogConfiguration.SIGNATURE_URL, signature)
    }

    @Test
    fun `the unsigned bundled snapshot is the first catalog and is marked as unsigned`() {
        val snapshot = repository().current()
        assertEquals(ModelCatalogRepository.Trust.UNSIGNED_BUNDLED, snapshot.trust)
        assertEquals(3, snapshot.catalog.catalogVersion)
    }

    @Test
    fun `a bundled signature must verify`() {
        val catalog = Fixtures.catalogBytes
        assertEquals(ModelCatalogRepository.Trust.SIGNED_BUNDLED, repository(bundledSignature = signer.envelope(catalog)).current().trust)
        val forged = TestSigner().envelope(catalog, keyId = signer.keyId)
        assertThrows(CatalogSignatureException::class.java) { repository(bundledSignature = forged).current() }
    }

    @Test
    fun `a newer signed catalog is verified, cached atomically and survives a restart`() {
        publish(version(4))
        val result = repository().refresh()
        assertEquals(ModelCatalogRepository.RefreshResult(4, updated = true), result)
        val restarted = repository()
        assertEquals(4, restarted.current().catalog.catalogVersion)
        assertEquals(ModelCatalogRepository.Trust.SIGNED_REFRESHED, restarted.current().trust)
        assertEquals(listOf("catalog-v3.json", "catalog-v3.sig.json", "catalog.accepted"), cache.list()!!.sorted())
        // The same catalog again: nothing to update.
        assertEquals(ModelCatalogRepository.RefreshResult(4, updated = false), restarted.refresh())
    }

    @Test
    fun `the signed release of the unsigned bundled snapshot replaces it`() {
        val same = Fixtures.catalogBytes
        publish(same)
        val repository = repository()
        assertEquals(ModelCatalogRepository.RefreshResult(3, updated = false), repository.refresh())
        assertEquals(ModelCatalogRepository.Trust.SIGNED_REFRESHED, repository.current().trust)
        // Appendix D: a signed catalog of the bundled version with other bytes also wins over the unsigned snapshot.
        val other = version(3, generatedAt = "2026-10-29T02:00:00Z")
        val fresh = temporary.newFolder("fresh")
        val second = ModelCatalogRepository(fresh, Fixtures.catalogBytes, null, signer.verifier(), server.transport, bundledFile = CatalogRules.CATALOG_FILE)
        publish(other)
        assertEquals(ModelCatalogRepository.RefreshResult(3, updated = true), second.refresh())
    }

    @Test
    fun `tampering, other keys, rollback and equivocation keep the previous catalog`() {
        publish(version(5))
        val repository = repository()
        repository.refresh()
        val cases = mapOf(
            "tampered catalog" to Pair(version(6).also { it[100] = (it[100] + 1).toByte() }, signer.envelope(version(6))),
            "tampered signature" to Pair(version(6), signer.envelope(version(7))),
            "other key" to Pair(version(6), TestSigner().envelope(version(6), keyId = signer.keyId)),
            "envelope of another file" to Pair(version(6), signer.envelope(version(6), catalog = "catalog-v2.json")),
            "schema 2 published as catalog-v3.json" to Pair(schema2(6), signer.envelope(schema2(6))),
            "rollback" to Pair(version(4), signer.envelope(version(4))),
            "same version, other bytes" to Pair(version(5, generatedAt = "2026-11-20T02:00:00Z"), signer.envelope(version(5, generatedAt = "2026-11-20T02:00:00Z"))),
            "the bundled version" to Pair(Fixtures.catalogBytes, signer.envelope(Fixtures.catalogBytes)),
        )
        cases.forEach { (name, pair) ->
            publish(pair.first, pair.second)
            val error = assertThrows(name, CatalogException::class.java) { repository.refresh() }
            assertEquals(name, SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, error.code)
            assertEquals(name, 5, repository.current().catalog.catalogVersion)
            assertEquals(name, 5, repository().current().catalog.catalogVersion)
        }
    }

    @Test
    fun `network failures report IO_FAILED and keep the catalog`() {
        val repository = repository()
        val error = assertThrows(ModelException::class.java) { repository.refresh() }
        assertEquals(SpeechErrorCodes.IO_FAILED, error.code)
        assertEquals(ModelCatalogRepository.Trust.UNSIGNED_BUNDLED, repository.current().trust)
        server.serve(ModelCatalogConfiguration.CATALOG_URL, version(4))
        assertEquals(SpeechErrorCodes.IO_FAILED, assertThrows(ModelException::class.java) { repository.refresh() }.code)
        assertFalse(File(cache, "catalog-v3.json").exists())
    }

    @Test
    fun `a damaged cache is dropped and the accepted version still blocks rollback`() {
        publish(version(5))
        repository().refresh()
        File(cache, "catalog-v3.json").appendText(" ")
        val restarted = repository()
        assertEquals("the damaged cache falls back to the bundled snapshot", 3, restarted.current().catalog.catalogVersion)
        assertFalse(File(cache, "catalog-v3.json").exists())
        publish(version(4))
        assertThrows(CatalogRollbackException::class.java) { restarted.refresh() }
        publish(version(5))
        assertEquals(ModelCatalogRepository.RefreshResult(5, updated = true), restarted.refresh())
        assertTrue(File(cache, "catalog-v3.json").exists())
    }

    @Test
    fun `the caches of older wire schemas give way to the bundled catalog`() {
        // What 1.0.0 / 1.1.0 (catalog-v1.json) and 1.2.0 (catalog-v2.json) left after refreshing (roadmap D106, D108).
        // zh-CN: 1.0.0 / 1.1.0 (catalog-v1.json) 与 1.2.0 (catalog-v2.json) 刷新后留下的缓存 (路线图 D106, D108).
        cache = temporary.newFolder("catalog")
        val v1 = Fixtures.catalogV1Bytes
        val v2 = Fixtures.catalogV2Bytes
        File(cache, "catalog-v1.json").writeBytes(v1)
        File(cache, "catalog-v1.sig.json").writeBytes(signer.envelope(v1, catalog = "catalog-v1.json"))
        File(cache, "catalog-v2.json").writeBytes(v2)
        File(cache, "catalog-v2.sig.json").writeBytes(signer.envelope(v2, catalog = "catalog-v2.json"))
        File(cache, "catalog.accepted").writeText("2\n${CatalogSignatureVerifier.sha256Hex(v2)}\n")
        val repository = repository()
        val snapshot = repository.current()
        assertEquals(3, snapshot.catalog.schemaVersion)
        assertEquals(3, snapshot.catalog.catalogVersion)
        assertEquals(ModelCatalogRepository.Trust.UNSIGNED_BUNDLED, snapshot.trust)
        assertEquals(listOf("catalog.accepted"), cache.list()!!.sorted())
        // The signed v3 release then replaces the unsigned snapshot; the accepted v2 is only a floor.
        // zh-CN: 之后已签名的 v3 发布替换未签名快照; 已接受的 v2 只作下限.
        publish(Fixtures.catalogBytes)
        assertEquals(ModelCatalogRepository.RefreshResult(3, updated = false), repository.refresh())
        assertEquals(ModelCatalogRepository.Trust.SIGNED_REFRESHED, repository().current().trust)
        assertEquals(listOf("catalog-v3.json", "catalog-v3.sig.json", "catalog.accepted"), cache.list()!!.sorted())
    }

    @Test
    fun `a bundled release 2 is read as schema 2 and the schema 3 refresh replaces it`() {
        // The 1.3.0 APK until roadmap P8.5.2 bundled the signed catalog-v2.json of release 2; a snapshot of an older schema stays readable.
        // zh-CN: 路线图 P8.5.2 之前的 1.3.0 APK 内置发布 2 的已签名 catalog-v2.json; 较旧 schema 的快照仍可读取.
        val v2 = Fixtures.catalogV2Bytes
        val repository = repository(v2, signer.envelope(v2, catalog = V2_FILE), bundledFile = V2_FILE)
        val bundled = repository.current()
        assertEquals(ModelCatalogRepository.Trust.SIGNED_BUNDLED, bundled.trust)
        assertEquals(2, bundled.catalog.schemaVersion)
        assertEquals(2, bundled.catalog.catalogVersion)
        publish(Fixtures.catalogBytes)
        assertEquals(ModelCatalogRepository.RefreshResult(3, updated = true), repository.refresh())
        assertEquals(3, repository.current().catalog.schemaVersion)
        // A snapshot of another schema than its file name is refused. zh-CN: schema 与文件名不符的快照被拒绝.
        assertThrows(CatalogException::class.java) { repository(Fixtures.catalogBytes, bundledFile = V2_FILE).bundled }
    }

    @Test
    fun `release 3 of catalog-v2 accepted by 1_2_0 does not block release 3 of catalog-v3`() {
        // 1.2.0 refreshed catalog-v2.json of release 3 (the same release without the newer kinds) and recorded no schema.
        // zh-CN: 1.2.0 刷新了发布 3 的 catalog-v2.json (同一发布, 不含较新的类别), 记录里没有 schema.
        cache = temporary.newFolder("catalog")
        val release3v2 = schema2(3)
        File(cache, "catalog-v2.json").writeBytes(release3v2)
        File(cache, "catalog-v2.sig.json").writeBytes(signer.envelope(release3v2, catalog = "catalog-v2.json"))
        File(cache, "catalog.accepted").writeText("3\n${CatalogSignatureVerifier.sha256Hex(release3v2)}\n")
        val v2 = Fixtures.catalogV2Bytes
        val repository = repository(v2, signer.envelope(v2, catalog = V2_FILE), bundledFile = V2_FILE)
        assertEquals(2, repository.current().catalog.catalogVersion)
        publish(Fixtures.catalogBytes)
        assertEquals(ModelCatalogRepository.RefreshResult(3, updated = true), repository.refresh())
        val restarted = repository(v2, signer.envelope(v2, catalog = V2_FILE), bundledFile = V2_FILE)
        assertEquals(3, restarted.current().catalog.catalogVersion)
        assertEquals(ModelCatalogRepository.Trust.SIGNED_REFRESHED, restarted.current().trust)
        assertEquals(listOf("3", CatalogSignatureVerifier.sha256Hex(Fixtures.catalogBytes), "3"), File(cache, "catalog.accepted").readLines())
        // Within one schema the same version with other bytes is still equivocation. zh-CN: 同一 schema 内同版本不同字节仍属分歧.
        publish(version(3, generatedAt = "2026-10-29T02:00:00Z"))
        assertThrows(CatalogRollbackException::class.java) { restarted.refresh() }
    }

    private companion object {
        /** The file name of a schema 2 snapshot. zh-CN: schema 2 快照的文件名. */
        const val V2_FILE = "catalog-v2.json"
    }
}
