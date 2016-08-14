package com.proxerme.app.helper

import android.content.Context
import android.content.SharedPreferences

import android.preference.PreferenceManager.getDefaultSharedPreferences

/**
 * A helper class, which gives access to the [SharedPreferences].

 * @author Ruben Gees
 */
object PreferenceHelper {

    const val PREFERENCE_HENTAI = "pref_hentai"
    const val PREFERENCE_LICENCES = "pref_licences"
    const val PREFERENCE_NEWS_NOTIFICATIONS = "pref_news_notifications"
    const val PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL = "pref_news_notifications_interval"
    const private val DEFAULT_NEWS_NOTIFICATIONS_INTERVAL = "60"

    fun isHentaiAllowed(context: Context) =
            getDefaultSharedPreferences(context).getBoolean(PREFERENCE_HENTAI, false)

    fun areNewsNotificationsEnabled(context: Context) =
            getDefaultSharedPreferences(context).getBoolean(PREFERENCE_NEWS_NOTIFICATIONS, false)

    fun setNewsNotificationsEnabled(context: Context, enabled: Boolean) =
            getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREFERENCE_NEWS_NOTIFICATIONS, enabled).apply()

    fun getNewsUpdateInterval(context: Context) =
            getDefaultSharedPreferences(context).getString(PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL,
                    DEFAULT_NEWS_NOTIFICATIONS_INTERVAL).toLong()

    fun setHentaiAllowed(context: Context, isAllowed: Boolean) =
            getDefaultSharedPreferences(context).edit().putBoolean(PREFERENCE_HENTAI, isAllowed).apply()
}
