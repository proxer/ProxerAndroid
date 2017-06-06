package me.proxer.app.fragment.ucp

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.ucp.HistoryAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.ucp.UcpHistoryEntry
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class HistoryFragment : PagedLoadingFragment<ProxerCall<List<UcpHistoryEntry>>, UcpHistoryEntry>() {

    companion object {
        fun newInstance(): HistoryFragment {
            return HistoryFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val itemsOnPage = 50
    override val isLoginRequired = true
    override val emptyResultMessage = R.string.error_no_data_history

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, StaggeredGridLayoutManager.VERTICAL)
    }

    override val innerAdapter by lazy { HistoryAdapter(GlideApp.with(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.callback = object : HistoryAdapter.HistoryAdapterCallback {
            override fun onItemClick(view: View, item: UcpHistoryEntry) {
                val imageView = view.find<ImageView>(R.id.image)

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
