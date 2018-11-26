package me.proxer.app.notification

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.multilineSnackbar
import org.koin.androidx.viewmodel.ext.viewModel
import kotlin.properties.Delegates

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

    override val viewModel by viewModel<NotificationViewModel>()

    override val layoutManager by lazy {
        LinearLayoutManager(context)
    }

    override var innerAdapter by Delegates.notNull<NotificationAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NotificationAdapter()

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { showPage(it.contentLink) }

        innerAdapter.deleteClickSubject
            .autoDisposable(this.scope())
            .subscribe { viewModel.addItemToDelete(it) }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.deletionError.observe(viewLifecycleOwner, Observer {
            it?.let {
                multilineSnackbar(
                    root, getString(R.string.error_notification_deletion, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                )
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_notifications, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> if (!innerAdapter.isEmpty()) {
                NotificationDeletionConfirmationDialog.show(hostingActivity, this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        AccountNotifications.cancel(requireContext())
    }
}
