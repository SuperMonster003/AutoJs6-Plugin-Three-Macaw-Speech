package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.JsonNumber
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import org.autojs.plugin.tool.api.PluginToolLimits
import org.autojs.plugin.tool.api.PluginToolNames
import org.autojs.plugin.tool.api.PluginToolValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.6.1 (D87, D93 - D99, D101 E2): the static catalog passes the checks AutoJs6 applies when it loads the
 * document (`docs/dev/plugin-tool-protocol-v1.md`, host `PluginToolCatalogParser` / `PluginToolSchema`): the short name,
 * the name lengths MCP clients allow, English ASCII descriptions within the limits, schemas within the subset with every
 * object closed, special types only on top-level input properties, and the declared risks, time limits and outputs.
 * zh-CN: 路线图 P7.6.1 (D87, D93 - D99, D101 E2): 静态目录通过 AutoJs6 载入文档时的检查: 短名, MCP 客户端允许的名称长度, 限长内的英文
 * ASCII 说明, schema 在子集内且每个 object 都闭合, 特殊类型只在输入的顶层属性上, 以及声明的风险, 时限与输出.
 */
class SpeechToolCatalogTest {

    private val catalog: Map<String, Any?> by lazy { StrictJson.parseObject(SpeechToolCatalog.JSON.toByteArray(Charsets.UTF_8)) }

    @Suppress("UNCHECKED_CAST")
    private val tools: List<Map<String, Any?>> get() = catalog["tools"] as List<Map<String, Any?>>

    private fun tool(name: String): Map<String, Any?> = tools.single { it["name"] == name }

    @Test
    fun `the short name is speech, valid and not reserved`() {
        assertEquals(SpeechToolCatalog.SHORT_NAME, catalog["shortName"])
        assertEquals("speech", SpeechToolCatalog.SHORT_NAME)
        assertTrue(PluginToolNames.isValidShortName(SpeechToolCatalog.SHORT_NAME))
        assertFalse(PluginToolNames.isReservedShortName(SpeechToolCatalog.SHORT_NAME))
        assertEquals(setOf("shortName", "tools"), catalog.keys)
    }

    @Test
    fun `the four tools fit the names MCP clients allow`() {
        assertEquals(listOf("listen", "speak", "transcribe", "synthesize"), tools.map { it["name"] })
        assertEquals(SpeechToolCatalog.TOOLS, tools.map { it["name"] })
        tools.forEach { tool ->
            val name = tool["name"] as String
            assertTrue(name, PluginToolNames.isValidToolName(name))
            val full = PluginToolNames.fullName(SpeechToolCatalog.SHORT_NAME, name)
            assertTrue(full, full.length <= PluginToolLimits.MAX_FULL_NAME_CHARS)
            assertTrue(full, PluginToolNames.fitsMcpName(full))
        }
        // D94: the longest names have 17 characters, 31 with the MCP prefix.
        assertEquals(17, tools.maxOf { PluginToolNames.fullName(SpeechToolCatalog.SHORT_NAME, it["name"] as String).length })
        assertEquals("mcp__autojs6__speech_synthesize".length, PluginToolLimits.MCP_TOOL_NAME_PREFIX.length + 17)
    }

    @Test
    fun `the document stays ASCII and within the size, title and description limits`() {
        val json = SpeechToolCatalog.JSON
        assertTrue(json.toByteArray(Charsets.UTF_8).size <= PluginToolLimits.MAX_CATALOG_BYTES)
        assertTrue("the catalog stays printable ASCII", json.all { it == '\n' || it.code in 0x20..0x7e })
        assertTrue(tools.size <= PluginToolLimits.MAX_TOOLS_PER_PLUGIN)
        tools.forEach { tool ->
            val title = tool["title"] as String
            val description = tool["description"] as String
            assertTrue(title, title.isNotBlank() && title.length <= PluginToolLimits.MAX_TITLE_CHARS && title.endsWith("(3-Macaw Speech)"))
            assertTrue(description.length in 200..PluginToolLimits.MAX_DESCRIPTION_CHARS)
            assertTrue("${tool["name"]} names its errors", description.contains("Errors: "))
            assertEquals(
                setOf("name", "title", "description", "inputSchema", "outputSchema", "risk", "timeoutMs", "maxConcurrency") +
                    if (tool["name"] == SpeechToolCatalog.TOOL_SYNTHESIZE) setOf("outputs") else emptySet(),
                tool.keys,
            )
        }
    }

