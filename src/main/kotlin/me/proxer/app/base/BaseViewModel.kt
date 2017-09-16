package me.proxer.app.base

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.settings.AgeConfirmationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T> : ViewModel() {

    open val data = MutableLiveData<T?>()
    open val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    open val isLoading = MutableLiveData<Boolean?>()

    protected open val isLoginRequired = false
    protected open val isAgeConfirmationRequired = false

    protected var dataDisposable: Disposable? = null
    protected val disposables = CompositeDisposable()

    protected abstract val dataSingle: Single<T>

    init {
        disposables += Observable.merge(bus.register(LoginEvent::class.java), bus.register(LogoutEvent::class.java))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (isLoginRequired || isLoginErrorPresent()) reload() }

        disposables += bus.register(AgeConfirmationEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (isAgeConfirmationRequired) reload() }

        disposables += bus.register(CaptchaSolvedEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (error.value?.message == R.string.error_captcha) {
                        reload()
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

    open fun reload() {
        error.value = null
        data.value = null

        load()
    }

    open fun validate() {
        if (isLoginRequired) Validators.validateLogin()
        if (isAgeConfirmationRequired) Validators.validateAgeConfirmation(globalContext)
    }

    private fun isLoginErrorPresent()  = error.value?.message == R.string.error_invalid_token
}
