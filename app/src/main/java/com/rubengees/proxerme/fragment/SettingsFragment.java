package com.rubengees.proxerme.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.rubengees.proxerme.R;
import com.rubengees.proxerme.interfaces.OnBackPressedListener;
import com.rubengees.proxerme.manager.NewsManager;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class SettingsFragment extends PreferenceFragmentCompat implements OnBackPressedListener, SharedPreferences.OnSharedPreferenceChangeListener {

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

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_news_notifications")) {
            NewsManager manager = NewsManager.getInstance(getContext());

            if (sharedPreferences.getBoolean("pref_news_notifications", false)) {
                manager.retrieveNewsLater();
            } else {
                manager.cancelNewsRetrieval();
            }
        }
    }
}
