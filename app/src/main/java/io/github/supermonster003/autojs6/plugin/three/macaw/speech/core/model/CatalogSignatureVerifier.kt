package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies the detached `speech-models` signature (roadmap D24) over the exact catalog bytes, with every envelope rule
 * of Shared-Assets `crypto.py` (`_signature_bytes`): exactly the four fields, the fixed algorithm and catalog name, a
 * key id that is both the expected one and `p256-` plus the SPKI SHA-256 prefix of [publicKeyDer], canonical base64
 * and one canonical DER ECDSA sequence. The key must be on P-256. Same structure as the Ace Editor font catalog
 * verifier and the host's codegen catalog verifier, without sharing their code.
 * zh-CN: 对目录原始字节校验 `speech-models` 分离签名 (路线图 D24), 逐条执行 Shared-Assets `crypto.py` (`_signature_bytes`) 的信封规则:
 * 恰好四个字段, 固定的算法与目录名, 密钥 ID 既等于期望值又等于 `p256-` 加 [publicKeyDer] 的 SPKI SHA-256 前缀, 规范 base64 以及单个规范
 * DER ECDSA 序列. 密钥必须为 P-256. 结构与 Ace Editor 字体目录及宿主 codegen 目录的校验器相同, 但不共享代码.
 */
class CatalogSignatureVerifier(publicKeyDer: ByteArray, private val expectedKeyId: String) {

    private val publicKey: PublicKey

    /** `p256-` and the first 16 hex digits of the SPKI SHA-256. zh-CN: `p256-` 加 SPKI SHA-256 的前 16 位十六进制. */
    val keyId: String

    init {
        publicKey = publicKeyFromDer(publicKeyDer)
        keyId = keyIdForDer(publicKeyDer)
        if (!KEY_ID.matches(expectedKeyId) || expectedKeyId != keyId) {
            throw CatalogSignatureException("expected keyId must equal p256- followed by the SPKI SHA-256 prefix")
        }
    }

    @Throws(CatalogSignatureException::class)
    fun verify(catalogBytes: ByteArray, envelopeBytes: ByteArray) {
        if (envelopeBytes.size > CatalogRules.MAX_SIGNATURE_BYTES) throw CatalogSignatureException("signature envelope exceeds its size limit")
        val envelope = try {
            StrictJson.parseObject(envelopeBytes)
        } catch (e: StrictJsonException) {
            throw CatalogSignatureException("Invalid signature envelope JSON: ${e.message}", e)
        }
        val der = signatureBytes(envelope)
        val valid = try {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(catalogBytes)
                verify(der)
            }
        } catch (e: Exception) {
            throw CatalogSignatureException("Unable to verify the catalog signature", e)
        }
        if (!valid) throw CatalogSignatureException("Catalog signature verification failed")
    }

    private fun signatureBytes(envelope: Map<String, Any?>): ByteArray {
        if (envelope.keys != FIELDS || envelope["algorithm"] != ALGORITHM || envelope["catalog"] != CatalogRules.CATALOG_FILE) {
            throw CatalogSignatureException("signature envelope must contain the fixed algorithm/catalog and exactly four fields")
        }
        if (envelope["keyId"] != expectedKeyId) throw CatalogSignatureException("signature keyId does not match the expected key")
        val value = envelope["signature"] as? String
        if (value == null || value.length > 128) throw CatalogSignatureException("signature must be bounded base64 text")
        val signature = Base64Strict.decode(value) ?: throw CatalogSignatureException("signature is not valid base64")
        if (Base64Strict.encode(signature) != value) throw CatalogSignatureException("signature base64 must be canonical")
        checkDer(signature)
        return signature
    }

    private fun checkDer(signature: ByteArray) {
        val bytes = IntArray(signature.size) { signature[it].toInt() and 0xFF }
        if (bytes.size !in 8..72 || bytes[0] != 0x30 || bytes[1] != bytes.size - 2) {
            throw CatalogSignatureException("signature must be one canonical DER ECDSA sequence")
        }
        var at = 2
        repeat(2) {
            if (at + 2 > bytes.size || bytes[at] != 2) throw CatalogSignatureException("signature must contain two DER integers")
            val size = bytes[at + 1]
            at += 2
            if (size !in 1..33 || at + size > bytes.size) throw CatalogSignatureException("signature contains an invalid DER integer")
            val integer = bytes.copyOfRange(at, at + size)
            val negative = (integer[0] and 0x80) != 0
            val padded = size > 1 && integer[0] == 0 && integer[1] < 0x80
            if (negative || padded || integer.all { it == 0 }) throw CatalogSignatureException("signature contains an invalid DER integer")
            at += size
        }
        if (at != bytes.size) throw CatalogSignatureException("signature has trailing DER data")
    }

    companion object {
        const val ALGORITHM = "ECDSA_P256_SHA256"
        private val FIELDS = setOf("algorithm", "catalog", "keyId", "signature")
        val KEY_ID = Regex("p256-[0-9a-f]{16}")

        fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

        fun keyIdForDer(der: ByteArray): String = "p256-" + sha256Hex(der).take(16)

        /**
         * SPKI DER of exactly one PEM `PUBLIC KEY` block (`crypto.pem_spki_sha256`).
         * zh-CN: 恰好一个 PEM `PUBLIC KEY` 块中的 SPKI DER (`crypto.pem_spki_sha256`).
         */
        @Throws(CatalogSignatureException::class)
        fun derFromPem(pem: ByteArray): ByteArray {
            val text = pem.toString(Charsets.US_ASCII)
            if (pem.any { it < 0 }) throw CatalogSignatureException("PEM public key must be ASCII")
            val lines = text.trim().lines().map { it.trim() }
            if (lines.size < 3 || lines.first() != "-----BEGIN PUBLIC KEY-----" || lines.last() != "-----END PUBLIC KEY-----") {
                throw CatalogSignatureException("expected one PEM PUBLIC KEY block")
            }
            return Base64Strict.decode(lines.subList(1, lines.size - 1).joinToString("")) ?: throw CatalogSignatureException("PEM public key body is not valid base64")
        }

        @Throws(CatalogSignatureException::class)
        fun publicKeyFromDer(der: ByteArray): PublicKey {
            val key = try {
                KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))
            } catch (e: Exception) {
                throw CatalogSignatureException("Invalid ECDSA public key", e)
            }
            if (!key.encoded.contentEquals(der)) throw CatalogSignatureException("public DER must be exactly one canonical SPKI key")
            val params = (key as? ECPublicKey)?.params ?: throw CatalogSignatureException("catalog key must be an EC public key")
            val field = params.curve.field as? ECFieldFp
            if (field?.p != P256_P || params.curve.a != P256_A || params.curve.b != P256_B || params.generator.affineX != P256_GX ||
                params.generator.affineY != P256_GY || params.order != P256_N || params.cofactor != 1
            ) {
                throw CatalogSignatureException("the signing key must use ECDSA P-256 (prime256v1)")
            }
            return key
        }

        private val P256_P = BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16)
        private val P256_A = P256_P.subtract(BigInteger.valueOf(3))
        private val P256_B = BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16)
        private val P256_GX = BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16)
        private val P256_GY = BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16)
        private val P256_N = BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
    }
}

