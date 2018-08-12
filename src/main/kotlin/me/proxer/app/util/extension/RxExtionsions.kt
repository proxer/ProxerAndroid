@file:Suppress("NOTHING_TO_INLINE", "MethodOverloading")

package me.proxer.app.util.extension

import android.arch.lifecycle.LifecycleOwner
import com.uber.autodispose.CompletableSubscribeProxy
import com.uber.autodispose.ObservableSubscribeProxy
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import timber.log.Timber

inline fun <T> Observable<T>.autoDispose(owner: LifecycleOwner) = this
    .autoDisposable(owner.scope())

inline fun <T> Single<T>.autoDispose(owner: LifecycleOwner) = this
    .autoDisposable(owner.scope())

inline fun Completable.autoDispose(owner: LifecycleOwner) = this
    .autoDisposable(owner.scope())

inline fun <T> Observable<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
        onError(it)
    }
}

inline fun <T> Observable<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
    }
}

inline fun <T> Observable<T>.subscribeAndLogErrors(): Disposable? {
    return this.subscribe({}, {
        Timber.e(it)
    })
}

inline fun <T> Flowable<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
        onError(it)
    }
}

inline fun <T> Flowable<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
    }
}

inline fun <T> Flowable<T>.subscribeAndLogErrors(): Disposable? {
    return this.subscribe({}, {
        Timber.e(it)
    })
}

inline fun <T> Single<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
        onError(it)
    }
}

inline fun <T> Single<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
    }
}

inline fun <T> Single<T>.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Timber.e(it)
    })
}

inline fun Completable.subscribeAndLogErrors(
    noinline onSuccess: () -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
        onError(it)
    }
}

inline fun Completable.subscribeAndLogErrors(noinline onSuccess: () -> Unit): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
    }
}

inline fun Completable.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Timber.e(it)
    })
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(
    noinline onSuccess: (T) -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
        onError(it)
    }
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(noinline onSuccess: (T) -> Unit): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
    }
}

inline fun <T> ObservableSubscribeProxy<T>.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Timber.e(it)
    })
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(
    noinline onSuccess: () -> Unit,
    noinline onError: (Throwable) -> Unit
): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
        onError(it)
    }
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(noinline onSuccess: () -> Unit): Disposable {
    return this.subscribe(onSuccess) {
        Timber.e(it)
    }
}

inline fun CompletableSubscribeProxy.subscribeAndLogErrors(): Disposable {
    return this.subscribe({}, {
        Timber.e(it)
    })
}
