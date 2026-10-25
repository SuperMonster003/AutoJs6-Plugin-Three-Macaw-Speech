package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.SystemClock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Peak resident memory: `VmHWM` after writing 5 to `/proc/self/clear_refs`, with `VmRSS` sampled every 50 ms as the
 * fallback where the reset is refused.
 * zh-CN: 峰值常驻内存: 向 `/proc/self/clear_refs` 写 5 之后的 `VmHWM`; 重置被拒绝时改用每 50 ms 采样的 `VmRSS`.
 */
internal class PeakMemory {
    data class Peak(val mib: Long, val method: String)

    private val running = AtomicBoolean(false)
    private val sampled = AtomicLong(0)
    private var reset = false
    private var thread: Thread? = null

    fun start() {
        reset = runCatching { File("/proc/self/clear_refs").writeText("5") }.isSuccess
        running.set(true)
        thread = Thread {
            while (running.get()) {
                sampled.accumulateAndGet(status("VmRSS")) { a, b -> maxOf(a, b) }
                SystemClock.sleep(50)
            }
        }.apply { start() }
    }

    fun stop(): Peak {
        running.set(false)
        thread?.join()
        sampled.accumulateAndGet(status("VmRSS")) { a, b -> maxOf(a, b) }
        val hwm = status("VmHWM")
        return if (reset && hwm > 0) Peak(hwm shr 10, "VmHWM") else Peak(sampled.get() shr 10, "sampled VmRSS")
    }

    /** A `/proc/self/status` field in KiB. zh-CN: `/proc/self/status` 中的字段 (KiB). */
    private fun status(field: String): Long = File("/proc/self/status").readLines().firstOrNull { it.startsWith("$field:") }
        ?.substringAfter(':')?.trim()?.substringBefore(' ')?.toLongOrNull() ?: 0
}
