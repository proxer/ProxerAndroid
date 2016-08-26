package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.klinker.android.link_builder.TouchableMovementMethod
import com.liucanwen.app.headerfooterrecyclerview.ExStaggeredGridLayoutManager
import com.liucanwen.app.headerfooterrecyclerview.HeaderAndFooterRecyclerViewAdapter
import com.liucanwen.app.headerfooterrecyclerview.HeaderSpanSizeLookup
import com.liucanwen.app.headerfooterrecyclerview.RecyclerViewUtils
import com.proxerme.app.R
import com.proxerme.app.adapter.PagingAdapter
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.interfaces.IdItem
import org.jetbrains.anko.onClick

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class EasyPagingFragment<T> : PagingFragment<T>()  where T : IdItem, T : Parcelable {

    private companion object {
        private const val EXCEPTION_STATE = "fragment_easy_paging_state_exception"
    }

    open protected val isSwipeToRefreshEnabled = true

    abstract protected val adapter: PagingAdapter<T>
    protected lateinit var headerAndFooterAdapter: HeaderAndFooterRecyclerViewAdapter

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

        headerAndFooterAdapter = HeaderAndFooterRecyclerViewAdapter(adapter)

        list.setHasFixedSize(true)
        list.adapter = headerAndFooterAdapter
        list.layoutManager = layoutManager
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (exception == null && !endReached) {
                    load()
                }
            }
        })

        if (layoutManager is GridLayoutManager) {
            (layoutManager as GridLayoutManager).spanSizeLookup =
                    HeaderSpanSizeLookup(headerAndFooterAdapter,
                            (layoutManager as GridLayoutManager).spanCount)
        } else if (layoutManager is ExStaggeredGridLayoutManager) {
            (layoutManager as ExStaggeredGridLayoutManager).spanSizeLookup =
                    HeaderSpanSizeLookup(headerAndFooterAdapter,
                            (layoutManager as ExStaggeredGridLayoutManager).spanCount)
        }

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
        progress.setColorSchemeColors(ContextCompat.getColor(context,
                R.color.primary))

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

    private fun showError(exception: ProxerException) {
        val errorContainer: ViewGroup

        if (adapter.itemCount <= 0) {
            errorContainer = LayoutInflater.from(context).inflate(R.layout.layout_error,
                    root, false) as ViewGroup
        } else {
            errorContainer = LayoutInflater.from(context).inflate(R.layout.item_error,
                    root, false) as ViewGroup

            errorContainer.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val errorText: TextView = errorContainer.findViewById(R.id.errorText) as TextView
        val errorButton: Button = errorContainer.findViewById(R.id.errorButton) as Button

        errorText.movementMethod = TouchableMovementMethod.getInstance()
        errorText.text = ErrorHandler.getMessageForErrorCode(context, exception)
        errorButton.onClick {
            load()
        }

        RecyclerViewUtils.setFooterView(list, errorContainer)
    }

    private fun hideError() {
        RecyclerViewUtils.removeFooterView(list)
    }
}