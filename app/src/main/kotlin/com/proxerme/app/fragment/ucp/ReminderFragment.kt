package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.ReminderAdapter
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.module.LoginModule
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ucp.entitiy.Reminder
import com.proxerme.library.connection.ucp.request.ReminderRequest

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class ReminderFragment : EasyPagingFragment<Reminder, ReminderAdapter.ReminderAdapterCallback>() {

    companion object {

        const val ITEMS_ON_PAGE = 30

        fun newInstance(): ReminderFragment {
            return ReminderFragment()
        }
    }

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@ReminderFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@ReminderFragment.doShowError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            this@ReminderFragment.load()
        }
    })

    override val section = Section.REMINDER
    override val itemsOnPage = ITEMS_ON_PAGE
    override val isSwipeToRefreshEnabled = false
    override val canLoad: Boolean
        get() = super.canLoad && loginModule.canLoad()

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ReminderAdapter(savedInstanceState)
        adapter.callback = object : ReminderAdapter.ReminderAdapterCallback() {
            override fun onItemClick(v: View, item: Reminder) {
                MediaActivity.navigateTo(activity, item.entryId, item.name)
            }

            override fun onRemoveClick(v: View, item: Reminder) {
                //TODO
            }
        }

        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
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

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<Reminder>> {
        return LoadingRequest(ReminderRequest(page).withLimit(ITEMS_ON_PAGE))
    }
}