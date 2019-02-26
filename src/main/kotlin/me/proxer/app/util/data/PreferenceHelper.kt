package me.proxer.app.util.data

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import me.proxer.app.manga.MangaReaderOrientation
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import okhttp3.logging.HttpLoggingInterceptor

/**
 * @author Ruben Gees
 */
@Suppress("UseDataClass")
class PreferenceHelper(
    Initializer: LocalDataInitializer,
    private val sharedPreferences: SharedPreferences,
    private val rxSharedPreferences: RxSharedPreferences
) {

    companion object {
        const val AGE_CONFIRMATION = "age_confirmation"
        const val AUTO_BOOKMARK = "auto_bookmark"
        const val START_PAGE = "start_page"
        const val THEME = "theme"
        const val NOTIFICATIONS_NEWS = "notifications_news"
        const val NOTIFICATIONS_ACCOUNT = "notifications_account"
        const val NOTIFICATIONS_CHAT = "notifications_chat"
        const val NOTIFICATIONS_INTERVAL = "notifications_interval"
        const val MANGA_READER_ORIENTATION = "manga_reader_orientation"
        const val EXTERNAL_CACHE = "external_cache"
        const val HTTP_LOG_LEVEL = "http_log_level"
        const val HTTP_VERBOSE = "http_log_verbose"
        const val HTTP_REDACT_TOKEN = "http_log_redact_token"
    }

    init {
        Initializer.initAndMigrateIfNecessary()
    }

    var isAgeRestrictedMediaAllowed
        get() = sharedPreferences.getBoolean(AGE_CONFIRMATION, false)
        set(value) {
            sharedPreferences.edit { putBoolean(AGE_CONFIRMATION, value) }
        }

    val isAgeRestrictedMediaAllowedObservable: Observable<Boolean>
        get() = rxSharedPreferences.getBoolean(AGE_CONFIRMATION, false)
            .asObservable()

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

    var mangaReaderOrientation
        get() = MangaReaderOrientation.values()[
            sharedPreferences.getInt(MANGA_READER_ORIENTATION, MangaReaderOrientation.LEFT_TO_RIGHT.ordinal)
        ]
        set(value) {
            sharedPreferences.edit { putInt(MANGA_READER_ORIENTATION, value.ordinal) }
        }

    var shouldCacheExternally
        get() = sharedPreferences.getBoolean(EXTERNAL_CACHE, true)
        set(value) {
            sharedPreferences.edit { putBoolean(EXTERNAL_CACHE, value) }
        }

    val isCacheExternallySet
        get() = sharedPreferences.contains(EXTERNAL_CACHE)

    @AppCompatDelegate.NightMode
    var nightMode
        get() = when (sharedPreferences.getString(THEME, "2")) {
            "0" -> AppCompatDelegate.MODE_NIGHT_AUTO
            "1" -> AppCompatDelegate.MODE_NIGHT_YES
            "2" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> throw IllegalArgumentException("Unknown night mode value")
        }
        set(value) {
            val stringValue = when (value) {
                AppCompatDelegate.MODE_NIGHT_AUTO -> "0"
                AppCompatDelegate.MODE_NIGHT_YES -> "1"
                AppCompatDelegate.MODE_NIGHT_NO -> "2"
                else -> throw IllegalArgumentException("Unknown night mode value: $value")
            }

            sharedPreferences.edit { putString(THEME, stringValue) }
        }

    val httpLogLevel
        get() = when (sharedPreferences.getString(HTTP_LOG_LEVEL, "0")) {
            "0" -> HttpLoggingInterceptor.Level.BASIC
            "1" -> HttpLoggingInterceptor.Level.HEADERS
            "2" -> HttpLoggingInterceptor.Level.BODY
            else -> throw IllegalArgumentException("Unknown http log level saved in shared preferences")
        }

    val shouldLogHttpVerbose
        get() = sharedPreferences.getBoolean(HTTP_VERBOSE, false)

    val shouldRedactToken
        get() = sharedPreferences.getBoolean(HTTP_REDACT_TOKEN, false)
}
