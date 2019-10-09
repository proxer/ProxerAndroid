package me.proxer.app.chat

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotterknife.bindView
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.base.BaseFragment
import me.proxer.app.chat.prv.conference.ConferenceFragment
import me.proxer.app.chat.pub.room.ChatRoomFragment
import me.proxer.app.util.extension.unsafeLazy

/**
 * @author Ruben Gees
 */
class ChatContainerFragment : BaseFragment(R.layout.fragment_chat_container) {

    companion object {
        private const val SHOW_MESSENGER_ARGUMENT = "show_messenger"

        fun newInstance(showMessenger: Boolean = false) = ChatContainerFragment().apply {
            arguments = bundleOf(SHOW_MESSENGER_ARGUMENT to showMessenger)
        }
    }

    override val hostingActivity: MainActivity
        get() = activity as MainActivity

    private val sectionsPagerAdapter by unsafeLazy { SectionsPagerAdapter() }
    private val sectionsTabCallback by unsafeLazy { SectionsTabCallback() }

    private val viewPager: ViewPager2 by bindView(R.id.viewPager)
    private val tabs: TabLayout by unsafeLazy { hostingActivity.tabs }

    private var mediator: TabLayoutMediator? = null

    private val showMessenger
        get() = requireArguments().getBoolean(SHOW_MESSENGER_ARGUMENT, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs.isVisible = true

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = sectionsPagerAdapter

        if (savedInstanceState == null) {
            viewPager.currentItem = if (showMessenger) 1 else 0
        }

        mediator = TabLayoutMediator(tabs, viewPager, sectionsTabCallback).also { it.attach() }
    }

    override fun onDestroyView() {
        mediator?.detach()
        mediator = null
        viewPager.adapter = null

        tabs.isGone = true

        super.onDestroyView()
    }

    private inner class SectionsPagerAdapter :
        FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {

        override fun getItemCount() = 2

        override fun createFragment(position: Int) = when (position) {
            0 -> ChatRoomFragment.newInstance()
            1 -> ConferenceFragment.newInstance()
            else -> error("Unknown index passed: $position")
        }
    }

    private inner class SectionsTabCallback : TabLayoutMediator.TabConfigurationStrategy {

        override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
            tab.text = when (position) {
                0 -> getString(R.string.fragment_chat_container_public)
                1 -> getString(R.string.fragment_chat_container_private)
                else -> error("Unknown index passed: $position")
            }
        }
    }
}
