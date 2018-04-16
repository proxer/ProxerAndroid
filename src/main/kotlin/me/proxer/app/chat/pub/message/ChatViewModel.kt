package me.proxer.app.chat.pub.message

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.chat.ChatMessage
import me.proxer.library.enums.ChatMessageAction
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ChatViewModel(private val chatRoomId: String) : PagedViewModel<ChatMessage>() {

    override val isLoginRequired = true
    override val itemsOnPage = 50

    override val dataSingle: Single<List<ChatMessage>>
        get() = Single.fromCallable { Validators.validateLogin() }
            .flatMap {
                api.chat().messages(chatRoomId)
                    .messageId(data.value?.lastOrNull()?.id ?: "0")
                    .buildSingle()
            }

    val sendMessageError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private var pollingDisposable: Disposable? = null

    private val messageQueue: Queue<String> = LinkedList<String>()
    private var messageDisposable: Disposable? = null

    private var currentFirstId = "0"

    override fun onCleared() {
        pollingDisposable?.dispose()
        messageDisposable?.dispose()

        pollingDisposable = null
        messageDisposable = null

        super.onCleared()
    }

    override fun load() {
        dataDisposable?.dispose()
        dataDisposable = dataSingle
            .doAfterSuccess { newData -> hasReachedEnd = newData.size < itemsOnPage }
            .map { newData -> mergeNewDataWithExistingData(newData, data.value?.lastOrNull()?.id ?: "0") }
            .subscribeOn(Schedulers.io())
            .doAfterTerminate { isRefreshing = false }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                refreshError.value = null
                error.value = null
                isLoading.value = true
            }
            .doOnSuccess { if (pollingDisposable == null) startPolling() }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors({
                currentFirstId = it.firstOrNull()?.id ?: "0"

                refreshError.value = null
                error.value = null
                data.value = it
            }, {
                if (data.value?.size ?: 0 > 0) {
                    refreshError.value = ErrorUtils.handle(it)
                } else {
                    error.value = ErrorUtils.handle(it)
                }
            })
    }

    fun sendMessage(text: String) {
        messageQueue.add(text)

        if (messageDisposable?.isDisposed != false) {
            doSendMessages()
        }
    }

    private fun mergeNewDataWithExistingData(newData: List<ChatMessage>, currentId: String): List<ChatMessage> {
        val messageIdsToDelete = newData.filter { it.action == ChatMessageAction.REMOVE_MESSAGE }
            .flatMap { listOf(it.id, it.message) }

        val result = data.value.let { existingData ->
            when (existingData) {
                null -> newData
                else -> when (currentId) {
                    "0" -> newData + existingData.filter { oldItem ->
                        newData.find { newItem -> oldItem.id == newItem.id } == null
                    }
                    else -> existingData.filter { oldItem ->
                        newData.find { newItem -> oldItem.id == newItem.id } == null
                    } + newData
                }
            }
        }

        return when (messageIdsToDelete.isNotEmpty()) {
            true -> result.filterNot { it.id in messageIdsToDelete }
            false -> result
        }
    }

    private fun startPolling() {
        pollingDisposable = Single.fromCallable { Validators.validateLogin() }
            .flatMap { api.chat().messages(chatRoomId).messageId("0").buildSingle() }
            .repeatWhen { it.concatMap { Flowable.timer(3, TimeUnit.SECONDS) } }
            .retryWhen { it.concatMap { Flowable.timer(3, TimeUnit.SECONDS) } }
            .map { newData -> mergeNewDataWithExistingData(newData, "0") }
            .delaySubscription(3, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors {
                if (it.isNotEmpty() && it.first().id != currentFirstId) {
                    currentFirstId = it.first().id

                    error.value = null
                    data.value = it
                }
            }
    }

    private fun doSendMessages() {
        messageDisposable?.dispose()

        messageQueue.poll()?.let { item ->
            messageDisposable = Single.fromCallable { Validators.validateLogin() }
                .flatMap { api.chat().sendMessage(chatRoomId, item).buildOptionalSingle() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors({
                    pollingDisposable?.dispose()

                    startPolling()
                    doSendMessages()
                }, {
                    messageQueue.clear()

                    sendMessageError.value = ErrorUtils.handle(it)
                })
        }
    }
}
