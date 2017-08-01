package me.proxer.app.base

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import me.proxer.app.MainApplication
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.settings.AgeConfirmationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T>(application: Application) : AndroidViewModel(application) {

    val data = MutableLiveData<T?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    open protected val isLoginRequired = false
    open protected val isAgeConfirmationRequired = false

    private val loginDisposable: Disposable
    private val ageConfirmationDisposable: Disposable

    init {
        loginDisposable = Observable.merge(MainApplication.bus.register(LoginEvent::class.java), MainApplication.bus.register(LogoutEvent::class.java))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (isLoginRequired) reload() }

        ageConfirmationDisposable = MainApplication.bus.register(AgeConfirmationEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (isAgeConfirmationRequired) reload() }
    }

    override fun onCleared() {
        loginDisposable.dispose()
        ageConfirmationDisposable.dispose()

        super.onCleared()
    }

    open fun load() = Unit

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