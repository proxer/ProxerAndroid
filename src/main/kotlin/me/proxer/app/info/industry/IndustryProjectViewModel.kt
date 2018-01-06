package me.proxer.app.info.industry

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.IndustryProject

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class IndustryProjectViewModel(private val industryId: String) : PagedContentViewModel<IndustryProject>() {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<IndustryProject>>
        get() = api.list().industryProjectList(industryId)
                .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(globalContext) && StorageHelper.isLoggedIn)
}
