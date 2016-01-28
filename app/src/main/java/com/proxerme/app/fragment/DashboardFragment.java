package com.proxerme.app.fragment;

import android.support.v4.app.Fragment;

import com.proxerme.app.activity.DashboardActivity;
import com.proxerme.app.interfaces.OnActivityListener;

/**
 * An abstract Fragment, all Fragments which are shown in the {@link DashboardActivity} should
 * inherit from.
 *
 * @author Ruben Gees
 */
public abstract class DashboardFragment extends Fragment implements OnActivityListener {

    protected DashboardActivity getDashboardActivity() throws RuntimeException {
        try {
            return (DashboardActivity) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException("Don't use this Fragment in another" +
                    " Activity than DashboardActivity.");
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void showErrorIfNecessary() {

    }

}
