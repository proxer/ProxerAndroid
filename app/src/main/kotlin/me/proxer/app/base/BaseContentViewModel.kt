package me.proxer.app.base

import android.app.Application
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
abstract class BaseContentViewModel<T>(application: Application) : BaseViewModel<T>(application) {

    protected var disposable: Disposable? = null

    abstract protected val endpoint: Endpoint<T>

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    override fun load() {
        disposable?.dispose()
        disposable = Single.fromCallable { validate() }
                .flatMap { endpoint.buildSingle() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribe({
                    error.value = null
                    data.value = it
                }, {
                    data.value = null
                    error.value = ErrorUtils.handle(it)
                })
    }
}
