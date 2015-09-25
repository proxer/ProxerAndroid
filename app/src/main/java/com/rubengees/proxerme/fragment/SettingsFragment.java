package com.rubengees.proxerme.fragment;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.TwoStatePreference;

import com.rubengees.proxerme.R;
import com.rubengees.proxerme.interfaces.OnBackPressedListener;
import com.rubengees.proxerme.manager.NewsManager;

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

        TwoStatePreference notificationPreference =
                (TwoStatePreference) findPreference("pref_news_notifications");

        notificationPreference.setOnPreferenceChangeListener(new Preference
                .OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                NewsManager manager = NewsManager.getInstance(getContext());

                if ((boolean) o) {
                    manager.retrieveNewsLater();
                } else {
                    manager.cancelNewsRetrieval();
                }

                return true;
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
