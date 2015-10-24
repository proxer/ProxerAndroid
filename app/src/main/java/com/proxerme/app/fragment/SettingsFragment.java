package com.proxerme.app.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.proxerme.app.R;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.PreferenceManager;

/**
 * A Fragment, showing the settings of this App.
 *
 * @author Ruben Gees
 */
public class SettingsFragment extends PreferenceFragmentCompat implements OnActivityListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @NonNull
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
    public void showErrorIfNecessary() {
        //Do nothing
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
        if (key.equals(PreferenceManager.PREFERENCE_NOTIFICATIONS)) {
            NewsManager manager = NewsManager.getInstance(getContext());
            boolean enabled =
                    sharedPreferences.getBoolean(PreferenceManager.PREFERENCE_NOTIFICATIONS, false);

            if (enabled) {
                manager.retrieveNewsLater();
            } else {
                manager.cancelNewsRetrieval();
            }
        } else if (key.equals(PreferenceManager.PREFERENCE_NOTIFICATIONS_INTERVAL)) {
            NewsManager.getInstance(getContext()).retrieveNewsLater();
        }
    }
}
