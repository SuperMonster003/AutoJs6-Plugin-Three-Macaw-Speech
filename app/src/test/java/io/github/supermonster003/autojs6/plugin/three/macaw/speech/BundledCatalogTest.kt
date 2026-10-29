package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
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
 * appendix D Q12 and D108: the released catalog must include a valid signature from the pinned family key.
 * zh-CN: 路线图 P2.4 的内置目录 (`assets/catalog/`, 锁定于 `locks/speech-catalog.lock`): Shared-Assets 的原字节, 解析器接受且版本与插件
 * 声明一致的目录, 固定的类别公钥, 以及附录 D Q12 与 D108 的规则: 发布目录必须包含可用内置公钥验证的签名, 且密钥 ID 一致.
 */
class BundledCatalogTest {

    private val lock = LockFiles.read("locks/speech-catalog.lock")
    private val directory = "app/src/main/assets/catalog"

    @Test
    fun `the bundled files are exactly the locked bytes`() {
        assertEquals("$directory/${CatalogRules.BUNDLED_CATALOG_FILE}", lock["catalog.path"])
        assertEquals("$directory/${CatalogRules.BUNDLED_SIGNATURE_FILE}", lock["signature.path"])
        assertEquals("$directory/catalog-signing-public.pem", lock["public-key.path"])
        assertEquals(lock["catalog.sha256"], LockFiles.sha256(lock.getValue("catalog.path")))
        assertEquals(lock["signature.sha256"], LockFiles.sha256(lock.getValue("signature.path")))
        assertEquals(lock["public-key.sha256"], LockFiles.sha256(lock.getValue("public-key.path")))
        val bundled = Files.list(RepositoryFiles.path(directory)).use { files -> files.map { it.fileName.toString() }.toList() }.toSet()
        assertEquals("the signed catalog and its trust anchor", setOf(CatalogRules.BUNDLED_CATALOG_FILE, CatalogRules.BUNDLED_SIGNATURE_FILE, "catalog-signing-public.pem"), bundled)
        assertTrue("the release snapshot must not reference a pending catalog", !lock.getValue("catalog.source").contains("/pending/"))
    }

    @Test
    fun `the bundled catalog parses at the advertised version`() {
        val catalog = ModelCatalogParser().parse(Files.readAllBytes(RepositoryFiles.path(lock.getValue("catalog.path"))))
        assertEquals(lock.getValue("catalog.version").toInt(), catalog.catalogVersion)
        assertEquals(ThreeMacawSpeechContract.CATALOG_VERSION, catalog.catalogVersion)
        assertEquals(CatalogRules.BUNDLED_SCHEMA, catalog.schemaVersion)
        assertTrue(catalog.models.isNotEmpty())
        assertTrue("every kind of the contract has a model", catalog.models.map { it.kind }.toSet().containsAll(listOf("asr", "kws", "punct", "tts")))
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
        val signature = RepositoryFiles.path("$directory/${CatalogRules.BUNDLED_SIGNATURE_FILE}")
        assertTrue("D108 requires the bundled release signature", Files.isRegularFile(signature))
        val pem = Files.readAllBytes(RepositoryFiles.path(lock.getValue("public-key.path")))
        val envelope = Files.readAllBytes(signature)
        assertEquals(CatalogSignatureVerifier.keyIdForDer(CatalogSignatureVerifier.derFromPem(pem)), StrictJson.parseObject(envelope)["keyId"])
        ModelCatalogConfiguration.verifier(pem).verify(Files.readAllBytes(RepositoryFiles.path(lock.getValue("catalog.path"))), envelope, CatalogRules.BUNDLED_CATALOG_FILE)
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
