package me.proxer.app.ucp.media

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.user.UserMediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class UcpMediaListViewModel(
    category: Category,
    filter: UserMediaListFilterType?
) : PagedContentViewModel<UserMediaListEntry>() {

    override val isLoginRequired = true
    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<UserMediaListEntry>>
        get() = api.ucp().mediaList()
            .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(globalContext) && StorageHelper.isLoggedIn)
            .category(category)
            .filter(filter)

    var category by Delegates.observable(category, { _, old, new ->
        if (old != new) reload()
    })

    var filter by Delegates.observable(filter, { _, old, new ->
        if (old != new) reload()
    })
}
