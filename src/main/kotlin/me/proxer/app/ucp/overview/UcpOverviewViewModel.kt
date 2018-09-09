package me.proxer.app.ucp.overview

import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
class UcpOverviewViewModel : BaseContentViewModel<Int>() {

    override val isLoginRequired = true

    override val endpoint: Endpoint<Int>
        get() = api.ucp().watchedEpisodes()
}
