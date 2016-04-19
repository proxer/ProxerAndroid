package com.proxerme.app.job;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.CancelReason;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.proxerme.app.event.MessageEnqueuedEvent;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.event.error.SendingMessageFailedEvent;
import com.proxerme.library.event.success.MessageSentEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class SendMessageJob extends Job {

    private static final int PRIORITY = 0;
    private static final int RETRY_LIMIT = 3;

    private String conferenceId;
    private String text;

    public SendMessageJob(@NonNull String conferenceId, @NonNull String text) {
        super(new Params(PRIORITY).groupBy(conferenceId).requireNetwork().persist());

        this.conferenceId = conferenceId;
        this.text = text;
    }

    @Override
    public void onAdded() {
        EventBus.getDefault().post(new MessageEnqueuedEvent());
    }

    @Override
    public void onRun() throws Throwable {
        ProxerConnection.sendMessage(conferenceId, text).executeSynchronized();

        EventBus.getDefault().postSticky(new MessageSentEvent());
    }

    @Override
    protected void onCancel(@CancelReason int cancelReason) {

    }

    @SuppressLint("SwitchIntDef")
    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount,
                                                     int maxRunCount) {
        if (throwable instanceof ProxerException) {
            ProxerException exception = (ProxerException) throwable;

            if (runCount < maxRunCount) {
                switch (((ProxerException) throwable).getErrorCode()) {
                    case ProxerException.ERROR_PROXER:
                        EventBus.getDefault()
                                .post(new SendingMessageFailedEvent(exception));

                        return RetryConstraint.CANCEL;
                    default:
                        return RetryConstraint.RETRY;
                }
            } else {
                EventBus.getDefault().post(new SendingMessageFailedEvent(exception));

                return RetryConstraint.CANCEL;
            }
        } else {
            if (runCount < maxRunCount) {
                return RetryConstraint.RETRY;
            } else {
                EventBus.getDefault().post(new SendingMessageFailedEvent(
                        new ProxerException(ProxerException.ERROR_UNKNOWN)));

                return RetryConstraint.CANCEL;
            }
        }
    }

    @Override
    protected int getRetryLimit() {
        return RETRY_LIMIT;
    }
}
