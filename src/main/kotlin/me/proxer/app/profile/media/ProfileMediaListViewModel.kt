package me.proxer.app.profile.media

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalEntry
import me.proxer.app.util.extension.toLocalEntryUcp
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ProfileMediaListViewModel(
    private val userId: String?,
    private val username: String?,
    category: Category,
    filter: UserMediaListFilterType?
) : PagedViewModel<LocalUserMediaListEntry>() {

    override val itemsOnPage = 30

    override val dataSingle: Single<List<LocalUserMediaListEntry>>
        get() = Single.fromCallable { validate() }
            .flatMap {
                val user = storageHelper.user

                when (user?.id == userId || user?.name?.equals(username, ignoreCase = true) == true) {
                    true -> api.ucp.mediaList()
                        .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
                        .category(category)
                        .filter(filter)
                        .page(page)
                        .limit(itemsOnPage)
                        .buildSingle()
                        .map { entries -> entries.map { it.toLocalEntryUcp() } }
                    false -> api.user.mediaList(userId, username)
                        .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
                        .category(category)
                        .filter(filter)
                        .page(page)
                        .limit(itemsOnPage)
                        .buildSingle()
                        .map { entries -> entries.map { it.toLocalEntry() } }
                }
            }

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }

    var filter by Delegates.observable(filter) { _, old, new ->
        if (old != new) reload()
    }

    init {
        disposables += storageHelper.isLoggedInObservable.subscribe { reload() }
    }

    private val deletionQueue = UniqueQueue<LocalUserMediaListEntry>()
    private var deletionDisposable: Disposable? = null

    override fun onCleared() {
        deletionDisposable?.dispose()
        deletionDisposable = null

        super.onCleared()
    }

    fun addItemToDelete(item: LocalUserMediaListEntry) {
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
