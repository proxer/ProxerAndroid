package me.proxer.app.info.translatorgroup

import android.app.Application
import me.proxer.app.MainApplication
import me.proxer.app.base.PagedViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.list.TranslatorGroupProject

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectViewModel(application: Application) : PagedViewModel<TranslatorGroupProject>(application) {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<TranslatorGroupProject>>
        get() = MainApplication.api.list().translatorGroupProjectList(translatorGroupId)

    lateinit var translatorGroupId: String
}