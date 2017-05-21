package me.proxer.app.task

import com.rubengees.ktask.base.LeafTask
import com.rubengees.ktask.base.Task
import org.greenrobot.eventbus.EventBus

/**
 * @author Ruben Gees
 */
abstract class EventBusTask<I, O> : LeafTask<I, O>() {

    override fun restoreCallbacks(from: Task<I, O>) {
        super.restoreCallbacks(from)

        safelyRegister()
    }

    override fun retainingDestroy() {
        safelyUnregister()

        super.retainingDestroy()
    }

    override fun destroy() {
        safelyUnregister()

        super.destroy()
    }

    protected fun safelyRegister() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    protected fun safelyUnregister() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}