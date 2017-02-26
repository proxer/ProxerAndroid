package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.HistoryAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.fragment.ucp.HistoryFragment.HistoryInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.DeviceUtils
import com.proxerme.library.connection.ucp.entitiy.HistoryEntry
import com.proxerme.library.connection.ucp.request.HistoryRequest

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class HistoryFragment : PagedLoadingFragment<HistoryInput, HistoryEntry>() {

    companion object {
        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }

    override val section = Section.HISTORY
    override val itemsOnPage = 50
    override val isLoginRequired = true

    override lateinit var adapter: HistoryAdapter
    override lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = HistoryAdapter()
        adapter.callback = object : HistoryAdapter.HistoryAdapterCallback() {
            override fun onItemClick(item: HistoryEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name, item.category)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = GridLayoutManager(context, DeviceUtils.calculateSpanAmount(activity) + 1)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun constructTask(): Task<HistoryInput, Array<HistoryEntry>> {
        return ProxerLoadingTask({ HistoryRequest(it.page).withLimit(it.itemsOnPage) })
    }

    override fun constructInput(page: Int): HistoryInput {
        return HistoryInput(page, itemsOnPage)
    }

    override fun getEmptyMessage(): Int {
        return R.string.error_no_data_history
    }

    class HistoryInput(page: Int, val itemsOnPage: Int) : PagedInput(page)
}
