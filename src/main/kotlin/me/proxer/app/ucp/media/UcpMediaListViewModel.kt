package me.proxer.app.ucp.media

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.user.UserMediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class UcpMediaListViewModel(
    category: Category,
    filter: UserMediaListFilterType?
) : PagedContentViewModel<UserMediaListEntry>() {

    override val isLoginRequired = true
    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<UserMediaListEntry>>
        get() = api.ucp.mediaList()
            .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
            .category(category)
            .filter(filter)

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }

    var filter by Delegates.observable(filter) { _, old, new ->
        if (old != new) reload()
    }

    private val deletionQueue = UniqueQueue<UserMediaListEntry>()
    private var deletionDisposable: Disposable? = null

    override fun onCleared() {
        deletionDisposable?.dispose()
        deletionDisposable = null

        super.onCleared()
    }

    fun addItemToDelete(item: UserMediaListEntry) {
        deletionQueue.add(item)

        if (deletionDisposable?.isDisposed != false) {
            doItemDeletion()
        }
    }

    private fun doItemDeletion() {
        deletionDisposable?.dispose()

        deletionQueue.poll()?.let { item ->
            deletionDisposable = api.ucp.deleteComment(item.commentId)
                .buildOptionalSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors({
                    data.value = data.value?.filterNot { newItem -> newItem == item }

                    doItemDeletion()
                }, {
                    deletionQueue.clear()

                    itemDeletionError.value = ErrorUtils.handle(it)
                })
        }
    }
}
