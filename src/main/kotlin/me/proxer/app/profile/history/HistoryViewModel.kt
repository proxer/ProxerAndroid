package me.proxer.app.profile.history

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.user.UserHistoryEntry

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class HistoryViewModel(
    private val userId: String?,
    private val username: String?
) : PagedContentViewModel<UserHistoryEntry>() {

    override val isLoginRequired = true
    override val itemsOnPage = 50

    override val endpoint: PagingLimitEndpoint<List<UserHistoryEntry>>
        get() = api.user().history(userId, username)
            .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(MainApplication.globalContext) && StorageHelper.isLoggedIn)
}
