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
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MainActivity
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.MarginDecoration
import me.proxer.app.util.extension.bindView
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
            handleRefreshError(error)
        }
    }

    override fun showContent() {
        // Don't do anything here, we don't hide the current content when loading.
    }

    override fun hideContent() {
        // Don't do anything here, we want to keep showing the current content.
    }

    open protected fun handleRefreshError(error: Throwable) {
        ErrorUtils.handle(activity as MainActivity, error).let {
            multilineSnackbar(root, getString(R.string.error_refresh, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction)
        }
    }

    override fun showError(message: Int, buttonMessage: Int, onButtonClickListener: View.OnClickListener?) {
        val errorContainer = when {
            adapter.hasFooter() -> adapter.footer!!
            else -> LayoutInflater.from(context).inflate(R.layout.layout_error, root, false)
        }

        errorContainer.find<TextView>(R.id.errorText).text = getString(message)
        errorContainer.find<Button>(R.id.errorButton).apply {
            text = when (buttonMessage) {
                ErrorUtils.ErrorAction.ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
                ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE -> null
                else -> getString(buttonMessage)
            }

            visibility = when (buttonMessage) {
                ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE -> View.GONE
                else -> View.VISIBLE
            }

            setOnClickListener(onButtonClickListener ?: View.OnClickListener {
                task.freshExecute(constructInput())
            })
        }

        errorContainer.layoutParams.height = when (innerAdapter.itemCount <= 0) {
            true -> ViewGroup.LayoutParams.MATCH_PARENT
            false -> ViewGroup.LayoutParams.WRAP_CONTENT
        }

        adapter.footer = errorContainer

        if (innerAdapter.itemCount <= 0) {
            errorContainer.post {
                errorContainer.layoutParams.height = errorContainer.height - list.paddingTop - list.paddingBottom

                adapter.notifyItemChanged(adapter.itemCount)
            }
        }
    }

    override fun hideError() {
        adapter.removeFooter()
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
}
