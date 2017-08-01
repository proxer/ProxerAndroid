package me.proxer.app.base

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import me.proxer.app.util.ErrorUtils

/**
 * @author Ruben Gees
 */
abstract class PagedViewModel<T>(application: Application) : BaseViewModel<List<T>>(application) {

    val refreshError = MutableLiveData<ErrorUtils.ErrorAction?>()

    protected var hasReachedEnd = false
    abstract protected val itemsOnPage: Int

    override fun load() {
        load(data.value?.size?.div(itemsOnPage) ?: 0)
    }

    override fun loadIfPossible() {
        if (!hasReachedEnd) {
            super.loadIfPossible()
        }
    }

    override fun refresh() {
        load(0)
    }

    override fun reload() {
        refreshError.value = null

        super.reload()
    }

    open protected fun load(page: Int) = Unit
}