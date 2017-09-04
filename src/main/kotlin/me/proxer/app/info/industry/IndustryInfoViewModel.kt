package me.proxer.app.info.industry

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Industry
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class IndustryInfoViewModel(application: Application) : BaseContentViewModel<Industry>(application) {

    override val endpoint: Endpoint<Industry>
        get() = api.info().industry(industryId)

    var industryId by Delegates.notNull<String>()
}
