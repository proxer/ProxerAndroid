package me.proxer.app.newbase

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.rubengees.rxbus.RxBus
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.R
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.base.CaptchaSolvedEvent
import me.proxer.app.settings.AgeConfirmationEvent
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Validators
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.ProxerApi
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * @author Ruben Gees
 */
abstract class NewBaseViewModel<T> : ViewModel(), KoinComponent {

    abstract val data: LiveData<T>
    abstract val networkState: LiveData<NetworkState>

    protected val bus by inject<RxBus>()
    protected val api by inject<ProxerApi>()
    protected val storageHelper by inject<StorageHelper>()
    protected val preferenceHelper by inject<PreferenceHelper>()
    protected val validators by inject<Validators>()

    protected open val isLoginRequired = false
    protected open val isAgeConfirmationRequired = false

    protected val disposables = CompositeDisposable()

    init {
        disposables += Observable.merge(bus.register(LoginEvent::class.java), bus.register(LogoutEvent::class.java))
            .subscribe { if (isLoginRequired || isLoginErrorPresent()) invalidate() }

        disposables += bus.register(CaptchaSolvedEvent::class.java)
            .subscribe { if (isCaptchaErrorPresent()) invalidate() }

        disposables += bus.register(AgeConfirmationEvent::class.java)
            .subscribe { if (isAgeConfirmationRequired) invalidate() }
    }

    override fun onCleared() {
        disposables.dispose()

        super.onCleared()
    }

    open fun invalidate() = Unit

    open fun retry() = Unit

    private fun isLoginErrorPresent(): Boolean {
        return (networkState.value as? NetworkState.Error?)?.errorAction?.buttonMessage == R.string.error_invalid_token
    }

    private fun isCaptchaErrorPresent(): Boolean {
        return (networkState.value as? NetworkState.Error?)?.errorAction?.buttonAction == ButtonAction.CAPTCHA
    }
}
