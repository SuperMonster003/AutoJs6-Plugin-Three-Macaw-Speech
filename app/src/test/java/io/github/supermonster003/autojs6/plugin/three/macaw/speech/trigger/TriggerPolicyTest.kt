package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The route of the trigger entries (roadmap D60): the assistant session only while 3-Macaw Speech is the default
 * digital assistant and its service runs, the same sheet directly otherwise; the entry names travel as stable ids.
 * zh-CN: 触发式入口的路由 (路线图 D60): 只有 3-Macaw Speech 是默认数字助理且其服务在运行时经助理会话, 否则直接打开同一个弹层; 入口名以固定 ID 传递.
 */
class TriggerPolicyTest {

    @Test
    fun `the session is used only for the running default assistant`() {
        assertEquals(TriggerRoute.SESSION, TriggerPolicy.route(assistantActive = true, serviceRunning = true))
        // Chosen as the assistant, but the system has not made the service ready yet: the sheet itself.
        assertEquals(TriggerRoute.SHEET, TriggerPolicy.route(assistantActive = true, serviceRunning = false))
        // A stale instance after another assistant was chosen never shows a session.
        assertEquals(TriggerRoute.SHEET, TriggerPolicy.route(assistantActive = false, serviceRunning = true))
        assertEquals(TriggerRoute.SHEET, TriggerPolicy.route(assistantActive = false, serviceRunning = false))
    }

    @Test
    fun `adding the tile asks the system on Android 13 and shows the steps otherwise`() {
        // D61: no system request before API 33, the manual steps instead.
        assertEquals(TileOutcome.MANUAL, TriggerPolicy.tileOutcome(sdk = 24))
        assertEquals(TileOutcome.MANUAL, TriggerPolicy.tileOutcome(sdk = 32))
        assertEquals(TileOutcome.REQUEST, TriggerPolicy.tileOutcome(sdk = 33))
        // StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED / _TILE_ALREADY_ADDED / _TILE_NOT_ADDED.
        assertEquals(TileOutcome.ADDED, TriggerPolicy.tileOutcome(sdk = 33, code = 2))
        assertEquals(TileOutcome.ALREADY_ADDED, TriggerPolicy.tileOutcome(sdk = 35, code = 1))
        assertEquals("a declined request shows nothing", TileOutcome.DECLINED, TriggerPolicy.tileOutcome(sdk = 35, code = 0))
        // Every error code (1000 and higher) and a request that could not be made fall back to the steps.
        listOf(1000, 1001, 1004, 1005, TriggerPolicy.TILE_REQUEST_UNAVAILABLE).forEach { code ->
            assertEquals("code $code", TileOutcome.MANUAL, TriggerPolicy.tileOutcome(sdk = 34, code = code))
        }
    }

    @Test
    fun `entry names are stable ids`() {
        assertEquals(listOf("tile", "widget", "shortcut", "floating", "key", "headset"), TriggerSource.entries.map { it.id })
        TriggerSource.entries.forEach { assertEquals(it, TriggerSource.of(it.id)) }
        assertNull("the assist gesture names no entry", TriggerSource.of(null))
        assertNull(TriggerSource.of("TILE"))
    }
}
