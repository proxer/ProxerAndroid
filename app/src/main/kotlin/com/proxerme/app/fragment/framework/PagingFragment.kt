package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.CallSuper
import com.proxerme.library.connection.ProxerException

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class PagingFragment<T> : LoadingFragment<Array<T>>() where T : Parcelable {

    private companion object {
        private const val CURRENTLY_LOADING_PAGE_STATE = "fragment_paging_state_next_page"
        private const val END_REACHED_STATE = "fragment_paging_state_end_reached"
    }

    abstract protected val itemsOnPage: Int

    protected var currentlyLoadingPage = -1
    protected var endReached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            currentlyLoadingPage = it.getInt(CURRENTLY_LOADING_PAGE_STATE)
            endReached = it.getBoolean(END_REACHED_STATE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(CURRENTLY_LOADING_PAGE_STATE, currentlyLoadingPage)
        outState.putBoolean(END_REACHED_STATE, endReached)
    }

    override fun onLoadStarted() {
        super.onLoadStarted()

        onPagedLoadStarted(currentlyLoadingPage)
    }

    override fun onLoadFinished(result: Array<T>) {
        super.onLoadFinished(result)

        onPagedLoadFinished(result, currentlyLoadingPage)
    }

    override fun onLoadFinishedWithError(result: ProxerException) {
        super.onLoadFinishedWithError(result)

        onPagedLoadFinishedWithError(result, currentlyLoadingPage)
    }

    override fun constructLoadingRequest(): LoadingRequest<Array<T>> {
        return constructPagedLoadingRequest(currentlyLoadingPage)
    }

    override fun load() {
        if (canLoad) {
            currentlyLoadingPage = calculateNextPage()

            super.load()
        }
    }

    override fun reset() {
        currentlyLoadingPage = -1
        endReached = false

        super.reset()
    }

    fun refresh() {
        if (canLoad) {
            currentlyLoadingPage = 0

            super.load()
        }
    }

    @CallSuper
    open protected fun onPagedLoadStarted(page: Int) {

    }

    @CallSuper
    open protected fun onPagedLoadFinished(result: Array<T>, page: Int) {
        currentlyLoadingPage = -1
        endReached = hasReachedEnd(result)
    }

    @CallSuper
    open protected fun onPagedLoadFinishedWithError(result: ProxerException, page: Int) {
        currentlyLoadingPage = -1
    }

    open fun hasReachedEnd(result: Array<T>) = result.size < itemsOnPage

    protected abstract fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<T>>

    protected abstract fun calculateNextPage(): Int
}