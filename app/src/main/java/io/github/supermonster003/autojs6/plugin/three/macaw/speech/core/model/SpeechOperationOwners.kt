package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

/** The ordinary-app Binder only lets a caller cancel its own operations, including late creation replies. */
internal class SpeechOperationOwners(private val cancelNative: (Int) -> Unit) : AutoCloseable {
    private val lock = Any()
    private val pending = mutableSetOf<Operation>()
    private val byId = mutableMapOf<Int, Operation>()
    private var closed = false

    fun begin(uid: Int): Operation = synchronized(lock) {
        check(!closed) { "Speech service has closed" }
        Operation(uid).also(pending::add)
    }

    fun cancel(uid: Int, id: Int) {
        synchronized(lock) { byId[id]?.takeIf { it.uid == uid } }?.abort()
    }

    override fun close() {
        val active = synchronized(lock) { closed = true; pending.toList() }
        active.forEach(Operation::abort)
    }

    inner class Operation internal constructor(val uid: Int) {
        private var id: Int? = null
        private var done = false
        private var cancelled = false

        fun attach(value: Int) {
            val abort = synchronized(lock) {
                check(id == null)
                id = value
                if (done) return
                if (cancelled || closed) { cancelled = true; true } else { byId[value] = this; false }
            }
            if (abort) cancelNative(value)
        }

        fun abort() {
            val value = synchronized(lock) {
                if (done || cancelled) return
                cancelled = true
                id
            }
            value?.let(cancelNative)
        }

        fun finish(): Boolean = synchronized(lock) {
            if (done) return false
            done = true
            pending.remove(this)
            id?.let { if (byId[it] === this) byId.remove(it) }
            true
        }
    }
}
