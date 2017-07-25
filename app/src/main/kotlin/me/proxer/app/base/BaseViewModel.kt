package me.proxer.app.base

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.toSingle
import me.proxer.library.api.ProxerCall

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T>(application: Application) : AndroidViewModel(application) {

    val data = MutableLiveData<T?>()
    val error = MutableLiveData<ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    protected var disposable: Disposable? = null
    protected val loadSingle: Single<T>
        get() = constructApiCall()
                .toSingle()
                .subscribeOn(Schedulers.io())

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    open fun load() {
        disposable?.dispose()
        disposable = loadSingle
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isLoading.value = true }
                .doAfterTerminate { isLoading.value = false }
                .subscribe({
                    data.value = it
                    error.value = null
                }, {
                    data.value = null
                    error.value = ErrorUtils.handle(it)
                })
    }

    open fun loadIfPossible() {
        if (isLoading.value != true && error.value == null) {
            load()
        }
    }

    open fun reload() {
        data.value = null
        error.value = null

        load()
    }

    open fun refresh() = load()

    abstract fun constructApiCall(): ProxerCall<T>
}
