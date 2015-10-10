package com.proxerme.app.fragment;

import android.support.v4.app.Fragment;

import com.proxerme.app.activity.DashboardActivity;
import com.proxerme.app.interfaces.OnActivityListener;

/**
 * An abstract {@link Fragment}, which all other Fragments should inherit from. It provides some
 * useful methods and implements interfaces, all inheritors share.
 */
public abstract class MainFragment extends Fragment implements OnActivityListener {

    public MainFragment() {
        
    }

    protected DashboardActivity getDashboardActivity(){
        try {
            return (DashboardActivity) getActivity();
        }catch(ClassCastException e){
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
