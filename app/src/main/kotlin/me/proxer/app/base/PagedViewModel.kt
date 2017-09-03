package me.proxer.app.base

import android.app.Application
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.library.entity.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class PagedViewModel<T>(application: Application) : BaseViewModel<List<T>>(application) {

    val refreshError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    protected open var hasReachedEnd = false
    protected var page = 0

    abstract protected val itemsOnPage: Int

    override fun load() {
        val currentPage = page

        dataDisposable?.dispose()
        dataDisposable = dataSingle
                .doAfterSuccess { newData -> hasReachedEnd = newData.size < itemsOnPage }
                .map { newData ->
                    data.value.let { existingData ->
                        when (existingData) {
                            null -> newData
                            else -> when (currentPage) {
                                0 -> newData + existingData.filter { oldItem ->
                                    newData.find { newItem -> areItemsTheSame(oldItem, newItem) } == null
                                }
                                else -> existingData.filter { oldItem ->
                                    newData.find { newItem -> areItemsTheSame(oldItem, newItem) } == null
                                } + newData
                            }
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    refreshError.value = null
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { page = data.value?.size?.div(itemsOnPage) ?: 0 }
                .doAfterTerminate { isLoading.value = false }
                .subscribe({
                    refreshError.value = null
                    error.value = null
                    data.value = it
                }, {
                    if (currentPage == 0 && data.value?.size ?: 0 > 0) {
                        refreshError.value = ErrorUtils.handle(it)
                    } else {
                        error.value = ErrorUtils.handle(it)
                    }
                })
    }

    override fun loadIfPossible() {
        if (!hasReachedEnd) {
            super.loadIfPossible()
        }
    }

    override fun refresh() {
        page = 0

        load()
    }

    override fun reload() {
        refreshError.value = null
        hasReachedEnd = false
        page = 0

        super.reload()
    }

    open protected fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }
}
