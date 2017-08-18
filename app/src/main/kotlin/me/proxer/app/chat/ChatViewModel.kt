package me.proxer.app.chat

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Observable
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
import me.proxer.app.chat.sync.ChatMessageEvent
import me.proxer.app.chat.sync.ChatSynchronizationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import org.jetbrains.anko.collections.forEachReversedByIndex

/**
 * @author Ruben Gees
 */
class ChatViewModel(application: Application) : PagedViewModel<LocalMessage>(application) {

    override val isLoginRequired = true
    override val itemsOnPage = ChatJob.MESSAGES_ON_PAGE

    override var hasReachedEnd
        get() = safeConference.isFullyLoaded
        set(value) = Unit

    override val dataSingle: Single<List<LocalMessage>>
        get() = Single.fromCallable { Validators.validateLogin() }
                .flatMap {
                    if (page == 0) {
                        chatDao.markConferenceAsRead(safeConference.id)
                        chatDao.getMessagesForConference(safeConference.id).let { messages ->
                            if (messages.isEmpty() && !safeConference.isFullyLoaded) {
                                ChatJob.scheduleMessageLoad(safeConference.id)

                                Single.never()
                            } else {
                                Single.just(messages)
                            }
                        }
                    } else {
                        ChatJob.scheduleMessageLoad(safeConference.id)

                        Single.never()
                    }
                }

    val conference = MutableLiveData<LocalConference>()

    private val safeConference: LocalConference
        get() = conference.value ?: throw NullPointerException("Conference cannot be null")

    init {
        disposables += bus.register(ChatFragmentPingEvent::class.java).subscribe()

        disposables += bus.register(ChatSynchronizationEvent::class.java)
                .flatMap {
                    val relevantConference = it.dataMap.entries.find { it.key.id == conference.value?.id }

                    when (relevantConference) {
                        null -> Observable.never()
                        else -> Observable.just(relevantConference)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { conference.value = it.key }
                .observeOn(Schedulers.io())
                .doOnNext { chatDao.markConferenceAsRead(it.key.id) }
                .map { (_, newData) ->
                    data.value.let { existingData ->
                        if (existingData != null) {
                            val result = mutableListOf<LocalMessage>()
                            val newDataCopy = newData.toMutableList()

                            existingData.forEachReversedByIndex { existingMessage ->
                                if (existingMessage.id > 0) {
                                    result += existingMessage
                                } else {
                                    val newIndex = newDataCopy.indexOfLast { newMessage ->
                                        existingMessage.action == newMessage.action
                                                && existingMessage.message == newMessage.message
                                    }

                                    if (newIndex >= 0) {
                                        result += newDataCopy[newIndex]

                                        newDataCopy.removeAt(newIndex)
                                    } else {
                                        result += existingMessage
                                    }
                                }
                            }

                            result.reverse()
                            result.addAll(0, newDataCopy)

                            result
                        } else {
                            newData
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dataDisposable?.dispose()

                    isLoading.value = false
                    refreshError.value = null
                    error.value = null
                    data.value = it
                }

        disposables += bus.register(ChatMessageEvent::class.java)
                .flatMap {
                    val (conference) = it.data

                    when (conference.id) {
                        safeConference.id -> Observable.just(it.data)
                        else -> Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { conference.value = it.first }
                .observeOn(Schedulers.io())
                .map { (_, newData) ->
                    data.value.let { existingData ->
                        when (existingData) {
                            null -> newData
                            else -> existingData + newData
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dataDisposable?.dispose()

                    isLoading.value = false
                    refreshError.value = null
                    error.value = null
                    data.value = it
                }

        disposables += bus.register(ChatErrorEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event: ChatErrorEvent ->
                    if (event.error is ChatJob.ChatMessageException) {
                        dataDisposable?.dispose()

                        isLoading.value = false
                        error.value = ErrorUtils.handle(event.error)
                    } else if (event.error is ChatJob.ChatSendMessageException) {
                        data.value?.let { existingData ->
                            existingData.find { event.error.id == it.id }?.let { invalidMessage ->
                                data.value = existingData - invalidMessage
                            }
                        }
                    }
                }
    }

    fun sendMessage(text: String) {
        disposables += Single
                .fromCallable { chatDatabase.insertMessageToSend(text, safeConference.id) }
                .map {
                    data.value.let { existingData ->
                        when (existingData) {
                            null -> listOf(it)
                            else -> listOf(it) + existingData
                        }
                    }
                }
                .doOnSuccess { if (!ChatJob.isRunning()) ChatJob.scheduleSynchronization() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { it: List<LocalMessage> ->
                    data.value = it
                }
    }

    override fun areItemsTheSame(old: LocalMessage, new: LocalMessage) = old.id == new.id
}
