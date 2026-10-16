package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogSignatureVerifier
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelCatalogConfiguration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelCatalogParser
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.LicenseNotices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * The bundled catalog of roadmap P2.4 (`assets/catalog/`, locked in `locks/speech-catalog.lock`): the exact Shared-Assets
 * bytes, a catalog the parser accepts at the version the plugin advertises, the pinned family key, and the rule of
 * appendix D Q12: once `catalog-v1.sig.json` is bundled it must verify against the bundled key, whose key id it names.
 * zh-CN: 路线图 P2.4 的内置目录 (`assets/catalog/`, 锁定于 `locks/speech-catalog.lock`): Shared-Assets 的原字节, 解析器接受且版本与插件
 * 声明一致的目录, 固定的类别公钥, 以及附录 D Q12 的规则: 一旦内置 `catalog-v1.sig.json`, 它必须能用内置公钥验签, 且所写密钥 ID 与该公钥一致.
 */
class BundledCatalogTest {

    private val lock = LockFiles.read("locks/speech-catalog.lock")
    private val directory = "app/src/main/assets/catalog"

    @Test
    fun `the bundled files are exactly the locked bytes`() {
        assertEquals("$directory/catalog-v1.json", lock["catalog.path"])
        assertEquals("$directory/catalog-signing-public.pem", lock["public-key.path"])
        assertEquals(lock["catalog.sha256"], LockFiles.sha256(lock.getValue("catalog.path")))
        assertEquals(lock["public-key.sha256"], LockFiles.sha256(lock.getValue("public-key.path")))
        val bundled = Files.list(RepositoryFiles.path(directory)).use { files -> files.map { it.fileName.toString() }.toList() }.toSet()
        assertTrue("only the catalog, its key and an optional signature: $bundled", bundled - setOf("catalog-v1.sig.json") == setOf("catalog-v1.json", "catalog-signing-public.pem"))
    }

    @Test
    fun `the bundled catalog parses at the advertised version`() {
        val catalog = ModelCatalogParser().parse(Files.readAllBytes(RepositoryFiles.path(lock.getValue("catalog.path"))))
        assertEquals(lock.getValue("catalog.version").toInt(), catalog.catalogVersion)
        assertEquals(ThreeMacawSpeechContract.CATALOG_VERSION, catalog.catalogVersion)
        assertTrue(catalog.models.isNotEmpty())
        assertTrue("every kind of the contract has a model", catalog.models.map { it.kind }.toSet().containsAll(listOf("asr", "kws", "tts")))
    }

    @Test
    fun `the bundled public key is the pinned family key`() {
        val pem = Files.readAllBytes(RepositoryFiles.path(lock.getValue("public-key.path")))
        val der = CatalogSignatureVerifier.derFromPem(pem)
        assertEquals(lock["public-key.spkiSha256"], CatalogSignatureVerifier.sha256Hex(der))
        assertEquals(lock["public-key.keyId"], CatalogSignatureVerifier.keyIdForDer(der))
        assertEquals(ModelCatalogConfiguration.PUBLIC_KEY_SHA256, lock["public-key.spkiSha256"])
        assertEquals(ModelCatalogConfiguration.KEY_ID, lock["public-key.keyId"])
    }

    @Test
    fun `a bundled signature must verify with the bundled key and name its key id`() {
        val signature = RepositoryFiles.path("$directory/catalog-v1.sig.json")
        if (!Files.exists(signature)) {
            // Appendix D Q12: before roadmap P2.5 the snapshot is unsigned; the plugin marks it as such at run time.
            assertTrue("an unsigned snapshot comes from the pending catalog", lock.getValue("catalog.source").contains("/pending/"))
            return
        }
        val pem = Files.readAllBytes(RepositoryFiles.path(lock.getValue("public-key.path")))
        val envelope = Files.readAllBytes(signature)
        assertEquals(CatalogSignatureVerifier.keyIdForDer(CatalogSignatureVerifier.derFromPem(pem)), StrictJson.parseObject(envelope)["keyId"])
        ModelCatalogConfiguration.verifier(pem).verify(Files.readAllBytes(RepositoryFiles.path(lock.getValue("catalog.path"))), envelope)
    }

    @Test
    fun `every license notice key of the catalog has its string in every language`() {
        val catalog = ModelCatalogParser().parse(Files.readAllBytes(RepositoryFiles.path(lock.getValue("catalog.path"))))
        val keys = catalog.models.flatMap { model -> model.versions.map { it.license.noticeKey } }.toSet()
        assertTrue("notice keys without a string: ${keys - LicenseNotices.KEYS.keys}", LicenseNotices.KEYS.keys.containsAll(keys))
        listOf("values", "values-en", "values-ar", "values-es", "values-fr", "values-ja", "values-ko", "values-ru", "values-zh", "values-zh-rHK", "values-zh-rTW")
            .forEach { locale ->
                val strings = RepositoryFiles.strings(locale)
                LicenseNotices.KEYS.keys.forEach { key -> assertTrue("$locale lacks $key", !strings[key].isNullOrBlank()) }
            }
    }
}
