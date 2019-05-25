package me.proxer.app.util.extension

import androidx.annotation.CheckResult
import com.squareup.moshi.Moshi

@CheckResult
inline fun <reified T> Moshi.fromJson(json: String) = adapter(T::class.java).fromJson(json)

@CheckResult
inline fun <reified T> Moshi.toJson(value: T?): String = adapter(T::class.java).toJson(value)
