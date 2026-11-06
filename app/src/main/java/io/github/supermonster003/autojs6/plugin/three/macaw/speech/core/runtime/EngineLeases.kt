package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.Closeable
import java.util.UUID

/**
 * The leases of contract version 3 (roadmap P11.2, D184, D185, D197, D203): a lease holds uses of loaded engines
 * ([EngineCache.Lease]), so they are not released on the idle timer, until its holder releases it, it expires or its
 * holder dies; leases of the same engines are counted by the cache. A holder is a calling uid (all scripts of one host
 * share it; the host keeps each script's leases apart and returns them when the script exits) and holds at most
 * [maxPerHolder] leases, counting the loads still in flight ([reserve]). Lease ids are random, so an id of an earlier
 * plugin process never names a lease of this one. Pure JVM.
 * zh-CN: 契约版本 3 的租约 (路线图 P11.2, D184, D185, D197, D203): 租约持有已加载引擎的使用 ([EngineCache.Lease]), 因此它们不按空闲计时释放,
 * 直到持有者归还, 租约到期或持有者死亡; 同一引擎的多个租约由缓存计数. 持有者是调用方 uid (同一宿主的全部脚本共用; 宿主分别记录每个脚本的租约,
 * 脚本退出时归还), 至多持有 [maxPerHolder] 个租约, 仍在加载的也计入 ([reserve]). 租约 ID 随机生成, 因此此前插件进程的 ID 不会指向本进程的租约. 纯 JVM.
 */
class EngineLeases(
    private val scheduler: IdleScheduler,
    private val maxPerHolder: Int = SpeechLimits.MAX_LEASES_PER_TOKEN,
    private val newId: () -> String = { "lease-${UUID.randomUUID()}" },
) {

    private class Record(val id: String, val holder: Int, val modelId: String, val uses: List<Closeable>, val onEnd: () -> Unit) {
        var cancelExpiry: (() -> Unit)? = null
    }

    /**
     * The place of one lease of [holder], taken before its engines load; [grant] turns it into the lease, [cancel] gives
     * the place back. Use it exactly once.
     * zh-CN: [holder] 的一个租约位置, 在其引擎加载之前占用; [grant] 把它变成租约, [cancel] 归还位置. 恰好使用一次.
     */
    inner class Ticket internal constructor(val holder: Int) {
        private var done = false

        /**
         * The lease of [uses] (engines of [modelId]); [onEnd] runs once when it ends (the death link goes). Closes [uses]
         * and fails when the ticket was used already.
         * zh-CN: [uses] ([modelId] 的引擎) 的租约; 租约结束时 [onEnd] 运行一次 (撤销死亡监听). 票据已用过时关闭 [uses] 并失败.
         */
        fun grant(modelId: String, uses: List<Closeable>, onEnd: () -> Unit = {}): String {
            val id = newId()
            val accepted = synchronized(records) {
                if (done) return@synchronized false
                done = true
                pending[holder] = (pending[holder] ?: 1) - 1
                records[id] = Record(id, holder, modelId, uses, onEnd)
                true
            }
            if (!accepted) {
                uses.forEach { runCatching { it.close() } }
                throw IllegalStateException("The lease ticket was used already")
            }
            return id
        }

        /** Gives the place back unless the lease was granted. zh-CN: 除非租约已发放, 否则归还位置. */
        fun cancel() {
            synchronized(records) {
                if (done) return
                done = true
                pending[holder] = (pending[holder] ?: 1) - 1
            }
        }
    }

    private val records = LinkedHashMap<String, Record>()
    private val pending = HashMap<Int, Int>()

    /**
     * A place for one more lease of [holder]; `LIMIT_EXCEEDED` when it holds or loads [maxPerHolder] already.
     * zh-CN: [holder] 再一个租约的位置; 已持有或正在加载 [maxPerHolder] 个时为 `LIMIT_EXCEEDED`.
     */
    fun reserve(holder: Int): Ticket = synchronized(records) {
        val count = records.values.count { it.holder == holder } + (pending[holder] ?: 0)
        if (count >= maxPerHolder) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "A caller holds at most $maxPerHolder leases; release one with speech.release()")
        }
        pending[holder] = (pending[holder] ?: 0) + 1
        Ticket(holder)
    }

    /**
     * Ends the lease [leaseId] of [holder]; false when it does not exist (released, expired, of a dead holder or of an
     * earlier process) or belongs to another holder, which keeps it.
     * zh-CN: 结束 [holder] 的租约 [leaseId]; 租约不存在 (已归还, 已到期, 持有者已死亡或属于此前的进程) 或属于其他持有者 (其租约保持不变) 时为 false.
     */
    fun release(leaseId: String, holder: Int): Boolean {
        val record = synchronized(records) {
            val found = records[leaseId]?.takeIf { it.holder == holder } ?: return false
            records.remove(leaseId)
            found
        }
        end(record)
        return true
    }

    /** Ends the lease [leaseId] whatever its holder: the holder died. zh-CN: 不论持有者, 结束租约 [leaseId]: 持有者已死亡. */
    fun drop(leaseId: String): Boolean {
        val record = synchronized(records) { records.remove(leaseId) } ?: return false
        end(record)
        return true
    }

    /**
     * Lets the lease [leaseId] end [minutes] from now (`keepLoaded` minutes once its session ended, roadmap P11.2.2);
     * [SpeechLimits.KEEP_LOADED_UNTIL_RELEASED] keeps it until it is released or its holder dies.
     * zh-CN: 让租约 [leaseId] 在 [minutes] 分钟后结束 (会话结束后的 `keepLoaded` 分钟数, 路线图 P11.2.2); [SpeechLimits.KEEP_LOADED_UNTIL_RELEASED]
     * 保留到归还或持有者死亡.
     */
    fun expireAfter(leaseId: String, minutes: Int) {
        if (minutes == SpeechLimits.KEEP_LOADED_UNTIL_RELEASED) return
        require(minutes in 1..SpeechLimits.MAX_KEEP_LOADED_MINUTES) { "keepLoaded minutes out of range: $minutes" }
        synchronized(records) {
            val record = records[leaseId] ?: return
            record.cancelExpiry?.invoke()
            record.cancelExpiry = scheduler.schedule(minutes * 60_000L) { expire(record) }
        }
    }

    /** How many leases [holder] holds now (loads in flight excluded). zh-CN: [holder] 当前持有的租约数 (不含仍在加载的). */
    fun count(holder: Int): Int = synchronized(records) { records.values.count { it.holder == holder } }

    /** The ids of every lease now, oldest first. zh-CN: 当前全部租约的 ID, 从早到晚. */
    fun ids(): List<String> = synchronized(records) { records.keys.toList() }

    /** The model of the lease [leaseId], or null. zh-CN: 租约 [leaseId] 的模型, 不存在时为 null. */
    fun modelOf(leaseId: String): String? = synchronized(records) { records[leaseId]?.modelId }

    private fun expire(record: Record) {
        val expired = synchronized(records) {
            if (records[record.id] !== record) return
            records.remove(record.id)
            record.cancelExpiry = null
            record
        }
        end(expired, timerFired = true)
    }

    private fun end(record: Record, timerFired: Boolean = false) {
        if (!timerFired) synchronized(records) { record.cancelExpiry?.invoke(); record.cancelExpiry = null }
        runCatching { record.onEnd() }
        record.uses.forEach { runCatching { it.close() } }
    }
}
