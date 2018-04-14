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
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.chat.ChatMessage
import me.proxer.library.enums.ChatMessageAction
import java.util.concurrent.TimeUnit

@GeneratedProvider
class ChatViewModel(private val chatRoomId: String) : PagedViewModel<ChatMessage>() {

    override val isLoginRequired = true
    override val itemsOnPage = 50

    override val dataSingle: Single<List<ChatMessage>>
        get() = api.chat().messages(chatRoomId).messageId(data.value?.lastOrNull()?.id ?: "0")
            .buildSingle()

    private var pollingDisposable: Disposable? = null

    override fun onCleared() {
        pollingDisposable?.dispose()
        pollingDisposable = null

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
        pollingDisposable = api.chat().messages(chatRoomId).messageId("0").buildSingle()
            .repeatWhen { it.concatMap { Flowable.timer(3, TimeUnit.SECONDS) } }
            .retryWhen { it.concatMap { Flowable.timer(3, TimeUnit.SECONDS) } }
            .map { newData -> mergeNewDataWithExistingData(newData, "0") }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                error.value = null
                isLoading.value = true
            }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors({
                error.value = null
                data.value = it
            }, {
                error.value = ErrorUtils.handle(it)
            })
    }
}
