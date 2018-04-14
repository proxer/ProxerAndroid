@file:Suppress("NOTHING_TO_INLINE", "MethodOverloading")

package me.proxer.app.util.extension

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.util.Log
import com.uber.autodispose.CompletableSubscribeProxy
import com.uber.autodispose.ObservableSubscribeProxy
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import me.proxer.app.MainApplication.Companion.LOGGING_TAG
import org.jetbrains.anko.getStackTraceString

inline fun <T> Observable<T>.autoDispose(owner: LifecycleOwner) = this
    .autoDisposable(owner.scope(Lifecycle.Event.ON_DESTROY))

inline fun <T> Single<T>.autoDispose(owner: LifecycleOwner) = this
    .autoDisposable(owner.scope(Lifecycle.Event.ON_DESTROY))

inline fun Completable.autoDispose(owner: LifecycleOwner) = this
    .autoDisposable(owner.scope(Lifecycle.Event.ON_DESTROY))

inline fun <T> Observable<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
        onError(it)
    })
}

inline fun <T> Observable<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun <T> Observable<T>.subscribeAndLogErrors(): Disposable? {
    return this.subscribe({}, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun <T> Flowable<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
        onError(it)
    })
}

inline fun <T> Flowable<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun <T> Flowable<T>.subscribeAndLogErrors(): Disposable? {
    return this.subscribe({}, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun <T> Single<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
        onError(it)
    })
}

inline fun <T> Single<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun <T> Single<T>.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun Completable.subscribeAndLogErrors(
    noinline onSuccess: () -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
        onError(it)
    })
}

inline fun Completable.subscribeAndLogErrors(noinline onSuccess: () -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun Completable.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
        onError(it)
    })
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(
    noinline onSuccess: () -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
        onError(it)
    })
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(noinline onSuccess: () -> Unit): Disposable {
    return this.subscribe(onSuccess, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Log.e(LOGGING_TAG, it.getStackTraceString())
    })
}
