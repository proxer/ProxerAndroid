package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.adapter.PagingAdapter
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.interfaces.IdItem

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class PagingFragment<T> : LoadingFragment<Array<T>>() where T : IdItem, T : Parcelable {

    private companion object {
        private const val CURRENTLY_LOADING_PAGE_STATE = "fragment_paging_state_next_page"
        private const val END_REACHED_STATE = "fragment_paging_state_end_reached"
    }

    abstract protected val itemsOnPage: Int

    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val adapter: PagingAdapter<T>
    open protected val list: RecyclerView by bindView(R.id.list)

    protected var currentlyLoadingPage = -1
    protected var endReached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            currentlyLoadingPage = it.getInt(CURRENTLY_LOADING_PAGE_STATE)
            endReached = it.getBoolean(END_REACHED_STATE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging_default, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.layoutManager = layoutManager
        list.adapter = adapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!endReached) {
                    load()
                }
            }
        })
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

    override fun clear() {
        adapter.clear()
    }

    fun refresh() {
        if (canLoad) {
            currentlyLoadingPage = 0

            super.load()
        }
    }

    open protected fun onPagedLoadStarted(page: Int) {

    }

    open protected fun onPagedLoadFinished(result: Array<T>, page: Int) {
        currentlyLoadingPage = -1
        endReached = result.size < itemsOnPage

        if (page < 0) {
            throw IllegalStateException("Page -1 when trying to insert")
        } else if (page == 0) {
            adapter.insert(result)
        } else {
            adapter.append(result)
        }
    }

    open protected fun onPagedLoadFinishedWithError(result: ProxerException, page: Int) {
        // TODO: Do error handling

        currentlyLoadingPage = -1
    }

    protected abstract fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<T>>

    private fun calculateNextPage(): Int {
        if (adapter.itemCount == 0) {
            return 0
        } else {
            return itemsOnPage / adapter.itemCount
        }
    }
}