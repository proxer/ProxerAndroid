package me.proxer.app.media.list

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Genre
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import java.util.EnumSet
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class MediaListViewModel(
    sortCriteria: MediaSearchSortCriteria,
    type: MediaType,
    var searchQuery: String?,
    var language: Language?,
    var genres: EnumSet<Genre>,
    var excludedGenres: EnumSet<Genre>,
    var fskConstraints: EnumSet<FskConstraint>
) : PagedContentViewModel<MediaListEntry>() {

    override val itemsOnPage = 30

    override val isLoginRequired: Boolean
        get() = type == MediaType.HENTAI || type == MediaType.HMANGA

    override val isAgeConfirmationRequired: Boolean
        get() = isLoginRequired

    override val endpoint: PagingLimitEndpoint<List<MediaListEntry>>
        get() = api.list().mediaSearch()
                .sort(sortCriteria)
                .name(searchQuery)
                .language(language)
                .genres(genres)
                .excludedGenres(excludedGenres)
                .fskConstraints(fskConstraints)
                .type(type)

    var sortCriteria by Delegates.observable(sortCriteria, { _, old, new ->
        if (old != new) reload()
    })

    var type by Delegates.observable(type, { _, old, new ->
        if (old != new) reload()
    })

    override fun mergeNewDataWithExistingData(newData: List<MediaListEntry>, currentPage: Int): List<MediaListEntry> {
        return data.value?.plus(newData) ?: newData
    }
}
