package me.proxer.app.profile.about

import com.gojuno.koptional.Optional
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.user.UserAbout

/**
 * @author Ruben Gees
 */
class ProfileAboutViewModel(
    private val userId: Optional<String>,
    private val username: Optional<String>
) : BaseContentViewModel<UserAbout>() {

    override val endpoint: Endpoint<UserAbout>
        get() = api.user.about(userId.toNullable(), username.toNullable())
}
