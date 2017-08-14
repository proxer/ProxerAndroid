package me.proxer.app.ucp.overview

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
class UcpOverviewViewModel(application: Application) : BaseContentViewModel<Int>(application) {

    override val isLoginRequired = true

    override val endpoint: Endpoint<Int>
        get() = api.ucp().watchedEpisodes()
}