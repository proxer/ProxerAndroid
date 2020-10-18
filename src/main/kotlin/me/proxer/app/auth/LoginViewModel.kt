package me.proxer.app.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalSettings
import me.proxer.library.ProxerApi
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ServerErrorType

/**
 * @author Ruben Gees
 */
class LoginViewModel : ViewModel() {

    val success = MutableLiveData<Unit?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()
    val isTwoFactorAuthenticationEnabled = MutableLiveData<Boolean?>()

    private val api by safeInject<ProxerApi>()
    private val storageHelper by safeInject<StorageHelper>()
    private val preferenceHelper by safeInject<PreferenceHelper>()

    private var dataDisposable: Disposable? = null

    init {
        isTwoFactorAuthenticationEnabled.value = preferenceHelper.isTwoFactorAuthenticationEnabled
    }

    override fun onCleared() {
        dataDisposable?.dispose()
        dataDisposable = null

        super.onCleared()
    }

    fun login(username: String, password: String, secretKey: String?) {
        if (isLoading.value != true) {
            dataDisposable?.dispose()
            dataDisposable = api.user.login(username, password)
                .secretKey(secretKey)
                .buildSingle().doOnSuccess { user ->
                    storageHelper.temporaryToken = user.loginToken
                }
                .flatMap { user -> api.ucp.settings().buildSingle().map { settings -> user to settings } }
                .doOnSuccess { (user, settings) ->
                    storageHelper.user = LocalUser(user.loginToken, user.id, username, user.image)
                    storageHelper.profileSettings = settings.toLocalSettings()
                }
                .doFinally {
                    storageHelper.temporaryToken = null
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribeAndLogErrors(
                    {
                        success.value = Unit
                    },
                    {
                        if (it is ProxerException && it.serverErrorType == ServerErrorType.USER_2FA_SECRET_REQUIRED) {
                            preferenceHelper.isTwoFactorAuthenticationEnabled = true
                            isTwoFactorAuthenticationEnabled.value = true
                        }

                        error.value = ErrorUtils.handle(it)
                    }
                )
        }
    }
}
