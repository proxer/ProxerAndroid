package me.proxer.app.chat.prv.conference

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.base.BaseViewModel
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerErrorEvent
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.safeInject
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ConferenceViewModel(searchQuery: String) : BaseViewModel<List<ConferenceWithMessage>>() {

    override val isLoginRequired = true

    override val data = MediatorLiveData<List<ConferenceWithMessage>?>()

    override val dataSingle: Single<List<ConferenceWithMessage>>
        get() = Single
            .fromCallable { validators.validateLogin() }
            .flatMap {
                if (!MessengerWorker.isRunning) MessengerWorker.enqueueSynchronization()

                Single.never()
            }

    var searchQuery: String = searchQuery
        set(value) {
            field = value

            source = messengerDao.getConferencesLiveData(value)
        }

    private val messengerDao by safeInject<MessengerDao>()

    private val sourceObserver = Observer<List<ConferenceWithMessage>?> {
        if (it != null) {
            val containsRelevantData = it.isNotEmpty() || storageHelper.areConferencesSynchronized

            if (containsRelevantData && storageHelper.isLoggedIn && error.value == null) {
                dataDisposable?.dispose()

                isLoading.value = false
                error.value = null
                data.value = it
            }
        }
    }

    private var source by Delegates.observable(messengerDao.getConferencesLiveData(searchQuery)) { _, old, new ->
        data.removeSource(old)
        data.addSource(new, sourceObserver)
    }

    init {
        data.addSource(source, sourceObserver)

        disposables += bus.register(MessengerErrorEvent::class.java)
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
    }
}
