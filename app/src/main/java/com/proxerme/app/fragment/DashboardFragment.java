package com.proxerme.app.fragment;

import android.support.v4.app.Fragment;

import com.proxerme.app.activity.DashboardActivity;
import com.proxerme.app.interfaces.OnActivityListener;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class DashboardFragment extends Fragment implements OnActivityListener {

    protected DashboardActivity getDashboardActivity() {
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
