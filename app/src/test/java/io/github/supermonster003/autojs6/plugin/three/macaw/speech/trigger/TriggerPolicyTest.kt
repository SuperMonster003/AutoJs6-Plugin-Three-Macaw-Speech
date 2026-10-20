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
    fun `entry names are stable ids`() {
        assertEquals(listOf("tile", "widget", "shortcut", "floating", "key"), TriggerSource.entries.map { it.id })
        TriggerSource.entries.forEach { assertEquals(it, TriggerSource.of(it.id)) }
        assertNull("the assist gesture names no entry", TriggerSource.of(null))
        assertNull(TriggerSource.of("TILE"))
    }
}
