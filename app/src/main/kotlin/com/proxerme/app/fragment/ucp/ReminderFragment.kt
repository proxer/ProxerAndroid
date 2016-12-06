package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.ReminderAdapter
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.LoadingTask
import com.proxerme.app.task.ValidatingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.Utils
import com.proxerme.app.util.Validators
import com.proxerme.library.connection.ProxerException
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

    private val removalSuccess = { nothing: Void? ->
        adapter.remove(adapter.itemsToRemove.first())

        if (adapter.itemsToRemove.isNotEmpty()) {
            processQueuedRemovals()
        }
    }

    private val removalException = { exception: Exception ->
        val amount = adapter.itemsToRemove.size
        adapter.clearRemovalQueue()

        when (exception) {
            is Validators.NotLoggedInException -> Snackbar.make(root, R.string.status_not_logged_in,
                    Snackbar.LENGTH_LONG).setAction(R.string.module_login_login, {
                LoginDialog.show(activity as AppCompatActivity)
            })
            is ProxerException -> {
                Snackbar.make(root,
                        ErrorHandler.getMessageForErrorCode(context, exception),
                        Snackbar.LENGTH_LONG).show()
            }
            else -> {
                Snackbar.make(root, context.resources
                        .getQuantityString(R.plurals.error_reminder_removal, amount, amount),
                        Snackbar.LENGTH_LONG).show()
            }
        }

        Unit
    }

    override val section = Section.REMINDER
    override val itemsOnPage = 30
    override val resetOnRefresh = true
    override val isLoginRequired = true

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ReminderAdapter

    @CategoryParameter.Category
    private var category: String? = null

    private var removalTask: Task<Void> = constructRemovalTask()

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
        removalTask.cancel()

        super.onDestroy()
    }

    override fun constructTask(pageCallback: () -> Int): ListenableTask<Array<Reminder>> {
        return LoadingTask({
            ReminderRequest(pageCallback.invoke())
                    .withCategory(category)
                    .withLimit(itemsOnPage)
        })
    }

    private fun constructRemovalTask(): Task<Void> {
        return ValidatingTask(LoadingTask({
            DeleteReminderRequest(adapter.itemsToRemove.first().id)
        }), { Validators.validateLogin(true) }, removalSuccess, removalException)
    }

    private fun processQueuedRemovals() {
        if (!removalTask.isWorking) {
            removalTask.execute()
        }
    }
}