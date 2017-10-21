package me.proxer.app.media.list

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class MediaListViewModel(sortCriteria: MediaSearchSortCriteria, type: MediaType, searchQuery: String?) :
        PagedContentViewModel<MediaListEntry>() {

    override val itemsOnPage = 30

    override val isLoginRequired: Boolean
        get() = type == MediaType.HENTAI || type == MediaType.HMANGA

    override val isAgeConfirmationRequired: Boolean
        get() = isLoginRequired

    override val endpoint: PagingLimitEndpoint<List<MediaListEntry>>
        get() = api.list().mediaSearch()
                .sort(sortCriteria)
                .name(searchQuery)
                .type(type)

    var sortCriteria by Delegates.observable(sortCriteria, { _, old, new ->
        if (old != new) reload()
    })

    var type by Delegates.observable(type, { _, old, new ->
        if (old != new) reload()
    })

    var searchQuery by Delegates.observable(searchQuery, { _, old, new ->
        if (old != new) reload()
    })
}
