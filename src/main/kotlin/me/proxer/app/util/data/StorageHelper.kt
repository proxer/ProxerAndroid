package me.proxer.app.util.data

import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.Parser
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.moshi
import me.proxer.app.auth.LocalUser
import java.lang.reflect.Type
import java.util.*

/**
 * A helper class, giving access to the storage.

 * @author Ruben Gees
 */
object StorageHelper {

    private const val FIRST_START = "first_start"
    private const val USER = "user"
    private const val TWO_FACTOR_AUTHENTICATION = "two_factor_authentication"
    private const val LAST_NEWS_DATE = "last_news_date"
    private const val LAST_NOTIFICATIONS_DATE = "last_notifications_date"
    private const val LAST_CHAT_MESSAGE_DATE = "last_chat_date"
    private const val CHAT_INTERVAL = "chat_interval"
    private const val CONFERENCES_SYNCHRONIZED = "conferences_synchronized"

    private const val DEFAULT_CHAT_INTERVAL = 10_000L
    private const val MAX_CHAT_INTERVAL = 850_000L

    init {
        if (!Hawk.isBuilt()) {
            Hawk.init(globalContext).setParser(object : Parser {
                override fun <T : Any?> fromJson(content: String, type: Type) = moshi.adapter<T>(type).fromJson(content)
                override fun toJson(body: Any) = moshi.adapter(body.javaClass).toJson(body)
            }).build()
        }
    }

    var isFirstStart: Boolean
        get() = Hawk.get(FIRST_START, true)
        set(value) {
            Hawk.put(FIRST_START, value)
        }

    var user: LocalUser?
        get() = Hawk.get(USER)
        set(value) {
            when (value) {
                null -> Hawk.delete(USER)
                else -> Hawk.put(USER, value)
            }
        }

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

    fun incrementChatInterval() {
        Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL).let {
            if (it < MAX_CHAT_INTERVAL) {
                Hawk.put(CHAT_INTERVAL, (it * 1.5f).toLong())
            }
        }
    }

    fun resetChatInterval() {
        Hawk.put(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)
    }
}