    @Test
    fun `risks, time limits and concurrency follow D93 and D95 - D98`() {
        assertEquals(
            mapOf("listen" to PluginToolValues.RISK_READONLY, "speak" to PluginToolValues.RISK_READONLY, "transcribe" to PluginToolValues.RISK_READONLY, "synthesize" to PluginToolValues.RISK_WRITE),
            tools.associate { it["name"] to it["risk"] },
        )
        assertEquals(
            mapOf("listen" to 120_000L, "speak" to 300_000L, "transcribe" to 300_000L, "synthesize" to 300_000L),
            tools.associate { it["name"] to (it["timeoutMs"] as JsonNumber).longOrNull() },
        )
        tools.forEach { tool ->
            assertTrue(((tool["timeoutMs"] as JsonNumber).longOrNull() ?: 0) <= PluginToolLimits.MAX_TIMEOUT_MS)
            assertEquals(1L, (tool["maxConcurrency"] as JsonNumber).longOrNull())
        }
    }

    @Test
    fun `every schema stays within the subset and every object is closed`() {
        tools.forEach { tool ->
            val name = tool["name"] as String
            @Suppress("UNCHECKED_CAST")
            checkNode(tool["inputSchema"] as Map<String, Any?>, "$name.inputSchema", depth = 1, rootProperty = false, input = true)
            @Suppress("UNCHECKED_CAST")
            checkNode(tool["outputSchema"] as Map<String, Any?>, "$name.outputSchema", depth = 1, rootProperty = false, input = false)
        }
        val json = SpeechToolCatalog.JSON
        assertEquals("every object of the schemas is closed", Regex(""""type": "object"""").findAll(json).count(), Regex(""""additionalProperties": false""").findAll(json).count())
    }

    @Test
    fun `only transcribe takes a file and only synthesize writes one`() {
        assertEquals(mapOf("file" to "file"), specialParameters(tool("transcribe")))
        listOf("listen", "speak", "synthesize").forEach { assertEquals(it, emptyMap<String, String>(), specialParameters(tool(it))) }
        @Suppress("UNCHECKED_CAST")
        val outputs = tool("synthesize")["outputs"] as List<Map<String, Any?>>
        assertEquals(1, outputs.size)
        assertEquals(SpeechToolCatalog.OUTPUT_AUDIO, outputs[0]["name"])
        assertEquals(PluginToolValues.OUTPUT_KIND_FILE, outputs[0]["kind"])
        assertEquals("audio/wav", outputs[0]["mimeType"])
        assertTrue(Regex("^[a-z0-9][a-z0-9.+-]*/[a-z0-9][a-z0-9.+-]*$").matches(SpeechToolCatalog.MIME_M4A))
    }

    @Test
    fun `the declared defaults are the defaults of the argument parser`() {
        val listen = properties(tool("listen"))
        val defaults = ListenRequest()
        assertEquals(defaults, SpeechToolArguments.listen(emptyMap()))
        assertEquals(defaults.silenceMillis, (listen.getValue("silenceMillis")["default"] as JsonNumber).longOrNull())
        assertEquals(defaults.noSpeechMillis, 1000 * (listen.getValue("noSpeechSeconds")["default"] as JsonNumber).longOrNull()!!)
        assertEquals(defaults.maxMillis, 1000 * (listen.getValue("maxSeconds")["default"] as JsonNumber).longOrNull()!!)
        assertEquals(listOf(1200L, 8L, 30L), listOf("silenceMillis", "noSpeechSeconds", "maxSeconds").map { (listen.getValue(it)["default"] as JsonNumber).longOrNull() })
        assertEquals(setOf("language", "silenceMillis", "noSpeechSeconds", "maxSeconds", "hotwords", "prompt"), listen.keys)
        assertEquals(setOf("text", "language", "voice", "speed", "wait"), properties(tool("speak")).keys)
        assertEquals(true, properties(tool("speak")).getValue("wait")["default"])
        assertEquals(setOf("file", "language", "model"), properties(tool("transcribe")).keys)
        val synthesize = properties(tool("synthesize"))
        assertEquals(setOf("text", "language", "voice", "speed", "format"), synthesize.keys)
        assertEquals(listOf("wav", "m4a"), synthesize.getValue("format")["enum"])
        assertEquals(SynthesizeRequest("x").format, synthesize.getValue("format")["default"])
        assertEquals(SpeakRequest("x").speed.toDouble(), (synthesize.getValue("speed")["default"] as JsonNumber).toDouble(), 0.0)
        assertEquals(listOf(4000L, 8000L), listOf("speak", "synthesize").map { (properties(tool(it)).getValue("text")["maxLength"] as JsonNumber).longOrNull() })
    }

