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
