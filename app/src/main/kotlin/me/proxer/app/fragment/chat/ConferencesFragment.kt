package me.proxer.app.fragment.chat

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.ChatActivity
import me.proxer.app.activity.NewChatActivity
import me.proxer.app.adapter.chat.ConferenceAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.event.LogoutEvent
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.helper.notification.ChatNotificationHelper
import me.proxer.app.job.ChatJob
import me.proxer.app.task.chat.ConferencesTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.bindView
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class ConferencesFragment : LoadingFragment<Unit, List<LocalConference>>() {

    companion object {
        var isActive: Boolean = false
            private set

        fun newInstance(): ConferencesFragment {
            return ConferencesFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isLoginRequired = true
    override val shouldRefreshAlways = true

    private val adapter by lazy { ConferenceAdapter(GlideApp.with(this)) }

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        isActive = true

        ChatNotificationHelper.cancel(context)

        if (!ChatJob.isRunning()) {
            ChatJob.scheduleSynchronization()
        }
    }

    override fun onPause() {
        isActive = false

        super.onPause()
    }

    override fun onDestroy() {
        adapter.destroy()

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conferences, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_conferences, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_chat -> NewChatActivity.navigateTo(activity, false)
            R.id.new_group -> NewChatActivity.navigateTo(activity, true)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.callback = object : ConferenceAdapter.ConferenceAdapterCallback {
            override fun onConferenceClick(item: LocalConference) {
                ChatActivity.navigateTo(activity, item)
            }
        }

        list.setHasFixedSize(true)
        list.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)
        list.adapter = adapter
    }

    override fun onSuccess(result: List<LocalConference>) {
        adapter.insert(result)

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (adapter.isEmpty()) {
            showError(R.string.error_no_data_conferences, R.string.error_action_new_chat, View.OnClickListener {
                NewChatActivity.navigateTo(activity)
            })
        }
    }

    override fun onLogout(event: LogoutEvent) {
        adapter.clear()

        super.onLogout(event)
    }

    override fun constructInput() = Unit
    override fun constructTask() = TaskBuilder.task(ConferencesTask())
            .async()
            .build()
}
