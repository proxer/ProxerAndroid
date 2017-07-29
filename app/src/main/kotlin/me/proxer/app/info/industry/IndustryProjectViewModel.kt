package me.proxer.app.info.industry

import android.app.Application
import me.proxer.app.MainApplication
import me.proxer.app.base.PagedViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.list.IndustryProject

/**
 * @author Ruben Gees
 */
class IndustryProjectViewModel(application: Application) : PagedViewModel<IndustryProject>(application) {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<IndustryProject>>
        get() = MainApplication.api.list().industryProjectList(industryId)

    lateinit var industryId: String
}
