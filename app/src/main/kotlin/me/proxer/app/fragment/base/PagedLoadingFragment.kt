package me.proxer.app.fragment.base

import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MainActivity
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.listener.EndlessRecyclerOnScrollListener
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
abstract class PagedLoadingFragment<I, O> : LoadingFragment<I, List<O>>() {

    companion object {
        private const val LIST_STATE = "list_state"
    }

    open protected val shouldReplaceOnRefresh = false
    open protected var hasReachedEnd = false

    override val isWorking: Boolean
        get() = task.isWorking || refreshTask.isWorking

    protected val refreshTask by lazy {
        TaskBuilder.task(constructRefreshTask())
                .validateBefore {
                    validate()
                }
                .bindToLifecycle(this, "${javaClass.simpleName}refreshTask")
                .onInnerStart {
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
                }.build()
    }

    protected lateinit var adapter: EasyHeaderFooterAdapter
    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val innerAdapter: PagingAdapter<O>
    abstract protected val itemsOnPage: Int

    private var listState: Parcelable? = null

    open protected val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = EasyHeaderFooterAdapter(innerAdapter)
        listState = savedInstanceState?.getParcelable(LIST_STATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setOnRefreshListener(when (isSwipeToRefreshEnabled) {
            true -> SwipeRefreshLayout.OnRefreshListener { refreshTask.execute(constructPagedInput(0)) }
            false -> null
        })

        setupList()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(LIST_STATE, layoutManager.onSaveInstanceState())
    }

    override fun onSuccess(result: List<O>) {
        hasReachedEnd = result.size < itemsOnPage

        innerAdapter.append(result)
        cache.mutate { innerAdapter.list }

        if (listState != null) {
            layoutManager.onRestoreInstanceState(listState)

            listState = null
        }

        super.onSuccess(result)
    }

    open protected fun onRefreshSuccess(result: List<O>) {
        when (shouldReplaceOnRefresh) {
            true -> {
                innerAdapter.insert(result)
                cache.mutate { result }
            }
            false -> {
                innerAdapter.replace(result)
            }
        }

        hideError()
        showContent()
    }

    open protected fun onRefreshError(error: Throwable) {
        val action = ErrorUtils.handle(activity as MainActivity, error)

        multilineSnackbar(root, getString(R.string.error_refresh, getString(action.message)), Snackbar.LENGTH_LONG,
                action.buttonMessage, action.buttonAction)
    }

    override fun showContent() {
        // Don't do anything.
    }

    override fun hideContent() {
        // Don't do anything.
    }

    override fun showError(message: Int, buttonMessage: Int, onButtonClickListener: View.OnClickListener?) {
        val errorContainer = when {
            adapter.hasFooter() -> adapter.footer!!
            else -> LayoutInflater.from(context).inflate(R.layout.layout_error, root, false)
        }

        errorContainer.layoutParams.height = when (adapter.itemCount <= 0) {
            true -> ViewGroup.LayoutParams.MATCH_PARENT
            false -> ViewGroup.LayoutParams.WRAP_CONTENT
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
                task.freshExecute(constructPagedInput(calculateNextPage()))
            })
        }

        adapter.footer = errorContainer
    }

    override fun hideError() {
        adapter.removeFooter()
    }

    open protected fun constructRefreshTask() = constructTask()

    override fun constructInput() = constructPagedInput(calculateNextPage())
    abstract protected fun constructPagedInput(page: Int): I

    private fun calculateNextPage(): Int {
        if (innerAdapter.isEmpty()) {
            return 0
        } else {
            return innerAdapter.itemCount / itemsOnPage
        }
    }

    private fun setupList() {
        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!hasReachedEnd && !adapter.hasFooter() && !isWorking) {
                    task.freshExecute(constructPagedInput(calculateNextPage()))
                }
            }
        })
    }
}
