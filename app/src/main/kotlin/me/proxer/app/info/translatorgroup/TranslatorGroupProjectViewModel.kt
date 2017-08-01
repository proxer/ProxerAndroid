package me.proxer.app.info.translatorgroup

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.list.TranslatorGroupProject

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectViewModel(application: Application) : PagedContentViewModel<TranslatorGroupProject>(application) {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<TranslatorGroupProject>>
        get() = api.list().translatorGroupProjectList(translatorGroupId)
                .includeHentai(StorageHelper.user != null
                        && PreferenceHelper.isAgeRestrictedMediaAllowed(getApplication()))

    lateinit var translatorGroupId: String
}