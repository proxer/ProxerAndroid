package me.proxer.app.bookmark

import com.gojuno.koptional.Optional
import io.reactivex.Single
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
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class BookmarkViewModel(category: Optional<Category>) : PagedContentViewModel<Bookmark>() {

    override val itemsOnPage = 30
    override val isLoginRequired = true

    override val endpoint: PagingLimitEndpoint<List<Bookmark>>
        get() = api.ucp().bookmarks()
            .category(category)
            .filterAvailable(filterAvailable)

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    val undoData = ResettingMutableLiveData<Unit?>()
    val undoError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var category by Delegates.observable(category.toNullable()) { _, old, new ->
        if (old != new) reload()
    }

    var filterAvailable by Delegates.observable<Boolean?>(null) { _, old, new ->
        if (old != new) reload()
    }

    private val deletionQueue = UniqueQueue<Bookmark>()
    private var deletionDisposable: Disposable? = null

    private var undoItem: Bookmark? = null
    private var undoDisposable: Disposable? = null

    override fun onCleared() {
        deletionDisposable?.dispose()
        deletionDisposable = null

        undoDisposable?.dispose()
        undoDisposable = null
        undoItem = null

        super.onCleared()
    }

    override fun refresh() = reload()

    fun addItemToDelete(item: Bookmark) {
        deletionQueue.add(item)

        if (deletionDisposable?.isDisposed != false) {
            doItemDeletion()
        }
    }

    fun undo() {
        val safeUndoItem = undoItem

        if (safeUndoItem != null) {
            deletionDisposable?.dispose()
            undoDisposable?.dispose()

            undoDisposable = Single.fromCallable { validators.validateLogin() }
                .flatMap { bookmarkSingle(safeUndoItem) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { undoError.value = null }
                .doOnEvent { _, _ -> undoData.value = null }
                .subscribeAndLogErrors({
                    data.value = listOf(safeUndoItem) + (data.value ?: emptyList())

                    // Explicitly hide error (if no other, real error was present) to remove no data message
                    if (error.value == null) {
                        error.value = null
                    }
                }, {
                    undoError.value = ErrorUtils.handle(it)
                })
        }
    }

    private fun doItemDeletion() {
        deletionQueue.poll()?.let { item ->
            deletionDisposable?.dispose()
            undoDisposable?.dispose()

            undoItem = null

            deletionDisposable = api.ucp().deleteBookmark(item.id)
                .buildOptionalSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { undoData.value = null }
                .subscribeAndLogErrors({
                    undoItem = item

                    undoData.value = Unit
                    data.value = data.value?.filterNot { newItem -> newItem == item }

                    doItemDeletion()
                }, {
                    deletionQueue.clear()

                    itemDeletionError.value = ErrorUtils.handle(it)
                })
        }
    }

    private fun bookmarkSingle(bookmark: Bookmark) = api.ucp()
        .setBookmark(bookmark.entryId, bookmark.episode, bookmark.language, bookmark.category)
        .buildOptionalSingle()
}
