package me.proxer.app.ucp.overview

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class UcpOverviewViewModel : BaseContentViewModel<Int>() {

    override val isLoginRequired = true

    override val endpoint: Endpoint<Int>
        get() = api.ucp().watchedEpisodes()
}
