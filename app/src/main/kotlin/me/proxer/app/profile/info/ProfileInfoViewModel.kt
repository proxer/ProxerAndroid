package me.proxer.app.profile.info

import android.app.Application
import me.proxer.app.MainApplication
import me.proxer.app.base.BaseViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entitiy.user.UserInfo

/**
 * @author Ruben Gees
 */
class ProfileInfoViewModel(application: Application) : BaseViewModel<UserInfo>(application) {

    override val endpoint: Endpoint<UserInfo>
        get() = MainApplication.api.user()
                .info(userId, username)

    var userId: String? = null
    var username: String? = null
}
