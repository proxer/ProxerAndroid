package me.proxer.app.auth

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.buildOptionalSingle

/**
 * @author Ruben Gees
 */
class LogoutViewModel(application: Application) : AndroidViewModel(application) {

    val data = MutableLiveData<Unit?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    private var dataDisposable: Disposable? = null

    override fun onCleared() {
        dataDisposable?.dispose()
        dataDisposable = null

        super.onCleared()
    }

    fun logout() {
        if (isLoading.value != true) {
            dataDisposable?.dispose()
            dataDisposable = api.user().logout()
                    .buildOptionalSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        error.value = null
                        isLoading.value = true
                    }
                    .doAfterTerminate { isLoading.value = false }
                    .subscribe({
                        data.value = Unit
                    }, {
                        error.value = ErrorUtils.handle(it)
                    })
        }
    }
}