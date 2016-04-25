package com.proxerme.app.fragment;

import android.support.v4.app.Fragment;

import com.proxerme.app.activity.MainActivity;
import com.proxerme.app.application.MainApplication;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.util.EventBusBuffer;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class MainFragment extends Fragment implements OnActivityListener {

    protected MainActivity getParentActivity() throws RuntimeException {
        try {
            return (MainActivity) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException("Don't use this Fragment in another" +
                    " Activity than MainActivity.");
        }
    }

    protected final MainApplication getMainApplication() {
        return (MainApplication) getActivity().getApplication();
    }

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

    @Override
    public boolean onBackPressed() {
        return false;
    }

    protected abstract EventBusBuffer getEventBusBuffer();

}
