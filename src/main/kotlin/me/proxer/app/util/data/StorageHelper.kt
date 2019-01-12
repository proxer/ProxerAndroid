package me.proxer.app.util.data

import android.content.Context
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.orhanobut.hawk.Hawk
import io.reactivex.Observable
import me.proxer.app.auth.LocalUser
import me.proxer.app.ucp.settings.LocalUcpSettings
import org.koin.core.KoinComponent
import java.util.Date

/**
 * @author Ruben Gees
 */
class StorageHelper(
    context: Context,
    initializer: HawkInitializer,
    private val rxPreferences: RxSharedPreferences
) : KoinComponent {

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
        const val CAST_INTRODUCTORY_OVERLAY_SHOWN = "cast_introductory_overlay_shown"
        const val MESSAGE_DRAFT_PREFIX = "message_draft_"
        const val LAUNCHES = "launches"
        const val RATED = "rated"

        private const val DEFAULT_CHAT_INTERVAL = 10_000L
        private const val MAX_CHAT_INTERVAL = 850_000L
    }

    init {
        initializer.initAndMigrateIfNecessary(context)
    }

    var user: LocalUser?
        get() = Hawk.get(USER)
        set(value) {
            Hawk.put(USER, value)
        }

    var ucpSettings: LocalUcpSettings
        get() = Hawk.get(UCP_SETTINGS) ?: LocalUcpSettings.default()
        set(value) {
            Hawk.put(UCP_SETTINGS, value)
        }

    val isLoggedIn: Boolean
        get() = Hawk.contains(USER)

    val isLoggedInObservable: Observable<Boolean>
        get() = rxPreferences.getString(USER)
            .asObservable()
            .map { it.isNotBlank() }

    var isTwoFactorAuthenticationEnabled: Boolean
        get() = Hawk.get(TWO_FACTOR_AUTHENTICATION, false)
        set(value) {
            Hawk.put(TWO_FACTOR_AUTHENTICATION, value)
        }

    var lastNewsDate: Date
        get() = Date(Hawk.get(LAST_NEWS_DATE, 0L))
        set(value) {
            Hawk.put(LAST_NEWS_DATE, value.time)
        }

    var lastNotificationsDate: Date
        get() = Date(Hawk.get(LAST_NOTIFICATIONS_DATE, 0L))
        set(value) {
            Hawk.put(LAST_NOTIFICATIONS_DATE, value.time)
        }

    var lastChatMessageDate: Date
        get() = Date(Hawk.get(LAST_CHAT_MESSAGE_DATE, 0L))
        set(value) {
            Hawk.put(LAST_CHAT_MESSAGE_DATE, value.time)
        }

    val chatInterval: Long
        get() = Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    var areConferencesSynchronized: Boolean
        get() = Hawk.get(CONFERENCES_SYNCHRONIZED, false)
        set(value) {
            Hawk.put(CONFERENCES_SYNCHRONIZED, value)
        }

    var lastTagUpdateDate: Date
        get() = Date(Hawk.get(LAST_TAG_UPDATE_DATE, 0L))
        set(value) {
            Hawk.put(LAST_TAG_UPDATE_DATE, value.time)
        }

    var wasCastIntroductoryOverlayShown: Boolean
        get() = Hawk.get(CAST_INTRODUCTORY_OVERLAY_SHOWN, false)
        set(value) {
            Hawk.put(CAST_INTRODUCTORY_OVERLAY_SHOWN, value)
        }

    var launches: Int
        get() = Hawk.get(LAUNCHES, 0)
        private set(value) {
            Hawk.put(LAUNCHES, value)
        }

    var hasRated: Boolean
        get() = Hawk.get(RATED, false)
        set(value) {
            Hawk.put(RATED, value)
        }

    fun incrementChatInterval() = Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL).let {
        if (it < MAX_CHAT_INTERVAL) {
            Hawk.put(CHAT_INTERVAL, (it * 1.5f).toLong())
        }
    }

    fun incrementLaunches() = Hawk.get(LAUNCHES, 0).let {
        Hawk.put(LAUNCHES, it + 1)
    }

    fun resetUcpSettings() = Hawk.delete(UCP_SETTINGS)

    fun resetChatInterval() = Hawk.put(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    fun putMessageDraft(id: String, draft: String) = Hawk.put("$MESSAGE_DRAFT_PREFIX$id", draft)

    fun getMessageDraft(id: String): String? = Hawk.get("$MESSAGE_DRAFT_PREFIX$id")

    fun deleteMessageDraft(id: String) = Hawk.delete("$MESSAGE_DRAFT_PREFIX$id")
}
