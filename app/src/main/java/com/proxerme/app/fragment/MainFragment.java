package com.proxerme.app.fragment;

import android.support.v4.app.Fragment;

import com.proxerme.app.activity.MainActivity;
import com.proxerme.app.interfaces.OnActivityListener;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class MainFragment extends Fragment implements OnActivityListener {

    protected MainActivity getParentActivity() throws RuntimeException {
        try {
            return (MainActivity) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException("Don't use this Fragment in another" +
                    " Activity than MainActivity.");
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

}
