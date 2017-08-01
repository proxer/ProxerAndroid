package me.proxer.app.auth

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ServerErrorType
import me.proxer.library.entitiy.user.User

/**
 * @author Ruben Gees
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    val data = MutableLiveData<User?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()
    val isTwoFactorAuthenticationEnabled = MutableLiveData<Boolean?>()

    private var disposable: Disposable? = null

    init {
        isTwoFactorAuthenticationEnabled.value = StorageHelper.isTwoFactorAuthenticationEnabled
    }

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    fun login(username: String, password: String, secretKey: String?) {
        if (isLoading.value != true) {
            disposable?.dispose()
            disposable = api.user().login(username, password)
                    .secretKey(secretKey)
                    .buildSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        error.value = null
                        isLoading.value = true
                    }
                    .doAfterTerminate { isLoading.value = false }
                    .subscribe({
                        data.value = it
                    }, {
                        if (it is ProxerException && it.serverErrorType == ServerErrorType.USER_2FA_SECRET_REQUIRED) {
                            StorageHelper.isTwoFactorAuthenticationEnabled = true
                            isTwoFactorAuthenticationEnabled.value = true
                        }

                        error.value = ErrorUtils.handle(it)
                    })
        }
    }
}
