package me.proxer.app.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.endScrolls
import me.proxer.app.util.extension.multilineSnackbar
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
abstract class PagedContentFragment<T> : BaseContentFragment<List<T>>() {

    override abstract val viewModel: PagedViewModel<T>

    override val isSwipeToRefreshEnabled = true

    open protected val emptyDataMessage get() = R.string.error_no_data
    open protected val pagingThreshold = 5

    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val innerAdapter: BaseAdapter<T, *>
    private lateinit var adapter: EasyHeaderFooterAdapter

    open protected val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override val contentContainer: ViewGroup
        get() = recyclerView

    override val errorContainer: ViewGroup
        get() = adapter.footer as ViewGroup

    override val errorInnerContainer: ViewGroup
        get() = errorContainer.findViewById(R.id.errorInnerContainer)

    override val errorText: TextView
        get() = errorContainer.findViewById(R.id.errorText)

    override val errorButton: Button
        get() = errorContainer.findViewById(R.id.errorButton)

    // This is an ugly hack, but I can't figure out another way around RecyclerView's bugs.
    private var isFirstData = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paged, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.refreshError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_refresh, getString(it.message)), Snackbar.LENGTH_LONG,
                        it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))
            }
        })

        adapter = EasyHeaderFooterAdapter(innerAdapter)
        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        recyclerView.endScrolls(pagingThreshold)
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .bindToLifecycle(this)
                .subscribe { viewModel.loadIfPossible() }
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun showData(data: List<T>) {
        updateRecyclerViewPadding()

        when {
            innerAdapter.isEmpty() -> {
                innerAdapter.swapDataAndNotifyInsertion(data)

                if (data.isEmpty()) {
                    showError(ErrorAction(emptyDataMessage, ErrorAction.ACTION_MESSAGE_HIDE))
                } else {
                    if (!isFirstData) {
                        recyclerView.let {
                            it.postDelayed({
                                scrollToTop()
                            }, 50)
                        }
                    }

                    isFirstData = false
                }
            }
            else -> Single.fromCallable { DiffUtil.calculateDiff(innerAdapter.provideDiffUtilCallback(data)) }
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .bindToLifecycle(this)
                    .subscribe { it: DiffUtil.DiffResult ->
                        val wasAtFirstPosition = isAtTop()

                        innerAdapter.swapDataAndNotifyWithDiffResult(data, it)

                        if (data.isEmpty()) {
                            showError(ErrorAction(emptyDataMessage, ErrorAction.ACTION_MESSAGE_HIDE))
                        } else {
                            if (wasAtFirstPosition) {
                                recyclerView.let {
                                    it.postDelayed({
                                        it.smoothScrollToPosition(0)
                                    }, 50)
                                }
                            }
                        }
                    }
        }
    }

    override fun hideData() = innerAdapter.clearAndNotifyRemoval()

    override fun showError(action: ErrorAction) {
        if (adapter.footer == null) {
            adapter.footer = LayoutInflater.from(context).inflate(R.layout.layout_error, root, false).apply {
                layoutParams.height = when (innerAdapter.itemCount <= 0) {
                    true -> ViewGroup.LayoutParams.MATCH_PARENT
                    false -> ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }

        updateRecyclerViewPadding()

        super.showError(action)
    }

    override fun hideError() {
        adapter.footer = null
    }

    private fun updateRecyclerViewPadding() = when (innerAdapter.itemCount <= 0 && adapter.footer != null) {
        true -> recyclerView.setPadding(0, 0, 0, 0)
        false -> {
            val horizontalPadding = DeviceUtils.getHorizontalMargin(context)
            val verticalPadding = DeviceUtils.getVerticalMargin(context)

            recyclerView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
    }

    private fun isAtTop() = layoutManager.let {
        when (it) {
            is StaggeredGridLayoutManager -> it.findFirstCompletelyVisibleItemPositions(null).contains(0)
            is LinearLayoutManager -> it.findFirstCompletelyVisibleItemPosition() == 0
            else -> false
        }
    }

    private fun scrollToTop() = layoutManager.let {
        when (it) {
            is StaggeredGridLayoutManager -> it.scrollToPositionWithOffset(0, 0)
            is LinearLayoutManager -> it.scrollToPositionWithOffset(0, 0)
        }
    }
}
