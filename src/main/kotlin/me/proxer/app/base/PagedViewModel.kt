package me.proxer.app.base

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class PagedViewModel<T> : BaseViewModel<List<T>>() {

    val refreshError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    protected open var hasReachedEnd = false
    protected var isRefreshing = false

    protected var page = 0
        get() = if (isRefreshing) 0 else field

    protected abstract val itemsOnPage: Int

    override fun load() {
        val currentPage = page

        dataDisposable?.dispose()
        dataDisposable = dataSingle
            .doAfterSuccess { newData -> hasReachedEnd = newData.size < itemsOnPage }
            .map { newData -> mergeNewDataWithExistingData(data.value ?: emptyList(), newData, currentPage) }
            .subscribeOn(Schedulers.io())
            .doAfterTerminate { isRefreshing = false }
            .doAfterSuccess { if (!isRefreshing) page++ }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                refreshError.value = null
                error.value = null
                isLoading.value = true
            }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors(
                {
                    refreshError.value = null
                    error.value = null
                    data.value = it
                },
                {
                    if (currentPage == 0 && data.value?.size ?: 0 > 0) {
                        refreshError.value = ErrorUtils.handle(it)
                    } else {
                        error.value = ErrorUtils.handle(it)
                    }
                }
            )
    }

    override fun loadIfPossible() {
        if (!hasReachedEnd) {
            super.loadIfPossible()
        }
    }

    override fun refresh() {
        isRefreshing = true

        load()
    }

    override fun reload() {
        refreshError.value = null
        hasReachedEnd = false
        isRefreshing = false
        page = 0

        super.reload()
    }

    protected open fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }

    protected open fun mergeNewDataWithExistingData(
        existingData: List<T>,
        newData: List<T>,
        currentPage: Int
    ) = when (currentPage) {
        0 -> newData + existingData.filter { oldItem ->
            newData.find { newItem -> areItemsTheSame(oldItem, newItem) } == null
        }
        else -> existingData.filter { oldItem ->
            newData.find { newItem -> areItemsTheSame(oldItem, newItem) } == null
        } + newData
    }
}
