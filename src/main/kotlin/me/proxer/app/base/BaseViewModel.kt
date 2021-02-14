package me.proxer.app.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rubengees.rxbus.RxBus
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Validators
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.ProxerApi

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T> : ViewModel() {

    open val data = MutableLiveData<T?>()
    open val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    open val isLoading = MutableLiveData<Boolean?>()

    protected open val isLoginRequired = true
    protected open val isAgeConfirmationRequired = false

    protected val bus by safeInject<RxBus>()
    protected val api by safeInject<ProxerApi>()
    protected val storageHelper by safeInject<StorageHelper>()
    protected val preferenceHelper by safeInject<PreferenceHelper>()
    protected val validators by safeInject<Validators>()

    protected var dataDisposable: Disposable? = null
    protected val disposables = CompositeDisposable()

    protected abstract val dataSingle: Single<T>

    init {
        disposables += storageHelper.isLoggedInObservable
            .subscribe { if (isLoginRequired || isLoginErrorPresent()) reload() }

        disposables += preferenceHelper.isAgeRestrictedMediaAllowedObservable
            .subscribe { if (isAgeConfirmationRequired) reload() }

        disposables += bus.register(CaptchaSolvedEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (error.value?.buttonAction == ButtonAction.CAPTCHA) {
                    refresh()
                }
            }

        disposables += bus.register(NetworkConnectedEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (error.value?.buttonAction == ButtonAction.NETWORK_SETTINGS) {
                    refresh()
                }
            }
    }

    override fun onCleared() {
        dataDisposable?.dispose()
        disposables.dispose()

        dataDisposable = null

        super.onCleared()
    }

    open fun load() {
        dataDisposable?.dispose()
        dataDisposable = dataSingle
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isLoading.value = true
                error.value = null
                data.value = null
            }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors(
                {
                    error.value = null
                    data.value = it
                },
                {
                    data.value = null
                    error.value = ErrorUtils.handle(it)
                }
            )
    }

    open fun loadIfPossible() {
        if (isLoading.value != true && error.value == null) {
            load()
        }
    }

    open fun refresh() = load()

    open fun reload() {
        error.value = null
        data.value = null

        load()
    }

    open fun validate() {
        if (isLoginRequired) validators.validateLogin()
        if (isAgeConfirmationRequired) validators.validateAgeConfirmation()
    }

    private fun isLoginErrorPresent() = error.value?.message == R.string.error_invalid_token
}
