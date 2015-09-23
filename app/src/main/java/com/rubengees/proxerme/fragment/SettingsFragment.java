package com.rubengees.proxerme.fragment;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.rubengees.proxerme.R;
import com.rubengees.proxerme.interfaces.OnBackPressedListener;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class SettingsFragment extends PreferenceFragmentCompat implements OnBackPressedListener {

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
