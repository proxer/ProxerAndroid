package com.proxerme.app.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * A helper class, which gives access to the {@link SharedPreferences}.
 *
 * @author Ruben Gees
 */
public class PreferenceManager {

    public static final String PREFERENCE_NOTIFICATIONS = "pref_notifications";
    public static final String PREFERENCE_NOTIFICATIONS_INTERVAL = "pref_notifications_interval";

    public static boolean areNotificationsEnabled(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return preferences.getBoolean(PREFERENCE_NOTIFICATIONS, false);
    }

    public static void setNotificationsEnabled(@NonNull Context context, boolean enabled) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        preferences.edit().putBoolean(PREFERENCE_NOTIFICATIONS, enabled).apply();
    }

    public static int getUpdateInterval(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return Integer.parseInt(preferences.getString(PREFERENCE_NOTIFICATIONS_INTERVAL, "60"));
    }
}
