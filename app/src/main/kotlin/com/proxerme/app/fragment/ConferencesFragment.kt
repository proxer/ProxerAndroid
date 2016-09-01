package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import com.proxerme.app.activity.ChatActivity
import com.proxerme.app.adapter.ConferenceAdapter
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ConferencesEvent
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
        adapter.callback = object : ConferenceAdapter.OnConferenceInteractionListener() {
            override fun onConferenceClick(v: View, conference: LocalConference) {
                ChatActivity.navigateTo(activity, conference)
            }
        }

        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun loadFromDB(): Collection<LocalConference> {
        return context.chatDatabase.getConferences()
    }

    override fun startLoadMore() {
        ChatService.loadMoreConferences(context)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConferencesChanged(@Suppress("UNUSED_PARAMETER") event: ConferencesEvent) {
        if (canLoad) {
            refresh()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMoreConferencesFailed(exception: ChatService.LoadMoreConferencesException) {
        showError(exception)
    }
}