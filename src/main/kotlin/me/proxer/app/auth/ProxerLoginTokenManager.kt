package me.proxer.app.auth

import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.LoginTokenManager

class ProxerLoginTokenManager : LoginTokenManager {

    override fun provide() = StorageHelper.user?.token
    override fun persist(loginToken: String?) {
        when (loginToken) {
            null -> if (StorageHelper.user?.token != loginToken) {
                StorageHelper.user = null

                bus.post(LogoutEvent())
            }
            else -> Unit /* Don't do anything in case the token is not null. We save the token
                            manually in the LoginDialog. */
        }
    }
}
