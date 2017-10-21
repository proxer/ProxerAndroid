package me.proxer.app.profile.comment

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.user.UserComment
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ProfileCommentViewModel(private val userId: String?, private val username: String?, category: Category?) :
        PagedContentViewModel<UserComment>() {

    override val itemsOnPage: Int
        get() = 10

    override val endpoint: PagingLimitEndpoint<List<UserComment>>
        get() = api.user().comments(userId, username)
                .category(category)

    var category by Delegates.observable(category, { _, old, new ->
        if (old != new) reload()
    })
}
