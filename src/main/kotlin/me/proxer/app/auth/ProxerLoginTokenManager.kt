package me.proxer.app.auth

import me.proxer.app.util.data.SecurePreferenceHelper
import me.proxer.library.LoginTokenManager

/**
 * @author Ruben Gees
 */
class ProxerLoginTokenManager(private val securePreferenceHelper: SecurePreferenceHelper) : LoginTokenManager {

    override fun provide() = securePreferenceHelper.user?.token

    override fun persist(loginToken: String?) {
        when (loginToken) {
            null -> securePreferenceHelper.user = null
            else -> Unit /* Don't do anything in case the token is not null. We save the token
                            manually in the LoginDialog. */
        }
    }
}
