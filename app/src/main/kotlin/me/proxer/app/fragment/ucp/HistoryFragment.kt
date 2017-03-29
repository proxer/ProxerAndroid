package me.proxer.app.fragment.ucp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.ucp.HistoryAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.ucp.UcpHistoryEntry

/**
 * @author Ruben Gees
 */
class HistoryFragment : PagedLoadingFragment<ProxerCall<List<UcpHistoryEntry>>, UcpHistoryEntry>() {

    companion object {
        fun newInstance(): HistoryFragment {
            return HistoryFragment().apply {
                arguments = Bundle()
            }
        }
    }

    override val itemsOnPage = 50
    override val isLoginRequired = true
    override val emptyResultMessage = R.string.error_no_data_history
    override val spanCount get() = super.spanCount + 1

    override val innerAdapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.callback = object : HistoryAdapter.HistoryAdapterCallback {
            override fun onItemClick(view: View, item: UcpHistoryEntry) {
                val imageView = view.findViewById(R.id.image) as ImageView

                MediaActivity.navigateTo(activity, item.id, item.name, item.category,
                        if (imageView.drawable != null) imageView else null)
            }
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<UcpHistoryEntry>>().build()
    override fun constructPagedInput(page: Int) = api.ucp().history()
            .page(page)
            .limit(itemsOnPage)
            .build()
}
