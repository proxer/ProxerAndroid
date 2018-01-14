package me.proxer.app.util.extension

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding2.support.v7.widget.scrollEvents
import io.reactivex.Observable
import io.reactivex.Single
import me.proxer.app.exception.PartialException
import me.proxer.library.api.Endpoint
import okhttp3.Call
import okhttp3.Response
import java.io.IOException

fun <T> Endpoint<T>.buildSingle(): Single<T> = Single.create { emitter ->
    val call = build()

    emitter.setCancellable { call.cancel() }

    try {
        emitter.onSuccess(call.safeExecute())
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(error)
        }
    }
}

fun <T : Any> Endpoint<T>.buildOptionalSingle(): Single<Optional<T>> = Single.create { emitter ->
    val call = build()

    emitter.setCancellable { call.cancel() }

    try {
        emitter.onSuccess(call.execute().toOptional())
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(error)
        }
    }
}

fun <I : Any, T : Any> Endpoint<T>.buildPartialErrorSingle(input: I): Single<T> = Single.create { emitter ->
    val call = build()

    emitter.setCancellable { call.cancel() }

    try {
        emitter.onSuccess(call.safeExecute())
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(PartialException(error, input))
        }
    }
}

fun Call.toSingle(): Single<Response> = Single.create { emitter ->
    emitter.setCancellable { cancel() }

    try {
        val result = execute()

        result.body()?.close()

        if (result.isSuccessful) {
            emitter.onSuccess(result)
        } else {
            if (!emitter.isDisposed) {
                emitter.onError(IOException("Load failed: ${result.message()}"))
            }
        }
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(error)
        }
    }
}

fun Call.toBodySingle(): Single<String> = Single.create { emitter ->
    emitter.setCancellable { cancel() }

    try {
        val result = execute()

        if (result.isSuccessful) {
            val body = result.body()

            if (body != null) {
                emitter.onSuccess(body.string())
            } else {
                if (!emitter.isDisposed) {
                    emitter.onError(IOException("body is null"))
                }
            }
        } else {
            if (!emitter.isDisposed) {
                emitter.onError(IOException("Load failed: ${result.message()}"))
            }
        }
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(error)
        }
    }
}

fun RecyclerView.endScrolls(threshold: Int = 5): Observable<Unit> = scrollEvents()
        .filter {
            layoutManager.let {
                val pastVisibleItems = when (it) {
                    is StaggeredGridLayoutManager -> {
                        val visibleItemPositions = IntArray(it.spanCount).apply {
                            it.findFirstVisibleItemPositions(this)
                        }

                        when (visibleItemPositions.isNotEmpty()) {
                            true -> visibleItemPositions[0]
                            false -> 0
                        }
                    }
                    is LinearLayoutManager -> it.findFirstVisibleItemPosition()
                    else -> 0
                }

                it.itemCount > 0 && it.childCount + pastVisibleItems >= it.itemCount - threshold
            }
        }
        .map { Unit }
