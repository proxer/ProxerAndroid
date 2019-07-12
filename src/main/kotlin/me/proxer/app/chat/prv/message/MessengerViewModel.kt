package me.proxer.app.chat.prv.message

import androidx.lifecycle.MediatorLiveData
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedViewModel
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerErrorEvent
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.koin.core.inject

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

    override val data = MediatorLiveData<List<LocalMessage>>()
    val conference = MediatorLiveData<LocalConference>()
    val draft = ResettingMutableLiveData<String?>()

    override val dataSingle: Single<List<LocalMessage>>
        get() = Single.fromCallable { validators.validateLogin() }
            .flatMap {
                when (page) {
                    0 -> messengerDao.markConferenceAsRead(safeConference.id)
                    else -> if (!hasReachedEnd) MessengerWorker.enqueueMessageLoad(safeConference.id)
                }

                Single.never<List<LocalMessage>>()
            }

    private val dataSource: (List<LocalMessage>?) -> Unit = {
        if (it != null && storageHelper.isLoggedIn) {
            if (it.isEmpty()) {
                if (!hasReachedEnd) {
                    MessengerWorker.enqueueMessageLoad(safeConference.id)
                }
            } else {
                if (error.value == null) {
                    dataDisposable?.dispose()

                    page = it.size / itemsOnPage

                    isLoading.value = false
                    error.value = null
                    data.value = it

                    Completable.fromAction { messengerDao.markConferenceAsRead(safeConference.id) }
                        .subscribeOn(Schedulers.io())
                        .subscribeAndLogErrors()
                }
            }
        }
    }

    private val conferenceSource: (LocalConference?) -> Unit = {
        if (it != null) conference.value = it
    }

    private val messengerDao by inject<MessengerDao>()

    private val safeConference: LocalConference
        get() = requireNotNull(conference.value)

    private var draftDisposable: Disposable? = null

    init {
        conference.value = initialConference

        data.addSource(messengerDao.getMessagesLiveDataForConference(initialConference.id), dataSource)
        conference.addSource(messengerDao.getConferenceLiveData(initialConference.id), conferenceSource)

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
        draftDisposable = Single
            .fromCallable { storageHelper.getMessageDraft(safeConference.id.toString()).toOptional() }
            .filterSome()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { draft.value = it }
    }

    fun updateDraft(draft: String) {
        draftDisposable?.dispose()
        draftDisposable = Single
            .fromCallable {
                if (draft.isBlank()) {
                    storageHelper.deleteMessageDraft(safeConference.id.toString())
                } else {
                    storageHelper.putMessageDraft(safeConference.id.toString(), draft)
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    fun sendMessage(text: String) {
        val safeUser = requireNotNull(storageHelper.user)

        disposables += Single
            .fromCallable { messengerDao.insertMessageToSend(safeUser, text, safeConference.id) }
            .doOnSuccess { if (!MessengerWorker.isRunning) MessengerWorker.enqueueSynchronization() }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
