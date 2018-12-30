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
import me.proxer.app.util.Validators
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.ProxerApi
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T> : ViewModel(), KoinComponent {

    open val data = MutableLiveData<T?>()
    open val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    open val isLoading = MutableLiveData<Boolean?>()

    protected open val isLoginRequired = false
    protected open val isAgeConfirmationRequired = false

    protected val bus by inject<RxBus>()
    protected val api by inject<ProxerApi>()
    protected val storageHelper by inject<StorageHelper>()
    protected val preferenceHelper by inject<PreferenceHelper>()
    protected val validators by inject<Validators>()

    protected var dataDisposable: Disposable? = null
    protected val disposables = CompositeDisposable()

    protected abstract val dataSingle: Single<T>

    init {
        disposables += storageHelper.isLoggedInObservable
            .skip(1)
            .subscribe { if (isLoginRequired || isLoginErrorPresent()) reload() }

        disposables += preferenceHelper.isAgeRestrictedMediaAllowedObservable
            .skip(1)
            .subscribe { if (isAgeConfirmationRequired) reload() }

        disposables += bus.register(CaptchaSolvedEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (error.value?.message == R.string.error_captcha) {
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
            .subscribeAndLogErrors({
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
        if (isLoginRequired) validators.validateLogin()
        if (isAgeConfirmationRequired) validators.validateAgeConfirmation()
    }

    private fun isLoginErrorPresent() = error.value?.message == R.string.error_invalid_token
}
