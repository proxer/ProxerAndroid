package me.proxer.app.util.data

import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.orhanobut.hawk.Hawk
import me.proxer.app.auth.LocalUser
import me.proxer.app.exception.StorageException
import me.proxer.app.ucp.settings.LocalUcpSettings
import me.proxer.library.enums.Language
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
class StorageHelper(
    initializer: LocalDataInitializer,
    rxPreferences: RxSharedPreferences
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
        const val MESSAGE_DRAFT_PREFIX = "message_draft_"
        const val COMMENT_DRAFT_PREFIX = "comment_draft_"
        const val LAST_MANGA_PAGE_PREFIX = "last_manga_page_"

        private const val DEFAULT_CHAT_INTERVAL = 10_000L
        private const val MAX_CHAT_INTERVAL = 850_000L
    }

    init {
        initializer.initAndMigrateIfNecessary()
    }

    var user: LocalUser?
        get() = Hawk.get(USER)
        set(value) {
            putOrThrow(USER, value)
        }

    var ucpSettings: LocalUcpSettings
        get() = Hawk.get(UCP_SETTINGS) ?: LocalUcpSettings.default()
        set(value) {
            putOrThrow(UCP_SETTINGS, value)
            putOrThrow(LAST_UCP_SETTINGS_UPDATE_DATE, Instant.now().toEpochMilli())
        }

    val isLoggedIn: Boolean
        get() = Hawk.contains(USER)

    val isLoggedInObservable = rxPreferences.getString(USER)
        .asObservable()
        .map { it.isNotBlank() }
        .publish()
        .autoConnect()

    var isTwoFactorAuthenticationEnabled: Boolean
        get() = Hawk.get(TWO_FACTOR_AUTHENTICATION, false)
        set(value) {
            putOrThrow(TWO_FACTOR_AUTHENTICATION, value)
        }

    var lastNewsDate: Instant
        get() = Instant.ofEpochMilli(Hawk.get(LAST_NEWS_DATE, 0L))
        set(value) {
            putOrThrow(LAST_NEWS_DATE, value.toEpochMilli())
        }

    var lastNotificationsDate: Instant
        get() = Instant.ofEpochMilli(Hawk.get(LAST_NOTIFICATIONS_DATE, 0L))
        set(value) {
            putOrThrow(LAST_NOTIFICATIONS_DATE, value.toEpochMilli())
        }

    var lastChatMessageDate: Instant
        get() = Instant.ofEpochMilli(Hawk.get(LAST_CHAT_MESSAGE_DATE, 0L))
        set(value) {
            putOrThrow(LAST_CHAT_MESSAGE_DATE, value.toEpochMilli())
        }

    val chatInterval: Long
        get() = Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    var areConferencesSynchronized: Boolean
        get() = Hawk.get(CONFERENCES_SYNCHRONIZED, false)
        set(value) {
            putOrThrow(CONFERENCES_SYNCHRONIZED, value)
        }

    var lastTagUpdateDate: Instant
        get() = Instant.ofEpochMilli(Hawk.get(LAST_TAG_UPDATE_DATE, 0L))
        set(value) {
            putOrThrow(LAST_TAG_UPDATE_DATE, value.toEpochMilli())
        }

    val lastUcpSettingsUpdateDate: Instant
        get() = Instant.ofEpochMilli(Hawk.get(LAST_UCP_SETTINGS_UPDATE_DATE, 0L))

    var lastAdAlertDate: Instant
        get() = Instant.ofEpochMilli(Hawk.get(LAST_AD_ALERT_DATE, 0L))
        set(value) {
            putOrThrow(LAST_AD_ALERT_DATE, value.toEpochMilli())
        }

    fun incrementChatInterval() = Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL).let {
        if (it < MAX_CHAT_INTERVAL) {
            putOrThrow(CHAT_INTERVAL, (it * 1.5f).toLong())
        }
    }

    fun resetUserData() {
        lastChatMessageDate = Instant.ofEpochMilli(0L)
        lastNotificationsDate = Instant.ofEpochMilli(0L)
        areConferencesSynchronized = false

        deleteOrThrow(UCP_SETTINGS)
        deleteOrThrow(LAST_UCP_SETTINGS_UPDATE_DATE)

        resetChatInterval()

        Hawk.keys()
            .filter {
                it.startsWith(MESSAGE_DRAFT_PREFIX) ||
                    it.startsWith(COMMENT_DRAFT_PREFIX) ||
                    it.startsWith(LAST_MANGA_PAGE_PREFIX)
            }
            .forEach { deleteOrThrow(it) }
    }

    fun resetChatInterval() = putOrThrow(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    fun putMessageDraft(id: String, draft: String) = putOrThrow("$MESSAGE_DRAFT_PREFIX$id", draft)

    fun getMessageDraft(id: String): String? = Hawk.get("$MESSAGE_DRAFT_PREFIX$id")

    fun deleteMessageDraft(id: String) = deleteOrThrow("$MESSAGE_DRAFT_PREFIX$id")

    fun putCommentDraft(entryId: String, draft: String) = putOrThrow("$COMMENT_DRAFT_PREFIX$entryId", draft)

    fun getCommentDraft(entryId: String): String? = Hawk.get("$COMMENT_DRAFT_PREFIX$entryId")

    fun deleteCommentDraft(entryId: String) = deleteOrThrow("$COMMENT_DRAFT_PREFIX$entryId")

    fun putLastMangaPage(id: String, chapter: Int, language: Language, page: Int) {
        putOrThrow("${LAST_MANGA_PAGE_PREFIX}_${id}_${chapter}_$language", page)
    }

    fun getLastMangaPage(id: String, chapter: Int, language: Language): Int? {
        return Hawk.get("${LAST_MANGA_PAGE_PREFIX}_${id}_${chapter}_$language")
    }

    private fun <T> putOrThrow(key: String, value: T) {
        if (!Hawk.put(key, value)) throw StorageException("Could not persist $key")
    }

    private fun deleteOrThrow(key: String) {
        if (!Hawk.delete(key)) throw StorageException("Could not delete $key")
    }
}
