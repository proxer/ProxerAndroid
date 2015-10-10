/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.proxerme.app.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.proxerme.app.R;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.PreferenceManager;

/**
 * A {@link Fragment}, showing the settings of this App.
 *
 * @author Ruben Gees
 */
public class SettingsFragment extends PreferenceFragmentCompat implements OnActivityListener, SharedPreferences.OnSharedPreferenceChangeListener {

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
