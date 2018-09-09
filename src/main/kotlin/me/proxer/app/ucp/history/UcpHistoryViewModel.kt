package me.proxer.app.ucp.history

import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.ucp.UcpHistoryEntry

/**
 * @author Ruben Gees
 */
class UcpHistoryViewModel : PagedContentViewModel<UcpHistoryEntry>() {

    override val isLoginRequired = true
    override val itemsOnPage = 50

    override val endpoint: PagingLimitEndpoint<List<UcpHistoryEntry>>
        get() = api.ucp().history()
}
