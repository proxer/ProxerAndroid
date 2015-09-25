package com.rubengees.proxerme.fragment;

import android.support.v4.app.Fragment;

import com.rubengees.proxerme.activity.DashboardActivity;
import com.rubengees.proxerme.interfaces.OnBackPressedListener;

/**
 * An abstract Fragment which all other Fragments should inherit from. It provides some useful
 * methods and implements interfaces, all inheritors share.
 */
public abstract class MainFragment extends Fragment implements OnBackPressedListener {

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
}
