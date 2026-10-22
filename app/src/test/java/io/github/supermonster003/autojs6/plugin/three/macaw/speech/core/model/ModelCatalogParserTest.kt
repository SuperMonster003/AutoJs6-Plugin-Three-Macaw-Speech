package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.Locale

/**
 * The Kotlin port of `distribution.validate_catalog` accepts the catalogs the Shared-Assets tools write (the bundled
 * snapshot and the synthetic fixture) and refuses each rule's counterexample.
 * zh-CN: `distribution.validate_catalog` 的 Kotlin 移植接受 Shared-Assets 工具写出的目录 (内置快照与合成夹具), 并拒绝每条规则的反例.
 */
class ModelCatalogParserTest {

    private val parser = ModelCatalogParser()

    @Test
    fun `the bundled catalog parses and every version can be installed from upstream`() {
        // Structural checks only: the Shared-Assets pending catalog may still be regenerated, the detailed rules are
        // tested on the synthetic fixture below.
        val bytes = Files.readAllBytes(RepositoryFiles.path("app/src/main/assets/catalog/catalog-v2.json"))
        val catalog = parser.parse(bytes)
        assertEquals("speech-models-v${catalog.catalogVersion}", catalog.releaseTag)
        assertTrue(catalog.models.isNotEmpty())
        assertEquals(2, catalog.schemaVersion)
        assertEquals(setOf("asr", "kws", "punct", "tts"), catalog.models.map { it.kind }.toSet())
        catalog.models.flatMap { it.versions }.forEach { version ->
            val mapped = version.upstream.flatMap { part ->
                when (part) {
                    is UpstreamPart.TarBz2 -> part.members.values
                    is UpstreamPart.RawFile -> listOf(part.path)
                }
            }
            assertEquals(version.files.map { it.path }.toSet(), mapped.toSet())
            version.mirror?.let { assertEquals(CatalogRules.archiveSize(version.files.map { file -> file.size }), it.size) }
        }
        CatalogRules.KINDS.forEach { kind -> assertTrue("a $kind model is recommended", catalog.recommendedModel(kind) != null) }
    }

    @Test
    fun `the fixture parses with every field mapped`() {
        val catalog = Fixtures.catalog
        assertEquals(2, catalog.schemaVersion)
        assertEquals(
            listOf("asr.test.noncommercial", "kws.test.unknown", "punct.test.ct", "punct.test.en", "tts.test.tiny"),
            catalog.models.map { it.modelId },
        )
        val tiny = catalog.model("tts.test.tiny")!!.latestVersion
        assertEquals("matcha", tiny.modelType)
        assertEquals(22050, tiny.sampleRate)
        assertEquals("espeak-ng-data", tiny.runtimeConfig["dataDir"])
        assertEquals(2, tiny.upstream.size)
        assertEquals("hifigan_test.onnx", (tiny.upstream[1] as UpstreamPart.RawFile).path)
        assertEquals(CommercialUse.NOT_ALLOWED, catalog.model("asr.test.noncommercial")!!.latestVersion.license.commercialUse)
        assertNull(catalog.model("kws.test.unknown")!!.latestVersion.speakers)
        assertEquals("Test tts.test.tiny", catalog.model("tts.test.tiny")!!.localizedName(Locale.US))
        assertTrue(catalog.model("tts.test.tiny")!!.localizedName(Locale.TRADITIONAL_CHINESE).startsWith(Char(0x6D4B).toString()))
        assertEquals(
            listOf("kws.test.unknown", "punct.test.ct", "punct.test.en", "tts.test.tiny"),
            catalog.query(language = "en-US").filter { it.recommended }.map { it.modelId },
        )
    }

