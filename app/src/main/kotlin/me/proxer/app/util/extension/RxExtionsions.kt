@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
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