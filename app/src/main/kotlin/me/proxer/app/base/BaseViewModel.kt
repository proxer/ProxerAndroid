package me.proxer.app.base

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.settings.AgeConfirmationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T>(application: Application) : AndroidViewModel(application) {

    val data = MutableLiveData<T?>()
    val error = MutableLiveData<ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    open protected val isLoginRequired = false
    open protected val isAgeConfirmationRequired = false

    protected var disposable: Disposable? = null

    abstract val endpoint: Endpoint<T>

    private val loginDisposable: Disposable
    private val ageConfirmationDisposable: Disposable

    init {
        loginDisposable = Observable.merge(bus.observe(LoginEvent::class.java), bus.observe(LogoutEvent::class.java))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (isLoginRequired) reload() }

        ageConfirmationDisposable = bus.observe(AgeConfirmationEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (isAgeConfirmationRequired) reload() }
    }

    override fun onCleared() {
        loginDisposable.dispose()
        ageConfirmationDisposable.dispose()
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    open fun load() {
        disposable?.dispose()
        disposable = endpoint.buildSingle()
                .subscribeOn(Schedulers.io())
                .flatMap { Single.fromCallable { it.apply { validate() } } }
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

    open fun reload() {
        error.value = null
        data.value = null

        load()
    }

    open fun validate() {
        if (isLoginRequired) Validators.validateLogin()
        if (isAgeConfirmationRequired) Validators.validateAgeConfirmation(getApplication())
    }
}
