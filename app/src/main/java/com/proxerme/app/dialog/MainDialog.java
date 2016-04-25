package com.proxerme.app.dialog;

import android.support.v4.app.DialogFragment;

import com.proxerme.app.application.MainApplication;
import com.proxerme.app.util.EventBusBuffer;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class MainDialog extends DialogFragment {

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
        getEventBusBuffer().stopAndProcess();
    }

    @Override
    public void onPause() {
        getEventBusBuffer().startBuffering();
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        getEventBusBuffer().stopAndPurge();
        getMainApplication().getRefWatcher().watch(this);

        super.onDestroy();
    }

    protected abstract EventBusBuffer getEventBusBuffer();

    protected final MainApplication getMainApplication() {
        return (MainApplication) getActivity().getApplication();
    }

}
