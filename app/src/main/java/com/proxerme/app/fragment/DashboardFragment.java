package com.proxerme.app.fragment;

import com.proxerme.app.activity.DashboardActivity;

/**
 * An abstract Fragment, all Fragments which are shown in the {@link DashboardActivity} should
 * inherit from.
 *
 * @author Ruben Gees
 */
public abstract class DashboardFragment extends MainFragment {

    @Override
    protected DashboardActivity getParentActivity() throws RuntimeException {
        try {
            return (DashboardActivity) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException("Don't use this Fragment in another" +
                    " Activity than DashboardActivity.");
        }
    }

}
