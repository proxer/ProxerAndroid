package me.proxer.app.profile.about

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.user.UserAbout

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ProfileAboutViewModel(
    private val userId: String?,
    private val username: String?
) : BaseContentViewModel<UserAbout>() {

    override val endpoint: Endpoint<UserAbout>
        get() = api.user().about(userId, username)
}
