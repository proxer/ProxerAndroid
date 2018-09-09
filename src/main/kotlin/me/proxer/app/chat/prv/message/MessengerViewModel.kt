package me.proxer.app.chat.prv.message

import androidx.lifecycle.MediatorLiveData
import com.gojuno.koptional.Some
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.messengerDao
import me.proxer.app.base.PagedViewModel
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.sync.MessengerErrorEvent
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
class MessengerViewModel(initialConference: LocalConference) : PagedViewModel<LocalMessage>() {

    override val isLoginRequired = true
    override val itemsOnPage = MessengerWorker.MESSAGES_ON_PAGE

    @Suppress("UNUSED_PARAMETER")
    override var hasReachedEnd
        get() = safeConference.isFullyLoaded
        set(value) = Unit

    override val data = object : MediatorLiveData<List<LocalMessage>>() {

        private val source by lazy { messengerDao.getMessagesLiveDataForConference(safeConference.id) }

        override fun onActive() {
            super.onActive()

            addSource(source) {
                it?.let { _ ->
                    if (StorageHelper.isLoggedIn) {
                        if (it.isEmpty() && !hasReachedEnd) {
                            MessengerWorker.enqueueMessageLoad(safeConference.id)
                        } else {
                            if (error.value == null) {
                                dataDisposable?.dispose()

                                page = it.size.div(itemsOnPage)

                                isLoading.value = false
                                error.value = null
                                this.value = it

                                Completable.fromAction { messengerDao.markConferenceAsRead(safeConference.id) }
                                    .subscribeOn(Schedulers.io())
                                    .subscribeAndLogErrors()
                            }
                        }
                    }
                }
            }
        }

        override fun onInactive() {
            removeSource(source)

            super.onInactive()
        }
    }

    override val dataSingle: Single<List<LocalMessage>>
        get() = Single.fromCallable { Validators.validateLogin() }
            .flatMap {
                when (page) {
                    0 -> messengerDao.markConferenceAsRead(safeConference.id)
                    else -> MessengerWorker.enqueueMessageLoad(safeConference.id)
                }

                Single.never<List<LocalMessage>>()
            }

    val conference = object : MediatorLiveData<LocalConference>() {

        private val source by lazy { messengerDao.getConferenceLiveData(safeConference.id) }

        override fun onActive() {
            super.onActive()

            addSource(source) {
                it?.let { _ -> this.value = it }
            }
        }

        override fun onInactive() {
            removeSource(source)

            super.onInactive()
        }
    }

    val draft = ResettingMutableLiveData<String?>()

    private val safeConference: LocalConference
        get() = conference.value ?: throw IllegalArgumentException("Conference cannot be null")

    private var draftDisposable: Disposable? = null

    init {
        conference.value = initialConference

        disposables += bus.register(MessengerErrorEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event: MessengerErrorEvent ->
                if (event.error is ChatMessageException) {
                    dataDisposable?.dispose()

                    isLoading.value = false
                    error.value = ErrorUtils.handle(event.error)
                }
            }
    }

    override fun onCleared() {
        draftDisposable?.dispose()
        draftDisposable = null

        super.onCleared()
    }

    fun loadDraft() {
        draftDisposable?.dispose()
        draftDisposable = Single.fromCallable {
            StorageHelper.getMessageDraft(safeConference.id.toString()).toOptional()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { it -> if (it is Some) draft.value = it.value }
    }

    fun updateDraft(draft: String) {
        draftDisposable?.dispose()
        draftDisposable = Single
            .fromCallable {
                if (draft.isBlank()) {
                    StorageHelper.deleteMessageDraft(safeConference.id.toString())
                } else {
                    StorageHelper.putMessageDraft(safeConference.id.toString(), draft)
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    fun sendMessage(text: String) {
        disposables += Single
            .fromCallable { messengerDao.insertMessageToSend(text, safeConference.id) }
            .doOnSuccess { if (!MessengerWorker.isRunning()) MessengerWorker.enqueueSynchronization() }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
