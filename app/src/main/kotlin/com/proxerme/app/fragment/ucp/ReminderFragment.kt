package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.ReminderAdapter
import com.proxerme.app.application.MainApplication
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.LoadingTask
import com.proxerme.app.task.Task
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
class ReminderFragment : PagedLoadingFragment<Reminder>() {

    companion object {

        fun newInstance(): ReminderFragment {
            return ReminderFragment()
        }
    }

    override val section = Section.REMINDER
    override val itemsOnPage = 30
    override val resetOnRefresh = true
    override val isLoginRequired = true

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ReminderAdapter

    @CategoryParameter.Category
    private var category: String? = null

    private var removalTask: ProxerCall? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ReminderAdapter()
        adapter.callback = object : ReminderAdapter.ReminderAdapterCallback() {
            override fun onItemClick(item: Reminder) {
                MediaActivity.navigateTo(activity, item.entryId, item.name)
            }

            override fun onRemoveClick(item: Reminder) {
                adapter.addItemToRemove(item)

                synchronize()
            }
        }

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)

        return super.onCreateView(inflater, container, savedInstanceState)
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

    override fun onDestroy() {
        removalTask?.cancel()
        removalTask = null

        super.onDestroy()
    }

    override fun constructTask(pageCallback: () -> Int): Task<Array<Reminder>> {
        return LoadingTask {
            ReminderRequest(pageCallback.invoke())
                    .withCategory(category)
                    .withLimit(itemsOnPage)
        }
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