package me.proxer.app.util.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.squareup.moshi.Moshi
import me.proxer.app.auth.LocalUser
import me.proxer.app.ucp.settings.LocalUcpSettings
import me.proxer.app.util.extension.fromJson
import me.proxer.app.util.extension.toJson
import me.proxer.library.enums.Language
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
class StorageHelper(
    initializer: LocalDataInitializer,
    rxPreferences: RxSharedPreferences,
    private val sharedPreferences: SharedPreferences,
    private val moshi: Moshi
) {

    internal companion object {
        const val USER = "user"
        const val UCP_SETTINGS = "ucp_settings"
        const val TWO_FACTOR_AUTHENTICATION = "two_factor_authentication"
        const val LAST_NEWS_DATE = "last_news_date"
        const val LAST_NOTIFICATIONS_DATE = "last_notifications_date"
        const val LAST_CHAT_MESSAGE_DATE = "last_chat_date"
        const val CHAT_INTERVAL = "chat_interval"
        const val CONFERENCES_SYNCHRONIZED = "conferences_synchronized"
        const val LAST_TAG_UPDATE_DATE = "last_tag_update_date"
        const val LAST_UCP_SETTINGS_UPDATE_DATE = "last_ucp_settings_update_date"
        const val LAST_AD_ALERT_DATE = "last_ad_alert_date"
        const val CAST_INTRODUCTORY_OVERLAY_SHOWN = "cast_introductory_overlay_shown"
        const val MESSAGE_DRAFT_PREFIX = "message_draft_"
        const val LAST_MANGA_PAGE_PREFIX = "last_manga_page_"
        const val LAUNCHES = "launches"
        const val RATED = "rated"

        private const val DEFAULT_CHAT_INTERVAL = 10_000L
        private const val MAX_CHAT_INTERVAL = 850_000L
    }

    init {
        initializer.initAndMigrateIfNecessary()
    }

    var user: LocalUser?
        get() = sharedPreferences.getString(USER, null)?.let { moshi.fromJson(it) }
        set(value) {
            sharedPreferences.edit(commit = true) {
                putString(USER, moshi.toJson(value))
            }
        }

    var ucpSettings: LocalUcpSettings
        get() = sharedPreferences.getString(UCP_SETTINGS, null)?.let { moshi.fromJson(it) }
            ?: LocalUcpSettings.default()
        set(value) {
            sharedPreferences.edit(commit = true) {
                putString(UCP_SETTINGS, moshi.toJson(value))
                putLong(LAST_UCP_SETTINGS_UPDATE_DATE, Instant.now().toEpochMilli())
            }

            lastUcpSettingsUpdateDate = Instant.now()
        }

    val isLoggedIn: Boolean
        get() = sharedPreferences.contains(USER)

    val isLoggedInObservable = rxPreferences.getString(USER)
        .asObservable()
        .map { it.isNotBlank() }
        .publish()
        .autoConnect()

    var isTwoFactorAuthenticationEnabled by booleanPreference(sharedPreferences, TWO_FACTOR_AUTHENTICATION)

    var lastNewsDate by instantPreference(sharedPreferences, LAST_NEWS_DATE)
    var lastNotificationsDate by instantPreference(sharedPreferences, LAST_NOTIFICATIONS_DATE)
    var lastChatMessageDate by instantPreference(sharedPreferences, LAST_CHAT_MESSAGE_DATE)
    var lastTagUpdateDate by instantPreference(sharedPreferences, LAST_TAG_UPDATE_DATE)
    var lastAdAlertDate by instantPreference(sharedPreferences, LAST_AD_ALERT_DATE)

    var areConferencesSynchronized by booleanPreference(sharedPreferences, CONFERENCES_SYNCHRONIZED)
    var wasCastIntroductoryOverlayShown by booleanPreference(sharedPreferences, CAST_INTRODUCTORY_OVERLAY_SHOWN)

    var lastUcpSettingsUpdateDate by instantPreference(sharedPreferences, LAST_UCP_SETTINGS_UPDATE_DATE)
        private set

    var chatInterval by longPreference(sharedPreferences, CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)
        private set

    var launches by intPreference(sharedPreferences, LAUNCHES)
        private set

    var hasRated by booleanPreference(sharedPreferences, RATED)

    fun incrementChatInterval() = chatInterval.let {
        if (it < MAX_CHAT_INTERVAL) {
            chatInterval = (it * 1.5f).toLong()
        }
    }

    fun incrementLaunches() {
        launches += 1
    }

    fun resetUserData() {
        lastChatMessageDate = Instant.ofEpochMilli(0L)
        lastNotificationsDate = Instant.ofEpochMilli(0L)
        areConferencesSynchronized = false

        sharedPreferences.edit(commit = true) {
            remove(UCP_SETTINGS)
            remove(LAST_UCP_SETTINGS_UPDATE_DATE)

            sharedPreferences.all.keys
                .filter { it.startsWith(MESSAGE_DRAFT_PREFIX) }
                .forEach { remove(it) }
        }

        resetChatInterval()
    }

    fun resetChatInterval() {
        chatInterval = DEFAULT_CHAT_INTERVAL
    }

    fun putMessageDraft(id: String, draft: String) = sharedPreferences.edit(commit = true) {
        putString("$MESSAGE_DRAFT_PREFIX$id", draft)
    }

    fun getMessageDraft(id: String): String? = sharedPreferences.getString("$MESSAGE_DRAFT_PREFIX$id", null)

    fun deleteMessageDraft(id: String) = sharedPreferences.edit(commit = true) {
        remove("$MESSAGE_DRAFT_PREFIX$id")
    }

    fun putLastMangaPage(id: String, chapter: Int, language: Language, page: Int) = sharedPreferences
        .edit(commit = true) {
            putInt("${LAST_MANGA_PAGE_PREFIX}_${id}_${chapter}_$language", page)
        }

    fun getLastMangaPage(id: String, chapter: Int, language: Language) = sharedPreferences
        .getInt("${LAST_MANGA_PAGE_PREFIX}_${id}_${chapter}_$language", Int.MIN_VALUE)
        .let { if (it == Int.MIN_VALUE) null else it }
}
