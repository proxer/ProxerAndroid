package me.proxer.app.util.rx

import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.Exceptions
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.Call
import okhttp3.Response

/**
 * @author Ruben Gees
 */
class CallResponseSingle(private val originalCall: Call) : Single<Response>() {

    override fun subscribeActual(observer: SingleObserver<in Response>) {
        val call = originalCall.clone()
        val disposable = CallDisposable(call)

        observer.onSubscribe(disposable)

        if (disposable.isDisposed) {
            return
        }

        var terminated = false

        try {
            call.execute().use {
                if (!disposable.isDisposed) {
                    terminated = true

                    observer.onSuccess(it)
                }
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

    private class CallDisposable(private val call: Call) : Disposable {

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
