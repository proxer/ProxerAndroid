package me.proxer.app.fragment.ucp

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.adapter.ucp.NotificationAdapter
import me.proxer.app.application.MainApplication
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.helper.notification.AccountNotificationHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.extension.ProxerNotification
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

    override lateinit var innerAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NotificationAdapter()
        innerAdapter.callback = object : NotificationAdapter.NotificationAdapterCallback {
            override fun onItemClick(item: ProxerNotification) {
                showPage(item.contentLink)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        AccountNotificationHelper.cancel(context)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<ProxerNotification>>().build()
    override fun constructPagedInput(page: Int) = MainApplication.api.notifications().notifications()
            .markAsRead(page == 0)
            .page(page)
            .limit(itemsOnPage)
            .build()
}