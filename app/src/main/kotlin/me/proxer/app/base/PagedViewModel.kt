package me.proxer.app.base

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.library.entitiy.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class PagedViewModel<T>(application: Application) : BaseViewModel<List<T>>(application) {

    abstract protected val itemsOnPage: Int

    val refreshError = MutableLiveData<ErrorAction?>()

    var hasReachedEnd = false
        protected set

    var page = 0
        protected set

    override fun load() {
        disposable?.dispose()
        disposable = loadSingle
                .map { newData ->
                    data.value.let { existingData ->
                        when (existingData) {
                            null -> newData
                            else -> when (page) {
                                0 -> newData + existingData.filter { item ->
                                    newData.find { oldItem -> areItemsTheSame(oldItem, item) } == null
                                }
                                else -> existingData.filter { item ->
                                    newData.find { oldItem -> areItemsTheSame(oldItem, item) } == null
                                } + newData
                            }
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isLoading.value = true }
                .doAfterSuccess { newData ->
                    hasReachedEnd = newData.size < itemsOnPage
                }
                .doAfterTerminate {
                    isLoading.value = false
                    page = data.value?.size?.div(itemsOnPage) ?: 0
                }
                .subscribe({
                    data.value = it
                    error.value = null
                    refreshError.value = null
                }, {
                    if (page == 0 && data.value?.size ?: 0 > 0) {
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

    open protected fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }
}
