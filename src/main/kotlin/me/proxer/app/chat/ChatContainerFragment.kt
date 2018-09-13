package me.proxer.app.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import kotterknife.bindView
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.base.BaseFragment
import me.proxer.app.chat.prv.conference.ConferenceFragment
import me.proxer.app.chat.pub.room.ChatRoomFragment
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class ChatContainerFragment : BaseFragment() {

    companion object {
        private const val SHOW_MESSENGER_ARGUMENT = "show_messenger"

        fun newInstance(showMessenger: Boolean = false) = ChatContainerFragment().apply {
            arguments = bundleOf(SHOW_MESSENGER_ARGUMENT to showMessenger)
        }
    }

    override val hostingActivity: MainActivity
        get() = activity as MainActivity

    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val tabs: TabLayout by unsafeLazy { hostingActivity.tabs }

    private val showMessenger
        get() = requireArguments().getBoolean(SHOW_MESSENGER_ARGUMENT, false)

    private var tabLayoutHelper: TabLayoutHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs.visibility = View.VISIBLE
        viewPager.adapter = SectionsPagerAdapter(childFragmentManager)

        tabLayoutHelper = TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }

        if (savedInstanceState == null) {
            viewPager.currentItem = if (showMessenger) 1 else 0
        }
    }

    override fun onDestroyView() {
        tabLayoutHelper?.release()
        tabLayoutHelper = null
        viewPager.adapter = null

        tabs.visibility = View.GONE

        super.onDestroyView()
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
