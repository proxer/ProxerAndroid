package me.proxer.app.util.data

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.orhanobut.hawk.Converter
import com.orhanobut.hawk.DataInfo
import com.orhanobut.hawk.Hawk
import me.proxer.app.auth.LocalUser
import me.proxer.app.util.extension.permitDisk
import org.koin.standalone.KoinComponent
import timber.log.Timber
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
        private const val LAUNCHES = "launches"
        private const val RATED = "rated"

        private const val DEFAULT_CHAT_INTERVAL = 10_000L
        private const val MAX_CHAT_INTERVAL = 850_000L
    }

    init {
        permitDisk {
            initHawk(context, jsonParser)

            migrateUser(context, jsonParser)
            migratePreferences(context)
        }
    }

    var user: LocalUser?
        get() = Hawk.get(USER)
        set(value) {
            permitDisk {
                when (value) {
                    null -> Hawk.delete(USER)
                    else -> Hawk.put(USER, value)
                }
            }
        }

    val isLoggedIn: Boolean
        get() = Hawk.contains(USER)

    var isTwoFactorAuthenticationEnabled: Boolean
        get() = Hawk.get(TWO_FACTOR_AUTHENTICATION, false)
        set(value) {
            permitDisk { Hawk.put(TWO_FACTOR_AUTHENTICATION, value) }
        }

    var lastNewsDate: Date
        get() = Date(Hawk.get(LAST_NEWS_DATE, 0L))
        set(value) {
            permitDisk { Hawk.put(LAST_NEWS_DATE, value.time) }
        }

    var lastNotificationsDate: Date
        get() = Date(Hawk.get(LAST_NOTIFICATIONS_DATE, 0L))
        set(value) {
            permitDisk { Hawk.put(LAST_NOTIFICATIONS_DATE, value.time) }
        }

    var lastChatMessageDate: Date
        get() = Date(Hawk.get(LAST_CHAT_MESSAGE_DATE, 0L))
        set(value) {
            permitDisk { Hawk.put(LAST_CHAT_MESSAGE_DATE, value.time) }
        }

    val chatInterval: Long
        get() = Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL)

    var areConferencesSynchronized: Boolean
        get() = Hawk.get(CONFERENCES_SYNCHRONIZED, false)
        set(value) {
            permitDisk { Hawk.put(CONFERENCES_SYNCHRONIZED, value) }
        }

    var lastTagUpdateDate: Date
        get() = Date(Hawk.get(LAST_TAG_UPDATE_DATE, 0L))
        set(value) {
            permitDisk { Hawk.put(LAST_TAG_UPDATE_DATE, value.time) }
        }

    var launches: Int
        get() = Hawk.get(LAUNCHES, 0)
        private set(value) {
            permitDisk { Hawk.put(LAUNCHES, value) }
        }

    var hasRated: Boolean
        get() = Hawk.get(RATED, false)
        set(value) {
            Hawk.put(RATED, value)
        }

    fun incrementChatInterval() = permitDisk {
        Hawk.get(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL).let {
            if (it < MAX_CHAT_INTERVAL) {
                Hawk.put(CHAT_INTERVAL, (it * 1.5f).toLong())
            }
        }
    }

    fun incrementLaunches() = permitDisk {
        Hawk.get(LAUNCHES, 0).let {
            Hawk.put(LAUNCHES, it + 1)
        }
    }

    fun resetChatInterval() = permitDisk { Hawk.put(CHAT_INTERVAL, DEFAULT_CHAT_INTERVAL) }

    fun putMessageDraft(id: String, draft: String) = permitDisk { Hawk.put("$MESSAGE_DRAFT_PREFIX$id", draft) }

    fun getMessageDraft(id: String): String? = Hawk.get("$MESSAGE_DRAFT_PREFIX$id")

    fun deleteMessageDraft(id: String) = permitDisk { Hawk.delete("$MESSAGE_DRAFT_PREFIX$id") }

    private fun initHawk(context: Context, jsonParser: HawkMoshiParser) {
        if (!Hawk.isBuilt()) {
            Hawk.init(context).setParser(jsonParser).setConverter(null).build()
        }
    }

    private fun migrateUser(
        context: Context,
        jsonParser: HawkMoshiParser
    ) {
        if (Hawk.contains(USER) && user == null) {
            // On older versions of the App, the user is saved in an obfuscated format. Fix this by reading the previous
            // format and saving in the proper format.
            Hawk.init(context)
                .setConverter(MigrationConverter(jsonParser))
                .build()

            val migrationUser: MigrationLocalUser? = Hawk.get(USER)

            Hawk.destroy()
            initHawk(context, jsonParser)

            if (migrationUser != null) {
                user = LocalUser(
                    migrationUser.token,
                    migrationUser.id,
                    migrationUser.name,
                    migrationUser.image
                )
            } else {
                Timber.e("Could not migrate user")
            }
        }
    }

    private fun migratePreferences(context: Context) {
        // On older versions of the App, this information is saved as an unencrypted preference. While not critical,
        // these are not preferences and should be hidden from the user.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (sharedPreferences.contains(LAUNCHES)) {
            launches = sharedPreferences.getInt(LAUNCHES, 0)

            sharedPreferences.edit(commit = true) { remove(LAUNCHES) }
        }

        if (sharedPreferences.contains(RATED)) {
            hasRated = sharedPreferences.getBoolean(RATED, false)

            sharedPreferences.edit(commit = true) { remove(RATED) }
        }
    }

    private class MigrationConverter(private val jsonParser: HawkMoshiParser) : Converter {
        override fun <T : Any?> toString(value: T): String {
            // This should never be called
            return ""
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> fromString(value: String, dataInfo: DataInfo?): T {
            return jsonParser.fromJson<MigrationLocalUser>(
                value,
                MigrationLocalUser::class.java
            ) as T
        }
    }
}
