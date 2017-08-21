package me.proxer.app.chat

import android.app.Application
import android.arch.lifecycle.MediatorLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.MainApplication.Companion.chatDatabase
import me.proxer.app.base.PagedViewModel
import me.proxer.app.chat.sync.ChatErrorEvent
import me.proxer.app.chat.sync.ChatJob
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators

/**
 * @author Ruben Gees
 */
class ChatViewModel(application: Application) : PagedViewModel<LocalMessage>(application) {

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
                    if (it.isEmpty() && !safeConference.isFullyLoaded) {
                        ChatJob.scheduleMessageLoad(safeConference.id)
                    } else {
                        if (error.value == null) {
                            dataDisposable?.dispose()

                            page = it.size.div(itemsOnPage)

                            isLoading.value = false
                            error.value = null
                            this.value = it
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
        disposables += bus.register(ChatErrorEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event: ChatErrorEvent ->
                    if (event.error is ChatJob.ChatMessageException) {
                        dataDisposable?.dispose()

                        isLoading.value = false
                        error.value = ErrorUtils.handle(event.error)
                    }
                }

        Completable
                .fromAction { if (!ChatJob.isRunning()) ChatJob.scheduleSynchronization() }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    fun sendMessage(text: String) {
        disposables += Single
                .fromCallable { chatDatabase.insertMessageToSend(text, safeConference.id) }
                .doOnSuccess { if (!ChatJob.isRunning()) ChatJob.scheduleSynchronization() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }
}
