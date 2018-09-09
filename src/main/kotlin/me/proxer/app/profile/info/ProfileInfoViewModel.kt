package me.proxer.app.profile.info

import com.gojuno.koptional.Optional
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.user.UserInfo

/**
 * @author Ruben Gees
 */
class ProfileInfoViewModel(
    private val userId: Optional<String>,
    private val username: Optional<String>
) : BaseContentViewModel<UserInfo>() {

    override val endpoint: Endpoint<UserInfo>
        get() = api.user().info(userId.toNullable(), username.toNullable())
}
