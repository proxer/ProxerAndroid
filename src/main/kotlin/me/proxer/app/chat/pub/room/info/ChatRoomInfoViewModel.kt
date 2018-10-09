package me.proxer.app.chat.pub.room.info

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.chat.ChatRoomUser
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class ChatRoomInfoViewModel(private val chatRoomId: String) : BaseContentViewModel<List<ChatRoomUser>>() {

    override val endpoint: Endpoint<List<ChatRoomUser>>
        get() = api.chat().roomUsers(chatRoomId)

    override val dataSingle: Single<List<ChatRoomUser>>
        get() = super.dataSingle
            .map { it.sortedWith(compareByDescending(ChatRoomUser::isModerator).thenBy(ChatRoomUser::getName)) }
            .doOnSuccess { if (pollingDisposable == null) startPolling() }

    private var pollingDisposable: Disposable? = null

    override fun onCleared() {
        pollingDisposable?.dispose()
        pollingDisposable = null

        super.onCleared()
    }

    fun pausePolling() {
        pollingDisposable?.dispose()
    }

    fun resumePolling() {
        if (data.value != null) {
            startPolling()
        }
    }

    private fun startPolling() {
        pollingDisposable = dataSingle
            .repeatWhen { it.concatMap { Flowable.timer(10, TimeUnit.SECONDS) } }
            .retryWhen { it.concatMap { Flowable.timer(10, TimeUnit.SECONDS) } }
            .delaySubscription(10, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors {
                error.value = null
                data.value = it
            }
    }
}
