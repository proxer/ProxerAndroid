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
import com.rubengees.ktask.base.Task
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MainActivity
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.task.PagedTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.MarginDecoration
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.paged
import me.proxer.app.util.listener.EndlessRecyclerOnScrollListener
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
abstract class PagedLoadingFragment<I, O> : LoadingFragment<Pair<Int, I>, Pair<Int, List<O>>>() {

    open protected val shouldReplaceOnRefresh = false
    open protected var hasReachedEnd = false

    open protected val spanCount get() = DeviceUtils.calculateSpanAmount(activity)

    protected lateinit var adapter: EasyHeaderFooterAdapter
    protected lateinit var layoutManager: StaggeredGridLayoutManager

    abstract protected val innerAdapter: PagingAdapter<O>
    abstract protected val itemsOnPage: Int

    open protected val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = EasyHeaderFooterAdapter(innerAdapter)
        layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setOnRefreshListener(when (isSwipeToRefreshEnabled) {
            true -> SwipeRefreshLayout.OnRefreshListener { task.freshExecute(0 to constructPagedInput(0)) }
            false -> null
        })

        setupList()
    }

    override fun onDestroyView() {
        innerAdapter.destroy()
        progress.setOnRefreshListener(null)

        super.onDestroyView()
    }

    override fun onSuccess(result: Pair<Int, List<O>>) {
        hasReachedEnd = result.second.size < itemsOnPage

        when (result.first) {
            0 -> when (shouldReplaceOnRefresh) {
                true -> {
                    innerAdapter.insert(result.second)
                }
                false -> {
                    innerAdapter.replace(result.second)
                }
            }
            else -> innerAdapter.append(result.second)
        }

        super.onSuccess(result)
    }

    override fun onError(error: Throwable) {
        if (error is PagedTask.PagedException) {
            if (error.page > 0 || innerAdapter.itemCount <= 0) {
                super.onError(error)
            } else {
                val action = ErrorUtils.handle(activity as MainActivity, error)

                multilineSnackbar(root, getString(R.string.error_refresh, getString(action.message)),
                        Snackbar.LENGTH_LONG, action.buttonMessage, action.buttonAction)
            }
        } else {
            val action = ErrorUtils.handle(activity as MainActivity, error)

            multilineSnackbar(root, getString(action.message), Snackbar.LENGTH_LONG, action.buttonMessage,
                    action.buttonAction)
        }
    }

    override fun showContent() {
        // Don't do anything here, we don't hide the current content when loading.
    }

    override fun hideContent() {
        // Don't do anything here, we want to keep showing the current content.
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

        list.post {
            errorContainer.layoutParams.height = when (innerAdapter.itemCount <= 0) {
                true -> list.height - list.paddingTop - list.paddingBottom
                false -> ViewGroup.LayoutParams.WRAP_CONTENT
            }

            adapter.footer = errorContainer
        }
    }

    override fun hideError() {
        adapter.removeFooter()
    }

    override fun saveResultToState(result: Pair<Int, List<O>>) {
        state.data = 0 to innerAdapter.list
    }

    override fun removeResultFromState(error: Throwable) {
        // We do not want to remove already loaded results here.
    }

    override fun removeErrorFromState(result: Pair<Int, List<O>>) {
        if (result.first > 0) {
            state.error = null
        }
    }

    override fun freshLoad() {
        innerAdapter.clear()

        super.freshLoad()
    }

    override fun constructTask() = TaskBuilder.task(constructPagedTask())
            .paged()
            .build()

    override fun constructInput() = calculateNextPage().run {
        this to constructPagedInput(this)
    }

    abstract protected fun constructPagedTask(): Task<I, List<O>>
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
                    task.freshExecute(constructInput())
                }
            }
        })
    }
}
