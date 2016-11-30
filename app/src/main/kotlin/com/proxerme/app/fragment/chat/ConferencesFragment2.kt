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
import com.proxerme.app.task.ConferenceTask2
import com.proxerme.app.task.Task
import com.proxerme.app.util.Utils
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.runOnUiThread

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferencesFragment2 : PagedLoadingFragment<LocalConference>() {

    companion object {
        fun newInstance(): ConferencesFragment2 {
            return ConferencesFragment2()
        }
    }

    override val section = Section.CONFERENCES
    override val itemsOnPage = 48
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

    override fun onDestroy() {
        adapter.callback = null

        super.onDestroy()
    }

    override fun constructTask(pageCallback: () -> Int): Task<Array<LocalConference>> {
        return ConferenceTask2({ context }, { pageCallback.invoke() == 0 })
    }

    @Suppress("unused")
    @Subscribe
    fun onConferencesChanged(@Suppress("UNUSED_PARAMETER") event: ChatSynchronizationEvent) {
        context.runOnUiThread {
            reset()
        }
    }
}