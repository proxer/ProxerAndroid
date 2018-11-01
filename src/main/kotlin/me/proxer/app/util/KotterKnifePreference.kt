package me.proxer.app.util

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.util.Collections
import java.util.WeakHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object KotterKnifePreference {
    fun reset(target: Any) = LazyRegistry.reset(target)
}

fun <V : Preference> PreferenceFragmentCompat.bindPreference(key: CharSequence):
    ReadOnlyProperty<PreferenceFragmentCompat, V> = required(key, preferenceFinder)

private val PreferenceFragmentCompat.preferenceFinder: PreferenceFragmentCompat.(CharSequence) -> Preference?
    get() = { findPreference(it) }

@Suppress("UNCHECKED_CAST")
private fun <T, V : Preference> required(key: CharSequence, finder: T.(CharSequence) -> Preference?) =
    Lazy { t: T, desc -> t.finder(key) as V? ?: preferenceNotFound(key, desc) }

private fun preferenceNotFound(key: CharSequence, desc: KProperty<*>): Nothing =
    throw IllegalStateException("Preference KEY $key for '${desc.name}' not found.")

// Like Kotlin's lazy delegate but the initializer gets the target and metadata passed to it
private class Lazy<in T, out V>(private val initializer: (T, KProperty<*>) -> V) : ReadOnlyProperty<T, V> {
    private object EMPTY

    private var value: Any? = EMPTY

    @Suppress("UnsafeCallOnNullableType")
    override fun getValue(thisRef: T, property: KProperty<*>): V {
        LazyRegistry.register(thisRef!!, this)

        if (value == EMPTY) {
            value = initializer(thisRef, property)
        }

        @Suppress("UNCHECKED_CAST")
        return value as V
    }

    fun reset() {
        value = EMPTY
    }
}

private object LazyRegistry {
    private val lazyMap = WeakHashMap<Any, MutableCollection<Lazy<*, *>>>()

    fun register(target: Any, lazy: Lazy<*, *>) {
        lazyMap.getOrPut(target) { Collections.newSetFromMap(WeakHashMap()) }.add(lazy)
    }

    fun reset(target: Any) {
        lazyMap[target]?.forEach { it.reset() }
    }
}
