package com.proxerme.app.fragment.framework

import adapter.FooterAdapter
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.adapter.framework.PagingAdapter.PagingAdapterCallback
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.Utils
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.interfaces.IdItem

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class EasyPagingFragment<T, C : PagingAdapterCallback<T>> :
        PagingFragment<T>()  where T : IdItem, T : Parcelable {

    private companion object {
        private const val EXCEPTION_STATE = "fragment_easy_paging_state_exception"
    }

    open protected val isSwipeToRefreshEnabled = true

    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val adapter: PagingAdapter<T, C>
    protected lateinit var footerAdapter: FooterAdapter

    open protected val list: RecyclerView by bindView(R.id.list)
    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)

    protected var exception: ProxerException? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            exception = it.getSerializable(EXCEPTION_STATE) as ProxerException?
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging_default, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        footerAdapter = FooterAdapter(adapter)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = footerAdapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (exception == null && !endReached) {
                    load()
                }
            }
        })

        initProgress()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(EXCEPTION_STATE, exception)
    }

    override fun onResume() {
        super.onResume()

        if (isLoading) {
            showLoading()
        } else {
            if (exception != null) {
                showError(exception!!)
            }
        }
    }

    override fun onDestroy() {
        adapter.callback = null
        progress.setOnRefreshListener(null)
        list.clearOnScrollListeners()

        super.onDestroy()
    }

    override fun onPagedLoadStarted(page: Int) {
        super.onPagedLoadStarted(page)

        exception = null

        if (adapter.isEmpty()) {
            showLoading()
        }

        hideError()
    }

    override fun onPagedLoadFinished(result: Array<T>, page: Int) {
        super.onPagedLoadFinished(result, page)

        if (page < 0) {
            throw IllegalStateException("Page -1 when trying to insert")
        } else if (page == 0) {
            adapter.insert(result)
        } else {
            adapter.append(result)
        }

        hideProgress()
    }

    override fun onPagedLoadFinishedWithError(result: ProxerException, page: Int) {
        super.onPagedLoadFinishedWithError(result, page)

        exception = result

        hideProgress()
        showError(result)
    }

    override fun calculateNextPage(): Int {
        if (adapter.itemCount == 0) {
            return 0
        } else {
            return adapter.itemCount / itemsOnPage
        }
    }

    override fun clear() {
        adapter.clear()
    }

    open protected fun initProgress() {
        progress.setColorSchemeResources(R.color.primary)

        progress.setOnRefreshListener {
            if (canLoad) {
                refresh()
            } else {
                hideProgress()
            }
        }
    }

    open protected fun showLoading() {
        progress.isEnabled = true
        progress.isRefreshing = true
    }

    open protected fun hideProgress() {
        progress.isRefreshing = false
        progress.isEnabled = isSwipeToRefreshEnabled
    }

    open protected fun doShowError(message: String, buttonMessage: String? = null,
                                   onButtonClickListener: View.OnClickListener? = null) {
        hideProgress()

        Utils.showError(context, message, footerAdapter,
                buttonMessage = null, parent = root,
                onWebClickListener = Link.OnClickListener { link ->
                    Utils.viewLink(context, link + "?device=mobile")
                },
                onButtonClickListener = onButtonClickListener ?: View.OnClickListener {
                    load()
                })
    }

    private fun showError(exception: ProxerException) {
        doShowError(ErrorHandler.getMessageForErrorCode(context, exception))
    }

    private fun hideError() {
        footerAdapter.removeFooter()
    }
}