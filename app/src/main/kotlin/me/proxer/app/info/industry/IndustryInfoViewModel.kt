package me.proxer.app.info.industry

import android.app.Application
import me.proxer.app.MainApplication
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entitiy.info.Industry

/**
 * @author Ruben Gees
 */
class IndustryInfoViewModel(application: Application) : BaseContentViewModel<Industry>(application) {

    override val endpoint: Endpoint<Industry>
        get() = MainApplication.api.info().industry(industryId)

    lateinit var industryId: String
}
