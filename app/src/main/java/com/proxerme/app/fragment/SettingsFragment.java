package com.proxerme.app.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.proxerme.app.R;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.manager.PreferenceManager;

/**
 * A Fragment, showing the settings of this App.
 *
 * @author Ruben Gees
 */
public class SettingsFragment extends PreferenceFragmentCompat implements OnActivityListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @NonNull
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);

        findPreference("pref_licences").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new LibsBuilder()
                        .withAboutVersionShownName(true)
                        .withAboutDescription(getString(R.string.about_description))
                        .withAboutIconShown(true)
                        .withAutoDetect(false)
                        .withAboutAppName(getString(R.string.app_name))
                        .withLibraries("glide", "jodatimeandroid", "bridge", "hawk", "butterknife",
                                "materialdialogs", "eventbus", "circleimageview", "priorityjobqueue")
                        .withExcludedLibraries("fastadapter", "materialize")
                        .withFields(R.string.class.getFields())
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .withActivityTitle(getContext().getString(R.string.about_libraries_title))
                        .start(getContext());

                return true;
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferenceManager.PREFERENCE_NEWS_NOTIFICATIONS: {
                boolean enabled =
                        sharedPreferences.getBoolean(PreferenceManager.PREFERENCE_NEWS_NOTIFICATIONS,
                                false);

                if (enabled) {
                    NotificationRetrievalManager.retrieveNewsLater(getContext());
                } else {
                    NotificationRetrievalManager.cancelNewsRetrieval(getContext());
                }
                break;
            }
            case PreferenceManager.PREFERENCE_MESSAGES_NOTIFICATIONS: {
                boolean enabled =
                        sharedPreferences.getBoolean(PreferenceManager.PREFERENCE_MESSAGES_NOTIFICATIONS,
                                false);

                if (enabled) {
                    NotificationRetrievalManager.retrieveMessagesLater(getContext());
                } else {
                    NotificationRetrievalManager.cancelMessagesRetrieval(getContext());
                }
                break;
            }
            case PreferenceManager.PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL:
                NotificationRetrievalManager.retrieveNewsLater(getContext());
                break;
        }
    }
}
