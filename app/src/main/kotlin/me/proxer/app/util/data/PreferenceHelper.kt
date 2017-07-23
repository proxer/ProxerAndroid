package me.proxer.app.util.data

import android.content.Context
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.v7.app.AppCompatDelegate
import me.proxer.app.util.MaterialDrawerWrapper.DrawerItem

/**
 * @author Ruben Gees
 */
object PreferenceHelper {

    const val AGE_CONFIRMATION = "age_confirmation"
    const val START_PAGE = "start_page"
    const val THEME = "theme"
    const val NOTIFICATIONS_NEWS = "notifications_news"
    const val NOTIFICATIONS_ACCOUNT = "notifications_account"
    const val NOTIFICATIONS_CHAT = "notifications_chat"
    const val NOTIFICATIONS_INTERVAL = "notifications_interval"
    const val MANGA_WLAN = "manga_unmetered_required"
    const val MANGA_CLEAN = "manga_clean"

    fun isAgeRestrictedMediaAllowed(context: Context)
            = getDefaultSharedPreferences(context).getBoolean(AGE_CONFIRMATION, false)

    fun getStartPage(context: Context) = DrawerItem
            .fromOrDefault(getDefaultSharedPreferences(context).getString(START_PAGE, "0").toLongOrNull())

    fun setAgeRestrictedMediaAllowed(context: Context, allowed: Boolean)
            = getDefaultSharedPreferences(context).edit().putBoolean(AGE_CONFIRMATION, allowed).apply()

    fun areNewsNotificationsEnabled(context: Context)
            = getDefaultSharedPreferences(context).getBoolean(NOTIFICATIONS_NEWS, false)

    fun areAccountNotificationsEnabled(context: Context)
            = getDefaultSharedPreferences(context).getBoolean(NOTIFICATIONS_ACCOUNT, false)

    fun areChatNotificationsEnabled(context: Context)
            = getDefaultSharedPreferences(context).getBoolean(NOTIFICATIONS_CHAT, true)

    fun setNewsNotificationsEnabled(context: Context, enabled: Boolean)
            = getDefaultSharedPreferences(context).edit().putBoolean(NOTIFICATIONS_NEWS, enabled).apply()

    fun setAccountNotificationsEnabled(context: Context, enabled: Boolean)
            = getDefaultSharedPreferences(context).edit().putBoolean(NOTIFICATIONS_ACCOUNT, enabled).apply()

    fun getNotificationsInterval(context: Context)
            = getDefaultSharedPreferences(context).getString(NOTIFICATIONS_INTERVAL, "30").toLong()

    fun isUnmeteredNetworkRequiredForMangaDownload(context: Context)
            = getDefaultSharedPreferences(context).getBoolean(MANGA_WLAN, true)

    @AppCompatDelegate.NightMode
    fun getNightMode(context: Context): Int {
        return when (getDefaultSharedPreferences(context).getString(THEME, "2")) {
            "0" -> AppCompatDelegate.MODE_NIGHT_AUTO
            "1" -> AppCompatDelegate.MODE_NIGHT_YES
            "2" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> throw RuntimeException("Invalid value")
        }
    }
}
