package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.interfaces.ProxerErrorResult
import com.proxerme.library.interfaces.ProxerResult

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class PagingFragment : LoadingFragment() {

    private companion object {
        const val STATE_NEXT_PAGE = "fragment_paging_state_next_page"
        const val STATE_END_REACHED = "fragment_paging_state_end_reached"
    }

    protected var nextPageToLoad = 0
    protected var endReached = false

    abstract protected val layoutManager: RecyclerView.LayoutManager

    open protected val list: RecyclerView by bindView(R.id.list)
    override val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    override val errorText: TextView by bindView(R.id.errorText)
    override val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            nextPageToLoad = it.getInt(STATE_NEXT_PAGE)
            endReached = it.getBoolean(STATE_END_REACHED)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.addOnScrollListener(object :
                EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!isLoading && !endReached && canLoad && currentError == null) {
                    notifyLoadStarted(false)
                    loadPage(nextPageToLoad)
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(STATE_NEXT_PAGE, nextPageToLoad)
        outState.putBoolean(STATE_END_REACHED, endReached)
    }

    override fun load(showProgress: Boolean) {
        super.load(showProgress)

        loadPage(0)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?,
                             savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging_default, container, false)
    }

    override fun showError(message: String, buttonMessage: String?,
                           onButtonClickListener: View.OnClickListener?) {
        super.showError(message, buttonMessage, onButtonClickListener)

        clear()
    }

    open protected fun notifyPagedLoadFinishedSuccessful(page: Int, result: ProxerResult<*>) {
        endReached = (result.item as Array<*>).isEmpty()

        if (page >= nextPageToLoad) {
            nextPageToLoad = page + 1
        }

        notifyLoadFinishedSuccessful(result)
    }

    open protected fun notifyPagedLoadFinishedWithError(page: Int, result: ProxerErrorResult) {
        endReached = false
        nextPageToLoad = 0

        notifyLoadFinishedWithError(result)
    }

    override fun reset() {
        nextPageToLoad = 0
        endReached = false
        clear()

        super.reset()
    }

    abstract protected fun clear()

    abstract protected fun loadPage(number: Int)
}