package me.proxer.app.auth

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.toOptionalSingle

/**
 * @author Ruben Gees
 */
class LogoutViewModel(application: Application) : AndroidViewModel(application) {

    val data = MutableLiveData<Unit?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    private var disposable: Disposable? = null

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    fun logout() {
        if (isLoading.value != true) {
            disposable?.dispose()
            disposable = MainApplication.api.user()
                    .logout()
                    .build()
                    .toOptionalSingle()
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