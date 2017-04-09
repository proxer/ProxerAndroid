package me.proxer.app.util.listener

import android.transition.Transition

/**
 * @author Ruben Gees
 */
interface TransitionListenerWrapper : Transition.TransitionListener {

    override fun onTransitionEnd(transition: Transition?) {}
    override fun onTransitionResume(transition: Transition?) {}
    override fun onTransitionPause(transition: Transition?) {}
    override fun onTransitionCancel(transition: Transition?) {}
    override fun onTransitionStart(transition: Transition?) {}
}