package me.proxer.app.helper

import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.Parser
import me.proxer.app.application.MainApplication.Companion.globalContext
import me.proxer.app.application.MainApplication.Companion.moshi
import me.proxer.app.entity.LocalUser
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

    var isFirstStart: Boolean
        get() {
            ensureInit()

            return Hawk.get(FIRST_START, true)
        }
        set(value) {
            ensureInit()

            Hawk.put(FIRST_START, value)
        }

    var user: LocalUser?
        get() {
            ensureInit()

            return Hawk.get(USER)
        }
        set(value) {
            ensureInit()

            when (value) {
                null -> Hawk.delete(USER)
                else -> Hawk.put(USER, value)
            }
        }

    var isTwoFactorAuthenticationEnabled: Boolean
        get() {
            ensureInit()

            return Hawk.get(TWO_FACTOR_AUTHENTICATION, false)
        }
        set(value) {
            ensureInit()

            Hawk.put(TWO_FACTOR_AUTHENTICATION, value)
        }

    var lastNewsDate: Date
        get() {
            ensureInit()

            return Date(Hawk.get(LAST_NEWS_DATE, 0L))
        }
        set(value) {
            ensureInit()

            Hawk.put(LAST_NEWS_DATE, value.time)
        }

    private fun ensureInit() {
        if (!Hawk.isBuilt()) {
            Hawk.init(globalContext).setParser(object : Parser {
                override fun <T : Any?> fromJson(content: String?, type: Type) = moshi.adapter<T>(type).fromJson(content)
                override fun toJson(body: Any) = moshi.adapter(body.javaClass).toJson(body)
            }).build()
        }
    }
}
