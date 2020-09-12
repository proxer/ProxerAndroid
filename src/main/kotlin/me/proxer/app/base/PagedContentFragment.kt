package me.proxer.app.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.doAfterAnimations
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.endScrolls
import me.proxer.app.util.extension.isAtCompleteTop
import me.proxer.app.util.extension.multilineSnackbar
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
abstract class PagedContentFragment<T>(
    @LayoutRes contentLayoutId: Int = R.layout.fragment_paged
) : BaseContentFragment<List<T>>(contentLayoutId) {

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

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (isAtTop() && positionStart == 0) {
                recyclerView.doAfterAnimations {
                    if (view != null) recyclerView.smoothScrollToPosition(0)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EasyHeaderFooterAdapter(innerAdapter)
        innerAdapter.positionResolver = ContainerPositionResolver(adapter)
        innerAdapter.registerAdapterDataObserver(adapterDataObserver)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        recyclerView.endScrolls(pagingThreshold)
            .throttleFirst(300, TimeUnit.MILLISECONDS)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.loadIfPossible() }

        viewModel.refreshError.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    hostingActivity.multilineSnackbar(
                        getString(R.string.error_refresh, getString(it.message)),
                        Snackbar.LENGTH_LONG,
                        it.buttonMessage,
                        it.toClickListener(hostingActivity)
                    )
                }
            }
        )
    }

    override fun onDestroyView() {
        innerAdapter.unregisterAdapterDataObserver(adapterDataObserver)

        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun showData(data: List<T>) {
        innerAdapter.swapDataAndNotifyWithDiffing(data)

        if (innerAdapter.isEmpty()) {
            showError(ErrorAction(emptyDataMessage, ACTION_MESSAGE_HIDE))
        }
    }

    override fun hideData() {
        innerAdapter.swapDataAndNotifyWithDiffing(emptyList())
    }

    override fun showError(action: ErrorAction) {
        // Assign error footer if not existing or another footer.
        if (adapter.footer?.findViewById<View>(R.id.errorContainer) == null) {
            adapter.footer = LayoutInflater.from(context).inflate(R.layout.layout_error, root, false).apply {
                layoutParams.height = when (innerAdapter.itemCount <= 0) {
                    true -> ViewGroup.LayoutParams.MATCH_PARENT
                    false -> ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }

        super.showError(action)
    }

    override fun hideError() {
        adapter.footer = null
    }

    protected open fun isAtTop() = recyclerView.isAtCompleteTop()
}
