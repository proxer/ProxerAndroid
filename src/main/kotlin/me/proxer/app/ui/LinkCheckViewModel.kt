package me.proxer.app.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.ProxerApi
import okhttp3.HttpUrl
import org.koin.core.KoinComponent

class LinkCheckViewModel : ViewModel(), KoinComponent {

    val data = MutableLiveData<Boolean?>()
    val isLoading = MutableLiveData<Boolean?>()

    private val api by safeInject<ProxerApi>()

    private var checkDisposable: Disposable? = null

    override fun onCleared() {
        checkDisposable?.dispose()
        checkDisposable = null

        super.onCleared()
    }

    fun check(link: HttpUrl) {
        checkDisposable?.dispose()

        checkDisposable = api.messenger.checkLink(link).buildSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isLoading.value = true
                data.value = null
            }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors(
                {
                    data.value = it.isSecure
                },
                {
                    data.value = false
                }
            )
    }
}
