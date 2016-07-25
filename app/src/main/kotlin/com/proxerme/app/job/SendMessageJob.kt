package com.proxerme.app.job

import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.proxerme.app.event.MessageSentEvent
import com.proxerme.app.manager.UserManager
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.experimental.chat.request.SendMessageRequest
import org.greenrobot.eventbus.EventBus

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class SendMessageJob(val conferenceId: String, val message: String) :
        Job(Params(1).requireNetwork().persist().groupBy(conferenceId)) {

    override fun onRun() {
        UserManager.reLoginSync()
        SendMessageRequest(conferenceId, message).executeSynchronized()

        EventBus.getDefault().post(MessageSentEvent(conferenceId))
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int,
                                        maxRunCount: Int): RetryConstraint {
        if (throwable is ProxerException) {
            if (throwable.errorCode == ProxerException.PROXER) {
                return RetryConstraint.CANCEL
            } else if (throwable.errorCode == ProxerException.UNPARSEABLE) {
                return RetryConstraint.CANCEL
            }
        }

        return RetryConstraint.RETRY
    }

    override fun onAdded() {
        //TODO
    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        //TODO
    }
}