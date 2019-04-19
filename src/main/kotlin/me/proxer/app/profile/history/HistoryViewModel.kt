package me.proxer.app.profile.history

import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.user.UserHistoryEntry

/**
 * @author Ruben Gees
 */
class HistoryViewModel(
    private val userId: String?,
    private val username: String?
) : PagedContentViewModel<UserHistoryEntry>() {

    override val itemsOnPage = 50

    override val endpoint: PagingLimitEndpoint<List<UserHistoryEntry>>
        get() = api.user.history(userId, username)
            .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
}
