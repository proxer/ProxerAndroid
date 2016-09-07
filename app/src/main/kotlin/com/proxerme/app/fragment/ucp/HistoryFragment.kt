package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import com.proxerme.app.adapter.ucp.HistoryAdapter
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ucp.entitiy.HistoryEntry
import com.proxerme.library.connection.ucp.request.HistoryRequest

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class HistoryFragment : EasyPagingFragment<HistoryEntry>() {

    companion object {
        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }

    override val section = SectionManager.Section.HISTORY
    override val itemsOnPage = 50

    override lateinit var adapter: HistoryAdapter
    override lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = HistoryAdapter(savedInstanceState)
        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<HistoryEntry>> {
        return LoadingRequest(HistoryRequest(page).withLimit(itemsOnPage))
    }

}