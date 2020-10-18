package me.proxer.app.chat.prv.create

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rubengees.rxbus.RxBus
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerErrorEvent
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.ProxerApi
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
class CreateConferenceViewModel : ViewModel() {

    val isLoading = MutableLiveData<Boolean>()
    val result = ResettingMutableLiveData<LocalConference>()
    val error = ResettingMutableLiveData<ErrorUtils.ErrorAction>()

    private val bus by safeInject<RxBus>()
    private val api by safeInject<ProxerApi>()
    private val messengerDao by safeInject<MessengerDao>()

    private var newConferenceId: Long? = null

    private var creationDisposable: Disposable? = null
    private var disposables = CompositeDisposable()

    init {
        disposables += bus.register(MessengerWorker.SynchronizationEvent::class.java)
            .flatMap {
                newConferenceId.let { newConferenceId ->
                    when (newConferenceId) {
                        null -> Observable.never()
                        else -> messengerDao.findConference(newConferenceId).let { foundConference ->
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

        disposables += bus.register(MessengerErrorEvent::class.java)
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

    fun createGroup(topic: String, firstMessage: String, participants: List<Participant>) = createConference(
        api
            .messenger
            .createConferenceGroup(topic, firstMessage, participants.map { it.username })
    )

    fun createChat(firstMessage: String, participant: Participant) = createConference(
        api
            .messenger
            .createConference(firstMessage, participant.username)
    )

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
            .subscribeAndLogErrors(
                {
                    newConferenceId = it.toLong()

                    MessengerWorker.enqueueSynchronization()
                },
                {
                    isLoading.value = false
                    error.value = ErrorUtils.handle(it)
                }
            )
    }
}
