package me.proxer.app.base

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.toSingle
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T>(application: Application) : AndroidViewModel(application) {

    val data = MutableLiveData<T?>()
    val error = MutableLiveData<ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    protected var disposable: Disposable? = null

    abstract val endpoint: Endpoint<T>

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    open fun load() {
        disposable?.dispose()
        disposable = endpoint.build()
                .toSingle()
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

    open fun loadIfPossible() {
        if (isLoading.value != true && error.value == null) {
            load()
        }
    }

    open fun refresh() = load()
}