    @Test
    fun `upstream-only versions with a null mirror are accepted`() {
        val text = Fixtures.catalogText
        val tiny = Fixtures.tiny.latestVersion
        val mirrorJson = Regex("\"mirror\": \\{[^}]*\"url\": \"${Regex.escape(tiny.mirror!!.url)}\"\\s*}").find(text)!!.value
        val catalog = parser.parse(text.replace(mirrorJson, "\"mirror\": null").toByteArray())
        val version = catalog.model("tts.test.tiny")!!.latestVersion
        assertNull(version.mirror)
        assertEquals(version.upstreamBytes, version.downloadBytes)
        val wrongSource = text.replace(mirrorJson, "\"mirror\": null").replace(tiny.sourceUrl, tiny.sourceUrl.replace("speech-models-v1", "speech-models-v3"))
        assertThrows(CatalogException::class.java) { parser.parse(wrongSource.toByteArray()) }
        // A missing key reads like null (more lenient than the reference validator, which requires the key).
        val missing = text.replace("$mirrorJson,", "")
        assertTrue(missing != text)
        assertNull(parser.parse(missing.toByteArray()).model("tts.test.tiny")!!.latestVersion.mirror)
    }

    @Test
    fun `every rule refuses its counterexample`() {
        val text = Fixtures.catalogText
        val tiny = Fixtures.tiny.latestVersion
        val mirror = tiny.mirror!!
        val file = tiny.files.first()
        val mutations = mapOf(
            "unknown top-level field" to text.replaceFirst("{", "{\n  \"extra\": 1,"),
            "schema version 3" to text.replace("\"schemaVersion\": 2", "\"schemaVersion\": 3"),
            "punct in schema 1" to text.replace("\"schemaVersion\": 2", "\"schemaVersion\": 1"),
            "release tag mismatch" to text.replace("\"releaseTag\": \"speech-models-v2\"", "\"releaseTag\": \"speech-models-v3\""),
            "other repository" to text.replace("\"repository\": \"SuperMonster003/AutoJs6-Shared-Assets\"", "\"repository\": \"someone/else\""),
            "bad generatedAt" to text.replace("2026-10-23T01:00:00Z", "2026-02-30T01:00:00Z"),
            "releasedAt after generatedAt" to text.replace("2026-10-23T00:00:00Z", "2026-10-24T00:00:00Z"),
            "float version code" to text.replaceFirst("\"versionCode\": 1", "\"versionCode\": 1.0"),
            "boolean recommended as number" to text.replaceFirst("\"recommended\": false", "\"recommended\": 0"),
            "mirror size" to text.replace("\"size\": ${mirror.size}", "\"size\": ${mirror.size + 512}"),
            "mirror name" to text.replace("\"fileName\": \"${mirror.fileName}\"", "\"fileName\": \"x.tar\""),
            "mirror sha" to text.replace("\"sha256\": \"${mirror.sha256}\"", "\"sha256\": \"${"0".repeat(64)}\""),
            "mirror of a later release" to text.replace(mirror.url, mirror.url.replace("speech-models-v1", "speech-models-v3")),
            "file path traversal" to text.replace("\"path\": \"${file.path}\"", "\"path\": \"../${file.path}\""),
            "uppercase sha" to text.replace(file.sha256, file.sha256.uppercase()),
            "commercialUse as text" to text.replace("\"commercialUse\": true", "\"commercialUse\": \"yes\""),
            "notice key outside the family" to text.replace("speech_model_license_commercial", "license_commercial"),
            "role on a missing file" to text.replace("\"acousticModel\": \"model-steps-3.onnx\"", "\"acousticModel\": \"missing.onnx\""),
            "unsupported transducer type" to text.replace("\"modelType\": \"zipformer2\"", "\"modelType\": \"conformer\""),
            "upstream on another repository" to text.replace("https://github.com/k2-fsa/sherpa-onnx/", "https://github.com/k2-fsa/other/"),
            "upstream member not covering files" to text.replace("\"tiny-model/lexicon.txt\": \"lexicon.txt\",", ""),
            "recommended version" to text.replace("\"recommendedVersionCode\": 1", "\"recommendedVersionCode\": null"),
            "speakers on asr" to text.replaceFirst("\"sampleRate\": 16000,", "\"sampleRate\": 16000, \"speakers\": 1,"),
            "duplicate key" to text.replaceFirst("\"catalogVersion\": 2,", "\"catalogVersion\": 2, \"catalogVersion\": 2,"),
            "sampleRate on punct" to text.replace("\"modelType\": \"ct-transformer\",", "\"modelType\": \"ct-transformer\", \"sampleRate\": 16000,"),
            "punct role on a missing file" to text.replace("\"bpeVocab\": \"bpe.vocab\"", "\"bpeVocab\": \"missing.vocab\""),
            "punct type of another kind" to text.replace("\"modelType\": \"cnn-bilstm\"", "\"modelType\": \"vits\""),
            "two member prefixes" to text.replace("\"./sherpa-onnx-online-punct-test/bpe.vocab\"", "\"././sherpa-onnx-online-punct-test/bpe.vocab\""),
            "kind prefix" to text.replaceFirst("\"kind\": \"kws\"", "\"kind\": \"tts\""),
        )
        mutations.forEach { (name, mutated) ->
            assertTrue("$name: mutation must change the text", mutated != text)
            assertThrows(name, CatalogException::class.java) { parser.parse(mutated.toByteArray()) }
        }
    }

