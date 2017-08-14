package me.proxer.app.notification

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class NotificationFragment : PagedContentFragment<ProxerNotification>() {

    companion object {
        fun newInstance() = NotificationFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = true
    override val emptyDataMessage = R.string.error_no_data_notifications

    override val viewModel: NotificationViewModel by unsafeLazy {
        ViewModelProviders.of(this).get(NotificationViewModel::class.java)
    }

    override val layoutManager by lazy {
        LinearLayoutManager(context)
    }

    override lateinit var innerAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NotificationAdapter()

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { showPage(it.contentLink) }

        innerAdapter.deleteClickSubject
                .bindToLifecycle(this)
                .subscribe { viewModel.addItemToDelete(it) }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.deletionError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_notification_removal, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_notifications, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> {
                if (!innerAdapter.isEmpty()) {
                    viewModel.deleteAll()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        AccountNotifications.cancel(context)
    }
}
