package me.proxer.app.ucp.settings

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
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalSettings
import me.proxer.library.ProxerApi
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * @author Ruben Gees
 */
class UcpSettingsViewModel : ViewModel(), KoinComponent {

    val data = MutableLiveData<LocalUcpSettings>()
    val error = ResettingMutableLiveData<ErrorUtils.ErrorAction>()
    val updateError = ResettingMutableLiveData<ErrorUtils.ErrorAction>()
    val isLoading = MutableLiveData<Boolean?>()

    private val api by inject<ProxerApi>()
    private val storageHelper by inject<StorageHelper>()

    private var disposable: Disposable? = null

    init {
        data.value = storageHelper.ucpSettings

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
            .doOnSuccess { storageHelper.ucpSettings = it }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isLoading.value = true
                error.value = null
                updateError.value = null
            }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors({
                data.value = it
            }, {
                error.value = ErrorUtils.handle(it)
            })
    }

    fun update(newData: LocalUcpSettings) {
        data.value = newData

        disposable?.dispose()
        disposable = api.ucp.setSettings(newData.toNonLocalSettings())
            .buildOptionalSingle()
            .doOnSuccess { storageHelper.ucpSettings = newData }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isLoading.value = false
                error.value = null
                updateError.value = null
            }
            .subscribeAndLogErrors({}, {
                updateError.value = ErrorUtils.handle(it)
            })
    }

    fun update() {
        val safeData = data.value

        if (safeData != null) {
            update(safeData)
        }
    }
}