    @Test
    fun `the schema 1 catalog cached by earlier releases still parses`() {
        val catalog = parser.parse(Fixtures.catalogV1Bytes)
        assertEquals(1, catalog.schemaVersion)
        assertEquals(1, catalog.catalogVersion)
        assertEquals(listOf("asr.test.noncommercial", "kws.test.unknown", "tts.test.tiny"), catalog.models.map { it.modelId })
        // The member prefix belongs to schema 2, like the punct kind and its model types.
        // zh-CN: 成员前缀与 punct 类别及其模型类型一样属于 schema 2.
        val text = Fixtures.catalogV1Bytes.toString(Charsets.UTF_8)
        val prefixed = text.replace("\"tiny-model/lexicon.txt\":", "\"./tiny-model/lexicon.txt\":")
        assertTrue(prefixed != text)
        val error = assertThrows(CatalogException::class.java) { parser.parse(prefixed.toByteArray()) }
        assertTrue(error.message, error.message!!.contains("requires catalog schemaVersion 2"))
    }

    @Test
    fun `punct models map their roles and keep prefixed member names`() {
        val catalog = Fixtures.catalog
        val ct = catalog.model("punct.test.ct")!!.latestVersion
        assertEquals("ct-transformer", ct.modelType)
        assertNull(ct.sampleRate)
        assertNull(ct.speakers)
        assertEquals(mapOf("model" to "model.int8.onnx"), ct.runtimeConfig)
        val en = catalog.model("punct.test.en")!!.latestVersion
        assertEquals("cnn-bilstm", en.modelType)
        assertEquals(mapOf("model" to "model.int8.onnx", "bpeVocab" to "bpe.vocab"), en.runtimeConfig)
        // The archive member keeps its `./`, the installed path does not.
        // zh-CN: 归档成员保留 `./`, 安装路径不带.
        assertEquals(
            mapOf("./sherpa-onnx-online-punct-test/bpe.vocab" to "bpe.vocab", "./sherpa-onnx-online-punct-test/model.int8.onnx" to "model.int8.onnx"),
            (en.upstream.single() as UpstreamPart.TarBz2).members,
        )
    }

