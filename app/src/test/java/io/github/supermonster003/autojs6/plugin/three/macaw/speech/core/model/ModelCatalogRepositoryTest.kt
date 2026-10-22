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
 * catalog.
 * zh-CN: 目录刷新与信任 (路线图 P2.4, D24, 附录 D): 未签名的内置快照先被使用并加以标注, 内置签名必须验签通过, 刷新的目录必须已签名, 版本
 * 单调并被原子缓存; 篡改的目录与签名, 其他密钥, 版本回退, 同版本不同内容与网络失败都保持原目录.
 */
class ModelCatalogRepositoryTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private val server = FakeGitHub()
    private val signer = TestSigner()
    private lateinit var cache: File

    @After
    fun stop() = server.close()

    private fun repository(bundled: ByteArray = Fixtures.catalogBytes, bundledSignature: ByteArray? = null, verifier: CatalogSignatureVerifier = signer.verifier()) =
        ModelCatalogRepository(
            cacheDirectory = if (::cache.isInitialized) cache else temporary.newFolder("catalog").also { cache = it },
            bundledCatalog = bundled,
            bundledSignature = bundledSignature,
            verifier = verifier,
            transport = server.transport,
        )

    /** The fixture catalog at another version from 2 on, still valid (its mirrors stay on releases v1 and v2). */
    private fun version(version: Int, generatedAt: String = "2026-10-${21 + version}T01:00:00Z"): ByteArray = Fixtures.catalogText
        .replace("\"catalogVersion\": 2", "\"catalogVersion\": $version")
        .replace("\"releaseTag\": \"speech-models-v2\"", "\"releaseTag\": \"speech-models-v$version\"")
        .replace("\"generatedAt\": \"2026-10-23T01:00:00Z\"", "\"generatedAt\": \"$generatedAt\"")
        .toByteArray()

    /** The schema 1 fixture at another version: valid, but not a catalog of `catalog-v2.json`. */
    private fun schema1(version: Int): ByteArray = Fixtures.catalogV1Bytes.toString(Charsets.UTF_8)
        .replace("\"catalogVersion\": 1", "\"catalogVersion\": $version")
        .replace("\"releaseTag\": \"speech-models-v1\"", "\"releaseTag\": \"speech-models-v$version\"")
        .toByteArray()

    private fun publish(catalog: ByteArray, signature: ByteArray = signer.envelope(catalog)) {
        server.serve(ModelCatalogConfiguration.CATALOG_URL, catalog)
        server.serve(ModelCatalogConfiguration.SIGNATURE_URL, signature)
    }

    @Test
    fun `the unsigned bundled snapshot is the first catalog and is marked as unsigned`() {
        val snapshot = repository().current()
        assertEquals(ModelCatalogRepository.Trust.UNSIGNED_BUNDLED, snapshot.trust)
        assertEquals(2, snapshot.catalog.catalogVersion)
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
        publish(version(3))
        val result = repository().refresh()
        assertEquals(ModelCatalogRepository.RefreshResult(3, updated = true), result)
        val restarted = repository()
        assertEquals(3, restarted.current().catalog.catalogVersion)
        assertEquals(ModelCatalogRepository.Trust.SIGNED_REFRESHED, restarted.current().trust)
        assertEquals(listOf("catalog-v2.json", "catalog-v2.sig.json", "catalog.accepted"), cache.list()!!.sorted())
        // The same catalog again: nothing to update.
        assertEquals(ModelCatalogRepository.RefreshResult(3, updated = false), restarted.refresh())
    }

    @Test
    fun `the signed release of the unsigned bundled snapshot replaces it`() {
        val same = Fixtures.catalogBytes
        publish(same)
        val repository = repository()
        assertEquals(ModelCatalogRepository.RefreshResult(2, updated = false), repository.refresh())
        assertEquals(ModelCatalogRepository.Trust.SIGNED_REFRESHED, repository.current().trust)
        // Appendix D: a signed catalog of the bundled version with other bytes also wins over the unsigned snapshot.
        val other = version(2, generatedAt = "2026-10-23T02:00:00Z")
        val fresh = temporary.newFolder("fresh")
        val second = ModelCatalogRepository(fresh, Fixtures.catalogBytes, null, signer.verifier(), server.transport)
        publish(other)
        assertEquals(ModelCatalogRepository.RefreshResult(2, updated = true), second.refresh())
    }

    @Test
    fun `tampering, other keys, rollback and equivocation keep the previous catalog`() {
        publish(version(4))
        val repository = repository()
        repository.refresh()
        val cases = mapOf(
            "tampered catalog" to Pair(version(5).also { it[100] = (it[100] + 1).toByte() }, signer.envelope(version(5))),
            "tampered signature" to Pair(version(5), signer.envelope(version(6))),
            "other key" to Pair(version(5), TestSigner().envelope(version(5), keyId = signer.keyId)),
            "envelope of another file" to Pair(version(5), signer.envelope(version(5), catalog = "catalog-v1.json")),
            "schema 1 published as catalog-v2.json" to Pair(schema1(5), signer.envelope(schema1(5))),
            "rollback" to Pair(version(3), signer.envelope(version(3))),
            "same version, other bytes" to Pair(version(4, generatedAt = "2026-10-25T02:00:00Z"), signer.envelope(version(4, generatedAt = "2026-10-25T02:00:00Z"))),
            "the bundled version" to Pair(Fixtures.catalogBytes, signer.envelope(Fixtures.catalogBytes)),
        )
        cases.forEach { (name, pair) ->
            publish(pair.first, pair.second)
            val error = assertThrows(name, CatalogException::class.java) { repository.refresh() }
            assertEquals(name, SpeechErrorCodes.MODEL_CHECKSUM_MISMATCH, error.code)
            assertEquals(name, 4, repository.current().catalog.catalogVersion)
            assertEquals(name, 4, repository().current().catalog.catalogVersion)
        }
    }

    @Test
    fun `network failures report IO_FAILED and keep the catalog`() {
        val repository = repository()
        val error = assertThrows(ModelException::class.java) { repository.refresh() }
        assertEquals(SpeechErrorCodes.IO_FAILED, error.code)
        assertEquals(ModelCatalogRepository.Trust.UNSIGNED_BUNDLED, repository.current().trust)
        server.serve(ModelCatalogConfiguration.CATALOG_URL, version(3))
        assertEquals(SpeechErrorCodes.IO_FAILED, assertThrows(ModelException::class.java) { repository.refresh() }.code)
        assertFalse(File(cache, "catalog-v2.json").exists())
    }

    @Test
    fun `a damaged cache is dropped and the accepted version still blocks rollback`() {
        publish(version(4))
        repository().refresh()
        File(cache, "catalog-v2.json").appendText(" ")
        val restarted = repository()
        assertEquals("the damaged cache falls back to the bundled snapshot", 2, restarted.current().catalog.catalogVersion)
        assertFalse(File(cache, "catalog-v2.json").exists())
        publish(version(3))
        assertThrows(CatalogRollbackException::class.java) { restarted.refresh() }
        publish(version(4))
        assertEquals(ModelCatalogRepository.RefreshResult(4, updated = true), restarted.refresh())
        assertTrue(File(cache, "catalog-v2.json").exists())
    }

    @Test
    fun `the signed v1 cache of an earlier release gives way to the bundled v2 catalog`() {
        // What 1.0.0 / 1.1.0 left after refreshing the signed speech-models-v1 (roadmap D106, D108).
        // zh-CN: 1.0.0 / 1.1.0 刷新已签名的 speech-models-v1 后留下的缓存 (路线图 D106, D108).
        cache = temporary.newFolder("catalog")
        val v1 = Fixtures.catalogV1Bytes
        File(cache, "catalog-v1.json").writeBytes(v1)
        File(cache, "catalog-v1.sig.json").writeBytes(signer.envelope(v1, catalog = "catalog-v1.json"))
        File(cache, "catalog.accepted").writeText("1\n${CatalogSignatureVerifier.sha256Hex(v1)}\n")
        val repository = repository()
        val snapshot = repository.current()
        assertEquals(2, snapshot.catalog.schemaVersion)
        assertEquals(2, snapshot.catalog.catalogVersion)
        assertEquals(ModelCatalogRepository.Trust.UNSIGNED_BUNDLED, snapshot.trust)
        assertEquals(listOf("catalog.accepted"), cache.list()!!.sorted())
        // The signed v2 release then replaces the unsigned snapshot; the accepted v1 is only a floor.
        // zh-CN: 之后已签名的 v2 发布替换未签名快照; 已接受的 v1 只作下限.
        publish(Fixtures.catalogBytes)
        assertEquals(ModelCatalogRepository.RefreshResult(2, updated = false), repository.refresh())
        assertEquals(ModelCatalogRepository.Trust.SIGNED_REFRESHED, repository().current().trust)
        assertEquals(listOf("catalog-v2.json", "catalog-v2.sig.json", "catalog.accepted"), cache.list()!!.sorted())
    }
}
