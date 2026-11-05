package io.github.supermonster003.autojs6.plugin.three.macaw.speech.transfer

import android.content.SharedPreferences

/**
 * In-memory SharedPreferences for the JVM tests of the settings transfer (the shape of the host library's test fake).
 * zh-CN: 供设置迁移 JVM 测试使用的内存 SharedPreferences (与宿主库测试替身同形).
 */
class FakeSharedPreferences(initial: Map<String, Any> = emptyMap()) : SharedPreferences {

    val values = LinkedHashMap<String, Any>(initial)
    var commits = 0
        private set
    var commitSucceeds = true

    override fun getAll(): MutableMap<String, *> = LinkedHashMap(values)
    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = (values[key] as? Set<String>)?.toMutableSet() ?: defValues
    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = key in values
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val puts = LinkedHashMap<String, Any>()
        private val removes = LinkedHashSet<String>()
        private var clear = false

        override fun putString(key: String, value: String?) = apply { if (value == null) removes.add(key) else puts[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { if (values == null) removes.add(key) else puts[key] = LinkedHashSet(values) }
        override fun putInt(key: String, value: Int) = apply { puts[key] = value }
        override fun putLong(key: String, value: Long) = apply { puts[key] = value }
        override fun putFloat(key: String, value: Float) = apply { puts[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { puts[key] = value }
        override fun remove(key: String) = apply { removes.add(key) }
        override fun clear() = apply { clear = true }

        override fun commit(): Boolean {
            commits++
            if (!commitSucceeds) return false
            if (clear) values.clear()
            removes.forEach { values.remove(it) }
            values.putAll(puts)
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