    @Test
    fun `kinds and model types of later plugins are skipped`() {
        val root = StrictJson.parseObject(Fixtures.catalogBytes).toMutableMap()
        @Suppress("UNCHECKED_CAST")
        val models = (root["models"] as List<Map<String, Any?>>).map { it.toMutableMap() }.toMutableList()
        fun build(): ByteArray = StrictJson.write(root + ("models" to models))
        // A model of an unknown kind: only its id is read, which must still name the kind and keep the order.
        // zh-CN: 未知类别的模型: 只读取其 ID, ID 仍须以该类别开头并保持顺序.
        models.add(1, mutableMapOf("modelId" to "dict.test.future", "kind" to "dict", "anything" to listOf(1, 2)))
        // A version of an unknown model type: only versionCode and the deprecated / revoked flags are read.
        // zh-CN: 未知模型类型的版本: 只读取 versionCode 与 deprecated / revoked 标记.
        val tiny = models.first { it["modelId"] == "tts.test.tiny" }
        @Suppress("UNCHECKED_CAST")
        val tinyVersions = (tiny["versions"] as List<Any?>) + mapOf("versionCode" to 2, "modelType" to "neural-codec", "files" to "anything")
        tiny["versions"] = tinyVersions
        tiny["recommendedVersionCode"] = 2
        // A model whose every version is of an unknown type is skipped whole.
        // zh-CN: 全部版本都是未知类型的模型整体跳过.
        val kws = models.first { it["modelId"] == "kws.test.unknown" }
        @Suppress("UNCHECKED_CAST")
        kws["versions"] = (kws["versions"] as List<Map<String, Any?>>).map { it + ("modelType" to "kws-future") }

        val catalog = parser.parse(build())
        assertEquals(listOf("asr.test.noncommercial", "punct.test.ct", "punct.test.en", "tts.test.tiny"), catalog.models.map { it.modelId })
        val model = catalog.model("tts.test.tiny")!!
        assertEquals(listOf(1), model.versions.map { it.versionCode })
        assertEquals(1, model.recommendedVersionCode)
        assertEquals(1, model.installableVersion!!.versionCode)

        // The skipped entries still take part in the order and recommendation checks.
        // zh-CN: 被跳过的条目仍参与排序与推荐版本检查.
        tiny["recommendedVersionCode"] = 1
        assertThrows(CatalogException::class.java) { parser.parse(build()) }
        tiny["recommendedVersionCode"] = 2
        tiny["versions"] = tinyVersions.reversed()
        assertThrows(CatalogException::class.java) { parser.parse(build()) }
        tiny["versions"] = tinyVersions
        assertEquals(4, parser.parse(build()).models.size)
        models[1]["modelId"] = "kws.test.future"
        assertThrows(CatalogException::class.java) { parser.parse(build()) }
        models[1]["modelId"] = "dict.test.future"
        models.add(models.removeAt(1))
        assertThrows(CatalogException::class.java) { parser.parse(build()) }
        models.add(1, models.removeAt(models.size - 1))
        kws["versions"] = listOf(mapOf("versionCode" to 1, "modelType" to "Not A Type"))
        assertThrows(CatalogException::class.java) { parser.parse(build()) }
    }

    @Test
    fun `unsorted models and a catalog below the minimum version are refused`() {
        val catalog = StrictJson.parseObject(Fixtures.catalogBytes).toMutableMap()
        catalog["models"] = (catalog["models"] as List<*>).reversed()
        assertThrows(CatalogException::class.java) { parser.parse(StrictJson.write(catalog)) }
        assertThrows(CatalogException::class.java) { parser.parse(Fixtures.catalogBytes, minimumCatalogVersion = 3) }
        assertThrows(CatalogException::class.java) { parser.parse(ByteArray((CatalogRules.MAX_CATALOG_BYTES + 1).toInt())) }
    }

    @Test
    fun `the canonical tar header matches the reference archive byte for byte`() {
        val tiny = Fixtures.tiny.latestVersion
        val archive = Fixtures.bytes(tiny.mirror!!.fileName)
        val first = tiny.files.first()
        assertEquals(CatalogRules.canonicalTarHeader(first.path, first.size).toList(), archive.copyOf(512).toList())
        assertEquals(tiny.mirror.size, CatalogRules.archiveSize(tiny.files.map { it.size }))
        assertEquals(tiny.mirror.size, archive.size.toLong())
    }

    @Test
    fun `timestamps follow the calendar and order by instant`() {
        assertTrue(Timestamp.parse("2026-10-16T08:00:00+08:00")!! == Timestamp.parse("2026-10-16T00:00:00Z")!!)
        assertTrue(Timestamp.parse("2024-02-29T00:00:00.5Z")!! > Timestamp.parse("2024-02-29T00:00:00Z")!!)
        listOf("2025-02-29T00:00:00Z", "2026-13-01T00:00:00Z", "2026-10-16T24:00:00Z", "2026-10-16T00:00:00+19:00", "0000-01-01T00:00:00Z", "2026-10-16 00:00:00Z")
            .forEach { assertNull(it, Timestamp.parse(it)) }
    }
}
