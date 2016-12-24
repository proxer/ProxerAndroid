package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.ReminderAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.fragment.ucp.ReminderFragment.ReminderInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.util.DeviceUtils
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.Validators
import com.proxerme.app.util.ViewUtils
import com.proxerme.library.connection.ucp.entitiy.Reminder
import com.proxerme.library.connection.ucp.request.DeleteReminderRequest
import com.proxerme.library.connection.ucp.request.ReminderRequest
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class ReminderFragment : PagedLoadingFragment<ReminderInput, Reminder>() {

    companion object {

        fun newInstance(): ReminderFragment {
            return ReminderFragment()
        }
    }

    private val removalSuccess = { nothing: Void? ->
        adapter.remove(adapter.itemsToRemove.first())

        if (adapter.itemsToRemove.isNotEmpty()) {
            processQueuedRemovals()
        }
    }

    private val removalException = { exception: Exception ->
        val amount = adapter.itemsToRemove.size
        val action = ErrorUtils.handle(activity as MainActivity, exception)

        adapter.clearRemovalQueue()

        ViewUtils.makeMultilineSnackbar(root, context.resources
                .getQuantityString(R.plurals.error_reminder_removal, amount, amount, action.message),
                Snackbar.LENGTH_LONG).setAction(action.buttonMessage, action.buttonAction).show()
    }

    override val section = Section.REMINDER
    override val itemsOnPage = 30
    override val resetOnRefresh = true
    override val isLoginRequired = true

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ReminderAdapter

    @CategoryParameter.Category
    private var category: String? = null

    private var removalTask: Task<RemovalInput, Void> = constructRemovalTask()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ReminderAdapter()
        adapter.callback = object : ReminderAdapter.ReminderAdapterCallback() {
            override fun onItemClick(item: Reminder) {
                MediaActivity.navigateTo(activity, item.entryId, item.name)
            }

            override fun onRemoveClick(item: Reminder) {
                adapter.addItemToRemove(item)
                processQueuedRemovals()
            }
        }

        setHasOptionsMenu(true)
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
        layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
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
        removalTask.destroy()

        super.onDestroy()
    }

    override fun constructTask(): ListenableTask<ReminderInput, Array<Reminder>> {
        return ProxerLoadingTask({
            ReminderRequest(it.page)
                    .withCategory(it.category)
                    .withLimit(it.itemsOnPage)
        })
    }

    override fun constructInput(page: Int): ReminderInput {
        return ReminderInput(page, category, itemsOnPage)
    }

    override fun getEmptyString(): String {
        return getString(R.string.error_no_data_reminder)
    }

    private fun constructRemovalTask(): Task<RemovalInput, Void> {
        return ValidatingTask(ProxerLoadingTask({
            DeleteReminderRequest(it.id)
        }), { Validators.validateLogin() }, removalSuccess, removalException)
    }

    private fun processQueuedRemovals() {
        if (!removalTask.isWorking) {
            removalTask.execute(RemovalInput(adapter.itemsToRemove.first().id))
        }
    }

    class ReminderInput(page: Int, val category: String?, val itemsOnPage: Int) : PagedInput(page)
    class RemovalInput(val id: String)
}