internal fun ByteArray.toHex(): String {
    val digits = "0123456789abcdef"
    val out = CharArray(size * 2)
    forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xFF
        out[index * 2] = digits[value ushr 4]
        out[index * 2 + 1] = digits[value and 0x0F]
    }
    return String(out)
}

/**
 * RFC 4648 base64 without whitespace and with canonical padding bits, for API 24 (java.util.Base64 needs API 26 and
 * android.util.Base64 is not available to JVM tests).
 * zh-CN: 不含空白, 填充位规范的 RFC 4648 base64, 供 API 24 使用 (java.util.Base64 需要 API 26, android.util.Base64 在 JVM 测试中不可用).
 */
internal object Base64Strict {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val REVERSE = IntArray(128) { -1 }.also { table -> ALPHABET.forEachIndexed { index, char -> table[char.code] = index } }

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index < bytes.size) {
            val b0 = bytes[index].toInt() and 0xFF
            val b1 = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xFF else -1
            val b2 = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else -1
            out.append(ALPHABET[b0 ushr 2])
            out.append(ALPHABET[((b0 and 0x03) shl 4) or (if (b1 < 0) 0 else b1 ushr 4)])
            out.append(if (b1 < 0) '=' else ALPHABET[((b1 and 0x0F) shl 2) or (if (b2 < 0) 0 else b2 ushr 6)])
            out.append(if (b2 < 0) '=' else ALPHABET[b2 and 0x3F])
            index += 3
        }
        return out.toString()
    }

    /** Null for any character outside the alphabet, a bad length, misplaced padding or non-zero padding bits. */
    fun decode(text: String): ByteArray? {
        if (text.length % 4 != 0) return null
        if (text.isEmpty()) return ByteArray(0)
        val padding = when {
            text.endsWith("==") -> 2
            text.endsWith("=") -> 1
            else -> 0
        }
        val body = text.length - padding
        if (text.substring(0, body).any { it.code >= 128 || REVERSE[it.code] < 0 }) return null
        val out = ByteArray(text.length / 4 * 3 - padding)
        var bits = 0
        var count = 0
        var at = 0
        for (index in 0 until body) {
            bits = (bits shl 6) or REVERSE[text[index].code]
            count += 6
            if (count >= 8) {
                count -= 8
                if (at < out.size) out[at++] = (bits ushr count).toByte()
                bits = bits and ((1 shl count) - 1)
            }
        }
        // Leftover bits must be zero (canonical encoding).
        if (bits != 0) return null
        return out
    }
}
