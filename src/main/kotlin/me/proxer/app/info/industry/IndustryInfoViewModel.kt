package me.proxer.app.info.industry

import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Industry

/**
 * @author Ruben Gees
 */
class IndustryInfoViewModel(private val industryId: String) : BaseContentViewModel<Industry>() {

    override val endpoint: Endpoint<Industry>
        get() = api.info.industry(industryId)
}
