package me.proxer.app.fragment.base

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.MarginDecoration
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.listener.EndlessRecyclerOnScrollListener
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
abstract class PagedLoadingFragment<I, O> : LoadingFragment<I, List<O>>() {

    open protected val shouldReplaceOnRefresh = false
    open protected var hasReachedEnd = false

    open protected val spanCount get() = DeviceUtils.calculateSpanAmount(activity)
    open protected val noDataMessage get() = R.string.error_no_data

    override val isWorking get() = super.isWorking || refreshTask.isWorking

    protected lateinit var refreshTask: AndroidLifecycleTask<I, List<O>>

    protected lateinit var adapter: EasyHeaderFooterAdapter
    protected lateinit var layoutManager: StaggeredGridLayoutManager

    abstract protected val innerAdapter: PagingAdapter<O>
    abstract protected val itemsOnPage: Int

    open protected val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshTask = TaskBuilder.task(constructRefreshTask())
                .validateBefore { validateRefresh() }
                .bindToLifecycle(this, "${javaClass.simpleName}refresh")
                .onInnerStart {
                    hideError()
                    hideContent()
                    setProgressVisible(true)
                }
                .onSuccess {
                    onRefreshSuccess(it)
                }
                .onError {
                    onRefreshError(it)
                }
                .onFinish {
                    setProgressVisible(isWorking)
                }
                .build()

        adapter = EasyHeaderFooterAdapter(innerAdapter)
        layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)

        innerAdapter.positionResolver = object : PagingAdapter.PositionResolver() {
            override fun resolveRealPosition(position: Int) = adapter.getRealPosition(position)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setOnRefreshListener(when (isSwipeToRefreshEnabled) {
            true -> SwipeRefreshLayout.OnRefreshListener { refreshTask.forceExecute(constructPagedInput(0)) }
            false -> null
        })

        setupList()
    }

    override fun onDestroyView() {
        innerAdapter.destroy()
        progress.setOnRefreshListener(null)
        list.clearOnScrollListeners()

        super.onDestroyView()
    }

    override fun onSuccess(result: List<O>) {
        hasReachedEnd = result.size < itemsOnPage

        innerAdapter.append(result)

        super.onSuccess(result)
    }

    open protected fun onRefreshSuccess(result: List<O>) {
        when (shouldReplaceOnRefresh) {
            true -> {
                innerAdapter.insert(result)
            }
            false -> {
                innerAdapter.replace(result)
            }
        }

        hideError()
        showContent()

        saveResultToState(result)
    }

    open protected fun onRefreshError(error: Throwable) {
        if (innerAdapter.itemCount <= 0) {
            super.onError(error)
        } else {
            showRefreshError(handleError(error))
        }
    }

    override fun showContent() {
        updateListPadding()
        showEmptyIfNecessary()
    }

    override fun hideContent() {
        // Don't do anything here, we want to keep showing the current content.
    }

    override fun showError(message: Int, buttonMessage: Int, buttonAction: View.OnClickListener?) {
        val errorContainer = when {
            adapter.hasFooter() -> adapter.footer!!
            else -> LayoutInflater.from(context).inflate(R.layout.layout_error, root, false)
        }

        errorContainer.find<TextView>(R.id.errorText).text = getString(message)
        errorContainer.find<Button>(R.id.errorButton).apply {
            text = when (buttonMessage) {
                ErrorAction.ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
                ErrorAction.ACTION_MESSAGE_HIDE -> null
                else -> getString(buttonMessage)
            }

            visibility = when (buttonMessage) {
                ErrorAction.ACTION_MESSAGE_HIDE -> View.GONE
                else -> View.VISIBLE
            }

            setOnClickListener(buttonAction ?: View.OnClickListener {
                task.freshExecute(constructInput())
            })
        }

        errorContainer.layoutParams.height = when (innerAdapter.itemCount <= 0) {
            true -> ViewGroup.LayoutParams.MATCH_PARENT
            false -> ViewGroup.LayoutParams.WRAP_CONTENT
        }

        adapter.footer = errorContainer

        updateListPadding()
    }

    override fun hideError() {
        adapter.removeFooter()
    }

    protected fun showRefreshError(action: ErrorAction) {
        showRefreshError(action.message, action.buttonMessage, action.buttonAction)
    }

    open protected fun showRefreshError(message: Int, buttonMessage: Int, buttonAction: View.OnClickListener?) {
        multilineSnackbar(root, getString(R.string.error_refresh, getString(message)),
                Snackbar.LENGTH_LONG, buttonMessage, buttonAction)
    }

    override fun saveResultToState(result: List<O>) {
        state.data = innerAdapter.list
    }

    override fun removeResultFromState() {
        // We do not want to remove already loaded results here.
    }

    override fun freshLoad() {
        innerAdapter.clear()

        super.freshLoad()
    }

    open protected fun showEmptyIfNecessary() {
        if (innerAdapter.itemCount <= 0) {
            showError(noDataMessage, ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    open protected fun validateRefresh() = validate()
    open protected fun constructRefreshTask() = constructTask()

    override fun constructInput() = calculateNextPage().run { constructPagedInput(this) }
    abstract protected fun constructPagedInput(page: Int): I

    private fun calculateNextPage() = when {
        innerAdapter.isEmpty() -> 0
        else -> innerAdapter.itemCount / itemsOnPage
    }

    private fun setupList() {
        list.layoutManager = layoutManager
        list.adapter = adapter
        list.addItemDecoration(MarginDecoration(context, layoutManager.spanCount))

        list.setHasFixedSize(true)
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!hasReachedEnd && !adapter.hasFooter() && !isWorking) {
                    task.forceExecute(constructInput())
                }
            }
        })
    }

    private fun updateListPadding() {
        val horizontalPadding = DeviceUtils.getHorizontalMargin(context)

        if (innerAdapter.itemCount <= 0) {
            list.setPadding(horizontalPadding, 0, horizontalPadding, 0)
        } else {
            val verticalPadding = DeviceUtils.getVerticalMargin(context)

            list.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
    }
}
