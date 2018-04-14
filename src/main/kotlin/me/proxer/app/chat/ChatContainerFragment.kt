package me.proxer.app.chat

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import kotterknife.bindView
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.base.BaseFragment
import me.proxer.app.chat.prv.conference.ConferenceFragment
import me.proxer.app.chat.pub.room.ChatRoomFragment
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf

class ChatContainerFragment : BaseFragment() {

    companion object {
        fun newInstance() = ChatContainerFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: MainActivity
        get() = activity as MainActivity

    private val sectionsPagerAdapter by unsafeLazy { SectionsPagerAdapter(childFragmentManager) }

    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hostingActivity.setElevation(0f)

        viewPager.adapter = sectionsPagerAdapter

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int) = when (position) {
            0 -> ChatRoomFragment.newInstance()
            1 -> ConferenceFragment.newInstance()
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): String = when (position) {
            0 -> "Öffentlich"
            1 -> "Privat"
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }
    }
}
