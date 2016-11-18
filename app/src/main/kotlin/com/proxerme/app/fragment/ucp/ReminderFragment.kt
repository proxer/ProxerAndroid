package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.ReminderAdapter
import com.proxerme.app.application.MainApplication
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.module.LoginModule
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.ucp.entitiy.Reminder
import com.proxerme.library.connection.ucp.request.DeleteReminderRequest
import com.proxerme.library.connection.ucp.request.ReminderRequest
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class ReminderFragment : EasyPagingFragment<Reminder>() {

    companion object {

        const val ITEMS_ON_PAGE = 30
        private const val CATEGORY_STATE = "fragment_reminder_state_category"

        fun newInstance(): ReminderFragment {
            return ReminderFragment()
        }
    }

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@ReminderFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@ReminderFragment.clear()
            this@ReminderFragment.doShowError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            this@ReminderFragment.load()
        }
    })

    override val section = Section.REMINDER
    override val itemsOnPage = ITEMS_ON_PAGE
    override val isSwipeToRefreshEnabled = true
    override val canLoad: Boolean
        get() = super.canLoad && loginModule.canLoad()

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ReminderAdapter

    @CategoryParameter.Category
    private var category: String? = null

    private var removalTask: ProxerCall? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            category = it.getString(CATEGORY_STATE)
        }

        adapter = ReminderAdapter(savedInstanceState)
        adapter.callback = object : ReminderAdapter.ReminderAdapterCallback() {
            override fun onItemClick(item: Reminder) {
                MediaActivity.navigateTo(activity, item.entryId, item.name)
            }

            override fun onRemoveClick(item: Reminder) {
                adapter.addItemToRemove(item)

                synchronize()
            }
        }

        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)

        setHasOptionsMenu(true)
        synchronize()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_reminder, menu)

        when (category) {
            CategoryParameter.ANIME -> menu.findItem(R.id.anime).isChecked = true
            CategoryParameter.MANGA -> menu.findItem(R.id.manga).isChecked = true
            else -> menu.findItem(R.id.all).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCategory = category

        when (item.itemId) {
            R.id.anime -> category = CategoryParameter.ANIME
            R.id.manga -> category = CategoryParameter.MANGA
            R.id.all -> category = null
        }

        if (category != previousCategory) {
            reset()

            item.isChecked = true
        }

        return super.onOptionsItemSelected(item)
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

    override fun onDestroy() {
        removalTask?.cancel()
        removalTask = null

        super.onDestroy()
    }

    override fun onPagedLoadStarted(page: Int) {
        super.onPagedLoadStarted(page)

        if (page == 0) {
            clear()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
        outState.putString(CATEGORY_STATE, category)
    }

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<Reminder>> {
        return LoadingRequest(ReminderRequest(page).withCategory(category).withLimit(ITEMS_ON_PAGE))
    }

    @Synchronized
    private fun synchronize() {
        if (removalTask != null) {
            return
        }

        if (adapter.itemsToRemove.isNotEmpty()) {
            val itemToRemove = adapter.itemsToRemove.first()

            removalTask = MainApplication.proxerConnection
                    .execute(DeleteReminderRequest(itemToRemove.id), {
                        adapter.remove(itemToRemove)
                        removalTask = null

                        synchronize()
                    }, {
                        val amount = adapter.itemsToRemove.size
                        val errorText = context.resources
                                .getQuantityString(R.plurals.error_reminder_removal, amount, amount)

                        adapter.clearRemovalQueue()
                        removalTask = null

                        Snackbar.make(root, errorText, Snackbar.LENGTH_LONG).show()
                    })
        }
    }
}