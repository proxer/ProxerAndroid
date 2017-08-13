package me.proxer.app.profile.media

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.user.UserMediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType

/**
 * @author Ruben Gees
 */
class ProfileMediaListViewModel(application: Application) : PagedContentViewModel<UserMediaListEntry>(application) {

    override val itemsOnPage: Int
        get() = 30

    override val endpoint: PagingLimitEndpoint<List<UserMediaListEntry>>
        get() = api.user().mediaList(userId, username)
                .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(globalContext)
                        && StorageHelper.user != null)
                .category(category)
                .filter(filter)

    var userId: String? = null
    var username: String? = null

    private var category: Category = Category.ANIME
    private var filter: UserMediaListFilterType? = null

    fun setCategory(value: Category, trigger: Boolean = true) {
        if (category != value) {
            category = value

            if (trigger) reload()
        }
    }

    fun setFilter(value: UserMediaListFilterType?, trigger: Boolean = true) {
        if (filter != value) {
            filter = value

            if (trigger) reload()
        }
    }
}
