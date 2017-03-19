package me.proxer.app.helper

import com.orhanobut.hawk.Hawk
import me.proxer.app.entity.LocalUser

/**
 * A helper class, giving access to the storage.

 * @author Ruben Gees
 */
object StorageHelper {

    private const val FIRST_START = "first_start"
    private const val USER = "user"
    private const val LOGIN_TOKEN = "login_token"

    var firstStart: Boolean
        get() = Hawk.get(FIRST_START, true)
        set(firstStart) {
            Hawk.put(FIRST_START, false)
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

    var loginToken: String?
        get() = Hawk.get(LOGIN_TOKEN)
        set(value) {
            when (value) {
                null -> Hawk.delete(LOGIN_TOKEN)
                else -> Hawk.put(LOGIN_TOKEN, value)
            }
        }
}
