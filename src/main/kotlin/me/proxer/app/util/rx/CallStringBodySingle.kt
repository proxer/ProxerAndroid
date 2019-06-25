package me.proxer.app.util.rx

import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.Exceptions
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.Call
import java.io.IOException

/**
 * @author Ruben Gees
 */
class CallStringBodySingle(private val originalCall: Call) : Single<String>() {

    override fun subscribeActual(observer: SingleObserver<in String>) {
        val call = originalCall.clone()
        val disposable = CallDisposable(call)

        observer.onSubscribe(disposable)

        if (disposable.isDisposed) {
            return
        }

        var terminated = false

        try {
            call.execute().use {
                val body = it.body?.string()

                if (!disposable.isDisposed) {
                    terminated = true

                    if (body == null) {
                        observer.onError(IOException(NullPointerException("body is null")))
                    } else {
                        observer.onSuccess(body)
                    }
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
