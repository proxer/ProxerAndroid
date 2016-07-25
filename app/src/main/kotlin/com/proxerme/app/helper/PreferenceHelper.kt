package com.proxerme.app.helper

import android.content.Context
import android.content.SharedPreferences

import android.preference.PreferenceManager.getDefaultSharedPreferences

/**
 * A helper class, which gives access to the [SharedPreferences].

 * @author Ruben Gees
 */
object PreferenceHelper {

    const val PREFERENCE_NEWS_NOTIFICATIONS = "pref_news_notifications"
    const val PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL = "pref_news_notifications_interval"
    const val PREFERENCE_MESSAGES_NOTIFICATIONS = "pref_messages_notifications"
    const private val DEFAULT_NEWS_NOTIFICATIONS_INTERVAL = "60"

    fun areNewsNotificationsEnabled(context: Context): Boolean =
            getDefaultSharedPreferences(context).getBoolean(PREFERENCE_NEWS_NOTIFICATIONS, false)

    fun setNewsNotificationsEnabled(context: Context, enabled: Boolean) =
            getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREFERENCE_NEWS_NOTIFICATIONS, enabled).apply()

    fun getNewsUpdateInterval(context: Context): Long =
            getDefaultSharedPreferences(context).getString(PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL,
                    DEFAULT_NEWS_NOTIFICATIONS_INTERVAL).toLong()

    fun areChatNotificationsEnabled(context: Context): Boolean {
        val preferences = getDefaultSharedPreferences(context)

        return preferences.getBoolean(PREFERENCE_MESSAGES_NOTIFICATIONS, false)
    }

    fun setChatNotificationsEnabled(context: Context, enabled: Boolean) {
        val preferences = getDefaultSharedPreferences(context)

        preferences.edit().putBoolean(PREFERENCE_MESSAGES_NOTIFICATIONS, enabled).apply()
    }
}
