@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import com.uber.autodispose.CompletableSubscribeProxy
import com.uber.autodispose.ObservableSubscribeProxy
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposeWith
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable

/**
 * @author Ruben Gees
 */

inline fun <T> Observable<T>.autoDispose(owner: LifecycleOwner) = this
        .autoDisposeWith(AndroidLifecycleScopeProvider.from(owner, Lifecycle.Event.ON_DESTROY))

inline fun <T> Single<T>.autoDispose(owner: LifecycleOwner) = this
        .autoDisposeWith(AndroidLifecycleScopeProvider.from(owner, Lifecycle.Event.ON_DESTROY))

inline fun Completable.autoDispose(owner: LifecycleOwner) = this
        .autoDisposeWith(AndroidLifecycleScopeProvider.from(owner, Lifecycle.Event.ON_DESTROY))

inline fun <T> Observable<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit,
                                                   noinline onError: (Throwable) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
        onError(it)
    })
}

inline fun <T> Observable<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
    })
}

inline fun <T> Observable<T>.subscribeAndLogErrors(): Disposable? {
    return this.subscribe({}, {
        it.printStackTrace()
    })
}

inline fun <T> Single<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit,
                                               noinline onError: (Throwable) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
        onError(it)
    })
}

inline fun <T> Single<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
    })
}

inline fun <T> Single<T>.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        it.printStackTrace()
    })
}

inline fun Completable.subscribeAndLogErrors(noinline onSuccess: () -> Unit,
                                             noinline onError: (Throwable) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
        onError(it)
    })
}

inline fun Completable.subscribeAndLogErrors(noinline onSuccess: () -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
    })
}

inline fun Completable.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        it.printStackTrace()
    })
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit,
                                                                 noinline onError: (Throwable) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
        onError(it)
    })
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
    })
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        it.printStackTrace()
    })
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(noinline onSuccess: () -> Unit,
                                                           noinline onError: (Throwable) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
        onError(it)
    })
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(noinline onSuccess: () -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        it.printStackTrace()
    })
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        it.printStackTrace()
    })
}

