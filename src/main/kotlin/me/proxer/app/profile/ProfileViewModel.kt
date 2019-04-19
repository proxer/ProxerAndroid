package me.proxer.app.profile

import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.user.UserInfo

/**
 * @author Ruben Gees
 */
class ProfileViewModel(
    private val userId: String?,
    private val username: String?
) : BaseContentViewModel<UserInfo>() {

    override val endpoint: Endpoint<UserInfo>
        get() = api.user.info(userId, username)
}
