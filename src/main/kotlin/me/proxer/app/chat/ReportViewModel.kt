package me.proxer.app.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
abstract class ReportViewModel : ViewModel() {

    val data = MutableLiveData<Unit?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()

    private var dataDisposable: Disposable? = null

    override fun onCleared() {
        dataDisposable?.dispose()
        dataDisposable = null

        super.onCleared()
    }

    fun sendReport(messageId: String, message: String) {
        if (isLoading.value != true) {
            dataDisposable?.dispose()
            dataDisposable = reportSingle(messageId, message)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribeAndLogErrors(
                    {
                        data.value = Unit
                    },
                    {
                        error.value = ErrorUtils.handle(it)
                    }
                )
        }
    }

    protected abstract fun reportSingle(id: String, message: String): Single<*>
}
