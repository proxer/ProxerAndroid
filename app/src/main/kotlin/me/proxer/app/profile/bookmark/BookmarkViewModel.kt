package me.proxer.app.profile.bookmark

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.ucp.Bookmark
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class BookmarkViewModel(application: Application) : PagedViewModel<Bookmark>(application) {

    override val itemsOnPage = 30
    override val isLoginRequired = true

    override val endpoint: PagingLimitEndpoint<List<Bookmark>>
        get() = MainApplication.api.ucp()
                .bookmarks()
                .category(category)

    val itemRemovalError = MutableLiveData<ErrorUtils.ErrorAction?>()

    private var category: Category? = null

    private val removalQueue = UniqueQueue<Bookmark>()
    private var removalDisposable: Disposable? = null

    override fun onCleared() {
        removalDisposable?.dispose()
        removalDisposable = null

        super.onCleared()
    }

    override fun refresh() = reload()

    fun setCategory(value: Category?, trigger: Boolean = true) {
        if (category != value) {
            category = value

            if (trigger) reload()
        }
    }

    fun addItemToRemove(item: Bookmark) {
        removalQueue.add(item)

        doItemRemoval()
    }

    private fun doItemRemoval() {
        removalDisposable?.dispose()

        removalQueue.poll()?.let { item ->
            removalDisposable = MainApplication.api.ucp()
                    .deleteBookmark(item.id)
                    .buildOptionalSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        data.value = data.value?.filterNot { it == item }

                        doItemRemoval()
                    }, {
                        removalQueue.clear()

                        itemRemovalError.value = ErrorUtils.handle(it)
                    })
        }
    }
}
