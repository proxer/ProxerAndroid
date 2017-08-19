package me.proxer.app.chat.conference

import android.app.Application
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
import me.proxer.app.chat.sync.ChatSynchronizationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.StorageHelper

/**
 * @author Ruben Gees
 */
class ConferenceViewModel(application: Application) : BaseViewModel<List<LocalConference>>(application) {

    override val isLoginRequired = true

    override val dataSingle: Single<List<LocalConference>>
        get() = Single
                .fromCallable { Validators.validateLogin() }
                .flatMap {
                    when (StorageHelper.areConferencesSynchronized) {
                        true -> Single.just(chatDao.getConferences())
                        else -> Single.never()
                    }
                }

    init {
        disposables += bus.register(ChatSynchronizationEvent::class.java)
                .map {
                    it.dataMap.keys.let { newData ->
                        data.value.let { existingData ->
                            when (existingData) {
                                null -> newData
                                else -> newData + existingData.filter { (oldId) ->
                                    newData.find { (newId) -> newId == oldId } == null
                                }
                            }
                        }
                    }.toList()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dataDisposable?.dispose()

                    isLoading.value = false
                    error.value = null
                    data.value = it
                }

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
                .subscribe()
    }
}
