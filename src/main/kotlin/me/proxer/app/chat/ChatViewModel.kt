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
import me.proxer.app.MainApplication.Companion.chatDatabase
import me.proxer.app.base.PagedViewModel
import me.proxer.app.chat.sync.ChatErrorEvent
import me.proxer.app.chat.sync.ChatJob
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.StorageHelper

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ChatViewModel(private val conference: LocalConference) : PagedViewModel<LocalMessage>() {

    override val isLoginRequired = true
    override val itemsOnPage = ChatJob.MESSAGES_ON_PAGE

    override var hasReachedEnd
        get() = conference.isFullyLoaded
        set(value) = Unit

    override val data = object : MediatorLiveData<List<LocalMessage>>() {

        private val source by lazy { chatDao.getMessagesLiveDataForConference(conference.id) }

        override fun onActive() {
            super.onActive()

            addSource(source, {
                it?.let {
                    if (StorageHelper.user != null) {
                        if (it.isEmpty() && !conference.isFullyLoaded) {
                            ChatJob.scheduleMessageLoad(conference.id)
                        } else {
                            if (error.value == null) {
                                dataDisposable?.dispose()

                                page = it.size.div(itemsOnPage)

                                isLoading.value = false
                                error.value = null
                                this.value = it

                                Completable.fromAction { chatDao.markConferenceAsRead(conference.id) }
                                        .subscribeOn(Schedulers.io())
                                        .subscribe()
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
                        0 -> chatDao.markConferenceAsRead(conference.id)
                        else -> ChatJob.scheduleMessageLoad(conference.id)
                    }

                    Single.never<List<LocalMessage>>()
                }

    init {
        disposables += bus.register(ChatErrorEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event: ChatErrorEvent ->
                    if (event.error is ChatMessageException) {
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
                .fromCallable { chatDatabase.insertMessageToSend(text, conference.id) }
                .doOnSuccess { if (!ChatJob.isRunning()) ChatJob.scheduleSynchronization() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }
}
