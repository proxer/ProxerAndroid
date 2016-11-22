package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.klinker.android.link_builder.Link
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.task.Task
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.KotterKnife
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagedLoadingFragment<T> : MainFragment() {

    private val successCallback = { data: Array<T> ->
        hasReachedEnd = data.size < itemsOnPage

        present(calculateNextPage(), data)
        adapter.append(data)
    }

    private val exceptionCallback = { exception: Exception ->
        val message = if (exception is ProxerException) {
            ErrorHandler.getMessageForErrorCode(context, exception)
        } else context.getString(R.string.error_unknown)

        showError(message)
    }

    open protected val isSwipeToRefreshEnabled = false

    private lateinit var task: Task<Array<T>>

    open protected val list: RecyclerView by bindView(R.id.list)
    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)

    protected lateinit var headerFooterAdapter: EasyHeaderFooterAdapter

    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val adapter: PagingAdapter<T>
    abstract protected val itemsOnPage: Int

    private var hasReachedEnd = false
    private var firstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        task = constructTask { calculateNextPage() }
                .onStart {
                    setRefreshing(true)
                }
                .onFinish {
                    setRefreshing(false)
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging_default, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.primary, R.color.accent)

        setupList()

        if (firstLoad) {
            firstLoad = false

            task.execute(successCallback, exceptionCallback)
        }
    }

    override fun onDestroyView() {
        list.adapter = null
        list.layoutManager = null
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        task.destroy()

        super.onDestroy()
    }

    open protected fun showError(message: String, buttonMessage: String? = null,
                                 onButtonClickListener: View.OnClickListener? = null) {
        Utils.showError(context, message, headerFooterAdapter,
                buttonMessage = buttonMessage, parent = root,
                onWebClickListener = Link.OnClickListener { link ->
                    Utils.viewLink(context, link + "?device=mobile")
                },
                onButtonClickListener = onButtonClickListener ?: View.OnClickListener {
                    task.execute(successCallback, exceptionCallback)

                    headerFooterAdapter.removeFooter()
                })
    }

    open protected fun reset() {
        task.reset()
        task.execute(successCallback, exceptionCallback)
    }

    open protected fun present(page: Int, data: Array<T>) {

    }

    private fun setupList() {
        headerFooterAdapter = EasyHeaderFooterAdapter(adapter)

        list.layoutManager = layoutManager
        list.adapter = headerFooterAdapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!hasReachedEnd && !headerFooterAdapter.hasFooter() && !task.isWorking) {
                    task.execute(successCallback, exceptionCallback)
                }
            }
        })
    }

    private fun calculateNextPage(): Int {
        if (adapter.isEmpty()) {
            return 0
        } else {
            return adapter.itemCount / itemsOnPage
        }
    }

    private fun setRefreshing(enable: Boolean) {
        progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
        progress.isRefreshing = enable
    }

    abstract fun constructTask(pageCallback: () -> Int): Task<Array<T>>
}