package me.proxer.app.chat.conference

import android.arch.lifecycle.MediatorLiveData
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.base.BaseViewModel
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.sync.ChatErrorEvent
import me.proxer.app.chat.sync.ChatJob
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ConferenceViewModel : BaseViewModel<List<LocalConference>>() {

    override val isLoginRequired = true

    override val data = MediatorLiveData<List<LocalConference>?>().apply {
        this.addSource(chatDao.getConferencesLiveData(), {
            it?.let {
                if (StorageHelper.user != null) {
                    if (!it.isEmpty() || StorageHelper.areConferencesSynchronized) {
                        if (error.value == null) {
                            dataDisposable?.dispose()

                            isLoading.value = false
                            error.value = null
                            this.value = it
                        }
                    }
                }
            }
        })
    }

    override val dataSingle: Single<List<LocalConference>>
        get() = Single
                .fromCallable { Validators.validateLogin() }
                .flatMap { Single.never<List<LocalConference>>() }

    init {
        disposables += bus.register(ChatErrorEvent::class.java)
                .map { ErrorUtils.handle(it.error) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (isLoading.value == true) {
                        dataDisposable?.dispose()

                        isLoading.value = false
                        data.value = null
                        error.value = it
                    }
                }

        Completable
                .fromAction { if (!ChatJob.isRunning()) ChatJob.scheduleSynchronization() }
                .subscribeOn(Schedulers.io())
                .subscribeAndLogErrors()
    }
}
