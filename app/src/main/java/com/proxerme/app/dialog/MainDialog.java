package com.proxerme.app.dialog;

import android.support.v4.app.DialogFragment;

import com.proxerme.app.application.MainApplication;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class MainDialog extends DialogFragment {

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    protected final MainApplication getMainApplication() {
        return (MainApplication) getActivity().getApplication();
    }

}
