package me.proxer.app.profile.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalSettings
import me.proxer.library.ProxerApi
import org.koin.core.KoinComponent

/**
 * @author Ruben Gees
 */
class ProfileSettingsViewModel : ViewModel(), KoinComponent {

    val data = MutableLiveData<LocalProfileSettings>()
    val error = ResettingMutableLiveData<ErrorUtils.ErrorAction>()
    val updateError = ResettingMutableLiveData<ErrorUtils.ErrorAction>()

    private val api by safeInject<ProxerApi>()
    private val storageHelper by safeInject<StorageHelper>()

    private var disposable: Disposable? = null

    init {
        data.value = storageHelper.profileSettings

        refresh()
    }

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    fun refresh() {
        disposable?.dispose()
        disposable = api.ucp.settings()
            .buildSingle()
            .map { it.toLocalSettings() }
            .doOnSuccess { storageHelper.profileSettings = it }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                error.value = null
                updateError.value = null
            }
            .subscribeAndLogErrors({
                data.value = it
            }, {
                error.value = ErrorUtils.handle(it)
            })
    }

    fun update(newData: LocalProfileSettings) {
        data.value = newData

        disposable?.dispose()
        disposable = api.ucp.setSettings(newData.toNonLocalSettings())
            .buildOptionalSingle()
            .doOnSuccess { storageHelper.profileSettings = newData }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                error.value = null
                updateError.value = null
            }
            .subscribeAndLogErrors({}, {
                updateError.value = ErrorUtils.handle(it)
            })
    }

    fun retryUpdate() {
        val safeData = data.value

        if (safeData != null) {
            update(safeData)
        }
    }
}
