package com.proxerme.app.fragment.framework

import android.support.v4.app.Fragment
import com.proxerme.app.application.MainApplication
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.UserManager

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class MainFragment : Fragment() {

    abstract val section: SectionManager.Section

    override fun onResume() {
        super.onResume()

        SectionManager.currentSection = section

        UserManager.reLogin()
    }

    override fun onPause() {
        super.onPause()

        SectionManager.currentSection = SectionManager.Section.NONE
    }

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    enum class RefreshLifecycle {CREATE, START, RESUME }
}