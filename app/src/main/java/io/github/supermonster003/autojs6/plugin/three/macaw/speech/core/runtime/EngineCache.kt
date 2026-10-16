package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import java.io.Closeable
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Identity of a loaded engine: the model, its installed version and the configuration [variant] (decoding method,
 * language), since one model may need differently configured instances.
 * zh-CN: 已加载引擎的身份: 模型, 其已安装版本与配置 [variant] (解码方式, 语言), 同一模型可能需要不同配置的实例.
 */
data class EngineKey(val modelId: String, val versionCode: Int, val variant: String)

/** Delays the idle release; [ScheduledExecutorService] in the app, a manual clock in tests. zh-CN: 延迟空闲释放; 应用中为 [ScheduledExecutorService], 测试中为手动时钟. */
fun interface IdleScheduler {
    fun schedule(delayMillis: Long, task: () -> Unit): () -> Unit

    companion object {
        fun of(executor: ScheduledExecutorService) = IdleScheduler { delay, task ->
            val future = executor.schedule(task, delay, TimeUnit.MILLISECONDS)
            return@IdleScheduler { future.cancel(false) }
        }
    }
}

/**
 * Loaded runtime instances shared by every session and file call that needs the same model (roadmap D30): [acquire]
 * loads on first use (on the caller's thread, which is never a Binder main path for sessions) and counts the users,
 * [Lease.close] gives a use back, and an instance nobody used for [idleMillis] is released. Loading one key never
 * blocks the others. [inUse] answers `ModelManager.isModelInUse` (a model in use cannot be removed), [evict] drops the
 * idle instances of a removed model. Pure JVM.
 * zh-CN: 由需要同一模型的所有会话与文件调用共享的已加载运行时实例 (路线图 D30): [acquire] 在首次使用时加载 (在调用方线程上, 对会话而言从不是 Binder
 * 主路径) 并计数使用者, [Lease.close] 归还一次使用, [idleMillis] 内无人使用的实例被释放. 加载一个键不会阻塞其他键. [inUse] 回答
 * `ModelManager.isModelInUse` (使用中的模型不能删除), [evict] 丢弃已删除模型的空闲实例. 纯 JVM.
 */
class EngineCache(
    private val idleMillis: Long,
    private val scheduler: IdleScheduler,
    private val log: (String) -> Unit = {},
) {

    internal class Entry(val key: EngineKey) {
        var engine: Closeable? = null
        var users = 0
        var cancelIdle: (() -> Unit)? = null
        val loadLock = Any()
    }

    /** One use of a loaded engine; close it exactly once. zh-CN: 已加载引擎的一次使用; 恰好关闭一次. */
    inner class Lease<E : Closeable> internal constructor(private val entry: Entry, val engine: E) : Closeable {
        private var closed = false

        val key: EngineKey get() = entry.key

        override fun close() {
            synchronized(entries) {
                if (closed) return
                closed = true
            }
            release(entry)
        }
    }

    private val entries = LinkedHashMap<EngineKey, Entry>()

    @Suppress("UNCHECKED_CAST")
    fun <E : Closeable> acquire(key: EngineKey, load: () -> E): Lease<E> {
        val entry = synchronized(entries) {
            entries.getOrPut(key) { Entry(key) }.also { item ->
                item.users++
                item.cancelIdle?.invoke()
                item.cancelIdle = null
            }
        }
        try {
            synchronized(entry.loadLock) {
                if (entry.engine == null) {
                    entry.engine = load()
                    log("Loaded ${key.modelId} v${key.versionCode} (${key.variant})")
                }
            }
        } catch (t: Throwable) {
            release(entry)
            throw t
        }
        return Lease(entry, entry.engine as E)
    }

    fun inUse(modelId: String): Boolean = synchronized(entries) { entries.values.any { it.key.modelId == modelId && it.users > 0 } }

    /** Keys of the instances currently loaded. zh-CN: 当前已加载实例的键. */
    fun loaded(): List<EngineKey> = synchronized(entries) { entries.values.filter { it.engine != null }.map { it.key } }

    /** Releases the idle instances of [modelId] now. zh-CN: 立即释放 [modelId] 的空闲实例. */
    fun evict(modelId: String) {
        val idle = synchronized(entries) {
            entries.values.filter { it.key.modelId == modelId && it.users == 0 }.onEach { item ->
                item.cancelIdle?.invoke()
                entries.remove(item.key)
            }
        }
        idle.forEach(::closeEngine)
    }

    /** Releases every idle instance now (memory pressure). zh-CN: 立即释放全部空闲实例 (内存紧张时). */
    fun trim() {
        val idle = synchronized(entries) {
            entries.values.filter { it.users == 0 }.onEach { item ->
                item.cancelIdle?.invoke()
                entries.remove(item.key)
            }
        }
        idle.forEach(::closeEngine)
    }

    private fun release(entry: Entry) {
        val discard = synchronized(entries) {
            entry.users--
            if (entry.users > 0) return
            if (entry.engine == null) {
                // The load failed: nothing to keep.
                if (entries[entry.key] === entry) entries.remove(entry.key)
                return
            }
            if (idleMillis <= 0) {
                entries.remove(entry.key)
                true
            } else {
                entry.cancelIdle = scheduler.schedule(idleMillis) { expire(entry) }
                false
            }
        }
        if (discard) closeEngine(entry)
    }

    private fun expire(entry: Entry) {
        synchronized(entries) {
            if (entry.users > 0 || entries[entry.key] !== entry) return
            entries.remove(entry.key)
            entry.cancelIdle = null
        }
        closeEngine(entry)
    }

    private fun closeEngine(entry: Entry) {
        val engine = synchronized(entry.loadLock) { entry.engine.also { entry.engine = null } } ?: return
        runCatching { engine.close() }
        log("Released ${entry.key.modelId} v${entry.key.versionCode} (${entry.key.variant})")
    }
}
