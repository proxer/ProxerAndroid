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
import me.proxer.library.api.ProxerCall

/**
 * @author Ruben Gees
 */

inline fun <T> ProxerCall<T>.toSingle(): Single<T> = Single.create<T> { emitter ->
    emitter.setCancellable { cancel() }

    try {
        emitter.onSuccess(execute())
    } catch (error: Throwable) {
        if (!emitter.isDisposed) {
            emitter.onError(error)
        }
    }
}

inline fun <T : Any> ProxerCall<T>.toOptionalSingle(): Single<Optional<T>> = Single.create<Optional<T>> { emitter ->
    emitter.setCancellable { cancel() }

    try {
        emitter.onSuccess(execute().toOptional())
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
