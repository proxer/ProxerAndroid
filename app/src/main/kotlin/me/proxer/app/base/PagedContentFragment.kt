package me.proxer.app.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
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
import org.jetbrains.anko.find
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
abstract class PagedContentFragment<T> : BaseContentFragment<List<T>>() {

    override abstract val viewModel: PagedViewModel<T>

    override val isSwipeToRefreshEnabled = true

    open protected val emptyDataMessage get() = R.string.error_no_data
    open protected val pagingThreshold = 5

    private lateinit var adapter: EasyHeaderFooterAdapter
    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val innerAdapter: BaseAdapter<T, *>

    open protected val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override val contentContainer: ViewGroup
        get() = recyclerView

    override val errorContainer: ViewGroup
        get() = adapter.footer as ViewGroup

    override val errorText: TextView
        get() = errorContainer.find(R.id.errorText)

    override val errorButton: Button
        get() = errorContainer.find(R.id.errorButton)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paged, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        adapter = EasyHeaderFooterAdapter(innerAdapter)
        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        viewModel.refreshError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, it.message, Snackbar.LENGTH_LONG, it.buttonMessage,
                        it.buttonAction?.toClickListener(hostingActivity))

                viewModel.refreshError.value = null
            }
        })

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        recyclerView.endScrolls(pagingThreshold)
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .bindToLifecycle(this)
                .subscribe { viewModel.loadIfPossible() }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun showData(data: List<T>) {
        when {
            data.isEmpty() -> {
                hideData()
                showError(ErrorAction(emptyDataMessage, ErrorAction.ACTION_MESSAGE_HIDE))
            }
            innerAdapter.isEmpty() -> {
                innerAdapter.swapData(data)
                innerAdapter.notifyItemRangeInserted(0, data.size)
            }
            else -> Single.fromCallable { DiffUtil.calculateDiff(innerAdapter.provideDiffUtilCallback(data)) }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .bindToLifecycle(this)
                    .subscribe { it: DiffUtil.DiffResult ->
                        innerAdapter.swapData(data)

                        it.dispatchUpdatesTo(adapter)
                    }
        }

        updateRecyclerViewPadding()
    }

    override fun hideData() {
        innerAdapter.itemCount.let {
            innerAdapter.clear()
            innerAdapter.notifyItemRangeRemoved(0, it)
        }
    }

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

    private fun updateRecyclerViewPadding() = when (innerAdapter.itemCount <= 0) {
        true -> recyclerView.setPadding(0, 0, 0, 0)
        false -> {
            val horizontalPadding = DeviceUtils.getHorizontalMargin(context)
            val verticalPadding = DeviceUtils.getVerticalMargin(context)

            recyclerView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
    }
}
