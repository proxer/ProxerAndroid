package me.proxer.app.util.listener

import android.os.Build
import android.support.annotation.RequiresApi
import android.transition.Transition

/**
 * @author Ruben Gees
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
interface TransitionListenerWrapper : Transition.TransitionListener {

    override fun onTransitionEnd(transition: Transition?) {}
    override fun onTransitionResume(transition: Transition?) {}
    override fun onTransitionPause(transition: Transition?) {}
    override fun onTransitionCancel(transition: Transition?) {}
    override fun onTransitionStart(transition: Transition?) {}
}