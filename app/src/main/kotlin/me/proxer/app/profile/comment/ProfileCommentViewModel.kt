package me.proxer.app.profile.comment

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.user.UserComment
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class ProfileCommentViewModel(application: Application) : PagedContentViewModel<UserComment>(application) {

    override val itemsOnPage: Int
        get() = 10

    override val endpoint: PagingLimitEndpoint<List<UserComment>>
        get() = api.user().comments(userId, username)
                .category(category)

    var userId: String? = null
    var username: String? = null

    private var category: Category? = null

    fun setCategory(value: Category?, trigger: Boolean = true) {
        if (category != value) {
            category = value

            if (trigger) reload()
        }
    }
}
