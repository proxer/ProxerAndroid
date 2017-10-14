package me.proxer.app.bookmark

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class BookmarkViewModel(category: Category?) : PagedContentViewModel<Bookmark>() {

    override val itemsOnPage = 30
    override val isLoginRequired = true

    override val endpoint: PagingLimitEndpoint<List<Bookmark>>
        get() = api.ucp().bookmarks().category(category)

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var category by Delegates.observable(category, { _, old, new ->
        if (old != new) reload()
    })

    private val deletionQueue = UniqueQueue<Bookmark>()
    private var deletionDisposable: Disposable? = null

    override fun onCleared() {
        deletionDisposable?.dispose()
        deletionDisposable = null

        super.onCleared()
    }

    override fun mergeNewDataWithExistingData(newData: List<Bookmark>, currentPage: Int): List<Bookmark> {
        return data.value.let { existingData ->
            when (existingData) {
                null -> newData
                else -> when (currentPage) {
                    0 -> newData
                    else -> existingData.filter { oldItem ->
                        newData.find { newItem -> areItemsTheSame(oldItem, newItem) } == null
                    } + newData
                }
            }
        }
    }

    fun addItemToDelete(item: Bookmark) {
        deletionQueue.add(item)

        if (deletionDisposable?.isDisposed != false) {
            doItemDeletion()
        }
    }

    private fun doItemDeletion() {
        deletionDisposable?.dispose()

        deletionQueue.poll()?.let { item ->
            deletionDisposable = api.ucp().deleteBookmark(item.id)
                    .buildOptionalSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeAndLogErrors({
                        data.value = data.value?.filterNot { it == item }

                        doItemDeletion()
                    }, {
                        deletionQueue.clear()

                        itemDeletionError.value = ErrorUtils.handle(it)
                    })
        }
    }
}
