package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.file.Files

/**
 * Signature checks of roadmap D24 with the envelope rules of Shared-Assets `crypto.py`: a fixed vector signed by the
 * reference tool through OpenSSL, signatures made in the test with fresh P-256 keys, and every refusal (tampered
 * catalog, tampered signature, another key, wrong key id, non-canonical base64 or DER, extra fields, other curves).
 * zh-CN: 路线图 D24 的签名校验, 采用 Shared-Assets `crypto.py` 的信封规则: 参考工具经 OpenSSL 签名的固定向量, 测试内用新 P-256 密钥生成的
 * 签名, 以及每种拒绝情形 (篡改目录, 篡改签名, 其他密钥, 错误密钥 ID, 非规范 base64 或 DER, 多余字段, 其他曲线).
 */
class CatalogSignatureVerifierTest {

    private val catalog = Fixtures.catalogBytes

    @Test
    fun `the OpenSSL signature of the reference tool verifies`() {
        val der = CatalogSignatureVerifier.derFromPem(Fixtures.bytes("catalog-signing-public.pem"))
        val envelope = StrictJson.parseObject(Fixtures.bytes("catalog-v1.sig.json"))
        val verifier = CatalogSignatureVerifier(der, envelope["keyId"] as String)
        verifier.verify(catalog, Fixtures.bytes("catalog-v1.sig.json"))
        assertThrows(CatalogSignatureException::class.java) { verifier.verify(catalog + " ".toByteArray(), Fixtures.bytes("catalog-v1.sig.json")) }
    }

    @Test
    fun `the bundled public key is the pinned speech-models family key`() {
        val pem = Files.readAllBytes(RepositoryFiles.path("app/src/main/assets/catalog/catalog-signing-public.pem"))
        val verifier = ModelCatalogConfiguration.verifier(pem)
        assertEquals("p256-bf5f0f579c7fbef1", verifier.keyId)
        assertEquals(ModelCatalogConfiguration.KEY_ID, verifier.keyId)
        assertEquals(ModelCatalogConfiguration.PUBLIC_KEY_SHA256, CatalogSignatureVerifier.sha256Hex(CatalogSignatureVerifier.derFromPem(pem)))
        // Another key cannot take the place of the pinned one.
        val other = TestSigner()
        val otherPem = ("-----BEGIN PUBLIC KEY-----\n" + Base64Strict.encode(other.publicDer).chunked(64).joinToString("\n") + "\n-----END PUBLIC KEY-----\n").toByteArray()
        assertThrows(CatalogSignatureException::class.java) { ModelCatalogConfiguration.verifier(otherPem) }
    }

    @Test
    fun `fresh P-256 signatures verify and every tampering is refused`() {
        val signer = TestSigner()
        val verifier = signer.verifier()
        val envelope = signer.envelope(catalog)
        verifier.verify(catalog, envelope)

        val tamperedCatalog = catalog.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        assertThrows("tampered catalog", CatalogSignatureException::class.java) { verifier.verify(tamperedCatalog, envelope) }

        val otherSignature = Base64Strict.encode(signer.derSignature("other".toByteArray()))
        assertThrows("tampered signature", CatalogSignatureException::class.java) { verifier.verify(catalog, signer.envelope(catalog, signature = otherSignature)) }

        val stranger = TestSigner()
        assertThrows("signed by another key", CatalogSignatureException::class.java) {
            verifier.verify(catalog, stranger.envelope(catalog, keyId = signer.keyId))
        }
        assertThrows("key id of another key", CatalogSignatureException::class.java) { verifier.verify(catalog, stranger.envelope(catalog)) }
        assertThrows("verifier pinned to the wrong key id", CatalogSignatureException::class.java) {
            CatalogSignatureVerifier(signer.publicDer, stranger.keyId)
        }
    }

    @Test
    fun `envelopes must be exact and signatures canonical`() {
        val signer = TestSigner()
        val verifier = signer.verifier()
        val signature = Base64Strict.encode(signer.derSignature(catalog))
        val text = signer.envelope(catalog, signature = signature).toString(Charsets.UTF_8)
        val bad = listOf(
            text.replace("\"catalog\": \"catalog-v1.json\"", "\"catalog\": \"catalog-v2.json\""),
            text.replace("ECDSA_P256_SHA256", "ECDSA_P384_SHA384"),
            text.replaceFirst("{", "{\n  \"extra\": true,"),
            text.replace("  \"catalog\": \"catalog-v1.json\",\n", ""),
            text.replace(signature, signature.dropLast(4) + "AAAA"),
            text.replace(signature, signature.trimEnd('=') ),
            text.replace(signature, " $signature"),
            text.replace(signature, signature + "\n"),
            text.replaceFirst("{", "{\"keyId\": \"x\", "),
        )
        // A 72 byte DER signature has no base64 padding, so dropping "=" leaves that envelope unchanged.
        bad.filter { it != text }.forEach { envelope ->
            assertThrows(envelope, CatalogSignatureException::class.java) { verifier.verify(catalog, envelope.toByteArray()) }
        }
        // A non-minimal DER integer (leading zero before a byte below 0x80) is refused even if the value is the same.
        val der = signer.derSignature(catalog)
        val rLength = der[3].toInt()
        val padded = byteArrayOf(0x30, (der[1] + 1).toByte(), 0x02, (rLength + 1).toByte(), 0x00) + der.copyOfRange(4, der.size)
        if (der[4].toInt() and 0x80 == 0) {
            assertThrows(CatalogSignatureException::class.java) { verifier.verify(catalog, signer.envelope(catalog, signature = Base64Strict.encode(padded))) }
        }
    }

    @Test
    fun `keys on other curves are refused`() {
        val p384 = TestSigner.generate("secp384r1")
        assertThrows(CatalogSignatureException::class.java) {
            CatalogSignatureVerifier(p384.public.encoded, CatalogSignatureVerifier.keyIdForDer(p384.public.encoded))
        }
    }

    @Test
    fun `strict base64 rejects what Python's validate mode rejects`() {
        assertEquals("hello", Base64Strict.decode("aGVsbG8=")!!.toString(Charsets.US_ASCII))
        listOf("aGVsbG8", "aGVsbG9=", "aGV sbG8=", "aGVsbG8=\n", "a===", "====").forEach { assertEquals(it, null, Base64Strict.decode(it)) }
        val bytes = ByteArray(256) { it.toByte() }
        assertEquals(bytes.toList(), Base64Strict.decode(Base64Strict.encode(bytes))!!.toList())
    }
}
