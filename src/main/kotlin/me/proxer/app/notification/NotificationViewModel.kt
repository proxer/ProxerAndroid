package me.proxer.app.notification

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toInstantBP
import me.proxer.library.enums.NotificationFilter

/**
 * @author Ruben Gees
 */
class NotificationViewModel : PagedViewModel<ProxerNotification>() {

    override val itemsOnPage = 30

    override val dataSingle: Single<List<ProxerNotification>>
        get() = Single.fromCallable { validators.validateLogin() }
            .flatMap {
                when (page) {
                    0 ->
                        api.notifications.notifications()
                            .markAsRead(true)
                            .page(page)
                            .filter(NotificationFilter.UNREAD)
                            .limit(Int.MAX_VALUE)
                            .buildSingle()
                            .doOnSuccess { items -> firstPageItemAmount = items.size }
                            .flatMap { unreadResult ->
                                readSingle().map { readResult ->
                                    mergeNewDataWithExistingData(unreadResult, readResult, 0)
                                }
                            }
                    else -> readSingle()
                }
            }
            .doOnSuccess {
                if (page == 0) {
                    it.firstOrNull()?.date?.toInstantBP()?.let { date ->
                        storageHelper.lastNotificationsDate = date
                    }
                }
            }

    val deletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private val deletionQueue = UniqueQueue<ProxerNotification>()
    private var deletionDisposable: Disposable? = null
    private var deletionAllDisposable: Disposable? = null

    private var firstPageItemAmount = 0

    init {
        bus.register(AccountNotificationEvent::class.java).subscribe()
    }

    override fun onCleared() {
        deletionAllDisposable?.dispose()
        deletionDisposable?.dispose()

        deletionAllDisposable = null
        deletionDisposable = null

        super.onCleared()
    }

    override fun refresh() = reload()

    override fun areItemsTheSame(old: ProxerNotification, new: ProxerNotification): Boolean {
        return old == new
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

        api.notifications.deleteAllNotifications()
            .buildSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors(
                {
                    data.value = emptyList()
                },
                {
                    deletionError.value = ErrorUtils.handle(it)
                }
            )
    }

    private fun doItemDeletion() {
        deletionAllDisposable?.dispose()
        deletionDisposable?.dispose()

        deletionQueue.poll()?.let { item ->
            deletionDisposable = api.notifications.deleteNotification(item.id)
                .buildSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors(
                    {
                        data.value = data.value?.filterNot { newItem -> newItem == item }

                        doItemDeletion()
                    },
                    {
                        deletionQueue.clear()

                        deletionError.value = ErrorUtils.handle(it)
                    }
                )
        }
    }

    private fun readSingle() = api.notifications.notifications()
        .page(page - firstPageItemAmount / itemsOnPage)
        .filter(NotificationFilter.READ)
        .limit(itemsOnPage)
        .buildSingle()
}
