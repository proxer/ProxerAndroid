package me.proxer.app.util.rx

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.Exceptions
import io.reactivex.plugins.RxJavaPlugins
import me.proxer.library.ProxerCall

/**
 * @author Ruben Gees
 */
class ProxerCallNullableSingle<T : Any>(private val originalCall: ProxerCall<T?>) : Single<Optional<T>>() {

    override fun subscribeActual(observer: SingleObserver<in Optional<T>>) {
        val call = originalCall.clone()
        val disposable = CallDisposable(call)

        observer.onSubscribe(disposable)

        if (disposable.isDisposed) {
            return
        }

        var terminated = false

        try {
            val response = call.execute().toOptional()

            if (!disposable.isDisposed) {
                terminated = true

                observer.onSuccess(response)
            }
        } catch (t: Throwable) {
            Exceptions.throwIfFatal(t)

            if (terminated) {
                RxJavaPlugins.onError(t)
            } else if (!disposable.isDisposed) {
                try {
                    observer.onError(t)
                } catch (inner: Throwable) {
                    Exceptions.throwIfFatal(inner)
                    RxJavaPlugins.onError(CompositeException(t, inner))
                }
            }
        }
    }

    private class CallDisposable(private val call: ProxerCall<*>) : Disposable {

        @Volatile
        private var isDisposed: Boolean = false

        override fun dispose() {
            isDisposed = true

            call.cancel()
        }

        override fun isDisposed(): Boolean {
            return isDisposed
        }
    }
}
