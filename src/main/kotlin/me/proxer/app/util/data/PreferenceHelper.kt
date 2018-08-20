package me.proxer.app.util.data

import android.content.Context
import android.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem

/**
 * @author Ruben Gees
 */
object PreferenceHelper {

    const val AGE_CONFIRMATION = "age_confirmation"
    const val AUTO_BOOKMARK = "auto_bookmark"
    const val START_PAGE = "start_page"
    const val THEME = "theme"
    const val NOTIFICATIONS_NEWS = "notifications_news"
    const val NOTIFICATIONS_ACCOUNT = "notifications_account"
    const val NOTIFICATIONS_CHAT = "notifications_chat"
    const val NOTIFICATIONS_INTERVAL = "notifications_interval"
    const val MANGA_VERTICAL_READER = "manga_vertical_reader"
    const val LAUNCHES = "launches"
    const val RATED = "rated"
    const val EXTERNAL_CACHE = "external_cache"

    fun isAgeRestrictedMediaAllowed(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(AGE_CONFIRMATION, false)

    fun areBookmarksAutomatic(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(AUTO_BOOKMARK, false)

    fun getStartPage(context: Context) = DrawerItem.fromIdOrDefault(getDefaultSharedPreferences(context)
        .getSafeString(START_PAGE, "0").toLongOrNull()
    )

    fun setAgeRestrictedMediaAllowed(context: Context, allowed: Boolean) = getDefaultSharedPreferences(context).edit()
        .putBoolean(AGE_CONFIRMATION, allowed).apply()

    fun areNewsNotificationsEnabled(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(NOTIFICATIONS_NEWS, false)

    fun areAccountNotificationsEnabled(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(NOTIFICATIONS_ACCOUNT, false)

    fun areChatNotificationsEnabled(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(NOTIFICATIONS_CHAT, true)

    fun setNewsNotificationsEnabled(context: Context, enabled: Boolean) = getDefaultSharedPreferences(context).edit()
        .putBoolean(NOTIFICATIONS_NEWS, enabled).apply()

    fun setAccountNotificationsEnabled(context: Context, enabled: Boolean) = getDefaultSharedPreferences(context).edit()
        .putBoolean(NOTIFICATIONS_ACCOUNT, enabled).apply()

    fun getNotificationsInterval(context: Context) = getDefaultSharedPreferences(context)
        .getSafeString(NOTIFICATIONS_INTERVAL, "30").toLong()

    fun isVerticalReaderEnabled(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(MANGA_VERTICAL_READER, true)

    fun setVerticalReaderEnabled(context: Context, enabled: Boolean) = getDefaultSharedPreferences(context).edit()
        .putBoolean(MANGA_VERTICAL_READER, enabled).apply()

    fun getLaunches(context: Context) = getDefaultSharedPreferences(context)
        .getInt(LAUNCHES, 0)

    fun incrementLaunches(context: Context) = getDefaultSharedPreferences(context).edit()
        .putInt(LAUNCHES, getLaunches(context) + 1).apply()

    fun hasRated(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(RATED, false)

    fun setHasRated(context: Context) = getDefaultSharedPreferences(context).edit()
        .putBoolean(RATED, true).apply()

    fun shouldCacheExternally(context: Context) = getDefaultSharedPreferences(context)
        .getBoolean(EXTERNAL_CACHE, true)

    fun setCacheExternally(context: Context, enabled: Boolean) = getDefaultSharedPreferences(context).edit()
        .putBoolean(EXTERNAL_CACHE, enabled).apply()

    fun isCacheExternallySet(context: Context) = getDefaultSharedPreferences(context).contains(EXTERNAL_CACHE)

    @AppCompatDelegate.NightMode
    fun getNightMode(context: Context) = when (getDefaultSharedPreferences(context).getString(THEME, "2")) {
        "0" -> AppCompatDelegate.MODE_NIGHT_AUTO
        "1" -> AppCompatDelegate.MODE_NIGHT_YES
        "2" -> AppCompatDelegate.MODE_NIGHT_NO
        else -> throw IllegalArgumentException("Invalid value")
    }
}
