package me.proxer.app.profile.info

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.user.UserInfo

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ProfileInfoViewModel(private val userId: String?, private val username: String?) :
    BaseContentViewModel<UserInfo>() {

    override val endpoint: Endpoint<UserInfo>
        get() = api.user().info(userId, username)
}
