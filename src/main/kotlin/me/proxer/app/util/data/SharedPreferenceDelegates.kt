@file:Suppress("UndocumentedPublicClass")

package me.proxer.app.util.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.threeten.bp.Instant
import kotlin.reflect.KProperty

class BooleanDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val default: Boolean
) {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>) = sharedPreferences.getBoolean(key, default)

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Boolean) {
        sharedPreferences.edit(commit = true) {
            putBoolean(key, value)
        }
    }
}

class IntDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val default: Int
) {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>) = sharedPreferences.getInt(key, default)

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Int) {
        sharedPreferences.edit(commit = true) {
            putInt(key, value)
        }
    }
}

class LongDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val default: Long
) {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): Long {
        return sharedPreferences.getLong(key, default)
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Long) {
        sharedPreferences.edit(commit = true) {
            putLong(key, value)
        }
    }
}

class StringDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val default: String?
) {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String? {
        return sharedPreferences.getString(key, default)
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String?) {
        sharedPreferences.edit(commit = true) {
            putString(key, value)
        }
    }
}

class InstantDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val default: Instant
) {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): Instant {
        return sharedPreferences.getLong(key, default.epochSecond).let { Instant.ofEpochMilli(it) }
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Instant) {
        sharedPreferences.edit(commit = true) {
            putLong(key, value.toEpochMilli())
        }
    }
}

fun booleanPreference(
    sharedPreferences: SharedPreferences,
    key: String,
    default: Boolean = false
) = BooleanDelegate(sharedPreferences, key, default)

fun intPreference(
    sharedPreferences: SharedPreferences,
    key: String,
    default: Int = 0
) = IntDelegate(sharedPreferences, key, default)

fun longPreference(
    sharedPreferences: SharedPreferences,
    key: String,
    default: Long = 0L
) = LongDelegate(sharedPreferences, key, default)

fun stringPreference(
    sharedPreferences: SharedPreferences,
    key: String,
    default: String? = null
) = StringDelegate(sharedPreferences, key, default)

fun instantPreference(
    sharedPreferences: SharedPreferences,
    key: String,
    default: Instant = Instant.ofEpochMilli(0L)
) = InstantDelegate(sharedPreferences, key, default)
