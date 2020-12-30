@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import io.reactivex.Single
import me.proxer.app.exception.PartialException
import me.proxer.app.util.rx.CallResponseSingle
import me.proxer.app.util.rx.CallStringBodySingle
import me.proxer.app.util.rx.ProxerCallNullableSingle
import me.proxer.app.util.rx.ProxerCallSingle
import me.proxer.library.api.Endpoint
import okhttp3.Call
import okhttp3.Response

inline fun <T : Any> Endpoint<T>.buildSingle() = ProxerCallSingle(build())

@JvmName("buildNullableSingle")
inline fun <T : Any> Endpoint<T?>.buildSingle() = ProxerCallNullableSingle(build())

inline fun <I, T : Any> Endpoint<T>.buildPartialErrorSingle(input: I): Single<T> = buildSingle()
    .onErrorResumeNext { Single.error(PartialException(it, input)) }

inline fun Call.toSingle(): Single<Response> = CallResponseSingle(this)

inline fun Call.toBodySingle(): Single<String> = CallStringBodySingle(this)
