package me.proxer.app.notification

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class NotificationViewModel : BaseContentViewModel<List<ProxerNotification>>() {

    override val isLoginRequired = true

    override val dataSingle: Single<List<ProxerNotification>>
        get() = super.dataSingle.doOnSuccess {
            it.firstOrNull()?.date?.let {
                StorageHelper.lastNotificationsDate = it
            }
        }

    override val endpoint: Endpoint<List<ProxerNotification>>
        get() = api.notifications().notifications()
                .markAsRead(true)
                .limit(Int.MAX_VALUE)

    val deletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private val deletionQueue = UniqueQueue<ProxerNotification>()
    private var deletionDisposable: Disposable? = null
    private var deletionAllDisposable: Disposable? = null

    init {
        bus.register(AccountNotificationEvent::class.java).subscribe()
    }

    override fun load() {
        dataDisposable?.dispose()
        dataDisposable = dataSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    isLoading.value = true
                    error.value = null
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribeAndLogErrors({
                    error.value = null
                    data.value = it
                }, {
                    data.value = null
                    error.value = ErrorUtils.handle(it)
                })
    }

    override fun onCleared() {
        deletionAllDisposable?.dispose()
        deletionDisposable?.dispose()

        deletionAllDisposable = null
        deletionDisposable = null

        super.onCleared()
    }

    fun addItemToDelete(item: ProxerNotification) {
        deletionQueue.add(item)

        if (deletionDisposable?.isDisposed != false) {
            doItemDeletion()
        }
    }

    fun deleteAll() {
        deletionAllDisposable?.dispose()
        deletionDisposable?.dispose()

        deletionQueue.clear()

        api.notifications().deleteAllNotifications()
                .buildOptionalSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors({
                    data.value = emptyList()
                }, {
                    deletionError.value = ErrorUtils.handle(it)
                })
    }

    private fun doItemDeletion() {
        deletionAllDisposable?.dispose()
        deletionDisposable?.dispose()

        deletionQueue.poll()?.let { item ->
            deletionDisposable = api.notifications().deleteNotification(item.id)
                    .buildOptionalSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeAndLogErrors({
                        data.value = data.value?.filterNot { it == item }

                        doItemDeletion()
                    }, {
                        deletionQueue.clear()

                        deletionError.value = ErrorUtils.handle(it)
                    })
        }
    }
}
