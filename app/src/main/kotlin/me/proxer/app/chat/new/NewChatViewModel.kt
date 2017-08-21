package me.proxer.app.chat.new

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.Participant
import me.proxer.app.chat.sync.ChatErrorEvent
import me.proxer.app.chat.sync.ChatJob
import me.proxer.app.chat.sync.ChatSynchronizationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
class NewChatViewModel(application: Application?) : AndroidViewModel(application) {

    val isLoading = MutableLiveData<Boolean>()
    val result = ResettingMutableLiveData<LocalConference>()
    val error = ResettingMutableLiveData<ErrorUtils.ErrorAction>()

    var isGroup: Boolean = false

    private var newConferenceId: Long? = null

    private var creationDisposable: Disposable? = null
    private var disposables = CompositeDisposable()

    init {
        disposables += bus.register(ChatSynchronizationEvent::class.java)
                .flatMap { newData ->
                    newConferenceId.let { newConferenceId ->
                        when (newConferenceId) {
                            null -> Observable.never()
                            else -> chatDao.findConference(newConferenceId).let { foundConference ->
                                when (foundConference) {
                                    null -> Observable.never()
                                    else -> Observable.just(foundConference)
                                }
                            }
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    newConferenceId = null

                    isLoading.value = false
                    error.value = null
                    result.value = it
                }

        disposables += bus.register(ChatErrorEvent::class.java)
                .flatMap {
                    newConferenceId.let { newConferenceId ->
                        when (newConferenceId) {
                            null -> Observable.never()
                            else -> Observable.just(ErrorUtils.handle(it.error))
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    newConferenceId = null

                    isLoading.value = false
                    result.value = null
                    error.value = it
                }
    }

    override fun onCleared() {
        creationDisposable?.dispose()
        disposables.dispose()

        creationDisposable = null

        super.onCleared()
    }

    fun createGroup(topic: String, firstMessage: String, participants: List<Participant>) = createConference(api
            .messenger()
            .createConferenceGroup(topic, firstMessage, participants.map { it.username }))

    fun createChat(firstMessage: String, participant: Participant) = createConference(api
            .messenger()
            .createConference(firstMessage, participant.username))

    private fun createConference(endpoint: Endpoint<String>) {
        creationDisposable?.dispose()
        creationDisposable = endpoint.buildSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    newConferenceId = null
                    isLoading.value = true
                    result.value = null
                    error.value = null
                }
                .subscribe({
                    newConferenceId = it.toLong()

                    ChatJob.scheduleSynchronization()
                }, {
                    isLoading.value = false
                    error.value = ErrorUtils.handle(it)
                })
    }
}
