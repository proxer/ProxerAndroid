package me.proxer.app.util.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import me.proxer.app.auth.LocalUser
import me.proxer.app.profile.settings.LocalProfileSettings
import me.proxer.library.enums.AnimeLanguage
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
        const val TEMPORARY_TOKEN = "temporary_token"
        const val PROFILE_SETTINGS = "ucp_settings"
        const val LAST_NOTIFICATIONS_DATE = "last_notifications_date"
        const val LAST_CHAT_MESSAGE_DATE = "last_chat_date"
        const val CHAT_INTERVAL = "chat_interval"
        const val CONFERENCES_SYNCHRONIZED = "conferences_synchronized"
        const val LAST_PROFILE_SETTINGS_UPDATE_DATE = "last_ucp_settings_update_date"
        const val LAST_AD_ALERT_DATE = "last_ad_alert_date"
        const val MESSAGE_DRAFT_PREFIX = "message_draft_"
        const val COMMENT_DRAFT_PREFIX = "comment_draft_"
        const val LAST_MANGA_PAGE_PREFIX = "last_manga_page_"
        const val LAST_ANIME_POSITION_PREFIX = "last_anime_time_"

        private const val DEFAULT_CHAT_INTERVAL = 10_000L
        private const val MAX_CHAT_INTERVAL = 850_000L
    }

    init {
        initializer.initAndMigrateIfNecessary()
    }

    var user: LocalUser?
        get() {
            return sharedPreferences.getString(USER, null)
                ?.let { moshi.adapter(LocalUser::class.java).fromJson(it) }
        }
        set(value) {
            sharedPreferences.edit(commit = true) {
                if (value != null) {
                    putString(USER, moshi.adapter(LocalUser::class.java).toJson(value))
                    remove(TEMPORARY_TOKEN)
                } else {
                    // Due to a bug in androidx-securty-crypto, we can't remove the entry here because then then
                    // no listener is called.
                    // TODO: Remove instead of put once fixed.
                    putString(USER, null)
                }
            }
        }

    /**
     * Special token to use when no user is set. Used for checking if a token is valid before saving the actual user
     * and thus updating the UI.
     */
    var temporaryToken: String?
        get() {
            return sharedPreferences.getString(TEMPORARY_TOKEN, null)
        }
        set(value) {
            sharedPreferences.edit(commit = true) {
                if (value != null) {
                    putString(TEMPORARY_TOKEN, value)
                } else {
                    remove(TEMPORARY_TOKEN)
                }
            }
        }

    var profileSettings: LocalProfileSettings
        get() {
            return sharedPreferences.getString(PROFILE_SETTINGS, null)
                ?.let { moshi.adapter(LocalProfileSettings::class.java).fromJson(it) }
                ?: LocalProfileSettings.default()
        }
        set(value) {
            sharedPreferences.edit {
                putString(PROFILE_SETTINGS, moshi.adapter(LocalProfileSettings::class.java).toJson(value))
                putLong(LAST_PROFILE_SETTINGS_UPDATE_DATE, Instant.now().toEpochMilli())
            }
        }

    val isLoggedIn: Boolean
        get() = sharedPreferences.getString(USER, null) != null

    val isLoggedInObservable: Observable<Boolean> = rxPreferences.getString(USER)
        .asObservable()
        .skip(1)
        .map { it.isNotBlank() }
        .distinctUntilChanged()
        .publish()
        .autoConnect()
        .observeOn(AndroidSchedulers.mainThread())

    var lastNotificationsDate: Instant
        get() = Instant.ofEpochMilli(sharedPreferences.getLong(LAST_NOTIFICATIONS_DATE, 0L))
        set(value) {
            sharedPreferences.edit { putLong(LAST_NOTIFICATIONS_DATE, value.toEpochMilli()) }
        }

    var lastChatMessageDate: Instant
        get() = Instant.ofEpochMilli(sharedPreferences.getLong(LAST_CHAT_MESSAGE_DATE, 0L))
        set(value) {
            sharedPreferences.edit { putLong(LAST_CHAT_MESSAGE_DATE, value.toEpochMilli()) }
        }

    val chatInterval: Long
        get() = sharedPreferences.getLong(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    var areConferencesSynchronized: Boolean
        get() = sharedPreferences.getBoolean(CONFERENCES_SYNCHRONIZED, false)
        set(value) {
            sharedPreferences.edit { putBoolean(CONFERENCES_SYNCHRONIZED, value) }
        }

    val lastUcpSettingsUpdateDate: Instant
        get() = Instant.ofEpochMilli(sharedPreferences.getLong(LAST_PROFILE_SETTINGS_UPDATE_DATE, 0L))

    var lastAdAlertDate: Instant
        get() = Instant.ofEpochMilli(sharedPreferences.getLong(LAST_AD_ALERT_DATE, 0L))
        set(value) {
            sharedPreferences.edit { putLong(LAST_AD_ALERT_DATE, value.toEpochMilli()) }
        }

    fun incrementChatInterval() = chatInterval.let {
        if (it < MAX_CHAT_INTERVAL) {
            sharedPreferences.edit { putLong(CHAT_INTERVAL, (it * 1.5f).toLong()) }
        }
    }

    fun reset() = sharedPreferences.edit {
        sharedPreferences.all.forEach { (key) -> remove(key) }
    }

    fun resetChatInterval() = sharedPreferences.edit {
        putLong(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)
    }

    fun putMessageDraft(id: String, draft: String) = sharedPreferences.edit {
        putString("$MESSAGE_DRAFT_PREFIX$id", draft)
    }

    fun getMessageDraft(id: String): String? = sharedPreferences.getString("$MESSAGE_DRAFT_PREFIX$id", null)

    fun deleteMessageDraft(id: String) = sharedPreferences.edit {
        remove("$MESSAGE_DRAFT_PREFIX$id")
    }

    fun putCommentDraft(entryId: String, draft: String) = sharedPreferences.edit {
        putString("$COMMENT_DRAFT_PREFIX$entryId", draft)
    }

    fun getCommentDraft(entryId: String): String? = sharedPreferences.getString("$COMMENT_DRAFT_PREFIX$entryId", null)

    fun deleteCommentDraft(entryId: String) = sharedPreferences.edit {
        remove("$COMMENT_DRAFT_PREFIX$entryId")
    }

    fun putLastMangaPage(id: String, chapter: Int, language: Language, page: Int) {
        sharedPreferences.edit { putInt("${LAST_MANGA_PAGE_PREFIX}_${id}_${chapter}_$language", page) }
    }

    fun getLastMangaPage(id: String, chapter: Int, language: Language): Int? {
        return sharedPreferences.getInt("${LAST_MANGA_PAGE_PREFIX}_${id}_${chapter}_$language", -1)
            .let { if (it == -1) null else it }
    }

    fun putLastAnimePosition(id: String, episode: Int, language: AnimeLanguage, position: Long) {
        sharedPreferences.edit { putLong("${LAST_ANIME_POSITION_PREFIX}_${id}_${episode}_$language", position) }
    }

    fun getLastAnimePosition(id: String, episode: Int, language: AnimeLanguage): Long? {
        return sharedPreferences.getLong("${LAST_ANIME_POSITION_PREFIX}_${id}_${episode}_$language", -1)
            .let { if (it == -1L) null else it }
    }
}
