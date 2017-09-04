package me.proxer.app.profile.info

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.user.UserInfo

/**
 * @author Ruben Gees
 */
class ProfileInfoViewModel(application: Application) : BaseContentViewModel<UserInfo>(application) {

    override val endpoint: Endpoint<UserInfo>
        get() = api.user().info(userId, username)

    var userId: String? = null
    var username: String? = null
}
