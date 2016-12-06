package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.HistoryAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.LoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ucp.entitiy.HistoryEntry
import com.proxerme.library.connection.ucp.request.HistoryRequest

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class HistoryFragment : PagedLoadingFragment<HistoryEntry>() {

    companion object {

        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }

    override val section = Section.HISTORY
    override val itemsOnPage = 50
    override val isSwipeToRefreshEnabled = false
    override val isLoginRequired = true

    override lateinit var adapter: HistoryAdapter
    override lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = HistoryAdapter()
        adapter.callback = object : HistoryAdapter.HistoryAdapterCallback() {
            override fun onItemClick(item: HistoryEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun constructTask(pageCallback: () -> Int): ListenableTask<Array<HistoryEntry>> {
        return LoadingTask({ HistoryRequest(pageCallback.invoke()).withLimit(itemsOnPage) })
    }
}