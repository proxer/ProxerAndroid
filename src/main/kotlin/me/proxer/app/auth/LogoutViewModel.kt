package me.proxer.app.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.ProxerApi
import org.koin.core.KoinComponent

/**
 * @author Ruben Gees
 */
class LogoutViewModel : ViewModel(), KoinComponent {

    val success = MutableLiveData<Unit?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    private val api by safeInject<ProxerApi>()

    private var dataDisposable: Disposable? = null

    override fun onCleared() {
        dataDisposable?.dispose()
        dataDisposable = null

        super.onCleared()
    }

    fun logout() {
        if (isLoading.value != true) {
            dataDisposable?.dispose()
            dataDisposable = api.user.logout()
                .buildOptionalSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribeAndLogErrors({
                    success.value = Unit
                }, {
                    error.value = ErrorUtils.handle(it)
                })
        }
    }
}
