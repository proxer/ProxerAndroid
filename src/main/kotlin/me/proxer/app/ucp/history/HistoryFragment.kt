package me.proxer.app.ucp.history

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.ucp.UcpHistoryEntry
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class HistoryFragment : PagedContentFragment<UcpHistoryEntry>() {

    companion object {
        fun newInstance() = HistoryFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_history
    override val isSwipeToRefreshEnabled = false

    override val viewModel by unsafeLazy { HistoryViewModelProvider.get(this) }

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, StaggeredGridLayoutManager.VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<HistoryAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = HistoryAdapter()

        innerAdapter.clickSubject
                .autoDispose(this)
                .subscribe { (view, item) ->
                    MediaActivity.navigateTo(activity, item.id, item.name, item.category,
                            if (view.drawable != null) view else null)
                }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }
}
