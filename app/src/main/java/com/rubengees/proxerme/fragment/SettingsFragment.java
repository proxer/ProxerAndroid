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

package com.rubengees.proxerme.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.rubengees.proxerme.R;
import com.rubengees.proxerme.interfaces.OnActivityListener;
import com.rubengees.proxerme.manager.NewsManager;

/**
 * Todo: Describe Class
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
