package me.proxer.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.ucp.NotificationAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.helper.notification.AccountNotificationHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.library.api.ProxerCall
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class NotificationFragment : PagedLoadingFragment<ProxerCall<List<ProxerNotification>>, ProxerNotification>() {

    companion object {
        fun newInstance(): NotificationFragment {
            return NotificationFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isSwipeToRefreshEnabled = true
    override val itemsOnPage = 30
    override val emptyResultMessage = R.string.error_no_data_notifications

    override val layoutManager by lazy {
        LinearLayoutManager(context)
    }

    override val innerAdapter = NotificationAdapter()

    private lateinit var removalTask: AndroidLifecycleTask<ProxerCall<Void?>, Void?>
    private val removalQueue = LinkedHashSet<ProxerNotification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        removalTask = TaskBuilder.asyncProxerTask<Void?>()
                .validateBefore {
                    Validators.validateLogin()
                }
                .bindToLifecycle(this, "${javaClass}_removal_task")
                .onSuccess {
                    if (removalQueue.isEmpty()) {
                        innerAdapter.clear()
                    } else {
                        innerAdapter.remove(removalQueue.first())
                        removalQueue.remove(removalQueue.first())
                    }

                    saveResultToState(innerAdapter.list)
                    deleteNotificationsFromQueue()
                    showContent()
                }
                .onError {
                    removalQueue.clear()

                    ErrorUtils.handle(activity as MainActivity, it).let {
                        multilineSnackbar(root, getString(R.string.error_bookmark_removal, getString(it.message)),
                                Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction)
                    }
                }.build()

        innerAdapter.callback = object : NotificationAdapter.NotificationAdapterCallback {
            override fun onItemClick(item: ProxerNotification) {
                showPage(item.contentLink)
            }

            override fun onDeleteClick(item: ProxerNotification) {
                removalQueue.add(item)

                deleteNotificationsFromQueue()
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_notifications, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> {
                if (!innerAdapter.isEmpty() && !task.isWorking) {
                    removalQueue.clear()
                    removalTask.freshExecute(api.notifications().deleteAllNotifications().build())
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        AccountNotificationHelper.cancel(context)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<ProxerNotification>>().build()
    override fun constructPagedInput(page: Int) = api.notifications().notifications()
            .markAsRead(page == 0)
            .page(page)
            .limit(itemsOnPage)
            .build()

    private fun deleteNotificationsFromQueue() {
        if (removalQueue.isNotEmpty()) {
            removalTask.execute(api.notifications().deleteNotification(removalQueue.first().id).build())
        }
    }
}