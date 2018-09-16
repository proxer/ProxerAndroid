package me.proxer.app.util.data

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem

/**
 * @author Ruben Gees
 */
@Suppress("UseDataClass")
class PreferenceHelper(private val sharedPreferences: SharedPreferences) {

    companion object {
        const val AGE_CONFIRMATION = "age_confirmation"
        const val AUTO_BOOKMARK = "auto_bookmark"
        const val START_PAGE = "start_page"
        const val THEME = "theme"
        const val NOTIFICATIONS_NEWS = "notifications_news"
        const val NOTIFICATIONS_ACCOUNT = "notifications_account"
        const val NOTIFICATIONS_CHAT = "notifications_chat"
        const val NOTIFICATIONS_INTERVAL = "notifications_interval"
        const val MANGA_VERTICAL_READER = "manga_vertical_reader"
        const val EXTERNAL_CACHE = "external_cache"
    }

    var isAgeRestrictedMediaAllowed
        get() = sharedPreferences.getBoolean(AGE_CONFIRMATION, false)
        set(value) {
            sharedPreferences.edit { putBoolean(AGE_CONFIRMATION, value) }
        }

    val areBookmarksAutomatic
        get() = sharedPreferences.getBoolean(AUTO_BOOKMARK, false)

    val startPage
        get() = DrawerItem.fromIdOrDefault(
            sharedPreferences.getSafeString(START_PAGE, "0").toLongOrNull()
        )

    var areNewsNotificationsEnabled
        get() = sharedPreferences.getBoolean(NOTIFICATIONS_NEWS, false)
        set(value) {
            sharedPreferences.edit { putBoolean(NOTIFICATIONS_NEWS, value) }
        }

    var areAccountNotificationsEnabled
        get() = sharedPreferences.getBoolean(NOTIFICATIONS_ACCOUNT, false)
        set(value) {
            sharedPreferences.edit { putBoolean(NOTIFICATIONS_ACCOUNT, value) }
        }

    val areChatNotificationsEnabled
        get() = sharedPreferences.getBoolean(NOTIFICATIONS_CHAT, true)

    val notificationsInterval
        get() = sharedPreferences.getSafeString(NOTIFICATIONS_INTERVAL, "30").toLong()

    var isVerticalReaderEnabled
        get() = sharedPreferences.getBoolean(MANGA_VERTICAL_READER, true)
        set(value) {
            sharedPreferences.edit { putBoolean(MANGA_VERTICAL_READER, value) }
        }

    var shouldCacheExternally
        get() = sharedPreferences.getBoolean(EXTERNAL_CACHE, true)
        set(value) {
            sharedPreferences.edit { putBoolean(EXTERNAL_CACHE, value) }
        }

    val isCacheExternallySet
        get() = sharedPreferences.contains(EXTERNAL_CACHE)

    @AppCompatDelegate.NightMode
    val nightMode
        get() = when (sharedPreferences.getString(THEME, "2")) {
            "0" -> AppCompatDelegate.MODE_NIGHT_AUTO
            "1" -> AppCompatDelegate.MODE_NIGHT_YES
            "2" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> throw IllegalArgumentException("Invalid value")
        }
}
