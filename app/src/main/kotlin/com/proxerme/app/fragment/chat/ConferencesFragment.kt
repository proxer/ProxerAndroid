package com.proxerme.app.fragment.chat

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.proxerme.app.R
import com.proxerme.app.activity.chat.ChatActivity
import com.proxerme.app.activity.chat.NewChatActivity
import com.proxerme.app.adapter.chat.ConferenceAdapter
import com.proxerme.app.adapter.chat.ConferenceAdapter.ConferenceAdapterCallback
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ChatSynchronizationEvent
import com.proxerme.app.fragment.framework.EasyChatServiceFragment
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.service.ChatService
import com.proxerme.app.util.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class ConferencesFragment : EasyChatServiceFragment<LocalConference>() {

    companion object {
        fun newInstance(): ConferencesFragment {
            return ConferencesFragment()
        }
    }

    override val section: SectionManager.Section = SectionManager.Section.CONFERENCES

    override lateinit var layoutManager: RecyclerView.LayoutManager
    override lateinit var adapter: ConferenceAdapter

    override val hasReachedEnd: Boolean
        get() = StorageHelper.conferenceListEndReached
    override val isLoading: Boolean
        get() = ChatService.isLoadingConferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceAdapter()
        adapter.callback = object : ConferenceAdapterCallback() {
            override fun onItemClick(item: LocalConference) {
                ChatActivity.navigateTo(activity, item)
            }
        }

        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)

        setHasOptionsMenu(true)
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

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onDestroy() {
        adapter.callback = null

        super.onDestroy()
    }

    override fun loadFromDB(): Collection<LocalConference> {
        return context.chatDatabase.getConferences()
    }

    override fun startLoadMore() {
        ChatService.loadMoreConferences(context)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConferencesChanged(@Suppress("UNUSED_PARAMETER") event: ChatSynchronizationEvent) {
        if (canLoad) {
            refresh()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMoreConferencesFailed(exception: ChatService.LoadMoreConferencesException) {
        showError(exception)
    }
}