package me.proxer.app.helper

import com.orhanobut.hawk.Hawk
import me.proxer.app.entity.LocalUser
import java.util.*

/**
 * A helper class, giving access to the storage.

 * @author Ruben Gees
 */
object StorageHelper {

    private const val FIRST_START = "first_start"
    private const val USER = "user"
    private const val TWO_FACTOR_AUTHENTICATION = "two_factor_authentication"
    private const val LAST_NEWS_DATE = "last_news_time"

    var isFirstStart: Boolean
        get() = Hawk.get(FIRST_START, true)
        set(value) {
            Hawk.put(FIRST_START, value)
        }

    var user: LocalUser?
        get() = Hawk.get(USER)
        set(value) {
            when (value) {
                null -> {
                    Hawk.delete(USER)
                }
                else -> {
                    Hawk.put(USER, value)
                }
            }
        }

    var isTwoFactorAuthenticationEnabled: Boolean
        get() = Hawk.get(TWO_FACTOR_AUTHENTICATION, false)
        set(value) {
            Hawk.put(TWO_FACTOR_AUTHENTICATION, value)
        }

    var lastNewsTime: Date
        get() = Date(Hawk.get(LAST_NEWS_DATE, 0L))
        set(value) {
            Hawk.put(LAST_NEWS_DATE, value.time)
        }
}
