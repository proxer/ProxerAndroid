package me.proxer.app.media.list

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.list.MediaListEntry
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType

/**
 * @author Ruben Gees
 */
class MediaListViewModel(application: Application) : PagedContentViewModel<MediaListEntry>(application) {

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

    private var sortCriteria = MediaSearchSortCriteria.RATING
    private var type = MediaType.ALL
    private var searchQuery: String? = null

    fun setSortCriteria(value: MediaSearchSortCriteria, trigger: Boolean = true) {
        if (sortCriteria != value) {
            sortCriteria = value

            if (trigger) reload()
        }
    }

    fun setType(value: MediaType, trigger: Boolean = true) {
        if (type != value) {
            type = value

            if (trigger) reload()
        }
    }

    fun setSearchQuery(value: String?, trigger: Boolean = true) {
        if (searchQuery != value) {
            searchQuery = value

            if (trigger) reload()
        }
    }
}
