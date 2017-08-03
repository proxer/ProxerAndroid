@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding2.support.v7.widget.scrollEvents
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableContainer
import me.proxer.app.util.ErrorUtils.PartialException
import me.proxer.library.api.Endpoint
import okhttp3.Call
import java.io.IOException

/**
 * @author Ruben Gees
 */

fun <T> Endpoint<T>.buildSingle(): Single<T> = Single.create { emitter ->
    val call = build()

    emitter.setCancellable { call.cancel() }

    try {
        emitter.onSuccess(call.execute())
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
        emitter.onSuccess(call.execute())
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(PartialException(error, input))
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
                emitter.onError(IOException())
            }
        } else {
            emitter.onError(IOException())
        }
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(error)
        }
    }
}

operator fun DisposableContainer.plus(disposable: Disposable) {
    add(disposable)
}

fun RecyclerView.endScrolls(threshold: Int = 5): Observable<Unit> = scrollEvents()
        .filter {
            layoutManager.let {
                val pastVisibleItems = when (it) {
                    is StaggeredGridLayoutManager -> {
                        IntArray(it.spanCount).apply {
                            it.findFirstVisibleItemPositions(this)
                        }.let { firstVisibleItems ->
                            when (firstVisibleItems.isNotEmpty()) {
                                true -> firstVisibleItems[0]
                                false -> 0
                            }
                        }
                    }
                    is LinearLayoutManager -> it.findFirstVisibleItemPosition()
                    else -> 0
                }

                it.itemCount > 0 && it.childCount + pastVisibleItems >= it.itemCount - threshold
            }
        }
        .map { Unit }
