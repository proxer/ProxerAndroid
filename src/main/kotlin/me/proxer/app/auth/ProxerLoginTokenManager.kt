package me.proxer.app.auth

import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.LoginTokenManager

/**
 * @author Ruben Gees
 */
class ProxerLoginTokenManager(private val storageHelper: StorageHelper) : LoginTokenManager {

    override fun provide() = storageHelper.user?.token
    override fun persist(loginToken: String?) {
        when (loginToken) {
            null -> storageHelper.user = null
            else -> Unit /* Don't do anything in case the token is not null. We save the token
                            manually in the LoginDialog. */
        }
    }
}
