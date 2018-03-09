package me.proxer.app.chat

import android.arch.lifecycle.MediatorLiveData
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.base.PagedViewModel
import me.proxer.app.chat.sync.ChatErrorEvent
import me.proxer.app.chat.sync.ChatJob
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ChatViewModel(initialConference: LocalConference) : PagedViewModel<LocalMessage>() {

    override val isLoginRequired = true
    override val itemsOnPage = ChatJob.MESSAGES_ON_PAGE

    override var hasReachedEnd
        get() = safeConference.isFullyLoaded
        set(value) = Unit

    override val data = object : MediatorLiveData<List<LocalMessage>>() {

        private val source by lazy { chatDao.getMessagesLiveDataForConference(safeConference.id) }

        override fun onActive() {
            super.onActive()

            addSource(source, {
                it?.let {
                    if (StorageHelper.isLoggedIn) {
                        if (it.isEmpty() && !hasReachedEnd) {
                            ChatJob.scheduleMessageLoad(safeConference.id)
                        } else {
                            if (error.value == null) {
                                dataDisposable?.dispose()

                                page = it.size.div(itemsOnPage)

                                isLoading.value = false
                                error.value = null
                                this.value = it

                                Completable.fromAction { chatDao.markConferenceAsRead(safeConference.id) }
                                    .subscribeOn(Schedulers.io())
                                    .subscribeAndLogErrors()
                            }
                        }
                    }
                }
            })
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
                    0 -> chatDao.markConferenceAsRead(safeConference.id)
                    else -> ChatJob.scheduleMessageLoad(safeConference.id)
                }

                Single.never<List<LocalMessage>>()
            }

    val conference = object : MediatorLiveData<LocalConference>() {

        private val source by lazy { chatDao.getConferenceLiveData(safeConference.id) }

        override fun onActive() {
            super.onActive()

            addSource(source, {
                it?.let { this.value = it }
            })
        }

        override fun onInactive() {
            removeSource(source)

            super.onInactive()
        }
    }

    private val safeConference: LocalConference
        get() = conference.value ?: throw IllegalArgumentException("Conference cannot be null")

    init {
        conference.value = initialConference

        disposables += bus.register(ChatErrorEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event: ChatErrorEvent ->
                if (event.error is ChatMessageException) {
                    dataDisposable?.dispose()

                    isLoading.value = false
                    error.value = ErrorUtils.handle(event.error)
                }

                ChatJob.scheduleSynchronization()
            }
    }

    fun sendMessage(text: String) {
        disposables += Single
            .fromCallable { chatDao.insertMessageToSend(text, safeConference.id) }
            .doOnSuccess { if (!ChatJob.isRunning()) ChatJob.scheduleSynchronization() }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
