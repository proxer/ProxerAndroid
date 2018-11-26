package me.proxer.app.chat.pub.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.ProxerApi
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * @author Ruben Gees
 */
class ChatReportViewModel : ViewModel(), KoinComponent {

    val data = MutableLiveData<Unit?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    private val api by inject<ProxerApi>()

    private var dataDisposable: Disposable? = null

    override fun onCleared() {
        dataDisposable?.dispose()
        dataDisposable = null

        super.onCleared()
    }

    fun sendReport(messageId: String, message: String) {
        if (isLoading.value != true) {
            dataDisposable?.dispose()
            dataDisposable = api.chat().reportMessage(messageId, message)
                .buildOptionalSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribeAndLogErrors({
                    data.value = Unit
                }, {
                    error.value = ErrorUtils.handle(it)
                })
        }
    }
}
