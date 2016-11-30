package com.proxerme.app.fragment.chat

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.chat.ChatActivity
import com.proxerme.app.activity.chat.NewChatActivity
import com.proxerme.app.adapter.chat.ConferenceAdapter
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.service.ChatService
import com.proxerme.app.task.ConferenceTask
import com.proxerme.app.task.Task
import com.proxerme.app.util.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferencesFragment : PagedLoadingFragment<LocalConference>() {

    companion object {
        fun newInstance(): ConferencesFragment {
            return ConferencesFragment()
        }
    }

    override val section = Section.CONFERENCES
    override val itemsOnPage = ChatService.CONFERENCES_ON_PAGE
    override val isLoginRequired = true
    override val isSwipeToRefreshEnabled = false

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ConferenceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceAdapter()
        adapter.callback = object : ConferenceAdapter.ConferenceAdapterCallback() {
            override fun onItemClick(item: LocalConference) {
                ChatActivity.navigateTo(activity, item)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_conferences, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_chat -> NewChatActivity.navigateTo(activity)
            R.id.new_group -> NewChatActivity.navigateTo(activity, isGroup = true)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun constructTask(pageCallback: () -> Int): Task<Array<LocalConference>> {
        return ConferenceTask({ context }, { pageCallback.invoke() == 0 })
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConferencesChanged(@Suppress("UNUSED_PARAMETER") event: ChatSynchronizationEvent) {
        adapter.update(event.newEntryMap.keys)
    }
}