package com.proxerme.app.fragment

import android.support.v4.app.Fragment
import com.proxerme.app.manager.UserManager

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class MainFragment : Fragment() {

    override fun onResume() {
        super.onResume()

        UserManager.reLogin()
    }
}