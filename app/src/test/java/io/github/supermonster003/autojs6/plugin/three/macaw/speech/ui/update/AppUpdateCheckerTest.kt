package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P11.2.4 (D206, D207): the update check reads this plugin's entry of the generated official plugin index. The
 * fixtures keep the members of `plugins.official.generated.json` (schema 2) that the check reads.
 * zh-CN: 路线图 P11.2.4 (D206, D207): 更新检查读取生成的官方插件索引中本插件的条目. 夹具只保留 `plugins.official.generated.json` (schema 2) 中检查会读的成员.
 */
class AppUpdateCheckerTest {

    private val macaw = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"

    private fun release(versionCode: Any?, versionName: Any? = "1.5.0", prerelease: Boolean = false, notes: Any? = "3-Macaw Speech 1.5.0\n\nPublished release.") =
        JSONObject()
            .put("versionName", versionName ?: JSONObject.NULL)
            .put("versionCode", versionCode ?: JSONObject.NULL)
            .put("versionDate", "2026-11-05")
            .put("changelogUrl", "https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases/tag/v1.5.0")
            .put("changelogText", notes ?: JSONObject.NULL)
            .put("prerelease", prerelease)

    private fun entry(packageName: String, vararg releases: JSONObject, title: Any? = "3-Macaw Speech") = JSONObject()
        .put("packageName", packageName)
        .put("title", title ?: JSONObject.NULL)
        .put("releases", JSONArray(releases.toList()))

    private fun index(vararg entries: JSONObject) = JSONObject()
        .put("schemaVersion", 2)
        .put("source", "OFFICIAL")
        .put("repository", JSONObject().put("owner", "SuperMonster003").put("repo", "AutoJs6-Official-Plugins-Index").put("branch", "main"))
        .put("items", JSONArray(entries.toList()))
        .toString()

    @Test
    fun `the entry of this plugin gives its newest release`() {
        val text = index(
            entry("io.github.supermonster003.autojs6.plugin.three.prism.camera", release(400, "4.0.0")),
            entry(macaw, release(175, "1.4.0"), release(177, "1.5.0"), release(176, "1.4.1")),
        )

        val found = AppUpdateChecker.decode(text, macaw)!!

        assertEquals("v1.5.0", found.tag)
        assertEquals(177L, found.versionCode)
        assertEquals("3-Macaw Speech 1.5.0", found.name)
        assertEquals("3-Macaw Speech 1.5.0\n\nPublished release.", found.notes)
        assertEquals("https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases/tag/v1.5.0", found.pageUrl)
        assertTrue(UpdateVersionPolicy.isNewer(found.versionCode, 172L))
    }

    @Test
    fun `no entry or no usable release is no release`() {
        assertNull(AppUpdateChecker.decode(index(entry("io.github.supermonster003.autojs6.plugin.three.prism.camera", release(400, "4.0.0"))), macaw))
        assertNull(AppUpdateChecker.decode(index(), macaw))
        assertNull(AppUpdateChecker.decode(index(entry(macaw)), macaw))
        assertNull(AppUpdateChecker.decode(index(JSONObject().put("packageName", macaw)), macaw))
        // Prereleases, missing or non-positive version codes and invalid version names never count.
        // zh-CN: 预发布版, 缺失或非正的 versionCode, 无效的版本名都不算.
        val unusable = index(entry(macaw, release(190, "1.7.0", prerelease = true), release(null), release(0), release(-3), release("191"), release(192, null), release(193, "1.8.0 beta"), release(194, "")))
        assertNull(AppUpdateChecker.decode(unusable, macaw))
    }

    @Test
    fun `a prerelease never hides the newest published release`() {
        val found = AppUpdateChecker.decode(index(entry(macaw, release(177, "1.5.0"), release(190, "1.7.0-rc.1", prerelease = true))), macaw)!!
        assertEquals(177L, found.versionCode)
        assertEquals("v1.5.0", found.tag)
    }

    @Test
    fun `json nulls and long notes are handled`() {
        val long = "x".repeat(AppUpdateChecker.MAXIMUM_NOTES_CHARACTERS + 10)
        val found = AppUpdateChecker.decode(index(entry(macaw, release(177, notes = null), title = null)), macaw)!!
        assertEquals("", found.notes)
        assertEquals("3-Macaw Speech 1.5.0", found.name)
        val truncated = AppUpdateChecker.decode(index(entry(macaw, release(5_000_000_000L, notes = "  $long  "))), macaw)!!
        assertEquals(AppUpdateChecker.MAXIMUM_NOTES_CHARACTERS, truncated.notes.length)
        assertEquals(5_000_000_000L, truncated.versionCode)
    }

    @Test
    fun `the index addresses follow the Plugin Center mirrors`() {
        assertEquals(
            listOf(
                "https://raw.githubusercontent.com/SuperMonster003/AutoJs6-Official-Plugins-Index/refs/heads/main/plugins.official.generated.json",
                "https://cdn.jsdelivr.net/gh/SuperMonster003/AutoJs6-Official-Plugins-Index@main/plugins.official.generated.json",
                "https://fastly.jsdelivr.net/gh/SuperMonster003/AutoJs6-Official-Plugins-Index@main/plugins.official.generated.json",
            ),
            AppUpdateChecker.INDEX_URLS,
        )
        assertTrue(AppUpdateChecker.MAXIMUM_RESPONSE_BYTES >= 4 * 1024 * 1024)
    }

    /**
     * Serves each path with the given status and body on a local port; records the paths asked for.
     * zh-CN: 在本机端口上按路径返回给定的状态码与正文; 记录被请求的路径.
     */
    private fun <T> serve(routes: Map<String, Pair<Int, String>>, block: (base: String, asked: List<String>) -> T): T {
        val asked = java.util.Collections.synchronizedList(mutableListOf<String>())
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            asked += path
            val (status, body) = routes[path] ?: (404 to "")
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            return block("http://127.0.0.1:${server.address.port}", asked)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `a failing address falls back to the next mirror`() {
        val body = index(entry(macaw, release(177L)))
        serve(mapOf("/raw" to (503 to "busy"), "/cdn" to (200 to "not json"), "/fastly" to (200 to body))) { base, asked ->
            val checker = AppUpdateChecker(listOf("$base/raw", "$base/cdn", "$base/fastly"), macaw)
            val result = checker.fetch(checker.Call())
            assertEquals(177L, (result as UpdateResult.Release).release.versionCode)
            assertEquals(listOf("/raw", "/cdn", "/fastly"), asked.toList())
        }
    }

    @Test
    fun `HTTP 404 ends the check as no release`() {
        serve(mapOf("/fastly" to (200 to index(entry(macaw, release(177L)))))) { base, asked ->
            val checker = AppUpdateChecker(listOf("$base/raw", "$base/fastly"), macaw)
            assertEquals(UpdateResult.NoRelease, checker.fetch(checker.Call()))
            assertEquals(listOf("/raw"), asked.toList())
        }
    }

    @Test
    fun `every address failing is a failure`() {
        serve(mapOf("/raw" to (500 to ""), "/cdn" to (502 to ""))) { base, _ ->
            val checker = AppUpdateChecker(listOf("$base/raw", "$base/cdn"), macaw)
            val failure = runCatching { checker.fetch(checker.Call()) }.exceptionOrNull()
            assertTrue("expected an IOException, got $failure", failure is java.io.IOException)
            assertTrue(failure!!.message.orEmpty().contains("HTTP 500"))
        }
    }
}
