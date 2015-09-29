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

package com.rubengees.proxerme.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class PreferenceManager {

    public static final String PREFERENCE_NEW_NEWS = "pref_news_new";
    public static final String PREFERENCE_NEWS_NOTIFICATIONS = "pref_news_notifications";
    public static final String PREFERENCE_FIRST_START = "pref_first_start";
    private static final String PREFERENCE_NEWS_LAST_ID = "pref_news_last_id";

    public static boolean areNotificationsEnabled(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return preferences.getBoolean(PREFERENCE_NEWS_NOTIFICATIONS, false);
    }

    public static void setNotificationsEnabled(@NonNull Context context, boolean enabled) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        preferences.edit().putBoolean(PREFERENCE_NEWS_NOTIFICATIONS, enabled).apply();
    }

    public static int getLastId(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return preferences.getInt(PREFERENCE_NEWS_LAST_ID, -1);
    }

    public static void setLastId(@NonNull Context context, int id) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        preferences.edit().putInt(PREFERENCE_NEWS_LAST_ID, id).apply();
    }

    public static int getNewNews(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return preferences.getInt(PREFERENCE_NEW_NEWS, 0);
    }

    public static void setNewNews(@NonNull Context context, int amount) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        preferences.edit().putInt(PREFERENCE_NEW_NEWS, amount).apply();
    }

    public static void setFirstStartOccurred(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        preferences.edit().putBoolean(PREFERENCE_FIRST_START, false).apply();
    }

    public static boolean isFirstStart(@NonNull Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);

        return preferences.getBoolean(PREFERENCE_FIRST_START, true);
    }
}
