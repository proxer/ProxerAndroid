package me.proxer.app.media

import android.app.Application
import me.proxer.app.MainApplication
import me.proxer.app.base.PagedViewModel
import me.proxer.library.api.PagingEndpoint
import me.proxer.library.entitiy.list.MediaListEntry
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaSearchSortCriteria.RATING
import me.proxer.library.enums.MediaType
import kotlin.properties.Delegates.observable

/**
 * @author Ruben Gees
 */
class MediaListViewModel(application: Application) : PagedViewModel<MediaListEntry>(application) {

    override val itemsOnPage = 30
    override val endpoint: PagingEndpoint<List<MediaListEntry>>
        get() = MainApplication.api.list()
                .mediaSearch()
                .limit(itemsOnPage)

    var searchQuery by observable<String?>(null, { _, old, new ->
        if (old != new) {
            load()
        }
    })

    var type by observable<MediaType>(MediaType.ALL_ANIME, { _, old, new ->
        if (old != new) {
            load()
        }
    })

    var sortCriteria by observable<MediaSearchSortCriteria>(RATING, { _, old, new ->
        if (old != new) {
            load()
        }
    })
}