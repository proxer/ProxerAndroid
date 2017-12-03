package me.proxer.app.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.endScrolls
import me.proxer.app.util.extension.isAtCompleteTop
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.postDelayedSafely
import me.proxer.app.util.extension.scrollToTop
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
abstract class PagedContentFragment<T> : BaseContentFragment<List<T>>() {

    abstract override val viewModel: PagedViewModel<T>

    override val isSwipeToRefreshEnabled = true

    protected open val emptyDataMessage get() = R.string.error_no_data
    protected open val pagingThreshold = 5

    protected abstract val layoutManager: RecyclerView.LayoutManager
    protected abstract val innerAdapter: BaseAdapter<T, *>
    protected var adapter by Delegates.notNull<EasyHeaderFooterAdapter>()

    protected open val recyclerView: RecyclerView by bindView(R.id.recyclerView)

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

    private var isFirstData = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paged, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                .autoDispose(this)
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

        val wasAtFirstPosition = isAtTop()
        val wasEmpty = innerAdapter.isEmpty()

        innerAdapter.swapDataAndNotifyWithDiffing(data)

        if (innerAdapter.isEmpty()) {
            showError(ErrorAction(emptyDataMessage, ACTION_MESSAGE_HIDE))
        } else if (!isFirstData && (wasAtFirstPosition || wasEmpty)) {
            recyclerView.postDelayedSafely({ recyclerView ->
                when {
                    wasEmpty -> scrollToTop()
                    else -> recyclerView.smoothScrollToPosition(0)
                }
            }, 50)
        }

        isFirstData = false
    }

    override fun hideData() = innerAdapter.swapDataAndNotifyWithDiffing(emptyList())

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

        isFirstData = false

        super.showError(action)
    }

    override fun hideError() {
        adapter.footer = null
    }

    protected open fun isAtTop() = layoutManager.isAtCompleteTop()
    protected open fun scrollToTop() = layoutManager.scrollToTop()

    protected open fun updateRecyclerViewPadding() = when (innerAdapter.itemCount <= 0 && adapter.footer != null) {
        true -> recyclerView.setPadding(0, 0, 0, 0)
        false -> {
            val horizontalPadding = DeviceUtils.getHorizontalMargin(safeContext)
            val verticalPadding = DeviceUtils.getVerticalMargin(safeContext)

            recyclerView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
    }
}
