package me.proxer.app.util.data

import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.Parser
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.moshi
import me.proxer.app.auth.LocalUser
import java.lang.reflect.Type
import java.util.Date

/**
 * @author Ruben Gees
 */
object StorageHelper {

    private const val USER = "user"
    private const val TWO_FACTOR_AUTHENTICATION = "two_factor_authentication"
    private const val LAST_NEWS_DATE = "last_news_date"
    private const val LAST_NOTIFICATIONS_DATE = "last_notifications_date"
    private const val LAST_CHAT_MESSAGE_DATE = "last_chat_date"
    private const val CHAT_INTERVAL = "chat_interval"
    private const val CONFERENCES_SYNCHRONIZED = "conferences_synchronized"
    private const val LAST_TAG_UPDATE_DATE = "last_tag_update_date"
    private const val MESSAGE_DRAFT_PREFIX = "message_draft_"

    private const val DEFAULT_CHAT_INTERVAL = 10_000L
    private const val MAX_CHAT_INTERVAL = 850_000L

    private val jsonParser = object : Parser {
        override fun <T : Any?> fromJson(content: String, type: Type) = moshi.adapter<T>(type).fromJson(content)
        override fun toJson(body: Any) = moshi.adapter(body.javaClass).toJson(body)
    }

    var user: LocalUser?
        get() = safeGet(USER)
        set(value) = when (value) {
            null -> safeDelete(USER)
            else -> safePut(USER, value)
        }

    val isLoggedIn: Boolean
        get() = safeContains(USER)

    var isTwoFactorAuthenticationEnabled: Boolean
        get() = safeGet(TWO_FACTOR_AUTHENTICATION, false)
        set(value) = safePut(TWO_FACTOR_AUTHENTICATION, value)

    var lastNewsDate: Date
        get() = Date(safeGet(LAST_NEWS_DATE, 0L))
        set(value) = safePut(LAST_NEWS_DATE, value.time)

    var lastNotificationsDate: Date
        get() = Date(safeGet(LAST_NOTIFICATIONS_DATE, 0L))
        set(value) = safePut(LAST_NOTIFICATIONS_DATE, value.time)

    var lastChatMessageDate: Date
        get() = Date(safeGet(LAST_CHAT_MESSAGE_DATE, 0L))
        set(value) = safePut(LAST_CHAT_MESSAGE_DATE, value.time)

    val chatInterval: Long
        get() = safeGet(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    var areConferencesSynchronized: Boolean
        get() = safeGet(CONFERENCES_SYNCHRONIZED, false)
        set(value) = safePut(CONFERENCES_SYNCHRONIZED, value)

    var lastTagUpdateDate: Date
        get() = Date(safeGet(LAST_TAG_UPDATE_DATE, 0L))
        set(value) = safePut(LAST_TAG_UPDATE_DATE, value.time)

    fun incrementChatInterval() = safeGet(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL).let {
        if (it < MAX_CHAT_INTERVAL) {
            safePut(CHAT_INTERVAL, (it * 1.5f).toLong())
        }
    }

    fun resetChatInterval() {
        ensureInit()

        safePut(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)
    }

    fun putMessageDraft(id: String, draft: String) = safePut("$MESSAGE_DRAFT_PREFIX$id", draft)

    fun getMessageDraft(id: String): String? = safeGet("$MESSAGE_DRAFT_PREFIX$id")

    fun deleteMessageDraft(id: String) = safeDelete("$MESSAGE_DRAFT_PREFIX$id")

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> safeGet(key: String, defaultValue: T? = null): T {
        ensureInit()

        return when {
            defaultValue != null -> Hawk.get(key, defaultValue)
            else -> Hawk.get(key)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> safePut(key: String, value: T) {
        ensureInit()

        Hawk.put(key, value)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun safeDelete(key: String) {
        ensureInit()

        Hawk.delete(key)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun safeContains(key: String): Boolean {
        ensureInit()

        return Hawk.contains(key)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun ensureInit() {
        if (!Hawk.isBuilt()) {
            Hawk.init(globalContext).setParser(jsonParser).build()
        }
    }
}