    @Suppress("UNCHECKED_CAST")
    private fun properties(tool: Map<String, Any?>): Map<String, Map<String, Any?>> =
        (tool["inputSchema"] as Map<String, Any?>)["properties"] as Map<String, Map<String, Any?>>

    private fun specialParameters(tool: Map<String, Any?>): Map<String, String> =
        properties(tool).mapNotNull { (name, node) -> (node["x-autojs6-type"] as? String)?.let { name to it } }.toMap()

    /** The host's subset rules (`PluginToolSchema`), repeated so the catalog fails here before it fails on a phone. */
    private fun checkNode(node: Map<String, Any?>, path: String, depth: Int, rootProperty: Boolean, input: Boolean) {
        assertTrue("$path: deeper than ${PluginToolLimits.MAX_SCHEMA_DEPTH}", depth <= PluginToolLimits.MAX_SCHEMA_DEPTH)
        node["x-autojs6-type"]?.let { special ->
            assertTrue("$path: special types only in input schemas", input)
            assertTrue("$path: special types only on top-level properties", rootProperty)
            assertTrue(path, special in listOf(PluginToolValues.SPECIAL_TYPE_IMAGE, PluginToolValues.SPECIAL_TYPE_FILE, PluginToolValues.SPECIAL_TYPE_DIRECTORY))
            assertEquals("$path: a special parameter is a string", "string", node["type"])
        }
        val type = node["type"] as? String ?: throw AssertionError("$path has no type")
        val common = setOf("type", "title", "description", "default", "enum")
        val allowed = common + when (type) {
            "object" -> setOf("properties", "required", "additionalProperties")
            "string" -> setOf("minLength", "maxLength")
            "integer", "number" -> setOf("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum")
            "boolean" -> emptySet()
            "array" -> setOf("items", "minItems", "maxItems")
            else -> throw AssertionError("$path: unknown type $type")
        }
        val unknown = node.keys - "x-autojs6-type" - allowed
        assertTrue("$path: unknown keywords $unknown", unknown.isEmpty())
        (node["description"] as? String)?.let { assertTrue(path, it.length <= PluginToolLimits.MAX_DESCRIPTION_CHARS) }
        node["enum"]?.let { values ->
            assertTrue("$path: enum only on scalars", type in setOf("string", "integer", "number", "boolean"))
            values as List<*>
            assertEquals("$path: distinct enum values", values.size, values.toSet().size)
            values.forEach { assertTrue("$path: enum value $it", matches(type, it)) }
        }
        node["default"]?.let { default ->
            assertTrue("$path: default $default", matches(type, default))
            (node["enum"] as? List<*>)?.let { assertTrue("$path: default within the enum", default in it) }
            if (default is JsonNumber) {
                (node["minimum"] as? JsonNumber)?.let { assertTrue("$path: default below minimum", default.toDouble() >= it.toDouble()) }
                (node["maximum"] as? JsonNumber)?.let { assertTrue("$path: default above maximum", default.toDouble() <= it.toDouble()) }
            }
        }
        when (type) {
            "object" -> {
                assertEquals("$path must be closed", false, node["additionalProperties"])
                @Suppress("UNCHECKED_CAST")
                val properties = node["properties"] as Map<String, Map<String, Any?>>
                assertTrue(path, properties.size <= PluginToolLimits.MAX_SCHEMA_PROPERTIES)
                (node["required"] as? List<*>)?.forEach { assertTrue("$path: required $it is a property", it in properties.keys) }
                properties.forEach { (name, child) ->
                    assertTrue("$path.$name", name.length in 1..64)
                    checkNode(child, "$path.$name", depth + 1, rootProperty = depth == 1, input = input)
                }
            }
            "array" -> {
                @Suppress("UNCHECKED_CAST")
                val items = node["items"] as Map<String, Any?>
                assertTrue("$path: items are a scalar or an object", items["type"] != "array")
                checkNode(items, "$path[]", depth + 1, rootProperty = false, input = input)
            }
        }
    }

    private fun matches(type: String, value: Any?): Boolean = when (type) {
        "string" -> value is String
        "integer" -> value is JsonNumber && value.isInteger
        "number" -> value is JsonNumber
        "boolean" -> value is Boolean
        else -> false
    }

}
