package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.HistoryAdapter
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.module.LoginModule
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ucp.entitiy.HistoryEntry
import com.proxerme.library.connection.ucp.request.HistoryRequest

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class HistoryFragment : EasyPagingFragment<HistoryEntry, HistoryAdapter.HistoryAdapterCallback>() {

    companion object {

        const val ITEMS_ON_PAGE = 50

        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@HistoryFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@HistoryFragment.doShowError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            this@HistoryFragment.load()
        }
    })

    override val section = Section.HISTORY
    override val itemsOnPage = ITEMS_ON_PAGE
    override val isSwipeToRefreshEnabled = false
    override val canLoad: Boolean
        get() = super.canLoad && loginModule.canLoad()

    override lateinit var adapter: HistoryAdapter
    override lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = HistoryAdapter(savedInstanceState)
        adapter.callback = object : HistoryAdapter.HistoryAdapterCallback() {
            override fun onItemClick(v: View, item: HistoryEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }

        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
    }

    override fun onStart() {
        super.onStart()

        loginModule.onStart()
    }

    override fun onResume() {
        super.onResume()

        loginModule.onResume()
    }

    override fun onStop() {
        loginModule.onStop()

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<HistoryEntry>> {
        return LoadingRequest(HistoryRequest(page).withLimit(ITEMS_ON_PAGE))
    }

}