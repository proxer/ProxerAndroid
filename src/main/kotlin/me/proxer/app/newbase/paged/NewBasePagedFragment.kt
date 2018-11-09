package me.proxer.app.newbase.paged

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.newbase.NewBaseContentFragment
import me.proxer.app.util.ErrorUtils.ErrorAction
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
abstract class NewBasePagedFragment<T> : NewBaseContentFragment<PagedList<T>>() {

    override val isSwipeToRefreshEnabled = true

    protected open val emptyDataMessage get() = R.string.error_no_data

    protected abstract val innerAdapter: NewBasePagedListAdapter<T, *>
    protected abstract val layoutManager: RecyclerView.LayoutManager

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paged, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EasyHeaderFooterAdapter(innerAdapter)
        innerAdapter.initWithWrappingAdapter(adapter)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun showData(data: PagedList<T>) {
        super.showData(data)

        innerAdapter.submitList(data)
    }

    override fun hideError() {
        adapter.footer = null
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

        val adjustedAction = when (action.message == R.string.error_no_data) {
            true -> ErrorAction(emptyDataMessage, ErrorAction.ACTION_MESSAGE_HIDE)
            false -> action
        }

        super.showError(adjustedAction)
    }
}
