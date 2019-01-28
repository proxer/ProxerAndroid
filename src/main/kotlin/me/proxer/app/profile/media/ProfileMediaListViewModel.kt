package me.proxer.app.profile.media

import com.gojuno.koptional.Optional
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.user.UserMediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ProfileMediaListViewModel(
    private val userId: Optional<String>,
    private val username: Optional<String>,
    category: Category,
    filter: Optional<UserMediaListFilterType>
) : PagedContentViewModel<UserMediaListEntry>() {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<UserMediaListEntry>>
        get() = api.user.mediaList(userId.toNullable(), username.toNullable())
            .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
            .category(category)
            .filter(filter)

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }

    var filter by Delegates.observable(filter.toNullable()) { _, old, new ->
        if (old != new) reload()
    }
}
