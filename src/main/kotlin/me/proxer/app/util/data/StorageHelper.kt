package me.proxer.app.util.data

import android.content.Context
import com.orhanobut.hawk.Converter
import com.orhanobut.hawk.DataInfo
import com.orhanobut.hawk.Hawk
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import me.proxer.app.auth.LocalUser
import org.koin.standalone.KoinComponent
import java.util.Date

/**
 * @author Ruben Gees
 */
class StorageHelper(context: Context, jsonParser: HawkMoshiParser) : KoinComponent {

    companion object {
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
    }

    init {
        initHawk(context, jsonParser)
        migrate(context, jsonParser)
    }

    var user: LocalUser?
        get() = Hawk.get(USER)
        set(value) {
            when (value) {
                null -> Hawk.delete(USER)
                else -> Hawk.put(USER, value)
            }
        }

    val isLoggedIn: Boolean
        get() = Hawk.contains(USER)

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

    fun incrementChatInterval() = Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL).let {
        if (it < MAX_CHAT_INTERVAL) {
            Hawk.put(CHAT_INTERVAL, (it * 1.5f).toLong())
        }
    }

    fun resetChatInterval() = Hawk.put(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    fun putMessageDraft(id: String, draft: String) = Hawk.put("$MESSAGE_DRAFT_PREFIX$id", draft)

    fun getMessageDraft(id: String): String? = Hawk.get("$MESSAGE_DRAFT_PREFIX$id")

    fun deleteMessageDraft(id: String) = Hawk.delete("$MESSAGE_DRAFT_PREFIX$id")

    private fun initHawk(context: Context, jsonParser: HawkMoshiParser) {
        if (!Hawk.isBuilt()) {
            Hawk.init(context).setParser(jsonParser).setConverter(null).build()
        }
    }

    private fun migrate(
        context: Context,
        jsonParser: HawkMoshiParser
    ) {
        if (Hawk.contains(USER) && user == null) {
            Hawk.init(context)
                .setConverter(MigrationConverter(jsonParser))
                .build()

            val brokenUser: MigrationLocalUser? = Hawk.get<MigrationLocalUser>("user")

            Hawk.destroy()
            initHawk(context, jsonParser)

            if (brokenUser != null) {
                Hawk.put(USER, LocalUser(brokenUser.token, brokenUser.id, brokenUser.name, brokenUser.image))
            }
        }
    }

    @JsonClass(generateAdapter = true)
    internal class MigrationLocalUser(
        @Json(name = "a") val token: String,
        @Json(name = "b") val id: String,
        @Json(name = "c") val name: String,
        @Json(name = "d") val image: String
    )

    private class MigrationConverter(private val jsonParser: HawkMoshiParser) : Converter {
        override fun <T : Any?> toString(value: T) = throw NotImplementedError("toString should not be called")

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> fromString(value: String, dataInfo: DataInfo?): T {
            return jsonParser.fromJson<MigrationLocalUser>(value, MigrationLocalUser::class.java) as T
        }
    }
}
