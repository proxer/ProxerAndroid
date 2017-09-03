package me.proxer.app.info.industry

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Industry

/**
 * @author Ruben Gees
 */
class IndustryInfoViewModel(application: Application) : BaseContentViewModel<Industry>(application) {

    override val endpoint: Endpoint<Industry>
        get() = api.info().industry(industryId)

    lateinit var industryId: String
}
