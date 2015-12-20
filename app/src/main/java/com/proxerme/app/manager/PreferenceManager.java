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

    public static final String PREFERENCE_NEWS_NOTIFICATIONS = "pref_news_notifications";
    public static final String PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL =
            "pref_news_notifications_interval";
    public static final String PREFERENCE_MESSAGES_NOTIFICATIONS = "pref_messages_notifications";

    public static boolean areNewsNotificationsEnabled(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return preferences.getBoolean(PREFERENCE_NEWS_NOTIFICATIONS, false);
    }

    public static void setNewsNotificationsEnabled(@NonNull Context context, boolean enabled) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        preferences.edit().putBoolean(PREFERENCE_NEWS_NOTIFICATIONS, enabled).apply();
    }

    public static int getNewsUpdateInterval(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return Integer.parseInt(preferences.getString(PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL, "60"));
    }

    public static boolean areMessagesNotificationsEnabled(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return preferences.getBoolean(PREFERENCE_MESSAGES_NOTIFICATIONS, false);
    }

    public static void setMessagesNotificationsEnabled(@NonNull Context context, boolean enabled) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        preferences.edit().putBoolean(PREFERENCE_MESSAGES_NOTIFICATIONS, enabled).apply();
    }
}
