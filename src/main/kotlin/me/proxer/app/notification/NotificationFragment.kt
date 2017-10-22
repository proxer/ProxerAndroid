package me.proxer.app.notification

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class NotificationFragment : BaseContentFragment<List<ProxerNotification>>() {

    companion object {
        fun newInstance() = NotificationFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = true

    override val viewModel by unsafeLazy { NotificationViewModelProvider.get(this) }

    private var adapter by Delegates.notNull<NotificationAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = NotificationAdapter()

        adapter.clickSubject
                .autoDispose(this)
                .subscribe { showPage(it.contentLink) }

        adapter.deleteClickSubject
                .autoDispose(this)
                .subscribe { viewModel.addItemToDelete(it) }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.deletionError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_notification_deletion, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))
            }
        })

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_notifications, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> if (!adapter.isEmpty()) {
                NotificationDeletionConfirmationDialog.show(hostingActivity, this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        AccountNotifications.cancel(context)
    }

    override fun showData(data: List<ProxerNotification>) {
        super.showData(data)

        val wasAtFirstPosition = isAtTop()
        val wasEmpty = adapter.isEmpty()

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            if (adapter.isEmpty()) {
                showError(ErrorAction(R.string.error_no_data_notifications, ACTION_MESSAGE_HIDE))
            } else if (wasAtFirstPosition || wasEmpty) {
                recyclerView.postDelayed({
                    when {
                        wasEmpty -> scrollToTop()
                        else -> recyclerView.smoothScrollToPosition(0)
                    }
                }, 50)
            }
        }
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        adapter.clearAndNotifyRemoval()

        super.showError(action)
    }

    private fun isAtTop() = recyclerView.layoutManager.let {
        when (it) {
            is LinearLayoutManager -> it.findFirstCompletelyVisibleItemPosition() == 0
            else -> false
        }
    }

    private fun scrollToTop() = recyclerView.layoutManager.let {
        when (it) {
            is StaggeredGridLayoutManager -> it.scrollToPositionWithOffset(0, 0)
            is LinearLayoutManager -> it.scrollToPositionWithOffset(0, 0)
        }
    }
}
